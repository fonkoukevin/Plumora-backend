[CmdletBinding()]
param(
	[string]$BaseUrl = "http://localhost:8080/api/v1",
	[string]$AdminEmail = "admin@plumora.test",
	[string]$AdminPassword = "password",
	[switch]$SkipAdmin
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$BaseUrl = $BaseUrl.TrimEnd("/")
$script:Passed = 0
$script:Failed = 0
$script:Skipped = 0
$script:RunSuffix = [guid]::NewGuid().ToString("N").Substring(0, 10)
$script:HasSkipHttpErrorCheck = (Get-Command Invoke-WebRequest).Parameters.ContainsKey("SkipHttpErrorCheck")
$script:HasUseBasicParsing = (Get-Command Invoke-WebRequest).Parameters.ContainsKey("UseBasicParsing")

function Write-Section {
	param([string]$Title)
	Write-Host ""
	Write-Host "== $Title ==" -ForegroundColor Cyan
}

function Write-Pass {
	param([string]$Name)
	$script:Passed++
	Write-Host "[PASS] $Name" -ForegroundColor Green
}

function Write-Fail {
	param(
		[string]$Name,
		[string]$Message
	)
	$script:Failed++
	Write-Host "[FAIL] $Name" -ForegroundColor Red
	Write-Host "       $Message" -ForegroundColor DarkRed
}

function Write-Skip {
	param(
		[string]$Name,
		[string]$Reason
	)
	$script:Skipped++
	Write-Host "[SKIP] $Name - $Reason" -ForegroundColor Yellow
}

function Limit-Text {
	param([string]$Text)
	if ([string]::IsNullOrWhiteSpace($Text)) {
		return ""
	}
	if ($Text.Length -le 700) {
		return $Text
	}
	return $Text.Substring(0, 700) + "..."
}

function Convert-ResponseJson {
	param([string]$Body)
	if ([string]::IsNullOrWhiteSpace($Body)) {
		return $null
	}
	try {
		return $Body | ConvertFrom-Json -ErrorAction Stop
	} catch {
		return $null
	}
}

function Get-ErrorBody {
	param($ErrorRecord)
	if ($ErrorRecord.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($ErrorRecord.ErrorDetails.Message)) {
		return $ErrorRecord.ErrorDetails.Message
	}

	$response = $ErrorRecord.Exception.Response
	if ($null -eq $response) {
		return ""
	}

	try {
		if ($response.Content) {
			return $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
		}
	} catch {
	}

	try {
		$stream = $response.GetResponseStream()
		if ($null -ne $stream) {
			$reader = [System.IO.StreamReader]::new($stream)
			return $reader.ReadToEnd()
		}
	} catch {
	}

	return ""
}

function Invoke-Api {
	param(
		[ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")]
		[string]$Method,
		[string]$Path,
		[object]$Body = $null,
		[string]$Token = $null
	)

	$headers = @{
		Accept = "application/json"
	}
	if (-not [string]::IsNullOrWhiteSpace($Token)) {
		$headers.Authorization = "Bearer $Token"
	}

	$params = @{
		Uri = "$BaseUrl$Path"
		Method = $Method
		Headers = $headers
		ErrorAction = "Stop"
	}
	if ($script:HasUseBasicParsing) {
		$params.UseBasicParsing = $true
	}
	if ($script:HasSkipHttpErrorCheck) {
		$params.SkipHttpErrorCheck = $true
	}
	if ($null -ne $Body) {
		$params.ContentType = "application/json"
		$params.Body = ($Body | ConvertTo-Json -Depth 30 -Compress)
	}

	try {
		$response = Invoke-WebRequest @params
		$bodyText = [string]$response.Content
		return [pscustomobject]@{
			StatusCode = [int]$response.StatusCode
			Body = $bodyText
			Json = Convert-ResponseJson $bodyText
			Uri = $params.Uri
		}
	} catch {
		$response = $_.Exception.Response
		if ($null -eq $response) {
			throw
		}
		$bodyText = Get-ErrorBody $_
		return [pscustomobject]@{
			StatusCode = [int]$response.StatusCode
			Body = $bodyText
			Json = Convert-ResponseJson $bodyText
			Uri = $params.Uri
		}
	}
}

function Require {
	param(
		[bool]$Condition,
		[string]$Message
	)
	if (-not $Condition) {
		throw $Message
	}
}

function Require-Property {
	param(
		[object]$Object,
		[string]$PropertyName
	)
	Require ($null -ne $Object) "Expected a JSON response."
	Require ($null -ne $Object.PSObject.Properties[$PropertyName]) "Missing property '$PropertyName'."
}

function Require-Any {
	param(
		[object[]]$Items,
		[scriptblock]$Predicate,
		[string]$Message
	)
	foreach ($item in @($Items)) {
		if (& $Predicate $item) {
			return
		}
	}
	throw $Message
}

function Test-Api {
	param(
		[string]$Name,
		[ValidateSet("GET", "POST", "PUT", "PATCH", "DELETE")]
		[string]$Method,
		[string]$Path,
		[int[]]$ExpectedStatus = @(200),
		[object]$Body = $null,
		[string]$Token = $null,
		[scriptblock]$Assert = $null
	)

	$response = Invoke-Api -Method $Method -Path $Path -Body $Body -Token $Token
	if (-not ($ExpectedStatus -contains $response.StatusCode)) {
		Write-Fail $Name "Expected HTTP $($ExpectedStatus -join '/') but got $($response.StatusCode) at $($response.Uri). Body: $(Limit-Text $response.Body)"
		throw "Stopping after failed API test: $Name"
	}

	try {
		if ($null -ne $Assert) {
			& $Assert $response.Json $response
		}
		Write-Pass $Name
	} catch {
		Write-Fail $Name $_.Exception.Message
		throw "Stopping after failed assertion: $Name"
	}
	return $response
}

function Register-TestUser {
	param(
		[string]$Prefix,
		[string[]]$Roles = @("READER")
	)

	$email = "$Prefix.$script:RunSuffix@plumora.test"
	$username = "${Prefix}_$script:RunSuffix"
	$password = "password123"

	$registerResponse = Test-Api `
		-Name "POST /auth/register ($email)" `
		-Method POST `
		-Path "/auth/register" `
		-ExpectedStatus 201 `
		-Body @{
			firstname = "Smoke"
			lastname = $Prefix
			username = $username
			email = $email
			password = $password
		} `
		-Assert {
			param($json)
			Require-Property $json "token"
			Require-Property $json "user"
			Require ($json.user.email -eq $email) "Registered email does not match."
		}

	$token = [string]$registerResponse.Json.token
	$user = $registerResponse.Json.user

	if ($Roles.Count -gt 0) {
		Test-Api `
			-Name "PUT /users/me/roles ($email)" `
			-Method PUT `
			-Path "/users/me/roles" `
			-Token $token `
			-Body @{ roles = $Roles } `
			-Assert {
				param($json)
				$actualRoles = @($json | ForEach-Object { $_.name })
				foreach ($role in $Roles) {
					Require ($actualRoles -contains $role) "Role '$role' was not applied."
				}
			} | Out-Null

		$userResponse = Test-Api `
			-Name "GET /users/me after role update ($email)" `
			-Method GET `
			-Path "/users/me" `
			-Token $token `
			-Assert {
				param($json)
				Require ($json.email -eq $email) "Current user email does not match."
			}
		$user = $userResponse.Json
	}

	return [pscustomobject]@{
		Email = $email
		Password = $password
		Token = $token
		User = $user
		Id = [string]$user.id
		Roles = $Roles
	}
}

function Login-User {
	param(
		[string]$Email,
		[string]$Password,
		[string]$Label
	)
	$response = Test-Api `
		-Name "POST /auth/login ($Label)" `
		-Method POST `
		-Path "/auth/login" `
		-Body @{
			email = $Email
			password = $Password
		} `
		-Assert {
			param($json)
			Require-Property $json "token"
			Require ($json.user.email -eq $Email.ToLowerInvariant()) "Logged user email does not match."
		}
	return [string]$response.Json.token
}

Write-Host "Plumora API smoke tests"
Write-Host "Base URL: $BaseUrl"
Write-Host "Run suffix: $script:RunSuffix"

Write-Section "Public and Security"
Test-Api -Name "GET /catalog/books is public" -Method GET -Path "/catalog/books?page=0&size=5" -Assert {
	param($json)
	Require-Property $json "content"
	Require-Property $json "totalElements"
} | Out-Null

Test-Api -Name "GET /uploads/book-covers/{filename} returns 404 for a missing cover" -Method GET -Path "/uploads/book-covers/missing-smoke-cover.png" -ExpectedStatus 404 -Assert {
	param($json)
	Require ($json.status -eq 404) "Missing cover should use the standard 404 error response."
} | Out-Null

Test-Api -Name "GET /users/me without token is blocked" -Method GET -Path "/users/me" -ExpectedStatus @(401, 403) | Out-Null

Write-Section "Auth and Users"
$author = Register-TestUser -Prefix "author" -Roles @("READER", "AUTHOR")
$reader = Register-TestUser -Prefix "reader" -Roles @("READER")
$progressReader = Register-TestUser -Prefix "progress" -Roles @("READER")
$betaReader = Register-TestUser -Prefix "beta" -Roles @("READER", "BETA_READER")
$refusingBetaReader = Register-TestUser -Prefix "refuse" -Roles @("READER", "BETA_READER")

$author.Token = Login-User -Email $author.Email -Password $author.Password -Label "author"

Test-Api -Name "GET /auth/me" -Method GET -Path "/auth/me" -Token $author.Token -Assert {
	param($json)
	Require ($json.email -eq $author.Email) "Auth profile email does not match."
} | Out-Null

Test-Api -Name "GET /users/me/roles" -Method GET -Path "/users/me/roles" -Token $author.Token -Assert {
	param($json)
	$roles = @($json | ForEach-Object { $_.name })
	Require ($roles -contains "AUTHOR") "AUTHOR role is missing."
} | Out-Null

Test-Api -Name "GET /roles" -Method GET -Path "/roles" -Token $author.Token -Assert {
	param($json)
	$roles = @($json | ForEach-Object { $_.name })
	foreach ($role in @("AUTHOR", "READER", "BETA_READER", "ADMIN")) {
		Require ($roles -contains $role) "Role '$role' is missing from /roles."
	}
} | Out-Null

Test-Api -Name "PUT /users/me" -Method PUT -Path "/users/me" -Token $reader.Token -Body @{
	firstname = "SmokeUpdated"
	lastname = "Reader"
	username = "reader_$script:RunSuffix"
	avatarUrl = "https://example.test/avatar.png"
	bio = "Updated by route smoke tests."
} -Assert {
	param($json)
	Require ($json.firstname -eq "SmokeUpdated") "Firstname was not updated."
	Require ($json.bio -eq "Updated by route smoke tests.") "Bio was not updated."
} | Out-Null

Test-Api -Name "PUT /users/me/roles rejects ADMIN self-assignment" -Method PUT -Path "/users/me/roles" -Token $reader.Token -ExpectedStatus 400 -Body @{
	roles = @("READER", "ADMIN")
} -Assert {
	param($json)
	Require ($json.status -eq 400) "Expected business error for ADMIN self-assignment."
} | Out-Null

Test-Api -Name "POST /auth/login rejects bad password" -Method POST -Path "/auth/login" -ExpectedStatus 401 -Body @{
	email = $author.Email
	password = "wrong-password"
} | Out-Null

Write-Section "Books and Chapters"
$bookTitle = "Smoke Routes $script:RunSuffix"
$publishedTitle = "$bookTitle Updated"
$bookBody = @{
	title = $bookTitle
	subtitle = "API route suite"
	summary = "Temporary book created by the Plumora API smoke tests."
	coverUrl = "https://example.test/covers/$script:RunSuffix.png"
	genre = "Fantasy"
	languageCode = "fr"
}

Test-Api -Name "POST /books without token is blocked" -Method POST -Path "/books" -ExpectedStatus @(401, 403) -Body $bookBody | Out-Null
Test-Api -Name "POST /books as READER is forbidden" -Method POST -Path "/books" -Token $reader.Token -ExpectedStatus 403 -Body $bookBody | Out-Null

$bookResponse = Test-Api -Name "POST /books" -Method POST -Path "/books" -Token $author.Token -ExpectedStatus 201 -Body $bookBody -Assert {
	param($json)
	Require ($json.status -eq "DRAFT") "New book should start as DRAFT."
	Require ($json.visibility -eq "PRIVATE") "New book should start as PRIVATE."
}
$bookId = [string]$bookResponse.Json.id

Test-Api -Name "GET /books/my-books" -Method GET -Path "/books/my-books" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $bookId } "Created book is missing from my books."
} | Out-Null

Test-Api -Name "GET /books/{bookId}" -Method GET -Path "/books/$bookId" -Token $author.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $bookId) "Book id does not match."
} | Out-Null

Test-Api -Name "PUT /books/{bookId}" -Method PUT -Path "/books/$bookId" -Token $author.Token -Body @{
	title = $publishedTitle
	subtitle = "API route suite updated"
	summary = "Updated summary from smoke tests."
	coverUrl = "https://example.test/covers/$script:RunSuffix-updated.png"
	genre = "Fantasy"
	languageCode = "fr"
} -Assert {
	param($json)
	Require ($json.title -eq $publishedTitle) "Book title was not updated."
} | Out-Null

Test-Api -Name "PATCH /books/{bookId}/publish rejects a book without chapters" -Method PATCH -Path "/books/$bookId/publish" -Token $author.Token -ExpectedStatus 400 | Out-Null

$chapterOne = Test-Api -Name "POST /books/{bookId}/chapters chapter 1" -Method POST -Path "/books/$bookId/chapters" -Token $author.Token -ExpectedStatus 201 -Body @{
	title = "Chapter One"
	content = "The city woke under quiet lanterns. The route tests followed every sign."
	chapterOrder = 1
} -Assert {
	param($json)
	Require ($json.chapterOrder -eq 1) "Chapter order should be 1."
}
$chapterOneId = [string]$chapterOne.Json.id

$chapterTwo = Test-Api -Name "POST /books/{bookId}/chapters chapter 2" -Method POST -Path "/books/$bookId/chapters" -Token $author.Token -ExpectedStatus 201 -Body @{
	title = "Chapter Two"
	content = "A second chapter exists only so the order and delete routes can be tested."
	chapterOrder = 2
}
$chapterTwoId = [string]$chapterTwo.Json.id

Test-Api -Name "POST /books/{bookId}/chapters rejects duplicate order" -Method POST -Path "/books/$bookId/chapters" -Token $author.Token -ExpectedStatus 400 -Body @{
	title = "Duplicate order"
	content = "This should not be accepted."
	chapterOrder = 1
} | Out-Null

Test-Api -Name "GET /books/{bookId}/chapters" -Method GET -Path "/books/$bookId/chapters" -Token $author.Token -Assert {
	param($json)
	Require (@($json).Count -ge 2) "Expected at least two chapters."
} | Out-Null

Test-Api -Name "GET /chapters/{chapterId}" -Method GET -Path "/chapters/$chapterOneId" -Token $author.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $chapterOneId) "Chapter id does not match."
} | Out-Null

$version = Test-Api -Name "POST /chapters/{chapterId}/versions" -Method POST -Path "/chapters/$chapterOneId/versions" -Token $author.Token -ExpectedStatus 201 -Assert {
	param($json)
	Require ($json.versionNumber -eq 1) "First chapter version should be number 1."
}
$versionId = [string]$version.Json.id

Test-Api -Name "PUT /chapters/{chapterId}" -Method PUT -Path "/chapters/$chapterOneId" -Token $author.Token -Body @{
	title = "Chapter One Revised"
	content = "The revised chapter makes the route suite verify version restoration."
} -Assert {
	param($json)
	Require ($json.title -eq "Chapter One Revised") "Chapter title was not updated."
} | Out-Null

Test-Api -Name "GET /chapters/{chapterId}/versions" -Method GET -Path "/chapters/$chapterOneId/versions" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $versionId } "Created chapter version is missing."
} | Out-Null

Test-Api -Name "GET /chapter-versions/{versionId}" -Method GET -Path "/chapter-versions/$versionId" -Token $author.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $versionId) "Version id does not match."
} | Out-Null

Test-Api -Name "POST /chapter-versions/{versionId}/restore" -Method POST -Path "/chapter-versions/$versionId/restore" -Token $author.Token -Assert {
	param($json)
	Require-Property $json "chapter"
	Require-Property $json "restoredVersion"
} | Out-Null

Test-Api -Name "PATCH /chapters/{chapterId}/order" -Method PATCH -Path "/chapters/$chapterTwoId/order" -Token $author.Token -Body @{
	chapterOrder = 3
} -Assert {
	param($json)
	Require ($json.chapterOrder -eq 3) "Chapter order was not updated."
} | Out-Null

Test-Api -Name "DELETE /chapters/{chapterId}" -Method DELETE -Path "/chapters/$chapterTwoId" -Token $author.Token -ExpectedStatus 204 | Out-Null

Test-Api -Name "PATCH /books/{bookId}/ready" -Method PATCH -Path "/books/$bookId/ready" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "READY_TO_PUBLISH") "Book should be READY_TO_PUBLISH."
} | Out-Null

Test-Api -Name "PATCH /books/{bookId}/publish" -Method PATCH -Path "/books/$bookId/publish" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "PUBLISHED") "Book should be PUBLISHED."
	Require ($json.visibility -eq "PUBLIC") "Book should be PUBLIC."
	Require ($null -ne $json.publishedAt) "Published date should be set."
} | Out-Null

$archiveBook = Test-Api -Name "POST /books for author archive route" -Method POST -Path "/books" -Token $author.Token -ExpectedStatus 201 -Body @{
	title = "Smoke Archive $script:RunSuffix"
	subtitle = "Archive route"
	summary = "Temporary archive route book."
	coverUrl = $null
	genre = "Fantasy"
	languageCode = "fr"
}
$archiveBookId = [string]$archiveBook.Json.id
Test-Api -Name "PATCH /books/{bookId}/archive" -Method PATCH -Path "/books/$archiveBookId/archive" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "ARCHIVED") "Book should be ARCHIVED."
} | Out-Null

$deleteBook = Test-Api -Name "POST /books for delete route" -Method POST -Path "/books" -Token $author.Token -ExpectedStatus 201 -Body @{
	title = "Smoke Delete $script:RunSuffix"
	subtitle = "Delete route"
	summary = "Temporary delete route book."
	coverUrl = $null
	genre = "Fantasy"
	languageCode = "fr"
}
$deleteBookId = [string]$deleteBook.Json.id
Test-Api -Name "DELETE /books/{bookId}" -Method DELETE -Path "/books/$deleteBookId" -Token $author.Token -ExpectedStatus 204 | Out-Null

Write-Section "Catalog"
$encodedTitle = [System.Uri]::EscapeDataString($publishedTitle)
Test-Api -Name "GET /catalog/books" -Method GET -Path "/catalog/books?page=0&size=100" -Assert {
	param($json)
	Require-Any @($json.content) { param($item) [string]$item.id -eq $bookId } "Published book is missing from catalog."
} | Out-Null

Test-Api -Name "GET /catalog/books/{bookId}" -Method GET -Path "/catalog/books/$bookId" -Assert {
	param($json)
	Require ([string]$json.id -eq $bookId) "Catalog detail id does not match."
	Require ($json.chapterCount -ge 1) "Catalog detail should expose chapter count."
} | Out-Null

Test-Api -Name "GET /catalog/books/search" -Method GET -Path "/catalog/books/search?q=$encodedTitle&page=0&size=20" -Assert {
	param($json)
	Require-Any @($json.content) { param($item) [string]$item.id -eq $bookId } "Search did not return the published book."
} | Out-Null

Test-Api -Name "GET /catalog/books/popular" -Method GET -Path "/catalog/books/popular?page=0&size=20" -Assert {
	param($json)
	Require-Property $json "content"
} | Out-Null

Test-Api -Name "GET /catalog/books/latest" -Method GET -Path "/catalog/books/latest?page=0&size=20" -Assert {
	param($json)
	Require-Property $json "content"
} | Out-Null

Test-Api -Name "GET /catalog/genres" -Method GET -Path "/catalog/genres" -Assert {
	param($json)
	Require (@($json) -contains "Fantasy") "Fantasy genre should be present."
} | Out-Null

Write-Section "Reading, Favorites and Reviews"
Test-Api -Name "GET /books/{bookId}/read" -Method GET -Path "/books/$bookId/read" -Token $reader.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $bookId) "Read book id does not match."
	Require (@($json.chapters).Count -ge 1) "Read session should include chapters."
	Require ($null -ne $json.progress) "Read session should include progress."
} | Out-Null

Test-Api -Name "GET /reading-progress/my" -Method GET -Path "/reading-progress/my" -Token $reader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.bookId -eq $bookId } "Reader progress is missing."
} | Out-Null

Test-Api -Name "GET /books/{bookId}/reading-progress" -Method GET -Path "/books/$bookId/reading-progress" -Token $reader.Token -Assert {
	param($json)
	Require ([string]$json.bookId -eq $bookId) "Book progress id does not match."
} | Out-Null

Test-Api -Name "POST /books/{bookId}/reading-progress" -Method POST -Path "/books/$bookId/reading-progress" -Token $progressReader.Token -ExpectedStatus 201 -Body @{
	currentChapterId = $chapterOneId
	progressPercentage = 25.5
} -Assert {
	param($json)
	Require ([string]$json.bookId -eq $bookId) "Created progress book id does not match."
} | Out-Null

Test-Api -Name "PUT /books/{bookId}/reading-progress" -Method PUT -Path "/books/$bookId/reading-progress" -Token $progressReader.Token -Body @{
	currentChapterId = $chapterOneId
	progressPercentage = 64.25
} -Assert {
	param($json)
	Require ([decimal]$json.progressPercentage -eq [decimal]64.25) "Progress percentage was not updated."
} | Out-Null

Test-Api -Name "PATCH /books/{bookId}/reading-progress/finish" -Method PATCH -Path "/books/$bookId/reading-progress/finish" -Token $progressReader.Token -Assert {
	param($json)
	Require ([decimal]$json.progressPercentage -eq [decimal]100.00) "Progress should be finished at 100%."
	Require ($null -ne $json.finishedAt) "Finished date should be set."
} | Out-Null

$favorite = Test-Api -Name "POST /books/{bookId}/favorites" -Method POST -Path "/books/$bookId/favorites" -Token $reader.Token -ExpectedStatus 201 -Assert {
	param($json)
	Require ([string]$json.bookId -eq $bookId) "Favorite book id does not match."
}

Test-Api -Name "GET /books/{bookId}/favorites/status true" -Method GET -Path "/books/$bookId/favorites/status" -Token $reader.Token -Assert {
	param($json)
	Require ($json.favorite -eq $true) "Book should be favorite."
} | Out-Null

Test-Api -Name "GET /favorites/my" -Method GET -Path "/favorites/my" -Token $reader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq [string]$favorite.Json.id } "Favorite is missing from my favorites."
} | Out-Null

Test-Api -Name "DELETE /books/{bookId}/favorites" -Method DELETE -Path "/books/$bookId/favorites" -Token $reader.Token -ExpectedStatus 204 | Out-Null

Test-Api -Name "GET /books/{bookId}/favorites/status false" -Method GET -Path "/books/$bookId/favorites/status" -Token $reader.Token -Assert {
	param($json)
	Require ($json.favorite -eq $false) "Book should no longer be favorite."
} | Out-Null

$review = Test-Api -Name "POST /books/{bookId}/reviews" -Method POST -Path "/books/$bookId/reviews" -Token $reader.Token -ExpectedStatus 201 -Body @{
	rating = 5
	comment = "Smoke test review."
} -Assert {
	param($json)
	Require ([string]$json.bookId -eq $bookId) "Review book id does not match."
	Require ($json.rating -eq 5) "Review rating does not match."
}
$reviewId = [string]$review.Json.id

Test-Api -Name "GET /books/{bookId}/reviews" -Method GET -Path "/books/$bookId/reviews" -Token $reader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $reviewId } "Review is missing from book reviews."
} | Out-Null

Test-Api -Name "GET /reviews/my" -Method GET -Path "/reviews/my" -Token $reader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $reviewId } "Review is missing from my reviews."
} | Out-Null

Test-Api -Name "PUT /reviews/{reviewId}" -Method PUT -Path "/reviews/$reviewId" -Token $reader.Token -Body @{
	rating = 4
	comment = "Smoke test review updated."
} -Assert {
	param($json)
	Require ($json.rating -eq 4) "Review rating was not updated."
} | Out-Null

Test-Api -Name "DELETE /reviews/{reviewId}" -Method DELETE -Path "/reviews/$reviewId" -Token $reader.Token -ExpectedStatus 204 | Out-Null

Write-Section "AI"
$writingSuggestion = Test-Api -Name "POST /ai/writing/suggestions" -Method POST -Path "/ai/writing/suggestions" -Token $author.Token -ExpectedStatus 201 -Body @{
	chapter_id = $chapterOneId
	selected_text = "The lantern was very very bright."
	context_text = "A short fantasy scene."
	action_type = "FIX_REPETITIONS"
} -Assert {
	param($json)
	Require ([string]$json.chapterId -eq $chapterOneId) "AI writing suggestion chapter id does not match."
	Require ($json.status -eq "PENDING") "AI writing suggestion should start as PENDING."
}
$suggestionId = [string]$writingSuggestion.Json.id
$writingRequestId = [string]$writingSuggestion.Json.requestId

Test-Api -Name "GET /ai/writing/requests" -Method GET -Path "/ai/writing/requests" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $writingRequestId } "AI writing request is missing."
} | Out-Null

Test-Api -Name "GET /ai/writing/requests/{requestId}" -Method GET -Path "/ai/writing/requests/$writingRequestId" -Token $author.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $writingRequestId) "AI writing request id does not match."
	Require (@($json.suggestions).Count -ge 1) "AI writing request should include suggestions."
} | Out-Null

Test-Api -Name "PATCH /ai/writing/suggestions/{suggestionId}/accept" -Method PATCH -Path "/ai/writing/suggestions/$suggestionId/accept" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "ACCEPTED") "Suggestion should be ACCEPTED."
} | Out-Null

Test-Api -Name "PATCH /ai/writing/suggestions/{suggestionId}/modify" -Method PATCH -Path "/ai/writing/suggestions/$suggestionId/modify" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "MODIFIED") "Suggestion should be MODIFIED."
} | Out-Null

Test-Api -Name "PATCH /ai/writing/suggestions/{suggestionId}/ignore" -Method PATCH -Path "/ai/writing/suggestions/$suggestionId/ignore" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "IGNORED") "Suggestion should be IGNORED."
} | Out-Null

$recommendation = Test-Api -Name "POST /ai/recommendations/books" -Method POST -Path "/ai/recommendations/books" -Token $reader.Token -ExpectedStatus 201 -Body @{
	query_text = "I want a fantasy book with lanterns and mystery."
	mood = "curious"
	preferred_duration = "short"
	preferred_genre = "Fantasy"
} -Assert {
	param($json)
	Require (@($json.recommendations).Count -ge 1) "AI recommendations should include at least one book."
}
$recommendationId = [string]$recommendation.Json.id

Test-Api -Name "GET /ai/recommendations/my-requests" -Method GET -Path "/ai/recommendations/my-requests" -Token $reader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $recommendationId } "AI recommendation request is missing."
} | Out-Null

Test-Api -Name "GET /ai/recommendations/requests/{requestId}" -Method GET -Path "/ai/recommendations/requests/$recommendationId" -Token $reader.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $recommendationId) "AI recommendation request id does not match."
} | Out-Null

Write-Section "Beta Reading and Notifications"
$campaign = Test-Api -Name "POST /books/{bookId}/beta-campaigns" -Method POST -Path "/books/$bookId/beta-campaigns" -Token $author.Token -ExpectedStatus 201 -Body @{
	title = "Smoke beta campaign $script:RunSuffix"
	instructions = "Focus on pacing and clarity."
	deadline = "2026-12-31"
} -Assert {
	param($json)
	Require ($json.status -eq "ACTIVE") "Campaign should be ACTIVE."
}
$campaignId = [string]$campaign.Json.id

Test-Api -Name "GET /books/{bookId}/beta-campaigns" -Method GET -Path "/books/$bookId/beta-campaigns" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $campaignId } "Campaign is missing from book campaigns."
} | Out-Null

Test-Api -Name "GET /beta-campaigns/{campaignId} as author" -Method GET -Path "/beta-campaigns/$campaignId" -Token $author.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $campaignId) "Campaign id does not match."
} | Out-Null

Test-Api -Name "PUT /beta-campaigns/{campaignId}/chapters" -Method PUT -Path "/beta-campaigns/$campaignId/chapters" -Token $author.Token -Body @{
	chapterIds = @($chapterOneId)
} -Assert {
	param($json)
	Require (@($json).Count -eq 1) "Exactly one chapter should be shared."
	Require ([string]@($json)[0].id -eq $chapterOneId) "Shared chapter id does not match."
} | Out-Null

Test-Api -Name "GET /beta-campaigns/{campaignId}/chapters as author" -Method GET -Path "/beta-campaigns/$campaignId/chapters" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $chapterOneId } "Shared chapter is missing."
} | Out-Null

$invitation = Test-Api -Name "POST /beta-campaigns/{campaignId}/invitations" -Method POST -Path "/beta-campaigns/$campaignId/invitations" -Token $author.Token -ExpectedStatus 201 -Body @{
	betaReaderId = $betaReader.Id
} -Assert {
	param($json)
	Require ($json.status -eq "PENDING") "Invitation should be PENDING."
	Require ([string]$json.betaReaderId -eq $betaReader.Id) "Invitation beta reader id does not match."
}
$invitationId = [string]$invitation.Json.id

Test-Api -Name "GET /beta-campaigns/{campaignId}/invitations" -Method GET -Path "/beta-campaigns/$campaignId/invitations" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $invitationId } "Invitation is missing from campaign invitations."
} | Out-Null

$betaNotifications = Test-Api -Name "GET /notifications/my for beta reader" -Method GET -Path "/notifications/my" -Token $betaReader.Token -Assert {
	param($json)
	Require (@($json).Count -ge 1) "Beta reader should have an invitation notification."
}
$betaNotificationId = [string]@($betaNotifications.Json)[0].id

Test-Api -Name "GET /notifications/unread-count" -Method GET -Path "/notifications/unread-count" -Token $betaReader.Token -Assert {
	param($json)
	Require ($json.unreadCount -ge 1) "Unread count should be at least 1."
} | Out-Null

Test-Api -Name "PATCH /notifications/{notificationId}/read" -Method PATCH -Path "/notifications/$betaNotificationId/read" -Token $betaReader.Token -Assert {
	param($json)
	Require ($json.is_read -eq $true) "Notification should be marked as read."
} | Out-Null

Test-Api -Name "PATCH /notifications/read-all" -Method PATCH -Path "/notifications/read-all" -Token $betaReader.Token -Assert {
	param($json)
	foreach ($notification in @($json)) {
		Require ($notification.is_read -eq $true) "All notifications should be read."
	}
} | Out-Null

Test-Api -Name "DELETE /notifications/{notificationId}" -Method DELETE -Path "/notifications/$betaNotificationId" -Token $betaReader.Token -ExpectedStatus 204 | Out-Null

Test-Api -Name "GET /beta-invitations/my-invitations" -Method GET -Path "/beta-invitations/my-invitations" -Token $betaReader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $invitationId } "Invitation is missing from beta reader invitations."
} | Out-Null

Test-Api -Name "PATCH /beta-invitations/{invitationId}/accept" -Method PATCH -Path "/beta-invitations/$invitationId/accept" -Token $betaReader.Token -Assert {
	param($json)
	Require ($json.status -eq "ACCEPTED") "Invitation should be ACCEPTED."
} | Out-Null

Test-Api -Name "GET /beta-campaigns/{campaignId} as beta reader" -Method GET -Path "/beta-campaigns/$campaignId" -Token $betaReader.Token -Assert {
	param($json)
	Require ([string]$json.id -eq $campaignId) "Beta reader campaign id does not match."
} | Out-Null

Test-Api -Name "GET /beta-campaigns/{campaignId}/chapters as beta reader" -Method GET -Path "/beta-campaigns/$campaignId/chapters" -Token $betaReader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $chapterOneId } "Beta reader cannot see shared chapter."
} | Out-Null

$betaComment = Test-Api -Name "POST /beta-comments" -Method POST -Path "/beta-comments" -Token $betaReader.Token -ExpectedStatus 201 -Body @{
	campaignId = $campaignId
	chapterId = $chapterOneId
	commentText = "This paragraph works, but the rhythm can be tighter."
	selectedText = "The city woke"
	positionStart = 0
	positionEnd = 14
	feedbackType = "PACING"
	priority = "HIGH"
} -Assert {
	param($json)
	Require ($json.status -eq "OPEN") "Beta comment should start as OPEN."
	Require ([string]$json.campaignId -eq $campaignId) "Beta comment campaign id does not match."
}
$betaCommentId = [string]$betaComment.Json.id

Test-Api -Name "GET /beta-campaigns/{campaignId}/comments as author" -Method GET -Path "/beta-campaigns/$campaignId/comments" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $betaCommentId } "Author cannot see beta comment."
} | Out-Null

Test-Api -Name "GET /beta-campaigns/{campaignId}/comments as beta reader" -Method GET -Path "/beta-campaigns/$campaignId/comments" -Token $betaReader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $betaCommentId } "Beta reader cannot see own beta comment."
} | Out-Null

Test-Api -Name "GET /books/{bookId}/beta-comments" -Method GET -Path "/books/$bookId/beta-comments" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $betaCommentId } "Book beta comment is missing."
} | Out-Null

Test-Api -Name "GET /chapters/{chapterId}/beta-comments" -Method GET -Path "/chapters/$chapterOneId/beta-comments" -Token $author.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $betaCommentId } "Chapter beta comment is missing."
} | Out-Null

Test-Api -Name "PATCH /beta-comments/{commentId}/status" -Method PATCH -Path "/beta-comments/$betaCommentId/status" -Token $author.Token -Body @{
	status = "RESOLVED"
} -Assert {
	param($json)
	Require ($json.status -eq "RESOLVED") "Beta comment should be RESOLVED."
} | Out-Null

Test-Api -Name "DELETE /beta-comments/{commentId}" -Method DELETE -Path "/beta-comments/$betaCommentId" -Token $betaReader.Token -ExpectedStatus 204 | Out-Null

Test-Api -Name "GET /notifications/my for author after beta comment" -Method GET -Path "/notifications/my" -Token $author.Token -Assert {
	param($json)
	Require (@($json).Count -ge 1) "Author should have a beta comment notification."
} | Out-Null

Test-Api -Name "PATCH /notifications/read-all for author" -Method PATCH -Path "/notifications/read-all" -Token $author.Token -Assert {
	param($json)
	foreach ($notification in @($json)) {
		Require ($notification.is_read -eq $true) "Author notifications should be read."
	}
} | Out-Null

$cancelCampaign = Test-Api -Name "POST /books/{bookId}/beta-campaigns for cancel/refuse" -Method POST -Path "/books/$bookId/beta-campaigns" -Token $author.Token -ExpectedStatus 201 -Body @{
	title = "Smoke beta cancel $script:RunSuffix"
	instructions = "This campaign covers refuse and cancel."
	deadline = "2026-12-31"
}
$cancelCampaignId = [string]$cancelCampaign.Json.id

$refuseInvitation = Test-Api -Name "POST /beta-campaigns/{campaignId}/invitations for refuse" -Method POST -Path "/beta-campaigns/$cancelCampaignId/invitations" -Token $author.Token -ExpectedStatus 201 -Body @{
	betaReaderId = $refusingBetaReader.Id
}
$refuseInvitationId = [string]$refuseInvitation.Json.id

Test-Api -Name "PATCH /beta-invitations/{invitationId}/refuse" -Method PATCH -Path "/beta-invitations/$refuseInvitationId/refuse" -Token $refusingBetaReader.Token -Assert {
	param($json)
	Require ($json.status -eq "REFUSED") "Invitation should be REFUSED."
} | Out-Null

Test-Api -Name "PATCH /beta-campaigns/{campaignId}/cancel" -Method PATCH -Path "/beta-campaigns/$cancelCampaignId/cancel" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "CANCELLED") "Campaign should be CANCELLED."
} | Out-Null

Test-Api -Name "PATCH /beta-campaigns/{campaignId}/close" -Method PATCH -Path "/beta-campaigns/$campaignId/close" -Token $author.Token -Assert {
	param($json)
	Require ($json.status -eq "CLOSED") "Campaign should be CLOSED."
} | Out-Null

Write-Section "Reports"
$report = Test-Api -Name "POST /books/{bookId}/reports" -Method POST -Path "/books/$bookId/reports" -Token $reader.Token -ExpectedStatus 201 -Body @{
	reason = "Smoke test report"
	description = "Created to verify report routes."
} -Assert {
	param($json)
	Require ($json.status -eq "OPEN") "Report should start as OPEN."
	Require ([string]$json.bookId -eq $bookId) "Report book id does not match."
}
$reportId = [string]$report.Json.id

Test-Api -Name "GET /reports/my" -Method GET -Path "/reports/my" -Token $reader.Token -Assert {
	param($json)
	Require-Any @($json) { param($item) [string]$item.id -eq $reportId } "Report is missing from my reports."
} | Out-Null

Write-Section "Admin"
if ($SkipAdmin) {
	Write-Skip "Admin route suite" "SkipAdmin was passed."
} else {
	$adminToken = Login-User -Email $AdminEmail -Password $AdminPassword -Label "admin"

	Test-Api -Name "GET /reports as admin" -Method GET -Path "/reports" -Token $adminToken -Assert {
		param($json)
		Require-Any @($json) { param($item) [string]$item.id -eq $reportId } "Report is missing from admin report list."
	} | Out-Null

	Test-Api -Name "PATCH /reports/{reportId}/status" -Method PATCH -Path "/reports/$reportId/status" -Token $adminToken -Body @{
		status = "IN_REVIEW"
	} -Assert {
		param($json)
		Require ($json.status -eq "IN_REVIEW") "Report should be IN_REVIEW."
	} | Out-Null

	Test-Api -Name "GET /admin/users" -Method GET -Path "/admin/users" -Token $adminToken -Assert {
		param($json)
		Require-Any @($json) { param($item) [string]$item.id -eq $reader.Id } "Reader is missing from admin users."
	} | Out-Null

	Test-Api -Name "PATCH /admin/users/{userId}/disable" -Method PATCH -Path "/admin/users/$($reader.Id)/disable" -Token $adminToken -Assert {
		param($json)
		Require ($json.active -eq $false) "User should be disabled."
	} | Out-Null

	Test-Api -Name "PATCH /admin/users/{userId}/enable" -Method PATCH -Path "/admin/users/$($reader.Id)/enable" -Token $adminToken -Assert {
		param($json)
		Require ($json.active -eq $true) "User should be enabled."
	} | Out-Null

	Test-Api -Name "GET /admin/books" -Method GET -Path "/admin/books" -Token $adminToken -Assert {
		param($json)
		Require-Any @($json) { param($item) [string]$item.id -eq $bookId } "Published book is missing from admin books."
	} | Out-Null

	Test-Api -Name "GET /admin/reports" -Method GET -Path "/admin/reports" -Token $adminToken -Assert {
		param($json)
		Require-Any @($json) { param($item) [string]$item.id -eq $reportId } "Report is missing from /admin/reports."
	} | Out-Null

	Test-Api -Name "PATCH /admin/books/{bookId}/archive" -Method PATCH -Path "/admin/books/$bookId/archive" -Token $adminToken -Assert {
		param($json)
		Require ($json.status -eq "ARCHIVED") "Book should be archived by admin."
		Require ($json.visibility -eq "PRIVATE") "Archived book should be private."
	} | Out-Null
}

Write-Host ""
Write-Host "Smoke test summary" -ForegroundColor Cyan
Write-Host "Passed : $script:Passed" -ForegroundColor Green
Write-Host "Skipped: $script:Skipped" -ForegroundColor Yellow
Write-Host "Failed : $script:Failed" -ForegroundColor $(if ($script:Failed -eq 0) { "Green" } else { "Red" })

if ($script:Failed -gt 0) {
	exit 1
}

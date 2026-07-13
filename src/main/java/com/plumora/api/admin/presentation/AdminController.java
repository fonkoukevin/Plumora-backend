package com.plumora.api.admin.presentation;

import com.plumora.api.admin.application.AdminAuditLogService;
import com.plumora.api.admin.application.AdminService;
import com.plumora.api.admin.domain.AdminAction;
import com.plumora.api.admin.domain.AdminBookType;
import com.plumora.api.admin.domain.AdminTargetType;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.presentation.BookMapper;
import com.plumora.api.book.presentation.BookResponse;
import com.plumora.api.report.presentation.ReportMapper;
import com.plumora.api.report.presentation.ReportResponse;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.UserStatus;
import com.plumora.api.user.presentation.UserMapper;
import com.plumora.api.user.presentation.UserResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminService adminService;
	private final AdminAuditLogService auditLogService;

	public AdminController(AdminService adminService, AdminAuditLogService auditLogService) {
		this.adminService = adminService;
		this.auditLogService = auditLogService;
	}

	@GetMapping("/dashboard")
	public AdminDashboardDto getDashboard() {
		return adminService.getDashboard();
	}

	@GetMapping("/users")
	public List<AdminUserListDto> getUsers(
		@RequestParam(required = false) String query,
		@RequestParam(required = false) RoleName role,
		@RequestParam(required = false) UserStatus status
	) {
		return adminService.getUsers(query, role, status)
			.stream()
			.map(AdminUserMapper::toListDto)
			.toList();
	}

	@GetMapping("/users/{userId}")
	public AdminUserDetailDto getUserDetail(@PathVariable UUID userId) {
		return AdminUserMapper.toDetailDto(adminService.getUserDetail(userId));
	}

	@PatchMapping("/users/{userId}/status")
	public UserResponse updateUserStatus(
		Principal principal,
		@PathVariable UUID userId,
		@Valid @RequestBody UpdateUserStatusRequest request
	) {
		return UserMapper.toResponse(adminService.updateUserStatus(principal.getName(), userId, request));
	}

	@PatchMapping("/users/{userId}/role")
	public UserResponse updateUserRole(
		Principal principal,
		@PathVariable UUID userId,
		@Valid @RequestBody UpdateUserRoleRequest request
	) {
		return UserMapper.toResponse(adminService.updateUserRoles(principal.getName(), userId, request));
	}

	@PatchMapping("/users/{userId}/disable")
	public UserResponse disableUser(Principal principal, @PathVariable UUID userId) {
		return UserMapper.toResponse(adminService.disableUser(principal.getName(), userId));
	}

	@PatchMapping("/users/{userId}/enable")
	public UserResponse enableUser(Principal principal, @PathVariable UUID userId) {
		return UserMapper.toResponse(adminService.enableUser(principal.getName(), userId));
	}

	@GetMapping("/books")
	public List<AdminBookListDto> getBooks(
		@RequestParam(required = false) String query,
		@RequestParam(required = false) AdminBookType type,
		@RequestParam(required = false) BookStatus status
	) {
		return adminService.getBooks(query, type, status)
			.stream()
			.map(book -> AdminBookMapper.toListDto(book, adminService.getReportsCount(book)))
			.toList();
	}

	@GetMapping("/books/{bookId}")
	public AdminBookDetailDto getBookDetail(@PathVariable UUID bookId) {
		return AdminBookMapper.toDetailDto(adminService.getBookDetail(bookId));
	}

	@PatchMapping("/books/{bookId}/status")
	public BookResponse updateBookStatus(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody UpdateBookStatusRequest request
	) {
		return BookMapper.toResponse(adminService.updateBookStatus(principal.getName(), bookId, request));
	}

	@PatchMapping("/books/{bookId}/metadata")
	public BookResponse updateBookMetadata(
		Principal principal,
		@PathVariable UUID bookId,
		@Valid @RequestBody UpdateBookMetadataRequest request
	) {
		return BookMapper.toResponse(adminService.updateBookMetadata(principal.getName(), bookId, request));
	}

	@PatchMapping("/books/{bookId}/archive")
	public BookResponse archiveBook(Principal principal, @PathVariable UUID bookId) {
		return BookMapper.toResponse(adminService.archiveBook(principal.getName(), bookId));
	}

	@DeleteMapping("/books/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeBook(Principal principal, @PathVariable UUID bookId) {
		adminService.archiveBook(principal.getName(), bookId);
	}

	@PostMapping("/books/import/gutendex/{gutendexId}")
	public AdminImportBookResponse importGutendexBook(Principal principal, @PathVariable int gutendexId) {
		return AdminBookMapper.toImportResponse(adminService.importGutendexBook(principal.getName(), gutendexId));
	}

	@GetMapping("/reports")
	public List<ReportResponse> getReports() {
		return adminService.getReports()
			.stream()
			.map(ReportMapper::toResponse)
			.toList();
	}

	@GetMapping("/audit-logs")
	public List<AdminActionLogDto> getAuditLogs(
		@RequestParam(required = false) AdminAction action,
		@RequestParam(required = false) UUID adminId,
		@RequestParam(required = false) AdminTargetType targetType,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
	) {
		return auditLogService.search(action, adminId, targetType, dateFrom, dateTo)
			.stream()
			.map(AdminAuditLogMapper::toResponse)
			.toList();
	}
}

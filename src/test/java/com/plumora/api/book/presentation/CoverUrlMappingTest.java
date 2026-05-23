package com.plumora.api.book.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plumora.api.ai.application.AiRecommendationService;
import com.plumora.api.ai.domain.AiRecommendationRequestEntity;
import com.plumora.api.ai.domain.AiRecommendationResult;
import com.plumora.api.ai.domain.AiSuggestionStatus;
import com.plumora.api.ai.domain.AiWritingActionType;
import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.ai.domain.AiWritingSuggestion;
import com.plumora.api.ai.presentation.AiRecommendationMapper;
import com.plumora.api.ai.presentation.AiWritingMapper;
import com.plumora.api.betaReading.domain.BetaCampaignStatus;
import com.plumora.api.betaReading.domain.BetaComment;
import com.plumora.api.betaReading.domain.BetaCommentFeedbackType;
import com.plumora.api.betaReading.domain.BetaCommentPriority;
import com.plumora.api.betaReading.domain.BetaCommentStatus;
import com.plumora.api.betaReading.domain.BetaInvitation;
import com.plumora.api.betaReading.domain.BetaInvitationStatus;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.betaReading.presentation.BetaReadingMapper;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.reading.application.ReadSession;
import com.plumora.api.reading.domain.Favorite;
import com.plumora.api.reading.domain.ReadingProgress;
import com.plumora.api.reading.domain.Review;
import com.plumora.api.reading.presentation.ReadingMapper;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.report.presentation.ReportMapper;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoverUrlMappingTest {

	private static final String COVER_URL = "https://placehold.co/600x900/263238/ffffff?text=Plumora";
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void bookAndCatalogResponsesIncludeCoverUrl() {
		Book book = book();

		assertThat(BookMapper.toResponse(book).coverUrl()).isEqualTo(COVER_URL);
		assertThat(BookMapper.toCatalogResponse(book).coverUrl()).isEqualTo(COVER_URL);
		assertThat(BookMapper.toCatalogDetailResponse(book, 3).coverUrl()).isEqualTo(COVER_URL);
	}

	@Test
	void createAndUpdateBookRequestsAcceptImageFieldAliases() throws Exception {
		String createPayload = """
			{
			  "title": "Livre avec image",
			  "summary": "Resume",
			  "imageUrl": "%s",
			  "genre": "Science-fiction"
			}
			""".formatted(COVER_URL);
		String updatePayload = """
			{
			  "title": "Livre avec image",
			  "summary": "Resume",
			  "cover_image_url": "%s",
			  "genre": "Science-fiction"
			}
			""".formatted(COVER_URL);

		CreateBookRequest createRequest = objectMapper.readValue(createPayload, CreateBookRequest.class);
		UpdateBookRequest updateRequest = objectMapper.readValue(updatePayload, UpdateBookRequest.class);

		assertThat(createRequest.coverUrl()).isEqualTo(COVER_URL);
		assertThat(updateRequest.coverUrl()).isEqualTo(COVER_URL);
	}

	@Test
	void readingResponsesIncludeBookCoverUrl() {
		Book book = book();
		User reader = user("reader@example.com");
		Chapter chapter = chapter(book);
		ReadingProgress progress = new ReadingProgress();
		progress.setId(UUID.randomUUID());
		progress.setBook(book);
		progress.setUser(reader);
		progress.setCurrentChapter(chapter);
		progress.setProgressPercentage(BigDecimal.valueOf(50));

		Favorite favorite = new Favorite();
		favorite.setId(UUID.randomUUID());
		favorite.setBook(book);
		favorite.setUser(reader);

		Review review = new Review();
		review.setId(UUID.randomUUID());
		review.setBook(book);
		review.setUser(reader);
		review.setRating(5);

		assertThat(ReadingMapper.toReadBookResponse(new ReadSession(book, List.of(chapter), progress)).coverUrl())
			.isEqualTo(COVER_URL);
		assertThat(ReadingMapper.toProgressResponse(progress).bookCoverUrl()).isEqualTo(COVER_URL);
		assertThat(ReadingMapper.toFavoriteResponse(favorite).bookCoverUrl()).isEqualTo(COVER_URL);
		assertThat(ReadingMapper.toReviewResponse(review).bookCoverUrl()).isEqualTo(COVER_URL);
	}

	@Test
	void betaReadingResponsesIncludeBookCoverUrl() {
		User betaReader = user("beta@example.com");
		BetaReadingCampaign campaign = campaign();
		Chapter chapter = chapter(campaign.getBook());

		BetaInvitation invitation = new BetaInvitation();
		invitation.setId(UUID.randomUUID());
		invitation.setCampaign(campaign);
		invitation.setBetaReader(betaReader);
		invitation.setStatus(BetaInvitationStatus.PENDING);

		BetaComment comment = new BetaComment();
		comment.setId(UUID.randomUUID());
		comment.setCampaign(campaign);
		comment.setChapter(chapter);
		comment.setBetaReader(betaReader);
		comment.setCommentText("Commentaire");
		comment.setFeedbackType(BetaCommentFeedbackType.PACING);
		comment.setPriority(BetaCommentPriority.MEDIUM);
		comment.setStatus(BetaCommentStatus.OPEN);

		assertThat(BetaReadingMapper.toCampaignResponse(campaign).bookCoverUrl()).isEqualTo(COVER_URL);
		assertThat(BetaReadingMapper.toInvitationResponse(invitation).bookCoverUrl()).isEqualTo(COVER_URL);
		assertThat(BetaReadingMapper.toCommentResponse(comment).bookCoverUrl()).isEqualTo(COVER_URL);
	}

	@Test
	void aiAndReportResponsesIncludeBookCoverUrl() {
		Book book = book();
		User reader = user("reader@example.com");
		Chapter chapter = chapter(book);

		AiRecommendationRequestEntity recommendationRequest = new AiRecommendationRequestEntity();
		recommendationRequest.setId(UUID.randomUUID());
		recommendationRequest.setUser(reader);
		recommendationRequest.setQueryText("fantasy");

		AiRecommendationResult recommendationResult = new AiRecommendationResult();
		recommendationResult.setId(UUID.randomUUID());
		recommendationResult.setRequest(recommendationRequest);
		recommendationResult.setBook(book);
		recommendationResult.setMatchScore(91);
		recommendationResult.setReasons(List.of("Genre correspondant."));
		recommendationResult.setRankPosition(1);

		AiWritingRequest writingRequest = new AiWritingRequest();
		writingRequest.setId(UUID.randomUUID());
		writingRequest.setUser(book.getAuthor());
		writingRequest.setChapter(chapter);
		writingRequest.setSelectedText("Texte");
		writingRequest.setActionType(AiWritingActionType.IMPROVE_STYLE);

		AiWritingSuggestion suggestion = new AiWritingSuggestion();
		suggestion.setId(UUID.randomUUID());
		suggestion.setRequest(writingRequest);
		suggestion.setSuggestionText("Suggestion");
		suggestion.setStatus(AiSuggestionStatus.PENDING);

		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setReporter(reader);
		report.setBook(book);
		report.setReason("Signalement");
		report.setStatus(ReportStatus.OPEN);

		var recommendationResponse = AiRecommendationMapper.toResponse(
			new AiRecommendationService.RecommendationBundle(recommendationRequest, List.of(recommendationResult))
		);

		assertThat(recommendationResponse.recommendations().getFirst().coverUrl()).isEqualTo(COVER_URL);
		assertThat(AiWritingMapper.toRequestResponse(writingRequest, List.of(suggestion)).bookCoverUrl()).isEqualTo(COVER_URL);
		assertThat(AiWritingMapper.toSuggestionResponse(suggestion).bookCoverUrl()).isEqualTo(COVER_URL);
		assertThat(ReportMapper.toResponse(report).bookCoverUrl()).isEqualTo(COVER_URL);
	}

	private static Book book() {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(user("author@example.com"));
		book.setTitle("Livre Plumora");
		book.setSubtitle("Couverture");
		book.setSummary("Un livre avec une image.");
		book.setCoverUrl(COVER_URL);
		book.setGenre("Fantasy");
		book.setLanguageCode("fr");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		book.setReadingCount(12);
		book.setAverageRating(BigDecimal.valueOf(4.5));
		return book;
	}

	private static Chapter chapter(Book book) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Chapitre 1");
		chapter.setContent("Contenu");
		chapter.setChapterOrder(1);
		return chapter;
	}

	private static BetaReadingCampaign campaign() {
		Book book = book();
		BetaReadingCampaign campaign = new BetaReadingCampaign();
		campaign.setId(UUID.randomUUID());
		campaign.setBook(book);
		campaign.setAuthor(book.getAuthor());
		campaign.setTitle("Campagne beta");
		campaign.setStatus(BetaCampaignStatus.ACTIVE);
		return campaign;
	}

	private static User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}

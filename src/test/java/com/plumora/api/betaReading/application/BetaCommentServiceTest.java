package com.plumora.api.betaReading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.betaReading.domain.BetaCampaignStatus;
import com.plumora.api.betaReading.domain.BetaComment;
import com.plumora.api.betaReading.domain.BetaCommentFeedbackType;
import com.plumora.api.betaReading.domain.BetaCommentPriority;
import com.plumora.api.betaReading.domain.BetaCommentStatus;
import com.plumora.api.betaReading.domain.BetaInvitation;
import com.plumora.api.betaReading.domain.BetaInvitationStatus;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.betaReading.infrastructure.BetaCommentRepository;
import com.plumora.api.betaReading.infrastructure.BetaInvitationRepository;
import com.plumora.api.betaReading.infrastructure.BetaReadingCampaignRepository;
import com.plumora.api.betaReading.infrastructure.BetaSharedChapterRepository;
import com.plumora.api.betaReading.presentation.CreateBetaCommentRequest;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.notification.application.NotificationService;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BetaCommentServiceTest {

	@Mock
	private BetaCommentRepository commentRepository;

	@Mock
	private BetaReadingCampaignRepository campaignRepository;

	@Mock
	private BetaInvitationRepository invitationRepository;

	@Mock
	private BetaSharedChapterRepository sharedChapterRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private BookService bookService;

	@Mock
	private UserService userService;

	@Mock
	private NotificationService notificationService;

	private BetaCommentService betaCommentService;

	@BeforeEach
	void setUp() {
		betaCommentService = new BetaCommentService(
			commentRepository,
			campaignRepository,
			invitationRepository,
			sharedChapterRepository,
			chapterRepository,
			bookService,
			userService,
			notificationService
		);
	}

	@Test
	void invitedBetaReaderCanComment() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);
		Chapter chapter = chapter(campaign.getBook());
		CreateBetaCommentRequest request = createRequest(campaign.getId(), chapter.getId());

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(invitationRepository.findByCampaignAndBetaReaderAndStatus(campaign, betaReader, BetaInvitationStatus.ACCEPTED))
			.thenReturn(Optional.of(invitation(campaign, betaReader)));
		when(chapterRepository.findByIdAndBook(chapter.getId(), campaign.getBook())).thenReturn(Optional.of(chapter));
		when(sharedChapterRepository.existsByCampaignAndChapter(campaign, chapter)).thenReturn(true);
		when(commentRepository.save(any(BetaComment.class))).thenAnswer(invocation -> {
			BetaComment comment = invocation.getArgument(0);
			comment.setId(UUID.randomUUID());
			return comment;
		});

		BetaComment comment = betaCommentService.createComment(betaReader.getEmail(), request);

		assertThat(comment.getCampaign()).isEqualTo(campaign);
		assertThat(comment.getChapter()).isEqualTo(chapter);
		assertThat(comment.getBetaReader()).isEqualTo(betaReader);
		assertThat(comment.getStatus()).isEqualTo(BetaCommentStatus.OPEN);
		verify(notificationService).createNotification(
			eq(author),
			eq("Nouveau commentaire beta"),
			any(String.class),
			eq(NotificationType.BETA_COMMENT)
		);
	}

	@Test
	void nonInvitedUserCannotComment() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);
		Chapter chapter = chapter(campaign.getBook());
		CreateBetaCommentRequest request = createRequest(campaign.getId(), chapter.getId());

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(invitationRepository.findByCampaignAndBetaReaderAndStatus(campaign, betaReader, BetaInvitationStatus.ACCEPTED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> betaCommentService.createComment(betaReader.getEmail(), request))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only accepted beta readers can comment in this campaign");
	}

	@Test
	void authorCanListCommentsOfOwnBook() {
		User author = user("author@example.com", RoleName.AUTHOR);
		Book book = book(author);
		BetaComment comment = comment(campaign(author), user("reader@example.com", RoleName.BETA_READER));

		when(bookService.getOwnedBook(author.getEmail(), book.getId())).thenReturn(book);
		when(commentRepository.findByBookOrderByCreatedAtDesc(book)).thenReturn(List.of(comment));

		List<BetaComment> comments = betaCommentService.getBookComments(author.getEmail(), book.getId());

		assertThat(comments).containsExactly(comment);
	}

	@Test
	void anotherAuthorCannotListComments() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User otherAuthor = user("other@example.com", RoleName.AUTHOR);
		Book book = book(author);

		when(bookService.getOwnedBook(otherAuthor.getEmail(), book.getId()))
			.thenThrow(new UnauthorizedActionException("Only the author can manage this book"));

		assertThatThrownBy(() -> betaCommentService.getBookComments(otherAuthor.getEmail(), book.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the author can manage this book");
	}

	private CreateBetaCommentRequest createRequest(UUID campaignId, UUID chapterId) {
		return new CreateBetaCommentRequest(
			campaignId,
			chapterId,
			"This scene needs a stronger emotional beat.",
			"selected text",
			10,
			23,
			BetaCommentFeedbackType.PACING,
			BetaCommentPriority.HIGH
		);
	}

	private BetaComment comment(BetaReadingCampaign campaign, User betaReader) {
		BetaComment comment = new BetaComment();
		comment.setId(UUID.randomUUID());
		comment.setCampaign(campaign);
		comment.setChapter(chapter(campaign.getBook()));
		comment.setBetaReader(betaReader);
		comment.setCommentText("Nice but slow.");
		comment.setFeedbackType(BetaCommentFeedbackType.PACING);
		comment.setPriority(BetaCommentPriority.MEDIUM);
		comment.setStatus(BetaCommentStatus.OPEN);
		comment.setCreatedAt(LocalDateTime.now());
		return comment;
	}

	private BetaInvitation invitation(BetaReadingCampaign campaign, User betaReader) {
		BetaInvitation invitation = new BetaInvitation();
		invitation.setId(UUID.randomUUID());
		invitation.setCampaign(campaign);
		invitation.setBetaReader(betaReader);
		invitation.setStatus(BetaInvitationStatus.ACCEPTED);
		return invitation;
	}

	private BetaReadingCampaign campaign(User author) {
		BetaReadingCampaign campaign = new BetaReadingCampaign();
		campaign.setId(UUID.randomUUID());
		campaign.setBook(book(author));
		campaign.setAuthor(author);
		campaign.setTitle("Beta campaign");
		campaign.setStatus(BetaCampaignStatus.ACTIVE);
		campaign.setCreatedAt(LocalDateTime.now());
		return campaign;
	}

	private Book book(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Beta Book");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.DRAFT);
		book.setVisibility(BookVisibility.PRIVATE);
		return book;
	}

	private Chapter chapter(Book book) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Chapter 1");
		chapter.setContent("Chapter content");
		chapter.setChapterOrder(1);
		return chapter;
	}

	private User user(String email, RoleName roleName) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setFirstname("Test");
		user.setLastname("User");
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		Role role = new Role(roleName, roleName.name());
		role.setId(UUID.randomUUID());
		user.setRoles(Set.of(role));
		return user;
	}
}

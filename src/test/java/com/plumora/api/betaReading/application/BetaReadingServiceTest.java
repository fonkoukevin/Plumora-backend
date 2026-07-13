package com.plumora.api.betaReading.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.betaReading.domain.BetaCampaignStatus;
import com.plumora.api.betaReading.domain.BetaChapterView;
import com.plumora.api.betaReading.domain.BetaInvitation;
import com.plumora.api.betaReading.domain.BetaInvitationStatus;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.betaReading.domain.BetaSharedChapter;
import com.plumora.api.betaReading.infrastructure.BetaChapterViewRepository;
import com.plumora.api.betaReading.infrastructure.BetaCommentRepository;
import com.plumora.api.betaReading.infrastructure.BetaInvitationRepository;
import com.plumora.api.betaReading.infrastructure.BetaReadingCampaignRepository;
import com.plumora.api.betaReading.infrastructure.BetaSharedChapterRepository;
import com.plumora.api.betaReading.presentation.CreateBetaCampaignRequest;
import com.plumora.api.betaReading.presentation.CreateBetaInvitationRequest;
import com.plumora.api.betaReading.presentation.UpdateSharedChaptersRequest;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.notification.application.NotificationService;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.Role;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.UserRepository;
import java.time.LocalDate;
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
class BetaReadingServiceTest {

	@Mock
	private BetaReadingCampaignRepository campaignRepository;

	@Mock
	private BetaInvitationRepository invitationRepository;

	@Mock
	private BetaSharedChapterRepository sharedChapterRepository;

	@Mock
	private BetaCommentRepository commentRepository;

	@Mock
	private BetaChapterViewRepository chapterViewRepository;

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private BookService bookService;

	@Mock
	private UserService userService;

	@Mock
	private NotificationService notificationService;

	private BetaReadingService betaReadingService;

	@BeforeEach
	void setUp() {
		betaReadingService = new BetaReadingService(
			campaignRepository,
			invitationRepository,
			sharedChapterRepository,
			commentRepository,
			chapterViewRepository,
			chapterRepository,
			userRepository,
			bookService,
			userService,
			notificationService
		);
	}

	@Test
	void createCampaignUsesOwnedBookAndStartsActive() {
		User author = user("author@example.com", RoleName.AUTHOR);
		Book book = book(author);
		CreateBetaCampaignRequest request = new CreateBetaCampaignRequest(
			"First beta",
			"Please focus on pacing.",
			LocalDate.now().plusDays(14)
		);

		when(bookService.getOwnedEditableBook(author.getEmail(), book.getId())).thenReturn(book);
		when(campaignRepository.save(any(BetaReadingCampaign.class))).thenAnswer(invocation -> {
			BetaReadingCampaign campaign = invocation.getArgument(0);
			campaign.setId(UUID.randomUUID());
			return campaign;
		});
		when(userRepository.findAllByRoles_Name(RoleName.BETA_READER)).thenReturn(List.of());

		BetaReadingCampaign campaign = betaReadingService.createCampaign(author.getEmail(), book.getId(), request);

		assertThat(campaign.getBook()).isEqualTo(book);
		assertThat(campaign.getAuthor()).isEqualTo(author);
		assertThat(campaign.getTitle()).isEqualTo("First beta");
		assertThat(campaign.getStatus()).isEqualTo(BetaCampaignStatus.ACTIVE);
		verify(bookService).startBetaReading(book);
	}

	@Test
	void createCampaignNotifiesAllBetaReadersExceptAuthor() {
		User author = user("author@example.com", RoleName.AUTHOR);
		Book book = book(author);
		User betaReaderOne = user("reader1@example.com", RoleName.BETA_READER);
		User betaReaderTwo = user("reader2@example.com", RoleName.BETA_READER);
		CreateBetaCampaignRequest request = new CreateBetaCampaignRequest("First beta", "Instructions", null);

		when(bookService.getOwnedEditableBook(author.getEmail(), book.getId())).thenReturn(book);
		when(campaignRepository.save(any(BetaReadingCampaign.class))).thenAnswer(invocation -> {
			BetaReadingCampaign campaign = invocation.getArgument(0);
			campaign.setId(UUID.randomUUID());
			return campaign;
		});
		when(userRepository.findAllByRoles_Name(RoleName.BETA_READER)).thenReturn(List.of(betaReaderOne, betaReaderTwo));

		betaReadingService.createCampaign(author.getEmail(), book.getId(), request);

		verify(notificationService).createNotification(
			eq(betaReaderOne),
			any(String.class),
			any(String.class),
			eq(NotificationType.BETA_CAMPAIGN_OPEN)
		);
		verify(notificationService).createNotification(
			eq(betaReaderTwo),
			any(String.class),
			any(String.class),
			eq(NotificationType.BETA_CAMPAIGN_OPEN)
		);
	}

	@Test
	void closeCampaignMovesBookToInCorrection() {
		User author = user("author@example.com", RoleName.AUTHOR);
		BetaReadingCampaign campaign = campaign(author);
		campaign.getBook().setStatus(BookStatus.IN_BETA_READING);

		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(campaignRepository.save(campaign)).thenReturn(campaign);

		BetaReadingCampaign closed = betaReadingService.closeCampaign(author.getEmail(), campaign.getId());

		assertThat(closed.getStatus()).isEqualTo(BetaCampaignStatus.CLOSED);
		assertThat(closed.getClosedAt()).isNotNull();
		verify(bookService).completeBetaReading(campaign.getBook());
	}

	@Test
	void cancelCampaignRevertsBookToDraft() {
		User author = user("author@example.com", RoleName.AUTHOR);
		BetaReadingCampaign campaign = campaign(author);
		campaign.getBook().setStatus(BookStatus.IN_BETA_READING);

		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(campaignRepository.save(campaign)).thenReturn(campaign);

		BetaReadingCampaign cancelled = betaReadingService.cancelCampaign(author.getEmail(), campaign.getId());

		assertThat(cancelled.getStatus()).isEqualTo(BetaCampaignStatus.CANCELLED);
		verify(bookService).cancelBetaReading(campaign.getBook());
	}

	@Test
	void closeCampaignRejectsCampaignThatIsNotActive() {
		User author = user("author@example.com", RoleName.AUTHOR);
		BetaReadingCampaign campaign = campaign(author);
		campaign.setStatus(BetaCampaignStatus.CLOSED);

		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));

		assertThatThrownBy(() -> betaReadingService.closeCampaign(author.getEmail(), campaign.getId()))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Only active beta-reading campaigns can be modified");
	}

	@Test
	void getOpenCampaignsReturnsActiveCampaignsForOtherUsers() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User otherReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);

		when(campaignRepository.findByStatusOrderByCreatedAtDesc(BetaCampaignStatus.ACTIVE)).thenReturn(List.of(campaign));

		List<BetaReadingCampaign> openCampaigns = betaReadingService.getOpenCampaigns(otherReader.getEmail());

		assertThat(openCampaigns).containsExactly(campaign);
	}

	@Test
	void getOpenCampaignsExcludesCampaignsAuthoredByCurrentUser() {
		User author = user("author@example.com", RoleName.AUTHOR);
		BetaReadingCampaign campaign = campaign(author);

		when(campaignRepository.findByStatusOrderByCreatedAtDesc(BetaCampaignStatus.ACTIVE)).thenReturn(List.of(campaign));

		List<BetaReadingCampaign> openCampaigns = betaReadingService.getOpenCampaigns(author.getEmail());

		assertThat(openCampaigns).isEmpty();
	}

	@Test
	void getCampaignAllowsAnyBetaReaderWithoutInvitation() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));

		BetaReadingCampaign result = betaReadingService.getCampaign(betaReader.getEmail(), campaign.getId());

		assertThat(result).isEqualTo(campaign);
	}

	@Test
	void getCampaignRejectsUserWithoutBetaReaderRole() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User plainReader = user("plain@example.com", RoleName.READER);
		BetaReadingCampaign campaign = campaign(author);

		when(userService.getCurrentUser(plainReader.getEmail())).thenReturn(plainReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));

		assertThatThrownBy(() -> betaReadingService.getCampaign(plainReader.getEmail(), campaign.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the author or beta readers can access this campaign");
	}

	@Test
	void createInvitationRejectsDuplicateInvitation() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);

		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(userRepository.findById(betaReader.getId())).thenReturn(Optional.of(betaReader));
		when(invitationRepository.existsByCampaignAndBetaReader(campaign, betaReader)).thenReturn(true);

		CreateBetaInvitationRequest request = new CreateBetaInvitationRequest(betaReader.getId());

		assertThatThrownBy(() -> betaReadingService.createInvitation(author.getEmail(), campaign.getId(), request))
			.isInstanceOf(DuplicateResourceException.class)
			.hasMessage("Beta reader is already invited to this campaign");
	}

	@Test
	void createInvitationCreatesBetaInvitationNotification() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);

		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(userRepository.findById(betaReader.getId())).thenReturn(Optional.of(betaReader));
		when(invitationRepository.existsByCampaignAndBetaReader(campaign, betaReader)).thenReturn(false);
		when(invitationRepository.save(any(BetaInvitation.class))).thenAnswer(invocation -> {
			BetaInvitation invitation = invocation.getArgument(0);
			invitation.setId(UUID.randomUUID());
			return invitation;
		});

		BetaInvitation invitation = betaReadingService.createInvitation(
			author.getEmail(),
			campaign.getId(),
			new CreateBetaInvitationRequest(betaReader.getId())
		);

		assertThat(invitation.getCampaign()).isEqualTo(campaign);
		assertThat(invitation.getBetaReader()).isEqualTo(betaReader);
		assertThat(invitation.getStatus()).isEqualTo(BetaInvitationStatus.PENDING);
		verify(notificationService).createNotification(
			eq(betaReader),
			eq("Invitation a une beta-lecture"),
			any(String.class),
			eq(NotificationType.BETA_INVITATION)
		);
	}

	@Test
	void acceptInvitationRequiresInvitedUserAndSetsRespondedAt() {
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaInvitation invitation = invitation(betaReader, BetaInvitationStatus.PENDING);

		when(invitationRepository.findByIdWithCampaignAndReader(invitation.getId())).thenReturn(Optional.of(invitation));
		when(invitationRepository.save(invitation)).thenReturn(invitation);

		BetaInvitation accepted = betaReadingService.acceptInvitation(betaReader.getEmail(), invitation.getId());

		assertThat(accepted.getStatus()).isEqualTo(BetaInvitationStatus.ACCEPTED);
		assertThat(accepted.getRespondedAt()).isNotNull();
	}

	@Test
	void acceptInvitationRejectsAnotherUser() {
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaInvitation invitation = invitation(betaReader, BetaInvitationStatus.PENDING);

		when(invitationRepository.findByIdWithCampaignAndReader(invitation.getId())).thenReturn(Optional.of(invitation));

		assertThatThrownBy(() -> betaReadingService.acceptInvitation("other@example.com", invitation.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the invited beta reader can respond to this invitation");
	}

	@Test
	void updateSharedChaptersRejectsChapterFromAnotherBook() {
		User author = user("author@example.com", RoleName.AUTHOR);
		BetaReadingCampaign campaign = campaign(author);
		UUID foreignChapterId = UUID.randomUUID();

		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(chapterRepository.findByIdAndBook(foreignChapterId, campaign.getBook())).thenReturn(Optional.empty());

		UpdateSharedChaptersRequest request = new UpdateSharedChaptersRequest(List.of(foreignChapterId));

		assertThatThrownBy(() -> betaReadingService.updateSharedChapters(author.getEmail(), campaign.getId(), request))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Shared chapters must belong to the campaign book");
	}

	@Test
	void getSharedChaptersRejectsUserWithoutBetaReaderRole() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User plainReader = user("plain@example.com", RoleName.READER);
		BetaReadingCampaign campaign = campaign(author);

		when(userService.getCurrentUser(plainReader.getEmail())).thenReturn(plainReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));

		assertThatThrownBy(() -> betaReadingService.getSharedChapters(plainReader.getEmail(), campaign.getId()))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only beta readers can access shared chapters");
	}

	@Test
	void getSharedChaptersReturnsSharedChaptersForAnyBetaReader() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);
		Chapter chapter = chapter(campaign.getBook());
		BetaSharedChapter sharedChapter = new BetaSharedChapter();
		sharedChapter.setCampaign(campaign);
		sharedChapter.setChapter(chapter);

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(sharedChapterRepository.findByCampaignOrderByChapterChapterOrderAsc(campaign)).thenReturn(List.of(sharedChapter));

		List<Chapter> chapters = betaReadingService.getSharedChapters(betaReader.getEmail(), campaign.getId());

		assertThat(chapters).containsExactly(chapter);
	}

	@Test
	void recordChapterViewSavesFirstViewOfSharedChapter() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);
		Chapter chapter = chapter(campaign.getBook());

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(chapterRepository.findByIdAndBook(chapter.getId(), campaign.getBook())).thenReturn(Optional.of(chapter));
		when(sharedChapterRepository.existsByCampaignAndChapter(campaign, chapter)).thenReturn(true);
		when(chapterViewRepository.existsByChapterAndBetaReader(chapter, betaReader)).thenReturn(false);

		betaReadingService.recordChapterView(betaReader.getEmail(), campaign.getId(), chapter.getId());

		verify(chapterViewRepository).save(any(BetaChapterView.class));
	}

	@Test
	void recordChapterViewIsIdempotent() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);
		Chapter chapter = chapter(campaign.getBook());

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(chapterRepository.findByIdAndBook(chapter.getId(), campaign.getBook())).thenReturn(Optional.of(chapter));
		when(sharedChapterRepository.existsByCampaignAndChapter(campaign, chapter)).thenReturn(true);
		when(chapterViewRepository.existsByChapterAndBetaReader(chapter, betaReader)).thenReturn(true);

		betaReadingService.recordChapterView(betaReader.getEmail(), campaign.getId(), chapter.getId());

		verify(chapterViewRepository, org.mockito.Mockito.never()).save(any(BetaChapterView.class));
	}

	@Test
	void recordChapterViewRejectsChapterNotSharedInCampaign() {
		User author = user("author@example.com", RoleName.AUTHOR);
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		BetaReadingCampaign campaign = campaign(author);
		Chapter chapter = chapter(campaign.getBook());

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(campaignRepository.findByIdWithBookAndAuthor(campaign.getId())).thenReturn(Optional.of(campaign));
		when(chapterRepository.findByIdAndBook(chapter.getId(), campaign.getBook())).thenReturn(Optional.of(chapter));
		when(sharedChapterRepository.existsByCampaignAndChapter(campaign, chapter)).thenReturn(false);

		assertThatThrownBy(() -> betaReadingService.recordChapterView(betaReader.getEmail(), campaign.getId(), chapter.getId()))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Beta chapter views can only target shared chapters");
	}

	@Test
	void getEngagedCampaignIdsMergesCommentedAndViewedCampaigns() {
		User betaReader = user("reader@example.com", RoleName.BETA_READER);
		UUID commentedCampaignId = UUID.randomUUID();
		UUID viewedCampaignId = UUID.randomUUID();
		List<UUID> campaignIds = List.of(commentedCampaignId, viewedCampaignId);

		when(userService.getCurrentUser(betaReader.getEmail())).thenReturn(betaReader);
		when(commentRepository.findCommentedCampaignIds(betaReader.getId(), campaignIds)).thenReturn(Set.of(commentedCampaignId));
		when(chapterViewRepository.findViewedCampaignIds(betaReader.getId(), campaignIds)).thenReturn(Set.of(viewedCampaignId));

		Set<UUID> engagedCampaignIds = betaReadingService.getEngagedCampaignIds(betaReader.getEmail(), campaignIds);

		assertThat(engagedCampaignIds).containsExactlyInAnyOrder(commentedCampaignId, viewedCampaignId);
	}

	@Test
	void getEngagedCampaignIdsReturnsEmptySetWithoutQueryingForEmptyInput() {
		User betaReader = user("reader@example.com", RoleName.BETA_READER);

		Set<UUID> engagedCampaignIds = betaReadingService.getEngagedCampaignIds(betaReader.getEmail(), List.of());

		assertThat(engagedCampaignIds).isEmpty();
	}

	private BetaInvitation invitation(User betaReader, BetaInvitationStatus status) {
		BetaInvitation invitation = new BetaInvitation();
		invitation.setId(UUID.randomUUID());
		invitation.setCampaign(campaign(user("author@example.com", RoleName.AUTHOR)));
		invitation.setBetaReader(betaReader);
		invitation.setStatus(status);
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

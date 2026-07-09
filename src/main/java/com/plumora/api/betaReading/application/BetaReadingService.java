package com.plumora.api.betaReading.application;

import com.plumora.api.betaReading.domain.BetaCampaignStatus;
import com.plumora.api.betaReading.domain.BetaInvitation;
import com.plumora.api.betaReading.domain.BetaInvitationStatus;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.betaReading.domain.BetaSharedChapter;
import com.plumora.api.betaReading.infrastructure.BetaInvitationRepository;
import com.plumora.api.betaReading.infrastructure.BetaReadingCampaignRepository;
import com.plumora.api.betaReading.infrastructure.BetaSharedChapterRepository;
import com.plumora.api.betaReading.presentation.CreateBetaCampaignRequest;
import com.plumora.api.betaReading.presentation.CreateBetaInvitationRequest;
import com.plumora.api.betaReading.presentation.UpdateSharedChaptersRequest;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.notification.application.NotificationService;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.DuplicateResourceException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BetaReadingService {

	private final BetaReadingCampaignRepository campaignRepository;
	private final BetaInvitationRepository invitationRepository;
	private final BetaSharedChapterRepository sharedChapterRepository;
	private final ChapterRepository chapterRepository;
	private final UserRepository userRepository;
	private final BookService bookService;
	private final UserService userService;
	private final NotificationService notificationService;

	public BetaReadingService(
		BetaReadingCampaignRepository campaignRepository,
		BetaInvitationRepository invitationRepository,
		BetaSharedChapterRepository sharedChapterRepository,
		ChapterRepository chapterRepository,
		UserRepository userRepository,
		BookService bookService,
		UserService userService,
		NotificationService notificationService
	) {
		this.campaignRepository = campaignRepository;
		this.invitationRepository = invitationRepository;
		this.sharedChapterRepository = sharedChapterRepository;
		this.chapterRepository = chapterRepository;
		this.userRepository = userRepository;
		this.bookService = bookService;
		this.userService = userService;
		this.notificationService = notificationService;
	}

	@Transactional
	public BetaReadingCampaign createCampaign(String currentUserEmail, UUID bookId, CreateBetaCampaignRequest request) {
		Book book = bookService.getOwnedEditableBook(currentUserEmail, bookId);

		BetaReadingCampaign campaign = new BetaReadingCampaign();
		campaign.setBook(book);
		campaign.setAuthor(book.getAuthor());
		campaign.setTitle(request.title());
		campaign.setInstructions(request.instructions());
		campaign.setDeadline(request.deadline());
		campaign.setStatus(BetaCampaignStatus.ACTIVE);
		BetaReadingCampaign savedCampaign = campaignRepository.save(campaign);

		notifyBetaReaders(savedCampaign);

		return savedCampaign;
	}

	@Transactional(readOnly = true)
	public List<BetaReadingCampaign> getOpenCampaigns() {
		return campaignRepository.findByStatusOrderByCreatedAtDesc(BetaCampaignStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public List<BetaReadingCampaign> getBookCampaigns(String currentUserEmail, UUID bookId) {
		Book book = bookService.getOwnedBook(currentUserEmail, bookId);
		return campaignRepository.findByBookOrderByCreatedAtDesc(book);
	}

	@Transactional(readOnly = true)
	public BetaReadingCampaign getCampaign(String currentUserEmail, UUID campaignId) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		BetaReadingCampaign campaign = findCampaign(campaignId);
		if (isCampaignAuthor(currentUserEmail, campaign) || currentUser.hasRole(RoleName.BETA_READER)) {
			return campaign;
		}
		throw new UnauthorizedActionException("Only the author or beta readers can access this campaign");
	}

	@Transactional
	public BetaReadingCampaign closeCampaign(String currentUserEmail, UUID campaignId) {
		BetaReadingCampaign campaign = getOwnedCampaign(currentUserEmail, campaignId);
		ensureActiveCampaign(campaign);
		campaign.setStatus(BetaCampaignStatus.CLOSED);
		campaign.setClosedAt(LocalDateTime.now());
		return campaignRepository.save(campaign);
	}

	@Transactional
	public BetaReadingCampaign cancelCampaign(String currentUserEmail, UUID campaignId) {
		BetaReadingCampaign campaign = getOwnedCampaign(currentUserEmail, campaignId);
		ensureActiveCampaign(campaign);
		campaign.setStatus(BetaCampaignStatus.CANCELLED);
		campaign.setClosedAt(LocalDateTime.now());
		return campaignRepository.save(campaign);
	}

	@Transactional
	public BetaInvitation createInvitation(String currentUserEmail, UUID campaignId, CreateBetaInvitationRequest request) {
		BetaReadingCampaign campaign = getOwnedCampaign(currentUserEmail, campaignId);
		ensureActiveCampaign(campaign);
		User betaReader = userRepository.findById(request.betaReaderId())
			.orElseThrow(() -> new ResourceNotFoundException("Beta reader was not found"));
		ensureBetaReaderRole(betaReader);
		if (invitationRepository.existsByCampaignAndBetaReader(campaign, betaReader)) {
			throw new DuplicateResourceException("Beta reader is already invited to this campaign");
		}

		BetaInvitation invitation = new BetaInvitation();
		invitation.setCampaign(campaign);
		invitation.setBetaReader(betaReader);
		invitation.setStatus(BetaInvitationStatus.PENDING);
		BetaInvitation savedInvitation = invitationRepository.save(invitation);

		notificationService.createNotification(
			betaReader,
			"Invitation a une beta-lecture",
			"You have been invited to beta-read \"" + campaign.getBook().getTitle() + "\".",
			NotificationType.BETA_INVITATION
		);

		return savedInvitation;
	}

	@Transactional(readOnly = true)
	public List<BetaInvitation> getCampaignInvitations(String currentUserEmail, UUID campaignId) {
		BetaReadingCampaign campaign = getOwnedCampaign(currentUserEmail, campaignId);
		return invitationRepository.findByCampaignOrderByInvitedAtDesc(campaign);
	}

	@Transactional(readOnly = true)
	public List<BetaInvitation> getMyInvitations(String currentUserEmail) {
		User betaReader = userService.getCurrentUser(currentUserEmail);
		return invitationRepository.findByBetaReaderOrderByInvitedAtDesc(betaReader);
	}

	@Transactional
	public BetaInvitation acceptInvitation(String currentUserEmail, UUID invitationId) {
		BetaInvitation invitation = getInvitation(invitationId);
		ensureInvitationReader(currentUserEmail, invitation);
		ensurePendingInvitation(invitation);
		invitation.setStatus(BetaInvitationStatus.ACCEPTED);
		invitation.setRespondedAt(LocalDateTime.now());
		return invitationRepository.save(invitation);
	}

	@Transactional
	public BetaInvitation refuseInvitation(String currentUserEmail, UUID invitationId) {
		BetaInvitation invitation = getInvitation(invitationId);
		ensureInvitationReader(currentUserEmail, invitation);
		ensurePendingInvitation(invitation);
		invitation.setStatus(BetaInvitationStatus.REFUSED);
		invitation.setRespondedAt(LocalDateTime.now());
		return invitationRepository.save(invitation);
	}

	@Transactional(readOnly = true)
	public List<Chapter> getSharedChapters(String currentUserEmail, UUID campaignId) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		BetaReadingCampaign campaign = findCampaign(campaignId);
		if (!isCampaignAuthor(currentUserEmail, campaign)) {
			ensureBetaReaderAccess(currentUser);
		}
		return sharedChapterRepository.findByCampaignOrderByChapterChapterOrderAsc(campaign)
			.stream()
			.map(BetaSharedChapter::getChapter)
			.toList();
	}

	@Transactional
	public List<Chapter> updateSharedChapters(String currentUserEmail, UUID campaignId, UpdateSharedChaptersRequest request) {
		BetaReadingCampaign campaign = getOwnedCampaign(currentUserEmail, campaignId);
		ensureActiveCampaign(campaign);

		Set<UUID> uniqueChapterIds = new LinkedHashSet<>(request.chapterIds());
		List<Chapter> chapters = uniqueChapterIds.stream()
			.map(chapterId -> findCampaignChapter(campaign.getBook(), chapterId))
			.toList();

		sharedChapterRepository.deleteByCampaign(campaign);
		List<BetaSharedChapter> sharedChapters = chapters.stream()
			.map(chapter -> {
				BetaSharedChapter sharedChapter = new BetaSharedChapter();
				sharedChapter.setCampaign(campaign);
				sharedChapter.setChapter(chapter);
				return sharedChapter;
			})
			.toList();
		sharedChapterRepository.saveAll(sharedChapters);
		return chapters;
	}

	private BetaReadingCampaign getOwnedCampaign(String currentUserEmail, UUID campaignId) {
		BetaReadingCampaign campaign = findCampaign(campaignId);
		if (!isCampaignAuthor(currentUserEmail, campaign)) {
			throw new UnauthorizedActionException("Only the book author can manage this beta-reading campaign");
		}
		return campaign;
	}

	private BetaReadingCampaign findCampaign(UUID campaignId) {
		return campaignRepository.findByIdWithBookAndAuthor(campaignId)
			.orElseThrow(() -> new ResourceNotFoundException("Beta-reading campaign was not found"));
	}

	private BetaInvitation getInvitation(UUID invitationId) {
		return invitationRepository.findByIdWithCampaignAndReader(invitationId)
			.orElseThrow(() -> new ResourceNotFoundException("Beta invitation was not found"));
	}

	private Chapter findCampaignChapter(Book campaignBook, UUID chapterId) {
		return chapterRepository.findByIdAndBook(chapterId, campaignBook)
			.orElseThrow(() -> new BusinessException("Shared chapters must belong to the campaign book"));
	}

	private boolean isCampaignAuthor(String currentUserEmail, BetaReadingCampaign campaign) {
		return campaign.getAuthor().getEmail().equals(currentUserEmail);
	}

	private void ensureBetaReaderAccess(User user) {
		if (!user.hasRole(RoleName.BETA_READER)) {
			throw new UnauthorizedActionException("Only beta readers can access shared chapters");
		}
	}

	private void notifyBetaReaders(BetaReadingCampaign campaign) {
		userRepository.findAllByRoles_Name(RoleName.BETA_READER).stream()
			.filter(betaReader -> !betaReader.getId().equals(campaign.getAuthor().getId()))
			.forEach(betaReader -> notificationService.createNotification(
				betaReader,
				"Nouvelle campagne de beta-lecture",
				campaign.getAuthor().getUsername() + " a ouvert une campagne de beta-lecture pour \"" + campaign.getBook().getTitle() + "\".",
				NotificationType.BETA_CAMPAIGN_OPEN
			));
	}

	private void ensureInvitationReader(String currentUserEmail, BetaInvitation invitation) {
		if (!invitation.getBetaReader().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the invited beta reader can respond to this invitation");
		}
	}

	private void ensureActiveCampaign(BetaReadingCampaign campaign) {
		if (campaign.getStatus() != BetaCampaignStatus.ACTIVE) {
			throw new BusinessException("Only active beta-reading campaigns can be modified");
		}
	}

	private void ensurePendingInvitation(BetaInvitation invitation) {
		if (invitation.getStatus() != BetaInvitationStatus.PENDING) {
			throw new BusinessException("Only pending beta invitations can be answered");
		}
	}

	private void ensureBetaReaderRole(User user) {
		if (!user.hasRole(RoleName.BETA_READER)) {
			throw new BusinessException("Invited user must have the BETA_READER role");
		}
	}
}

package com.plumora.api.betaReading.application;

import com.plumora.api.betaReading.domain.BetaComment;
import com.plumora.api.betaReading.domain.BetaCommentStatus;
import com.plumora.api.betaReading.domain.BetaInvitationStatus;
import com.plumora.api.betaReading.domain.BetaReadingCampaign;
import com.plumora.api.betaReading.infrastructure.BetaCommentRepository;
import com.plumora.api.betaReading.infrastructure.BetaInvitationRepository;
import com.plumora.api.betaReading.infrastructure.BetaReadingCampaignRepository;
import com.plumora.api.betaReading.infrastructure.BetaSharedChapterRepository;
import com.plumora.api.betaReading.presentation.CreateBetaCommentRequest;
import com.plumora.api.book.application.BookService;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.notification.application.NotificationService;
import com.plumora.api.notification.domain.NotificationType;
import com.plumora.api.shared.exception.BusinessException;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.application.UserService;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BetaCommentService {

	private final BetaCommentRepository commentRepository;
	private final BetaReadingCampaignRepository campaignRepository;
	private final BetaInvitationRepository invitationRepository;
	private final BetaSharedChapterRepository sharedChapterRepository;
	private final ChapterRepository chapterRepository;
	private final BookService bookService;
	private final UserService userService;
	private final NotificationService notificationService;

	public BetaCommentService(
		BetaCommentRepository commentRepository,
		BetaReadingCampaignRepository campaignRepository,
		BetaInvitationRepository invitationRepository,
		BetaSharedChapterRepository sharedChapterRepository,
		ChapterRepository chapterRepository,
		BookService bookService,
		UserService userService,
		NotificationService notificationService
	) {
		this.commentRepository = commentRepository;
		this.campaignRepository = campaignRepository;
		this.invitationRepository = invitationRepository;
		this.sharedChapterRepository = sharedChapterRepository;
		this.chapterRepository = chapterRepository;
		this.bookService = bookService;
		this.userService = userService;
		this.notificationService = notificationService;
	}

	@Transactional
	public BetaComment createComment(String currentUserEmail, CreateBetaCommentRequest request) {
		User betaReader = userService.getCurrentUser(currentUserEmail);
		BetaReadingCampaign campaign = findCampaign(request.campaignId());
		ensureAcceptedInvitation(betaReader, campaign);
		Chapter chapter = findCampaignChapter(campaign.getBook(), request.chapterId());
		ensureChapterIsShared(campaign, chapter);
		ensureValidPositions(request.positionStart(), request.positionEnd());

		BetaComment comment = new BetaComment();
		comment.setCampaign(campaign);
		comment.setChapter(chapter);
		comment.setBetaReader(betaReader);
		comment.setCommentText(request.commentText());
		comment.setSelectedText(request.selectedText());
		comment.setPositionStart(request.positionStart());
		comment.setPositionEnd(request.positionEnd());
		comment.setFeedbackType(request.feedbackType());
		comment.setPriority(request.priority());
		comment.setStatus(BetaCommentStatus.OPEN);
		BetaComment savedComment = commentRepository.save(comment);

		notificationService.createNotification(
			campaign.getAuthor(),
			"Nouveau commentaire beta",
			betaReader.getUsername() + " left a beta-reading comment on \"" + campaign.getBook().getTitle() + "\".",
			NotificationType.BETA_COMMENT_RECEIVED
		);

		return savedComment;
	}

	@Transactional(readOnly = true)
	public List<BetaComment> getCampaignComments(String currentUserEmail, UUID campaignId) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		BetaReadingCampaign campaign = findCampaign(campaignId);
		if (isCampaignAuthor(currentUserEmail, campaign)) {
			return commentRepository.findByCampaignOrderByCreatedAtDesc(campaign);
		}
		ensureAcceptedInvitation(currentUser, campaign);
		return commentRepository.findByCampaignAndBetaReaderOrderByCreatedAtDesc(campaign, currentUser);
	}

	@Transactional(readOnly = true)
	public List<BetaComment> getBookComments(String currentUserEmail, UUID bookId) {
		Book book = bookService.getOwnedBook(currentUserEmail, bookId);
		return commentRepository.findByBookOrderByCreatedAtDesc(book);
	}

	@Transactional(readOnly = true)
	public List<BetaComment> getChapterComments(String currentUserEmail, UUID chapterId) {
		User currentUser = userService.getCurrentUser(currentUserEmail);
		Chapter chapter = findChapter(chapterId);
		if (chapter.getBook().getAuthor().getEmail().equals(currentUserEmail)) {
			return commentRepository.findByChapterOrderByCreatedAtDesc(chapter);
		}
		return commentRepository.findByChapterAndBetaReaderOrderByCreatedAtDesc(chapter, currentUser);
	}

	@Transactional
	public BetaComment updateCommentStatus(String currentUserEmail, UUID commentId, BetaCommentStatus status) {
		BetaComment comment = findComment(commentId);
		ensureCommentAuthor(currentUserEmail, comment);
		comment.setStatus(status);
		return commentRepository.save(comment);
	}

	@Transactional
	public void deleteComment(String currentUserEmail, UUID commentId) {
		BetaComment comment = findComment(commentId);
		if (!comment.getBetaReader().getEmail().equals(currentUserEmail)) {
			throw new UnauthorizedActionException("Only the beta reader who created this comment can delete it");
		}
		commentRepository.delete(comment);
	}

	private BetaReadingCampaign findCampaign(UUID campaignId) {
		return campaignRepository.findByIdWithBookAndAuthor(campaignId)
			.orElseThrow(() -> new ResourceNotFoundException("Beta-reading campaign was not found"));
	}

	private BetaComment findComment(UUID commentId) {
		return commentRepository.findByIdWithDetails(commentId)
			.orElseThrow(() -> new ResourceNotFoundException("Beta comment was not found"));
	}

	private Chapter findChapter(UUID chapterId) {
		return chapterRepository.findById(chapterId)
			.orElseThrow(() -> new ResourceNotFoundException("Chapter was not found"));
	}

	private Chapter findCampaignChapter(Book campaignBook, UUID chapterId) {
		return chapterRepository.findByIdAndBook(chapterId, campaignBook)
			.orElseThrow(() -> new BusinessException("Commented chapter must belong to the campaign book"));
	}

	private void ensureAcceptedInvitation(User betaReader, BetaReadingCampaign campaign) {
		if (invitationRepository.findByCampaignAndBetaReaderAndStatus(campaign, betaReader, BetaInvitationStatus.ACCEPTED).isEmpty()) {
			throw new UnauthorizedActionException("Only accepted beta readers can comment in this campaign");
		}
	}

	private void ensureChapterIsShared(BetaReadingCampaign campaign, Chapter chapter) {
		if (!sharedChapterRepository.existsByCampaignAndChapter(campaign, chapter)) {
			throw new BusinessException("Beta comments can only target shared chapters");
		}
	}

	private void ensureValidPositions(Integer positionStart, Integer positionEnd) {
		if (positionStart != null && positionEnd != null && positionStart > positionEnd) {
			throw new BusinessException("positionStart must be less than or equal to positionEnd");
		}
	}

	private boolean isCampaignAuthor(String currentUserEmail, BetaReadingCampaign campaign) {
		return campaign.getAuthor().getEmail().equals(currentUserEmail);
	}

	private void ensureCommentAuthor(String currentUserEmail, BetaComment comment) {
		if (!isCampaignAuthor(currentUserEmail, comment.getCampaign())) {
			throw new UnauthorizedActionException("Only the book author can update beta comment status");
		}
	}
}

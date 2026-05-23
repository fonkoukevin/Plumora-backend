package com.plumora.api.admin.application;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.report.application.ReportService;
import com.plumora.api.report.domain.Report;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import com.plumora.api.user.domain.User;
import com.plumora.api.user.infrastructure.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

	private final UserRepository userRepository;
	private final BookRepository bookRepository;
	private final ReportService reportService;

	public AdminService(UserRepository userRepository, BookRepository bookRepository, ReportService reportService) {
		this.userRepository = userRepository;
		this.bookRepository = bookRepository;
		this.reportService = reportService;
	}

	@Transactional(readOnly = true)
	public List<User> getUsers() {
		return userRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional
	public User disableUser(UUID userId) {
		User user = findUser(userId);
		user.setActive(false);
		return userRepository.save(user);
	}

	@Transactional
	public User enableUser(UUID userId) {
		User user = findUser(userId);
		user.setActive(true);
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public List<Book> getBooks() {
		return bookRepository.findAllWithAuthorOrderByCreatedAtDesc();
	}

	@Transactional
	public Book archiveBook(UUID bookId) {
		Book book = findBook(bookId);
		book.setStatus(BookStatus.ARCHIVED);
		book.setVisibility(BookVisibility.PRIVATE);
		return bookRepository.save(book);
	}

	@Transactional(readOnly = true)
	public List<Report> getReports() {
		return reportService.getAllReports();
	}

	private User findUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new ResourceNotFoundException("User was not found"));
	}

	private Book findBook(UUID bookId) {
		return bookRepository.findByIdWithAuthor(bookId)
			.orElseThrow(() -> new ResourceNotFoundException("Book was not found"));
	}
}

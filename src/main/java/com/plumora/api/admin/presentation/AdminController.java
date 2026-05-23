package com.plumora.api.admin.presentation;

import com.plumora.api.admin.application.AdminService;
import com.plumora.api.book.presentation.BookMapper;
import com.plumora.api.book.presentation.BookResponse;
import com.plumora.api.report.presentation.ReportMapper;
import com.plumora.api.report.presentation.ReportResponse;
import com.plumora.api.user.presentation.UserMapper;
import com.plumora.api.user.presentation.UserResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/users")
	public List<UserResponse> getUsers() {
		return adminService.getUsers()
			.stream()
			.map(UserMapper::toResponse)
			.toList();
	}

	@PatchMapping("/users/{userId}/disable")
	public UserResponse disableUser(@PathVariable UUID userId) {
		return UserMapper.toResponse(adminService.disableUser(userId));
	}

	@PatchMapping("/users/{userId}/enable")
	public UserResponse enableUser(@PathVariable UUID userId) {
		return UserMapper.toResponse(adminService.enableUser(userId));
	}

	@GetMapping("/books")
	public List<BookResponse> getBooks() {
		return adminService.getBooks()
			.stream()
			.map(BookMapper::toResponse)
			.toList();
	}

	@PatchMapping("/books/{bookId}/archive")
	public BookResponse archiveBook(@PathVariable UUID bookId) {
		return BookMapper.toResponse(adminService.archiveBook(bookId));
	}

	@GetMapping("/reports")
	public List<ReportResponse> getReports() {
		return adminService.getReports()
			.stream()
			.map(ReportMapper::toResponse)
			.toList();
	}
}

package com.plumora.api.shared.application;

import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.shared.presentation.PlatformStatsResponse;
import com.plumora.api.user.domain.RoleName;
import com.plumora.api.user.infrastructure.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformStatsService {

	private final BookRepository bookRepository;
	private final UserRepository userRepository;

	public PlatformStatsService(BookRepository bookRepository, UserRepository userRepository) {
		this.bookRepository = bookRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public PlatformStatsResponse getPublicStats() {
		return new PlatformStatsResponse(
			bookRepository.countByStatus(BookStatus.PUBLISHED),
			userRepository.countByRoles_Name(RoleName.AUTHOR),
			userRepository.countByRoles_Name(RoleName.READER)
		);
	}
}

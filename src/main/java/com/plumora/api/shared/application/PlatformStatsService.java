package com.plumora.api.shared.application;

import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.shared.presentation.PlatformStatsResponse;
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

	// Semantics pinned in docs/api-contract.md ("Public stats") so the landing page never
	// claims more than a visitor can actually find on the platform:
	//   - totalBooks/totalAuthors: same PUBLISHED + PUBLIC + valid-cover filter as the catalog
	//     (countCatalogBooks/countDistinctCatalogAuthors mirror findCatalogBooks's WHERE
	//     clause), not every PUBLISHED book or every AUTHOR-role account regardless of whether
	//     they have ever actually published anything visible.
	//   - totalReaders: every active registered account (countByActiveTrue), not just accounts
	//     holding the READER role - mirrors activeUsers on the admin dashboard.
	@Transactional(readOnly = true)
	public PlatformStatsResponse getPublicStats() {
		return new PlatformStatsResponse(
			bookRepository.countCatalogBooks(BookStatus.PUBLISHED, BookVisibility.PUBLIC),
			bookRepository.countDistinctCatalogAuthors(BookStatus.PUBLISHED, BookVisibility.PUBLIC),
			userRepository.countByActiveTrue()
		);
	}
}

package com.plumora.api.reading.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.reading.domain.ReadingProgress;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, UUID> {
	@EntityGraph(attributePaths = {"book", "book.author", "currentChapter"})
	List<ReadingProgress> findByUserOrderByLastReadAtDescStartedAtDesc(User user);

	@EntityGraph(attributePaths = {"book", "book.author", "currentChapter"})
	Optional<ReadingProgress> findByUserAndBook(User user, Book book);

	boolean existsByUserAndBook(User user, Book book);
}

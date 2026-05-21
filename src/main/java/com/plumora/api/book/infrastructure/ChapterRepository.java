package com.plumora.api.book.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
	List<Chapter> findByBookOrderByChapterOrderAsc(Book book);

	Optional<Chapter> findByIdAndBook(UUID id, Book book);

	boolean existsByBookAndChapterOrder(Book book, int chapterOrder);

	long countByBook(Book book);
}

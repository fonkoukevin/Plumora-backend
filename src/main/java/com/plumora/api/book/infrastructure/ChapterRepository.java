package com.plumora.api.book.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {
	List<Chapter> findByBookOrderByChapterOrderAsc(Book book);

	Optional<Chapter> findByIdAndBook(UUID id, Book book);

	@EntityGraph(attributePaths = {"book", "book.author"})
	@Query("select c from Chapter c where c.id = :id")
	Optional<Chapter> findByIdWithBookAndAuthor(@Param("id") UUID id);

	boolean existsByBookAndChapterOrder(Book book, int chapterOrder);

	long countByBook(Book book);
}

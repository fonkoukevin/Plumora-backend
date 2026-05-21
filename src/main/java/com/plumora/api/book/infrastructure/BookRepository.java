package com.plumora.api.book.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, UUID> {
	List<Book> findByAuthorOrderByCreatedAtDesc(User author);

	@EntityGraph(attributePaths = "author")
	@Query("select b from Book b where b.id = :id")
	Optional<Book> findByIdWithAuthor(@Param("id") UUID id);
}

package com.plumora.api.reading.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.reading.domain.Review;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
	@EntityGraph(attributePaths = {"user", "book", "book.author"})
	List<Review> findByBookOrderByCreatedAtDesc(Book book);

	@EntityGraph(attributePaths = {"user", "book", "book.author"})
	List<Review> findByUserOrderByCreatedAtDesc(User user);

	List<Review> findByBook(Book book);
}

package com.plumora.api.book.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, UUID> {
	List<Book> findByAuthorOrderByCreatedAtDesc(User author);

	long countByStatus(BookStatus status);

	long countByExternalSourceIsNull();

	long countByExternalSourceIsNotNull();

	boolean existsByExternalSourceAndExternalId(ExternalBookSource externalSource, String externalId);

	@EntityGraph(attributePaths = "author")
	Optional<Book> findByExternalSourceAndExternalId(ExternalBookSource externalSource, String externalId);

	@EntityGraph(attributePaths = "author")
	@Query("select b from Book b order by b.createdAt desc")
	List<Book> findAllWithAuthorOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = "author")
	@Query("select b from Book b where b.id = :id")
	Optional<Book> findByIdWithAuthor(@Param("id") UUID id);

	@EntityGraph(attributePaths = "author")
	@Query("""
		select b from Book b
		where b.status = :status
			and b.visibility = :visibility
			and b.publishedAt is not null
			and b.coverUrl is not null
			and trim(b.coverUrl) <> ''
			and lower(b.coverUrl) not like '%placehold.co%'
			and lower(b.coverUrl) not like '%example.com%'
			and lower(b.coverUrl) not like '%example.org%'
			and lower(b.coverUrl) not like '%example.net%'
		""")
	Page<Book> findCatalogBooks(
		@Param("status") BookStatus status,
		@Param("visibility") BookVisibility visibility,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "author")
	@Query("""
		select b from Book b
		where b.status = :status
			and b.visibility = :visibility
			and b.publishedAt is not null
		""")
	List<Book> findPublishedPublicBooksForRecommendations(
		@Param("status") BookStatus status,
		@Param("visibility") BookVisibility visibility,
		Pageable pageable
	);

	@EntityGraph(attributePaths = "author")
	@Query("""
		select b from Book b
		where b.id = :id
			and b.status = :status
			and b.visibility = :visibility
			and b.publishedAt is not null
		""")
	Optional<Book> findCatalogBookById(
		@Param("id") UUID id,
		@Param("status") BookStatus status,
		@Param("visibility") BookVisibility visibility
	);

	@EntityGraph(attributePaths = "author")
	@Query("""
		select b from Book b
		join b.author a
		where b.status = :status
			and b.visibility = :visibility
			and b.publishedAt is not null
			and b.coverUrl is not null
			and trim(b.coverUrl) <> ''
			and lower(b.coverUrl) not like '%placehold.co%'
			and lower(b.coverUrl) not like '%example.com%'
			and lower(b.coverUrl) not like '%example.org%'
			and lower(b.coverUrl) not like '%example.net%'
			and (:genre is null or lower(b.genre) = :genre)
			and (
				:query is null
				or lower(b.title) like :query
				or lower(coalesce(b.summary, '')) like :query
				or lower(b.genre) like :query
				or lower(a.username) like :query
				or lower(coalesce(a.firstname, '')) like :query
				or lower(coalesce(a.lastname, '')) like :query
			)
		""")
	Page<Book> searchCatalogBooks(
		@Param("status") BookStatus status,
		@Param("visibility") BookVisibility visibility,
		@Param("query") String query,
		@Param("genre") String genre,
		Pageable pageable
	);

	@Query("""
		select distinct b.genre from Book b
		where b.status = :status
			and b.visibility = :visibility
			and b.publishedAt is not null
			and b.coverUrl is not null
			and trim(b.coverUrl) <> ''
			and lower(b.coverUrl) not like '%placehold.co%'
			and lower(b.coverUrl) not like '%example.com%'
			and lower(b.coverUrl) not like '%example.org%'
			and lower(b.coverUrl) not like '%example.net%'
		order by b.genre asc
		""")
	List<String> findCatalogGenres(
		@Param("status") BookStatus status,
		@Param("visibility") BookVisibility visibility
	);
}

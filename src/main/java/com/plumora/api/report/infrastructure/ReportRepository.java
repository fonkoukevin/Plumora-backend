package com.plumora.api.report.infrastructure;

import com.plumora.api.book.domain.Book;
import com.plumora.api.report.domain.Report;
import com.plumora.api.report.domain.ReportStatus;
import com.plumora.api.user.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, UUID> {

	long countByStatus(ReportStatus status);

	long countByReporter(User reporter);

	long countByBook(Book book);

	boolean existsByReporterAndBookAndStatus(User reporter, Book book, ReportStatus status);

	@EntityGraph(attributePaths = {"reporter", "book", "book.author"})
	List<Report> findByReporterOrderByCreatedAtDesc(User reporter);

	@EntityGraph(attributePaths = {"reporter", "book", "book.author"})
	List<Report> findAllByOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = {"reporter", "book", "book.author"})
	@Query("select r from Report r where r.id = :id")
	Optional<Report> findByIdWithReporterAndBook(@Param("id") UUID id);
}

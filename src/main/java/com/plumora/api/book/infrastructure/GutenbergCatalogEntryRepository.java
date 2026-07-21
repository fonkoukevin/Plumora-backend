package com.plumora.api.book.infrastructure;

import com.plumora.api.book.domain.GutenbergCatalogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GutenbergCatalogEntryRepository extends JpaRepository<GutenbergCatalogEntry, Integer> {

	// :query and :subject are expected to already be lowercased "%...%" LIKE patterns built by
	// the caller (see ExternalBookService), matching the convention used by BookRepository.
	@Query("""
		select e from GutenbergCatalogEntry e
		where (:subject is null
			or lower(coalesce(e.bookshelves, '')) like :subject
			or lower(coalesce(e.subjects, '')) like :subject
		)
		and (:query is null
			or lower(e.title) like :query
			or lower(coalesce(e.authors, '')) like :query
		)
		order by e.issuedDate desc nulls last, e.title asc
		""")
	Page<GutenbergCatalogEntry> search(
		@Param("query") String query,
		@Param("subject") String subject,
		Pageable pageable
	);
}

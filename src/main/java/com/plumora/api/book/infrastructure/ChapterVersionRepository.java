package com.plumora.api.book.infrastructure;

import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ChapterVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterVersionRepository extends JpaRepository<ChapterVersion, UUID> {
	List<ChapterVersion> findByChapterOrderByVersionNumberDesc(Chapter chapter);

	@Query("select coalesce(max(version.versionNumber), 0) from ChapterVersion version where version.chapter = :chapter")
	int findMaxVersionNumberByChapter(@Param("chapter") Chapter chapter);
}

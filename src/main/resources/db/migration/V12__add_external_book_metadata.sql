ALTER TABLE books
	ADD COLUMN external_source VARCHAR(40),
	ADD COLUMN external_id VARCHAR(100),
	ADD COLUMN external_authors JSONB,
	ADD COLUMN external_subjects JSONB,
	ADD COLUMN external_languages JSONB,
	ADD COLUMN source_url VARCHAR(500),
	ADD COLUMN read_url VARCHAR(1000),
	ADD COLUMN download_count INTEGER,
	ADD COLUMN formats_json JSONB;

CREATE UNIQUE INDEX uk_books_external_source_id
	ON books (external_source, external_id)
	WHERE external_source IS NOT NULL
		AND external_id IS NOT NULL;

CREATE INDEX idx_books_external_source
	ON books (external_source);

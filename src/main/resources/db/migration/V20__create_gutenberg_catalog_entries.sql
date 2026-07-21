-- Local mirror of Project Gutenberg's own official catalog (downloaded directly from
-- gutenberg.org, never from gutendex.com - see GutenbergCatalogSyncService). Lets book
-- discovery/search work entirely against our own database, and lets reading construct
-- gutenberg.org content URLs directly, without any live call to the third-party gutendex.com
-- API that is blocked by Cloudflare from the production VPS.
CREATE TABLE gutenberg_catalog_entries (
    gutenberg_id INTEGER PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    authors VARCHAR(500),
    language_code VARCHAR(10) NOT NULL,
    subjects TEXT,
    bookshelves TEXT,
    issued_date DATE,
    cover_url VARCHAR(500),
    content_url VARCHAR(500) NOT NULL,
    synced_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_gutenberg_catalog_entries_language_code ON gutenberg_catalog_entries(language_code);
CREATE INDEX idx_gutenberg_catalog_entries_title_lower ON gutenberg_catalog_entries(lower(title));
CREATE INDEX idx_gutenberg_catalog_entries_bookshelves_lower ON gutenberg_catalog_entries(lower(bookshelves));

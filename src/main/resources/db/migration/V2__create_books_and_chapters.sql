CREATE TABLE books (
    id_book UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users(id_user),
    title VARCHAR(150) NOT NULL,
    subtitle VARCHAR(200),
    summary TEXT,
    cover_url VARCHAR(500),
    genre VARCHAR(80) NOT NULL,
    language_code VARCHAR(10) DEFAULT 'fr',
    status VARCHAR(40) NOT NULL,
    visibility VARCHAR(40) NOT NULL,
    published_at TIMESTAMP,
    reading_count INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE chapters (
    id_chapter UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    content TEXT,
    chapter_order INTEGER NOT NULL,
    word_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    UNIQUE(book_id, chapter_order)
);

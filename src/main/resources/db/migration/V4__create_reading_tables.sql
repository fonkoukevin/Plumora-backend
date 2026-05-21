CREATE TABLE reading_progress (
    id_reading_progress UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
    current_chapter_id UUID REFERENCES chapters(id_chapter) ON DELETE SET NULL,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    started_at TIMESTAMP NOT NULL,
    last_read_at TIMESTAMP,
    finished_at TIMESTAMP,
    CONSTRAINT uk_reading_progress_user_book UNIQUE(user_id, book_id),
    CONSTRAINT chk_reading_progress_percentage CHECK(progress_percentage >= 0.00 AND progress_percentage <= 100.00)
);

CREATE TABLE favorites (
    id_favorite UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_favorites_user_book UNIQUE(user_id, book_id)
);

CREATE TABLE reviews (
    id_review UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_reviews_user_book UNIQUE(user_id, book_id),
    CONSTRAINT chk_reviews_rating CHECK(rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reading_progress_user_id ON reading_progress(user_id);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_reviews_book_id ON reviews(book_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);

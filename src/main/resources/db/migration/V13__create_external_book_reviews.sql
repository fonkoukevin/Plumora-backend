CREATE TABLE external_book_reviews (
	id_external_book_review UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
	external_source VARCHAR(40) NOT NULL,
	external_id VARCHAR(100) NOT NULL,
	rating INTEGER NOT NULL,
	comment TEXT,
	created_at TIMESTAMP NOT NULL,
	updated_at TIMESTAMP,
	CONSTRAINT chk_external_book_reviews_rating CHECK(rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_external_book_reviews_book
	ON external_book_reviews(external_source, external_id, created_at DESC);

CREATE INDEX idx_external_book_reviews_user_id
	ON external_book_reviews(user_id);

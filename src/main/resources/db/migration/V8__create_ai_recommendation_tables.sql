CREATE TABLE ai_recommendation_requests (
	id_ai_recommendation_request UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
	query_text TEXT NOT NULL,
	mood VARCHAR(40),
	preferred_duration VARCHAR(30),
	preferred_genre VARCHAR(80),
	created_at TIMESTAMP NOT NULL
);

CREATE TABLE ai_recommendation_results (
	id_ai_recommendation_result UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	request_id UUID NOT NULL REFERENCES ai_recommendation_requests(id_ai_recommendation_request) ON DELETE CASCADE,
	book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
	match_score INTEGER NOT NULL,
	reasons JSONB,
	rank_position INTEGER NOT NULL,
	UNIQUE(request_id, book_id),
	UNIQUE(request_id, rank_position)
);

CREATE INDEX idx_ai_recommendation_requests_user_id ON ai_recommendation_requests(user_id);
CREATE INDEX idx_ai_recommendation_requests_created_at ON ai_recommendation_requests(created_at DESC);
CREATE INDEX idx_ai_recommendation_results_request_id ON ai_recommendation_results(request_id);
CREATE INDEX idx_ai_recommendation_results_book_id ON ai_recommendation_results(book_id);

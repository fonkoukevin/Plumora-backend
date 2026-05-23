CREATE TABLE ai_writing_requests (
	id_ai_writing_request UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
	chapter_id UUID NOT NULL REFERENCES chapters(id_chapter) ON DELETE CASCADE,
	selected_text TEXT NOT NULL,
	context_text TEXT,
	action_type VARCHAR(50) NOT NULL,
	created_at TIMESTAMP NOT NULL
);

CREATE TABLE ai_writing_suggestions (
	id_ai_writing_suggestion UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	request_id UUID NOT NULL REFERENCES ai_writing_requests(id_ai_writing_request) ON DELETE CASCADE,
	suggestion_text TEXT NOT NULL,
	explanation TEXT,
	status VARCHAR(30) NOT NULL,
	created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ai_writing_requests_user_id ON ai_writing_requests(user_id);
CREATE INDEX idx_ai_writing_requests_chapter_id ON ai_writing_requests(chapter_id);
CREATE INDEX idx_ai_writing_requests_created_at ON ai_writing_requests(created_at DESC);
CREATE INDEX idx_ai_writing_suggestions_request_id ON ai_writing_suggestions(request_id);
CREATE INDEX idx_ai_writing_suggestions_status ON ai_writing_suggestions(status);

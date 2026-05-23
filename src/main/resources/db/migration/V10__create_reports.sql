CREATE TABLE reports (
    id_report UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
CREATE INDEX idx_reports_book_id ON reports(book_id);
CREATE INDEX idx_reports_status ON reports(status);

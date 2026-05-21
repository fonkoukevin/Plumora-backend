CREATE TABLE beta_comments (
    id_beta_comment UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES beta_reading_campaigns(id_beta_reading_campaign) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters(id_chapter) ON DELETE CASCADE,
    beta_reader_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    selected_text TEXT,
    position_start INTEGER,
    position_end INTEGER,
    feedback_type VARCHAR(40) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_beta_comments_positions CHECK (
        position_start IS NULL
        OR position_end IS NULL
        OR position_start <= position_end
    )
);

CREATE INDEX idx_beta_comments_campaign_id ON beta_comments(campaign_id);
CREATE INDEX idx_beta_comments_chapter_id ON beta_comments(chapter_id);
CREATE INDEX idx_beta_comments_beta_reader_id ON beta_comments(beta_reader_id);
CREATE INDEX idx_beta_comments_status ON beta_comments(status);

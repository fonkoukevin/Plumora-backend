CREATE TABLE beta_chapter_views (
    id_beta_chapter_view UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES beta_reading_campaigns(id_beta_reading_campaign) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters(id_chapter) ON DELETE CASCADE,
    beta_reader_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    viewed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_beta_chapter_views_chapter_reader UNIQUE (chapter_id, beta_reader_id)
);

CREATE INDEX idx_beta_chapter_views_campaign_id ON beta_chapter_views(campaign_id);
CREATE INDEX idx_beta_chapter_views_beta_reader_id ON beta_chapter_views(beta_reader_id);

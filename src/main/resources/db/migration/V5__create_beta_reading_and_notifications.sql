CREATE TABLE beta_reading_campaigns (
    id_beta_reading_campaign UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID NOT NULL REFERENCES books(id_book) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    instructions TEXT,
    deadline DATE,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP
);

CREATE TABLE beta_invitations (
    id_beta_invitation UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES beta_reading_campaigns(id_beta_reading_campaign) ON DELETE CASCADE,
    beta_reader_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    invited_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    CONSTRAINT uk_beta_invitations_campaign_reader UNIQUE(campaign_id, beta_reader_id)
);

CREATE TABLE beta_shared_chapters (
    id_beta_shared_chapter UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES beta_reading_campaigns(id_beta_reading_campaign) ON DELETE CASCADE,
    chapter_id UUID NOT NULL REFERENCES chapters(id_chapter) ON DELETE CASCADE,
    CONSTRAINT uk_beta_shared_chapters_campaign_chapter UNIQUE(campaign_id, chapter_id)
);

CREATE TABLE notifications (
    id_notification UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

CREATE INDEX idx_beta_reading_campaigns_book_id ON beta_reading_campaigns(book_id);
CREATE INDEX idx_beta_reading_campaigns_author_id ON beta_reading_campaigns(author_id);
CREATE INDEX idx_beta_invitations_campaign_id ON beta_invitations(campaign_id);
CREATE INDEX idx_beta_invitations_beta_reader_id ON beta_invitations(beta_reader_id);
CREATE INDEX idx_beta_shared_chapters_campaign_id ON beta_shared_chapters(campaign_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);

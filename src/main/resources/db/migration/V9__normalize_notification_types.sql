UPDATE notifications
SET type = 'BETA_COMMENT_RECEIVED'
WHERE type = 'BETA_COMMENT';

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);

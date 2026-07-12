CREATE TABLE admin_audit_logs (
    id_admin_audit_log UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    admin_email VARCHAR(150) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID,
    description VARCHAR(2000),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_admin_audit_logs_admin_id ON admin_audit_logs(admin_id);
CREATE INDEX idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX idx_admin_audit_logs_target_type ON admin_audit_logs(target_type);
CREATE INDEX idx_admin_audit_logs_created_at ON admin_audit_logs(created_at);

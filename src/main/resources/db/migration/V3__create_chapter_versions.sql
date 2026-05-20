CREATE TABLE chapter_versions (
    id_chapter_version UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id UUID NOT NULL REFERENCES chapters(id_chapter) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES users(id_user),
    version_number INTEGER NOT NULL,
    content_snapshot TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(chapter_id, version_number)
);

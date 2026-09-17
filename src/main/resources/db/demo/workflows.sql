-- Deterministic identifiers and insert-only fixtures preserve jury interactions on restart.
INSERT INTO beta_reading_campaigns
    (id_beta_reading_campaign, book_id, author_id, title, instructions, status, created_at)
VALUES ('60000000-0000-4000-8000-000000000001',
    '20000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001',
    'Cartographie des Silences - lecture de demonstration',
    'Donnez votre avis sur le rythme et la clarte du premier chapitre.', 'ACTIVE', now())
ON CONFLICT DO NOTHING;

INSERT INTO beta_shared_chapters (id_beta_shared_chapter, campaign_id, chapter_id)
VALUES ('61000000-0000-4000-8000-000000000001',
    '60000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000009')
ON CONFLICT DO NOTHING;

INSERT INTO beta_invitations
    (id_beta_invitation, campaign_id, beta_reader_id, status, invited_at, responded_at)
VALUES ('62000000-0000-4000-8000-000000000001',
    '60000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000004',
    'ACCEPTED', now(), now()),
    ('62000000-0000-4000-8000-000000000002',
    '60000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000005',
    'PENDING', now(), null)
ON CONFLICT DO NOTHING;

INSERT INTO beta_comments
    (id_beta_comment, campaign_id, chapter_id, beta_reader_id, comment_text,
     selected_text, feedback_type, priority, status, created_at)
VALUES ('63000000-0000-4000-8000-000000000001',
    '60000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000009',
    '10000000-0000-4000-8000-000000000004',
    'Une ouverture intrigante. Preciser la reaction physique d Ines renforcerait la tension.',
    'Les radios se turent ensemble.', 'STYLE', 'MEDIUM', 'OPEN', now())
ON CONFLICT DO NOTHING;

INSERT INTO notifications (id_notification, user_id, title, message, type, is_read, created_at)
VALUES ('64000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001',
    'Un retour de beta-lecture', 'Noah a commente Zone blanche.', 'BETA_COMMENT_RECEIVED', false, now()),
    ('64000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000005',
    'Invitation a une beta-lecture', 'Alice vous invite a lire Cartographie des Silences.',
    'BETA_INVITATION', false, now())
ON CONFLICT DO NOTHING;

INSERT INTO ai_writing_requests
    (id_ai_writing_request, user_id, chapter_id, selected_text, context_text, action_type, created_at)
VALUES ('65000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001',
    '30000000-0000-4000-8000-000000000009', 'Les radios se turent ensemble.',
    'Ouverture du chapitre Zone blanche.', 'IMPROVE_STYLE', now())
ON CONFLICT DO NOTHING;

INSERT INTO ai_writing_suggestions
    (id_ai_writing_suggestion, request_id, suggestion_text, explanation, status, created_at)
VALUES ('66000000-0000-4000-8000-000000000001', '65000000-0000-4000-8000-000000000001',
    'Toutes les radios se turent au meme instant, laissant Ines seule face au silence.',
    'Exemple de demonstration ecrit a l avance, sans appel a un fournisseur IA.', 'PENDING', now())
ON CONFLICT DO NOTHING;

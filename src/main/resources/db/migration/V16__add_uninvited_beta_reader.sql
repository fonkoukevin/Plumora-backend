-- Third beta-reader demo account, seeded without any beta invitation.
-- Used to verify that any user with the BETA_READER role can discover,
-- read and comment on active campaigns without being individually invited.

INSERT INTO users (id_user, firstname, lastname, username, email, password_hash, avatar_url, bio, is_active, created_at, updated_at)
VALUES (
	'10000000-0000-4000-8000-000000000009',
	'Theo',
	'Rimbaud',
	'theo_beta',
	'theo.beta@plumora.test',
	'$2a$10$hitz6ikg/grYTEyhlMlzXe2tPujxA2vHpFKU8v14gcpEWVPZrtlkm',
	'https://ui-avatars.com/api/?background=56309a&color=fff&name=Theo+Rimbaud',
	'Beta-lecteur jamais invite, utilise pour verifier l''acces ouvert aux campagnes actives.',
	TRUE,
	now() - interval '5 days',
	now()
)
ON CONFLICT (email) DO UPDATE SET
	firstname = EXCLUDED.firstname,
	lastname = EXCLUDED.lastname,
	username = EXCLUDED.username,
	password_hash = EXCLUDED.password_hash,
	avatar_url = EXCLUDED.avatar_url,
	bio = EXCLUDED.bio,
	is_active = TRUE,
	updated_at = now();

WITH assignments(email, role_name) AS (
	VALUES
		('theo.beta@plumora.test', 'READER'),
		('theo.beta@plumora.test', 'BETA_READER')
)
INSERT INTO user_roles (id_user, id_role, assigned_at)
SELECT u.id_user, r.id_role, now() - interval '5 days'
FROM assignments a
JOIN users u ON u.email = a.email
JOIN roles r ON r.name = a.role_name
ON CONFLICT (id_user, id_role) DO NOTHING;

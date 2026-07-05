-- Replace network-dependent demo covers with bundled Plumora cover images.
-- V14 may already be applied in local databases, so this is a forward-only update.

UPDATE books
SET cover_url = CASE id_book
	WHEN '20000000-0000-4000-8000-000000000001'::uuid THEN 'uploads/book-covers/plumora-cendrelune.png'
	WHEN '20000000-0000-4000-8000-000000000002'::uuid THEN 'uploads/book-covers/plumora-orchestre.png'
	WHEN '20000000-0000-4000-8000-000000000003'::uuid THEN 'uploads/book-covers/plumora-station-minuit.png'
	WHEN '20000000-0000-4000-8000-000000000004'::uuid THEN 'uploads/book-covers/plumora-jardin-versions.png'
	WHEN '20000000-0000-4000-8000-000000000005'::uuid THEN 'uploads/book-covers/plumora-silences.png'
	WHEN '20000000-0000-4000-8000-000000000006'::uuid THEN 'uploads/book-covers/plumora-atlas-lucioles.png'
	WHEN '20000000-0000-4000-8000-000000000007'::uuid THEN 'uploads/book-covers/plumora-mer-sans-sommeil.png'
	WHEN '20000000-0000-4000-8000-000000000008'::uuid THEN 'uploads/book-covers/plumora-archives-verre.png'
	WHEN '20000000-0000-4000-8000-000000000009'::uuid THEN 'uploads/book-covers/plumora-lumen.png'
	WHEN '20000000-0000-4000-8000-000000000010'::uuid THEN 'uploads/book-covers/plumora-toits-nacre.png'
	WHEN '20000000-0000-4000-8000-000000000011'::uuid THEN 'uploads/book-covers/plumora-etoiles-perdues.png'
	WHEN '20000000-0000-4000-8000-000000000012'::uuid THEN 'uploads/book-covers/plumora-carnets-nacre.png'
	ELSE cover_url
END,
updated_at = now()
WHERE id_book IN (
	'20000000-0000-4000-8000-000000000001'::uuid,
	'20000000-0000-4000-8000-000000000002'::uuid,
	'20000000-0000-4000-8000-000000000003'::uuid,
	'20000000-0000-4000-8000-000000000004'::uuid,
	'20000000-0000-4000-8000-000000000005'::uuid,
	'20000000-0000-4000-8000-000000000006'::uuid,
	'20000000-0000-4000-8000-000000000007'::uuid,
	'20000000-0000-4000-8000-000000000008'::uuid,
	'20000000-0000-4000-8000-000000000009'::uuid,
	'20000000-0000-4000-8000-000000000010'::uuid,
	'20000000-0000-4000-8000-000000000011'::uuid,
	'20000000-0000-4000-8000-000000000012'::uuid
);

-- Expand the internal Plumora discovery catalog with additional platform authors.
-- All books are original demo content and use bundled local cover artwork.

WITH plumora_authors(id_user, firstname, lastname, username, email, bio) AS (
	VALUES
		('10000000-0000-4000-8000-000000000010'::uuid, 'Yasmine', 'Diallo', 'yasmine_diallo', 'yasmine.diallo@plumora.test', 'Autrice de romans historiques et de drames familiaux.'),
		('10000000-0000-4000-8000-000000000011'::uuid, 'Gabriel', 'Costa', 'gabriel_costa', 'gabriel.costa@plumora.test', 'Auteur de science-fiction contemplative et de récits d exploration.'),
		('10000000-0000-4000-8000-000000000012'::uuid, 'Camille', 'Derenne', 'camille_derenne', 'camille.derenne@plumora.test', 'Autrice de romans contemporains chaleureux et sensibles.'),
		('10000000-0000-4000-8000-000000000013'::uuid, 'Idriss', 'Ndao', 'idriss_ndao', 'idriss.ndao@plumora.test', 'Auteur de polars urbains et de suspense fantastique.'),
		('10000000-0000-4000-8000-000000000014'::uuid, 'Solène', 'Armand', 'solene_armand', 'solene.armand@plumora.test', 'Autrice de fantasy lumineuse pour jeunes lecteurs et grands rêveurs.')
)
INSERT INTO users (
	id_user, firstname, lastname, username, email, password_hash, avatar_url, bio,
	is_active, created_at, updated_at
)
SELECT
	id_user,
	firstname,
	lastname,
	username,
	email,
	'$2a$10$hitz6ikg/grYTEyhlMlzXe2tPujxA2vHpFKU8v14gcpEWVPZrtlkm',
	'https://ui-avatars.com/api/?background=56309a&color=fff&name=' || firstname || '+' || lastname,
	bio,
	TRUE,
	now() - interval '90 days',
	now()
FROM plumora_authors
ON CONFLICT (id_user) DO UPDATE SET
	firstname = EXCLUDED.firstname,
	lastname = EXCLUDED.lastname,
	username = EXCLUDED.username,
	email = EXCLUDED.email,
	password_hash = EXCLUDED.password_hash,
	avatar_url = EXCLUDED.avatar_url,
	bio = EXCLUDED.bio,
	is_active = TRUE,
	updated_at = now();

WITH assignments(email, role_name) AS (
	VALUES
		('yasmine.diallo@plumora.test', 'AUTHOR'),
		('yasmine.diallo@plumora.test', 'READER'),
		('gabriel.costa@plumora.test', 'AUTHOR'),
		('gabriel.costa@plumora.test', 'READER'),
		('camille.derenne@plumora.test', 'AUTHOR'),
		('camille.derenne@plumora.test', 'READER'),
		('idriss.ndao@plumora.test', 'AUTHOR'),
		('idriss.ndao@plumora.test', 'READER'),
		('solene.armand@plumora.test', 'AUTHOR'),
		('solene.armand@plumora.test', 'READER')
)
INSERT INTO user_roles (id_user, id_role, assigned_at)
SELECT u.id_user, r.id_role, now() - interval '90 days'
FROM assignments a
JOIN users u ON u.email = a.email
JOIN roles r ON r.name = a.role_name
ON CONFLICT (id_user, id_role) DO NOTHING;

INSERT INTO books (
	id_book, author_id, title, subtitle, summary, cover_url, genre, language_code,
	status, visibility, published_at, reading_count, average_rating,
	external_source, external_id, external_authors, external_subjects, external_languages,
	source_url, read_url, download_count, formats_json, created_at, updated_at
)
VALUES
	(
		'20000000-0000-4000-8000-000000000013',
		'10000000-0000-4000-8000-000000000010',
		'Les Heures d''ambre',
		'Paris, 1900',
		'À l Exposition universelle, une jeune horlogère découvre dans des montres abandonnées des messages qui annoncent des événements avant qu ils ne se produisent.',
		'uploads/book-covers/plumora-heures-ambre.png',
		'Roman historique', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '29 days',
		384, 4.50, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '75 days', now() - interval '29 days'
	),
	(
		'20000000-0000-4000-8000-000000000014',
		'10000000-0000-4000-8000-000000000010',
		'La Maison des marées',
		NULL,
		'Deux sœurs que tout oppose héritent de la maison battue par les vents où elles ont grandi et doivent décider ce qu elles veulent encore sauver de leur histoire.',
		'uploads/book-covers/plumora-maison-marees.png',
		'Drame', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '24 days',
		291, 4.00, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '67 days', now() - interval '24 days'
	),
	(
		'20000000-0000-4000-8000-000000000015',
		'10000000-0000-4000-8000-000000000011',
		'Orbite 9',
		'Le jardin du silence',
		'Seule botaniste d une station orbitale, Alma reçoit depuis sa serre un signal que les instruments déclarent impossible et qui semble répondre à ses plantes.',
		'uploads/book-covers/plumora-orbite-neuf.png',
		'Science-fiction', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '14 days',
		447, 5.00, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '58 days', now() - interval '14 days'
	),
	(
		'20000000-0000-4000-8000-000000000016',
		'10000000-0000-4000-8000-000000000011',
		'Mémoire de Titan',
		NULL,
		'Un coursier transporte les derniers souvenirs terrestres entre les colonies de Saturne, jusqu au jour où une capsule lui restitue une enfance qu il n a jamais vécue.',
		'uploads/book-covers/plumora-memoire-titan.png',
		'Science-fiction', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '10 days',
		326, 4.50, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '51 days', now() - interval '10 days'
	),
	(
		'20000000-0000-4000-8000-000000000017',
		'10000000-0000-4000-8000-000000000012',
		'Le Café des jours pluvieux',
		NULL,
		'Quand son petit café risque de fermer, Inaya réunit cinq habitués qui ne se parlaient jamais et leur propose de partager chaque semaine une histoire vraie.',
		'uploads/book-covers/plumora-cafe-jours-pluvieux.png',
		'Feel-good', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '6 days',
		238, 4.50, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '46 days', now() - interval '6 days'
	),
	(
		'20000000-0000-4000-8000-000000000018',
		'10000000-0000-4000-8000-000000000012',
		'Ceux qui dansent sous la pluie',
		NULL,
		'Après avoir rangé son violon pendant deux ans, Simon rencontre une troupe de danse de rue qui transforme ses silences en nouveaux départs.',
		'uploads/book-covers/plumora-dansent-sous-pluie.png',
		'Contemporain', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '4 days',
		179, 4.00, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '39 days', now() - interval '4 days'
	),
	(
		'20000000-0000-4000-8000-000000000019',
		'10000000-0000-4000-8000-000000000013',
		'Les Disparus du canal',
		'Une enquête de Nils Aubry',
		'Un vélo rouge abandonné au bord de l eau relie trois disparitions que la police croyait sans rapport, dans un quartier où chacun observe le canal depuis sa fenêtre.',
		'uploads/book-covers/plumora-disparus-canal.png',
		'Polar', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '3 days',
		412, 4.50, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '43 days', now() - interval '3 days'
	),
	(
		'20000000-0000-4000-8000-000000000020',
		'10000000-0000-4000-8000-000000000013',
		'La Chambre 314',
		NULL,
		'Pendant les orages, une porte apparaît au troisième étage de l hôtel Belladone. Ceux qui l ouvrent y retrouvent un souvenir qui refuse de rester mort.',
		'uploads/book-covers/plumora-chambre-314.png',
		'Horreur', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '2 days',
		156, 3.50, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '35 days', now() - interval '2 days'
	),
	(
		'20000000-0000-4000-8000-000000000021',
		'10000000-0000-4000-8000-000000000014',
		'Le Royaume des cerfs-volants',
		NULL,
		'Lina et Tao suivent le fil d un cerf-volant magique jusqu à des îles suspendues, où le vent disparaît peu à peu et menace de faire tomber les villages du ciel.',
		'uploads/book-covers/plumora-royaume-cerfs-volants.png',
		'Jeunesse', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '1 day',
		263, 5.00, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '31 days', now() - interval '1 day'
	),
	(
		'20000000-0000-4000-8000-000000000022',
		'10000000-0000-4000-8000-000000000014',
		'La Bibliothèque des nuages',
		NULL,
		'Apprentie dans une bibliothèque suspendue, Éloïse doit recueillir les histoires oubliées cachées dans les nuages avant que la grande saison sèche ne les efface.',
		'uploads/book-covers/plumora-bibliotheque-nuages.png',
		'Fantasy', 'fr', 'PUBLISHED', 'PUBLIC', now() - interval '6 hours',
		98, 4.50, NULL, NULL, '[]'::jsonb, '[]'::jsonb, '["fr"]'::jsonb,
		NULL, NULL, NULL, '{}'::jsonb, now() - interval '27 days', now() - interval '6 hours'
	)
ON CONFLICT (id_book) DO UPDATE SET
	author_id = EXCLUDED.author_id,
	title = EXCLUDED.title,
	subtitle = EXCLUDED.subtitle,
	summary = EXCLUDED.summary,
	cover_url = EXCLUDED.cover_url,
	genre = EXCLUDED.genre,
	language_code = EXCLUDED.language_code,
	status = EXCLUDED.status,
	visibility = EXCLUDED.visibility,
	published_at = EXCLUDED.published_at,
	reading_count = EXCLUDED.reading_count,
	average_rating = EXCLUDED.average_rating,
	external_source = NULL,
	external_id = NULL,
	external_authors = '[]'::jsonb,
	external_subjects = '[]'::jsonb,
	external_languages = EXCLUDED.external_languages,
	source_url = NULL,
	read_url = NULL,
	download_count = NULL,
	formats_json = '{}'::jsonb,
	updated_at = EXCLUDED.updated_at;

INSERT INTO chapters (
	id_chapter, book_id, title, content, chapter_order, word_count, created_at, updated_at
)
VALUES
	('30000000-0000-4000-8000-000000000024', '20000000-0000-4000-8000-000000000013', 'Le pavillon des horloges', 'Sous la verrière du pavillon, mille cadrans battaient à des heures différentes. Adèle savait reconnaître une montre malade au bruit de son ressort, mais celle que le visiteur avait laissée sur son établi ne faisait aucun son. À l intérieur, à la place du mécanisme, une bande de papier portait trois mots et une heure précise. Elle leva les yeux vers la grande horloge de l exposition. Il restait onze minutes avant que le message ne devienne vrai.', 1, 78, now() - interval '74 days', now() - interval '29 days'),
	('30000000-0000-4000-8000-000000000025', '20000000-0000-4000-8000-000000000013', 'Onze minutes d avance', 'Adèle traversa la foule en tenant la montre contre son cœur. Les fiacres, les robes claires et les vendeurs de journaux lui barraient le passage. Le message annonçait la chute d une passerelle près du fleuve. Lorsqu elle atteignit le quai, les premiers visiteurs montaient déjà sur la structure. Personne ne voulut écouter une apprentie couverte de graisse. Alors la montre se remit à battre, non pas comme une horloge, mais comme un compte à rebours.', 2, 76, now() - interval '73 days', now() - interval '29 days'),
	('30000000-0000-4000-8000-000000000026', '20000000-0000-4000-8000-000000000014', 'La clé sous le pot bleu', 'Maya trouva la clé exactement où leur mère la cachait autrefois, sous le pot bleu fendu par le gel. La maison semblait plus petite, mais le vent y faisait toujours trembler les mêmes vitres. De l autre côté du jardin, Salomé attendait sans retirer son manteau. Les deux sœurs ne s étaient pas vues depuis six ans. Entre elles, la porte refusait de s ouvrir, gonflée par le sel et par tout ce qu elles n avaient jamais su se dire.', 1, 82, now() - interval '66 days', now() - interval '24 days'),
	('30000000-0000-4000-8000-000000000027', '20000000-0000-4000-8000-000000000014', 'La chambre face au large', 'Dans la chambre de leur enfance, le papier peint se décollait en longues vagues pâles. Salomé ouvrit la fenêtre malgré la pluie et découvrit, gravées dans le bois, les dates de toutes les tempêtes vécues ici. La dernière était celle de leur départ. Maya posa une main sur le mur humide. Sous plusieurs couches de peinture, quelque chose avait été muré. Elles se regardèrent enfin avec la même question, et aucune ne proposa de remettre la découverte au lendemain.', 2, 79, now() - interval '65 days', now() - interval '24 days'),
	('30000000-0000-4000-8000-000000000028', '20000000-0000-4000-8000-000000000015', 'La pousse impossible', 'À 04:12, toutes les feuilles de la serre se tournèrent vers Saturne. Alma coupa l irrigation, vérifia les lampes puis posa sa paume contre la vitre. Une pousse inconnue avait traversé le terreau stérile du bac neuf. Ses nervures pulsaient selon une suite de nombres premiers. La station prétendait qu aucune anomalie n existait. Pourtant, dans son casque, une voix très lointaine venait de prononcer son prénom avec huit secondes de retard.', 1, 74, now() - interval '57 days', now() - interval '14 days'),
	('30000000-0000-4000-8000-000000000029', '20000000-0000-4000-8000-000000000015', 'Réponse chlorophylle', 'Alma transforma les lampes horticoles en émetteur. Une impulsion courte pour oui, deux pour non. La pousse inclina sa tige avant même la première question. Derrière le hublot, les anneaux de Saturne coupaient le ciel comme une portée musicale. Le signal ne venait pas de la planète, comprit-elle, mais d un point beaucoup plus proche. Quelque chose s était réveillé dans le module abandonné de la station, là où aucune plante n aurait dû survivre.', 2, 77, now() - interval '56 days', now() - interval '14 days'),
	('30000000-0000-4000-8000-000000000030', '20000000-0000-4000-8000-000000000016', 'La capsule bleue', 'Noé transportait des souvenirs depuis assez longtemps pour ne plus être curieux. Il pesait les capsules, contrôlait leur température et ne lisait jamais les noms. Sur Titan, la glace punissait les distractions. Mais la capsule bleue chuchota dès qu il franchit le lac gelé. Elle lui montra une cuisine ensoleillée, une femme qui riait et ses propres mains d enfant couvertes de farine. Aucun de ces souvenirs ne figurait dans son dossier de naissance.', 1, 78, now() - interval '50 days', now() - interval '10 days'),
	('30000000-0000-4000-8000-000000000031', '20000000-0000-4000-8000-000000000016', 'Le lac de méthane', 'La balise de l avant-poste clignotait de l autre côté du lac. Noé aurait dû suivre les piquets lumineux, mais la capsule tirait sa combinaison vers une ligne sombre sous la glace. À chaque pas, un nouveau souvenir se dépliait en lui. Il reconnut le visage de la destinataire avant d avoir lu son nom. Puis la surface craqua et révéla, emprisonnée plusieurs mètres plus bas, une navette portant l emblème effacé de sa propre famille.', 2, 76, now() - interval '49 days', now() - interval '10 days'),
	('30000000-0000-4000-8000-000000000032', '20000000-0000-4000-8000-000000000017', 'Table près de la fenêtre', 'Le mardi de la grande pluie, Inaya servit cinq cafés et n encaissa personne. Elle posa au centre de la table un carnet rouge sauvé de la réserve. Les habitués protestèrent avec douceur, chacun réfugié derrière sa tasse. Pour empêcher le café de fermer, expliqua-t-elle, il fallait lui donner une raison d exister. La première page attendait une histoire vraie. Dehors, l eau effaçait la rue; dedans, un vieil homme prit enfin le stylo.', 1, 81, now() - interval '45 days', now() - interval '6 days'),
	('30000000-0000-4000-8000-000000000033', '20000000-0000-4000-8000-000000000017', 'Le carnet rouge', 'Marcel écrivit seulement une adresse. La jeune femme au manteau vert reconnut aussitôt l immeuble de son enfance, tandis que le livreur près du comptoir laissa tomber sa cuillère. Trois inconnus partageaient donc le même souvenir d un balcon rempli de géraniums. Inaya resservit du café et tourna la page. La pluie redoubla contre la vitre, mais personne ne regarda l heure. Pour la première fois depuis des mois, la salle semblait trop petite pour toutes les histoires qui arrivaient.', 2, 80, now() - interval '44 days', now() - interval '6 days'),
	('30000000-0000-4000-8000-000000000034', '20000000-0000-4000-8000-000000000018', 'Un violon dans la housse', 'Simon traversait la place chaque soir sans ouvrir la housse qu il portait sur le dos. Ce jeudi, la pluie avait chassé les passants, sauf quatre danseurs qui répétaient sur les dalles brillantes. Leur enceinte tomba en panne au milieu d un mouvement. Une fille lui demanda de jouer quelque chose. Il répondit qu il ne savait plus. Elle sourit, compta jusqu à quatre et commença tout de même à danser dans le silence.', 1, 73, now() - interval '38 days', now() - interval '4 days'),
	('30000000-0000-4000-8000-000000000035', '20000000-0000-4000-8000-000000000018', 'Après la dernière note', 'La première note fut si fragile que Simon crut que la pluie l avait inventée. Les danseurs adaptèrent leurs pas à son hésitation. Personne ne lui demanda pourquoi ses mains tremblaient, ni pourquoi la mélodie s arrêtait toujours avant le refrain. À la troisième reprise, les fenêtres autour de la place commencèrent à s ouvrir. Simon ferma les yeux. La musique qu il avait refusé de jouer pendant deux ans retrouva enfin un chemin qui ne passait pas par le souvenir.', 2, 78, now() - interval '37 days', now() - interval '4 days'),
	('30000000-0000-4000-8000-000000000036', '20000000-0000-4000-8000-000000000019', 'Le vélo rouge', 'Le vélo reposait contre l anneau d amarrage, parfaitement sec malgré la pluie. Nils photographia la chaîne neuve, le pneu couvert d argile et la sonnette entourée d un fil bleu. Sur le canal, une péniche passa sans lumière. La propriétaire du vélo avait disparu la veille, comme deux autres personnes au cours du même mois. Les dossiers ne partageaient ni âge, ni métier, ni adresse. Mais tous trois avaient publié la même photo du pont à minuit.', 1, 81, now() - interval '42 days', now() - interval '3 days'),
	('30000000-0000-4000-8000-000000000037', '20000000-0000-4000-8000-000000000019', 'Sous le pont des Forges', 'À marée basse, un escalier apparut sous le pont des Forges. Nils descendit jusqu à une porte rouge que les plans municipaux ne mentionnaient pas. Derrière, des dizaines de photographies du canal séchaient sur des fils. Chacune montrait un disparu quelques heures avant son effacement. La plus récente représentait Nils lui-même, penché sur le vélo rouge. Au dos, une date était inscrite: demain. Il entendit alors la porte se refermer au-dessus de lui.', 2, 76, now() - interval '41 days', now() - interval '3 days'),
	('30000000-0000-4000-8000-000000000038', '20000000-0000-4000-8000-000000000020', 'L étage absent', 'L ascenseur indiquait trois, puis quatre. Jamais trois cent quatorze. Pourtant, pendant l orage, une nouvelle touche apparut sous les doigts de Léonie. Les portes s ouvrirent sur un couloir plus long que l hôtel. Les lampes s allumaient une à une jusqu à une porte entrouverte. Sur le tapis, une ombre avançait sans corps. Léonie reconnut le parfum de sa mère, morte depuis douze ans, et la voix derrière la porte lui demanda pourquoi elle avait tant tardé.', 1, 79, now() - interval '34 days', now() - interval '2 days'),
	('30000000-0000-4000-8000-000000000039', '20000000-0000-4000-8000-000000000020', 'Le registre mouillé', 'Le registre de la réception était trempé alors que le toit ne fuyait pas. À chaque éclair, un nom nouveau apparaissait à la page 314. Léonie chercha le sien et trouva celui de sa mère, accompagné d une date située trois jours dans le futur. Dans le miroir derrière le comptoir, le couloir impossible se reflétait encore. Toutes ses portes étaient fermées, sauf une. Quelqu un venait d y poser une valise identique à celle que Léonie gardait sous son lit depuis l enfance.', 2, 80, now() - interval '33 days', now() - interval '2 days'),
	('30000000-0000-4000-8000-000000000040', '20000000-0000-4000-8000-000000000021', 'Le fil dans le ciel', 'Le cerf-volant descendit des nuages sans appartenir à personne. Son fil doré s enroula doucement autour du poignet de Lina, puis tira vers le bord de la falaise. Tao voulut le couper, mais le vent forma sous leurs pieds un escalier transparent. Très haut, des îles flottaient comme des bateaux immobiles. Sur la plus proche, un village entier agitait des mouchoirs. Le grand cerf-volant se gonfla et les emporta avant qu ils aient le temps d avoir peur.', 1, 80, now() - interval '30 days', now() - interval '1 day'),
	('30000000-0000-4000-8000-000000000041', '20000000-0000-4000-8000-000000000021', 'L île sans vent', 'Sur l île d Azur, les moulins ne tournaient plus et les oiseaux marchaient. La doyenne expliqua que chaque village gardait autrefois une direction du vent, jusqu à ce que le royaume oublie de les partager. Lina leva leur cerf-volant: sa toile brillait de quatre couleurs, mais une cinquième restait grise. Pour rallumer le ciel, ils devraient atteindre l île interdite au-dessus de la tempête. Tao regarda le fil qui disparaissait dans les nuages et serra le nœud de son sac.', 2, 83, now() - interval '29 days', now() - interval '1 day'),
	('30000000-0000-4000-8000-000000000042', '20000000-0000-4000-8000-000000000022', 'Le rayon des histoires perdues', 'Éloïse montait l échelle depuis une heure lorsque les rayonnages traversèrent le premier nuage. Autour d elle, les gouttes murmuraient des débuts de contes sans fin. Elle tendit son filet vers une petite masse dorée qui avait la forme d un livre ouvert. Dès qu elle la toucha, une ville entière apparut entre les étagères, minuscule et lumineuse. Puis le nuage se mit à fondre, et avec lui les habitants oublièrent jusqu au nom de leur rivière.', 1, 78, now() - interval '26 days', now() - interval '6 hours'),
	('30000000-0000-4000-8000-000000000043', '20000000-0000-4000-8000-000000000022', 'La saison sèche', 'Les bibliothécaires avaient fermé les fenêtres, mais la chaleur entrait par les histoires. Chaque matin, une étagère devenait blanche. Éloïse consulta l atlas des pluies et découvrit une page arrachée exactement au-dessus de leur cité. Quelqu un avait condamné les nuages à disparaître. Dans sa poche, le petit livre doré battait comme un oiseau. Il connaissait le chemin vers la page manquante, à condition qu Éloïse accepte de quitter la bibliothèque avant la tombée du dernier mot.', 2, 77, now() - interval '25 days', now() - interval '6 hours')
ON CONFLICT (id_chapter) DO UPDATE SET
	book_id = EXCLUDED.book_id,
	title = EXCLUDED.title,
	content = EXCLUDED.content,
	chapter_order = EXCLUDED.chapter_order,
	word_count = EXCLUDED.word_count,
	updated_at = EXCLUDED.updated_at;

INSERT INTO favorites (id_favorite, user_id, book_id, created_at)
VALUES
	('71000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000013', now() - interval '20 days'),
	('71000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000014', now() - interval '18 days'),
	('71000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000015', now() - interval '9 days'),
	('71000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000016', now() - interval '8 days'),
	('71000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000017', now() - interval '5 days'),
	('71000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000018', now() - interval '3 days'),
	('71000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000019', now() - interval '2 days'),
	('71000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000020', now() - interval '1 day'),
	('71000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000021', now() - interval '12 hours'),
	('71000000-0000-4000-8000-000000000018', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000022', now() - interval '3 hours')
ON CONFLICT (user_id, book_id) DO NOTHING;

INSERT INTO reviews (id_review, user_id, book_id, rating, comment, created_at, updated_at)
VALUES
	('72000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000013', 5, 'Une intrigue historique très visuelle et un beau personnage d horlogère.', now() - interval '18 days', now() - interval '18 days'),
	('72000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000013', 4, 'Le Paris de 1900 fonctionne très bien et le mystère donne envie de continuer.', now() - interval '15 days', now() - interval '15 days'),
	('72000000-0000-4000-8000-000000000014', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000014', 4, 'Un drame familial sensible avec une superbe atmosphère marine.', now() - interval '14 days', now() - interval '14 days'),
	('72000000-0000-4000-8000-000000000015', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000014', 4, 'Les deux sœurs sont crédibles et la maison devient presque un personnage.', now() - interval '12 days', now() - interval '12 days'),
	('72000000-0000-4000-8000-000000000016', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000015', 5, 'Science-fiction calme, mystérieuse et vraiment originale.', now() - interval '8 days', now() - interval '8 days'),
	('72000000-0000-4000-8000-000000000017', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000015', 5, 'La serre orbitale et le signal végétal forment une excellente accroche.', now() - interval '6 days', now() - interval '6 days'),
	('72000000-0000-4000-8000-000000000018', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000016', 4, 'Très belle idée autour des souvenirs transportés entre les lunes.', now() - interval '7 days', now() - interval '7 days'),
	('72000000-0000-4000-8000-000000000019', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000016', 5, 'Une aventure intime avec une vraie sensation d immensité.', now() - interval '5 days', now() - interval '5 days'),
	('72000000-0000-4000-8000-000000000020', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000017', 5, 'Chaleureux sans être trop sucré, parfait pour une lecture du dimanche.', now() - interval '4 days', now() - interval '4 days'),
	('72000000-0000-4000-8000-000000000021', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000017', 4, 'Les habitués du café donnent immédiatement envie de les connaître.', now() - interval '3 days', now() - interval '3 days'),
	('72000000-0000-4000-8000-000000000022', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000018', 4, 'Un début lumineux sur le deuil, la musique et le mouvement.', now() - interval '3 days', now() - interval '3 days'),
	('72000000-0000-4000-8000-000000000023', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000018', 4, 'La scène sur la place mouillée est très vivante.', now() - interval '2 days', now() - interval '2 days'),
	('72000000-0000-4000-8000-000000000024', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000019', 5, 'Polar nocturne efficace, avec un indice central simple et mémorable.', now() - interval '2 days', now() - interval '2 days'),
	('72000000-0000-4000-8000-000000000025', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000019', 4, 'Le canal et ses entrepôts créent une très bonne tension.', now() - interval '1 day', now() - interval '1 day'),
	('72000000-0000-4000-8000-000000000026', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000020', 4, 'Une horreur discrète qui mise bien sur l attente et les souvenirs.', now() - interval '1 day', now() - interval '1 day'),
	('72000000-0000-4000-8000-000000000027', '10000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000020', 3, 'Très bonne ambiance, même si le rythme prend son temps.', now() - interval '12 hours', now() - interval '12 hours'),
	('72000000-0000-4000-8000-000000000028', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000021', 5, 'Une aventure jeunesse colorée qui donne envie de prendre le vent.', now() - interval '10 hours', now() - interval '10 hours'),
	('72000000-0000-4000-8000-000000000029', '10000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000021', 5, 'Les îles suspendues et les cerfs-volants sont immédiatement attachants.', now() - interval '8 hours', now() - interval '8 hours'),
	('72000000-0000-4000-8000-000000000030', '10000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000022', 5, 'Une fantasy douce avec un univers de bibliothèque magnifique.', now() - interval '2 hours', now() - interval '2 hours'),
	('72000000-0000-4000-8000-000000000031', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000022', 4, 'Poétique, accessible et parfaite pour terminer la sélection découverte.', now() - interval '1 hour', now() - interval '1 hour')
ON CONFLICT (id_review) DO UPDATE SET
	user_id = EXCLUDED.user_id,
	book_id = EXCLUDED.book_id,
	rating = EXCLUDED.rating,
	comment = EXCLUDED.comment,
	updated_at = EXCLUDED.updated_at;

UPDATE books b
SET average_rating = ratings.average_rating
FROM (
	SELECT book_id, round(avg(rating)::numeric, 2) AS average_rating
	FROM reviews
	WHERE book_id BETWEEN
		'20000000-0000-4000-8000-000000000013'::uuid
		AND '20000000-0000-4000-8000-000000000022'::uuid
	GROUP BY book_id
) ratings
WHERE b.id_book = ratings.book_id;

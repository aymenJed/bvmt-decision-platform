-- =====================================================================
-- V3__seed_instruments_and_admin.sql
-- Données de démarrage : instruments BVMT les plus liquides + admin par défaut
-- =====================================================================

-- ---------------------------------------------------------------------
-- Instruments : top 20+ valeurs représentatives de la BVMT
-- (ISINs à vérifier/compléter lors de l'intégration avec le CMF)
-- ---------------------------------------------------------------------
INSERT INTO instrument (isin, ticker, name, instrument_type, sector, market, currency, is_active)
VALUES
 -- Banques
 ('TN0002200053', 'BT',     'Banque de Tunisie',                  'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0001800053', 'BIAT',   'Banque Internationale Arabe de Tunisie','EQUITY','Banques',  'BVMT', 'TND', TRUE),
 ('TN0001400058', 'ATB',    'Arab Tunisian Bank',                 'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0003100708', 'BNA',    'Banque Nationale Agricole',          'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0001000017', 'STB',    'Société Tunisienne de Banque',       'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0005200013', 'BH',     'Banque de l''Habitat',               'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0003900017', 'UIB',    'Union Internationale de Banques',    'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0007410018', 'UBCI',   'UBCI',                               'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),
 ('TN0002500017', 'ATTIJARI','Attijari Bank',                     'EQUITY', 'Banques',     'BVMT', 'TND', TRUE),

 -- Agro-alimentaire / boissons
 ('TN0001100031', 'SFBT',   'Société de Fabrication des Boissons de Tunisie','EQUITY','Agroalimentaire','BVMT','TND',TRUE),
 ('TN0003000015', 'DELICE', 'Délice Holding',                     'EQUITY', 'Agroalimentaire','BVMT','TND',TRUE),

 -- Services financiers / leasing
 ('TN0007180017', 'TL',     'Tunisie Leasing',                    'EQUITY', 'Services financiers','BVMT','TND',TRUE),
 ('TN0006000017', 'MPBS',   'MPBS',                               'EQUITY', 'Services financiers','BVMT','TND',TRUE),
 ('TN0005490011', 'CIL',    'Compagnie Internationale de Leasing','EQUITY', 'Services financiers','BVMT','TND',TRUE),

 -- Industrie
 ('TN0008200015', 'PGH',    'Poulina Group Holding',              'EQUITY', 'Holding diversifié','BVMT','TND',TRUE),
 ('TN0007300012', 'TPR',    'Tunisie Profilés Aluminium',         'EQUITY', 'Industrie',   'BVMT', 'TND', TRUE),

 -- Assurance
 ('TN0003500015', 'STAR',   'STAR Assurances',                    'EQUITY', 'Assurance',   'BVMT', 'TND', TRUE),

 -- Télécoms / Tech
 ('TN0005000017', 'SOTETEL','Société Tunisienne d''Entreprises de Télécommunications','EQUITY','Télécoms','BVMT','TND',TRUE),

 -- Indices
 ('TN-INDEX-01',  'TUNINDEX','Tunindex',                          'INDEX',  'Indice',      'BVMT', 'TND', TRUE),
 ('TN-INDEX-02',  'TUNINDEX20','Tunindex 20',                     'INDEX',  'Indice',      'BVMT', 'TND', TRUE)
ON CONFLICT (isin) DO NOTHING;

-- ---------------------------------------------------------------------
-- Compte admin par défaut (mot de passe : admin — À CHANGER au 1er login)
-- Hash BCrypt de "admin" avec coût 12
-- ---------------------------------------------------------------------
INSERT INTO app_user (username, email, password_hash, full_name, enabled)
VALUES ('admin', 'admin@bvmt-platform.local',
        '$2a$12$VQPGxY9b5kY6xRZy1kP1bujDTAyqRA3F8YxZpLQOCnWfLQ1oOdxtG',
        'Administrateur', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u, app_role r
WHERE u.username = 'admin' AND r.name IN ('ROLE_ADMIN', 'ROLE_ANALYST', 'ROLE_USER')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Compte trader démo (mot de passe : trader — À CHANGER)
-- ---------------------------------------------------------------------
INSERT INTO app_user (username, email, password_hash, full_name, enabled)
VALUES ('trader', 'trader@bvmt-platform.local',
        '$2a$12$o/y4C9wJWBDh3Q6EtNi30OhlU1Ty3jYBhJuS84j3bAm5wH1pBTQY2',
        'Trader Démo', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_user u, app_role r
WHERE u.username = 'trader' AND r.name IN ('ROLE_USER', 'ROLE_ANALYST')
ON CONFLICT DO NOTHING;

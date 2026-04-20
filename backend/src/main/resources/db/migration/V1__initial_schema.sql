-- =====================================================================
-- V1__initial_schema.sql
-- Schéma initial : référentiel titres, utilisateurs, portefeuilles
-- =====================================================================

-- Activer TimescaleDB (extension doit être installée sur l'instance)
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- ---------------------------------------------------------------------
-- Utilisateurs & sécurité
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE app_role (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE     -- ROLE_USER, ROLE_ANALYST, ROLE_ADMIN
);

CREATE TABLE app_user_role (
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES app_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO app_role(name) VALUES ('ROLE_USER'), ('ROLE_ANALYST'), ('ROLE_ADMIN');

-- ---------------------------------------------------------------------
-- Référentiel des instruments cotés
-- ---------------------------------------------------------------------
CREATE TABLE instrument (
    id              BIGSERIAL    PRIMARY KEY,
    isin            VARCHAR(12)  NOT NULL UNIQUE,
    ticker          VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    instrument_type VARCHAR(20)  NOT NULL
        CHECK (instrument_type IN ('EQUITY','BOND','SICAV','FCP','INDEX')),
    sector          VARCHAR(100),
    market          VARCHAR(50)  NOT NULL DEFAULT 'BVMT',   -- BVMT / HORS_COTE
    currency        CHAR(3)      NOT NULL DEFAULT 'TND',
    listing_date    DATE,
    nominal_value   NUMERIC(15,4),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    metadata        JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_instrument_type   ON instrument(instrument_type) WHERE is_active;
CREATE INDEX idx_instrument_ticker ON instrument(ticker)           WHERE is_active;

-- Métadonnées spécifiques aux obligations (BTA/BTC)
CREATE TABLE bond_details (
    instrument_id   BIGINT       PRIMARY KEY REFERENCES instrument(id) ON DELETE CASCADE,
    issuer          VARCHAR(255) NOT NULL,
    issue_date      DATE         NOT NULL,
    maturity_date   DATE         NOT NULL,
    coupon_rate     NUMERIC(7,4) NOT NULL,              -- en %
    coupon_type     VARCHAR(20)  NOT NULL               -- FIXED / VARIABLE / ZERO
        CHECK (coupon_type IN ('FIXED','VARIABLE','ZERO')),
    coupon_frequency SMALLINT    NOT NULL DEFAULT 1,    -- par an
    face_value      NUMERIC(15,4) NOT NULL
);

-- =====================================================================
-- V1 installation terminée
-- =====================================================================

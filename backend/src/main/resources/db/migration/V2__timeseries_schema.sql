-- =====================================================================
-- V2__timeseries_schema.sql
-- Séries temporelles des cours (hypertables TimescaleDB) + signaux
-- =====================================================================

-- ---------------------------------------------------------------------
-- Cours OHLCV journalier (clôture officielle BVMT)
-- ---------------------------------------------------------------------
CREATE TABLE price_daily (
    instrument_id  BIGINT       NOT NULL REFERENCES instrument(id) ON DELETE CASCADE,
    trade_date     DATE         NOT NULL,
    open_price     NUMERIC(15,4),
    high_price     NUMERIC(15,4),
    low_price      NUMERIC(15,4),
    close_price    NUMERIC(15,4) NOT NULL,
    reference_price NUMERIC(15,4),                -- cours de référence (veille)
    volume         BIGINT       NOT NULL DEFAULT 0,
    turnover       NUMERIC(18,2) NOT NULL DEFAULT 0,  -- capitaux échangés
    nb_trades      INTEGER      NOT NULL DEFAULT 0,
    variation_pct  NUMERIC(7,4),
    source         VARCHAR(30)  NOT NULL DEFAULT 'BVMT_BULLETIN',
    ingested_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (instrument_id, trade_date)
);

-- Conversion en hypertable (partitionnement temporel par mois)
SELECT create_hypertable('price_daily', 'trade_date',
                         chunk_time_interval => INTERVAL '1 month',
                         if_not_exists => TRUE);

CREATE INDEX idx_price_daily_instrument_date
    ON price_daily(instrument_id, trade_date DESC);

-- Compression des chunks > 6 mois (gain ~90% d'espace)
ALTER TABLE price_daily SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'instrument_id',
    timescaledb.compress_orderby   = 'trade_date DESC'
);
SELECT add_compression_policy('price_daily', INTERVAL '6 months');

-- ---------------------------------------------------------------------
-- Carnet d'ordres / tick intraday (optionnel, alimenté par scraping)
-- ---------------------------------------------------------------------
CREATE TABLE price_tick (
    instrument_id BIGINT       NOT NULL REFERENCES instrument(id) ON DELETE CASCADE,
    tick_time     TIMESTAMPTZ  NOT NULL,
    price         NUMERIC(15,4) NOT NULL,
    volume        BIGINT,
    bid_price     NUMERIC(15,4),
    ask_price     NUMERIC(15,4),
    bid_size      BIGINT,
    ask_size      BIGINT,
    source        VARCHAR(30)  NOT NULL,
    PRIMARY KEY (instrument_id, tick_time)
);
SELECT create_hypertable('price_tick', 'tick_time',
                         chunk_time_interval => INTERVAL '1 week',
                         if_not_exists => TRUE);

-- Rétention 90j pour les ticks (volume énorme)
SELECT add_retention_policy('price_tick', INTERVAL '90 days');

-- ---------------------------------------------------------------------
-- Indicateurs techniques calculés & mis en cache
-- ---------------------------------------------------------------------
CREATE TABLE indicator_daily (
    instrument_id  BIGINT       NOT NULL REFERENCES instrument(id) ON DELETE CASCADE,
    trade_date     DATE         NOT NULL,
    indicator_code VARCHAR(20)  NOT NULL,        -- RSI_14 / SMA_20 / EMA_12 / MACD / YTM...
    value          NUMERIC(18,6) NOT NULL,
    meta           JSONB,                        -- ex: { "signal": 0.23, "histogram": 0.02 }
    computed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (instrument_id, trade_date, indicator_code)
);
SELECT create_hypertable('indicator_daily', 'trade_date',
                         chunk_time_interval => INTERVAL '3 months',
                         if_not_exists => TRUE);

-- ---------------------------------------------------------------------
-- Signaux de trading générés par le moteur de règles
-- ---------------------------------------------------------------------
CREATE TABLE trading_signal (
    id            BIGSERIAL    PRIMARY KEY,
    instrument_id BIGINT       NOT NULL REFERENCES instrument(id) ON DELETE CASCADE,
    signal_date   DATE         NOT NULL,
    signal_type   VARCHAR(10)  NOT NULL
        CHECK (signal_type IN ('BUY','SELL','HOLD')),
    strength      VARCHAR(10)  NOT NULL
        CHECK (strength IN ('WEAK','MEDIUM','STRONG')),
    rule_code     VARCHAR(50)  NOT NULL,      -- ex: RSI_OVERSOLD, MACD_GOLDEN_CROSS
    triggering_value NUMERIC(18,6),
    reference_price  NUMERIC(15,4) NOT NULL,
    rationale     TEXT,
    confidence    NUMERIC(5,2),               -- 0-100
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (instrument_id, signal_date, rule_code)
);
CREATE INDEX idx_signal_date ON trading_signal(signal_date DESC);
CREATE INDEX idx_signal_instrument ON trading_signal(instrument_id, signal_date DESC);

-- ---------------------------------------------------------------------
-- Configuration des règles (paramétrable via UI admin)
-- ---------------------------------------------------------------------
CREATE TABLE rule_definition (
    id           BIGSERIAL    PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    signal_type  VARCHAR(10)  NOT NULL
        CHECK (signal_type IN ('BUY','SELL','HOLD')),
    is_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    parameters   JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

INSERT INTO rule_definition(code, name, description, signal_type, parameters) VALUES
 ('RSI_OVERSOLD',      'RSI survente',            'RSI(14) < seuil', 'BUY',  '{"period":14,"threshold":30}'),
 ('RSI_OVERBOUGHT',    'RSI surachat',            'RSI(14) > seuil', 'SELL', '{"period":14,"threshold":70}'),
 ('SMA_GOLDEN_CROSS',  'Golden Cross SMA 20/50',  'SMA20 croise au-dessus SMA50', 'BUY',  '{"short":20,"long":50}'),
 ('SMA_DEATH_CROSS',   'Death Cross SMA 20/50',   'SMA20 croise en-dessous SMA50','SELL', '{"short":20,"long":50}'),
 ('MACD_BULLISH_CROSS','MACD croisement haussier','MACD > signal',   'BUY',  '{"fast":12,"slow":26,"signal":9}'),
 ('MACD_BEARISH_CROSS','MACD croisement baissier','MACD < signal',   'SELL', '{"fast":12,"slow":26,"signal":9}');

-- ---------------------------------------------------------------------
-- Portefeuilles des utilisateurs (watchlists + positions)
-- ---------------------------------------------------------------------
CREATE TABLE portfolio (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    name       VARCHAR(150) NOT NULL,
    is_default BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE portfolio_position (
    id            BIGSERIAL  PRIMARY KEY,
    portfolio_id  BIGINT     NOT NULL REFERENCES portfolio(id) ON DELETE CASCADE,
    instrument_id BIGINT     NOT NULL REFERENCES instrument(id),
    quantity      NUMERIC(18,4) NOT NULL DEFAULT 0,
    avg_cost      NUMERIC(15,4) NOT NULL DEFAULT 0,
    opened_at     DATE       NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (portfolio_id, instrument_id)
);

CREATE TABLE watchlist_item (
    id            BIGSERIAL  PRIMARY KEY,
    user_id       BIGINT     NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    instrument_id BIGINT     NOT NULL REFERENCES instrument(id),
    added_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, instrument_id)
);

-- ---------------------------------------------------------------------
-- Vues continues pour l'agrégation hebdo / mensuelle (perf !)
-- ---------------------------------------------------------------------
CREATE MATERIALIZED VIEW price_weekly
WITH (timescaledb.continuous) AS
SELECT
    instrument_id,
    time_bucket('1 week', trade_date) AS week_start,
    first(open_price, trade_date)  AS open_price,
    max(high_price)                 AS high_price,
    min(low_price)                  AS low_price,
    last(close_price, trade_date)   AS close_price,
    sum(volume)                     AS volume,
    sum(turnover)                   AS turnover
FROM price_daily
GROUP BY instrument_id, week_start
WITH NO DATA;

SELECT add_continuous_aggregate_policy('price_weekly',
    start_offset => INTERVAL '1 year',
    end_offset   => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day');

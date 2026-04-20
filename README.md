# BVMT Decision Platform

Plateforme d'aide à la décision (trading & analyse) pour le marché financier tunisien — Bourse des Valeurs Mobilières de Tunis (BVMT).

## Stack

- **Backend** : Java 22 · Spring Boot 3.3 · Spring Data JPA · Spring Security (JWT) · WebSocket STOMP · Flyway
- **Base de données** : PostgreSQL 16 + **TimescaleDB** (hypertables pour les séries temporelles)
- **Frontend** : Angular 17 standalone · RxJS · Chart.js · @stomp/rx-stomp · SockJS
- **ETL** : Bulletin Officiel BVMT (PDF) · Scraping Ilboursa (optionnel) · Import Excel (Apache POI)
- **Infra** : Docker Compose · Nginx

## Architecture

```
┌─────────────────┐  HTTPS/WS   ┌──────────────────┐  JDBC   ┌──────────────┐
│  Angular 17 SPA │ ──────────▶ │  Spring Boot API │ ──────▶ │  PostgreSQL  │
│  (nginx :4200)  │             │  (REST + STOMP)  │         │  TimescaleDB │
└─────────────────┘             └──────────────────┘         └──────────────┘
        ▲                               │  HTTPS  ▲
        │ STOMP/SockJS                  ▼         │
        └────── live signals ──────────┐│         │ cron ETL
                                       ││         │
                                ┌──────▼┴─────────▼───┐
                                │   Sources BVMT      │
                                │ • Bulletin PDF      │
                                │ • Ilboursa (opt.)   │
                                │ • Excel manuel      │
                                └─────────────────────┘
```

## Démarrage rapide

### Prérequis
- Docker & Docker Compose, OU
- Java 22, Maven 3.9+, Node.js 20+, PostgreSQL 16 avec TimescaleDB

### Avec Docker Compose (recommandé)

```bash
cp .env.example .env   # puis éditer JWT_SECRET
docker compose up -d --build
```

- Frontend  : http://localhost:4200
- Backend   : http://localhost:8080/api
- Swagger   : http://localhost:8080/api/swagger-ui.html
- Actuator  : http://localhost:8080/api/actuator/health

**Comptes démo** (à changer en prod !) :
- `admin / admin`    — rôles ADMIN + ANALYST + USER
- `trader / trader`  — rôles USER + ANALYST

### Lancement manuel

```bash
# 1) PostgreSQL + TimescaleDB
docker run -d --name bvmt-db \
  -e POSTGRES_USER=bvmt -e POSTGRES_PASSWORD=bvmt -e POSTGRES_DB=bvmt \
  -p 5432:5432 timescale/timescaledb:latest-pg16

# 2) Backend
cd backend
mvn spring-boot:run

# 3) Frontend
cd ../frontend
npm install
npm start
```

## Périmètre métier

**Instruments supportés** :
- Actions BVMT (BH, BT, BIAT, SFBT, PGH, STAR, Attijari, ATB, BNA, UIB, Délice, TPR, Tunisie Leasing…)
- Bons du Trésor (BTA / BTC) — calcul YTM intégré
- SICAV / FCP
- Indices (Tunindex, Tunindex20)

**Indicateurs techniques** :
- RSI (lissage Wilder, périodes configurables)
- SMA (20, 50, 200…)
- EMA (12, 26, …)
- MACD (12/26/9 avec histogramme et ligne signal)
- YTM (Newton-Raphson pour obligations)

**Règles de signaux** (moteur extensible, pattern Strategy) :
- `RSI_OVERSOLD` / `RSI_OVERBOUGHT`
- `SMA_GOLDEN_CROSS` / `SMA_DEATH_CROSS`
- `MACD_BULLISH_CROSS`
- *Ajouter une nouvelle règle = créer un `@Component` implémentant `TradingRule` — découverte automatique par Spring.*

## Sourcing des données

| Source | Type | Priorité | Statut |
|---|---|---|---|
| [BVMT Bulletin Officiel](https://www.bvmt.com.tn/editions-statistique) | PDF quotidien | 1 (primaire) | Actif |
| Distributeurs officiels BVMT (Market Data) | Licence payante | 0 (prod) | À négocier |
| [Ilboursa A-Z](https://www.ilboursa.com/marches/aaz) | Scraping HTML | 10 (repli) | **Désactivé** par défaut (CGU) |
| Import Excel manuel | Upload `/etl/import/excel` | Manuel | Actif |

## Configuration ETL

Dans `application.yml` :
```yaml
bvmt:
  etl:
    enabled: true
    daily-import-cron: "0 0 18 * * MON-FRI"   # après clôture + marge
    sources:
      ilboursa-enabled: false   # vérifier CGU avant d'activer
```

## Sécurité

- JWT HS256 (access 24h + refresh 7j) — secret configurable via `JWT_SECRET`
- BCrypt coût 12 pour les mots de passe
- Rôles : `ROLE_USER`, `ROLE_ANALYST`, `ROLE_ADMIN`
- CORS restrictif (localhost:4200 en dev, à configurer en prod)
- Pas de token dans les URLs, pas de données sensibles en logs

## Tests

```bash
cd backend
mvn test
```

Tests unitaires fournis :
- `RsiIndicatorTest` — jeu de référence Wilder + cas limites
- `SmaEmaIndicatorTest` — réactivité EMA
- `MacdIndicatorTest` — cohérence histogramme
- `YtmCalculatorTest` — obligation au pair et en décote

## Structure du projet

```
bvmt-decision-platform/
├── backend/                      # Spring Boot
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/bvmt/decision/
│       ├── config/               # Security, WebSocket, RestClient
│       ├── controller/           # REST endpoints
│       ├── dto/                  # Records I/O
│       ├── entity/               # JPA entities
│       ├── repository/           # Spring Data repos
│       ├── service/
│       │   ├── etl/              # Bulletin PDF, Ilboursa, Excel, ImportService
│       │   ├── indicator/        # RSI, SMA, EMA, MACD, YTM
│       │   └── signal/           # TradingRule + SignalEngine
│       ├── scheduler/            # EtlScheduler cron
│       ├── security/             # JWT
│       ├── websocket/            # AlertPublisher
│       └── exception/            # GlobalExceptionHandler
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/         # V1 (schema), V2 (timeseries), V3 (seed)
├── frontend/                     # Angular 17
│   ├── package.json
│   ├── Dockerfile / nginx.conf
│   └── src/app/
│       ├── core/                 # models, services, guards, interceptors
│       └── features/             # auth, dashboard, signals, instruments, portfolio
├── docker/                       # init-db.sql
├── docker-compose.yml
└── docs/                         # Document d'architecture .docx
```

## Roadmap

- [ ] Intégration flux intraday officiel (après licence BVMT)
- [ ] Backtesting des règles sur historique
- [ ] Notifications email / SMS sur alertes
- [ ] ML : détection de tendances avec LSTM
- [ ] Mobile app (Capacitor sur le code Angular existant)

## Avertissement

Les signaux générés par cette plateforme sont des **aides à la décision**, pas des recommandations d'investissement au sens réglementaire. L'utilisateur reste seul responsable de ses décisions de trading.

## Licence

Projet interne — à définir selon cadre d'exploitation.

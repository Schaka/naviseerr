# Local Development

Naviseerr's local development environment spins up all required services automatically using
[Testcontainers](https://testcontainers.com/). Docker must be running on your machine.

## Prerequisites

- JDK 25
- Docker (with the Docker daemon running)
- The repository cloned locally

## Running

Use `LocalNaviseerrStarter` instead of the regular `NaviseerrApplication` main class. This
alternative entry point boots all service containers before starting Spring, so the app connects
to live local instances of every dependency.

**Important:** Always set `LC_ALL=en_US.UTF-8` in your run configuration, otherwise some
locale-sensitive operations may fail.

In IntelliJ, open the run configuration for `LocalNaviseerrStarter` and add to the
environment variables:

```
LC_ALL=en_US.UTF-8
```

## What Gets Started

| Service | Port | Credentials |
|---|---|---|
| PostgreSQL 17 | random | `naviseerr` / `naviseerr` |
| Navidrome | random | `admin` / `admin` |
| Lidarr (nightly) | random | `admin` / `admin` — API key printed at startup |
| slskd | random | Web: `admin` / `admin` — API key: `naviseerr-local-dev` |
| naviseerr-tagging | random | — |

All mapped ports and connection strings are printed to the log after startup.

Naviseerr itself is configured automatically via `System.setProperty(...)` calls, so no manual
`application.yml` changes are required for local development.

It is recommended, if you want to test scrobbling and grabbing data from Last.FM for your users, to create a copy of `application.yml`, call is `application-local.yml` and start your Spring application with the profile `local`.
In IntelliJ, the profile can easily be set. In other IDEs, pass `-Dspring.profiles.active=local`.

## Directory Layout

Two directories are created in the project root if they don't exist:

```
naviseerr/
├── music-library/          ← shared music volume (read-write for all containers)
└── local-runtime/
    ├── downloads/          ← slskd download target + tagging service working directory
    ├── downloads/incomplete/
    ├── lidarr/             ← Lidarr config and database (persisted across restarts)
    ├── navidrome/          ← Navidrome database (persisted across restarts)
    ├── postgres/           ← PostgreSQL data directory (persisted across restarts)
    └── slskd/              ← slskd config, credentials file, database
```

## Soulseek Credentials

slskd needs a real Soulseek account. Credentials are resolved in this order:

1. Environment variables `SLSKD_SOULSEEK_USERNAME` and `SLSKD_SOULSEEK_PASSWORD`
2. `local-runtime/slskd/soulseek-credentials.txt` (auto-created on first run with random values)

On first boot with no credentials, random values are generated and saved to the credentials
file. These are reused on subsequent runs so the same account persists. If you want to use
your own Soulseek account, set the environment variables before starting.

## Lidarr Setup

The local dev starter automatically:

- Creates the `/music` root folder
- Sets up Forms authentication (`admin` / `admin`)
- Installs the [Tubifarry](https://github.com/TypNull/Tubifarry) plugin
- Enables all Primary Album Types in the Standard metadata profile
- Enables the Soulseek protocol in all delay profiles
- Registers slskd as both a download client and indexer

These steps happen on every fresh container boot (the setup is idempotent).

## Tagging Service

The tagging service (`ghcr.io/schaka/naviseerr-tagging:stable`) is included in the startup.
`ACOUSTID_API_KEY` is intentionally **not** set in the local dev environment. Audio fingerprinting
endpoints will warn but not crash. If you want to test real AcoustID lookups, set:

```
ACOUSTID_API_KEY=your_key
```

in your run configuration's environment variables (it gets forwarded to the container).

OneTagger metadata tagging runs without an API key — that feature is fully functional
out of the box.

### OneTagger Configuration

OneTagger uses `/config/autotagger.json` inside the container. This path is only mounted
when the file exists on your machine at `tagging-service/config/autotagger.json`
(gitignored — not committed).

To enable it:

```bash
cp tagging-service/config/autotagger.example.json tagging-service/config/autotagger.json
```

Then edit `autotagger.json` to add any API tokens you need (e.g. Discogs token under
`custom.discogs.token`). See the [OneTagger documentation](https://onetagger.github.io/docs/)
for all available options.

Without this file, OneTagger falls back to its built-in defaults (Beatport only), which will
fail for most general music. The example config uses MusicBrainz and Discogs instead.

## Frontend Development

The Quasar/Vue 3 frontend is compiled as part of the Gradle build — there is no separate build
step and no reverse proxy hiding two servers. For active frontend work, running a dedicated dev
server with hot-reload is recommended.

Requirements:

- Node.js 24.13.1
- pnpm 9.12.3 (`corepack enable pnpm` from a Node 24 install handles this automatically)

```bash
cd src/main/frontend
pnpm install
pnpm dev
```

The dev server proxies API calls to wherever the backend is running.

If you only need the backend, `LocalNaviseerrStarter` already handles everything. IntelliJ IDEA
(Community Edition) integrates well with both Kotlin/Gradle and the Vue frontend simultaneously.

## Disclaimer — Real Downloads

> **Warning:** slskd is a real Soulseek client and will connect to the live Soulseek network.
> When Lidarr triggers a search or when you manually request music, actual files will be
> downloaded from other users. Be mindful of what you request and ensure you are complying
> with your local laws and the Soulseek terms of service.

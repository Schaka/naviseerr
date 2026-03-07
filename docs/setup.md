# Setup Guide

> **Work in progress.** This guide covers the components that are currently implemented.
> Not all features described in the project overview are available yet.

## Stack Overview

Naviseerr sits on top of an existing self-hosted music stack and ties the pieces together.
All components communicate over a shared Docker network.

| Component | Purpose |
|---|---|
| [Navidrome](https://www.navidrome.org/) | Music server, user accounts, playback history |
| [Lidarr](https://lidarr.audio/) | Artist/album download management |
| [slskd](https://github.com/slskd/slskd) | Soulseek download client |
| [Tubifarry](https://github.com/TypNull/Tubifarry) | Lidarr plugin: slskd integration |
| naviseerr-tagging | Audio fingerprinting + metadata tagging (OneTagger wrapper) |
| naviseerr | The orchestration layer itself |
| PostgreSQL | naviseerr's own database |

## Docker Compose

Below is a full example. Adjust volume paths, IP addresses, and credentials for your environment.

```yaml
networks:
  music:
    driver: bridge

services:
  postgres:
    image: postgres:17
    restart: unless-stopped
    networks: [music]
    environment:
      POSTGRES_DB: naviseerr
      POSTGRES_USER: naviseerr
      POSTGRES_PASSWORD: naviseerr
    volumes:
      - ./data/postgres:/var/lib/postgresql/data

  navidrome:
    image: deluan/navidrome:latest
    restart: unless-stopped
    networks: [music]
    ports:
      - "4533:4533"
    environment:
      ND_MUSICFOLDER: /music
      ND_DATAFOLDER: /data
      ND_LOGLEVEL: info
      ND_SESSIONTIMEOUT: 24h
    volumes:
      - /path/to/your/music:/music:ro
      - ./data/navidrome:/data

  lidarr:
    image: lscr.io/linuxserver/lidarr:nightly
    restart: unless-stopped
    networks: [music]
    ports:
      - "8686:8686"
    environment:
      PUID: 1000
      PGID: 1000
      TZ: UTC
    volumes:
      - ./data/lidarr:/config
      - /path/to/your/music:/music
      - /path/to/your/downloads:/downloads

  slskd:
    image: slskd/slskd:latest
    restart: unless-stopped
    networks: [music]
    ports:
      - "5030:5030"
    environment:
      SLSKD_SLSK_USERNAME: your_soulseek_username
      SLSKD_SLSK_PASSWORD: your_soulseek_password
      SLSKD_API_KEY: your_slskd_api_key
      SLSKD_REMOTE_CONFIGURATION: "true"
    volumes:
      - /path/to/your/music:/music:ro
      - /path/to/your/downloads:/downloads
      - ./data/slskd:/app

  naviseerr-tagging:
    image: ghcr.io/schaka/naviseerr-tagging:stable
    restart: unless-stopped
    networks: [music]
    environment:
      PUID: 1000
      PGID: 1000
      ACOUSTID_API_KEY: your_acoustid_key   # optional, needed for audio fingerprinting
    volumes:
      - /path/to/your/music:/music
      - /path/to/your/downloads:/downloads
      - ./tagging-config:/config             # see "Tagging Service" section below

  naviseerr:
    image: ghcr.io/schaka/naviseerr:stable
    user: 1000:1000
    restart: unless-stopped
    networks: [music]
    ports:
      - "8080:8080"
    depends_on: [postgres, navidrome, lidarr, slskd, naviseerr-tagging]
    volumes:
      - ./application.yml:/config/application.yml:ro
```

## Lidarr Setup

### Use the Nightly Image

Plugin support requires the **nightly** LinuxServer image (`lscr.io/linuxserver/lidarr:nightly`).
The stable release does not expose the plugin API.

### Install Tubifarry

Tubifarry is the Lidarr plugin that connects Lidarr to slskd for Soulseek downloads.

1. In Lidarr, go to **System → Plugins**
2. Enter the GitHub URL: `https://github.com/TypNull/Tubifarry`
3. Click **Install** and let Lidarr restart

### Add slskd as Download Client

After Tubifarry is installed:

1. Go to **Settings → Download Clients → Add**
2. Choose **Slskd** (provided by Tubifarry)
3. Set the host to your slskd address and the API key you configured for slskd

### Add slskd as Indexer

1. Go to **Settings → Indexers → Add**
2. Choose **Slskd** (provided by Tubifarry)
3. Use the same host and API key as the download client

### Enable Soulseek in the Delay Profile

Lidarr's default delay profile only allows Usenet and Torrent. Soulseek releases are silently
rejected without this step.

1. Go to **Settings → Profiles → Delay Profiles**
2. Edit the default profile (or all of them)
3. Add **Soulseek** to the allowed protocols and set delay to `0`

### Enable All Primary Album Types in the Metadata Profile

By default, Lidarr's Standard metadata profile only tracks Albums. Expand this to include
EPs, singles, and other release types:

1. Go to **Settings → Profiles → Metadata Profiles**
2. Edit the **Standard** profile
3. Under **Primary Types**, enable all types you want to track

## Naviseerr Configuration

Naviseerr is configured via `application.yml`. Mount this file into the container at
`/config/application.yml`.

Create `application.yml` next to your `docker-compose.yml` (or where you see fit):
For up to date reference, see [application.yml](../src/main/resources/application.yml).

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/naviseerr
    username: naviseerr
    password: naviseerr

navidrome:
  url: "http://navidrome:4533"
  admin-user: "your_navidrome_admin_username"
  admin-pass: "your_navidrome_admin_password"

lastfm:
  api-key: "your_lastfm_api_key"
  shared-secret: "your_lastfm_shared_secret"

lidarr:
  url: "http://lidarr:8686"
  api-key: "your_lidarr_api_key"
  quality-profile-id: 2    # 1 = Any, 2 = Lossless (default); match your Lidarr profile
  metadata-profile-id: 1   # 1 = Standard profile (default)

slskd:
  url: "http://slskd:5030"
  api-key: "your_slskd_api_key"
  download-dir: "/downloads"  # path as seen by the naviseerr container

tagging:
  url: "http://naviseerr-tagging:8090"

```

**Getting your Last.fm credentials:** Register an API application at
https://www.last.fm/api/account/create. The `api-key` and `shared-secret` come from there.

**Getting your Lidarr API key:** Lidarr → Settings → General → API Key.

**Getting your slskd API key:** Set `SLSKD_API_KEY` in slskd's environment or `slskd.yml`.

## Tagging Service

`naviseerr-tagging` is a small HTTP service that wraps [OneTagger](https://onetagger.github.io/)
(metadata tagging) and a Python fingerprinting script (AcoustID + MusicBrainz).

### Volume Mounts

The tagging service needs access to the same paths as naviseerr itself:

| Host path | Container path | Purpose |
|---|---|---|
| `/path/to/your/music` | `/music` | Music library (read-write) |
| `/path/to/your/downloads` | `/downloads` | Download staging area (read-write) |
| `./tagging-config` | `/config` | OneTagger configuration (optional, read-only) |

Paths must be **identical** to what naviseerr sees — naviseerr tells the tagging service
which path to process, so the same path must resolve to the same files in both containers.

### Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `PUID` | `1000` | User ID to run as |
| `PGID` | `1000` | Group ID to run as |
| `ACOUSTID_API_KEY` | (empty) | Required for audio fingerprinting via AcoustID |
| `TAGGING_SERVICE_PORT` | `8090` | HTTP listen port |
| `ONETAGGER_BINARY` | `/usr/local/bin/onetagger-cli` | Path to OneTagger CLI |
| `ONETAGGER_DEFAULT_CONFIG` | `/config/autotagger.json` | Default OneTagger config |

OneTagger is only available on **amd64**. On arm64 the binary is not included and the
`/api/v1/onetagger/tag` endpoint returns `501 Not Implemented`.

### OneTagger Configuration

OneTagger reads its configuration from `/config/autotagger.json` inside the container.
Mount a directory containing this file to `/config`.

A fully commented example is available at
[`tagging-service/config/autotagger.example.json`](../tagging-service/config/autotagger.example.json)
in the repository.

Copy and customise it:

```bash
mkdir -p ./tagging-config
cp tagging-service/config/autotagger.example.json ./tagging-config/autotagger.json
# Edit autotagger.json — at minimum set custom.discogs.token if using Discogs
```

Key settings to review:

- `platforms` — which metadata sources to query (e.g. `["musicbrainz", "discogs"]`)
- `custom.discogs.token` — required if `discogs` is in `platforms`
- `strictness` — match confidence threshold (0.0–1.0, default `0.7`)
- `overwrite` — whether to overwrite existing tags
- `includeSubfolders` — process nested directories

See the [OneTagger documentation](https://onetagger.github.io/docs/) for the full reference.

## API Endpoints (Tagging Service)

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/api/v1/onetagger/tag` | Run OneTagger on a directory (amd64 only) |
| `POST` | `/api/v1/fingerprint` | AcoustID fingerprint lookup |
| `POST` | `/api/v1/tag` | Full pipeline: fingerprint + MusicBrainz lookup + write tags |

All tagging requests block until the job completes (one job at a time, serialised).

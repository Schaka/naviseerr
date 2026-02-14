# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Naviseerr is a self-hosted music request + metadata + recommendation + orchestration layer across the open source stack. It integrates with existing open-source music infrastructure (Navidrome, Lidarr, slskd, Last.fm, ListenBrainz, Audiomuse-AI) to provide multi-user music discovery, requesting, and recommendation features.

## Tech Stack Constraints

- **No Python.** No raw JavaScript — all JS must be transpiled from TypeScript.
- **Frontend:** Vue 3 with Quasar
- **Backend:** Spring Boot (with native compilation) using Kotlin/Gradle
- **LLM communication:** REST or MCP only — no direct LLM access
- **Metadata:** MusicBrainz as primary source
- **Fail fast**: Whenever possible. Do not use nullable types if avoidable. Immutability first.

## Code conventions
- **Java Docs** are okay. But no inline comments, code NEEDS to speak for itself.
- Don't wrap every REST call inside a try/catch -  fail fast is more important
- unless they're calls that can reasonably be expected to fail (scrobble to Last.FM), fail on apps that are part of the stack and need to be running (Navidrome, Lidarr, Slskd)
- if any retries are used, they need to be tied into the OpenFeign ecosystem

## Core Architecture

Multi-user system where accounts are pulled from Navidrome (Jellyfin support deferred until 2.0). Key subsystems:

1. **Request Management** — Users search/request artists and albums, routed to Lidarr or direct slskd download (for content without MusicBrainz entries or single songs)
2. **Scrobbling Bridge** — Reads per-user listens from Navidrome and scrobbles to Last.fm and ListenBrainz (Navidrome can't do per-user scrobbling natively)
3. **Recommendation Engine** — Aggregates suggestions from Last.fm, ListenBrainz, Audiomuse-AI, and optionally an OpenAI-compatible API based on listening history and playlists
4. **Download Pipeline** — Lidarr (via Tubifarry plugin for slskd, plus torrents/Usenet) with octo-fiesta as backup; direct slskd for untrackable media; Picard/OneTagger for metadata tagging of slskd downloads

## Deferred Features (design for but don't implement in v1)

- Stats/analytics dashboard
- Jellyfin support
- Download completion notifications
- Playlist import from Spotify/YT Music
- OpenAI-compatible LLM recommendation API with pre-filled prompts

## External Integrations (all REST)

| Service | Purpose |
|---|---|
| Navidrome | User accounts, playback history |
| Lidarr | Artist/album download management |
| slskd | Direct Soulseek downloads |
| MusicBrainz | Metadata source |
| Picard/OneTagger | Metadata tagging |
| Last.fm | Scrobbling, recommendations |
| ListenBrainz | Scrobbling, recommendations |
| Audiomuse-AI | Music discovery suggestions |

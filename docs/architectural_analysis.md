# LLM provided architectural analysis
This analysis was created going back and forth on my plans for Naviseerr with an LLM. I do not use AI blindly for anything but rubber ducking and prototyping.


---

This is a highly viable, albeit complex, architectural challenge. The "Holy Grail" of self-hosted music is bridging the gap between **Acquisition** (Lidarr/Slskd), **Consumption** (Navidrome), and **Discovery** (Recommendations). Existing tools usually do two of these, but rarely all three in a unified user loop.

Here is my architectural evaluation and design proposal for **Naviseerr**.

---

# Naviseerr: Architectural Design Document

## 1. High-Level Architecture
The system will function as an **Orchestrator**. It does not play music, and it does not store music files permanently. It sits in the middle of your stack, managing state, metadata, and user intent.

### The "Dual-Pipeline" Strategy
The biggest hurdle in this ecosystem is Lidarr’s strict adherence to MusicBrainz release groups. To support single songs or non-standard releases (Soulseek’s strength), Naviseerr must implement two distinct download pipelines:

1.  ** The Strict Pipeline (Albums/Artists):**
    *   **Source:** MusicBrainz.
    *   **Executor:** Lidarr.
    *   **Mechanism:** Naviseerr pushes a release to Lidarr via API. Lidarr grabs it via Prowlarr/Usenet or Slskd (via the Tubifarry plugin). Lidarr handles the renaming/moving.
2.  **The Ad-Hoc Pipeline (Singles/Bootlegs):**
    *   **Source:** Direct Search (Slskd) or "Vibes".
    *   **Executor:** Naviseerr internal logic + Slskd + OneTagger.
    *   **Mechanism:** Naviseerr instructs Slskd to download specific files to a "Staging Area." Once complete, Naviseerr triggers a tagging process (OneTagger CLI) and moves the file to a specific "Singles" folder mapped to Navidrome.

## 2. Tech Stack & Implementation Details

### Backend: Spring Boot 3 (Kotlin)
Kotlin is the perfect choice here. It provides the null-safety and conciseness required for complex logic while maintaining the raw IO performance and threading capabilities (Virtual Threads/Coroutines) needed for handling multiple download streams and heavy metadata parsing.

*   **Core:** Spring Boot 4.x.
*   **Database:** PostgreSQL (Essential for relational mapping between Users, Requests, and MusicBrainz IDs).
*   **Scheduling:** Quartz or Spring `@Scheduled` for syncing history and recommendation engines.
*   **API Client:** `Feign` (synchronous) or `WebClient` (reactive) for communicating with Lidarr, Navidrome, Last.fm, and OpenAI-compatible endpoints.
*   **LLM Integration:** Spring AI or LangChain4j (Java/Kotlin) to handle the REST abstraction for MCP/OpenAI communication.

### Frontend: Vue 3 + Quasar (TypeScript)
We will strictly enforce TypeScript.
*   **Framework:** Quasar. It is chosen over Svelte for this specific use case because Quasar offers a massive, enterprise-grade component library (Data Tables, Dialogs, Media Cards) out of the box. We want to build an interface, not design UI components from scratch.
*   **State Management:** Pinia (standard for Vue 3).
*   **API Layer:** Generated TypeScript clients based on the Spring Boot OpenAPI (Swagger) spec.

## 3. Core Modules

### A. The Identity & Sync Module
Instead of maintaining a separate user database, we will treat Navidrome as the "Identity Provider" (IDP) where possible, or mirror it.
*   **Auth:** Users log in with Navidrome credentials. Naviseerr verifies this against the Navidrome Subsonic API.
*   **History Sync:** A background job runs every X minutes. It queries the Navidrome Subsonic API (`getNowPlaying` and `getScrobbleTimeline`) to populate a local `listening_history` table in Postgres.
*   **Scrobble Dispatcher:** When a new listen is detected in Navidrome, Naviseerr pushes it to Last.fm/ListenBrainz on behalf of the user (requires storing user tokens for these services).

### B. The Discovery Engine
This is the "Brain" of Naviseerr. It aggregates data to generate a `RecommendationScore`.
*   **Inputs:**
    1.  **Calculated Affinity:** (From History) "User listens to 80% Synthwave."
    2.  **External Recommendations:** Fetch "Similar Artists" from Last.fm/ListenBrainz APIs.
    3.  **LLM Vibe Check:** A user prompts "Give me sad robot music." Naviseerr sends a structured prompt to an OpenAI-compatible API. The response is parsed into Artist/Track names, then matched against MusicBrainz.
    4.  **Audiomuse-AI:** If Audiomuse exposes an API (or we read its generated database/embeddings), we use this to find sonic similarity for tracks already in the library.

### C. The Acquisition Manager (The Pipelines)
This module decides *how* to download content.
*   **Logic:**
    *   If the user requests an **Album** exists in MusicBrainz -> Send to **Lidarr**.
    *   If the user requests a **Single Track** OR the release is not in MB -> Send to **Slskd** directly.

### D. The Tagger Implementation (Ad-Hoc Pipeline)
Since we cannot use Python code, we cannot use the `mutagen` library directly. We will use **OneTagger** (written in Rust, high performance) or **Picard** (headless).
*   **Implementation:**
    1.  Naviseerr monitors the Slskd download folder.
    2.  Upon completion, Kotlin invokes `ProcessBuilder` to run the OneTagger CLI binary.
    3.  Arguments are passed dynamically: `--artist "X" --title "Y" --input-dir "/downloads" --output-dir "/library"`.
    4.  Naviseerr triggers a specific folder scan in Navidrome (using the `scan` API endpoint).

## 4. User Journey & Feature Mapping

### Phase 1: The Request
*   **UI:** User searches for "Daft Punk".
*   **Backend:** Queries MusicBrainz. Checks Lidarr if it's already monitored. Checks Navidrome if it's already available.
*   **Action:** User clicks "Request Album".
*   **Result:** Naviseerr calls Lidarr API `POST /album`.

### Phase 2: The "Missing Link" (Slskd Direct)
*   **UI:** User wants a specific remix not on MusicBrainz.
*   **Action:** User selects "Manual Search" (wrapping Slskd search). User clicks "Download".
*   **Backend:** Naviseerr calls Slskd API `POST /transfers`.
*   **Post-Processing:** Naviseerr waits for transfer completion. It moves the file to a temporary directory. It prompts the user (via UI notification) to confirm metadata: "Is this *Get Lucky (Radio Edit)*?"
*   **Tagging:** Naviseerr runs the CLI tagger and moves the file to `/music/commons/singles/`.

### Phase 3: The Feedback Loop
*   **Analysis:** The user listens to the new download. Naviseerr detects this via the Navidrome sync.
*   **Stats:** The `UserStats` table is updated.
*   **Recommendation update:** The "For You" dashboard refreshes. It queries the `Discovery` module. "Since you liked Daft Punk, here is Justice (via Last.fm) and Kavinsky (via LLM Vibe Match)."

## 5. Deployment Architecture (Docker Compose)

To ensure this is easy to self-host, the stack is containerized.

```yaml
services:
  naviseerr:
    image: naviseerr/core:latest
    volumes:
      - /music:/music
      - /downloads:/downloads
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/naviseerr
      - LIDARR_URL=http://lidarr:8686
      - SLSKD_URL=http://slskd:5030
      - NAVIDROME_URL=http://navidrome:4533
      - ONETAGGER_PATH=/usr/bin/onetagger

  db:
    image: postgres:17-alpine

  # Integrated dependencies for the "Full Stack" experience
  onetagger-service:
    # A sidecar container or integrated binary in the main image
    # allowing CLI execution
```

## 6. Challenges & Solutions

| Challenge | Solution |
| :--- | :--- |
| **Lidarr Single File Limits** | Bypass Lidarr for singles. Use direct Slskd API + CLI Tagging (OneTagger). |
| **Metadata Quality** | For the Ad-Hoc pipeline, force a UI "Confirm Metadata" step before moving to the library to ensure the CLI tagger gets the right arguments. |
| **Latency** | Navidrome API polling can be slow. Use optimistic UI updates in Naviseerr (mark as "Listened" immediately if played from Naviseerr Web UI) and eventual consistency for external players. |
| **Python Avoidance** | Strictly use Kotlin for logic. Use `ProcessBuilder` or HTTP REST calls for any tool that happens to be written in Python (like Octo-fiesta if used as a backup provider). |

## 7. Future Proofing (The Non-MVP list)

1.  **Octo-Fiesta Integration:** If Usenet/Torrents/Slskd fail, Naviseerr can trigger an Octo-fiesta instance (running in its own container) via webhook to attempt a "brute force" search, then ingest the result.
2.  **Playlist Import:** A module to parse a pasted Spotify URL, resolve ISRCs against MusicBrainz, and bulk-request the missing items.
3.  **Federation:** Sharing recommendations between different self-hosted Naviseerr instances.

## Conclusion
This architecture is entirely feasible. The complexity lies not in the code logic, but in the robust error handling required when orchestrating three different external APIs (Lidarr, Slskd, Navidrome) and a CLI tool (OneTagger). By sticking to **Spring Boot/Kotlin**, we gain the robustness needed for this orchestration. By using **Vue3/Quasar**, we ensure a responsive, app-like experience that feels like a premium streaming service.

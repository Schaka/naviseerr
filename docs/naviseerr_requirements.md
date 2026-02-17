You are a senior software architect intimately familiar with the self hosted open source eco system regarding music, like (but not limited to) Navidrome, Jellyfin, Lidarr (with Plugins) and Slskd for downloading via Soulseek.
You're also aware of all methods using music discovery, like (but not limited to) Last.fm recommendations, Audiomuse-AI.

Evaluate the possbility of designing an application that's completely self hosted and can replace popular music streaming services for most people. Let's call it Naviseerr for now.

The application/system has the following requirement:
- Musicbrainz as metadata source, other sources if available
- multi user, with each user being able to request artists/albums not in the shared library
- able to use an existing tool to properly tag downloaded music with metadata (only for slskd downloads)
- get suggestions per user, based on their listening habits
- music is downloaded via adding to Lidarr, which can then download via slskd (via Tubifarry plugin), torrents or Usenet with potentially octo-fiesta as a backup
- ability to directly download via slskd if media can't be added to Lidarr (i.e. no musicbrainz entry) or for single song support, which Lidarr does not allow
- history/stats of who/what a user listened to

A typical user journey would look like this:
- logs into Naviseerr, requests some artists => sent to Lidarr, downloaded via Slskd, tagged with Picard or similar
- logs into Navidrome, listens to some music => scrobbled to Last.fm and Listbrainz, analyzed with Audiomuse-AI
- logs back into Naviseerr, sees an overview of their listening history, recommendations are now available based on their past requests and listening history from Navidrome
- they discover a new artist and request it, it gets pulled into the library via Lidarr/Slskd => user listens to it and favorites it in their player/Navidrome
- automated personalized recommendations are further adjusted on history
- recommendations are created by user on demand (i.e. based on existing playlist, some selected artists against Audiomuse-AI or OpenAI compatible API request)


Some things to consider:
- to get recommendations from Last.fm, there likely needs to be some functionality reading user' listens from Navidrome/Jellyfin and scrolling them to Last.fm because Navidrome can't do this per user
- accounts could be pulled from Navidrome/Jellyfin, instead of creating a Naviseerr account for each user
- similarly scrobble to Listenbrainz
- app will likely need several sections for different ways to add new music to the library
  - search manually for artists/albums
  - Spotify/Tidal charts/top 100
  - Last.fm recommendations
  - Listenbrainz recommendations
  - Audiomuse-AI suggestions for songs in library or from their shared database
- some form of OpenAI compatible API support to pull artists from, based on "vibes" or a specific prompt
- the entire "stack" of software that Naviseerr slots into can contain several existing (well maintained) open source projects

Software to take inspiration from:
- Sonobarr
- Seerr (for requesting content via Lidarr)
- OneTagger and Picard for tagging music with metadata
- [Explo](https://github.com/LumePart/Explo)
- [DiscoveryLastFM](https://github.com/MrRobotoGit/DiscoveryLastFM)
- [SoulSync](https://github.com/Nezreka/SoulSync)
- Octo-fiesta for Navidrome or Allstarr for Jellyfin

Tech stack:
- avoid Python use in our own code at all cost
- no raw Javascript whatsoever - anything interpreted as Javascript should be purely transpiled from Typescript
- use Vue3 with Quasar for the frontend or Svelte if that's a better fit
- Spring Boot with Kotlin - it's what developers are familiar with and performs well in an app full of background tasks
- no direct access to any LLM - all communication should be done via REST or MCP

Features to not include in a first iteration but consider when designing the architecture:
- stats/analytics in Naviseerr
- Jellyfin support
- notifications that requested music is now available
- import of Spotify or YT Music Playlists
- OpenAI compatible API to ask an LLM of the user's choice for recommendations, with pre-filled prompt from recent listens, playlist, etc
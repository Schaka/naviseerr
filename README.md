# Naviseerr - self-hosted music manager

<p align="center">
    <img src="logos/naviseerr_icon.png" width=384>
</p>

## Introduction

### Logging

To enable debug logging, change `INFO` following line in `application.yml` to either `DEBUG` or `TRACE`:

```yml
    com.github.schaka: INFO
```

### What's going on here? Is this a usable project?

Right now, it's not. There are some plans to combine a bunch of simple scripts into one more usable apps.
The initial inspiration was Overseerr, Navidrome (hence the name), Lidarr, Lidify, Soularr and slsdk.

So far, it can only download your Lidarr "Wanted" list via Slskd. Matching is still inconsistent and functionality like retries on error'd downloads haven't been implemented yet.
However, matching is pretty good so far.

### Get started on testing base functionality

Currently, the code is only published as a docker image to [GitHub](https://github.com/Schaka/naviseerr/pkgs/container/naviseerr).
If you cannot use Docker, you'll have to compile it yourself from source.

### Setting up Docker

- follow the mapping for `application.yml` examples below
- within that host folder, put a copy of [application.yml](https://github.com/Schaka/naviseerr/blob/develop/src/main/resources/application-template.yml) from this repository
- adjust said copy with your own servers

Slskd and Lidarr should have access to the same folders with the same mappings. Naviseerr does not attempt to map between directories and does NOT touch any files itself.

### So what's the roadmap?

#### For 1.0
- Users duplicated from Navidrome, or if possible direct authentication against Navidrome
- Use Lidarr as media manager to send requests to, similar to Overseerr
- Create a local copy of Lidarr library to treat as "the truth" and populate with additional metadata from LastFM and Spotify
- Scan Lidarr's "Wanted" list and use slsdk API to pull media, then trigger manual import via Lidarr API (or copy + ) - using metadata from above library
- Import to Navidrome, if Navidrome isn't pointed towards Lidarr library already
- Use Spotify API to make recommendations on existing library, existing Navidrome playlists and past Naviseerr requests

### For 2.0
- Replace Lidarr with internal Naviseerr media manager, to avoid dependency on one more app (Lidarr) and Musicbrainz by extension
- Integration with Navidrome to keep track of what is being played, to make Spotify API recommendations based on user-plays


## JetBrains
Thank you to [<img src="logos/jetbrains.svg" alt="JetBrains" width="32"> JetBrains](http://www.jetbrains.com/) for providing us with free licenses to their great tools.

* [<img src="logos/idea.svg" alt="Idea" width="32"> IntelliJ Idea](https://www.jetbrains.com/idea/)
* [<img src="logos/webstorm.svg" alt="WebStorm" width="32"> WebStorm](http://www.jetbrains.com/webstorm/)
* [<img src="logos/rider.svg" alt="Rider" width="32"> Rider](http://www.jetbrains.com/rider/)

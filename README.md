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
However, matching is in a state usable enough to start out populating your library.

### Development
The Quasar/VueJS frontend is compiled as part of the build process. There is no separate build step and no reverse proxy hiding 2 servers.
While not at all required, it is recommended to run a separate frontend server during development.

- install NodeJS 22.10+
- `corepack enable pnpm`
- navigate to `src/main/frontend` and `pnpm dev` to start the server

If you require the backend to be running, the easiest way is
`./gradlew bootRun`

However, I highly recommend IntelliJ Idea (Community Edition), as it integrates fairly seamlessly with both.

**I have no idea what I'm doing regarding frontend, so expect bad practices and general ugliness.**

### So what's the roadmap?

#### For 1.0
- [x] Users duplicated from Navidrome, or if possible direct authentication against Navidrome
- [x] Create a local copy of Lidarr library to treat as "the truth"
- [ ] ...and populate with additional metadata from LastFM and Spotify
- [x] Scan Lidarr's "Wanted" list and use slsdk API to pull media, then trigger manual import via Lidarr API (or copy) - using metadata from above library
- [ ] Use Lidarr as media manager to send requests to, similar to Overseerr
- [ ] Import to Navidrome, if Navidrome isn't pointed towards Lidarr library already
- [ ] Use Spotify API to make recommendations on existing library, existing Navidrome playlists and past Naviseerr requests

### For 2.0
- Replace Lidarr with internal Naviseerr media manager, to avoid dependency on one more app (Lidarr) and Musicbrainz by extension
- Integration with Navidrome to keep track of what is being played, to make Spotify API recommendations based on user-plays

### Get started on testing base functionality

Currently, the code is only published as a docker image to [GitHub](https://github.com/Schaka/naviseerr/pkgs/container/naviseerr).
If you cannot use Docker, you'll have to compile it yourself from source.

Only a JVM based image is published at this point. A GraalVM native image requires too many workarounds at this point.
Speeding up development times and making sure everything works as expected after running locally is more important at this stage of the project.
I may pick up a native image build later on again.

### Setting up Docker

- follow the mapping for `application.yml` examples below
- within that host folder, put a copy of [application.yml](https://github.com/Schaka/naviseerr/blob/develop/src/main/resources/application-template.yml) from this repository
- adjust said copy with your own servers

**Slskd and Lidarr should have access to the same folders with the same mappings. Naviseerr does not attempt to map between directories and does NOT touch any files itself.**

### Docker config

Before using this, please make sure you've created the `application.yml` file and put it in the correct config directory you intend to map.
The application requires it. You need to supply it, or Naviseerr will not start correctly.


An example of a `docker-compose.yml` may look like this:

```yml
version: '3'

services:
  naviseerr:
    container_name: naviseerr
    image: ghcr.io/schaka/naviseerr:stable
    user: 1000:1000 # Replace with your user who should own your application.yml file
    volumes:
      # Make sure those folders already exist. Otherwise Docker may create them as root and they will not be writeable by Naviseerr
      - /appdata/naviseerr/config/application.yml:/workspace/application.yml
      - /appdata/naviseerr/logs:/workspace/logs
      - /appdata/naviseerr/database:/workspace/database
      - /share_media:/data
    environment:
      # Uses https://github.com/dmikusa/tiny-health-checker supplied by paketo buildpacks
      - THC_PATH=/health
      - THC_PORT=8081
    healthcheck:
      test: [ "CMD", "/workspace/health-check" ]
      start_period: 30s
      interval: 5s
      retries: 3
```

To get the latest build as found in the development branch, grab the following image: `ghcr.io/schaka/naviseerr:develop`.

## JetBrains
Thank you to [<img src="logos/jetbrains.svg" alt="JetBrains" width="32"> JetBrains](http://www.jetbrains.com/) for providing us with free licenses to their great tools.

* [<img src="logos/idea.svg" alt="Idea" width="32"> IntelliJ Idea](https://www.jetbrains.com/idea/)
* [<img src="logos/webstorm.svg" alt="WebStorm" width="32"> WebStorm](http://www.jetbrains.com/webstorm/)
* [<img src="logos/rider.svg" alt="Rider" width="32"> Rider](http://www.jetbrains.com/rider/)

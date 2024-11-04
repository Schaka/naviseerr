CREATE TABLE artists(
  id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
  hash INT NOT NULL,
  lidarr_id BIGINT NOT NULL,
  navidrome_id VARCHAR(255), -- may not be available until Navidrome succesfully scans and identifies
  spotify_id VARCHAR(255),
  musicbrainz_id VARCHAR(255),
  last_fm_id BIGINT,
  name VARCHAR(511) NOT NULL,
  path VARCHAR(1023) NOT NULL
);

CREATE INDEX artists_lidarr_id ON artists(lidarr_id);
CREATE INDEX artists_navidrome_id ON artists(navidrome_id);
CREATE INDEX artists_spotify_id ON artists(spotify_id);
CREATE INDEX artists_musicbrainz_id ON artists(musicbrainz_id);
CREATE INDEX artists_last_fm_id ON artists(last_fm_id);

// we don't keep track of each file that's part of a release
// each release may contain more (or different) files than listed on Musicbrainz anyway
CREATE TABLE releases(
    id BIGINT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    hash INT NOT NULL,
    artist_id BIGINT NOT NULL,
    lidarr_id BIGINT UNIQUE NOT NULL,
    navidrome_id VARCHAR(255), -- may not be available until Navidrome successfully scans and identifies
    spotify_id VARCHAR(255),
    musicbrainz_id VARCHAR(255),
    last_fm_id BIGINT,
    name VARCHAR(511) NOT NULL,
    path VARCHAR(1023) NOT NULL,
    type VARCHAR(255) NOT NULL,
    complete BOOLEAN NOT NULL DEFAULT FALSE,
    highest_quality_available BOOLEAN NOT NULL DEFAULT FALSE,
    last_download_attempt DATETIME,

    FOREIGN KEY(artist_id) REFERENCES artists(id)
);

CREATE INDEX releases_lidarr_id ON releases(lidarr_id);
CREATE INDEX releases_navidrome_id ON releases(navidrome_id);
CREATE INDEX releases_spotify_id ON releases(spotify_id);
CREATE INDEX releases_musicbrainz_id ON releases(musicbrainz_id);
CREATE INDEX releases_last_fm_id ON releases(last_fm_id);


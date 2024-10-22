CREATE TABLE artists(
  id BIGINT PRIMARY KEY NOT NULL,
  lidarr_id VARCHAR(255) NOT NULL,
  navidrome_id VARCHAR(255), -- may not be available until Navidrome succesfully scans and identifies
  spotify_id VARCHAR(255),
  musicbrainz_id VARCHAR(255),
  last_fm_id BIGINT,
  name VARCHAR(511) NOT NULL,
  path VARCHAR(1023) NOT NULL
);

// we don't keep track of each file that's part of a release
// each release may contain more (or different) files than listed on Musicbrainz anyway
CREATE TABLE releases(
    id BIGINT PRIMARY KEY NOT NULL,
    lidarr_id VARCHAR(255) NOT NULL,
    navidrome_id VARCHAR(255), -- may not be available until Navidrome succesfully scans and identifies
    spotify_id VARCHAR(255),
    musicbrainz_id VARCHAR(255),
    last_fm_id BIGINT,
    name VARCHAR(511) NOT NULL,
    path VARCHAR(1023) NOT NULL,
    type VARCHAR(255) NOT NULL,
    complete BOOLEAN NOT NULL DEFAULT FALSE,
    highest_quality_available BOOLEAN NOT NULL DEFAULT FALSE,
    artist_id BIGINT,

    FOREIGN KEY(artist_id) REFERENCES artists(id)
)


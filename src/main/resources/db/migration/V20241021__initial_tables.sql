CREATE TABLE artists(
  id BIGINT PRIMARY KEY NOT NULL,
  name VARCHAR(1024) NOT NULL,
  musicbrainz_id BIGINT,
  last_fm_id BIGINT,
  spotify_id BIGINT
);

CREATE TABLE releases(
    id BIGINT PRIMARY KEY NOT NULL,
    name VARCHAR(1024) NOT NULL,
    type VARCHAR(255) NOT NULL,
    musicbrainz_id BIGINT,
    last_fm_id BIGINT,
    spotify_id BIGINT,
    artist_id BIGINT,

    FOREIGN KEY(artist_id) REFERENCES artists(id)
)
CREATE TABLE naviseerr_users (
     id UUID PRIMARY KEY,
     username VARCHAR(255) NOT NULL UNIQUE,
     navidrome_id VARCHAR(255) NOT NULL,
     navidrome_token VARCHAR(255) NOT NULL,
     subsonic_token VARCHAR(255) NOT NULL,
     subsonic_salt VARCHAR(255) NOT NULL,
     last_login TIMESTAMP NOT NULL,
     last_fm_session_key VARCHAR(255),
     listenbrainz_api_key VARCHAR(255),
     last_fm_scrobbling_enabled BOOLEAN NOT NULL DEFAULT TRUE,
     listenbrainz_scrobbling_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_plays (
     id UUID PRIMARY KEY,
     user_id UUID NOT NULL REFERENCES naviseerr_users(id),
     navidrome_play_id VARCHAR(255) NOT NULL UNIQUE,
     track_id VARCHAR(255) NOT NULL,
     track_name VARCHAR(512) NOT NULL,
     artist_name VARCHAR(512) NOT NULL,
     album_name VARCHAR(512),
     duration INTEGER NOT NULL,
     played_at TIMESTAMP NOT NULL,
     musicbrainz_track_id VARCHAR(36),
     musicbrainz_artist_id VARCHAR(36),
     musicbrainz_album_id VARCHAR(36),
     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_plays_user_id ON user_plays(user_id);
CREATE INDEX idx_user_plays_played_at ON user_plays(played_at);

CREATE TABLE library_artists (
    id UUID PRIMARY KEY,
    musicbrainz_id VARCHAR(36) UNIQUE,
    lidarr_id BIGINT UNIQUE,
    name VARCHAR(512) NOT NULL,
    clean_name VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    source VARCHAR(32) NOT NULL DEFAULT 'LIDARR',
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_library_artists_mb_id ON library_artists(musicbrainz_id);

CREATE TABLE library_albums (
    id UUID PRIMARY KEY,
    artist_id UUID NOT NULL REFERENCES library_artists(id),
    musicbrainz_id VARCHAR(36) UNIQUE,
    lidarr_id BIGINT UNIQUE,
    title VARCHAR(512) NOT NULL,
    album_type VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    source VARCHAR(32) NOT NULL DEFAULT 'LIDARR',
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_library_albums_mb_id ON library_albums(musicbrainz_id);
CREATE INDEX idx_library_albums_artist_id ON library_albums(artist_id);

CREATE TABLE media_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES naviseerr_users(id),
    artist_id UUID REFERENCES library_artists(id),
    album_id UUID REFERENCES library_albums(id),
    musicbrainz_artist_id VARCHAR(36) NOT NULL,
    musicbrainz_album_id VARCHAR(36),
    artist_name VARCHAR(512) NOT NULL,
    album_title VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    lidarr_artist_id BIGINT,
    lidarr_album_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_media_requests_user_id ON media_requests(user_id);
CREATE INDEX idx_media_requests_status ON media_requests(status);
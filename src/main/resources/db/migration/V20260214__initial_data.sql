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
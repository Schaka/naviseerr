use std::path::PathBuf;

pub struct Config {
    pub port: u16,
    pub onetagger_binary: PathBuf,
    pub onetagger_default_config: PathBuf,
    pub tagger_script: PathBuf,
    pub acoustid_api_key: String,
}

impl Config {
    pub fn from_env() -> Self {
        Self {
            port: env_or("TAGGING_SERVICE_PORT", "8090")
                .parse()
                .expect("TAGGING_SERVICE_PORT must be a valid port number"),
            onetagger_binary: PathBuf::from(env_or("ONETAGGER_BINARY", "/usr/local/bin/onetagger-cli")),
            onetagger_default_config: PathBuf::from(env_or("ONETAGGER_DEFAULT_CONFIG", "/config/autotagger.json")),
            tagger_script: PathBuf::from(env_or("TAGGER_SCRIPT", "/app/scripts/tagger.py")),
            acoustid_api_key: env_or("ACOUSTID_API_KEY", ""),
        }
    }

    pub fn onetagger_available(&self) -> bool {
        self.onetagger_binary.exists()
    }
}

fn env_or(key: &str, default: &str) -> String {
    std::env::var(key).unwrap_or_else(|_| default.to_string())
}

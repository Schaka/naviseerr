use std::path::Path;

use tokio::process::Command;

use crate::config::Config;

pub fn build_fingerprint_command(config: &Config, path: &Path) -> Command {
    let mut cmd = Command::new("python3");
    cmd.arg(&config.tagger_script);
    cmd.arg("fingerprint");
    cmd.arg(path);
    cmd.env("ACOUSTID_API_KEY", &config.acoustid_api_key);
    cmd
}

pub fn build_tag_command(config: &Config, path: &Path) -> Command {
    let mut cmd = Command::new("python3");
    cmd.arg(&config.tagger_script);
    cmd.arg("tag");
    cmd.arg(path);
    cmd.env("ACOUSTID_API_KEY", &config.acoustid_api_key);
    cmd
}

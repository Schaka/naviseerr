use std::path::Path;

use tokio::process::Command;

use crate::config::Config;

pub fn build_command(config: &Config, path: &Path, config_path: Option<&Path>) -> Command {
    let mut cmd = Command::new(&config.onetagger_binary);
    cmd.arg("autotagger");
    cmd.arg("--path").arg(path);

    let cfg = config_path.unwrap_or(&config.onetagger_default_config);
    cmd.arg("--config").arg(cfg);

    cmd
}

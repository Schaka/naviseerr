use std::path::PathBuf;

use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OneTaggerRequest {
    pub path: PathBuf,
    pub config_path: Option<PathBuf>,
}

#[derive(Deserialize)]
pub struct TaggerRequest {
    pub path: PathBuf,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TaggingResponse {
    pub success: bool,
    pub exit_code: i32,
    pub stdout: String,
    pub stderr: String,
    pub duration_ms: u64,
}

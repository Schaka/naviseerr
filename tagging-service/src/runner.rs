use std::time::Instant;

use tokio::process::Command;

use crate::error::AppError;
use crate::models::TaggingResponse;

pub async fn run_command(mut cmd: Command) -> Result<TaggingResponse, AppError> {
    let start = Instant::now();

    let output = cmd
        .output()
        .await
        .map_err(|e| AppError::ExecutionFailed(format!("Failed to spawn process: {e}")))?;

    let duration_ms = start.elapsed().as_millis() as u64;
    let exit_code = output.status.code().unwrap_or(-1);

    Ok(TaggingResponse {
        success: output.status.success(),
        exit_code,
        stdout: String::from_utf8_lossy(&output.stdout).into_owned(),
        stderr: String::from_utf8_lossy(&output.stderr).into_owned(),
        duration_ms,
    })
}

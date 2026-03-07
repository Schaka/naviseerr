use std::sync::Arc;

use axum::Json;
use tokio::sync::Semaphore;
use tracing::info;

use crate::config::Config;
use crate::error::AppError;
use crate::models::{OneTaggerRequest, TaggingResponse, TaggerRequest};
use crate::{onetagger, runner, tagger};

pub struct AppState {
    pub config: Config,
    pub semaphore: Semaphore,
}

pub async fn health() -> &'static str {
    "OK"
}

pub async fn onetagger_tag(
    state: Arc<AppState>,
    Json(req): Json<OneTaggerRequest>,
) -> Result<Json<TaggingResponse>, AppError> {
    if !state.config.onetagger_available() {
        return Err(AppError::NotImplemented(
            "OneTagger is not available on this platform (arm64)".to_string(),
        ));
    }

    validate_path(&req.path)?;
    if let Some(ref cfg) = req.config_path {
        validate_path(cfg)?;
    }

    info!("OneTagger tag request for: {}", req.path.display());

    let _permit = state.semaphore.acquire().await.expect("semaphore closed");
    let cmd = onetagger::build_command(&state.config, &req.path, req.config_path.as_deref());
    let response = runner::run_command(cmd).await?;

    info!(
        "OneTagger completed: success={}, exit_code={}, duration={}ms",
        response.success, response.exit_code, response.duration_ms
    );

    Ok(Json(response))
}

pub async fn fingerprint(
    state: Arc<AppState>,
    Json(req): Json<TaggerRequest>,
) -> Result<Json<TaggingResponse>, AppError> {
    validate_path(&req.path)?;

    info!("Fingerprint request for: {}", req.path.display());

    let _permit = state.semaphore.acquire().await.expect("semaphore closed");
    let cmd = tagger::build_fingerprint_command(&state.config, &req.path);
    let response = runner::run_command(cmd).await?;

    info!(
        "Fingerprint completed: success={}, exit_code={}, duration={}ms",
        response.success, response.exit_code, response.duration_ms
    );

    Ok(Json(response))
}

pub async fn tag(
    state: Arc<AppState>,
    Json(req): Json<TaggerRequest>,
) -> Result<Json<TaggingResponse>, AppError> {
    validate_path(&req.path)?;

    info!("Tag request for: {}", req.path.display());

    let _permit = state.semaphore.acquire().await.expect("semaphore closed");
    let cmd = tagger::build_tag_command(&state.config, &req.path);
    let response = runner::run_command(cmd).await?;

    info!(
        "Tag completed: success={}, exit_code={}, duration={}ms",
        response.success, response.exit_code, response.duration_ms
    );

    Ok(Json(response))
}

fn validate_path(path: &std::path::Path) -> Result<(), AppError> {
    if !path.exists() {
        return Err(AppError::PathNotFound(format!(
            "Path does not exist: {}",
            path.display()
        )));
    }
    Ok(())
}

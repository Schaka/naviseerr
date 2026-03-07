mod config;
mod error;
mod handlers;
mod models;
mod onetagger;
mod runner;
mod tagger;

use std::sync::Arc;

use axum::routing::{get, post};
use axum::Router;
use tokio::sync::Semaphore;
use tower_http::trace::TraceLayer;
use tracing::info;
use tracing_subscriber::EnvFilter;

use crate::config::Config;
use crate::handlers::AppState;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")))
        .init();

    let config = Config::from_env();
    let port = config.port;

    if config.acoustid_api_key.is_empty() {
        tracing::warn!("ACOUSTID_API_KEY is not set — fingerprint lookups will fail");
    }

    if !config.onetagger_available() {
        tracing::warn!(
            "OneTagger binary not found at {} — OneTagger endpoints will return 501",
            config.onetagger_binary.display()
        );
    }

    let state = Arc::new(AppState {
        config,
        semaphore: Semaphore::new(1),
    });

    let app = Router::new()
        .route("/health", get(handlers::health))
        .route(
            "/api/v1/onetagger/tag",
            post({
                let state = Arc::clone(&state);
                move |body| handlers::onetagger_tag(state, body)
            }),
        )
        .route(
            "/api/v1/fingerprint",
            post({
                let state = Arc::clone(&state);
                move |body| handlers::fingerprint(state, body)
            }),
        )
        .route(
            "/api/v1/tag",
            post({
                let state = Arc::clone(&state);
                move |body| handlers::tag(state, body)
            }),
        )
        .layer(TraceLayer::new_for_http());

    let addr = format!("0.0.0.0:{port}");
    info!("Tagging service listening on {addr}");

    let listener = tokio::net::TcpListener::bind(&addr).await.expect("failed to bind");
    axum::serve(listener, app).await.expect("server error");
}

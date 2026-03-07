use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use serde_json::json;

pub enum AppError {
    PathNotFound(String),
    NotImplemented(String),
    ExecutionFailed(String),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match self {
            AppError::PathNotFound(msg) => (StatusCode::BAD_REQUEST, msg),
            AppError::NotImplemented(msg) => (StatusCode::NOT_IMPLEMENTED, msg),
            AppError::ExecutionFailed(msg) => (StatusCode::INTERNAL_SERVER_ERROR, msg),
        };

        let body = axum::Json(json!({
            "success": false,
            "error": message,
        }));

        (status, body).into_response()
    }
}

ARG RUST_VERSION=1.87

# ---- Builder stage ----
FROM rust:${RUST_VERSION}-bookworm AS builder

WORKDIR /build
COPY Cargo.toml Cargo.lock* ./
COPY src/ src/

RUN cargo build --release

# ---- Runtime stage ----
FROM ubuntu:24.04

ARG TARGETARCH
ARG ONETAGGER_VERSION=1.7.0

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    libchromaprint-tools \
    libasound2t64 \
    curl \
    gosu \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY scripts/requirements.txt /tmp/requirements.txt
RUN python3 -m pip install --no-cache-dir --break-system-packages -r /tmp/requirements.txt \
    && rm /tmp/requirements.txt

RUN if [ "$TARGETARCH" = "amd64" ]; then \
      curl -fsSL "https://github.com/Marekkon5/onetagger/releases/download/${ONETAGGER_VERSION}/OneTagger-linux-cli.tar.gz" \
        | tar -xz -C /usr/local/bin/ \
      && chmod +x /usr/local/bin/onetagger-cli; \
    else \
      echo "OneTagger not available for ${TARGETARCH}"; \
    fi

COPY --from=builder /build/target/release/tagging-service /usr/local/bin/tagging-service
COPY scripts/ /app/scripts/
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh /app/scripts/tagger.py

ENV TAGGING_SERVICE_PORT=8090
ENV ONETAGGER_BINARY=/usr/local/bin/onetagger-cli
ENV ONETAGGER_DEFAULT_CONFIG=/config/autotagger.json
ENV TAGGER_SCRIPT=/app/scripts/tagger.py

EXPOSE 8090

ENTRYPOINT ["/entrypoint.sh"]

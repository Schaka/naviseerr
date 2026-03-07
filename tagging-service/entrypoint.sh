#!/bin/bash
set -e

PUID=${PUID:-1000}
PGID=${PGID:-1000}

groupadd -o -g "$PGID" tagging 2>/dev/null || true
useradd -o -u "$PUID" -g "$PGID" -m tagging 2>/dev/null || true

exec gosu tagging /usr/local/bin/tagging-service

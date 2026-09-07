#!/usr/bin/env bash
# Backs up the SQLite database of a running door-codes-manager container.
# Usage: scripts/backup.sh <container-name-or-id> [output-file]
set -euo pipefail

container="${1:?Usage: $0 <container-name-or-id> [output-file]}"
out="${2:-backup_door_codes_manager_$(date +%Y%m%d%H%M%S).db}"

docker exec "$container" sqlite3 /data/door_codes_manager.db ".backup '/data/$(basename "$out")'"
docker cp "$container:/data/$(basename "$out")" "$out"
docker exec "$container" rm "/data/$(basename "$out")"

echo "Backup written to $out"

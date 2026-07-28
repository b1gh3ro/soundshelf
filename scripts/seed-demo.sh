#!/usr/bin/env bash
#
# Fills the demo account with a library big enough for the analytics dashboard to
# mean something. Only catalog ids are stored here - the API looks up the real
# metadata from iTunes, so the seeded rows go through exactly the same code path
# as a manual save.
#
# Usage:
#   ./scripts/seed-demo.sh                              # against localhost
#   API_BASE=https://your-api.onrender.com ./scripts/seed-demo.sh
#
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
EMAIL="${DEMO_EMAIL:-demo@soundshelf.app}"
PASSWORD="${DEMO_PASSWORD:-demo1234}"
ALBUMS_FILE="$(dirname "$0")/demo-albums.txt"

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 1; }

json_field() { python3 -c "import sys,json;print(json.load(sys.stdin).get('$1',''))" 2>/dev/null || true; }

echo "Seeding $EMAIL at $API_BASE"

# Register is fine to fail - it just means the demo account already exists.
curl -s -o /dev/null -X POST "$API_BASE/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"Demo Listener\"}" || true

TOKEN=$(curl -s -X POST "$API_BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | json_field token)

if [ -z "$TOKEN" ]; then
  echo "Could not log in as $EMAIL - is the API running at $API_BASE?" >&2
  exit 1
fi

saved=0; skipped=0; failed=0

while IFS= read -r line; do
  line="${line%%#*}"                      # strip trailing comment
  line="$(echo "$line" | sed 's/[[:space:]]*$//')"
  [ -z "$line" ] && continue

  id="$(echo "$line" | cut -d'|' -f1)"
  rating="$(echo "$line" | cut -d'|' -f2)"
  notes="$(echo "$line" | cut -d'|' -f3-)"

  body="{\"appleCatalogId\":$id"
  [ "$rating" != "-" ] && [ -n "$rating" ] && body="$body,\"userRating\":$rating"
  [ -n "$notes" ] && body="$body,\"userNotes\":$(python3 -c "import json,sys;print(json.dumps(sys.argv[1]))" "$notes")"
  body="$body}"

  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_BASE/api/library" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$body")

  case "$code" in
    201) saved=$((saved+1)) ;;
    409) skipped=$((skipped+1)) ;;
    *)   failed=$((failed+1)); echo "  ! id $id returned HTTP $code" >&2 ;;
  esac
done < "$ALBUMS_FILE"

echo "Done. saved=$saved already-present=$skipped failed=$failed"
curl -s "$API_BASE/api/analytics/summary" -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json;t=json.load(sys.stdin)['totals'];print(f\"Library now: {t['albums']} albums, {t['artists']} artists, {t['genres']} genres\")"

#!/usr/bin/env bash
# Pack a Magisk / KernelSU / APatch flashable zip from module/.
# Usage: pack-module.sh [--apk path/to/app.apk] [--out path/to/out.zip]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$ROOT/module"
APK=""
OUT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      APK="${2:-}"
      shift 2
      ;;
    --out)
      OUT="${2:-}"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      echo "Usage: pack-module.sh [--apk path/to/app.apk] [--out path/to/out.zip]" >&2
      exit 2
      ;;
  esac
done

VERSION="$(grep '^version=' "$MOD/module.prop" | cut -d= -f2-)"
OUT="${OUT:-$ROOT/module-dist/victorcam-root-module-${VERSION}.zip}"

STAGING="$(mktemp -d)"
cleanup() { rm -rf "$STAGING"; }
trap cleanup EXIT

# Copy module contents. pack-module.sh is a repo helper, not part of the zip.
if command -v rsync >/dev/null 2>&1; then
  rsync -a --exclude 'pack-module.sh' --exclude '.gitkeep' "$MOD/" "$STAGING/"
else
  cp -a "$MOD/." "$STAGING/"
  rm -f "$STAGING/pack-module.sh" "$STAGING/.gitkeep"
fi

if [[ -n "$APK" ]]; then
  if [[ ! -f "$APK" ]]; then
    echo "APK not found: $APK" >&2
    exit 1
  fi
  mkdir -p "$STAGING/system/priv-app/VictorCam"
  cp -f "$APK" "$STAGING/system/priv-app/VictorCam/VictorCam.apk"
fi

mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"
# module.prop must be at the zip root.
if command -v zip >/dev/null 2>&1; then
  (cd "$STAGING" && zip -r -9 -X "$OUT" . -x "*.DS_Store")
else
  python3 -c 'import sys, zipfile; from pathlib import Path; staging=Path(sys.argv[1]); out=Path(sys.argv[2]); zf=zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED); [zf.write(p, p.relative_to(staging).as_posix()) for p in sorted(staging.rglob("*")) if p.is_file() and p.name != ".DS_Store"]; zf.close()' "$STAGING" "$OUT"
fi
echo "Wrote $OUT"

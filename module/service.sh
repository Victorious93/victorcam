#!/system/bin/sh
# Late-start: pre-grant permissions VictorCam already declares.
# Does not grant extra privileged capabilities and does not touch HookDenylist.

PKG="com.victorious.cam"
MODDIR="${0%/*}"

# Wait until Package Manager can see the priv-app (after overlay mount).
i=0
while [ "$i" -lt 60 ]; do
  if pm path "$PKG" >/dev/null 2>&1; then
    break
  fi
  i=$((i + 1))
  sleep 1
done

if ! pm path "$PKG" >/dev/null 2>&1; then
  log -t VictorCamModule "com.victorious.cam not installed yet; skip grants"
  exit 0
fi

grant() {
  pm grant "$PKG" "$1" >/dev/null 2>&1 || true
}

# Dangerous / runtime permissions from AndroidManifest.xml.
grant android.permission.CAMERA
grant android.permission.RECORD_AUDIO
grant android.permission.READ_MEDIA_VIDEO
grant android.permission.READ_MEDIA_IMAGES
grant android.permission.POST_NOTIFICATIONS

# Special access declared in the manifest (not a privileged permission).
# MediaProjection still requires the system consent dialog.
if command -v appops >/dev/null 2>&1; then
  appops set "$PKG" SCHEDULE_EXACT_ALARM allow >/dev/null 2>&1 || true
else
  cmd appops set "$PKG" SCHEDULE_EXACT_ALARM allow >/dev/null 2>&1 || true
fi

log -t VictorCamModule "pre-granted declared runtime permissions for $PKG"

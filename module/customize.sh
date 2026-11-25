#!/system/bin/sh
# Optional VictorCam priv-app module (Magisk / KernelSU / APatch format).
# The app works fully without this module.

PKG="com.victorious.cam"
APP_DIR="$MODPATH/system/priv-app/VictorCam"
PERM_XML="$MODPATH/system/etc/permissions/privapp-permissions-com.victorious.cam.xml"
APK_DEST="$APP_DIR/VictorCam.apk"

ui_print "**********************************************"
ui_print " VictorCam optional privileged-install module"
ui_print "**********************************************"
ui_print " "
ui_print " HARD WARNINGS"
ui_print " - Root and privileged install have real security risk."
ui_print " - This module is OPTIONAL. VictorCam works fully without it."
ui_print " - It does NOT bypass the banking / payment / government-ID /"
ui_print "   KYC denylist in the app (HookDenylist)."
ui_print " - It does NOT hide root, spoof Play Integrity, or enable"
ui_print "   fraud, impersonation, or KYC/biometric bypass."
ui_print " - Intended use: privacy testing, streaming demos, and"
ui_print "   user-owned content only."
ui_print " "

MANAGER="Magisk-compatible"
if [ -n "$KSU" ]; then
  MANAGER="KernelSU"
elif [ -n "$APATCH" ]; then
  MANAGER="APatch"
elif [ -n "$MAGISK_VER" ]; then
  MANAGER="Magisk $MAGISK_VER"
fi
ui_print "- Manager: $MANAGER"

fail() {
  if type abort >/dev/null 2>&1; then
    abort "$1"
  else
    ui_print "$1"
    exit 1
  fi
}

perm() {
  _path="$1"
  if type set_perm >/dev/null 2>&1; then
    set_perm "$_path" 0 0 0644
  else
    chmod 0644 "$_path"
  fi
}

mkdir -p "$APP_DIR"

# Resolve the APK: bundled in the zip, loose at zip root, or already installed.
SRC=""
if [ -f "$APK_DEST" ]; then
  SRC="$APK_DEST"
else
  for candidate in \
      "$MODPATH/VictorCam.apk" \
      "$MODPATH/app-debug.apk" \
      "$MODPATH/app-release.apk"; do
    if [ -f "$candidate" ]; then
      SRC="$candidate"
      break
    fi
  done
fi

if [ -z "$SRC" ] && [ "$BOOTMODE" = "true" ]; then
  INST="$(pm path "$PKG" 2>/dev/null | head -n 1 | sed "s/^package://")"
  if [ -n "$INST" ] && [ -f "$INST" ]; then
    SRC="$INST"
  fi
fi

if [ -z "$SRC" ]; then
  ui_print "! No VictorCam APK found."
  ui_print "! Install com.victorious.cam first, use the CI zip that"
  ui_print "! bundles the debug APK, or add VictorCam.apk at the zip root."
  fail "! Aborting. The already-installed app is unchanged."
fi

if [ "$SRC" != "$APK_DEST" ]; then
  ui_print "- Copying APK from $SRC"
  cp -f "$SRC" "$APK_DEST" || fail "! Failed to copy APK"
fi

rm -f "$MODPATH/VictorCam.apk" "$MODPATH/app-debug.apk" "$MODPATH/app-release.apk"

if [ "$BOOTMODE" = "true" ]; then
  INST="$(pm path "$PKG" 2>/dev/null | head -n 1 | sed "s/^package://")"
  case "$INST" in
    */priv-app/VictorCam/*) ;;
    "") ;;
    *)
      ui_print "- Removing the user-installed copy (keeping app data)"
      ui_print "  so it does not conflict with the priv-app after reboot."
      pm uninstall -k --user 0 "$PKG" >/dev/null 2>&1 || true
      ;;
  esac
fi

if [ ! -f "$PERM_XML" ]; then
  fail "! Missing privapp-permissions XML"
fi

perm "$APK_DEST"
perm "$PERM_XML"

ui_print "- Privileged install staged. Reboot is required."
ui_print "- After reboot, service.sh pre-grants declared runtime permissions."
ui_print "- HookDenylist is unchanged. No root hiding is installed."

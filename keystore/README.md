# Local debug keystore

Keystores are never committed (`*.jks` / `*.keystore` are gitignored).
Without a keystore here, debug builds use Android's default debug signing.

To use a stable local debug key:

```sh
keytool -genkeypair -v -keystore keystore/victorcam-debug.jks \
  -alias victorcamdebug -keyalg RSA -keysize 2048 -validity 10000
```

Then put the passwords in `local.properties` (which is gitignored):

```
victorcam.debug.storePassword=<your store password>
victorcam.debug.keyPassword=<your key password>
```

Or set env vars `VICTORCAM_DEBUG_STORE_PASSWORD` / `VICTORCAM_DEBUG_KEY_PASSWORD`.

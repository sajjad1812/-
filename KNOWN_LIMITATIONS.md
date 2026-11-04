# Known limitations

- Not yet compiled or run; first CI build may reveal errors.
- "Glass" is a translucent surface, not true background blur.
- Dynamic Island is an overlay, not a hardware cutout integration. It does not yet react to media, timers or notifications. Some Xiaomi/Samsung versions need extra battery or overlay settings. Overlays may be hidden above the lock screen and some full-screen apps.
- Single `app` module for now; the planned multi-module split comes later.
- No Gradle wrapper is committed; CI uses `gradle/actions/setup-gradle`.
- Release APK uses the debug signing key.
- Android prevents ordinary apps from replacing the secure lock screen or toggling most system settings directly; later modules will use only supported APIs.

# SnapFlow Android — milestone 1

Native Kotlin Android app implementing **actual direct HTTPS MP4/WebM downloads** through Android DownloadManager. This is not a YouTube/Instagram/Facebook extraction client. It does not bypass DRM, login, API limits or platform terms.

## Build
Prerequisites: JDK 17, Android SDK 35, Gradle 8.9.

```bash
cd snapflow
gradle :app:assembleDebug
```

APK: `snapflow/app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds it from the `feature/snapflow-android-downloader` branch and uploads the binary as an artifact after a successful build.

## Real device acceptance test
1. Install APK and open SnapFlow.
2. Use a **direct, HTTPS** URL ending in `.mp4` or `.webm` from a source that permits downloading. URL must not be a website/watch page.
3. Tap Start Download. Watch queued/running/complete status and the Android system notification.
4. Check the actual video in `Downloads/SnapFlow`, use Play and verify it plays offline.
5. Test invalid URL, interruption and 404 separately. A queued status alone does not prove download success.

## Limits and roadmap
- Milestone 1 is direct files only. Does not resolve YouTube, Instagram or Facebook page URLs.
- No server is needed for direct downloads.
- No pause/resume promises: native DownloadManager controls its own retry behavior.
- A file's real type and integrity are not verified before enqueue; server errors show a failed status.
- OS/account/network/provider constraints and phone installation must be tested in real conditions.
- Next: instrumented tests, content-type validation, improved file naming, polished UI and licensed-source adapters.

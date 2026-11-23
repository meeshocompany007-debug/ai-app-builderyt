# SnapDay — Android Video Downloader & Player (v0.5)

**Approved visual source:** [DESIGN_REFERENCE_SNAPDAY.md](DESIGN_REFERENCE_SNAPDAY.md)
**App launcher:** `SnapDayActivity.kt`. The rejected old single-screen `MainActivity.kt` was deleted.
**Brand:** SnapDay, pink-violet rounded-square play/download logo, #7923E8, white Home, dark navy media.

## Screens
1 Splash
2 Onboarding 1
3 Onboarding 2
4 Onboarding 3
5 Optional Login
6 Email Login
7 Create Account
8 Skip/Success
9 Home
10 Video Player
11 Download Video Options
12 Floating Download Button
13 MP3 Player
14 Download Manager
15 Files
16 Settings
17 Browser
18 History
19 About/Profile
20 Premium (optional ₹99/month concept)

## Build
GitHub Actions `SnapDay Android APK` on `feature/snapflow-android-downloader`.
The successful build uploads `SnapDay-debug-APK` containing `app-debug.apk`.
Android tests in `app/src/androidTest` validate real authorized MP4 bytes, on-device site-engine initialization and page navigation; no mock video-transfer completion.

## Real downloading
- Direct HTTPS MP4/WebM: Android DownloadManager, saves under `Downloads/SnapFlow`. This storage subfolder is retained for backwards compatibility; the UI brand is SnapDay.
- Site adapter: on-device yt-dlp library, working with sources that allow downloading. Does **not** guarantee YouTube/Instagram/Facebook links, DRM content, paid/private media or login-restricted streams.
- Default public-domain smoke test URL: `https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4`.
- The UI only offers source formats that are actually known; no fake resolution options.

## Planned / unavailable services
- Google and email login require a real authentication backend. Buttons report this; Skip works.
- Premium ₹99/month requires Google Play Billing plus receipt verification. No real charge is made in this build.
- Content-discovery thumbnails from the user's uploaded image are **visual references**, not live platform feeds or promises of downloadable content.
- Expanded MP3 conversion/subtitles require testing and source permission.

## Installing
Download `SnapDay-debug-APK` from the GitHub Actions run, unzip, install `app-debug.apk`.
The debug signing key may differ from old builds; Android may require uninstalling the old debug APK first. Back up app-owned media before uninstalling because private app data may be removed.
Verify every navigation screen and a real, authorized download on the device.

## Long-term maintenance
A 20-year service life is possible only with frequent Android SDK upgrades, security patches, backend and API maintenance, backups, licensing reviews, regression testing and changes in platform terms. Review dependencies monthly and test major Android updates.

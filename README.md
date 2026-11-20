# RewardWatch Android app
Native Android Java app (API 24+) showing **Google's demo rewarded ads**.
Rewards are 5 demo-only in-app coins per SDK-confirmed reward callback, with 20 coins unlocking a teal theme.
No real payout, gift cards, or cash withdrawal. No user accounts.
This APK is a test build. Live AdMob integration requires compliant publisher IDs, consent/privacy UX, age suitability and policy review.

## Get APK
Open GitHub Actions > Build RewardWatch APK > latest run on rewardwatch-apk-build > Artifacts > RewardWatch-debug-APK.
Download and unzip the artifact to obtain app-debug.apk.

## Build
Java 17, Gradle 8.9, Android SDK 35, com.google.android.gms:play-services-ads:25.5.0.
Use gradle :app:assembleDebug.

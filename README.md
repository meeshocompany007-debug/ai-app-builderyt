# RewardWatch V2 — Native Android

New purple-and-gold Android demo with custom icon, Home, Watch Ads, Rewards, History and Profile pages.

## Working features
- Google official TEST rewarded ad: each SDK-confirmed completion grants 5 in-app demo coins.
- Once-per-device-day daily bonus grants 2 demo coins.
- Spend 20 coins for a neon theme.
- History filters (All, Earned, Used), 10-ad daily counter, locally saved data, data reset.
- This is not a monetary rewards product: no UPI, cash, transfer, gifts or payouts. Test ads do not earn production ad revenue.

## APK build
Open Actions / Build RewardWatch V2 APK on the rewardwatch-apk-build branch.
After the workflow succeeds, download RewardWatch-V2-debug-APK and unzip to get app-debug.apk.

## Requirements
Java 17, Gradle 8.9, Android SDK 35, Gradle Android plugin 8.7.3.
Run gradle :app:assembleDebug.

## Long-term care
Android and ad SDK changes, Google policy updates, security patches, and new Android versions require regular testing, upgrading and maintenance. A 20-year no-maintenance lifespan is unrealistic. This MVP has on-device coins, not server-verified balances.

# AI Builder — Project Studio

Phone-friendly website and Android project builder. Open index.html for the standalone design demo, or run the server for accounts, saved revisions, optional AI design generation, and Android builds. Agent Builder is reserved for a future release.

## Run the workspace

Install Node 24 or newer and the zip command. Run npm test, then npm start. Open http://127.0.0.1:3000/studio and register a username with a 12+ character password. For a phone on your trusted LAN, set HOST to your computer's LAN address for testing with a throwaway account. Use HTTPS for real accounts.

Choose Website or Android App. Starter mode uses a fixed layout with a working notes, tasks or expense list. AI mode asks the provider to design sections and select from those same features. The model returns data, never executable code. Keep the current version open to revise a project. Older versions remain available for preview and export. In the sandbox preview browser storage may be blocked. Exported pages store entries on the user's device.

## Provider and budget

AI is off unless GEMINI_API_KEY, GEMINI_MODEL and a positive integer AI_DAILY_LIMIT are set on the server. The limit counts attempted provider calls across this server database, including failed requests. It is not a currency cap. Verify model availability, quotas and provider pricing before enabling it. Never put secrets in the repository or browser. No paid deployment is required for starter mode.

## Android

Source ZIPs include an Android Studio Gradle project. Use JDK 17, Gradle 8.9 and Android SDK 35. Open the extracted project with local Gradle, or run gradle assembleDebug. The web application runs inside a native Android WebView with local assets. The debug application is for testing; release distribution requires a separate stable signing key and release process.

ENABLE_APK=1 enables an authenticated debug build endpoint in a host provisioned with the same toolchain and ANDROID_HOME. One build at a time, up to ten attempts per UTC day. Builds use fixed templates, child directories, and a filtered environment. Keep the debug keystore stable for install-over-update testing. Do not enable this worker on a public multi-tenant host until OS-level resource isolation and per-user workload limits are in place. The verification workflow builds a sample and uploads its APK to the associated Actions run.

## Service operation

Default server binds to loopback and stores user credentials (salted scrypt hashes), sessions, project versions and call counters in DATA_DIR/studio.sqlite. Supabase is deferred. Production requires HTTPS via a reverse proxy, NODE_ENV=production for Secure cookies, restricted persistent storage, request and storage limits, and tested backups (SQLite online backup, not a hot copy of just the main database). Restore the database in staging and compare project versions and account isolation. Rate limits use the immediate peer; configure edge throttling for reverse-proxied traffic. This is a single-instance beta. Recovery of forgotten passwords, cloud preview hosting, billing and release signing are not implemented.

Long-term ownership: monitor failures, disk space and provider spend daily; review backups and security updates weekly; test restores, Android versions and dependency upgrades quarterly; reassess hosting, models, schema migrations and signing-key recovery yearly. A 20-year service requires continued maintenance and migration. Source HTML, Android project exports and versioned records reduce dependence on a provider.

## Container and backup

Build with `docker build -t ai-builder .` and run behind a TLS reverse proxy, with a persistent volume mounted at /app/data. The default container serves the workspace, HTML and Android source. Debug APK builds require a separately provisioned Android toolchain. No provider account, key, deployment or spend is created by building the container. The image recipe has not been deployed to a hosting account.

Run `node backup.mjs` on the server to take an integrity-checked SQLite snapshot. Keep backups outside the container on protected storage. For restoration stop the server, preserve the old data directory, copy a snapshot as studio.sqlite into a new empty data directory, set DATA_DIR there and restart. Verify login and saved revisions. Snapshots contain session and credential records and must be protected.

## Public studio and installable AI Builder

Open https://ai-app-builderyt-studio.pluckymusic2.chatgpt.site for the keyless public studio. The site builds task, note and expense starters, keeps device-local version history and produces separate version-specific share URLs. Share links contain the layout and title. Entries are stored separately on each visitor's browser. Opening a newer share link for the same project on the same origin keeps the visitor's tasks. Published snapshots can be copied, and clearing browser storage erases local history. This public edition does not synchronize drafts across accounts.

Run `node live/build.mjs build/site` before starting the Node server to exercise the same interface at /live. Run `node build-builder.mjs` and `gradle -p build/builder assembleDebug` for the actual builder Android app. It bundles the studio for offline use and offers HTML/Android-source export through Android's document picker. Published links open the hosted studio. Generating a separately signed APK for each end user's project requires the Android worker in the server edition. Agent Builder remains Coming Soon.

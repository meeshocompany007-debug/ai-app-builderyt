# AI App & Website Builder

The default Studio at `/` and `/studio` creates versioned source projects. Sign in, choose Web or Android, and submit an idea. Open a project to request a targeted edit. The activity stream comes from the running job. Source and history remain available after refresh. A successful Android build includes a debug APK and a browser preview of the Flutter UI. Publishing a web snapshot assigns a stable hostname to its running preview.

## Local start

Requires Node 24 and npm. Run from the repository root.

```sh
npm ci
cp .env.example .env
npm run lint
npm run typecheck
npm run build
npm test
npm start
```

Open http://127.0.0.1:3000. To generate projects, configure the runner and gateway below and fill in the environment file. Set exactly one provider key, LLM_PROVIDER (openai, anthropic, or gemini), a compatible LLM_MODEL, and a positive global LLM_DAILY_CALL_LIMIT. Each planning, editing and repair request consumes a call. The server does not send its credentials into project containers.

## Execution infrastructure

Use one studio process with a dedicated Docker daemon on a Linux worker, the gVisor runsc runtime, per-container storage quotas (overlay2 on XFS with project quotas) and resource controls. Give the service account access to that dedicated daemon. The worker images must include sh, sleep, curl, python3, a writable /workspace and a runnable uid 1000. Build trusted images and push them to your own registry. Configure their immutable repo digests in WEB_BUILD_IMAGE and ANDROID_BUILD_IMAGE. The Android image requires a compatible Flutter SDK base with a licensed Android SDK. A registry and preconfigured base are deployment inputs.

```sh
docker build -f services/sandbox/Web.Dockerfile -t "$WEB_IMAGE_TAG" .
docker push "$WEB_IMAGE_TAG"
docker build -f services/sandbox/Android.Dockerfile --build-arg FLUTTER_BASE="$FLUTTER_BASE" -t "$ANDROID_IMAGE_TAG" .
docker push "$ANDROID_IMAGE_TAG"
docker network create --internal studio-mirror
```

Supply curated npm, Dart, Maven and Gradle mirrors reachable from that internal network or prewarm the pinned image caches for the allowed dependencies. Configure mirror URLs in the worker images. The isolated build network must have no route to the application database, metadata services or Docker API. Generated runtimes have networking disabled and can serve HTTP through the gateway. A runtime app requiring external integrations needs a separately designed egress policy.

Set PREVIEW_DOMAIN to a dedicated domain on a separate registrable domain from the studio. Point its wildcard DNS and wildcard TLS ingress to the studio service, preserving Host. Configure ingress to accept only the studio and preview hostnames, cap request sizes and disable buffering on event streams. Back up DATA_DIR and configure Docker object retention, monitoring and per-tenant quotas before accepting public traffic. Snapshot rollback restores source and points at the historical runtime. Generated app data lives in the runtime container and is not migrated to a subsequent version. The platform database and APK files use persistent DATA_DIR.

## Verification and release status

`npm test` runs the local regression and HTTP integration suite. With a separate test deployment and real provider credentials configured, set STUDIO_TEST_URL and run `npm run test:live`. This creates a test account, invokes a real model, builds a web app, edits it, checks both HTTP features on its published URL, compiles Flutter and downloads the APK. This test consumes model credit and isolated worker resources. It fails when prerequisites are absent. The Flutter web preview is not a native device test. Android debug APKs require release signing for distribution. The implementation still needs an infrastructure-backed acceptance run and operational hardening before production use.

## Earlier repository implementation

The following notes document the older layout-based builder, static deployment and sample Android workflows. Those outputs use the earlier project format.

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

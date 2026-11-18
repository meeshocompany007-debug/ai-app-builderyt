# Owner settings
Open the served studio at /studio and choose ⋮ → Admin settings. The owner signs in with Google. The server pins the designated verified Gmail address using its SHA-256 digest and uses a separate one-hour owner session.

Register an OAuth web application in Google Cloud with the exact HTTPS redirect URL https://YOUR-HOST/admin/callback. Supply GOOGLE_ADMIN_CLIENT_ID, GOOGLE_ADMIN_CLIENT_SECRET and GOOGLE_ADMIN_REDIRECT_URI to the Node server and restart it. Keep credentials in the host secret settings. The static site cannot serve these routes.

The owner can add, rename, disable or remove Gemini and OpenAI model tools and assign them to automatic planning/fast and advanced routes. Use a model ID available to the configured provider account that supports JSON responses. Gemini requires GEMINI_API_KEY. OpenAI requires OPENAI_API_KEY and Chat Completions JSON object support. AI_DAILY_LIMIT sets the deployment ceiling. ENABLE_APK=1 and a provisioned worker are required for APK builds. Controls persist in the existing SQLite database. Model IDs and limits from the environment initialize settings on the first run. Other provider protocols require dedicated adapters.

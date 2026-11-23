# SnapDay UI — APPROVED DESIGN SOURCE OF TRUTH
Status: User approved / mandatory reference, 2026-11-23.

The user-uploaded 19-screen storyboard is the final visual reference. Previous SnapFlow dark single-page UI is rejected and MUST NOT be restored.

## Non-negotiable brand
- App name: **SnapDay — Video Downloader & Player** (not SnapFlow).
- Primary #7923E8; purple / pink gradients; white surfaces; dark navy players; refined typography; 16–24dp rounded cards, subtle shadows; crisp mobile Android status/navigation.
- Logo: rounded square pink-purple gradient + white play + download symbol.
- English UI except Hindi video titles / satsang visual copy.
- No fake video qualities, hard-coded false view counts, fake video downloads, or dummy account/payment success.
- User can SKIP account entirely. Core playback and permitted downloads do not require premium.
- The image's named YouTube/Facebook/etc logos are **shortcuts/discovery**, not a guarantee of unrestricted downloading.
- Use real download status from DownloadManager and site resolver; show failures without pretending success.
- For user-uploaded image, see chat attachment dated 2026-11-23; this document preserves every screen requirement permanently in the repository.

## Required storyboard screen navigation and functional UX
01 Splash launch — SnapDay gradient logo, subtitle and loading.
02 Onboarding 1 — headphone-person illustration, social shortcuts, "Download Videos from All Platforms", dots, Next and Skip (1/3).
03 Onboarding 2 — HD/4K card, purple player, "Choose Your Video Quality", Next and Skip (2/3).
04 Onboarding 3 — rocket illustration, "Fast, Safe & Easy", Get Started and Skip (3/3).
05 Optional login — Google/email buttons and Skip for Now (backend-dependent sign in must be honest).
06 Email login — email/password, eye visibility, remember me, Forgot Password, Sign Up, Skip for Now.
07 Create account — name/email/password, Sign Up, terms/privacy, Skip for Now.
08 You're All Set — verified purple shield, checklist, Continue.
09 Home — white background, toolbar, search/paste link, YouTube/Instagram/Facebook/TikTok shortcuts, category chips For You/Trending/Satsang/Music/Shorts, Hindi satsang card, music content, bottom nav Home/Downloads/Player/Files/Me, draggable in-app floating purple download button.
10 Video player — rich preview/player controls, scrubber, play/pause, prev/next, share/save/download, suggested items. Playback only for real saved media.
11 Download Video options — tabs Video/Audio (MP3)/Subtitles, quality choices only when actual stream formats are verified; NO fabricated 4K/2K/1080p options.
12 Floating download — draggable purple button opens Paste Link / Download Video / Download MP3 / Open Browser (permission dependent).
13 Dark navy MP3 player — artwork, seek, play/pause, shuffle/repeat, playlist, equalizer, sleep timer; only working for supported local audio files.
14 Download manager — All/Video/Audio/In Progress, real status/bytes, play/failed reason.
15 File manager — Videos/Audio (MP3)/Images/Documents categories and real local files where accessible.
16 Settings — Download Quality, Download Path, Video Player, Audio Player, Floating Download Button, Auto Detect Link, Notifications, Storage.
17 In-app browser — address input, shortcuts and navigation, accessible web browsing (no DRM bypass).
18 History — watched/downloaded tabs, real history/transactions.
19 About/Profile — SnapDay logo/version, optional Login, Rate App, Share, Feedback, Privacy Policy, Terms.
20 Premium optional separate upgrade screen — ₹99/month concept, not charged until verified store billing backend exists.

## Release acceptance
- APK must compile and install; on-device or emulator verify all routes reachable, back navigation, scroll, no crashes.
- Sample authorized MP4 must actually download and saved bytes be valid.
- Site extraction is permission/website dependent, not guaranteed for arbitrary YouTube/Facebook/Instagram links.
- No pretend Google auth or fake premium payment; unavailable functionality must show an honest message.
- Maintain security, OS SDK updates, backups, dependency upgrades and regression tests indefinitely. 20-year maintenance requires continuous investment and migrations.

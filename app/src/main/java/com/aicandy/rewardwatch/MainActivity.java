package com.aicandy.rewardwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String TEST_AD_UNIT = "ca-app-pub-3940256099942544/5224354917";
    private static final int BG = Color.rgb(17, 12, 32);
    private static final int PANEL = Color.rgb(32, 22, 51);
    private static final int PANEL2 = Color.rgb(42, 29, 64);
    private static final int TEXT = Color.rgb(249, 247, 255);
    private static final int MUTED = Color.rgb(191, 180, 211);
    private static final int PURPLE = Color.rgb(157, 57, 250);
    private static final int GOLD = Color.rgb(255, 193, 60);
    private static final int GREEN = Color.rgb(97, 224, 156);
    private static final int RED = Color.rgb(250, 119, 150);

    private static final int HOME = 0;
    private static final int EARN = 1;
    private static final int REWARDS = 2;
    private static final int PROFILE = 3;
    private static final int HISTORY = 4;

    private SharedPreferences prefs;
    private RewardedAd loadedAd;
    private boolean adLoading;
    private boolean adShowing;
    private boolean rewardedThisShow;
    private int currentPage = HOME;
    private int historyFilter = 0;

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable surface(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable bordered(int color, int radius, int borderColor) {
        GradientDrawable d = surface(color, radius);
        d.setStroke(dp(1), borderColor);
        return d;
    }

    private GradientDrawable gradient(boolean teal, int radius) {
        int[] colors = teal
            ? new int[]{Color.rgb(13, 104, 107), Color.rgb(29, 51, 83)}
            : new int[]{Color.rgb(101, 37, 179), Color.rgb(35, 15, 74)};
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private void append(LinearLayout host, View view, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        host.addView(view, p);
    }

    private void pair(LinearLayout row, View view, int weight, int left) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, weight);
        p.leftMargin = dp(left);
        row.addView(view, p);
    }

    private LinearLayout card() {
        LinearLayout c = vertical();
        c.setPadding(dp(19), dp(19), dp(19), dp(19));
        c.setBackground(bordered(PANEL, 22, Color.rgb(59, 43, 86)));
        return c;
    }

    private Button button(String title, boolean gold) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(gold ? BG : Color.WHITE);
        b.setBackground(surface(gold ? GOLD : PURPLE, 17));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(10), dp(13), dp(10), dp(13));
        return b;
    }

    private int coins() { return prefs.getInt("coins", 0); }

    private boolean neon() { return prefs.getBoolean("theme_active", false); }

    private String dayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private int watchedToday() {
        return dayKey().equals(prefs.getString("ads_date", ""))
            ? prefs.getInt("ads_today", 0) : 0;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        prefs = getSharedPreferences("rewardwatch_demo_v2", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        render();
        MobileAds.initialize(this, status -> runOnUiThread(this::loadRewardedAd));
    }

    private void open(int page) {
        currentPage = page;
        render();
    }

    @Override
    public void onBackPressed() {
        if (currentPage != HOME) open(HOME);
        else super.onBackPressed();
    }

    private void render() {
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets sys = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, sys.top, 0, sys.bottom);
                return insets;
            });
        } else {
            root.setFitsSystemWindows(true);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout body = vertical();
        body.setPadding(dp(20), dp(22), dp(20), dp(26));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if (currentPage == HOME) buildHome(body);
        else if (currentPage == EARN) buildEarn(body);
        else if (currentPage == REWARDS) buildRewards(body);
        else if (currentPage == PROFILE) buildProfile(body);
        else buildHistory(body);

        root.addView(bottomNavigation(), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private TextView title(String text) {
        return label(text, 26, TEXT, true);
    }

    private void pageHeader(LinearLayout body, String subtitle, String heading) {
        TextView top = label("REWARDWATCH  •  DEMO", 12, GOLD, true);
        top.setLetterSpacing(0.11f);
        append(body, top, 0);
        append(body, title(heading), 13);
        append(body, label(subtitle, 14, MUTED, false), 5);
    }

    private LinearLayout logo(int size) {
        LinearLayout box = horizontal();
        box.setGravity(Gravity.CENTER);
        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.ic_rewardwatch);
        box.addView(image, new LinearLayout.LayoutParams(dp(size), dp(size)));
        return box;
    }

    private TextView coinPill() {
        TextView p = label("★  " + coins(), 16, GOLD, true);
        p.setGravity(Gravity.CENTER);
        p.setPadding(dp(14), dp(9), dp(14), dp(9));
        p.setBackground(surface(Color.rgb(49, 31, 73), 18));
        return p;
    }

    private void buildHome(LinearLayout body) {
        LinearLayout brand = horizontal();
        TextView brandText = title("RewardWatch");
        pair(brand, brandText, 1, 0);
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(-2, -2);
        brand.addView(coinPill(), pillParams);
        append(body, brand, 0);

        LinearLayout welcome = horizontal();
        welcome.setPadding(dp(12), dp(12), dp(14), dp(12));
        welcome.setBackground(gradient(neon(), 18));
        welcome.addView(logo(49), new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout lines = vertical();
        append(lines, label("Welcome Back!", 17, TEXT, true), 0);
        append(lines, label("Your rewards corner ✨", 12, MUTED, false), 4);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(0, -2, 1);
        wl.leftMargin = dp(10);
        welcome.addView(lines, wl);
        append(body, welcome, 20);

        LinearLayout hero = vertical();
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setBackground(gradient(neon(), 26));
        hero.setPadding(dp(22), dp(22), dp(22), dp(25));
        append(hero, logo(105), 0);
        TextView heading = label("Watch Video Ads", 24, TEXT, true);
        heading.setGravity(Gravity.CENTER);
        append(hero, heading, 7);
        TextView sub = label("Earn 5 demo coins per completed ad", 13, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        append(hero, sub, 7);
        Button watch = button("▶  Watch & Earn", true);
        watch.setOnClickListener(v -> open(EARN));
        append(hero, watch, 20);
        append(body, hero, 18);

        append(body, label("Quick Actions", 19, TEXT, true), 24);
        LinearLayout quick = horizontal();
        pair(quick, quickAction("✦", "Daily +2", this::claimDaily), 1, 0);
        pair(quick, quickAction("★", "Themes", () -> open(REWARDS)), 1, 10);
        pair(quick, quickAction("◷", "History", () -> open(HISTORY)), 1, 10);
        append(body, quick, 12);

        LinearLayout progressCard = card();
        append(progressCard, label("Your Progress", 19, TEXT, true), 0);
        LinearLayout progressRow = horizontal();
        pair(progressRow, label("Today's rewarded videos", 13, MUTED, false), 1, 0);
        progressRow.addView(label(Math.min(watchedToday(), 10) + " / 10", 14, TEXT, true));
        append(progressCard, progressRow, 16);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(10);
        progress.setProgress(Math.min(watchedToday(), 10));
        progress.setProgressTintList(ColorStateList.valueOf(PURPLE));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(PANEL2));
        append(progressCard, progress, 12);
        append(progressCard, label("Coins balance: " + coins() + "   •   In-app only", 12, MUTED, false), 15);
        append(body, progressCard, 18);
        append(body, label("TEST ADS ONLY • No cash or UPI payout.", 12, MUTED, false), 18);
    }

    private LinearLayout quickAction(String icon, String text, Runnable click) {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(2), dp(14), dp(2), dp(14));
        box.setBackground(bordered(PANEL, 17, Color.rgb(60, 43, 87)));
        TextView ico = label(icon, 24, GOLD, true);
        ico.setGravity(Gravity.CENTER);
        append(box, ico, 0);
        TextView caption = label(text, 12, TEXT, true);
        caption.setGravity(Gravity.CENTER);
        append(box, caption, 7);
        box.setOnClickListener(v -> click.run());
        return box;
    }

    private void buildEarn(LinearLayout body) {
        pageHeader(body, "Complete a video ad for your reward", "Watch Video Ad");
        LinearLayout graphic = vertical();
        graphic.setGravity(Gravity.CENTER_HORIZONTAL);
        graphic.setPadding(dp(16), dp(26), dp(16), dp(26));
        graphic.setBackground(gradient(neon(), 24));
        append(graphic, logo(110), 0);
        TextView reward = label("★  +5 Coins", 28, GOLD, true);
        reward.setGravity(Gravity.CENTER);
        append(graphic, reward, 15);
        TextView explainer = label("Watch a test rewarded video", 15, TEXT, true);
        explainer.setGravity(Gravity.CENTER);
        append(graphic, explainer, 8);
        append(body, graphic, 22);

        LinearLayout ruleCard = card();
        append(ruleCard, label("How it works", 19, TEXT, true), 0);
        append(ruleCard, label("①  Tap Show Ad below", 14, MUTED, false), 14);
        append(ruleCard, label("②  Watch until the ad reward is granted", 14, MUTED, false), 10);
        append(ruleCard, label("③  Get +5 in-app demo coins", 14, MUTED, false), 10);
        append(body, ruleCard, 18);

        LinearLayout adCard = card();
        String status;
        if (adShowing) status = "Video is currently playing…";
        else if (adLoading) status = "Loading Google test ad…";
        else if (loadedAd != null) status = "✓ Test ad ready to watch";
        else status = "Ad not available. Check internet and retry.";
        append(adCard, label(status, 14, adLoading ? MUTED : (loadedAd != null ? GREEN : MUTED), true), 0);
        Button watch = button("▶  Show Video Ad", false);
        boolean available = loadedAd != null && !adLoading && !adShowing;
        watch.setEnabled(available);
        watch.setAlpha(available ? 1f : 0.5f);
        watch.setOnClickListener(v -> showRewardedAd());
        append(adCard, watch, 14);
        if (!adLoading && loadedAd == null && !adShowing) {
            Button retry = button("Retry loading", false);
            retry.setOnClickListener(v -> loadRewardedAd());
            append(adCard, retry, 10);
        }
        append(body, adCard, 18);
        append(body, label("Google's official test ad. No real-money earning.", 12, MUTED, false), 18);
    }

    private void buildRewards(LinearLayout body) {
        pageHeader(body, "Spend demo coins to unlock app themes", "My Rewards");
        LinearLayout balance = vertical();
        balance.setPadding(dp(22), dp(22), dp(22), dp(22));
        balance.setBackground(gradient(neon(), 22));
        append(balance, label("YOUR COIN BALANCE", 12, MUTED, true), 0);
        append(balance, label("★  " + coins() + " coins", 34, GOLD, true), 11);
        append(balance, label("For in-app rewards only", 13, TEXT, false), 9);
        append(body, balance, 23);

        LinearLayout theme = card();
        append(theme, logo(72), 0);
        append(theme, label("Neon Theme", 23, TEXT, true), 10);
        append(theme, label("Unlock a stylish teal dashboard", 14, MUTED, false), 8);
        boolean owned = prefs.getBoolean("theme_owned", false);
        TextView price = label(owned ? "✓ Unlocked" : "★  20 coins", 17, GOLD, true);
        append(theme, price, 18);
        Button unlock = button(owned ? (neon() ? "✓ Neon Active • Disable" : "Activate Neon Theme") : "Unlock Theme • 20 Coins", false);
        unlock.setOnClickListener(v -> themeAction());
        append(theme, unlock, 15);
        append(body, theme, 18);
        append(body, label("Note: These coins are not redeemable or transferable.", 12, MUTED, false), 17);
    }

    private void themeAction() {
        if (prefs.getBoolean("theme_owned", false)) {
            prefs.edit().putBoolean("theme_active", !neon()).apply();
            render();
            return;
        }
        if (coins() < CoinRules.NEON_THEME_PRICE) {
            toast("You need 20 coins to unlock this theme");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Unlock Neon Theme?")
            .setMessage("Spend 20 demo coins for this in-app theme?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Unlock", (d, which) -> {
                try {
                    int remaining = CoinRules.spend(coins(), CoinRules.NEON_THEME_PRICE);
                    prefs.edit().putInt("coins", remaining)
                        .putBoolean("theme_owned", true)
                        .putBoolean("theme_active", true).apply();
                    log("U", "Unlocked Neon Theme", -CoinRules.NEON_THEME_PRICE);
                    render();
                } catch (IllegalArgumentException e) {
                    toast("Not enough demo coins");
                }
            }).show();
    }

    private void buildHistory(LinearLayout body) {
        pageHeader(body, "Every demo coin movement on this device", "Activity History");
        LinearLayout filters = horizontal();
        pair(filters, historyChip("All", 0), 1, 0);
        pair(filters, historyChip("Earned", 1), 1, 9);
        pair(filters, historyChip("Used", 2), 1, 9);
        append(body, filters, 22);
        String history = prefs.getString("history", "");
        if (history.isEmpty()) {
            LinearLayout empty = card();
            append(empty, label("No activity yet", 19, TEXT, true), 0);
            append(empty, label("Watch your first video or collect a daily bonus.", 14, MUTED, false), 12);
            append(body, empty, 18);
            return;
        }
        int shown = 0;
        for (String entry : history.split("~")) {
            String[] parts = entry.split("\\^", -1);
            if (parts.length != 4) continue;
            boolean earned = "E".equals(parts[0]);
            if ((historyFilter == 1 && !earned) || (historyFilter == 2 && earned)) continue;
            LinearLayout item = card();
            LinearLayout row = horizontal();
            LinearLayout info = vertical();
            append(info, label(parts[1], 15, TEXT, true), 0);
            append(info, label(parts[3], 12, MUTED, false), 5);
            pair(row, info, 1, 0);
            row.addView(label((earned ? "+" : "") + parts[2] + " ★", 16,
                earned ? GREEN : RED, true));
            append(item, row, 0);
            append(body, item, 12);
            shown++;
        }
        if (shown == 0) append(body, label("Nothing in this category yet.", 14, MUTED, false), 20);
    }

    private Button historyChip(String title, int type) {
        Button chip = button(title, false);
        chip.setTextSize(13);
        chip.setBackground(surface(historyFilter == type ? PURPLE : PANEL2, 14));
        chip.setOnClickListener(v -> {
            historyFilter = type;
            render();
        });
        return chip;
    }

    private void buildProfile(LinearLayout body) {
        pageHeader(body, "Local demo profile • no sign-in required", "My Profile");
        LinearLayout card = card();
        append(card, logo(85), 0);
        append(card, label("RewardWatch Member", 20, TEXT, true), 12);
        append(card, label("Demo account — data saved only on this phone", 13, MUTED, false), 7);
        append(card, label("★  " + coins() + " coins", 26, GOLD, true), 20);
        append(card, label("Today's rewarded ads: " + watchedToday(), 14, MUTED, false), 8);
        append(body, card, 20);

        Button history = button("◷  View Activity History", false);
        history.setOnClickListener(v -> open(HISTORY));
        append(body, history, 19);
        LinearLayout privacy = card();
        append(privacy, label("Privacy & rewards", 17, TEXT, true), 0);
        append(privacy, label("Coins, progress and unlocked theme are stored locally. The test ad is served by Google's test ad SDK. No cash, bank account or UPI is involved.", 14, MUTED, false), 12);
        append(body, privacy, 18);

        Button reset = button("Reset Demo Data", false);
        reset.setBackground(surface(Color.rgb(100, 37, 63), 16));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Reset RewardWatch?")
            .setMessage("This will erase your local coins, history, theme and daily progress.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset", (d, which) -> {
                prefs.edit().clear().apply();
                historyFilter = 0;
                open(HOME);
            }).show());
        append(body, reset, 19);
        append(body, label("RewardWatch v2.0 • Android demo", 12, MUTED, false), 17);
    }

    private LinearLayout bottomNavigation() {
        LinearLayout nav = horizontal();
        nav.setPadding(dp(5), dp(9), dp(5), dp(8));
        nav.setBackgroundColor(Color.rgb(24, 16, 41));
        navItem(nav, "⌂", "Home", HOME);
        navItem(nav, "▶", "Earn", EARN);
        navItem(nav, "★", "Rewards", REWARDS);
        navItem(nav, "●", "Profile", PROFILE);
        return nav;
    }

    private void navItem(LinearLayout nav, String icon, String title, int index) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), dp(7), dp(2), dp(7));
        boolean selected = index == currentPage || (currentPage == HISTORY && index == PROFILE);
        item.setBackground(surface(selected ? Color.rgb(93, 30, 173) : Color.TRANSPARENT, 15));
        TextView glyph = label(icon, 23, selected ? GOLD : MUTED, true);
        glyph.setGravity(Gravity.CENTER);
        append(item, glyph, 0);
        TextView t = label(title, 12, selected ? TEXT : MUTED, selected);
        t.setGravity(Gravity.CENTER);
        append(item, t, 3);
        item.setOnClickListener(v -> open(index));
        pair(nav, item, 1, 0);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void claimDaily() {
        if (dayKey().equals(prefs.getString("bonus_date", ""))) {
            toast("Today's demo bonus has already been collected");
            return;
        }
        try {
            int next = CoinRules.credit(coins(), CoinRules.DAILY_BONUS);
            prefs.edit().putInt("coins", next).putString("bonus_date", dayKey()).apply();
            log("E", "Daily demo bonus", CoinRules.DAILY_BONUS);
            toast("+2 demo coins collected!");
            render();
        } catch (ArithmeticException e) {
            toast("Coin balance is full");
        }
    }

    private void log(String type, String description, int amount) {
        String now = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date());
        String prior = prefs.getString("history", "");
        String line = type + "^" + description + "^" + amount + "^" + now;
        String full = prior.isEmpty() ? line : line + "~" + prior;
        String[] entries = full.split("~");
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < entries.length && i < 30; i++) {
            if (i > 0) output.append("~");
            output.append(entries[i]);
        }
        prefs.edit().putString("history", output.toString()).apply();
    }

    private void loadRewardedAd() {
        if (loadedAd != null || adLoading || adShowing || isFinishing() || isDestroyed()) return;
        adLoading = true;
        if (currentPage == EARN) render();
        RewardedAd.load(this, TEST_AD_UNIT, new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) {
                    if (isFinishing() || isDestroyed()) return;
                    loadedAd = ad;
                    adLoading = false;
                    if (currentPage == EARN) render();
                }
                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    loadedAd = null;
                    adLoading = false;
                    if (!isFinishing() && currentPage == EARN) render();
                }
            });
    }

    private void showRewardedAd() {
        if (loadedAd == null || adShowing) return;
        RewardedAd showing = loadedAd;
        loadedAd = null;
        adShowing = true;
        rewardedThisShow = false;
        showing.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                adShowing = false;
                if (!isFinishing()) {
                    render();
                    loadRewardedAd();
                }
            }
            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                adShowing = false;
                if (!isFinishing()) {
                    toast("Video could not be shown");
                    render();
                    loadRewardedAd();
                }
            }
        });
        showing.show(this, rewardItem -> {
            if (rewardedThisShow) return;
            rewardedThisShow = true;
            try {
                int total = CoinRules.credit(coins(), CoinRules.AD_REWARD);
                prefs.edit().putInt("coins", total)
                    .putString("ads_date", dayKey())
                    .putInt("ads_today", watchedToday() + 1).apply();
                log("E", "Watched Video Ad", CoinRules.AD_REWARD);
                toast("+5 demo coins earned!");
                if (!isFinishing()) render();
            } catch (ArithmeticException ex) {
                toast("Coin balance is full");
            }
        });
    }
}

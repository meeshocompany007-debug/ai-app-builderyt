package com.aicandy.rewardwatch;

/** Demo-only in-app currency. Never redeemable for real money. */
public final class CoinRules {
    public static final int AD_REWARD = 5;
    public static final int DAILY_BONUS = 2;
    public static final int NEON_THEME_PRICE = 20;
    private CoinRules() {}
    public static int credit(int balance, int amount) {
        if (balance < 0 || amount <= 0) throw new IllegalArgumentException("invalid credit");
        return Math.addExact(balance, amount);
    }
    public static int spend(int balance, int price) {
        if (price <= 0 || balance < price) throw new IllegalArgumentException("insufficient coins");
        return balance - price;
    }
}

import com.aicandy.rewardwatch.CoinRules;
public final class CoinRulesTest {
    static void ok(boolean b) { if (!b) throw new AssertionError("Coin rule failed"); }
    public static void main(String[] args) {
        ok(CoinRules.credit(0, CoinRules.AD_REWARD) == 5);
        ok(CoinRules.credit(15, CoinRules.AD_REWARD) == 20);
        ok(CoinRules.credit(0, CoinRules.DAILY_BONUS) == 2);
        ok(CoinRules.spend(20, CoinRules.NEON_THEME_PRICE) == 0);
        ok(CoinRules.spend(25, CoinRules.NEON_THEME_PRICE) == 5);
        boolean denied = false;
        try { CoinRules.spend(19, 20); } catch (IllegalArgumentException ex) { denied = true; }
        ok(denied);
        denied = false;
        try { CoinRules.credit(Integer.MAX_VALUE, 5); } catch (ArithmeticException ex) { denied = true; }
        ok(denied);
        System.out.println("PASS: 7 coin-rule checks");
    }
}

package com.aicandy.rewardwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class MainActivity extends Activity {
  // Google's official TEST ad unit. Demo coins are not cash or transferable.
  static final String TEST_AD = "ca-app-pub-3940256099942544/5224354917";
  static final int PURPLE = Color.rgb(88,102,242), NAVY = Color.rgb(18,28,59);
  SharedPreferences prefs;
  TextView balanceView, activityView, statusView;
  Button watchBtn, retryBtn, themeBtn;
  LinearLayout hero;
  RewardedAd ad;
  boolean loading, playing, awarded;

  int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
  GradientDrawable bg(int color, int radius) {
    GradientDrawable b = new GradientDrawable();
    b.setColor(color); b.setCornerRadius(dp(radius)); return b;
  }
  GradientDrawable gradient(boolean teal) {
    GradientDrawable b = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
      teal ? new int[]{Color.rgb(4,74,76),Color.rgb(19,126,124)}
           : new int[]{Color.rgb(22,28,79),Color.rgb(47,54,137)});
    b.setCornerRadius(dp(25)); return b;
  }
  LinearLayout column() {
    LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); return c;
  }
  TextView text(String s,int sp,int color, boolean bold) {
    TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color);
    if(bold) v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    return v;
  }
  void add(LinearLayout box, View child, int top) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
    p.topMargin=dp(top); box.addView(child,p);
  }
  LinearLayout card() { LinearLayout c=column(); c.setPadding(dp(19),dp(19),dp(19),dp(19)); c.setBackground(bg(Color.WHITE,22)); c.setElevation(dp(2)); return c; }
  Button button(String s,boolean primary) {
    Button b = new Button(this); b.setText(s); b.setAllCaps(false);
    b.setTextSize(15); b.setTextColor(primary?Color.WHITE:PURPLE);
    b.setBackground(bg(primary?PURPLE:Color.rgb(237,239,255),15));
    return b;
  }

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    prefs=getSharedPreferences("rewardwatch_demo",MODE_PRIVATE);
    ScrollView scroll=new ScrollView(this);
    scroll.setBackgroundColor(Color.rgb(244,246,252)); scroll.setFillViewport(true);
    LinearLayout page=column(); page.setPadding(dp(20),dp(26),dp(20),dp(32));
    scroll.addView(page); setContentView(scroll);
    add(page,text("RewardWatch   ✦  DEMO",23,NAVY,true),0);
    hero=column(); hero.setPadding(dp(22),dp(22),dp(22),dp(22));
    hero.setBackground(gradient(false));
    add(hero,text("YOUR REWARDS",12,Color.rgb(213,217,249),true),0);
    balanceView=text("0 coins",43,Color.WHITE,true); add(hero,balanceView,10);
    add(hero,text("✓ In-app demo points only",13,Color.rgb(221,225,255),false),12);
    add(page,hero,24);
    add(page,text("Watch. Earn. Unlock.",24,NAVY,true),27);
    add(page,text("Aapka reward corner ✨",14,Color.rgb(106,115,145),false),5);
    LinearLayout adCard=card();
    add(adCard,text("▶  Rewarded video",19,NAVY,true),0);
    add(adCard,text("Complete an ad to get +5 coins",13,Color.rgb(106,115,145),false),8);
    statusView=text("Initializing test video…",13,Color.rgb(106,115,145),false);
    add(adCard,statusView,16);
    watchBtn=button("Watch video ad  +5 coins",true); watchBtn.setEnabled(false);
    add(adCard,watchBtn,10);
    retryBtn=button("Retry loading",false); add(adCard,retryBtn,5);
    add(page,adCard,19);
    LinearLayout themeCard=card();
    add(themeCard,text("✦  Neon theme",19,NAVY,true),0);
    add(themeCard,text("Unlock your teal dashboard for 20 coins",13,Color.rgb(106,115,145),false),8);
    themeBtn=button("Unlock • 20 coins",false); add(themeCard,themeBtn,15);
    add(page,themeCard,15);
    LinearLayout historyCard=card();
    add(historyCard,text("Recent activity",18,NAVY,true),0);
    activityView=text("No activity yet",13,Color.rgb(106,115,145),false); add(historyCard,activityView,12);
    add(page,historyCard,15);
    add(page,text("ⓘ  Test ads only. No cash/UPI payout. Coins and history stay on this device.",12,Color.rgb(106,115,145),false),22);
    Button reset=button("Reset demo data",false); add(page,reset,14);
    watchBtn.setOnClickListener(v->showAd());
    retryBtn.setOnClickListener(v->loadAd());
    themeBtn.setOnClickListener(v->themeClick());
    reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Reset demo?")
      .setMessage("All local coins, activity and themes will be erased.")
      .setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{
        prefs.edit().clear().apply(); render();
      }).show());
    render(); updateStatus("Initializing test video…");
    MobileAds.initialize(this, status->runOnUiThread(this::loadAd));
  }

  void render() {
    int coins=prefs.getInt("coins",0);
    balanceView.setText(coins+" coins");
    boolean owned=prefs.getBoolean("owned",false), active=prefs.getBoolean("active",false);
    themeBtn.setText(!owned?"Unlock • 20 coins":active?"✓ Neon active · Tap to change":"Activate neon theme");
    hero.setBackground(gradient(active));
    watchBtn.setBackground(bg(active?Color.rgb(19,151,139):PURPLE,15));
    String recent=prefs.getString("history","");
    activityView.setText(recent.isEmpty()?"No activity yet. Watch your first video!":recent.replace("|","\n\n"));
  }
  void log(String s) {
    String stamp=new SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date());
    String previous=prefs.getString("history","");
    String entry="• "+s+"  ("+stamp+")";
    String result=entry+(previous.isEmpty()?"":"|"+previous);
    String[] rows=result.split("\\|");
    StringBuilder limited=new StringBuilder();
    for(int i=0;i<rows.length&&i<6;i++) {
      if(i>0)limited.append("|");limited.append(rows[i]);
    }
    prefs.edit().putString("history",limited.toString()).apply();
  }
  void updateStatus(String message) {
    statusView.setText(message);
    boolean available=ad!=null&&!loading&&!playing;
    watchBtn.setEnabled(available);watchBtn.setAlpha(available?1f:.55f);
    retryBtn.setVisibility(ad==null&&!loading&&!playing?View.VISIBLE:View.GONE);
  }
  void loadAd() {
    if(loading||playing||ad!=null||isFinishing())return;
    loading=true;updateStatus("Loading a test video…");
    RewardedAd.load(this,TEST_AD,new AdRequest.Builder().build(),new RewardedAdLoadCallback(){
      @Override public void onAdLoaded(RewardedAd loaded) {
        if(isFinishing()||isDestroyed())return;
        loading=false;ad=loaded;updateStatus("Video ready. Tap to watch.");
      }
      @Override public void onAdFailedToLoad(LoadAdError e) {
        loading=false;ad=null;updateStatus("Test ad not available. Check internet and retry.");
      }
    });
  }
  void showAd() {
    if(ad==null||playing)return;
    RewardedAd current=ad;ad=null;playing=true;awarded=false;
    updateStatus("Video playing…");
    current.setFullScreenContentCallback(new FullScreenContentCallback(){
      @Override public void onAdDismissedFullScreenContent(){playing=false;loadAd();}
      @Override public void onAdFailedToShowFullScreenContent(AdError e) {
        playing=false;Toast.makeText(MainActivity.this,"Could not show ad",Toast.LENGTH_SHORT).show();loadAd();
      }
    });
    current.show(this, reward->{
      if(awarded)return;
      awarded=true;
      int currentCoins=prefs.getInt("coins",0);
      int next=Math.addExact(currentCoins,5);
      prefs.edit().putInt("coins",next).apply();
      log("+5 coins • Completed rewarded ad");render();
      Toast.makeText(this,"+5 demo coins!",Toast.LENGTH_SHORT).show();
    });
  }
  void themeClick() {
    boolean owned=prefs.getBoolean("owned",false);
    if(owned) {
      prefs.edit().putBoolean("active",!prefs.getBoolean("active",false)).apply();render();return;
    }
    int coins=prefs.getInt("coins",0);
    if(coins<20){Toast.makeText(this,"20 coins required",Toast.LENGTH_SHORT).show();return;}
    new AlertDialog.Builder(this).setTitle("Unlock Neon Theme?")
      .setMessage("Spend 20 in-app demo coins?")
      .setNegativeButton("Cancel",null)
      .setPositiveButton("Unlock",(d,w)->{
        int latest=prefs.getInt("coins",0);
        if(latest<20)return;
        prefs.edit().putInt("coins",latest-20).putBoolean("owned",true).putBoolean("active",true).apply();
        log("-20 coins • Neon theme unlocked");render();
      }).show();
  }
}

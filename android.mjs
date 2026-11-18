import {createHash} from 'node:crypto';
import {mkdir,writeFile} from 'node:fs/promises';
import {dirname,join} from 'node:path';
import {esc,render} from './core.mjs';
export function androidFiles(spec,id,version=1){
 const app='studio.generated.p'+createHash('sha256').update(id).digest('hex').slice(0,20);
 return {
 'settings.gradle':"pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositories { google(); mavenCentral() } }\nrootProject.name='StudioApp'\ninclude ':app'\n",
 'app/build.gradle':`plugins { id 'com.android.application' version '8.7.3' }
android { namespace 'studio.runtime.app'; compileSdk 35; defaultConfig { applicationId '${app}'; minSdk 23; targetSdk 35; versionCode ${version}; versionName '1.${version}' } }`,
 'app/src/main/AndroidManifest.xml':'<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application android:allowBackup="false" android:theme="@android:style/Theme.Material.NoActionBar" android:label="@string/app_name"><activity android:name="studio.runtime.app.MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>',
 'app/src/main/res/values/strings.xml':'<resources><string name="app_name" formatted="false">"'+esc(spec.title.replaceAll('\\','\\\\').replaceAll('"','\\"').replaceAll("'","\\'"))+'"</string></resources>',
 'app/src/main/assets/index.html':render(spec,id),
 'app/src/main/java/studio/runtime/app/MainActivity.java':`package studio.runtime.app;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  WebView screen = new WebView(this);
  screen.getSettings().setJavaScriptEnabled(true);
  screen.getSettings().setDomStorageEnabled(true);
  screen.getSettings().setAllowContentAccess(false);
  screen.setWebViewClient(new android.webkit.WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v, android.webkit.WebResourceRequest r){ return true; }
  });
  setContentView(screen);
  screen.loadUrl("file:///android_asset/index.html");
 }
}`
 };
}
export async function materialize(files,dir){for(const [path,data] of Object.entries(files)){const dest=join(dir,path);await mkdir(dirname(dest),{recursive:true});await writeFile(dest,data)}}

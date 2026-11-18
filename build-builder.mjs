import {readFile,mkdir,writeFile} from 'node:fs/promises';
import {execFileSync} from 'node:child_process';
import {androidFiles,materialize} from './android.mjs';
import {starter} from './core.mjs';
const root='build/builder',files=androidFiles(starter('AI Builder'),'ai-builder-studio');
files['app/build.gradle']=files['app/build.gradle'].replace(/applicationId '[^']+'/,"applicationId 'studio.ai.builder.workspace'");
execFileSync(process.execPath,['live/build.mjs','build/site']);
files['app/src/main/assets/index.html']=await readFile('build/site/index.html','utf8');
files['app/src/main/java/studio/runtime/app/MainActivity.java']=`package studio.runtime.app;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.util.Base64;
import android.widget.Toast;
import java.io.*;
public final class MainActivity extends Activity {
 private byte[] pending;
 private WebView web;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  web=new WebView(this);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowContentAccess(false);
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req){
    Uri uri=req.getUrl();
    if(req.isForMainFrame() && "https".equals(uri.getScheme()) && "ai-app-builderyt-studio.pluckymusic2.chatgpt.site".equals(uri.getHost()))startActivity(new Intent(Intent.ACTION_VIEW,uri));
    return true;
   }
  });
  web.addJavascriptInterface(new Exports(),"BuilderFiles");
  setContentView(web);
  web.loadUrl("file:///android_asset/index.html");
 }
 public final class Exports {
  @JavascriptInterface public void save(String name,String encoded){
   if(!("Android-source.zip".equals(name)||"my-project.html".equals(name))||encoded==null||encoded.length()>4000000)return;
   runOnUiThread(()->{
    if(pending!=null)return;
    try{pending=Base64.decode(encoded,Base64.DEFAULT);
     Intent picker=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(name.endsWith(".zip")?"application/zip":"text/html").putExtra(Intent.EXTRA_TITLE,name);
     startActivityForResult(picker,21);
    }catch(RuntimeException e){pending=null;Toast.makeText(MainActivity.this,"Export unavailable",Toast.LENGTH_SHORT).show();}
   });
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request!=21)return;
  try{if(result==RESULT_OK&&pending!=null&&data!=null&&data.getData()!=null){
   try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out!=null)out.write(pending);}
   Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();
  }}catch(IOException e){Toast.makeText(this,"Save failed",Toast.LENGTH_SHORT).show();}finally{pending=null;}
 }
}`;
await materialize(files,root);

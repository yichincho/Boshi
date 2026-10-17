package tw.boshi.keyboard;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/** A short permission handoff from the IME; returns to the original app without recording. */
public class VoicePermissionActivity extends Activity {
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  TextView info=new TextView(this);info.setText("Boshi 語音輸入需要麥克風權限，將說話內容轉成文字。Boshi 不建立錄音檔。");
  info.setPadding(24,24,24,24);setContentView(info);
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){finish();return;}
  if(state==null)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
  super.onRequestPermissionsResult(request,permissions,grants);
  if(request!=1)return;
  boolean granted=grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED;
  Toast.makeText(this,granted?"已允許麥克風，回到輸入框後點 Boshi 的 🎤 開始":"尚未允許麥克風，可到 Boshi 應用程式權限中開啟",Toast.LENGTH_LONG).show();
  finish();
 }
}

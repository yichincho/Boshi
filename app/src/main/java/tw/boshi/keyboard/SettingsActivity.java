package tw.boshi.keyboard;
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.view.*;import android.widget.*;import java.io.*;
public class SettingsActivity extends Activity{
 private EditText key,model;private Spinner provider;private TextView status;private static final int PICK=23;
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 TextView title=new TextView(this);title.setText("Boshi AI Keyboard · 測試版");title.setTextSize(23);root.addView(title);
 TextView info=new TextView(this);info.setText("已內建開源嘸蝦米字碼表，離線即可使用，不需要下載或匯入。也可自行匯入 Rime YAML / TSV 覆蓋預設表。\n本版為獨立實驗鍵盤，尚非完整 Trime / 嘸蝦米。");root.addView(info);
 
 Button source=new Button(this);source.setText("查看開源字碼表來源與授權");root.addView(source);source.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/jdh8/ibus-boshiamy"))));
 Button load=new Button(this);load.setText("匯入 Rime YAML / TSV 字碼表");root.addView(load);load.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("*/*");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,PICK);});
 status=new TextView(this);root.addView(status);try{status.setText("已載入 "+new CodeDecoder().load(this)+" 筆字碼（內建或自訂）");}catch(Exception e){status.setText("字碼尚未匯入");}
 provider=new Spinner(this);provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"停用 AI","Gemini","NVIDIA"}));root.addView(provider);
 key=new EditText(this);key.setHint("API Key（不會上傳到 GitHub）");root.addView(key);
 model=new EditText(this);model.setHint("模型名稱，例如 gemini-2.5-flash");root.addView(model);
 android.content.SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);provider.setSelection(p.getInt("provider",0));key.setText(p.getString("key",""));model.setText(p.getString("model","gemini-2.5-flash"));
 Button save=new Button(this);save.setText("儲存 API 設定");root.addView(save);save.setOnClickListener(v->{p.edit().putInt("provider",provider.getSelectedItemPosition()).putString("key",key.getText().toString().trim()).putString("model",model.getText().toString().trim()).apply();Toast.makeText(this,"已儲存在手機內；本測試版未加密 API Key",Toast.LENGTH_LONG).show();});
 Button open=new Button(this);open.setText("開啟系統輸入法設定");root.addView(open);open.setOnClickListener(v->startActivity(new Intent("android.settings.INPUT_METHOD_SETTINGS")));
 }
 private void downloadOpenTable(Button btn){
  new AlertDialog.Builder(this).setTitle("下載開源嘸蝦米字碼表")
   .setMessage("來源：jdh8/ibus-boshiamy（GPLv3+）\\n會透過 HTTPS 從 GitHub 下載 boshiamy.txt，匯入後取代目前碼表。檔案保存在手機 App 私有空間。確定下載？")
   .setNegativeButton("取消",null).setPositiveButton("下載並匯入",(dialog,which)->{
    btn.setEnabled(false);status.setText("正在下載並匯入開源字碼表…");
    new Thread(()->{
     try{
      java.net.URL url=new java.net.URL("https://raw.githubusercontent.com/jdh8/ibus-boshiamy/master/boshiamy.txt");
      java.net.HttpURLConnection conn=(java.net.HttpURLConnection)url.openConnection();
      conn.setConnectTimeout(12000);conn.setReadTimeout(20000);conn.setInstanceFollowRedirects(true);
      try{
       if(conn.getResponseCode()!=200)throw new IOException("HTTP "+conn.getResponseCode());
       if(conn.getContentLengthLong()>5_000_000)throw new IOException("下載檔過大");
       try(InputStream in=new java.io.BufferedInputStream(conn.getInputStream())){
        runOnUiThread(()->status.setText("下載已連線，正在解析及匯入字碼…"));
        int imported=DictionaryImporter.importFile(this,in,"boshiamy.txt");
        int loaded=new CodeDecoder().load(this);
        runOnUiThread(()->status.setText("已匯入 "+imported+" 筆開源碼表；載入 "+loaded+" 筆"));
       }
      }finally{conn.disconnect();}
     }catch(Exception e){runOnUiThread(()->{status.setText("下載／匯入失敗："+e.getClass().getSimpleName()+"："+e.getMessage());Toast.makeText(this,"字碼匯入失敗，請查看畫面原因",Toast.LENGTH_LONG).show();});}
     finally{runOnUiThread(()->btn.setEnabled(true));}
    }).start();
   }).show();
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK||result!=RESULT_OK||data==null)return;
 try(InputStream in=getContentResolver().openInputStream(data.getData())){String name="";try(android.database.Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int idx=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(idx>=0)name=cursor.getString(idx);}}int imported=DictionaryImporter.importFile(this,in,name);int loaded=new CodeDecoder().load(this);status.setText("已匯入 "+imported+" 筆字碼；載入 "+loaded+" 筆");}catch(Exception e){status.setText("匯入失敗："+e.getMessage());}
 }
}

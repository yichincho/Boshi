package tw.boshi.keyboard;
import android.app.*;import android.os.*;import android.content.*;import android.net.Uri;import android.view.*;import android.widget.*;import java.io.*;
public class SettingsActivity extends Activity{
 private EditText key,model;private Spinner provider;private TextView status;private static final int PICK=23;
 @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,24,24,24);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
 TextView title=new TextView(this);title.setText("Boshi AI Keyboard · 測試版");title.setTextSize(23);root.addView(title);
 TextView info=new TextView(this);info.setText("可匯入 Rime .dict.yaml（字 TAB 字碼）或 UTF-8 TSV（字碼 TAB 字）。請自行確認碼表使用授權。\n本版為獨立實驗鍵盤，尚非完整 Trime / 嘸蝦米。");root.addView(info);
 Button load=new Button(this);load.setText("匯入 Rime YAML / TSV 字碼表");root.addView(load);load.setOnClickListener(v->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("*/*");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,PICK);});
 status=new TextView(this);root.addView(status);try{status.setText("已載入 "+new CodeDecoder().load(this)+" 筆字碼");}catch(Exception e){status.setText("字碼尚未匯入");}
 provider=new Spinner(this);provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"停用 AI","Gemini","NVIDIA"}));root.addView(provider);
 key=new EditText(this);key.setHint("API Key（不會上傳到 GitHub）");root.addView(key);
 model=new EditText(this);model.setHint("模型名稱，例如 gemini-2.5-flash");root.addView(model);
 android.content.SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);provider.setSelection(p.getInt("provider",0));key.setText(p.getString("key",""));model.setText(p.getString("model","gemini-2.5-flash"));
 Button save=new Button(this);save.setText("儲存 API 設定");root.addView(save);save.setOnClickListener(v->{p.edit().putInt("provider",provider.getSelectedItemPosition()).putString("key",key.getText().toString().trim()).putString("model",model.getText().toString().trim()).apply();Toast.makeText(this,"已儲存在手機內；本測試版未加密 API Key",Toast.LENGTH_LONG).show();});
 Button open=new Button(this);open.setText("開啟系統輸入法設定");root.addView(open);open.setOnClickListener(v->startActivity(new Intent("android.settings.INPUT_METHOD_SETTINGS")));
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK||result!=RESULT_OK||data==null)return;
 try(InputStream in=getContentResolver().openInputStream(data.getData())){String name="";try(android.database.Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int idx=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(idx>=0)name=cursor.getString(idx);}}int imported=DictionaryImporter.importFile(this,in,name);int loaded=new CodeDecoder().load(this);status.setText("已匯入 "+imported+" 筆字碼；載入 "+loaded+" 筆");}catch(Exception e){status.setText("匯入失敗："+e.getMessage());}
 }
}

package tw.boshi.keyboard;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.io.IOException;

public class SettingsActivity extends Activity {
 private EditText key, model;
 private Spinner provider;
 private TextView status;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout root=new LinearLayout(this);root.setPadding(24,32,24,24);root.setOrientation(LinearLayout.VERTICAL);
  ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
  TextView title=new TextView(this);title.setText("Boshi 嘸蝦米鍵盤 v0.4");title.setTextSize(23);root.addView(title);
  TextView intro=new TextView(this);intro.setText("已內建開源嘸蝦米字碼表，不需要下載、TSV 或 YAML 匯入。\n資料來源：jdh8/ibus-boshiamy（GPL 開源資料）。");root.addView(intro);
  status=new TextView(this);status.setTextSize(18);status.setPadding(0,20,0,20);root.addView(status);
  status.setText("正在檢查內建字碼表…");
  new Thread(()->{
   try {
    int loaded=new CodeDecoder().load(this);
    runOnUiThread(()->status.setText(loaded>10000?"✅ 已內建並載入 "+loaded+" 筆字碼，可以直接使用。":"⚠️ 字碼筆數不足："+loaded));
   } catch(Exception e) {
    runOnUiThread(()->status.setText("❌ 內建字碼表讀取失敗："+e.getClass().getSimpleName()));
   }
  }).start();
  Button enable=new Button(this);enable.setText("1. 啟用 Boshi 系統輸入法");root.addView(enable);
  enable.setOnClickListener(v->startActivity(new Intent("android.settings.INPUT_METHOD_SETTINGS")));
  Button choose=new Button(this);choose.setText("2. 切換使用 Boshi 鍵盤");root.addView(choose);
  choose.setOnClickListener(v->{
   android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
   if(imm!=null)imm.showInputMethodPicker();
  });
  TextView sub=new TextView(this);sub.setText("選用：AI 整句候選（不設定也能離線輸入）");sub.setTextSize(18);root.addView(sub);
  provider=new Spinner(this);provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"停用 AI","Gemini","NVIDIA"}));root.addView(provider);
  key=new EditText(this);key.setHint("個人 API Key");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(key);
  model=new EditText(this);model.setHint("模型 ID");root.addView(model);
  SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);provider.setSelection(p.getInt("provider",0));key.setText(p.getString("key",""));model.setText(p.getString("model","gemini-2.5-flash"));
  Button save=new Button(this);save.setText("儲存 AI 設定");root.addView(save);
  save.setOnClickListener(v->{p.edit().putInt("provider",provider.getSelectedItemPosition()).putString("key",key.getText().toString().trim()).putString("model",model.getText().toString().trim()).apply();Toast.makeText(this,"設定已儲存（測試版 Key 尚未加密）",Toast.LENGTH_LONG).show();});
  Button source=new Button(this);source.setText("開源字碼表來源與授權");root.addView(source);
  source.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/jdh8/ibus-boshiamy"))));
 }
}

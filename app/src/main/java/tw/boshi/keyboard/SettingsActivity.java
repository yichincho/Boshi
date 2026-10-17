package tw.boshi.keyboard;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends Activity {
 static final String[] PROVIDER_NAMES={"停用 AI","Gemini","NVIDIA"};
 static final String[] DEFAULT_MODELS={"","gemini-3.1-flash-lite","meta/llama-3.3-70b-instruct"};
 static final int[] KEY_HEIGHTS={48,56,64};
 private EditText key,model;
 private Button pick;
 private TextView status,keyHint;
 private SharedPreferences p;
 private SecureKeyStore store;
 private int provider;

 /** Model ID for a provider, falling back to v0.4's single "model" field and then the default. */
 static String model(SharedPreferences p,int provider){
  if(provider<1)return "";
  String legacy=p.getInt("provider",0)==provider?p.getString("model",""):"";
  String m=p.getString("model_"+SecureKeyStore.PROVIDERS[provider-1],legacy);
  return m.isEmpty()?DEFAULT_MODELS[provider]:m;
 }

 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  p=getSharedPreferences("config",MODE_PRIVATE);store=new SecureKeyStore(this);provider=p.getInt("provider",0);
  LinearLayout root=new LinearLayout(this);root.setPadding(dp(20),dp(16),dp(20),dp(32));root.setOrientation(LinearLayout.VERTICAL);
  ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
  root.addView(text("Boshi 嘸蝦米鍵盤 v0.6.1",26));
  root.addView(text("已內建開源嘸蝦米字碼表，不需要下載或匯入。\n資料來源：jdh8/ibus-boshiamy（GPL 開源資料）。",15));
  status=text("正在檢查內建字碼表…",17);status.setPadding(0,dp(12),0,dp(12));root.addView(status);
  new Thread(()->{
   try {
    int loaded=new CodeDecoder().load(this);
    runOnUiThread(()->status.setText(loaded>10000?"✅ 已內建並載入 "+loaded+" 筆字碼，可以直接使用。":"⚠️ 字碼筆數不足："+loaded));
   } catch(Exception e) {
    runOnUiThread(()->status.setText("❌ 內建字碼表讀取失敗："+e.getClass().getSimpleName()));
   }
  }).start();
  Button enable=button("1. 啟用 Boshi 系統輸入法");root.addView(enable);
  enable.setOnClickListener(v->startActivity(new Intent("android.settings.INPUT_METHOD_SETTINGS")));
  Button choose=button("2. 切換使用 Boshi 鍵盤");root.addView(choose);
  choose.setOnClickListener(v->{
   android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
   if(imm!=null)imm.showInputMethodPicker();
  });

  root.addView(section("鍵盤高度"));
  RadioGroup height=new RadioGroup(this);height.setOrientation(RadioGroup.HORIZONTAL);
  String[] heightNames={"標準","大","特大"};int currentHeight=p.getInt("key_height",KEY_HEIGHTS[1]);
  for(int i=0;i<KEY_HEIGHTS.length;i++){RadioButton r=radio(heightNames[i],1000+i);height.addView(r);if(KEY_HEIGHTS[i]==currentHeight)r.setChecked(true);}
  height.setOnCheckedChangeListener((g,id)->p.edit().putInt("key_height",KEY_HEIGHTS[id-1000]).apply());
  root.addView(height);

  root.addView(section("選用：AI 整句候選（不設定也能離線輸入）"));
  RadioGroup providers=new RadioGroup(this);providers.setOrientation(RadioGroup.HORIZONTAL);
  for(int i=0;i<PROVIDER_NAMES.length;i++)providers.addView(radio(PROVIDER_NAMES[i],2000+i));
  ((RadioButton)providers.getChildAt(provider)).setChecked(true);
  root.addView(providers);
  keyHint=text("",14);root.addView(keyHint);
  key=new EditText(this);key.setSingleLine(true);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(key);
  model=new EditText(this);model.setSingleLine(true);model.setHint("模型 ID");root.addView(model);
  pick=button("選擇模型（讀取目前可用清單）");root.addView(pick);
  pick.setOnClickListener(v->pickModel());
  LinearLayout actions=new LinearLayout(this);
  Button paste=button("貼上"),save=button("儲存"),clear=button("清除金鑰");
  for(Button x:new Button[]{paste,save,clear})actions.addView(x,new LinearLayout.LayoutParams(0,-2,1));
  root.addView(actions);
  Button test=button("測試連線");root.addView(test);
  Switch auto=new Switch(this);auto.setText("6 個中文字起，自動 AI 輔助句子候選");auto.setTextSize(16);auto.setPadding(0,dp(12),0,dp(4));auto.setChecked(p.getBoolean("auto_predict",true));root.addView(auto);
  auto.setOnCheckedChangeListener((x,on)->p.edit().putBoolean("auto_predict",on).apply());
  Switch habits=new Switch(this);habits.setText("AI 聯想參考我的常用詞（最多 30 個常用詞會一起送給 AI）");habits.setTextSize(16);habits.setPadding(0,dp(8),0,dp(4));habits.setChecked(p.getBoolean("ai_habits",true));root.addView(habits);
  habits.setOnCheckedChangeListener((x,on)->p.edit().putBoolean("ai_habits",on).apply());
  root.addView(text("每位使用者需自行輸入 API Key；金鑰以 Android Keystore 加密保存。1～5 個中文字只用一般候選；6 字起才使用 AI。自動聯想會送出目前句子（游標前最多 120 個 UTF-16 字元）；有待選字碼時，自動聯想或按 AI ✨ 會送出目前解碼的中文候選。密碼欄位不會使用 AI。",14));
  addLearningSection(root);
  Button source=button("開源字碼表來源與授權");root.addView(source);
  source.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/jdh8/ibus-boshiamy"))));

  showProvider();
  providers.setOnCheckedChangeListener((g,id)->{provider=id-2000;p.edit().putInt("provider",provider).apply();showProvider();});
  paste.setOnClickListener(v->{
   ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
   if(cm!=null&&cm.hasPrimaryClip())key.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this).toString().trim());else toast("剪貼簿沒有文字");
  });
  save.setOnClickListener(v->{if(saveSettings())toast("API Key 已加密儲存");});
  clear.setOnClickListener(v->{if(provider<1)return;store.clear(SecureKeyStore.PROVIDERS[provider-1]);key.setText("");toast(PROVIDER_NAMES[provider]+" API Key 已清除");});
  test.setOnClickListener(v->{
   if(!saveSettings())return;
   int target=provider;String k=store.get(SecureKeyStore.PROVIDERS[target-1]),m=model(p,target);
   test.setEnabled(false);
   new Thread(()->{
    String msg;
    try{List<String> r=AiClient.query(target,k,m,"今天天氣很好");msg=r.isEmpty()?"已連線，但沒有取得候選":"✅ 連線成功："+r.get(0);}
    catch(Exception e){msg="❌ 連線失敗："+e.getMessage();}
    String shown=msg;runOnUiThread(()->{test.setEnabled(true);toast(shown);});
   }).start();
  });
 }

 private void showProvider(){
  boolean on=provider>0;
  for(android.view.View v:new android.view.View[]{key,model,pick})v.setEnabled(on);
  if(!on){keyHint.setText("AI 已停用，鍵盤只使用離線字碼表。");key.setText("");key.setHint("");model.setText("");return;}
  keyHint.setText(provider==1?"到 Google AI Studio（aistudio.google.com）建立 Gemini API Key 後貼上。":"到 build.nvidia.com 建立 NVIDIA API Key（nvapi- 開頭）後貼上，再按「選擇模型」挑一個目前可用的模型。");
  key.setHint(provider==1?"貼上 Gemini API Key":"貼上 NVIDIA API Key");
  key.setText(store.get(SecureKeyStore.PROVIDERS[provider-1]));
  model.setText(model(p,provider));
 }

 /** Lists common models first (only those still offered when the live list loads), then every other chat model. */
 private void pickModel(){
  if(provider<1)return;
  int target=provider;String k=key.getText().toString().trim();
  pick.setEnabled(false);pick.setText("讀取模型清單中…");
  new Thread(()->{
   List<String> live=null;String error=null;
   try{live=AiClient.listModels(target,k);}catch(Exception e){error=e.getMessage();}
   List<String> items=new ArrayList<>();
   for(String m:AiClient.COMMON_MODELS[target])if(live==null||live.contains(m))items.add(m);
   int common=items.size();
   if(live!=null)for(String m:live)if(!items.contains(m))items.add(m);
   String title=error!=null?"無法讀取即時清單，先顯示常用模型\n"+error:"可用模型 "+items.size()+" 個（★ 為常用）";
   runOnUiThread(()->{
    pick.setEnabled(true);pick.setText("選擇模型（讀取目前可用清單）");
    if(target!=provider)return;
    String[] labels=new String[items.size()];for(int i=0;i<labels.length;i++)labels[i]=(i<common?"★ ":"")+items.get(i);
    new android.app.AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,i)->model.setText(items.get(i))).setNegativeButton("取消",null).show();
   });
  }).start();
 }

 private boolean saveSettings(){
  if(provider<1){toast("請先選擇 AI 服務");return false;}
  String k=key.getText().toString().trim(),m=model.getText().toString().trim();
  if(k.isEmpty()){toast("請輸入 API Key");return false;}
  try{store.save(SecureKeyStore.PROVIDERS[provider-1],k);}catch(Exception e){toast("無法儲存，請重新嘗試");return false;}
  p.edit().putString("model_"+SecureKeyStore.PROVIDERS[provider-1],m.isEmpty()?DEFAULT_MODELS[provider]:m).apply();
  return true;
 }

 /** 常用詞學習: the on-device SQLite store, how often it is analysed, its stats and a reset. */
 private void addLearningSection(LinearLayout root){
  root.addView(section("常用詞學習（SQLite，只存在這支手機）"));
  Switch learn=new Switch(this);learn.setText("記錄我選的字詞，常用的排前面並自動接續");learn.setTextSize(16);learn.setChecked(p.getBoolean("learn",true));root.addView(learn);
  learn.setOnCheckedChangeListener((x,on)->p.edit().putBoolean("learn",on).apply());
  root.addView(text("自動分析間隔",15));
  RadioGroup interval=new RadioGroup(this);interval.setOrientation(RadioGroup.HORIZONTAL);
  String[] names={"每小時","每 6 小時","每天"};int current=p.getInt("analyze_hours",6);
  for(int i=0;i<AnalysisJob.HOURS.length;i++){RadioButton r=radio(names[i],3000+i);interval.addView(r);if(AnalysisJob.HOURS[i]==current)r.setChecked(true);}
  interval.setOnCheckedChangeListener((g,id)->{p.edit().putInt("analyze_hours",AnalysisJob.HOURS[id-3000]).apply();AnalysisJob.schedule(this);});
  root.addView(interval);
  TextView stats=text("讀取學習資料中…",14);root.addView(stats);
  LinearLayout actions=new LinearLayout(this);
  Button analyze=button("立即分析"),reset=button("清除學習資料");
  actions.addView(analyze,new LinearLayout.LayoutParams(0,-2,1));actions.addView(reset,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
  root.addView(text("分析會淡化 30 天沒用的字詞、整理常一起出現的片語；不會上傳。密碼欄位與標示「不要學習」的欄位不記錄。",14));
  try{AnalysisJob.schedule(this);}catch(Exception ignored){}
  Runnable load=()->new Thread(()->{String t;try{t=UserDict.get(this).summary()+lastAnalyzed();}catch(Exception e){t="無法讀取學習資料："+e.getMessage();}String shown=t;runOnUiThread(()->stats.setText(shown));}).start();
  load.run();
  analyze.setOnClickListener(v->{analyze.setEnabled(false);new Thread(()->{String t;try{t=AnalysisJob.runNow(this)+lastAnalyzed();}catch(Exception e){t="分析失敗："+e.getMessage();}String shown=t;runOnUiThread(()->{analyze.setEnabled(true);stats.setText(shown);toast("分析完成");});}).start();});
  reset.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("清除所有學習資料？").setMessage("常用字詞順序與接續詞會回到預設。").setPositiveButton("清除",(d,w)->{UserDict.get(this).clear();stats.postDelayed(load,300);toast("已清除");}).setNegativeButton("取消",null).show());
 }
 private String lastAnalyzed(){long at=p.getLong("analyzed_at",0);return at==0?"\n尚未分析":"\n上次分析："+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(at));}

 private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setPadding(0,dp(6),0,dp(6));return t;}
 private TextView section(String s){TextView t=text(s,18);t.setPadding(0,dp(20),0,dp(4));return t;}
 private Button button(String s){Button x=new Button(this);x.setText(s);x.setAllCaps(false);x.setMinHeight(dp(48));return x;}
 private RadioButton radio(String s,int id){RadioButton r=new RadioButton(this);r.setText(s);r.setId(id);r.setTextSize(16);r.setPadding(0,0,dp(12),0);return r;}
 private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}


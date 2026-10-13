package tw.boshi.keyboard;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

public class BoshiImeService extends InputMethodService {
 private static final String[] LETTERS={"qwertyuiop","asdfghjkl","zxcvbnm"};
 private static final String[] SYMBOLS={"1234567890","@#$%&-+()/","*\"':;!?"};
 private LinearLayout root,bar,keys;private TextView preedit;private final StringBuilder buffer=new StringBuilder();private CodeDecoder decoder;private List<String> candidates=new ArrayList<>();private int epoch=0;private boolean secure=false,symbols=false,english=false,shift=false;
 private final Handler handler=new Handler(android.os.Looper.getMainLooper());private Runnable repeat;
 private final Runnable predictTask=this::predict;private final Map<String,List<String>> predictCache=new LinkedHashMap<String,List<String>>(){protected boolean removeEldestEntry(Map.Entry<String,List<String>> e){return size()>30;}};
 private int keyBg,keyFn,keyPressed,keyText,barText,accent;private int keyHeight;
 private UserDict dict;private String lastWord="";private boolean noLearn=false;

 private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(15);t.setTextColor(barText);t.setPadding(dp(10),0,dp(10),0);t.setGravity(Gravity.CENTER_VERTICAL);return t;}

 private void loadTheme(){
  boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  keyBg=dark?0xff3c4043:0xffffffff;keyFn=dark?0xff2a2c2e:0xffc4c9d0;keyPressed=dark?0xff5f6368:0xffaab2bd;
  keyText=dark?0xffe8eaed:0xff202124;barText=dark?0xffbdc1c6:0xff5f6368;accent=dark?0xff8ab4f8:0xff1a73e8;
  root.setBackgroundColor(dark?0xff202124:0xffe8eaed);
  SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);keyHeight=p.getInt("key_height",56);
 }

 @Override public View onCreateInputView(){
  decoder=new CodeDecoder();try{decoder.load(this);}catch(Exception ignored){}
  dict=UserDict.get(this);try{AnalysisJob.schedule(this);}catch(Exception ignored){}
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(3),0,dp(3),dp(6));
  loadTheme();
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top,new LinearLayout.LayoutParams(-1,dp(48)));
  preedit=label("");preedit.setTextColor(accent);preedit.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);top.addView(preedit,new LinearLayout.LayoutParams(-2,-1));
  HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);scroll.addView(bar,new FrameLayout.LayoutParams(-2,-1));top.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
  keys=new LinearLayout(this);keys.setOrientation(LinearLayout.VERTICAL);root.addView(keys);
  buildKeys();refresh();return root;
 }

 @Override public void onStartInputView(EditorInfo info,boolean restarting){
  super.onStartInputView(info,restarting);
  int h=getSharedPreferences("config",MODE_PRIVATE).getInt("key_height",56);
  if(keys!=null&&h!=keyHeight){keyHeight=h;buildKeys();}
 }

 private void buildKeys(){
  keys.removeAllViews();String[] rows=symbols?SYMBOLS:LETTERS;
  for(int r=0;r<3;r++){
   LinearLayout line=row();
   if(r==1&&!symbols)spacer(line,0.5f);
   if(r==2){
    if(symbols)addKey(line,english?",":"，",()->symbol(english?",":"，"),1.5f,true);
    else if(english){TextView sh=addKey(line,"⇧",()->{shift=!shift;buildKeys();},1.5f,true);if(shift)sh.setTextColor(accent);}
    else addKey(line,"AI ✨",this::ai,1.5f,true);
   }
   for(char ch:rows[r].toCharArray()){String s=String.valueOf(ch);if(symbols)addKey(line,s,()->symbol(s),1,false);else{String shown=english&&!shift?s:s.toUpperCase(Locale.ROOT);addKey(line,shown,()->type(ch),1,false);}}
   if(r==1&&!symbols)spacer(line,0.5f);
   if(r==2){TextView del=addKey(line,"⌫",this::back,1.5f,true);repeatOnHold(del);}
  }
  LinearLayout bottom=row();
  TextView lang=addKey(bottom,english?"英":"中",this::toggleLanguage,1.2f,true);lang.setTextColor(accent);lang.setTypeface(null,Typeface.BOLD);
  addKey(bottom,symbols?"ABC":"?123",()->{symbols=!symbols;buildKeys();},1.3f,true);
  String comma=english?",":(symbols?"、":"，"),period=english?".":"。";
  addKey(bottom,comma,()->symbol(comma),1,true);
  addKey(bottom,english?"English":"嘸蝦米",this::space,3.5f,false);
  addKey(bottom,period,()->symbol(period),1,true);
  TextView enter=addKey(bottom,"↵",this::enter,1.5f,true);enter.setTextColor(0xffffffff);enter.setBackground(keyDrawable(accent,keyPressed));
 }

 private LinearLayout row(){LinearLayout l=new LinearLayout(this);keys.addView(l,new LinearLayout.LayoutParams(-1,dp(keyHeight)));return l;}
 private void spacer(LinearLayout row,float weight){row.addView(new View(this),new LinearLayout.LayoutParams(0,-1,weight));}
 private GradientDrawable shape(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(6));return g;}
 private StateListDrawable keyDrawable(int normal,int pressed){StateListDrawable s=new StateListDrawable();s.addState(new int[]{android.R.attr.state_pressed},shape(pressed));s.addState(new int[]{},shape(normal));return s;}

 private TextView addKey(LinearLayout row,String s,Runnable action,float weight,boolean fn){
  TextView k=new TextView(this);k.setText(s);k.setGravity(Gravity.CENTER);k.setTextColor(keyText);
  k.setTextSize(s.length()==1&&!fn?24:(s.length()>3?15:18));
  k.setBackground(keyDrawable(fn?keyFn:keyBg,keyPressed));k.setElevation(dp(1));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,weight);lp.setMargins(dp(3),dp(4),dp(3),dp(4));row.addView(k,lp);
  k.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);action.run();});
  return k;
 }

 /** Keep deleting while ⌫ is held, like system keyboards. */
 private void repeatOnHold(TextView del){
  del.setOnLongClickListener(v->{repeat=new Runnable(){public void run(){back();handler.postDelayed(this,60);}};handler.post(repeat);return true;});
  del.setOnTouchListener((v,e)->{if((e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)&&repeat!=null){handler.removeCallbacks(repeat);repeat=null;}return false;});
 }

 @Override public void onStartInput(EditorInfo info,boolean restarting){super.onStartInput(info,restarting);int variation=info.inputType&android.text.InputType.TYPE_MASK_VARIATION;secure=variation==android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;int cls=info.inputType&android.text.InputType.TYPE_MASK_CLASS;if(cls==android.text.InputType.TYPE_CLASS_NUMBER||cls==android.text.InputType.TYPE_CLASS_PHONE)secure=true;noLearn=(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0;lastWord="";handler.removeCallbacks(predictTask);buffer.setLength(0);epoch++;refresh();}
 private void type(char c){
  if(english){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(String.valueOf(shift?Character.toUpperCase(c):c),1);if(shift){shift=false;buildKeys();}return;}
  if(buffer.length()<48)buffer.append(c);refresh();}

 /** 中/英 key: English types letters straight into the field; pending Boshiamy codes are committed first. */
 private void toggleLanguage(){
  if(buffer.length()>0)space();
  handler.removeCallbacks(predictTask);english=!english;shift=false;symbols=false;buildKeys();refresh();
 }
 private void back(){if(buffer.length()>0){buffer.deleteCharAt(buffer.length()-1);refresh();}else{lastWord="";if(getCurrentInputConnection()!=null)getCurrentInputConnection().deleteSurroundingText(1,0);}}
 private void commit(String s){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);buffer.setLength(0);epoch++;refresh();List<String> local=localSuggestions();if(!local.isEmpty())showPredictions(local);if(!s.trim().isEmpty())schedulePredict();}

 private boolean learnEnabled(){return dict!=null&&!secure&&!noLearn&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("learn",true);}
 /** Saves the pick to the on-device SQLite store (code may be null for a suggestion) and remembers it as context. */
 private void learn(String code,String word){if(!english&&learnEnabled())dict.record(code,lastWord,word);lastWord=word;}
 /** Words this user typed after the previous one before; instant and offline. */
 private List<String> localSuggestions(){return english||!learnEnabled()?new ArrayList<>():dict.suggestions(lastWord,5);}

 /** Settings needed to call the AI, or null when AI is off or has no key. */
 private String[] aiConfig(){SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);int provider=p.getInt("provider",0);if(provider<1)return null;String key=new SecureKeyStore(this).get(SecureKeyStore.PROVIDERS[provider-1]);return key.isEmpty()?null:new String[]{String.valueOf(provider),key,SettingsActivity.model(p,provider)};}
 private boolean autoPredict(){return !secure&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("auto_predict",true);}

 /** After a pick, wait briefly so quick typing does not send one request per character. */
 private void schedulePredict(){handler.removeCallbacks(predictTask);if(autoPredict())handler.postDelayed(predictTask,400);}

 /** Shows AI follow-up suggestions for the text before the cursor; tapping one inserts it and predicts again. */
 private void predict(){
  if(buffer.length()>0||!autoPredict()||bar==null)return;
  String[] cfg=aiConfig();if(cfg==null)return;
  InputConnection ic=getCurrentInputConnection();CharSequence cs=ic==null?null:ic.getTextBeforeCursor(60,0);
  String before=cs==null?"":cs.toString().trim();if(before.isEmpty())return;
  int requestEpoch=epoch;List<String> local=localSuggestions(),cached=predictCache.get(before);
  if(cached!=null){showPredictions(merge(local,cached));return;}
  List<String> habits=learnEnabled()&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("ai_habits",true)?dict.habits(lastWord):new ArrayList<>();
  if(local.isEmpty())bar.removeAllViews();bar.addView(label("AI 聯想中…"),new LinearLayout.LayoutParams(-2,-1));
  new Thread(()->{
   try{List<String> list=AiClient.predict(Integer.parseInt(cfg[0]),cfg[1],cfg[2],before,habits);handler.post(()->{predictCache.put(before,list);if(requestEpoch==epoch&&buffer.length()==0)showPredictions(merge(local,list));});}
   catch(Exception e){handler.post(()->{if(requestEpoch==epoch&&buffer.length()==0){showPredictions(local);bar.addView(label("AI 聯想失敗："+e.getMessage()),new LinearLayout.LayoutParams(-2,-1));}});}
  }).start();
 }
 private void showPredictions(List<String> list){bar.removeAllViews();if(list.isEmpty()){bar.addView(label("沒有聯想候選"),new LinearLayout.LayoutParams(-2,-1));return;}for(int i=0;i<list.size();i++)candidate(list.get(i),i==0,null);}
 private static List<String> merge(List<String> a,List<String> b){LinkedHashSet<String> s=new LinkedHashSet<>(a);s.addAll(b);return new ArrayList<>(s);}
 private void symbol(String s){if(english){commitPlain(s);return;}if(buffer.length()>0){String w=candidates.isEmpty()?buffer.toString():candidates.get(0);if(!candidates.isEmpty())learn(buffer.toString(),w);lastWord="";commit(w+s);}else{lastWord="";commit(s);}}
 private void commitPlain(String s){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);}
 private void space(){if(english){commitPlain(" ");return;}if(buffer.length()>0){if(candidates.isEmpty()){lastWord="";commit(buffer.toString());}else{String w=candidates.get(0);learn(buffer.toString(),w);commit(w);}}else{lastWord="";commit(" ");}}
 private void enter(){if(buffer.length()>0)space();lastWord="";InputConnection ic=getCurrentInputConnection();if(ic!=null){ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));}}

 /** A candidate that is learned when tapped: code is the typed Boshiamy code, or null for a next-word suggestion. */
 private void candidate(String word,boolean first,String code){candidate(word,first);bar.getChildAt(bar.getChildCount()-1).setOnClickListener(v->{learn(code,word);commit(word);});}
 private void candidate(String word,boolean first){
  TextView t=label(word);t.setTextSize(20);t.setTextColor(first?accent:keyText);if(first)t.setTypeface(null,Typeface.BOLD);
  t.setBackground(keyDrawable(0x00000000,keyPressed));t.setPadding(dp(14),0,dp(14),0);
  bar.addView(t,new LinearLayout.LayoutParams(-2,-1));t.setOnClickListener(v->commit(word));
 }
 private void refresh(){if(preedit==null||decoder==null)return;epoch++;String raw=buffer.toString();preedit.setText(raw);preedit.setVisibility(raw.isEmpty()?View.GONE:View.VISIBLE);candidates=decoder.decode(raw);if(dict!=null&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("learn",true))candidates=dict.preferred(raw,candidates);bar.removeAllViews();if(raw.isEmpty()){bar.addView(label(english?"英文模式 · 點左下角「英」切回嘸蝦米":aiConfig()==null?"輸入字碼開始打字 · 設定 AI Key 後選字會自動聯想":"輸入字碼開始打字"),new LinearLayout.LayoutParams(-2,-1));return;}
 if(candidates.isEmpty())bar.addView(label("未匹配字碼；空白鍵輸出英文"),new LinearLayout.LayoutParams(-2,-1));
 for(int i=0;i<candidates.size();i++)candidate(candidates.get(i),i==0,raw);
 }
 private void ai(){if(secure){Toast.makeText(this,"密碼欄位不使用 AI",Toast.LENGTH_SHORT).show();return;}if(buffer.length()==0)return;String[] cfg=aiConfig();if(cfg==null){Toast.makeText(this,"請先開啟 Boshi 設定並填寫 AI Key",Toast.LENGTH_SHORT).show();return;}
 String text=candidates.isEmpty()?buffer.toString():candidates.get(0);int requestEpoch=epoch;bar.removeAllViews();bar.addView(label("AI 生成中…"),new LinearLayout.LayoutParams(-2,-1));
 new Thread(()->{try{List<String> list=AiClient.query(Integer.parseInt(cfg[0]),cfg[1],cfg[2],text);handler.post(()->{if(requestEpoch!=epoch||bar==null)return;bar.removeAllViews();for(int i=0;i<list.size();i++)candidate(list.get(i),i==0);if(list.isEmpty())bar.addView(label("無有效候選"),new LinearLayout.LayoutParams(-2,-1));});}
 catch(Exception e){handler.post(()->{if(requestEpoch==epoch){refresh();Toast.makeText(this,"AI 失敗："+e.getMessage(),Toast.LENGTH_SHORT).show();}});}}).start();
 }
}

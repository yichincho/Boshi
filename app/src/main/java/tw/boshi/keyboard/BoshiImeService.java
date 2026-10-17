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
 private QuickPhraseBook quickPhrases=new QuickPhraseBook();private UserDict dict;private OfflineSuggestions offline;private TextView voiceKey;private VoiceInputController voice;private InputConnection voiceTarget;private String lastWord="";private boolean noLearn=false;

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
  offline=new OfflineSuggestions();
  try{offline.load(new java.io.InputStreamReader(getAssets().open("offline_phrases.txt"),"UTF-8"));}
  catch(Exception e){Toast.makeText(this,"內建聯想詞庫載入失敗，請重新安裝新版 APK",Toast.LENGTH_LONG).show();}
  dict=UserDict.get(this);try{AnalysisJob.schedule(this);}catch(Exception ignored){}
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(3),0,dp(3),dp(6));
  loadTheme();
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top,new LinearLayout.LayoutParams(-1,dp(48)));
  preedit=label("");preedit.setTextColor(accent);preedit.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);top.addView(preedit,new LinearLayout.LayoutParams(-2,-1));
  HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);scroll.addView(bar,new FrameLayout.LayoutParams(-2,-1));top.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
  voiceKey=label("🎤");voiceKey.setTextSize(23);voiceKey.setGravity(Gravity.CENTER);
  voiceKey.setTextColor(accent);voiceKey.setBackground(keyDrawable(keyFn,keyPressed));
  voiceKey.setContentDescription("開始語音輸入");
  top.addView(voiceKey,new LinearLayout.LayoutParams(dp(48),-1));voiceKey.setOnClickListener(v->toggleVoice());
  if(voice==null)voice=new VoiceInputController(this,new VoiceInputController.Listener(){
   public void onState(String text,boolean listening,boolean active){
    if(voiceKey!=null){voiceKey.setText(active?(listening?"■":"…"):"🎤");voiceKey.setContentDescription(active?(listening?"停止語音輸入":"正在辨識語音"):"開始語音輸入");}
    if(active){bar.removeAllViews();bar.addView(label(text),new LinearLayout.LayoutParams(-2,-1));}
    else refresh();
   }
   public void onText(String text){
    InputConnection target=voiceTarget;voiceTarget=null;
    if(target!=null&&target==getCurrentInputConnection()&&!secure){lastWord="";commit(text);}
   }
   public void onError(String message){voiceTarget=null;Toast.makeText(BoshiImeService.this,message,Toast.LENGTH_LONG).show();}
  });
  keys=new LinearLayout(this);keys.setOrientation(LinearLayout.VERTICAL);root.addView(keys);
  loadQuickPhrases();buildKeys();refresh();return root;
 }

 @Override public void onStartInputView(EditorInfo info,boolean restarting){
  super.onStartInputView(info,restarting);
  int h=getSharedPreferences("config",MODE_PRIVATE).getInt("key_height",56);
  if(keys!=null&&h!=keyHeight){keyHeight=h;buildKeys();}
  loadQuickPhrases();refresh();if(buffer.length()==0)schedulePredict();
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
  String comma=english?",":(symbols?"、":"，"),period=".";
  addKey(bottom,comma,()->symbol(comma),1,true);
  addKey(bottom,english?"English":"嘸蝦米",this::space,3.5f,false);
  addKey(bottom,period,this::period,1,true);
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

 @Override public void onStartInput(EditorInfo info,boolean restarting){super.onStartInput(info,restarting);cancelVoice();int variation=info.inputType&android.text.InputType.TYPE_MASK_VARIATION;secure=variation==android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;int cls=info.inputType&android.text.InputType.TYPE_MASK_CLASS;if(cls==android.text.InputType.TYPE_CLASS_NUMBER||cls==android.text.InputType.TYPE_CLASS_PHONE)secure=true;noLearn=(info.imeOptions&EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0;lastWord="";handler.removeCallbacks(predictTask);buffer.setLength(0);epoch++;refresh();}
 private void type(char c){
  cancelVoice();
  if(english){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(String.valueOf(shift?Character.toUpperCase(c):c),1);if(shift){shift=false;buildKeys();}return;}
  if(buffer.length()<48)buffer.append(c);refresh();}

 /** 中/英 key: English types letters straight into the field; pending Boshiamy codes are committed first. */
 private void toggleLanguage(){
  cancelVoice();
  if(buffer.length()>0)space();
  handler.removeCallbacks(predictTask);english=!english;shift=false;symbols=false;buildKeys();refresh();
 }
 private void back(){cancelVoice();handler.removeCallbacks(predictTask);epoch++;if(buffer.length()>0){buffer.deleteCharAt(buffer.length()-1);refresh();}else{lastWord="";if(getCurrentInputConnection()!=null)getCurrentInputConnection().deleteSurroundingText(1,0);refresh();schedulePredict();}}
 private void commit(String s){cancelVoice();InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);buffer.setLength(0);epoch++;refresh();if(!s.trim().isEmpty())schedulePredict();}

 private boolean learnEnabled(){return dict!=null&&!secure&&!noLearn&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("learn",true);}
 /** Saves the pick to the on-device SQLite store (code may be null for a suggestion) and remembers it as context. */
 private void learn(String code,String word){if(!english&&learnEnabled())dict.record(code,lastWord,word);lastWord=word;}
 /** Personal followers first, then bundled phrase completions, even with no learning history. */
 private List<String> localSuggestions(){
  if(english||secure||offline==null)return new ArrayList<>();
  String context=beforeCursor();if(context.isEmpty())return new ArrayList<>();
  String head=!lastWord.isEmpty()&&context.endsWith(lastWord)?lastWord:context.substring(context.offsetByCodePoints(context.length(),-1));
  List<String> learned=learnEnabled()?dict.suggestions(head,8):new ArrayList<>();
  return OfflineSuggestions.combine(learned,offline.suggest(context,8),8);
 }

 /** Settings needed to call the AI, or null when AI is off or has no key. */
 private String[] aiConfig(){SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);int provider=p.getInt("provider",0);if(provider<1)return null;String key=new SecureKeyStore(this).get(SecureKeyStore.PROVIDERS[provider-1]);return key.isEmpty()?null:new String[]{String.valueOf(provider),key,SettingsActivity.model(p,provider)};}
 private boolean autoPredict(){return !secure&&!english&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("auto_predict",true);}

 /** After a pick, wait briefly so quick typing does not send one request per character. */
 private void schedulePredict(){handler.removeCallbacks(predictTask);if(autoPredict())handler.postDelayed(predictTask,400);}

 /** The current sentence only; earlier sentences do not satisfy the six-character gate. */
 private String beforeCursor(){
  InputConnection ic=getCurrentInputConnection();CharSequence cs=ic==null?null:ic.getTextBeforeCursor(120,0);
  return InputRules.sentence(cs==null?"":cs.toString());
 }
 private String composedText(){return candidates.isEmpty()?"":candidates.get(0);}

 /** Automatic requests keep local candidates visible, then append AI suggestions. */
 private void predict(){requestAi(false);}
 private void requestAi(boolean manual){
  handler.removeCallbacks(predictTask);
  if(secure||english||bar==null||(!manual&&!autoPredict()))return;
  final String raw=buffer.toString();final boolean composing=!raw.isEmpty();
  if(composing&&quickPhrases.find(raw)!=null){
   if(manual)Toast.makeText(this,"點選 📌 片語名稱即可插入完整原文",Toast.LENGTH_SHORT).show();
   return;
  }
  // With pending codes, AI suggests complete replacements; otherwise it suggests continuations.
  final String text=composing?composedText():beforeCursor();
  if(!InputRules.useAi(text)){
   if(manual)Toast.makeText(this,"1～5 個中文字使用一般候選，6 字起才使用 AI",Toast.LENGTH_SHORT).show();
   return;
  }
  final String[] cfg=aiConfig();
  if(cfg==null){if(manual)Toast.makeText(this,"請先開啟 Boshi 設定並填寫 AI Key",Toast.LENGTH_SHORT).show();return;}
  final int requestEpoch=++epoch;
  final String before=beforeCursor();
  final String cacheKey=cfg[0]+":"+cfg[2]+":"+(composing?"compose:":"next:")+text;
  final List<String> local=composing?new ArrayList<>(candidates):localSuggestions();
  List<String> cached=predictCache.get(cacheKey);
  if(cached!=null){showAiCandidates(merge(local,cached),composing?raw:null,local.size());return;}
  final List<String> habits=learnEnabled()&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("ai_habits",true)?dict.habits(lastWord):new ArrayList<>();
  showAiCandidates(local,composing?raw:null,local.size());
  bar.addView(label("AI 聯想中…"),new LinearLayout.LayoutParams(-2,-1));
  new Thread(()->{
   try{
    List<String> list=composing?AiClient.query(Integer.parseInt(cfg[0]),cfg[1],cfg[2],text):AiClient.predict(Integer.parseInt(cfg[0]),cfg[1],cfg[2],text,habits);
    handler.post(()->{
     if(!currentRequest(requestEpoch,raw,before,manual))return;
     predictCache.put(cacheKey,list);showAiCandidates(merge(local,list),composing?raw:null,local.size());
    });
   }catch(Exception e){handler.post(()->{
    if(!currentRequest(requestEpoch,raw,before,manual))return;
    showAiCandidates(local,composing?raw:null,local.size());
    Toast.makeText(this,"AI 聯想失敗，保留一般候選",Toast.LENGTH_SHORT).show();
   });}
  }).start();
 }
 private boolean currentRequest(int requestEpoch,String raw,String before,boolean manual){
  return requestEpoch==epoch&&!secure&&!english&&bar!=null&&(manual||autoPredict())
   &&raw.equals(buffer.toString())&&before.equals(beforeCursor());
 }
 private void showAiCandidates(List<String> list,String code,int localCount){
  bar.removeAllViews();
  for(int i=0;i<list.size();i++){
   if(code!=null&&i>=localCount)candidate(list.get(i),false); // AI rewrites are not literal dictionary mappings.
   else candidate(list.get(i),i==0,code);
  }
 }
 private void showPredictions(List<String> list){showAiCandidates(list,null,list.size());}
 private static List<String> merge(List<String> a,List<String> b){LinkedHashSet<String> s=new LinkedHashSet<>(a);s.addAll(b);return new ArrayList<>(s);}
 private void period(){
  String mark=InputRules.period(buffer.toString(),english);
  if("？".equals(mark)){buffer.setLength(0);lastWord="";commit(mark);}
  else symbol(mark);
 }
 private void symbol(String s){cancelVoice();if(english){commitPlain(s);return;}if(buffer.length()>0){String w=candidates.isEmpty()?buffer.toString():candidates.get(0);if(!candidates.isEmpty())learn(buffer.toString(),w);lastWord="";commit(w+s);}else{lastWord="";commit(s);}}
 private void commitPlain(String s){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);}
 private void space(){cancelVoice();if(english){commitPlain(" ");return;}if(buffer.length()>0){if(candidates.isEmpty()){lastWord="";commit(buffer.toString());}else{String w=candidates.get(0);learn(buffer.toString(),w);commit(w);}}else{lastWord="";commit(" ");}}
 private void enter(){cancelVoice();if(buffer.length()>0)space();lastWord="";InputConnection ic=getCurrentInputConnection();if(ic!=null){ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));}}

 /** A candidate that is learned when tapped: code is the typed Boshiamy code, or null for a next-word suggestion. */
 private void candidate(String word,boolean first,String code){candidate(word,first);bar.getChildAt(bar.getChildCount()-1).setOnClickListener(v->{learn(code,word);commit(word);});}
 private void candidate(String word,boolean first){
  TextView t=label(word);t.setTextSize(20);t.setTextColor(first?accent:keyText);if(first)t.setTypeface(null,Typeface.BOLD);
  t.setBackground(keyDrawable(0x00000000,keyPressed));t.setPadding(dp(14),0,dp(14),0);
  bar.addView(t,new LinearLayout.LayoutParams(-2,-1));t.setOnClickListener(v->commit(word));
 }
 private void refresh(){handler.removeCallbacks(predictTask);if(preedit==null||decoder==null)return;epoch++;String raw=buffer.toString();preedit.setText(raw);preedit.setVisibility(raw.isEmpty()?View.GONE:View.VISIBLE);candidates=decoder.decode(raw);if(dict!=null&&getSharedPreferences("config",MODE_PRIVATE).getBoolean("learn",true))candidates=dict.preferred(raw,candidates);bar.removeAllViews();if(raw.isEmpty()){
  List<String> local=localSuggestions();
  if(!local.isEmpty())showPredictions(local);
  else bar.addView(label(english?"英文模式 · 點左下角「英」切回嘸蝦米":beforeCursor().isEmpty()?"輸入字碼開始打字":"暫無接續詞，請繼續輸入字碼"),new LinearLayout.LayoutParams(-2,-1));
  return;
 }
 QuickPhraseBook.Entry shortcut=secure||english?null:quickPhrases.find(raw);
 if(shortcut!=null)quickPhraseCandidate(shortcut);
 if(candidates.isEmpty()&&shortcut==null)bar.addView(label("未匹配字碼；空白鍵輸出英文"),new LinearLayout.LayoutParams(-2,-1));
 for(int i=0;i<candidates.size();i++)candidate(candidates.get(i),i==0,raw);
 if(shortcut==null&&InputRules.useAi(composedText()))schedulePredict();
 }
 private void loadQuickPhrases(){
  try{quickPhrases=new QuickPhraseStore(this).load();}
  catch(IllegalArgumentException e){quickPhrases=new QuickPhraseBook();Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
 }
 private void quickPhraseCandidate(QuickPhraseBook.Entry entry){
  TextView item=label("📌 "+entry.name);item.setTextSize(19);item.setTextColor(accent);
  item.setBackground(keyDrawable(0x00000000,keyPressed));item.setContentDescription("常用字串："+entry.name+"，點選插入完整內容");
  bar.addView(item,new LinearLayout.LayoutParams(-2,-1));
  item.setOnClickListener(v->{
   cancelVoice();handler.removeCallbacks(predictTask);epoch++;
   InputConnection ic=getCurrentInputConnection();if(ic==null)return;
   ic.commitText(entry.text,1);buffer.setLength(0);lastWord="";
   // Literal snippets do not enter learned picks/habits or trigger an AI request.
   refresh();
  });
 }
 private void ai(){
  cancelVoice();
  if(secure){Toast.makeText(this,"密碼欄位不使用 AI",Toast.LENGTH_SHORT).show();return;}
  requestAi(true);
 }
 private void toggleVoice(){
  if(secure){Toast.makeText(this,"此欄位不使用語音輸入",Toast.LENGTH_SHORT).show();return;}
  if(voice==null)return;
  if(voice.active()){
   if(voice.processing())Toast.makeText(this,"正在轉成文字，請稍候",Toast.LENGTH_SHORT).show();
   else voice.stop();
   return;
  }
  if(buffer.length()>0){Toast.makeText(this,"請先完成目前選字，再點 🎤 語音輸入",Toast.LENGTH_SHORT).show();return;}
  if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   try{startActivity(new android.content.Intent(this,VoicePermissionActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));}
   catch(RuntimeException e){Toast.makeText(this,"請開啟 Boshi 設定允許麥克風權限",Toast.LENGTH_LONG).show();}
   return;
  }
  voiceTarget=getCurrentInputConnection();if(voiceTarget==null)return;
  handler.removeCallbacks(predictTask);epoch++;voice.start();
 }
 private void cancelVoice(){voiceTarget=null;if(voice!=null)voice.cancel();}
 @Override public void onUpdateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd){
  super.onUpdateSelection(oldStart,oldEnd,newStart,newEnd,candidatesStart,candidatesEnd);
  if(voice!=null&&voice.active()&&(oldStart!=newStart||oldEnd!=newEnd))cancelVoice();
 }
 @Override public void onFinishInputView(boolean finishingInput){cancelVoice();handler.removeCallbacks(predictTask);epoch++;super.onFinishInputView(finishingInput);}
 @Override public void onDestroy(){cancelVoice();handler.removeCallbacks(predictTask);super.onDestroy();}
 @Override public void onFinishInput(){
  cancelVoice();
  handler.removeCallbacks(predictTask);epoch++;buffer.setLength(0);
  if(repeat!=null){handler.removeCallbacks(repeat);repeat=null;}
  super.onFinishInput();
 }
}

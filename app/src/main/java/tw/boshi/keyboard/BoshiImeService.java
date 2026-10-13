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
 private LinearLayout root,bar,keys;private TextView preedit;private final StringBuilder buffer=new StringBuilder();private CodeDecoder decoder;private List<String> candidates=new ArrayList<>();private int epoch=0;private boolean secure=false,symbols=false;
 private final Handler handler=new Handler(android.os.Looper.getMainLooper());private Runnable repeat;
 private int keyBg,keyFn,keyPressed,keyText,barText,accent;private int keyHeight;

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
   if(r==2)addKey(line,symbols?"，":"AI ✨",symbols?()->symbol("，"):this::ai,1.5f,true);
   for(char ch:rows[r].toCharArray()){String s=String.valueOf(ch);if(symbols)addKey(line,s,()->symbol(s),1,false);else addKey(line,s.toUpperCase(Locale.ROOT),()->type(ch),1,false);}
   if(r==1&&!symbols)spacer(line,0.5f);
   if(r==2){TextView del=addKey(line,"⌫",this::back,1.5f,true);repeatOnHold(del);}
  }
  LinearLayout bottom=row();
  addKey(bottom,symbols?"ABC":"?123",()->{symbols=!symbols;buildKeys();},1.5f,true);
  addKey(bottom,symbols?"、":"，",()->symbol(symbols?"、":"，"),1,true);
  addKey(bottom,"嘸蝦米",this::space,4.5f,false);
  addKey(bottom,"。",()->symbol("。"),1,true);
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

 @Override public void onStartInput(EditorInfo info,boolean restarting){super.onStartInput(info,restarting);int variation=info.inputType&android.text.InputType.TYPE_MASK_VARIATION;secure=variation==android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;buffer.setLength(0);epoch++;refresh();}
 private void type(char c){if(buffer.length()<48)buffer.append(c);refresh();}
 private void back(){if(buffer.length()>0){buffer.deleteCharAt(buffer.length()-1);refresh();}else if(getCurrentInputConnection()!=null)getCurrentInputConnection().deleteSurroundingText(1,0);}
 private void commit(String s){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);buffer.setLength(0);epoch++;refresh();}
 private void symbol(String s){if(buffer.length()>0)commit((candidates.isEmpty()?buffer.toString():candidates.get(0))+s);else commit(s);}
 private void space(){if(buffer.length()>0)commit(candidates.isEmpty()?buffer.toString():candidates.get(0));else commit(" ");}
 private void enter(){if(buffer.length()>0)space();InputConnection ic=getCurrentInputConnection();if(ic!=null){ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ENTER));}}

 private void candidate(String word,boolean first){
  TextView t=label(word);t.setTextSize(20);t.setTextColor(first?accent:keyText);if(first)t.setTypeface(null,Typeface.BOLD);
  t.setBackground(keyDrawable(0x00000000,keyPressed));t.setPadding(dp(14),0,dp(14),0);
  bar.addView(t,new LinearLayout.LayoutParams(-2,-1));t.setOnClickListener(v->commit(word));
 }
 private void refresh(){if(preedit==null||decoder==null)return;epoch++;String raw=buffer.toString();preedit.setText(raw);preedit.setVisibility(raw.isEmpty()?View.GONE:View.VISIBLE);candidates=decoder.decode(raw);bar.removeAllViews();if(raw.isEmpty()){bar.addView(label("Boshi 嘸蝦米 · 空白鍵送出第一候選"),new LinearLayout.LayoutParams(-2,-1));return;}
 if(candidates.isEmpty())bar.addView(label("未匹配字碼；空白鍵輸出英文"),new LinearLayout.LayoutParams(-2,-1));
 for(int i=0;i<candidates.size();i++)candidate(candidates.get(i),i==0);
 }
 private void ai(){if(secure){Toast.makeText(this,"密碼欄位不使用 AI",Toast.LENGTH_SHORT).show();return;}if(buffer.length()==0)return;SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);int provider=p.getInt("provider",0);String key=provider<1?"":new SecureKeyStore(this).get(SecureKeyStore.PROVIDERS[provider-1]),model=SettingsActivity.model(p,provider);if(key.isEmpty()){Toast.makeText(this,"請先開啟 Boshi 設定並填寫 AI Key",Toast.LENGTH_SHORT).show();return;}
 String text=candidates.isEmpty()?buffer.toString():candidates.get(0);int requestEpoch=epoch;bar.removeAllViews();bar.addView(label("AI 生成中…"),new LinearLayout.LayoutParams(-2,-1));
 new Thread(()->{try{List<String> list=AiClient.query(provider,key,model,text);handler.post(()->{if(requestEpoch!=epoch||bar==null)return;bar.removeAllViews();for(int i=0;i<list.size();i++)candidate(list.get(i),i==0);if(list.isEmpty())bar.addView(label("無有效候選"),new LinearLayout.LayoutParams(-2,-1));});}
 catch(Exception e){handler.post(()->{if(requestEpoch==epoch){refresh();Toast.makeText(this,"AI 失敗："+e.getMessage(),Toast.LENGTH_SHORT).show();}});}}).start();
 }
}

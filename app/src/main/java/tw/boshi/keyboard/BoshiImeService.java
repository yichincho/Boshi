package tw.boshi.keyboard;
import android.inputmethodservice.InputMethodService;import android.view.*;import android.view.inputmethod.*;import android.widget.*;import android.content.*;import java.util.*;
public class BoshiImeService extends InputMethodService {
 private LinearLayout root,bar,keys;private TextView preedit;private final StringBuilder buffer=new StringBuilder();private CodeDecoder decoder;private List<String> candidates=new ArrayList<>();private int epoch=0;private boolean secure=false;
 private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(17);t.setPadding(7,12,7,12);t.setGravity(17);return t;}
 @Override public View onCreateInputView(){decoder=new CodeDecoder();try{decoder.load(this);}catch(Exception ignored){}root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xffeeeeee);
 preedit=label("");root.addView(preedit);HorizontalScrollView scroll=new HorizontalScrollView(this);bar=new LinearLayout(this);bar.setOrientation(0);scroll.addView(bar);root.addView(scroll);
 keys=new LinearLayout(this);keys.setOrientation(1);root.addView(keys);
 for(String row:new String[]{"qwertyuiop","asdfghjkl","zxcvbnm"}){LinearLayout line=new LinearLayout(this);line.setOrientation(0);keys.addView(line);for(char ch:row.toCharArray())addButton(line,String.valueOf(ch),()->type(ch),1);}
 LinearLayout actions=new LinearLayout(this);keys.addView(actions);addButton(actions,"⌫",()->back(),1);addButton(actions,"空格",()->space(),2);addButton(actions,"AI ✨",()->ai(),1);addButton(actions,"↵",()->enter(),1);
 refresh();return root;}
 private void addButton(LinearLayout row,String s,Runnable action,int weight){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setPadding(0,0,0,0);row.addView(b,new LinearLayout.LayoutParams(0,50*(int)getResources().getDisplayMetrics().density,weight));b.setOnClickListener(v->action.run());}
 @Override public void onStartInput(EditorInfo info,boolean restarting){super.onStartInput(info,restarting);int variation=info.inputType&android.text.InputType.TYPE_MASK_VARIATION;secure=variation==android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD||variation==android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD;buffer.setLength(0);epoch++;refresh();}
 private void type(char c){if(buffer.length()<48)buffer.append(c);refresh();}
 private void back(){if(buffer.length()>0){buffer.deleteCharAt(buffer.length()-1);refresh();}else if(getCurrentInputConnection()!=null)getCurrentInputConnection().deleteSurroundingText(1,0);}
 private void commit(String s){InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);buffer.setLength(0);epoch++;refresh();}
 private void space(){if(buffer.length()>0)commit(candidates.isEmpty()?buffer.toString():candidates.get(0));else commit(" ");}
 private void enter(){if(buffer.length()>0)space();InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.sendKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_ENTER));}
 private void refresh(){if(preedit==null||decoder==null)return;epoch++;String raw=buffer.toString();preedit.setText(raw.isEmpty()?"Boshi · 連續組句":raw);candidates=decoder.decode(raw);bar.removeAllViews();if(raw.isEmpty()){bar.addView(label("選字後按空格送出 · AI 需手動啟動"));return;}
 if(candidates.isEmpty())bar.addView(label("未匹配字碼；可按空格輸出英文"));
 for(String word:new ArrayList<>(candidates)){Button b=new Button(this);b.setAllCaps(false);b.setText(word);bar.addView(b);b.setOnClickListener(v->commit(word));}
 }
 private void ai(){if(secure||buffer.length()==0)return;android.content.SharedPreferences p=getSharedPreferences("config",MODE_PRIVATE);int provider=p.getInt("provider",0);String key=p.getString("key",""),model=p.getString("model","");if(provider==0||key.isEmpty()){Toast.makeText(this,"請先開啟 Boshi 設定並填寫 AI Key",Toast.LENGTH_SHORT).show();return;}
 String text=candidates.isEmpty()?buffer.toString():candidates.get(0);int requestEpoch=epoch;bar.removeAllViews();bar.addView(label("AI 生成中…"));
 new Thread(()->{try{List<String> list=AiClient.query(provider,key,model,text);new android.os.Handler(getMainLooper()).post(()->{if(requestEpoch!=epoch||bar==null)return;bar.removeAllViews();for(String sentence:list){Button b=new Button(this);b.setText(sentence);b.setAllCaps(false);bar.addView(b);b.setOnClickListener(v->commit(sentence));}if(list.isEmpty())bar.addView(label("無有效候選"));});}
 catch(Exception e){new android.os.Handler(getMainLooper()).post(()->{if(requestEpoch==epoch){refresh();Toast.makeText(this,"AI 失敗："+e.getMessage(),Toast.LENGTH_SHORT).show();}});}}).start();
 }
}

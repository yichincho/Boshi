package tw.boshi.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.*;

/** Add, edit and delete literal multi-line snippets. */
public class QuickPhraseActivity extends Activity {
 private QuickPhraseStore store;
 private QuickPhraseBook book;
 private LinearLayout root;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);store=new QuickPhraseStore(this);
  try{book=store.load();}catch(IllegalArgumentException e){toast(e.getMessage());finish();return;}
  render();
 }
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setPadding(0,dp(6),0,dp(6));return v;}
 private Button button(String value){Button b=new Button(this);b.setText(value);return b;}
 private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
 private void render(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(16),dp(20),dp(24));
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
  root.addView(text("常用字串／快捷片語",25));
  root.addView(text("在中文鍵盤輸入快捷字碼，點選候選欄的 📌 名稱，即可插入完整內容。同一字碼可儲存多筆內容，支援標點與換行，不使用 AI 改寫。空白鍵仍使用一般嘸蝦米候選。",15));
  Button add=button("＋ 新增常用字串");root.addView(add);add.setOnClickListener(v->edit(null));
  root.addView(text("已儲存 "+book.entries().size()+" 組",15));
  if(book.entries().isEmpty())root.addView(text("尚無常用字串。可新增地址、Email 或常用回覆。",17));
  for(QuickPhraseBook.Entry entry:book.entries()){
   root.addView(text(entry.code+"  ·  "+entry.name,19));
   TextView preview=text(entry.text,15);preview.setMaxLines(3);preview.setEllipsize(TextUtils.TruncateAt.END);root.addView(preview);
   LinearLayout actions=new LinearLayout(this);Button edit=button("編輯"),delete=button("刪除");
   actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1));actions.addView(delete,new LinearLayout.LayoutParams(0,-2,1));root.addView(actions);
   edit.setOnClickListener(v->edit(entry));
   delete.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("刪除「"+entry.name+"」？").setMessage(entry.preview(80)+"\n\n只刪除這一筆，同字碼其他片語會保留。").setNegativeButton("取消",null).setPositiveButton("刪除",(dialog,which)->{
    QuickPhraseBook updated=QuickPhraseBook.decode(book.encode());updated.delete(entry.id);
    try{store.save(updated);book=updated;render();}catch(IllegalStateException e){toast(e.getMessage());}
   }).show());
  }
 }
 private EditText field(LinearLayout form,String label,int limit,boolean multiline,String value){
  form.addView(text(label,15));EditText input=new EditText(this);
  input.setInputType(multiline?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
  input.setSingleLine(!multiline);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
  if(multiline){input.setMinLines(5);input.setMaxLines(10);input.setGravity(Gravity.TOP);}
  input.setText(value);form.addView(input,new LinearLayout.LayoutParams(-1,-2));return input;
 }
 private void edit(QuickPhraseBook.Entry entry){
  LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(18),0,dp(18),dp(12));
  EditText code=field(form,"快捷字碼（1～16 個英文字母，以英文模式輸入）",16,false,entry==null?"":entry.code);
  EditText name=field(form,"候選欄顯示名稱（例如：地址、回覆訊息）",60,false,entry==null?"":entry.name);
  EditText content=field(form,"完整內容（可輸入整段文字與換行，最多 10,000 字元）",QuickPhraseBook.MAX_TEXT,true,entry==null?"":entry.text);
  ScrollView scroll=new ScrollView(this);scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(entry==null?"新增常用字串":"編輯常用字串").setView(scroll).setNegativeButton("取消",null).setPositiveButton("儲存",null).create();
  dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{
    QuickPhraseBook updated=QuickPhraseBook.decode(book.encode());
    updated.save(entry==null?null:entry.id,code.getText().toString(),name.getText().toString(),content.getText().toString());
    store.save(updated);book=updated;dialog.dismiss();render();toast("已儲存，回到輸入框即可使用");
   }catch(IllegalArgumentException|IllegalStateException e){toast(e.getMessage());}
  }));
  dialog.show();
 }
}

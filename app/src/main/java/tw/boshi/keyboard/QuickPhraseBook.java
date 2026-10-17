package tw.boshi.keyboard;

import java.io.*;
import java.util.*;

/** Exact shortcut-to-literal-text mappings, independent of the decoder and AI. */
final class QuickPhraseBook {
 static final int MAX_TEXT=10000,MAX_ENTRIES=200;
 static final class Entry {
  final String code,name,text;
  Entry(String code,String name,String text){this.code=code;this.name=name;this.text=text;}
 }
 private final Map<String,Entry> entries=new LinkedHashMap<>();
 private static String normalize(String code){return code==null?"":code.trim().toLowerCase(Locale.ROOT);}
 Entry find(String code){return entries.get(normalize(code));}
 List<Entry> entries(){return new ArrayList<>(entries.values());}
 void save(String previousCode,String code,String name,String text){
  code=normalize(code);name=name==null?"":name.trim();
  if(!code.matches("[a-z]{1,16}"))throw new IllegalArgumentException("快捷字碼請使用 1～16 個英文字母");
  if(name.length()>60)throw new IllegalArgumentException("名稱最多 60 個字元");
  if(text==null||text.trim().isEmpty())throw new IllegalArgumentException("請輸入完整字串內容");
  if(text.length()>MAX_TEXT)throw new IllegalArgumentException("內容最多 10,000 個字元");
  String previous=previousCode==null?null:normalize(previousCode);
  if(previous!=null&&!entries.containsKey(previous))throw new IllegalArgumentException("原字串已不存在，請重新開啟編輯");
  if(entries.containsKey(code)&&!code.equals(previous))throw new IllegalArgumentException("這個快捷字碼已使用，請換另一個");
  if(previous==null&&entries.size()>=MAX_ENTRIES)throw new IllegalArgumentException("最多可儲存 200 組常用字串");
  if(previous!=null&&!previous.equals(code))entries.remove(previous);
  entries.put(code,new Entry(code,name.isEmpty()?code:name,text));
 }
 void delete(String code){entries.remove(normalize(code));}
 String encode(){
  try{
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
   out.writeInt(1);out.writeInt(entries.size());
   for(Entry entry:entries.values()){out.writeUTF(entry.code);out.writeUTF(entry.name);out.writeUTF(entry.text);}
   out.close();return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }catch(IOException e){throw new IllegalArgumentException("常用字串無法編碼",e);}
 }
 static QuickPhraseBook decode(String encoded){
  QuickPhraseBook book=new QuickPhraseBook();if(encoded==null||encoded.isEmpty())return book;
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))){
   if(in.readInt()!=1)throw new IOException("Unknown dictionary version");
   int count=in.readInt();if(count<0||count>MAX_ENTRIES)throw new IOException("Invalid dictionary size");
   for(int i=0;i<count;i++)book.save(null,in.readUTF(),in.readUTF(),in.readUTF());
   if(in.available()!=0)throw new IOException("Unexpected dictionary data");
   return book;
  }catch(IOException|IllegalArgumentException e){throw new IllegalArgumentException("常用字串資料讀取失敗，請保留資料並重新開啟",e);}
 }
}

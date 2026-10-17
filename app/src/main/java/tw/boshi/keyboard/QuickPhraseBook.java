package tw.boshi.keyboard;

import java.io.*;
import java.util.*;

/** Multiple literal phrases per code; edits and deletions identify a single stable entry ID. */
final class QuickPhraseBook {
 static final int MAX_TEXT=10000,MAX_ENTRIES=200;
 static final class Entry {
  final String id,code,name,text;
  Entry(String id,String code,String name,String text){this.id=id;this.code=code;this.name=name;this.text=text;}
  String preview(int limit){
   String compact=text.replaceAll("\\s+"," ").trim();
   if(limit<=0)return "";
   return compact.codePointCount(0,compact.length())<=limit?compact:compact.substring(0,compact.offsetByCodePoints(0,limit))+"…";
  }
 }
 private final Map<String,Entry> entries=new LinkedHashMap<>();
 private static String normalize(String code){return code==null?"":code.trim().toLowerCase(Locale.ROOT);}
 Entry get(String id){return entries.get(id);}
 List<Entry> matches(String code){
  String normalized=normalize(code);List<Entry> matches=new ArrayList<>();
  for(Entry entry:entries.values())if(entry.code.equals(normalized))matches.add(entry);
  return matches;
 }
 List<Entry> entries(){return new ArrayList<>(entries.values());}
 private static Entry validated(String id,String code,String name,String text){
  code=normalize(code);name=name==null?"":name.trim();
  if(id==null||id.isEmpty()||id.length()>128)throw new IllegalArgumentException("片語識別資料無效");
  if(!code.matches("[a-z]{1,16}"))throw new IllegalArgumentException("快捷字碼請使用 1～16 個英文字母");
  if(name.length()>60)throw new IllegalArgumentException("名稱最多 60 個字元");
  if(text==null||text.trim().isEmpty())throw new IllegalArgumentException("請輸入完整字串內容");
  if(text.length()>MAX_TEXT)throw new IllegalArgumentException("內容最多 10,000 個字元");
  return new Entry(id,code,name.isEmpty()?code:name,text);
 }
 Entry save(String id,String code,String name,String text){
  if(id!=null&&!entries.containsKey(id))throw new IllegalArgumentException("原字串已不存在，請重新開啟編輯");
  if(id==null&&entries.size()>=MAX_ENTRIES)throw new IllegalArgumentException("最多可儲存 200 組常用字串");
  Entry entry=validated(id==null?UUID.randomUUID().toString():id,code,name,text);
  entries.put(entry.id,entry);return entry;
 }
 void delete(String id){entries.remove(id);}
 String encode(){
  try{
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
   out.writeInt(2);out.writeInt(entries.size());
   for(Entry entry:entries.values()){out.writeUTF(entry.id);out.writeUTF(entry.code);out.writeUTF(entry.name);out.writeUTF(entry.text);}
   out.close();return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }catch(IOException e){throw new IllegalArgumentException("常用字串無法編碼",e);}
 }
 static QuickPhraseBook decode(String encoded){
  QuickPhraseBook book=new QuickPhraseBook();if(encoded==null||encoded.isEmpty())return book;
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))){
   int version=in.readInt();if(version!=1&&version!=2)throw new IOException("Unknown dictionary version");
   int count=in.readInt();if(count<0||count>MAX_ENTRIES)throw new IOException("Invalid dictionary size");
   for(int i=0;i<count;i++){
    String id=version==2?in.readUTF():UUID.randomUUID().toString();
    Entry entry=validated(id,in.readUTF(),in.readUTF(),in.readUTF());
    if(book.entries.containsKey(id))throw new IOException("Duplicate entry ID");
    book.entries.put(id,entry);
   }
   if(in.available()!=0)throw new IOException("Unexpected dictionary data");
   return book;
  }catch(IOException|IllegalArgumentException e){throw new IllegalArgumentException("常用字串資料讀取失敗，請保留資料並重新開啟",e);}
 }
}

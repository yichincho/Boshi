package tw.boshi.keyboard;
import android.content.Context;
import java.io.*;
import java.util.*;
public final class DictionaryImporter {
 private DictionaryImporter(){}
 public static int importFile(Context context, InputStream source, String name) throws IOException {
  boolean yaml=name.toLowerCase(Locale.ROOT).endsWith(".yaml")||name.toLowerCase(Locale.ROOT).endsWith(".yml");
  File temp=new File(context.getFilesDir(),"codes-import.tmp");
  File dest=new File(context.getFilesDir(),"codes.tsv");
  LinkedHashSet<String> entries=new LinkedHashSet<>();
  boolean body=!yaml; int count=0;
  try(BufferedReader reader=new BufferedReader(new InputStreamReader(source,"UTF-8"))){
   String line;
   while((line=reader.readLine())!=null){
    line=line.replace("\uFEFF","");
    if(yaml&&!body){if(line.trim().equals("..."))body=true;continue;}
    if(line.startsWith("#")||line.trim().isEmpty())continue;
    String[] parts=line.split("\t");if(parts.length<2)continue;
    String code=yaml?parts[1].trim():parts[0].trim();
    String word=yaml?parts[0].trim():parts[1].trim();
    // Basic keyboard currently supports Latin-letter codes only. Skip control/variant syntax.
    if(!code.matches("[a-zA-Z]{1,8}")||word.isEmpty())continue;
    entries.add(code.toLowerCase(Locale.ROOT)+"\t"+word);
    if(entries.size()>250000)throw new IOException("碼表超過 250000 筆上限");
   }
  }
  if(entries.isEmpty())throw new IOException("找不到可用的字碼資料。請確認格式和欄位順序");
  // Atomic replacement: keep the previous valid dictionary if parsing/writing fails.
  try(BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),"UTF-8"))){
   for(String entry:entries){out.write(entry);out.newLine();count++;}
  }
  File old=new File(context.getFilesDir(),"codes-old.tsv");
  if(old.exists())old.delete();
  if(dest.exists()&&!dest.renameTo(old))throw new IOException("無法備份舊碼表");
  if(!temp.renameTo(dest)){if(old.exists())old.renameTo(dest);throw new IOException("無法啟用新碼表");}
  if(old.exists())old.delete();
  return count;
 }
}

package tw.boshi.keyboard;

import java.io.*;
import java.util.*;

/** Bundled 同音字 lookup: same-tone characters first, then the same syllable in other tones. */
final class Homophones {
 static final class Entry {
  final String text,code;
  Entry(String text,String code){this.text=text;this.code=code;}
 }
 private final Map<String,List<Entry>> byReading=new HashMap<>();
 private final Map<String,List<String>> readings=new HashMap<>();
 private final Map<String,List<String>> tones=new HashMap<>();

 /** Lines: reading TAB character TAB Boshiamy code, already ordered by frequency. */
 void load(Reader source)throws IOException {
  try(BufferedReader reader=new BufferedReader(source)) {
   String line;
   while((line=reader.readLine())!=null) {
    String[] f=line.split("\t");
    if(f.length!=3||f[0].length()<2||f[1].isEmpty()||f[1].codePointCount(0,f[1].length())!=1)continue;
    String reading=f[0];
    List<Entry> list=byReading.get(reading);
    if(list==null){
     list=new ArrayList<>();byReading.put(reading,list);
     tones.computeIfAbsent(syllable(reading),k->new ArrayList<>()).add(reading);
    }
    list.add(new Entry(f[1],f[2]));
    List<String> r=readings.computeIfAbsent(f[1],k->new ArrayList<>());
    if(!r.contains(reading))r.add(reading);
   }
  }
 }

 int size(){return readings.size();}

 /** Readings of one character, e.g. 行 → [hang2, xing2]. */
 List<String> readingsOf(String ch){List<String> r=readings.get(ch);return r==null?Collections.emptyList():r;}

 /** Characters sounding like {@code ch}, excluding itself; exact tone matches come first. */
 List<Entry> of(String ch,int limit) {
  LinkedHashMap<String,Entry> out=new LinkedHashMap<>();
  List<String> own=readingsOf(ch);
  for(String r:own)for(Entry e:byReading.get(r))if(!e.text.equals(ch))out.putIfAbsent(e.text,e);
  for(String r:own)for(String other:tones.get(syllable(r))) {
   if(own.contains(other))continue;
   for(Entry e:byReading.get(other))if(!e.text.equals(ch))out.putIfAbsent(e.text,e);
  }
  List<Entry> list=new ArrayList<>(out.values());
  return list.subList(0,Math.min(limit,list.size()));
 }

 /** The last character of a candidate, so 你好 can become 你號. */
 static String lastChar(String word) {
  if(word==null||word.isEmpty())return "";
  return word.substring(word.offsetByCodePoints(word.length(),-1));
 }

 private static String syllable(String reading){
  char last=reading.charAt(reading.length()-1);
  return last>='1'&&last<='5'?reading.substring(0,reading.length()-1):reading;
 }
}

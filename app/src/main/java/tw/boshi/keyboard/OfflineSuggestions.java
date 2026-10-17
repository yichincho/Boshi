package tw.boshi.keyboard;

import java.io.*;
import java.util.*;

/** Bundled, frequency-ordered phrase continuations. No learning history or network required. */
final class OfflineSuggestions {
 private static final int PER_CONTEXT=8;
 private final Map<String,List<String>> followers=new HashMap<>();

 void load(Reader source)throws IOException {
  try(BufferedReader reader=new BufferedReader(source)) {
   String phrase;
   while((phrase=reader.readLine())!=null) {
    phrase=phrase.trim();
    if(phrase.isEmpty()||phrase.startsWith("#"))continue;
    int count=phrase.codePointCount(0,phrase.length());
    if(count<2||count>12||!phrase.codePoints().allMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN))continue;
    for(int n=1;n<=Math.min(4,count-1);n++) {
     int cut=phrase.offsetByCodePoints(0,n);
     String head=phrase.substring(0,cut),tail=phrase.substring(cut);
     List<String> list=followers.computeIfAbsent(head,k->new ArrayList<>());
     if(list.size()<PER_CONTEXT&&!list.contains(tail))list.add(tail);
    }
   }
  }
 }

 List<String> suggest(String context,int limit) {
  if(context==null||context.isEmpty()||limit<=0)return new ArrayList<>();
  LinkedHashSet<String> out=new LinkedHashSet<>();
  int count=context.codePointCount(0,context.length());
  for(int n=Math.min(4,count);n>=1;n--) {
   String suffix=context.substring(context.offsetByCodePoints(context.length(),-n));
   List<String> list=followers.get(suffix);
   if(list!=null)out.addAll(list);
   if(out.size()>=limit)break;
  }
  return new ArrayList<>(out).subList(0,Math.min(limit,out.size()));
 }

 static List<String> combine(List<String> learned,List<String> builtIn,int limit) {
  if(limit<=0)return new ArrayList<>();
  LinkedHashSet<String> out=new LinkedHashSet<>(learned);out.addAll(builtIn);
  return new ArrayList<>(out).subList(0,Math.min(limit,out.size()));
 }
}

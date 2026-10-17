package tw.boshi.keyboard;

import java.io.*;
import java.util.*;

public class OfflineSuggestionsTest {
 public static void main(String[] args)throws Exception {
  OfflineSuggestions words=new OfflineSuggestions();
  words.load(new StringReader("有沒有\n有時間\n有空\n有需要\n有問題\n我是有一些\n我是有一點\n有時間\n# comment\n"));
  check(words.suggest("有",8).equals(Arrays.asList("沒有","時間","空","需要","問題")),"fresh install has offline followers for 有");
  List<String> after=words.suggest("我是有",8);
  check(after.get(0).equals("一些")&&after.contains("沒有"),"longest matching context first, then single-character fallback");
  check(!after.contains("有沒有"),"candidate contains continuation only, no duplicate prefix");
  check(words.suggest("有",2).size()==2,"candidate limit honored");
  check(words.suggest("",8).isEmpty(),"empty field has no irrelevant predictions");
  check(words.suggest("陌生字",8).isEmpty(),"unmatched context returns empty");
  check(words.suggest("有",0).isEmpty(),"zero limit is safe");
  check(OfflineSuggestions.combine(Arrays.asList("時間","習慣"),words.suggest("有",8),8).equals(Arrays.asList("時間","習慣","沒有","空","需要","問題")),"personal habits precede built-ins without duplicates");
  check(OfflineSuggestions.combine(Collections.emptyList(),words.suggest("有",8),8).size()==5,"learning disabled or no history still offers built-ins");
  OfflineSuggestions packaged=new OfflineSuggestions();
  try(Reader r=new InputStreamReader(new FileInputStream(args[0]),"UTF-8")){packaged.load(r);}
  check(packaged.suggest("有",8).contains("時間"),"packaged asset supplies real 有 continuation");
  check(!packaged.suggest("我是有",8).isEmpty(),"screenshot context has candidates without API or history");
  check(!packaged.suggest("你好",8).isEmpty(),"another common context has built-in candidates");
  System.out.println("PASS: 12 offline suggestion checks");
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}

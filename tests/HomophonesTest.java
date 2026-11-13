package tw.boshi.keyboard;

import java.io.*;
import java.util.*;

public class HomophonesTest {
 public static void main(String[] args)throws Exception {
  Homophones h=new Homophones();
  h.load(new StringReader("hao3\t好\tgz\nhao3\t郝\tx\nhao4\t號\tq\nhao4\t好\tgz\nhang2\t行\tmv\nxing2\t行\tmv\nxing2\t型\ty\nxing4\t幸\tz\nbad\n"));
  List<Homophones.Entry> hao=h.of("好",10);
  check(texts(hao).equals(Arrays.asList("郝","號")),"own readings first, itself excluded: "+texts(hao));
  check("x".equals(hao.get(0).code),"code is kept for each homophone");
  check(texts(h.of("行",10)).equals(Arrays.asList("型","幸")),"polyphones use every reading, then other tones");
  check(h.of("行",1).size()==1,"limit is respected");
  check(h.of("無",10).isEmpty(),"unknown character has no homophones");
  check(h.readingsOf("行").equals(Arrays.asList("hang2","xing2")),"readings in file order");
  check("好".equals(Homophones.lastChar("你好")),"last character of phrase");
  check("𠀀".equals(Homophones.lastChar("你𠀀")),"supplementary last character");
  check("".equals(Homophones.lastChar("")),"empty word");
  if(args.length>0) {
   Homophones real=new Homophones();
   real.load(new InputStreamReader(new FileInputStream(args[0]),"UTF-8"));
   check(real.size()>8000,"bundled table has many characters: "+real.size());
   check(!real.of("是",40).isEmpty()&&texts(real.of("是",40)).contains("事"),"是 sounds like 事");
  }
  System.out.println("PASS: homophone checks");
 }
 private static List<String> texts(List<Homophones.Entry> list){List<String> out=new ArrayList<>();for(Homophones.Entry e:list)out.add(e.text);return out;}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}

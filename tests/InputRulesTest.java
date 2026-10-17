package tw.boshi.keyboard;

public class InputRulesTest {
 public static void main(String[] args) {
  check("？".equals(InputRules.period("s",false)),"S + period is a question mark");
  check(".".equals(InputRules.period("",false)),"standalone period stays ASCII");
  check(".".equals(InputRules.period("s",true)),"English s + period is unchanged");
  check(".".equals(InputRules.period("sa",false)),"other codes use normal period");
  check(!InputRules.useAi("今天好熱"),"four Han characters stay local");
  check(!InputRules.useAi("今天真的熱"),"five Han characters stay local");
  check(InputRules.useAi("今天真的很熱"),"six Han characters enable AI");
  check(!InputRules.useAi("今天真的熱 ABC 123！"),"letters and punctuation do not count");
  check(!InputRules.useAi(InputRules.sentence("今天真的很熱。你好")),"previous sentence does not trigger AI");
  check(!InputRules.useAi(InputRules.sentence("今天真的很熱\n你好")),"newline resets context");
  check("你好".equals(InputRules.sentence("今天真的很熱？你好")),"question mark resets context");
  check(!InputRules.useAi(null),"missing context stays local");
  check(!InputRules.useAi("𠀀𠀀𠀀𠀀𠀀"),"supplementary Han count once each");
  check(InputRules.useAi("𠀀𠀀𠀀𠀀𠀀𠀀"),"six supplementary Han enable AI");
  System.out.println("PASS: 14 input-rule checks");
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}

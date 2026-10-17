package tw.boshi.keyboard;
import java.util.*;
public class QuickPhraseBookTest {
 private static int checks;
 public static void main(String[] args){
  QuickPhraseBook book=new QuickPhraseBook();
  String text="  已收到您的訊息。\n第二行：123，謝謝！🙂\n";
  book.save(null,"HF","回覆訊息",text);
  check(book.find("hf").text.equals(text),"paragraph keeps whitespace, punctuation, digits and newlines exactly");
  check(book.find("HF").name.equals("回覆訊息"),"shortcut lookup is case insensitive");
  check(book.find("h")==null,"shortcut only matches exact code");
  check(book.find("missing")==null,"unknown code leaves normal dictionary behavior intact");
  fails(()->book.save(null,"hf","duplicate","文字"),"duplicate code is rejected without replacing existing text");
  check(book.find("hf").text.equals(text),"failed duplicate save keeps original");
  QuickPhraseBook restored=QuickPhraseBook.decode(book.encode());
  check(restored.find("hf").text.equals(text),"storage round trip preserves the complete paragraph and emoji");
  restored.save("hf","mail","Email","hello@example.com");
  check(restored.find("hf")==null&&restored.find("mail").text.equals("hello@example.com"),"editing code removes old match");
  restored.save(null,"dz","地址","臺中市測試路一號");
  fails(()->restored.save("dz","mail","衝突","新內容"),"editing into another code is rejected");
  check(restored.find("dz")!=null,"failed edit preserves old shortcut");
  restored.delete("mail");check(restored.find("mail")==null&&restored.entries().size()==1,"delete removes only selected entry");
  fails(()->book.save(null,"a1","","內容"),"only keyboard letter codes are accepted");
  fails(()->book.save(null,"abc",""," \n "),"empty paragraph is rejected");
  fails(()->book.save(null,"abc","","字".repeat(10001)),"oversized paragraph has a validation error");
  check(QuickPhraseBook.decode("").entries().isEmpty(),"new install starts with an empty custom dictionary");
  fails(()->QuickPhraseBook.decode("invalid"),"damaged storage is reported rather than partially loaded");
  System.out.println("PASS: "+checks+" quick phrase checks");
 }
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void fails(Runnable action,String message){boolean failed=false;try{action.run();}catch(IllegalArgumentException e){failed=true;}check(failed,message);}
}

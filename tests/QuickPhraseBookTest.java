package tw.boshi.keyboard;
import java.io.*;
import java.util.*;
public class QuickPhraseBookTest {
 private static int checks;
 public static void main(String[] args)throws Exception {
  QuickPhraseBook book=new QuickPhraseBook();
  String text="  已收到您的訊息。\n第二行：123，謝謝！🙂\n";
  QuickPhraseBook.Entry first=book.save(null,"SUB","回覆一",text);
  QuickPhraseBook.Entry second=book.save(null,"sub","回覆二","另一段內容");
  check(book.matches("sub").size()==2,"same decomposition code stores multiple phrases");
  check(!first.id.equals(second.id),"each same-code phrase has an independent ID");
  check(book.matches(" SUB ").size()==2,"case and surrounding whitespace normalize");
  check(book.matches("sub").get(0).text.equals(text),"original paragraph remains exact after adding a sibling");
  check(book.matches("sub").get(1).text.equals("另一段內容"),"both literal contents are returned in order");
  check(book.matches("su").isEmpty()&&book.matches("missing").isEmpty(),"only exact codes match");
  QuickPhraseBook restored=QuickPhraseBook.decode(book.encode());
  check(restored.matches("sub").size()==2,"round trip keeps all duplicate-code candidates");
  check(restored.get(first.id).text.equals(text),"round trip preserves newlines, whitespace and emoji");
  check(restored.get(second.id)!=null,"entry IDs survive reload for independent editing");
  restored.save(first.id,"sub","已修改","只修改第一段\n保留換行");
  check(restored.get(first.id).text.equals("只修改第一段\n保留換行"),"edit updates selected entry");
  check(restored.get(second.id).text.equals("另一段內容"),"editing one same-code entry leaves the other intact");
  restored.save(first.id,"mail","Email","hello@example.com");
  check(restored.matches("sub").size()==1&&restored.matches("mail").size()==1,"renaming moves only the selected entry");
  restored.save(second.id,"mail","另一個Email","other@example.com");
  check(restored.matches("mail").size()==2,"editing into an existing code is allowed");
  restored.delete(first.id);
  check(restored.matches("mail").size()==1&&restored.get(second.id)!=null,"delete uses ID and keeps same-code sibling");
  restored.delete(first.id);check(restored.entries().size()==1,"repeated deletion cannot remove sibling");
  fails(()->restored.save(first.id,"sub","不存在","文字"),"stale edit is rejected");
  book.save(null,"sub","回覆二","另一段內容");
  check(book.matches("sub").size()==3,"even identical code/name/text entries have independent identities");
  String preview=book.save(null,"emoji","","🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂🙂").preview(16);
  check(preview.codePointCount(0,preview.length())==17&&preview.endsWith("…"),"preview truncates on Unicode boundaries");
  check(book.matches("emoji").get(0).name.equals("emoji"),"blank name defaults to code");
  QuickPhraseBook legacy=QuickPhraseBook.decode(legacy());
  check(legacy.matches("sub").get(0).text.equals(text),"version 1 dictionaries load without losing original text");
  String legacyId=legacy.entries().get(0).id;
  legacy.save(null,"sub","新增","升級後的第二段");
  check(legacy.matches("sub").size()==2,"legacy dictionary can add same code after upgrade");
  QuickPhraseBook migrated=QuickPhraseBook.decode(legacy.encode());
  check(migrated.get(legacyId)!=null&&migrated.matches("sub").size()==2,"migrated IDs and siblings survive version 2 persistence");
  fails(()->book.save(null,"a1","","內容"),"invalid letter code rejected");
  fails(()->book.save(null,"abc",""," \n "),"empty content rejected");
  fails(()->book.save(null,"abc","","字".repeat(10001)),"oversized paragraph rejected");
  check(QuickPhraseBook.decode("").entries().isEmpty(),"new install starts empty");
  fails(()->QuickPhraseBook.decode("invalid"),"corrupt dictionary rejected");
  QuickPhraseBook full=new QuickPhraseBook();
  for(int i=0;i<QuickPhraseBook.MAX_ENTRIES;i++)full.save(null,"sub","項目"+i,"內容"+i);
  check(full.matches("sub").size()==QuickPhraseBook.MAX_ENTRIES,"capacity counts entries rather than unique codes");
  fails(()->full.save(null,"sub","額外","內容"),"entry capacity remains bounded");
  QuickPhraseBook.Entry selected=full.entries().get(0);
  full.save(selected.id,"sub","修改","更新內容");
  check(full.get(selected.id).text.equals("更新內容"),"editing remains possible at capacity");
  System.out.println("PASS: "+checks+" quick phrase checks");
 }
 private static String legacy()throws Exception {
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
  out.writeInt(1);out.writeInt(1);out.writeUTF("sub");out.writeUTF("回覆一");out.writeUTF("  已收到您的訊息。\n第二行：123，謝謝！🙂\n");out.close();
  return Base64.getEncoder().encodeToString(bytes.toByteArray());
 }
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void fails(Runnable action,String message){boolean failed=false;try{action.run();}catch(IllegalArgumentException e){failed=true;}check(failed,message);}
}

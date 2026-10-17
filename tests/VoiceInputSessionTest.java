package tw.boshi.keyboard;

public class VoiceInputSessionTest {
 public static void main(String[] args) {
  VoiceInputSession session=new VoiceInputSession();
  check(!session.active(),"starts idle");
  int first=session.start();
  check(session.listening()&&session.current(first),"start captures a live session");
  check(session.start()==-1,"cannot restart while listening");
  check(session.stop(first)&&session.processing(),"stop waits for final text");
  check(!session.stop(first),"repeated stop is ignored");
  check(session.start()==-1,"cannot restart before final result or error");
  check(session.finish(first)&&!session.active(),"final result completes the session");
  check(!session.finish(first),"duplicate final result must not insert twice");
  int second=session.start();
  check(second!=first&&!session.current(first),"previous callbacks cannot affect next dictation");
  session.cancel();
  check(!session.current(second)&&!session.finish(second),"leaving field rejects late results");
  int third=session.start();
  check(session.finish(third),"error may finish while listening");
  check(!session.active(),"error returns to idle so user can retry");
  System.out.println("PASS: 12 voice session checks");
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}

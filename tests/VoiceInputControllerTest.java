package tw.boshi.keyboard;
import android.content.Context;
import android.os.*;
import android.speech.*;
import java.util.*;
public class VoiceInputControllerTest {
 private static int checks;
 private static class Host implements VoiceInputController.Listener {
  final List<String> texts=new ArrayList<>(),errors=new ArrayList<>();
  public void onState(String text,boolean listening,boolean active){}
  public void onText(String text){texts.add(text);}
  public void onError(String text){errors.add(text);}
 }
 private static Bundle result(String... text){Bundle b=new Bundle();b.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,new ArrayList<>(Arrays.asList(text)));return b;}
 public static void main(String[] args){
  Host host=new Host();VoiceInputController voice=new VoiceInputController(new Context(),host);
  voice.start();SpeechRecognizer first=SpeechRecognizer.last;
  check(voice.active(),"start activates dictation");
  check("zh-TW".equals(first.intent.extras.get(RecognizerIntent.EXTRA_LANGUAGE)),"request Taiwan Chinese");
  first.listener.onPartialResults(result("暫時文字"));
  check(host.texts.isEmpty(),"partial results are never inserted into the field");
  voice.stop();
  check(voice.processing(),"manual stop waits for final results rather than restarting capture");
  voice.start();check(SpeechRecognizer.last==first,"cannot start another recognition while processing");
  first.listener.onResults(result("  語音輸入測試  ","alternative"));
  check(host.texts.equals(Arrays.asList("語音輸入測試")),"only final best text is inserted");
  check(!voice.active()&&first.destroyed,"success cleans up microphone session");
  first.listener.onResults(result("重複"));
  check(host.texts.size()==1,"duplicate callbacks cannot insert twice");
  voice.start();SpeechRecognizer cancelled=SpeechRecognizer.last;voice.cancel();
  cancelled.listener.onResults(result("遲到"));
  check(host.texts.size()==1&&!voice.active()&&cancelled.destroyed,"cancelled field session cannot insert late text");
  voice.start();SpeechRecognizer failed=SpeechRecognizer.last;
  failed.listener.onError(SpeechRecognizer.ERROR_NETWORK);
  check(!voice.active()&&failed.destroyed&&host.errors.get(0).contains("連線"),"network error releases mic and explains retry");
  voice.start();SpeechRecognizer empty=SpeechRecognizer.last;empty.listener.onResults(new Bundle());
  check(!voice.active()&&host.texts.size()==1,"empty recognition does not insert anything");
  voice.start();Handler.pending.run();
  check(voice.processing(),"maximum listening timeout requests final text");
  Handler.pending.run();
  check(!voice.active()&&host.errors.get(host.errors.size()-1).contains("逾時"),"processing timeout recovers to idle");
  SpeechRecognizer.available=false;voice.start();
  check(!voice.active()&&host.errors.get(host.errors.size()-1).contains("語音辨識服務"),"missing service gives setup hint");
  SpeechRecognizer.available=true;SpeechRecognizer.deny=true;voice.start();
  check(!voice.active()&&SpeechRecognizer.last.destroyed&&host.errors.get(host.errors.size()-1).contains("麥克風"),"permission error recovers without leaking recognizer");
  SpeechRecognizer.deny=false;voice.start();SpeechRecognizer retry=SpeechRecognizer.last;
  retry.listener.onEndOfSpeech();check(voice.processing(),"automatic end also waits for final results");
  retry.listener.onResults(result("可以再試"));
  check(host.texts.equals(Arrays.asList("語音輸入測試","可以再試")),"new session works after error and cancellation");
  System.out.println("PASS: "+checks+" voice controller checks (test SDK doubles; no hardware recognition)");
 }
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}

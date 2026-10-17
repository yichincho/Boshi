package tw.boshi.keyboard;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/** Uses the phone's speech-to-text service; never creates an audio file or sends a chat message. */
final class VoiceInputController {
 interface Listener {
  void onState(String text,boolean listening,boolean active);
  void onText(String text);
  void onError(String text);
 }
 private final Context context;
 private final Listener listener;
 private final VoiceInputSession session=new VoiceInputSession();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private SpeechRecognizer recognizer;
 private Runnable timeout;

 VoiceInputController(Context context,Listener listener){this.context=context;this.listener=listener;}
 boolean active(){return session.active();}
 boolean processing(){return session.processing();}

 void start(){
  if(active())return;
  if(!SpeechRecognizer.isRecognitionAvailable(context)){listener.onError("手機未提供語音辨識服務，請先啟用 Google 語音辨識或系統語音服務");return;}
  final int token=session.start();currentToken=token;
  try{
   recognizer=SpeechRecognizer.createSpeechRecognizer(context);
   recognizer.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){if(session.current(token))listener.onState("請開始說話，點 ■ 結束",true,true);}
    public void onBeginningOfSpeech(){if(session.current(token))listener.onState("正在聆聽，點 ■ 結束",true,true);}
    public void onRmsChanged(float rms){}
    public void onBufferReceived(byte[] audio){} // Audio is not retained by Boshi.
    public void onEndOfSpeech(){
     if(!session.current(token))return;
     session.stop(token);listener.onState("正在轉成文字…",false,true);
     arm(15000,()->fail(token,"語音辨識逾時，請再試一次"));
    }
    public void onError(int error){fail(token,errorMessage(error));}
    public void onResults(Bundle results){
     if(!session.finish(token))return;
     String text=firstResult(results);dispose();listener.onState("",false,false);
     if(text.isEmpty())listener.onError("沒有辨識到文字，請再試一次");else listener.onText(text);
    }
    public void onPartialResults(Bundle results){
     if(!session.current(token))return;
     String text=firstResult(results);
     if(!text.isEmpty())listener.onState(text,session.listening(),true);
    }
    public void onEvent(int event,Bundle params){}
   });
   Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-TW");
   intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
   intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   listener.onState("正在啟動語音輸入…",true,true);
   arm(45000,this::stop);recognizer.startListening(intent);
  }catch(SecurityException e){fail(token,"請允許 Boshi 使用麥克風後再試一次");}
  catch(RuntimeException e){fail(token,"無法啟動語音辨識，請檢查手機的語音服務");}
 }

 void stop(){
  if(!session.listening()||recognizer==null)return;
  // The token is captured by the recognizer callbacks; no new session may start while processing.
  final int token=currentToken;
  session.stop(token);
  listener.onState("正在轉成文字…",false,true);
  arm(15000,()->fail(token,"語音辨識逾時，請再試一次"));
  try{recognizer.stopListening();}catch(RuntimeException e){fail(token,"無法完成語音辨識，請再試一次");}
 }
 private int currentToken;

 void cancel(){
  boolean wasActive=active();session.cancel();dispose();
  if(wasActive)listener.onState("",false,false);
 }
 private void fail(int token,String text){
  if(!session.finish(token))return;
  dispose();listener.onState("",false,false);listener.onError(text);
 }
 private void arm(long milliseconds,Runnable action){
  if(timeout!=null)handler.removeCallbacks(timeout);
  timeout=action;handler.postDelayed(action,milliseconds);
 }
 private void dispose(){
  if(timeout!=null){handler.removeCallbacks(timeout);timeout=null;}
  SpeechRecognizer old=recognizer;recognizer=null;
  if(old!=null){try{old.cancel();}catch(RuntimeException ignored){}try{old.destroy();}catch(RuntimeException ignored){}}
 }
 private static String firstResult(Bundle result){
  ArrayList<String> values=result==null?null:result.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
  return values==null||values.isEmpty()||values.get(0)==null?"":values.get(0).trim();
 }
 private static String errorMessage(int error){
  switch(error){
   case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:return "請允許 Boshi 使用麥克風";
   case SpeechRecognizer.ERROR_NO_MATCH:
   case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:return "沒有聽清楚，請再說一次";
   case SpeechRecognizer.ERROR_NETWORK:
   case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:return "語音服務連線失敗，請檢查網路後重試";
   case SpeechRecognizer.ERROR_AUDIO:return "無法使用麥克風，請確認沒有其他程式正在使用";
   case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:return "手機語音服務忙碌，請稍後再試";
   default:return "語音辨識失敗（"+error+"），請再試一次";
  }
 }
}

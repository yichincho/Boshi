package android.speech;
import android.content.*;
public class SpeechRecognizer {
 public static final String RESULTS_RECOGNITION="results";
 public static final int ERROR_INSUFFICIENT_PERMISSIONS=9,ERROR_NO_MATCH=7,ERROR_SPEECH_TIMEOUT=6,ERROR_NETWORK=2,ERROR_NETWORK_TIMEOUT=1,ERROR_AUDIO=3,ERROR_RECOGNIZER_BUSY=8;
 public static boolean available=true,deny=false;
 public static SpeechRecognizer last;
 public RecognitionListener listener;public Intent intent;public boolean destroyed;
 public static boolean isRecognitionAvailable(Context context){return available;}
 public static SpeechRecognizer createSpeechRecognizer(Context context){last=new SpeechRecognizer();return last;}
 public void setRecognitionListener(RecognitionListener value){listener=value;}
 public void startListening(Intent value){if(deny)throw new SecurityException();intent=value;}
 public void stopListening(){}
 public void cancel(){}
 public void destroy(){destroyed=true;}
}

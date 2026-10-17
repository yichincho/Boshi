package android.speech;
import android.os.Bundle;
public interface RecognitionListener {
 void onReadyForSpeech(Bundle b); void onBeginningOfSpeech(); void onRmsChanged(float rms);
 void onBufferReceived(byte[] audio); void onEndOfSpeech(); void onError(int error);
 void onResults(Bundle results); void onPartialResults(Bundle results); void onEvent(int event,Bundle params);
}

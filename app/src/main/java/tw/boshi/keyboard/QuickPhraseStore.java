package tw.boshi.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Separate app-private storage; clearing learned words does not delete explicit shortcuts. */
final class QuickPhraseStore {
 private final SharedPreferences preferences;
 QuickPhraseStore(Context context){preferences=context.getSharedPreferences("quick_phrases",Context.MODE_PRIVATE);}
 QuickPhraseBook load(){return QuickPhraseBook.decode(preferences.getString("entries",""));}
 void save(QuickPhraseBook book){
  if(!preferences.edit().putString("entries",book.encode()).commit())throw new IllegalStateException("儲存失敗，請稍後再試");
 }
}

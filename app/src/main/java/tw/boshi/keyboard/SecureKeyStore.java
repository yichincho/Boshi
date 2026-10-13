package tw.boshi.keyboard;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Per-provider API keys encrypted with an Android Keystore AES-GCM key (same approach as yichincho/buylist). */
public final class SecureKeyStore {
 public static final String[] PROVIDERS={"gemini","nvidia"};
 private static final String ALIAS_PREFIX="boshi-api-key-v1-";
 private final SharedPreferences prefs;
 public SecureKeyStore(Context c){prefs=c.getSharedPreferences("encrypted_api_credentials",Context.MODE_PRIVATE);migrateLegacy(c);}

 /** v0.4 stored one plaintext key in "config"; move it into the encrypted slot of the provider it was used with. */
 private void migrateLegacy(Context c){
  SharedPreferences old=c.getSharedPreferences("config",Context.MODE_PRIVATE);String key=old.getString("key","");
  if(key.isEmpty())return;
  int provider=old.getInt("provider",0);
  try{save(PROVIDERS[provider==2?1:0],key);old.edit().remove("key").apply();}catch(Exception ignored){}
 }

 public String get(String provider){
  String iv=prefs.getString(provider+"_iv",null),data=prefs.getString(provider+"_ciphertext",null);
  if(iv==null||data==null)return "";
  try{
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.DECRYPT_MODE,key(provider),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
   return new String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8);
  }catch(Exception invalidated){clear(provider);return "";}
 }

 public void save(String provider,String value)throws Exception{
  if(value==null||value.trim().isEmpty())throw new IllegalArgumentException("API key is empty");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(provider));
  byte[] data=cipher.doFinal(value.trim().getBytes(StandardCharsets.UTF_8));
  prefs.edit().putString(provider+"_iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString(provider+"_ciphertext",Base64.encodeToString(data,Base64.NO_WRAP)).apply();
 }

 public void clear(String provider){
  prefs.edit().remove(provider+"_iv").remove(provider+"_ciphertext").apply();
  try{KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);if(s.containsAlias(ALIAS_PREFIX+provider))s.deleteEntry(ALIAS_PREFIX+provider);}catch(Exception ignored){}
 }

 private SecretKey key(String provider)throws Exception{
  String alias=ALIAS_PREFIX+provider;KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);
  SecretKey existing=(SecretKey)s.getKey(alias,null);if(existing!=null)return existing;
  KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
  return g.generateKey();
 }
}

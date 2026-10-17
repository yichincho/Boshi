package tw.boshi.keyboard;

/** Input rules shared by keyboard actions and AI request gates. */
final class InputRules {
 private InputRules() {}

 static String period(String raw, boolean english) {
  return !english && "s".equalsIgnoreCase(raw) ? "？" : ".";
 }

 /** Count Chinese characters, not Latin input codes, spaces or punctuation. */
 static boolean useAi(String text) {
  return text != null && text.codePoints()
   .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)
   .limit(6).count() > 5;
 }

 /** Earlier sentences must not make a new short sentence eligible for AI. */
 static String sentence(String text) {
  if (text == null) return "";
  int start = 0;
  for (int i = 0; i < text.length(); i++) {
   if ("。！？.!?\n\r".indexOf(text.charAt(i)) >= 0) start = i + 1;
  }
  return text.substring(start).trim();
 }
}

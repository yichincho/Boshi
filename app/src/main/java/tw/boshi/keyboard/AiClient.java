package tw.boshi.keyboard;
import org.json.*;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class AiClient {
 /** Commonly used chat models; the picker keeps only those the live model list still offers. */
 static final String[][] COMMON_MODELS={
  {},
  {"gemini-3.1-flash-lite","gemini-2.5-flash","gemini-2.5-flash-lite","gemini-2.5-pro"},
  {"meta/llama-3.3-70b-instruct","meta/llama-4-maverick-17b-128e-instruct","qwen/qwen3.5-397b-a17b","deepseek-ai/deepseek-v3.1","moonshotai/kimi-k2-instruct","openai/gpt-oss-120b","openai/gpt-oss-20b"}
 };
 private static final String[] NOT_CHAT={"embed","rerank","retriev","guard","safety","reward","parse","clip","ocr","detector","-vl","vision","whisper","tts","asr","audio","pii","topic-control","content-safety","bge","nv-embed","deplot","kosmos","paligemma","fuyu","neva","vila","cosmos","flux","stable-diffusion","sdxl"};

 public static List<String> query(int provider,String key,String model,String text)throws Exception {
  if(provider<1||key.isEmpty()||model.isEmpty())throw new IOException("AI 未設定");
  String instruction="你是繁體中文輸入法候選器。根據以下文字，給出三種自然、保留原意的繁體中文完整句子。僅輸出 JSON 字串陣列，不要解說。文字："+text;
  URL url;JSONObject request=new JSONObject();
  if(provider==1){url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(model,"UTF-8")+":generateContent");request.put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",instruction)))));}
  else {url=new URL("https://integrate.api.nvidia.com/v1/chat/completions");request.put("model",model);request.put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",instruction)));request.put("max_tokens",1024);}
  JSONObject resp=new JSONObject(send(provider,key,url,request.toString(),provider==1?15000:30000));String content;
  if(provider==1)content=resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
  else content=resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
  content=content.replaceAll("(?s)<think>.*?</think>","");
  int start=content.indexOf('['),end=content.lastIndexOf(']');if(start<0||end<=start)throw new IOException("AI 候選格式錯誤（可換一個模型試試）");
  JSONArray a=new JSONArray(content.substring(start,end+1));List<String> out=new ArrayList<>();for(int i=0;i<Math.min(5,a.length());i++){String s=a.optString(i,"").trim();if(!s.isEmpty()&&s.length()<300)out.add(s);}return out;
 }

 /** Chat models the key can use right now, read from the provider's model list. */
 public static List<String> listModels(int provider,String key)throws Exception {
  if(provider<1||key.isEmpty())throw new IOException("請先輸入 API Key");
  List<String> out=new ArrayList<>();
  if(provider==1){
   JSONArray a=new JSONObject(send(provider,key,new URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000"),null,15000)).optJSONArray("models");
   for(int i=0;a!=null&&i<a.length();i++){JSONObject m=a.getJSONObject(i);String id=m.optString("name","").replaceFirst("^models/","");
    if(id.startsWith("gemini")&&m.optJSONArray("supportedGenerationMethods")!=null&&m.getJSONArray("supportedGenerationMethods").toString().contains("\"generateContent\"")&&!id.contains("tts")&&!id.contains("image")&&!id.contains("embedding"))out.add(id);}
  } else {
   JSONArray a=new JSONObject(send(provider,key,new URL("https://integrate.api.nvidia.com/v1/models"),null,15000)).optJSONArray("data");
   for(int i=0;a!=null&&i<a.length();i++){String id=a.getJSONObject(i).optString("id","");boolean chat=!id.isEmpty();for(String bad:NOT_CHAT)if(id.toLowerCase(Locale.ROOT).contains(bad))chat=false;if(chat)out.add(id);}
  }
  Collections.sort(out);return out;
 }

 private static String send(int provider,String key,URL url,String body,int readTimeout)throws IOException {
  HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(8000);c.setReadTimeout(readTimeout);
  if(provider==1)c.setRequestProperty("x-goog-api-key",key);else c.setRequestProperty("Authorization","Bearer "+key);
  c.setRequestProperty("Accept","application/json");
  try{
   if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
   int code=c.getResponseCode();
   if(code<200||code>=300)throw new IOException(describe(code,read(c.getErrorStream())));
   return read(c.getInputStream());
  }catch(SocketTimeoutException e){throw new IOException("等待逾時，伺服器忙碌或模型太慢，可換一個模型");}
  finally{c.disconnect();}
 }

 /** Turns an HTTP failure into a hint the user can act on, keeping the provider's own message. */
 private static String describe(int code,String body){
  String detail="";
  try{JSONObject j=new JSONObject(body);Object e=j.opt("error");detail=e instanceof JSONObject?((JSONObject)e).optString("message",""):j.optString("detail",e==null?"":String.valueOf(e));}catch(Exception ignored){detail=body;}
  detail=detail.trim();if(detail.length()>120)detail=detail.substring(0,120)+"…";
  String hint=code==401||code==403?"API Key 無效或沒有權限":code==404?"找不到這個模型，請按「選擇模型」換一個":code==429?"用量超過限制，稍後再試":code>=500?"服務暫時故障，稍後再試或換模型":"請求失敗";
  return hint+"（HTTP "+code+(detail.isEmpty()?"":"："+detail)+"）";
 }

 private static String read(InputStream in)throws IOException {
  if(in==null)return "";
  ByteArrayOutputStream s=new ByteArrayOutputStream();try(InputStream i=in){byte[] buf=new byte[8192];int n;while((n=i.read(buf))!=-1&&s.size()<2000000)s.write(buf,0,n);}
  return s.toString("UTF-8");
 }
}

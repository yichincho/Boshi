package tw.boshi.keyboard;
import org.json.*;import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
public class AiClient {
 public static List<String> query(int provider,String key,String model,String text)throws Exception {
  if(provider<1||key.isEmpty()||model.isEmpty())throw new IOException("AI 未設定");
  String instruction="你是繁體中文輸入法候選器。根據以下文字，給出三種自然、保留原意的繁體中文完整句子。僅輸出 JSON 字串陣列，不要解說。文字："+text;
  URL url;JSONObject request=new JSONObject(); String auth=null;
  if(provider==1){url=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+URLEncoder.encode(model,"UTF-8")+":generateContent");request.put("contents",new JSONArray().put(new JSONObject().put("parts",new JSONArray().put(new JSONObject().put("text",instruction)))));auth="gemini";}
  else {url=new URL("https://integrate.api.nvidia.com/v1/chat/completions");request.put("model",model);request.put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",instruction)));request.put("max_tokens",300);auth="nvidia";}
  HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(5500);c.setReadTimeout(9000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
  if(auth.equals("gemini"))c.setRequestProperty("x-goog-api-key",key);else c.setRequestProperty("Authorization","Bearer "+key);
  try{try(OutputStream out=c.getOutputStream()){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
   int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("API HTTP "+code);
   StringBuilder body=new StringBuilder();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1&&body.length()<30000)body.append(new String(buf,0,n,StandardCharsets.UTF_8));}
   JSONObject resp=new JSONObject(body.toString());String content;
   if(provider==1)content=resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
   else content=resp.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
   int start=content.indexOf('['),end=content.lastIndexOf(']');if(start<0||end<=start)throw new IOException("AI 候選格式錯誤");
   JSONArray a=new JSONArray(content.substring(start,end+1));List<String> out=new ArrayList<>();for(int i=0;i<Math.min(5,a.length());i++){String s=a.optString(i,"").trim();if(!s.isEmpty()&&s.length()<300)out.add(s);}return out;
  }finally{c.disconnect();}
 }
}

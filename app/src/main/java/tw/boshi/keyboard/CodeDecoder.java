package tw.boshi.keyboard;
import android.content.Context;
import java.io.*;import java.util.*;
public class CodeDecoder {
 private final Map<String,List<String>> dict=new HashMap<>(); private int longest=1;
 public synchronized int load(Context c)throws IOException{
  dict.clear();longest=1; File f=new File(c.getFilesDir(),"codes.tsv");
  InputStream source=f.exists()?new FileInputStream(f):c.getAssets().open("boshiamy.tsv");
  int n=0;try(BufferedReader rd=new BufferedReader(new InputStreamReader(source,"UTF-8"))){
   String s;while((s=rd.readLine())!=null){String[] a=s.split("\t",2);if(a.length!=2||!a[0].matches("[a-zA-Z]{1,8}")||a[1].trim().isEmpty())continue;
    String code=a[0].toLowerCase(Locale.ROOT);dict.computeIfAbsent(code,k->new ArrayList<>()).add(a[1].trim());longest=Math.max(longest,code.length());n++;
   }
  }
  if(n==0 && f.exists()){
   // Recover from an invalid old manual import without requiring user action.
   f.delete();return load(c);
  }
  return n;
 }
 private static class State{String text;int score;State(String s,int p){text=s;score=p;}}
 public synchronized List<String> decode(String input){
  input=input.toLowerCase(Locale.ROOT); if(input.length()>48)return Collections.emptyList();
  List<List<State>> dp=new ArrayList<>();for(int i=0;i<=input.length();i++)dp.add(new ArrayList<>());
  dp.get(0).add(new State("",0));
  for(int end=1;end<=input.length();end++){
   List<State> next=dp.get(end);
   for(int start=Math.max(0,end-longest);start<end;start++){
    List<String> words=dict.get(input.substring(start,end));if(words==null)continue;
    for(State s:dp.get(start))for(int j=0;j<Math.min(words.size(),5);j++) next.add(new State(s.text+words.get(j),s.score+j*2+1));
   }
   next.sort(Comparator.comparingInt(a->a.score));if(next.size()>20)next.subList(20,next.size()).clear();
  }
  LinkedHashSet<String> result=new LinkedHashSet<>();for(State s:dp.get(input.length()))result.add(s.text);
  List<String> out=new ArrayList<>(result);return out.subList(0,Math.min(5,out.size()));
 }
}

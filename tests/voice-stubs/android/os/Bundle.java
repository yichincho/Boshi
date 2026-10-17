package android.os;
import java.util.*;
public class Bundle {
 private final Map<String,ArrayList<String>> data=new HashMap<>();
 public void putStringArrayList(String key,ArrayList<String> values){data.put(key,values);}
 public ArrayList<String> getStringArrayList(String key){return data.get(key);}
}

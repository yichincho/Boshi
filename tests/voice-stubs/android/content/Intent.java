package android.content;
import java.util.*;
public class Intent {
 public final Map<String,Object> extras=new HashMap<>();
 public Intent(String action){}
 public Intent putExtra(String key,String value){extras.put(key,value);return this;}
 public Intent putExtra(String key,boolean value){extras.put(key,value);return this;}
 public Intent putExtra(String key,int value){extras.put(key,value);return this;}
}

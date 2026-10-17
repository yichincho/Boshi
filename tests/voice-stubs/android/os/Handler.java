package android.os;
public class Handler {
 public static Runnable pending;
 public Handler(Looper looper){}
 public void postDelayed(Runnable action,long delay){pending=action;}
 public void removeCallbacks(Runnable action){if(pending==action)pending=null;}
}

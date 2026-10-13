package tw.boshi.keyboard;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * On-device learning store (SQLite, app-private, never uploaded).
 * picks:   which word the user chose for a Boshiamy code, so it is offered first next time.
 * bigrams: which word the user typed right after another, for offline next-word suggestions.
 * phrases: frequent two-step continuations built from bigrams by {@link #analyze()}.
 */
public final class UserDict extends SQLiteOpenHelper {
 private static final long DAY=86400000L;
 private static UserDict instance;
 private final ExecutorService writer=Executors.newSingleThreadExecutor();

 public static synchronized UserDict get(Context c){if(instance==null)instance=new UserDict(c.getApplicationContext());return instance;}
 private UserDict(Context c){super(c,"user_dict.db",null,1);setWriteAheadLoggingEnabled(true);}

 @Override public void onCreate(SQLiteDatabase db){
  db.execSQL("CREATE TABLE picks(code TEXT NOT NULL,word TEXT NOT NULL,count REAL NOT NULL,last INTEGER NOT NULL,PRIMARY KEY(code,word))");
  db.execSQL("CREATE TABLE bigrams(prev TEXT NOT NULL,next TEXT NOT NULL,count REAL NOT NULL,last INTEGER NOT NULL,PRIMARY KEY(prev,next))");
  db.execSQL("CREATE TABLE phrases(head TEXT NOT NULL,tail TEXT NOT NULL,count REAL NOT NULL,PRIMARY KEY(head,tail))");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}

 /** Records a commit in the background: the code→word choice (code may be null) and the word that preceded it. */
 public void record(String code,String prev,String word){
  long now=System.currentTimeMillis();
  writer.execute(()->{
   SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
   try{
    if(code!=null&&!code.isEmpty())bump(db,"picks","code","word",code,word,now);
    if(prev!=null&&!prev.isEmpty())bump(db,"bigrams","prev","next",prev,word,now);
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
  });
 }
 private static void bump(SQLiteDatabase db,String table,String a,String b,String va,String vb,long now){
  // UPSERT needs SQLite 3.24 (Android 11); minSdk 26 ships 3.18, so insert-or-ignore then update.
  db.execSQL("INSERT OR IGNORE INTO "+table+"("+a+","+b+",count,last) VALUES(?,?,0,?)",new Object[]{va,vb,now});
  db.execSQL("UPDATE "+table+" SET count=count+1,last=? WHERE "+a+"=? AND "+b+"=?",new Object[]{now,va,vb});
 }

 /** Puts the words the user picked for this exact code before the decoder's own order. */
 public List<String> preferred(String code,List<String> decoded){
  if(code.isEmpty())return decoded;
  LinkedHashSet<String> out=new LinkedHashSet<>();
  try(Cursor c=getReadableDatabase().rawQuery("SELECT word FROM picks WHERE code=? ORDER BY count DESC,last DESC LIMIT 5",new String[]{code})){while(c.moveToNext())out.add(c.getString(0));}
  catch(Exception ignored){return decoded;}
  out.addAll(decoded);
  return new ArrayList<>(out).subList(0,Math.min(8,out.size()));
 }

 /** What the user usually types after {@code prev}: direct followers first, then analysed phrases. */
 public List<String> suggestions(String prev,int limit){
  LinkedHashSet<String> out=new LinkedHashSet<>();
  if(prev==null||prev.isEmpty())return new ArrayList<>();
  try{
   SQLiteDatabase db=getReadableDatabase();
   try(Cursor c=db.rawQuery("SELECT next FROM bigrams WHERE prev=? ORDER BY count DESC,last DESC LIMIT ?",new String[]{prev,String.valueOf(limit)})){while(c.moveToNext())out.add(c.getString(0));}
   try(Cursor c=db.rawQuery("SELECT tail FROM phrases WHERE head=? ORDER BY count DESC LIMIT 3",new String[]{prev})){while(c.moveToNext())out.add(c.getString(0));}
  }catch(Exception ignored){}
  return new ArrayList<>(out);
 }

 /**
  * A short sample of the user's own wording for the AI prompt: what usually follows {@code prev},
  * then their most used multi-character words and pairs. At most 30 short items.
  */
 public List<String> habits(String prev){
  LinkedHashSet<String> out=new LinkedHashSet<>();
  try{
   SQLiteDatabase db=getReadableDatabase();
   if(prev!=null&&!prev.isEmpty()){
    try(Cursor c=db.rawQuery("SELECT ?||next FROM bigrams WHERE prev=? ORDER BY count DESC LIMIT 8",new String[]{prev,prev})){while(c.moveToNext())out.add(c.getString(0));}
    try(Cursor c=db.rawQuery("SELECT ?||tail FROM phrases WHERE head=? ORDER BY count DESC LIMIT 4",new String[]{prev,prev})){while(c.moveToNext())out.add(c.getString(0));}
   }
   try(Cursor c=db.rawQuery("SELECT word FROM picks WHERE length(word)>=2 GROUP BY word HAVING SUM(count)>=2 ORDER BY SUM(count) DESC LIMIT 12",null)){while(c.moveToNext())out.add(c.getString(0));}
   try(Cursor c=db.rawQuery("SELECT prev||next FROM bigrams WHERE count>=2 ORDER BY count DESC LIMIT 10",null)){while(c.moveToNext())out.add(c.getString(0));}
  }catch(Exception ignored){}
  List<String> list=new ArrayList<>();for(String h:out)if(h.length()<=12&&list.size()<30)list.add(h);
  return list;
 }

 /** Periodic job: fade entries unused for 30 days, drop the faded-out ones and rebuild frequent phrases. */
 public synchronized String analyze(){
  long now=System.currentTimeMillis();SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try{
   for(String t:new String[]{"picks","bigrams"}){
    db.execSQL("UPDATE "+t+" SET count=count*0.8 WHERE last<?",new Object[]{now-30*DAY});
    db.execSQL("DELETE FROM "+t+" WHERE count<0.5");
   }
   db.execSQL("DELETE FROM phrases");
   db.execSQL("INSERT INTO phrases(head,tail,count) SELECT a.prev,a.next||b.next,MIN(a.count,b.count) FROM bigrams a JOIN bigrams b ON a.next=b.prev WHERE a.count>=3 AND b.count>=3 AND a.next<>b.next ORDER BY MIN(a.count,b.count) DESC LIMIT 2000");
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
  return summary();
 }

 /** Human-readable totals and the most used words, for the settings screen. */
 public String summary(){
  SQLiteDatabase db=getReadableDatabase();
  long picks=count(db,"picks"),bigrams=count(db,"bigrams"),phrases=count(db,"phrases");
  StringBuilder top=new StringBuilder();
  try(Cursor c=db.rawQuery("SELECT word FROM picks GROUP BY word ORDER BY SUM(count) DESC LIMIT 12",null)){while(c.moveToNext())top.append(top.length()>0?"、":"").append(c.getString(0));}
  StringBuilder pairs=new StringBuilder();
  try(Cursor c=db.rawQuery("SELECT prev||next FROM bigrams ORDER BY count DESC LIMIT 8",null)){while(c.moveToNext())pairs.append(pairs.length()>0?"、":"").append(c.getString(0));}
  return "已記錄 "+picks+" 個選字、"+bigrams+" 組接續詞、"+phrases+" 個常用片語"+(top.length()>0?"\n常用字詞："+top:"")+(pairs.length()>0?"\n常用組合："+pairs:"");
 }
 private static long count(SQLiteDatabase db,String t){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+t,null)){return c.moveToFirst()?c.getLong(0):0;}}

 public void clear(){writer.execute(()->{SQLiteDatabase db=getWritableDatabase();for(String t:new String[]{"picks","bigrams","phrases"})db.delete(t,null,null);});}
}

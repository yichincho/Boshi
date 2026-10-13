package tw.boshi.keyboard;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Re-analyses the learned words every N hours (set in Boshi settings) while the battery is not low. */
public class AnalysisJob extends JobService {
 private static final int JOB_ID=1001;
 static final int[] HOURS={1,6,24};

 /** (Re)schedules the periodic job when it is missing or its interval changed. */
 static void schedule(Context c){
  int hours=c.getSharedPreferences("config",MODE_PRIVATE).getInt("analyze_hours",6);
  JobScheduler js=(JobScheduler)c.getSystemService(JOB_SCHEDULER_SERVICE);if(js==null)return;
  JobInfo current=js.getPendingJob(JOB_ID);long period=hours*3600000L;
  if(current!=null&&current.getIntervalMillis()==period)return;
  js.schedule(new JobInfo.Builder(JOB_ID,new ComponentName(c,AnalysisJob.class)).setPeriodic(period).setRequiresBatteryNotLow(true).setPersisted(true).build());
 }

 /** Runs one analysis and remembers when, for the settings screen. */
 static String runNow(Context c){
  String summary=UserDict.get(c).analyze();
  c.getSharedPreferences("config",MODE_PRIVATE).edit().putLong("analyzed_at",System.currentTimeMillis()).apply();
  return summary;
 }

 @Override public boolean onStartJob(JobParameters params){
  new Thread(()->{try{runNow(this);}catch(Exception ignored){}jobFinished(params,false);}).start();
  return true;
 }
 @Override public boolean onStopJob(JobParameters params){return true;}
}

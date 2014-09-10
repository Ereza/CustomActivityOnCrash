package cat.ereza.example.customactivityoncrash;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashableApplication extends Application {

    public final static String TAG = "CRASHABLE";
    public final static int EXTRA_TIME_TO_WAIT_MILLIS = 200;

    @Override
    public void onCreate() {
        super.onCreate();

        //We define a default exception handler that just kills the process so it can be called from Crashlytics/ACRA
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                //You MUST do this to finish the process, otherwise you will be stuck in limbo.
                Log.d(TAG, "Finishing process!");
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });

        //Initialize your error handlers as normal, they will most likely keep a reference to the original exception handler
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);

        //Keep a reference to the original handler to use it later, after handling the exception ourselves
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.d(TAG, "Crashed, executing UncaughtExceptionHandler");

                Intent intent = new Intent(CrashableApplication.this, ErrorActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);

                //Change this to anything you need to treat the error and pass any parameters you like...
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                intent.putExtra(ErrorActivity.EXTRA_EXCEPTION, sw.toString());

                //We define an alarm to launch our new intent in very little time
                //EXTRA_TIME_TO_WAIT_MILLIS is necessary to avoid launching it while it still has not finished (I know it's ugly...)
                PendingIntent pendingIntent = PendingIntent.getActivity(CrashableApplication.this, 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + EXTRA_TIME_TO_WAIT_MILLIS, pendingIntent);

                //Call the original handler which will handle the exception as normal and then launch our own finishing handler (avoiding the "App has stopped" dialog)
                originalHandler.uncaughtException(thread, throwable);
            }
        });
    }
}

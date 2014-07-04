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

    @Override
    public void onCreate() {
        super.onCreate();
        //Initialize your error handlers as normal
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);

        //Keep a reference to the original handler if you plan to use it later (see comments below)
        //final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();

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

                PendingIntent pendingIntent = PendingIntent.getActivity(CrashableApplication.this, 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent);


                //Log here your exception to any crash-reporting system if needed
                //If you use Acra, you might want to call ACRA.getErrorReporter().handleException(throwable);
                //If you use Crashlytics, you might want to call Crashlytics.logException(throwable);

                //You can also call originalHandler.uncaughtException(thread, throwable), but
                //you will probably get the system "App has stopped" dialog if you do so (depends on how the original handler handles the exception)

                //You MUST do this to finish the process, otherwise you will be stuck in limbo.
                Log.d(TAG, "Finishing process!");
                System.exit(0);

            }
        });
    }
}

package cat.ereza.example.customactivityoncrash;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

public class CrashableApplication extends Application {

    public final static String TAG = "CRASHABLE";

    private WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);

    @Override
    public void onCreate() {
        super.onCreate();

        //We define a default exception handler that does what we want so it can be called from Crashlytics/ACRA
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, final Throwable throwable) {
                Log.d(TAG, "App has crashed, executing UncaughtExceptionHandler", throwable);

                new Thread() {
                    @Override
                    public void run() {
                        final Intent intent = new Intent(CrashableApplication.this, ErrorActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        //Change this to anything you need to treat the error and pass any parameters you like...
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwable.printStackTrace(pw);
                        intent.putExtra(ErrorActivity.EXTRA_EXCEPTION, sw.toString());

                        startActivity(intent);
                        final Activity lastActivity = lastActivityCreated.get();
                        if (lastActivity != null) {
                            //We finish the activity, this solves a bug, see https://github.com/ACRA/acra/issues/42
                            lastActivity.finish();
                            lastActivityCreated.clear();
                        }
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(10);
                    }
                }.start();
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    if (!(activity instanceof ErrorActivity)) {
                        // Copied from ACRA:
                        // Ignore ErrorActivity because we want the last
                        // application Activity that was started so that we can
                        // explicitly kill it off.
                        lastActivityCreated = new WeakReference<>(activity);
                    }
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    //Do nothing
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    //Do nothing
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    //Do nothing
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    //Do nothing
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    //Do nothing
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    //Do nothing
                }
            });
        }
        //Initialize your error handlers as normal, they will most likely keep a reference to the original exception handler
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);
    }
}

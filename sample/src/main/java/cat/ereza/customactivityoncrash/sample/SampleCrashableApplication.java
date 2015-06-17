package cat.ereza.customactivityoncrash.sample;

import android.app.Application;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class SampleCrashableApplication extends Application {

    public final static String TAG = "SampleCrashableApp";

    @Override
    public void onCreate() {
        super.onCreate();

        //Install CustomActivityOnCrash
        CustomActivityOnCrash.install(this, ErrorActivity.class, ErrorActivity.EXTRA_EXCEPTION, false);

        //Now initialize your error handlers as normal, they will most likely keep a reference to the original exception handler
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);
    }
}

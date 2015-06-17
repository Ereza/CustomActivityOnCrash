/*
 * Copyright 2015 Eduard Ereza Martínez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cat.ereza.customactivityoncrash;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

public class CustomActivityOnCrash {
    private final static String TAG = "CustomActivityOnCrash";
    private static final String CAOC_HANDLER_PACKAGE_NAME = "cat.ereza.customactivityoncrash";
    private static final String DEFAULT_HANDLER_PACKAGE_NAME = "com.android.internal.os";
    private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1

    private static WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);
    private static Application application;
    private static boolean isInBackground = false;

    /**
     * Installs CustomActivityOnCrash on the application.
     *
     * @param context       Context to use for obtaining the ApplicationContext. Must not be null.
     * @param activityClass Activity to launch when the app crashes. Must not be null.
     */
    public static void install(Context context, final Class<? extends Activity> activityClass) {
        installInternal(context, activityClass, null, true, false);
    }

    /**
     * Installs CustomActivityOnCrash on the application.
     *
     * @param context            Context to use for obtaining the ApplicationContext. Must not be null.
     * @param activityClass      Activity to launch when the app crashes. Must not be null.
     * @param exceptionParameter String to use as a Intent extra to pass the stack trace to the launched activity. Must not be null.
     */
    public static void install(Context context, final Class<? extends Activity> activityClass, final String exceptionParameter) {
        installInternal(context, activityClass, exceptionParameter, true, true);
    }

    /**
     * Installs CustomActivityOnCrash on the application.
     *
     * @param context                         Context to use for obtaining the ApplicationContext. Must not be null.
     * @param activityClass                   Activity to launch when the app crashes. Must not be null.
     * @param startActivityEvenIfInBackground true if you want to launch the error activity even if the app is in background, false otherwise. This has no effect in API<14 and the activity is always launched.
     */
    public static void install(Context context, final Class<? extends Activity> activityClass, boolean startActivityEvenIfInBackground) {
        installInternal(context, activityClass, null, startActivityEvenIfInBackground, false);
    }

    /**
     * Installs CustomActivityOnCrash on the application.
     *
     * @param context                         Context to use for obtaining the ApplicationContext. Must not be null.
     * @param activityClass                   Activity to launch when the app crashes. Must not be null.
     * @param exceptionParameter              String to use as a Intent extra to pass the stack trace to the launched activity. Must not be null.
     * @param startActivityEvenIfInBackground true if you want to launch the error activity even if the app is in background, false otherwise. This has no effect in API<14 and the activity is always launched.
     */
    public static void install(Context context, final Class<? extends Activity> activityClass, final String exceptionParameter, boolean startActivityEvenIfInBackground) {
        installInternal(context, activityClass, exceptionParameter, startActivityEvenIfInBackground, true);
    }

    private static void installInternal(Context context, final Class<? extends Activity> activityClass, final String exceptionParameter, final boolean startActivityEvenIfInBackground, boolean reportExceptionParameter) {
        try {
            if (context == null) {
                Log.e(TAG, "context is null! Not installing...");
            } else if (activityClass == null) {
                Log.e(TAG, "activityClass is null! Not installing...");
            } else {
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    Log.w(TAG, "CustomActivityOnCrash will be installed, but may not be reliable in API lower than 14");
                }
                if (reportExceptionParameter && (exceptionParameter == null || exceptionParameter.equals(""))) {
                    Log.e(TAG, "exceptionParameter is null or empty! Will install, but will not pass any data to the launched activity");
                }

                //INSTALL!
                Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

                if (oldHandler != null && oldHandler.getClass().getName().startsWith(CAOC_HANDLER_PACKAGE_NAME)) {
                    Log.e(TAG, "You already have installed CustomActivityOnCrash, doing nothing!");
                } else {
                    if (oldHandler != null && !oldHandler.getClass().getName().startsWith(DEFAULT_HANDLER_PACKAGE_NAME)) {
                        Log.e(TAG, "IMPORTANT WARNING! You already have an UncaughtExceptionHandler, are you sure this is correct? If you use ACRA, Crashlytics or similar libraries, you must initialize them AFTER CustomActivityOnCrash! Installing anyway, but your original handler will not be called.");
                    }

                    application = (Application) context.getApplicationContext();

                    //We define a default exception handler that does what we want so it can be called from Crashlytics/ACRA
                    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread thread, final Throwable throwable) {
                            Log.e(TAG, "App has crashed, executing UncaughtExceptionHandler", throwable);
                            new Thread() {
                                @Override
                                public void run() {
                                    if (isStackTraceLikelyConflictive(throwable, activityClass)) {
                                        Log.e(TAG, "Your application class or your error activity have crashed, the custom activity will not be launched!");
                                    } else {
                                        if (startActivityEvenIfInBackground || !isInBackground) {
                                            final Intent intent = new Intent(application, activityClass);
                                            if (exceptionParameter != null && !exceptionParameter.equals("")) {
                                                StringWriter sw = new StringWriter();
                                                PrintWriter pw = new PrintWriter(sw);
                                                throwable.printStackTrace(pw);
                                                String stackTraceString = sw.toString();

                                                //Reduce data to 128KB so we don't get a TransactionTooLargeException when sending the intent.
                                                //The limit is 1MB on Android but some devices seem to have it lower.
                                                //See: http://developer.android.com/reference/android/os/TransactionTooLargeException.html
                                                //And: http://stackoverflow.com/questions/11451393/what-to-do-on-transactiontoolargeexception#comment46697371_12809171
                                                if (stackTraceString.length() > MAX_STACK_TRACE_SIZE) {
                                                    String disclaimer = " [stack trace too large]";
                                                    stackTraceString = stackTraceString.substring(0, MAX_STACK_TRACE_SIZE - disclaimer.length()) + disclaimer;
                                                }

                                                intent.putExtra(exceptionParameter, stackTraceString);
                                            }
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            application.startActivity(intent);
                                        }
                                    }
                                    final Activity lastActivity = lastActivityCreated.get();
                                    if (lastActivity != null) {
                                        //We finish the activity, this solves a bug which causes infinite recursion.
                                        //This is unsolvable in API<14, so beware!
                                        //See: https://github.com/ACRA/acra/issues/42
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
                        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                            int currentlyStartedActivities = 0;

                            @Override
                            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                                if (activity.getClass() != activityClass) {
                                    // Copied from ACRA:
                                    // Ignore activityClass because we want the last
                                    // application Activity that was started so that we can
                                    // explicitly kill it off.
                                    lastActivityCreated = new WeakReference<>(activity);
                                }
                            }

                            @Override
                            public void onActivityStarted(Activity activity) {
                                currentlyStartedActivities++;
                                isInBackground = (currentlyStartedActivities == 0);
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
                                currentlyStartedActivities--;
                                isInBackground = (currentlyStartedActivities == 0);
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
                }

                Log.i(TAG, "CustomActivityOnCrash has been installed.");
            }
        } catch (Throwable t) {
            Log.e(TAG, "An unknown error occurred when initializing CustomActivityOnCrash, it may not have been installed. Please report this as a bug if needed.", t);
        }
    }

    /**
     * Checks if the stack trace that just crashed is conflictive. This is true in the following scenarios:
     * - The application has crashed while initializing (handleBindApplication is in the stack)
     * - The error activity has crashed (activityClass is in the stack)
     *
     * @param throwable     The throwable from which the stack trace will be checked
     * @param activityClass The activity class to launch when the app crashes
     * @return true if this stack trace is conflictive and the activity must not be launched, false otherwise
     */
    private static boolean isStackTraceLikelyConflictive(Throwable throwable, Class<? extends Activity> activityClass) {
        do {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if ((element.getClassName().equals("android.app.ActivityThread") && element.getMethodName().equals("handleBindApplication")) || element.getClassName().equals(activityClass.getName())) {
                    return true;
                }
            }
        } while ((throwable = throwable.getCause()) != null);
        return false;
    }
}

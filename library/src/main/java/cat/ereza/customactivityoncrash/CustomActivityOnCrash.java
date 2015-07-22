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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

import cat.ereza.customactivityoncrash.activity.DefaultErrorActivity;

@SuppressLint("NewApi")
public final class CustomActivityOnCrash {
    private static final String EXTRA_STACK_TRACE = "cat.ereza.customactivityoncrash.EXTRA_STACK_TRACE";
    private static final String EXTRA_RESTART_ACTIVITY_CLASS = "cat.ereza.customactivityoncrash.EXTRA_RESTART_ACTIVITY_CLASS";

    private final static String TAG = "CustomActivityOnCrash";
    private static final String CAOC_HANDLER_PACKAGE_NAME = "cat.ereza.customactivityoncrash";
    private static final String DEFAULT_HANDLER_PACKAGE_NAME = "com.android.internal.os";
    private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1

    private static WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);
    private static Application application;
    private static boolean isInBackground = false;

    //Settable properties
    private static boolean launchActivityEvenIfInBackground = true;
    private static Class<? extends Activity> errorActivityClass = DefaultErrorActivity.class;
    private static Class<? extends Activity> restartActivityClass = null;

    /**
     * Installs CustomActivityOnCrash on the application using the default error activity.
     *
     * @param context Context to use for obtaining the ApplicationContext. Must not be null.
     */
    public static void install(Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "Install failed: context is null!");
            } else {
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    Log.w(TAG, "CustomActivityOnCrash will be installed, but may not be reliable in API lower than 14");
                }

                //INSTALL!
                Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

                if (oldHandler != null && oldHandler.getClass().getName().startsWith(CAOC_HANDLER_PACKAGE_NAME)) {
                    Log.e(TAG, "You have already installed CustomActivityOnCrash, doing nothing!");
                } else {
                    if (oldHandler != null && !oldHandler.getClass().getName().startsWith(DEFAULT_HANDLER_PACKAGE_NAME)) {
                        Log.e(TAG, "IMPORTANT WARNING! You already have an UncaughtExceptionHandler, are you sure this is correct? If you use ACRA, Crashlytics or similar libraries, you must initialize them AFTER CustomActivityOnCrash! Installing anyway, but your original handler will not be called.");
                    }

                    application = (Application) context.getApplicationContext();

                    //We define a default exception handler that does what we want so it can be called from Crashlytics/ACRA
                    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread thread, final Throwable throwable) {
                            Log.e(TAG, "App has crashed, executing CustomActivityOnCrash's UncaughtExceptionHandler", throwable);

                            if (isStackTraceLikelyConflictive(throwable, errorActivityClass)) {
                                Log.e(TAG, "Your application class or your error activity have crashed, the custom activity will not be launched!");
                            } else {
                                if (launchActivityEvenIfInBackground || !isInBackground) {
                                    final Intent intent = new Intent(application, errorActivityClass);
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

                                    intent.putExtra(EXTRA_STACK_TRACE, stackTraceString);
                                    intent.putExtra(EXTRA_RESTART_ACTIVITY_CLASS, restartActivityClass);
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
                    });
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                            int currentlyStartedActivities = 0;

                            @Override
                            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                                if (activity.getClass() != errorActivityClass) {
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

                    Log.i(TAG, "CustomActivityOnCrash has been installed.");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "An unknown error occurred while installing CustomActivityOnCrash, it may not have been properly initialized. Please report this as a bug if needed.", t);
        }
    }

    /**
     * Given an Intent, returns the stack trace extra from it.
     *
     * @param intent The Intent. Must not be null.
     * @return The stacktrace, or null if not provided.
     */
    public static String getStackTraceFromIntent(Intent intent) {
        return intent.getStringExtra(CustomActivityOnCrash.EXTRA_STACK_TRACE);
    }

    /**
     * Given an Intent, returns the restart activity class extra from it.
     *
     * @param intent The Intent. Must not be null.
     * @return The restart activity class, or null if not provided.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Activity> getRestartActivityClassFromIntent(Intent intent) {
        Serializable serializedClass = intent.getSerializableExtra(CustomActivityOnCrash.EXTRA_RESTART_ACTIVITY_CLASS);

        if (serializedClass != null && serializedClass instanceof Class) {
            return (Class<? extends Activity>) serializedClass;
        } else {
            return null;
        }
    }

    /**
     * Initializes CustomActivityOnCrash on the application.
     *
     * @param context            Context to use for obtaining the ApplicationContext. Must not be null.
     * @param errorActivityClass Activity to launch when the app crashes. Must not be null.
     * @deprecated Will be removed in the future. Use setErrorActivityClass(class) and install(context)
     */
    @Deprecated
    public static void init(Context context, final Class<? extends Activity> errorActivityClass) {
        setErrorActivityClass(errorActivityClass);
        setLaunchActivityEvenIfInBackground(true);
        install(context);
    }

    /**
     * Initializes CustomActivityOnCrash on the application.
     *
     * @param context                          Context to use for obtaining the ApplicationContext. Must not be null.
     * @param errorActivityClass               Activity to launch when the app crashes. Must not be null.
     * @param launchActivityEvenIfInBackground true if you want to launch the error activity even if the app is in background, false otherwise. This has no effect in API<14 and the activity is always launched.
     * @deprecated Will be removed in the future. Use setErrorActivityClass(class), setLaunchActivityEvenIfInBackground(boolean) and install(context)
     */
    @Deprecated
    public static void init(Context context, final Class<? extends Activity> errorActivityClass, boolean launchActivityEvenIfInBackground) {
        setErrorActivityClass(errorActivityClass);
        setLaunchActivityEvenIfInBackground(launchActivityEvenIfInBackground);
        install(context);
    }

    /**
     * INTERNAL method that checks if the stack trace that just crashed is conflictive. This is true in the following scenarios:
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

    /**
     * Returns the error activity class to launch when a crash occurs.
     *
     * @return The class, or DefaultErrorActivity if not set.
     */
    public static Class<? extends Activity> getErrorActivityClass() {
        return errorActivityClass;
    }

    /**
     * Sets the error activity class to launch when a crash occurs.
     * If null,the default error activity will be used.
     */
    public static void setErrorActivityClass(Class<? extends Activity> errorActivityClass) {
        if (errorActivityClass != null) {
            CustomActivityOnCrash.errorActivityClass = errorActivityClass;
        } else {
            CustomActivityOnCrash.errorActivityClass = DefaultErrorActivity.class;
        }
    }

    /**
     * Returns the main activity class that the error activity must launch when a crash occurs.
     *
     * @return The class, or null if not set.
     */
    public static Class<? extends Activity> getRestartActivityClass() {
        return restartActivityClass;
    }

    /**
     * Sets the main activity class that the error activity must launch when a crash occurs.
     * If not set or set to null, the default error activity will close instead.
     */
    public static void setRestartActivityClass(Class<? extends Activity> restartActivityClass) {
        CustomActivityOnCrash.restartActivityClass = restartActivityClass;
    }

    /**
     * Returns if the error activity must be launched even if the app is on background.
     *
     * @return true if it will be launched, false otherwise.
     */
    public static boolean isLaunchActivityEvenIfInBackground() {
        return launchActivityEvenIfInBackground;
    }

    /**
     * Defines if the error activity must be launched even if the app is on background.
     * Set it to true if you want to launch the error activity even if the app is in background,
     * false if you want it not to launch and crash silently.
     * This has no effect in API<14 and the error activity is always launched.
     * The default is true (the app will be brought to front when a crash occurs).
     */
    public static void setLaunchActivityEvenIfInBackground(boolean launchActivityEvenIfInBackground) {
        CustomActivityOnCrash.launchActivityEvenIfInBackground = launchActivityEvenIfInBackground;
    }
}

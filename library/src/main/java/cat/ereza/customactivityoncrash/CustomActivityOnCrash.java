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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cat.ereza.customactivityoncrash.activity.DefaultErrorActivity;

@SuppressLint("NewApi")
public final class CustomActivityOnCrash {

    private final static String TAG = "CustomActivityOnCrash";

    //Extras passed to the error activity
    private static final String EXTRA_RESTART_ACTIVITY_CLASS = "cat.ereza.customactivityoncrash.EXTRA_RESTART_ACTIVITY_CLASS";
    private static final String EXTRA_SHOW_ERROR_DETAILS = "cat.ereza.customactivityoncrash.EXTRA_SHOW_ERROR_DETAILS";
    private static final String EXTRA_STACK_TRACE = "cat.ereza.customactivityoncrash.EXTRA_STACK_TRACE";
    private static final String EXTRA_IMAGE_DRAWABLE_ID = "cat.ereza.customactivityoncrash.EXTRA_IMAGE_DRAWABLE_ID";
    private static final String EXTRA_EVENT_LISTENER = "cat.ereza.customactivityoncrash.EXTRA_EVENT_LISTENER";

    //General constants
    private static final String INTENT_ACTION_ERROR_ACTIVITY = "cat.ereza.customactivityoncrash.ERROR";
    private static final String INTENT_ACTION_RESTART_ACTIVITY = "cat.ereza.customactivityoncrash.RESTART";
    private static final String CAOC_HANDLER_PACKAGE_NAME = "cat.ereza.customactivityoncrash";
    private static final String DEFAULT_HANDLER_PACKAGE_NAME = "com.android.internal.os";
    private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1
    private static final int TIMESTAMP_DIFFERENCE_TO_AVOID_RESTART_LOOPS_IN_MILLIS = 2000;

    //Shared preferences
    private static final String SHARED_PREFERENCES_FILE = "custom_activity_on_crash";
    private static final String SHARED_PREFERENCES_FIELD_TIMESTAMP = "last_crash_timestamp";

    //Internal variables
    private static Application application;
    private static WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);
    private static boolean isInBackground = false;

    //Settable properties and their defaults
    private static boolean launchErrorActivityWhenInBackground = true;
    private static boolean showErrorDetails = true;
    private static boolean enableAppRestart = true;
    private static int defaultErrorActivityDrawableId = R.drawable.customactivityoncrash_error_image;
    private static Class<? extends Activity> errorActivityClass = null;
    private static Class<? extends Activity> restartActivityClass = null;
    private static EventListener eventListener = null;

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
                final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

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

                            if (hasCrashedInTheLastSeconds(application)) {
                                Log.e(TAG, "App already crashed in the last 2 seconds, not starting custom error activity because we could enter a restart loop. Are you sure that your app does not crash directly on init?", throwable);
                                if (oldHandler != null) {
                                    oldHandler.uncaughtException(thread, throwable);
                                    return;
                                }
                            } else {
                                setLastCrashTimestamp(application, new Date().getTime());

                                if (errorActivityClass == null) {
                                    errorActivityClass = guessErrorActivityClass(application);
                                }

                                if (isStackTraceLikelyConflictive(throwable, errorActivityClass)) {
                                    Log.e(TAG, "Your application class or your error activity have crashed, the custom activity will not be launched!");
                                    if (oldHandler != null) {
                                        oldHandler.uncaughtException(thread, throwable);
                                        return;
                                    }
                                } else if (launchErrorActivityWhenInBackground || !isInBackground) {

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

                                    if (enableAppRestart && restartActivityClass == null) {
                                        //We can set the restartActivityClass because the app will terminate right now,
                                        //and when relaunched, will be null again by default.
                                        restartActivityClass = guessRestartActivityClass(application);
                                    } else if (!enableAppRestart) {
                                        //In case someone sets the activity and then decides to not restart
                                        restartActivityClass = null;
                                    }

                                    intent.putExtra(EXTRA_STACK_TRACE, stackTraceString);
                                    intent.putExtra(EXTRA_RESTART_ACTIVITY_CLASS, restartActivityClass);
                                    intent.putExtra(EXTRA_SHOW_ERROR_DETAILS, showErrorDetails);
                                    intent.putExtra(EXTRA_EVENT_LISTENER, eventListener);
                                    intent.putExtra(EXTRA_IMAGE_DRAWABLE_ID, defaultErrorActivityDrawableId);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    if (eventListener != null) {
                                        eventListener.onLaunchErrorActivity();
                                    }
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
                            killCurrentProcess();
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
     * Given an Intent, returns if the error details button should be displayed.
     *
     * @param intent The Intent. Must not be null.
     * @return true if the button must be shown, false otherwise.
     */
    public static boolean isShowErrorDetailsFromIntent(Intent intent) {
        return intent.getBooleanExtra(CustomActivityOnCrash.EXTRA_SHOW_ERROR_DETAILS, true);
    }

    /**
     * Given an Intent, returns the drawable id of the image to show on the default error activity.
     *
     * @param intent The Intent. Must not be null.
     * @return The id of the drawable to use.
     */
    public static int getDefaultErrorActivityDrawableIdFromIntent(Intent intent) {
        return intent.getIntExtra(CustomActivityOnCrash.EXTRA_IMAGE_DRAWABLE_ID, R.drawable.customactivityoncrash_error_image);
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
     * Given an Intent, returns several error details including the stack trace extra from the intent.
     *
     * @param context A valid context. Must not be null.
     * @param intent  The Intent. Must not be null.
     * @return The full error details.
     */
    public static String getAllErrorDetailsFromIntent(Context context, Intent intent) {
        //I don't think that this needs localization because it's a development string...

        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        //Get build date
        String buildDateAsString = getBuildDateAsString(context, dateFormat);

        //Get app version
        String versionName = getVersionName(context);

        String errorDetails = "";

        errorDetails += "Build version: " + versionName + " \n";
        errorDetails += "Build date: " + buildDateAsString + " \n";
        errorDetails += "Current date: " + dateFormat.format(currentDate) + " \n";
        //Added a space between line feeds to fix #18.
        //Ideally, we should not use this method at all... It is only formatted this way because of coupling with the default error activity.
        //We should move it to a method that returns a bean, and let anyone format it as they wish.
        errorDetails += "Device: " + getDeviceModelName() + " \n \n";
        errorDetails += "Stack trace:  \n";
        errorDetails += getStackTraceFromIntent(intent);
        return errorDetails;
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
     * Given an Intent, returns the event listener extra from it.
     *
     * @param intent The Intent. Must not be null.
     * @return The event listener, or null if not provided.
     */
    @SuppressWarnings("unchecked")
    public static EventListener getEventListenerFromIntent(Intent intent) {
        Serializable serializedClass = intent.getSerializableExtra(CustomActivityOnCrash.EXTRA_EVENT_LISTENER);

        if (serializedClass != null && serializedClass instanceof EventListener) {
            return (EventListener) serializedClass;
        } else {
            return null;
        }
    }

    /**
     * Given an Intent, restarts the app and launches a startActivity to that intent.
     * The flags NEW_TASK and CLEAR_TASK are set if the Intent does not have them, to ensure
     * the app stack is fully cleared.
     * Must only be used from your error activity.
     *
     * @param activity The current error activity. Must not be null.
     * @param intent   The Intent. Must not be null.
     * @deprecated You should use restartApplicationWithIntent(activity, intent, eventListener).
     * If you don't want to use an eventListener, just pass null.
     */
    @Deprecated
    public static void restartApplicationWithIntent(Activity activity, Intent intent) {
        restartApplicationWithIntent(activity, intent, null);
    }

    /**
     * Given an Intent, restarts the app and launches a startActivity to that intent.
     * The flags NEW_TASK and CLEAR_TASK are set if the Intent does not have them, to ensure
     * the app stack is fully cleared.
     * If an event listener is provided, the restart app event is invoked.
     * Must only be used from your error activity.
     *
     * @param activity      The current error activity. Must not be null.
     * @param intent        The Intent. Must not be null.
     * @param eventListener The event listener as obtained by calling getEventListenerFromIntent.
     */
    public static void restartApplicationWithIntent(Activity activity, Intent intent, EventListener eventListener) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (eventListener != null) {
            eventListener.onRestartAppFromErrorActivity();
        }
        activity.finish();
        activity.startActivity(intent);
        killCurrentProcess();
    }

    /**
     * Closes the app. Must only be used from your error activity.
     *
     * @param activity The current error activity. Must not be null.
     * @deprecated You should use closeApplication(activity, eventListener).
     * If you don't want to use an eventListener, just pass null.
     */
    @Deprecated
    public static void closeApplication(Activity activity) {
        closeApplication(activity, null);
    }

    /**
     * Closes the app.
     * If an event listener is provided, the close app event is invoked.
     * Must only be used from your error activity.
     *
     * @param activity      The current error activity. Must not be null.
     * @param eventListener The event listener as obtained by calling getEventListenerFromIntent.
     */
    public static void closeApplication(Activity activity, EventListener eventListener) {
        if (eventListener != null) {
            eventListener.onCloseAppFromErrorActivity();
        }
        activity.finish();
        killCurrentProcess();
    }


    /// SETTERS AND GETTERS FOR THE CUSTOMIZABLE PROPERTIES

    /**
     * Returns if the error activity must be launched when the app is on background.
     *
     * @return true if it will be launched, false otherwise.
     */
    public static boolean isLaunchErrorActivityWhenInBackground() {
        return launchErrorActivityWhenInBackground;
    }

    /**
     * Defines if the error activity must be launched when the app is on background.
     * Set it to true if you want to launch the error activity when the app is in background,
     * false if you want it not to launch and crash silently.
     * This has no effect in API<14 and the error activity is always launched.
     * The default is true (the app will be brought to front when a crash occurs).
     */
    public static void setLaunchErrorActivityWhenInBackground(boolean launchErrorActivityWhenInBackground) {
        CustomActivityOnCrash.launchErrorActivityWhenInBackground = launchErrorActivityWhenInBackground;
    }

    /**
     * Returns if the error activity will show the error details button.
     *
     * @return true if it will be shown, false otherwise.
     */
    public static boolean isShowErrorDetails() {
        return showErrorDetails;
    }

    /**
     * Defines if the error activity must shown the error details button.
     * Set it to true if you want to show the full stack trace and device info,
     * false if you want it to be hidden.
     * The default is true.
     */
    public static void setShowErrorDetails(boolean showErrorDetails) {
        CustomActivityOnCrash.showErrorDetails = showErrorDetails;
    }

    /**
     * Returns the default error activity drawable identifier.
     *
     * @return the default error activity drawable identifier
     */
    public static int getDefaultErrorActivityDrawable() {
        return defaultErrorActivityDrawableId;
    }

    /**
     * Defines which drawable to use in the default error activity image.
     * Set this if you want to use an image other than the default one.
     * The default is R.drawable.customactivityoncrash_error_image (a cute upside-down bug).
     */
    public static void setDefaultErrorActivityDrawable(int defaultErrorActivityDrawableId) {
        CustomActivityOnCrash.defaultErrorActivityDrawableId = defaultErrorActivityDrawableId;
    }

    /**
     * Returns if the error activity should show a restart button.
     * Note that even if restart is enabled, a valid restart activity could not be found.
     * In that case, a close button will still be used.
     *
     * @return true if a restart button should be shown, false if a close button must be used.
     */
    public static boolean isEnableAppRestart() {
        return enableAppRestart;
    }

    /**
     * Defines if the error activity should show a restart button.
     * Set it to true if you want to show a restart button,
     * false if you want to show a close button.
     * Note that even if restart is enabled, a valid restart activity could not be found.
     * In that case, a close button will still be used.
     * The default is true.
     */
    public static void setEnableAppRestart(boolean enableAppRestart) {
        CustomActivityOnCrash.enableAppRestart = enableAppRestart;
    }

    /**
     * Returns the error activity class to launch when a crash occurs.
     *
     * @return The class, or null if not set.
     */
    public static Class<? extends Activity> getErrorActivityClass() {
        return errorActivityClass;
    }

    /**
     * Sets the error activity class to launch when a crash occurs.
     * If null,the default error activity will be used.
     */
    public static void setErrorActivityClass(Class<? extends Activity> errorActivityClass) {
        CustomActivityOnCrash.errorActivityClass = errorActivityClass;
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
     * Returns the event listener to be called when events occur, so they can be reported
     * by the app as, for example, Google Analytics events.
     *
     * @return The event listener, or null if not set.
     */
    public static EventListener getEventListener() {
        return eventListener;
    }

    /**
     * Sets an event listener to be called when events occur, so they can be reported
     * by the app as, for example, Google Analytics events.
     * If not set or set to null, no events will be reported.
     *
     * @param eventListener The event listener.
     * @throws IllegalArgumentException if the eventListener is an inner or anonymous class
     */
    public static void setEventListener(EventListener eventListener) {
        if (eventListener != null && eventListener.getClass().getEnclosingClass() != null && !Modifier.isStatic(eventListener.getClass().getModifiers())) {
            throw new IllegalArgumentException("The event listener cannot be an inner or anonymous class, because it will need to be serialized. Change it to a class of its own, or make it a static inner class.");
        } else {
            CustomActivityOnCrash.eventListener = eventListener;
        }
    }


    /// INTERNAL METHODS NOT TO BE USED BY THIRD PARTIES

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
     * INTERNAL method that returns the build date of the current APK as a string, or null if unable to determine it.
     *
     * @param context    A valid context. Must not be null.
     * @param dateFormat DateFormat to use to convert from Date to String
     * @return The formatted date, or "Unknown" if unable to determine it.
     */
    private static String getBuildDateAsString(Context context, DateFormat dateFormat) {
        String buildDate;
        try {
            long time;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                time = packageInfo.lastUpdateTime;
            } else {
                ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                ZipFile zf = new ZipFile(ai.sourceDir);
                ZipEntry ze = zf.getEntry("classes.dex");
                time = ze.getTime();
                zf.close();

                if (time == 312764400000L) {
                    // build date is Fri, Nov 30 1979 00:00:00
                    // gradle most likely didn't set the proper timestamp for files in the apk.
                    // see https://code.google.com/p/android/issues/detail?id=220039
                    return "Unknown";
                }
            }

            buildDate = dateFormat.format(new Date(time));
        } catch (Exception e) {
            buildDate = "Unknown";
        }
        return buildDate;
    }

    /**
     * INTERNAL method that returns the version name of the current app, or null if unable to determine it.
     *
     * @param context A valid context. Must not be null.
     * @return The version name, or "Unknown if unable to determine it.
     */
    private static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * INTERNAL method that returns the device model name with correct capitalization.
     * Taken from: http://stackoverflow.com/a/12707479/1254846
     *
     * @return The device model name (i.e., "LGE Nexus 5")
     */
    private static String getDeviceModelName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    /**
     * INTERNAL method that capitalizes the first character of a string
     *
     * @param s The string to capitalize
     * @return The capitalized string
     */
    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    /**
     * INTERNAL method used to guess which activity must be called from the error activity to restart the app.
     * It will first get activities from the AndroidManifest with intent filter <action android:name="cat.ereza.customactivityoncrash.RESTART" />,
     * if it cannot find them, then it will get the default launcher.
     * If there is no default launcher, this returns null.
     *
     * @param context A valid context. Must not be null.
     * @return The guessed restart activity class, or null if no suitable one is found
     */
    private static Class<? extends Activity> guessRestartActivityClass(Context context) {
        Class<? extends Activity> resolvedActivityClass;

        //If action is defined, use that
        resolvedActivityClass = getRestartActivityClassWithIntentFilter(context);

        //Else, get the default launcher activity
        if (resolvedActivityClass == null) {
            resolvedActivityClass = getLauncherActivity(context);
        }

        return resolvedActivityClass;
    }

    /**
     * INTERNAL method used to get the first activity with an intent-filter <action android:name="cat.ereza.customactivityoncrash.RESTART" />,
     * If there is no activity with that intent filter, this returns null.
     *
     * @param context A valid context. Must not be null.
     * @return A valid activity class, or null if no suitable one is found
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Activity> getRestartActivityClassWithIntentFilter(Context context) {
        Intent searchedIntent = new Intent().setAction(INTENT_ACTION_RESTART_ACTIVITY).setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(searchedIntent,
                PackageManager.GET_RESOLVED_FILTER);

        if (resolveInfos != null && resolveInfos.size() > 0) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            try {
                return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e(TAG, "Failed when resolving the restart activity class via intent filter, stack trace follows!", e);
            }
        }

        return null;
    }

    /**
     * INTERNAL method used to get the default launcher activity for the app.
     * If there is no launchable activity, this returns null.
     *
     * @param context A valid context. Must not be null.
     * @return A valid activity class, or null if no suitable one is found
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Activity> getLauncherActivity(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            try {
                return (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e(TAG, "Failed when resolving the restart activity class via getLaunchIntentForPackage, stack trace follows!", e);
            }
        }

        return null;
    }

    /**
     * INTERNAL method used to guess which error activity must be called when the app crashes.
     * It will first get activities from the AndroidManifest with intent filter <action android:name="cat.ereza.customactivityoncrash.ERROR" />,
     * if it cannot find them, then it will use the default error activity.
     *
     * @param context A valid context. Must not be null.
     * @return The guessed error activity class, or the default error activity if not found
     */
    private static Class<? extends Activity> guessErrorActivityClass(Context context) {
        Class<? extends Activity> resolvedActivityClass;

        //If action is defined, use that
        resolvedActivityClass = getErrorActivityClassWithIntentFilter(context);

        //Else, get the default launcher activity
        if (resolvedActivityClass == null) {
            resolvedActivityClass = DefaultErrorActivity.class;
        }

        return resolvedActivityClass;
    }

    /**
     * INTERNAL method used to get the first activity with an intent-filter <action android:name="cat.ereza.customactivityoncrash.ERROR" />,
     * If there is no activity with that intent filter, this returns null.
     *
     * @param context A valid context. Must not be null.
     * @return A valid activity class, or null if no suitable one is found
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Activity> getErrorActivityClassWithIntentFilter(Context context) {
        Intent searchedIntent = new Intent().setAction(INTENT_ACTION_ERROR_ACTIVITY).setPackage(context.getPackageName());
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(searchedIntent,
                PackageManager.GET_RESOLVED_FILTER);

        if (resolveInfos != null && resolveInfos.size() > 0) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            try {
                return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e(TAG, "Failed when resolving the error activity class via intent filter, stack trace follows!", e);
            }
        }

        return null;
    }

    /**
     * INTERNAL method that kills the current process.
     * It is used after restarting or killing the app.
     */
    private static void killCurrentProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    /**
     * INTERNAL method that stores the last crash timestamp
     *
     * @param timestamp The current timestamp.
     */
    private static void setLastCrashTimestamp(Context context, long timestamp) {
        context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).edit().putLong(SHARED_PREFERENCES_FIELD_TIMESTAMP, timestamp).commit();
    }

    /**
     * INTERNAL method that gets the last crash timestamp
     *
     * @return The last crash timestamp, or -1 if not set.
     */
    private static long getLastCrashTimestamp(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).getLong(SHARED_PREFERENCES_FIELD_TIMESTAMP, -1);
    }

    /**
     * INTERNAL method that tells if the app has crashed in the last seconds.
     * This is used to avoid restart loops.
     *
     * @return true if the app has crashed in the last seconds, false otherwise.
     */
    private static boolean hasCrashedInTheLastSeconds(Context context) {
        long lastTimestamp = getLastCrashTimestamp(context);
        long currentTimestamp = new Date().getTime();

        return (lastTimestamp <= currentTimestamp && currentTimestamp - lastTimestamp < TIMESTAMP_DIFFERENCE_TO_AVOID_RESTART_LOOPS_IN_MILLIS);
    }

    /**
     * Interface to be called when events occur, so they can be reported
     * by the app as, for example, Google Analytics events.
     */
    public interface EventListener extends Serializable {
        void onLaunchErrorActivity();

        void onRestartAppFromErrorActivity();

        void onCloseAppFromErrorActivity();
    }
}

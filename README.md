# Custom Activity On Crash library

This library allows launching a custom activity when the app crashes, instead of showing the hated "Unfortunately, X has stopped" dialog.

![](https://github.com/Ereza/CustomActivityOnCrash/blob/master/images/frontpage.png)

## How to use

### 1. Add a dependency

Add the following dependency to your build.gradle:
```gradle
dependencies {
    compile 'cat.ereza:customactivityoncrash:1.5.0'
}
```

You can also do it manually, by downloading the source code, importing the `library` folder as an Android Library Module, and adding a dependency on your project to that module.

### 2. Set up your application

On your application class, use this snippet:
```java
    @Override
    public void onCreate() {
        super.onCreate();

        //Install CustomActivityOnCrash
        CustomActivityOnCrash.install(this);

        //Now initialize your error handlers as normal
        //i.e., ACRA.init(this);
        //or Fabric.with(this, new Crashlytics())
    }
```

**WARNING!** If you already have ACRA, Crashlytics or any similar library in your app, it will still work as normal, but the CustomActivityOnCrash initialization **MUST** be done first, or the original reporting tool will stop working.

### 3. Test it

Make the app crash by using something like this in your code:
```java
throw new RuntimeException("Boom!");
```

The error activity should show up, instead of the system dialog.

### Optional: Customization

**Custom behavior**

You can call the following methods at any time to customize how the library works, although usually you will call them before calling `install(context)`:

```java
CustomActivityOnCrash.setLaunchErrorActivityWhenInBackground(boolean);
```
This method defines if the error activity should be launched when the app crashes while on background.
By default, this is true. On API<14, it's always true since there is no way to detect if the app is in foreground.
If you set it to `false`, a crash while in background won't launch the error activity nor the system dialog, so it will be a silent crash.
The default is `true`.

```java
CustomActivityOnCrash.setShowErrorDetails(boolean);
```
This method defines if the error activity must show a button with error details.
If you set it to `false`, the button on the default error activity will disappear, thus disabling the user from seeing the stack trace.
The default is `true`.

```java
CustomActivityOnCrash.setDefaultErrorActivityDrawable(int);
```
This method allows changing the default upside-down bug image with an image of your choice.
You may pass a resource id for a drawable or a mipmap.
The default is `R.drawable.customactivityoncrash_error_image`.

```java
CustomActivityOnCrash.setEnableAppRestart(boolean);
```
This method defines if the error activity must show a "Restart app" button or a "Close app" button.
If you set it to `false`, the button on the default error activity will close the app instead of restarting.
Warning! If you set it to `true`, there is the possibility of it still displaying the "Close app" button,
if no restart activity is specified or found!
The default is `true`.

```java
CustomActivityOnCrash.setEventListener(EventListener);
```
This method allows you to specify an event listener in order to get notified when the library shows the error activity, restarts or closes the app.
The EventListener you provide can not be an anonymous or non-static inner class, because it needs to be serialized by the library. The library will throw an exception if you try to set an invalid class.
If you set it to null, no event listener will be invoked.
The default is null.

```java
CustomActivityOnCrash.setRestartActivityClass(Class<? extends Activity>);
```
This method sets the activity that must be launched by the error activity when the user presses the button to restart the app.
If you don't set it (or set it to null), the library will use the first activity on your manifest that has an intent-filter with action
`cat.ereza.customactivityoncrash.RESTART`, and if there is none, the default launchable activity on your app.
If no launchable activity can be found and you didn't specify any, the "restart app" button will become a "close app" button,
even if `setEnableAppRestart` is set to `true`.

As noted, you can also use the following intent-filter to specify the restart activity:
```xml
<intent-filter>
    <!-- ... -->
    <action android:name="cat.ereza.customactivityoncrash.RESTART" />
</intent-filter>
```

```java
CustomActivityOnCrash.setErrorActivityClass(Class<? extends Activity>);
```
This method allows you to set a custom error activity to be launched, instead of the default one.
Use it if you need further customization that is not just strings, colors or themes (see below).
If you don't set it (or set it to null), the library will use the first activity on your manifest that has an intent-filter with action
`cat.ereza.customactivityoncrash.ERROR`, and if there is none, a default error activity from the library.
If you use this, the activity **must** be declared in your `AndroidManifest.xml`, with `process` set to `:error_activity`.

Example:
```xml
<activity
    android:name="cat.ereza.sample.customactivityoncrash.activity.CustomErrorActivity"
    android:label="@string/error_title"
    android:process=":error_activity" />
```

As noted, you can also use the following intent-filter to specify the error activity:
```xml
<intent-filter>
    <!-- ... -->
    <action android:name="cat.ereza.customactivityoncrash.ERROR" />
</intent-filter>
```

**Customization of the default activity**

You can override several resources to customize the default activity:

*Theme:*

You can override the default error activity theme by defining a theme in your app with the following id: `CustomActivityOnCrashTheme`

*Image:*

By default, an image of a bug is displayed. You can change it to any image by creating a `customactivityoncrash_error_image` drawable on all density buckets (mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi).
You can also use the provided `CustomActivityOnCrash.setDefaultErrorActivityDrawable(int)` method.

*Strings:*

You can provide new strings and translations for the default error activity strings by overriding the following strings:
```xml
    <string name="customactivityoncrash_error_activity_error_occurred_explanation">An unexpected error occurred.\nSorry for the inconvenience.</string>
    <string name="customactivityoncrash_error_activity_unknown_exception">Unknown exception</string>
    <string name="customactivityoncrash_error_activity_restart_app">Restart app</string>
    <string name="customactivityoncrash_error_activity_close_app">Close app</string>
    <string name="customactivityoncrash_error_activity_error_details">Error details</string>
    <string name="customactivityoncrash_error_activity_error_details_title">Error details</string>
    <string name="customactivityoncrash_error_activity_error_details_close">Close</string>
    <string name="customactivityoncrash_error_activity_error_details_copy">Copy to clipboard</string>
    <string name="customactivityoncrash_error_activity_error_details_copied">Copied to clipboard</string>
    <string name="customactivityoncrash_error_activity_error_details_clipboard_label">Error information</string>
```

*There is a `sample` project module with examples of these overrides. If in doubt, check the code in that module.*

**Completely custom error activity**

If you choose to create your own completely custom error activity, you can use these methods:

```java
CustomActivityOnCrash.getStackTraceFromIntent(getIntent());
```
Returns the stack trace that caused the error as a string.

```java
CustomActivityOnCrash.getAllErrorDetailsFromIntent(getIntent());
```
Returns several error details including the stack trace that caused the error, as a string. This is used in the default error activity error details dialog.

```java
CustomActivityOnCrash.getRestartActivityClassFromIntent(getIntent());
```
Returns the class of the activity you have to launch to restart the app, or `null` if not set.

```java
CustomActivityOnCrash.getEventListenerFromIntent(getIntent());
```
Returns the event listener that you must pass to `restartApplicationWithIntent(activity, intent, eventListener)` or `closeApplication(activity, eventListener)`.

```java
CustomActivityOnCrash.restartApplicationWithIntent(activity, intent, eventListener);
```
Kills the current process and restarts the app again with an `startActivity()` to the passed intent.
You **MUST** call this to restart the app, or you will end up having several `Application` class instances and experience multiprocess issues in API<17.

```java
CustomActivityOnCrash.closeApplication(activity, eventListener);
```
Closes the app and kills the current process.
You **MUST** call this to close the app, or you will end up having several Application class instances and experience multiprocess issues in API<17.

*The `sample` project module includes an example of a custom error activity. If in doubt, check the code in that module.*

## Using Proguard?

No need to add special rules, the library should work even with obfuscation.

## Inner workings

This library relies on the `Thread.setDefaultUncaughtExceptionHandler` method.
When an exception is caught by the library's `UncaughtExceptionHandler` it does the following:

1. Captures the stack trace that caused the crash
2. Launches a new intent to the error activity passing the stacktrace as an extra.
3. Kills the current process.

The inner workings are based on [ACRA](https://github.com/ACRA/acra)'s dialog reporting mode with some minor tweaks. Look at the code if you need more detail about how it works.

## Incompatibilities

* CustomActivityOnCrash will not work in these cases:
    * With any custom `UncaughtExceptionHandler` set after initializing the library, that does not call back to the original handler.
    * With ACRA enabled and reporting mode set to `TOAST` or `DIALOG`.
* If you use a custom `UncaughtExceptionHandler`, it will not be called if you initialize it before the library initialization (so, Crashlytics or ACRA initialization must be done **after** CustomActivityOnCrash initialization).
* On some rare cases on devices with API<14, the app may enter a restart loop when a crash occurs. Therefore, using it on API<14 is not recommended.
* If your app initialization or error activity crash, there is a possibility of entering an infinite restart loop (this is checked by the library for the most common cases, but could happen in rarer cases).
* The library has not been tested with multidex enabled. It uses Class.forName() to load classes, so maybe that could cause some problem in API<21. If you test it with such configuration, please provide feedback!
* The library has not been tested with multiprocess apps. If you test it with such configuration, please provide feedback too!

## Disclaimers

* This will not avoid ANRs from happening.
* This will not catch native errors.
* There is no guarantee that this will work on every device.
* This library will not make you toast for breakfast :)

## Contributing & license

Any contribution in order to make this library better will be welcome!

The library is licensed under the [Apache License 2.0](https://github.com/Ereza/CustomActivityOnCrash/blob/master/LICENSE).

The bug image used in the default error activity is licensed under CC-BY by Riffschievous: https://www.sketchport.com/drawing/6119265933459456/lady-bug
If you use the image in your app, don't forget to mention that!
# Custom Activity On Crash library

This library allows launching a custom activity when the app crashes, instead of showing the hated "Unfortunately, X has stopped" dialog.

![](https://github.com/Ereza/CustomActivityOnCrash/blob/master/images/frontpage.png)

## How to use

### One-step install

Add the following dependency to your build.gradle:
```gradle
dependencies {
    compile 'cat.ereza:customactivityoncrash:2.2.0'
}
```

...and you are done!

Of course, you can combine this library with any other crash handler such as Crashlytics, ACRA or Firebase, just set them up as you would normally.

### Try it

Force an app crash by throwing an uncaught exception, using something like this in your code:
```java
throw new RuntimeException("Boom!");
```

### Advanced setup

You can customize the behavior of this library in several ways by setting its configuration at any moment.
However, it's recommended to do it on your `Application` class so it becomes available as soon as possible.

Add a snippet like this to your `Application` class:
```java
@Override
public void onCreate() {
    super.onCreate();

    CaocConfig.Builder.create()
        .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
        .enabled(false) //default: true
        .showErrorDetails(false) //default: true
        .showRestartButton(false) //default: true
        .logErrorOnRestart(false) //default: true
        .trackActivities(true) //default: false
        .minTimeBetweenCrashesMs(2000) //default: 3000
        .errorDrawable(R.drawable.ic_custom_drawable) //default: bug image
        .restartActivity(YourCustomActivity.class) //default: null (your app's launch activity)
        .errorActivity(YourCustomErrorActivity.class) //default: null (default error activity)
        .eventListener(new YourCustomEventListener()) //default: null
        .apply();
}
```

## Customization options

### Custom behavior

**Here is a more detailed explanation of each option that can be set using `CaocConfig.Builder`:**

```java
launchWhenInBackground(int);
```
> This method defines if the error activity should be launched when the app crashes while on background.
> There are three modes:
> - `CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM`: launch the error activity even if the app is in background.
> - `CaocConfig.BACKGROUND_MODE_CRASH`: launch the default system error when the app is in background.
> - `CaocConfig.BACKGROUND_MODE_SILENT`: crash silently when the app is in background.
>
> The default is `CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM`.

```java
enabled(boolean);
```
> Defines if CustomActivityOnCrash crash interception mechanism is enabled.
> Set it to `true` if you want CustomActivityOnCrash to intercept crashes,
> `false` if you want them to be treated as if the library was not installed.
> This can be used to enable or disable the library depending on flavors or buildTypes.
> The default is `true`.

```java
showErrorDetails(boolean);
```
> This method defines if the error activity must show a button with error details.
> If you set it to `false`, the button on the default error activity will disappear, thus disabling the user from seeing the stack trace.
> The default is `true`.

```java
trackActivities(boolean);
```
> This method defines if the library must track the activities the user visits and their lifecycle calls.
> This is displayed on the default error activity as part of the error details.
> The default is `false`.

```java
showRestartButton(boolean);
```
> This method defines if the error activity must show a "Restart app" button or a "Close app" button.
> If you set it to `false`, the button on the default error activity will close the app instead of restarting.
> If you set it to `true` and your app has no launch activity, it will still display a "Close app" button!
> The default is `true`.

```java
logErrorOnRestart(boolean);
```
> This controls if the stack trace must be relogged when the custom error activity is launched.
> This functionality exists because the Android Studio default Logcat view only shows the output for the
> current process. This makes it easier to see the stack trace of the crash. You can disable it if you
> don't want an extra log.
> The default is `true`.

```java
minTimeBetweenCrashesMs(boolean);
```
> Defines the time that must pass between app crashes to determine that we are not in a crash loop.
> If a crash has occurred less that this time ago, the error activity will not be launched and the system
> crash screen will be invoked.
> The default is `3000`.

```java
errorDrawable(Integer);
```
> This method allows changing the default upside-down bug image with an image of your choice.
> You can pass a resource id for a drawable or a mipmap.
> The default is `null` (the bug image is used).

```java
restartActivity(Class<? extends Activity>);
```
> This method sets the activity that must be launched by the error activity when the user presses the button to restart the app.
> If you don't set it (or set it to null), the library will use the first activity on your manifest that has an intent-filter with action
> `cat.ereza.customactivityoncrash.RESTART`, and if there is none, the default launchable activity on your app.
> If no launchable activity can be found and you didn't specify any, the "restart app" button will become a "close app" button,
> even if `showRestartButton` is set to `true`.
>
> As noted, you can also use the following intent-filter to specify the restart activity:
> ```xml
> <intent-filter>
>     <!-- ... -->
>     <action android:name="cat.ereza.customactivityoncrash.RESTART" />
> </intent-filter>
> ```

```java
errorActivity(Class<? extends Activity>);
```
> This method allows you to set a custom error activity to be launched, instead of the default one.
> Use it if you need further customization that is not just strings, colors or themes (see below).
> If you don't set it (or set it to null), the library will use the first activity on your manifest that has an intent-filter with action
> `cat.ereza.customactivityoncrash.ERROR`, and if there is none, a default error activity from the library.
> If you use this, the activity **must** be declared in your `AndroidManifest.xml`, with `process` set to `:error_activity`.
>
> Example:
> ```xml
> <activity
>     android:name="cat.ereza.customactivityoncrash.sample.CustomErrorActivity"
>     android:label="@string/error_title"
>     android:process=":error_activity" />
> ```
>
> As noted, you can also use the following intent-filter to specify the error activity:
> ```xml
> <intent-filter>
>     <!-- ... -->
>     <action android:name="cat.ereza.customactivityoncrash.ERROR" />
> </intent-filter>
> ```

```java
eventListener(EventListener);
```
> This method allows you to specify an event listener in order to get notified when the library shows the error activity, restarts or closes the app.
> The EventListener you provide can not be an anonymous or non-static inner class, because it needs to be serialized by the library. The library will throw an exception if you try to set an invalid class.
> If you set it to `null`, no event listener will be invoked.
> The default is `null`.

### Customization of the default activity

You can override several resources to customize the default activity:

**Theme:**

The activity uses your application theme by default. It must be a child of `Theme.AppCompat` or a default one will be used.
If you want to specify a specific theme only for the error activity, you can do so by redeclaring it on your manifest like this:

```xml
<activity
    android:name="cat.ereza.customactivityoncrash.activity.DefaultErrorActivity"
    android:theme="@style/YourThemeHere"
    android:process=":error_activity" />
```

**Image:**

By default, an image of a bug is displayed. You can change it to any image by using the provided `errorDrawable(int)` method.
You can also do it the old way and create a `customactivityoncrash_error_image` drawable on all density buckets (mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi).

**Strings:**

You can provide new strings and translations for the default error activity strings by overriding the following strings:
```xml
<string name="customactivityoncrash_error_activity_error_occurred_explanation">An unexpected error occurred.\nSorry for the inconvenience.</string>
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

### Completely custom error activity

If you choose to create your own completely custom error activity, you can use these methods:

```java
CustomActivityOnCrash.getStackTraceFromIntent(getIntent());
```
> Returns the stack trace that caused the error as a string.

```java
CustomActivityOnCrash.getAllErrorDetailsFromIntent(getIntent());
```
> Returns several error details including the stack trace that caused the error, as a string. This is used in the default error activity error details dialog.

```java
CustomActivityOnCrash.getConfigFromIntent(getIntent());
```
> Returns the config of the library when the crash happened. Used to call some methods.

```java
CustomActivityOnCrash.restartApplication(activity, config);
```
> Kills the current process and restarts the app again with a `startActivity()` to the passed intent.
> You **MUST** call this to restart the app, or you will end up having several `Application` class instances and experience multiprocess issues.

```java
CustomActivityOnCrash.restartApplicationWithIntent(activity, intent, config);
```
> The same as `CustomActivityOnCrash.restartApplication`, but allows you to specify a custom intent.

```java
CustomActivityOnCrash.closeApplication(activity, eventListener);
```
> Closes the app and kills the current process.
> You **MUST** call this to close the app, or you will end up having several Application class instances and experience multiprocess issues.

**The `sample` project module includes an example of a custom error activity. If in doubt, check the code in that module.**

## Using Proguard?

No need to add special rules, the library should work even with obfuscation.

## Inner workings

This library relies on the `Thread.setDefaultUncaughtExceptionHandler` method.
When an exception is caught by the library's `UncaughtExceptionHandler` it does the following:

1. Captures the stack trace that caused the crash
2. Launches a new intent to the error activity in a new process passing the crash info as an extra.
3. Kills the current process.

The inner workings are based on [ACRA](https://github.com/ACRA/acra)'s dialog reporting mode with some minor tweaks. Look at the code if you need more detail about how it works.

## Incompatibilities

* CustomActivityOnCrash will not work in these cases:
    * With any custom `UncaughtExceptionHandler` set after initializing the library, that does not call back to the original handler.
    * With ACRA enabled and reporting mode set to `TOAST` or `DIALOG`.
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

The bug image used in the default error activity is licensed under CC-BY by Riff: https://www.sketchport.com/drawing/6119265933459456/lady-bug
If you use the image in your app, don't forget to mention that!
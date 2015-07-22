# Custom Activity On Crash library

This library allows launching a custom activity when the app crashes, instead of showing the hated "Unfortunately, X has stopped" dialog.

## How to use

### 1. Add a dependency

Add the following dependency to your build.gradle:
```gradle
dependencies{
    compile 'cat.ereza:customactivityoncrash:1.0.1'
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
        //or Crashlytics.start(this);
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
CustomActivityOnCrash.setLaunchActivityEvenIfInBackground(false);
```
This method defines if the error activity should be launched even if the app crashed while on background.
By default, this is true. On API<14, it's always true since there is no way to detect if the app is in foreground.
If you set it to false, a crash while in background won't launch the error activity nor the system dialog, so it will be a silent crash.

```java
CustomActivityOnCrash.setRestartActivityClass(MainActivity.class);
```
This method sets the activity that must be launched by the error activity when the user presses the button to restart the app.
You should pass the initial activity of your application.
If you don't set it, or set it to null, instead of restarting, the button will close the app.

```java
CustomActivityOnCrash.setErrorActivityClass(ErrorActivity.class);
```
This method allows you to set a custom error activity to be launched, instead of the default one.
Use it if you need further customization that is not just strings, colors or themes (see below).

**Customization of the default activity**

You can override several resources to customize the default activity:

*Theme:*

You can override the default error activity theme by defining a theme in your app with the following id: `CustomActivityOnCrashTheme`

*Strings:*

You can provide new strings and translations for the default error activity strings by overriding the following strings:
```xml
    <string name="customactivityoncrash_error_activity_title">An error occurred!</string>
    <string name="customactivityoncrash_error_activity_error_occurred">An error occurred. We\'re deeply sorry.</string>
    <string name="customactivityoncrash_error_activity_error_details">Error details:</string>
    <string name="customactivityoncrash_error_activity_unknown_exception">Unknown exception</string>
    <string name="customactivityoncrash_error_activity_restart_app">Restart app</string>
    <string name="customactivityoncrash_error_activity_close_app">Close app</string>
```

**There is a `sample` project module with examples of these overrides. If in doubt, check the code in the `sample` module.**

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
    * With ACRA enabled and reporting mode set to `TOAST` or `DIALOG`.
    * With any custom `UncaughtExceptionHandler` set after initializing the library, that does not call back to the original handler.
* Your `UncaughtExceptionHandler` will not be called if you initialize it before the library initialization (so, Crashlytics or ACRA initialization must be done **after** CustomActivityOnCrash initialization).
* On some rare cases on devices with API<14, the app may enter a restart loop when a crash occurs. Therefore, using it on API<14 is not recommended.
* If your app initialization or error activity crash, there is a possibility of entering an infinite restart loop (this is checked by the library for the most common cases, but could happen in rarer cases).

## Disclaimers

* This will not avoid ANRs from happening.
* This will not catch native errors.
* There is no guarantee that this will work on every device.
* This library will not make you toast for breakfast :)

## Contributing & license

Any contribution in order to make this library better will be welcome!

The library is licensed under the [Apache License 2.0](https://github.com/Ereza/CustomActivityOnCrash/blob/master/LICENSE).
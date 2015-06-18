# Custom Activity On Crash library

This library allows launching a custom activity when the app crashes, instead of showing the hated "App has stopped unexpectedly" dialog.

## How to use

### Add a dependency

**Option 1: Gradle dependency:**

(Still pending submission to Maven Central)

**Option 2: Manually:**

1. Download the source code and import the `library` folder as an Android Library Module in Android Studio.
2. Add a dependency on your project to that module.

### Make it work

1. Add a custom activity that will be your error activity. Specify in it this additional parameter: `process=":error_report"`.
2. On your application class, use this snippet:
```java
    @Override
    public void onCreate() {
        super.onCreate();

        //Install CustomActivityOnCrash
        CustomActivityOnCrash.init(this, ErrorActivity.class, true);

        //Now initialize your error handlers as normal
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);
    }
```
3. (Optional) On your error activity, use `getIntent().getStringExtra(CustomActivityOnCrash.EXTRA_STACK_TRACE)` to retrieve the stack trace and display it if you wish.

**WARNING!** As you see, if you already have ACRA, Crashlytics or any similar library bundled into your app, you can make them coexist, but the CustomActivityOnCrash initialization **must** be done first.

**If in doubt, check the `sample` project module.**

### Test it

Make the app crash by using something like this on your code:
```java
throw new RuntimeException("Boom!");
```

Your custom error activity should show up, instead of the system dialog.

## Using Proguard?

No need to add special rules, the library should work even with obfuscation.

## Inner workings

This library relies on the `Thread.setDefaultUncaughtExceptionHandler` method.
When an exception is caught by the library's `UncaughtExceptionHandler` it does the following:

1. Captures the stack trace that caused the crash
2. Launches a new intent to your custom error activity passing the stacktrace as an extra.
3. Kills the current process.

The inner workings are based on [ACRA](https://github.com/ACRA/acra)'s dialog reporting mode with some minor tweaks. Look at the code if you need more detail about how it works.

## Incompatibilities

* The CustomActivityOnCrash handler will not be called in these cases:
    * With ACRA enabled and reporting mode set to `TOAST` or `DIALOG`.
    * With any other custom `UncaughtExceptionHandler` set after initializing the library, that does not call back to the original handler.
* Your `UncaughtExceptionHandler` will not be called if you initialize it before the library initialization (so, Crashlytics or ACRA initialization must be done **after** CustomActivityOnCrash initialization).
* On some rare cases on devices with API<14, the app may enter a restart loop.

## Disclaimers

* This will not avoid ANRs from happening.
* This will not catch native errors.
* There is no guarantee that this will work on every device.
* If your app initialization or error activity crash, there is a possibility of entering an infinite restart loop (this is checked by the library for the most common cases, but could happen in rarer cases)
* This library will make you toast for breakfast :)
* Of course, no refunds accepted! ;)

## Contributing & license

Any contribution in order to make this library better will be welcome!

The library is licensed under the [Apache License 2.0](https://github.com/Ereza/CustomActivityOnCrash/blob/master/LICENSE).
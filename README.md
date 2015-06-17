# Custom Activity On Crash library

This library allows launching a custom activity when the app crashes, instead of showing the hated "App has stopped unexpectedly" dialog.

## How to use

If you already have ACRA, Crashlytics or any similar library bundled into your app, you can make them coexist, but the CustomActivityOnCrash initialization must be done first. The exceptions will get reported just like they usually do.

## Inner workings

This library relies on the `Thread.setDefaultUncaughtExceptionHandler` method. When an exception is caught by the `UncaughtExceptionHandler`, it captures the stack trace and launches a new intent to your custom error activity, then kills the current process. Look at the code and you will see how it works (it's just a single class).

The inner workings are based on ACRA's dialog reporting mode with some minor tweaks.

## Using Proguard?

Meh

## Disclaimers

I have tested this on the following devices:

* HTC One M8, Android 4.4
* Samsung Galaxy S4 mini (GT-I9195), Android 4.4
* Samsung Galaxy A3 (SM-A300FU), Android 4.4
* Nexus 5, Android 5.0
* Nexus 5, Android 5.1

It works fine on those devices, but I haven't applied it to any production code, so be aware that errors may arise.

* There is no guarantee that this will work on your device
* There is no guarantee that this will work on Android <4.0.3, but maybe it does (I haven't tried it, I currently develop for API 15+)
* This will most likely not work if your app deals with any native libraries.
* Make sure that your error-handling activities and application class initialization don't crash ever! If they do, you'll end up in an endless loop (although you could try to filter errors from those classes when handling the Throwable).
* This does not avoid ANRs from happening, they will be handled as normal.
* There is no guarantee that this sample app will make you toast for breakfast :)
* Of course, no refunds accepted! ;)

## Contributing

I have limited time but any contribution in order to make this sample better will be welcome!
CustomActivityOnCrash
=====================

Sample Android project to demonstrate how to launch a custom activity when your app crashes, instead of showing the hated "App has stopped unexpectedly" dialog.

How to use
=====================

**This is just a sample.** Take a look at it and implement it in your project as needed, making any tweaks you want in order to fit your needs.

This sample relies on the `Thread.setDefaultUncaughtExceptionHandler` method and launches a new intent to your error activity, then kills the current process. Look at the code and you will see how it works (it's all in the application class).

The sample passes the stack trace to the `ErrorActivity`, but you can customize it as you wish. Keep in mind that the `Intent` limits are around 1MB and you may get a `TransactionTooLargeException`. Reduce the passed data to fit those limits.

If you already have ACRA, Crashlytics or any similar library bundled into your app, it is likely that you can make them coexist. The exceptions will get reported just like they usually do.

Disclaimers
=====================

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

Contributing
=====================

I have limited time but any contribution in order to make this sample better will be welcome!
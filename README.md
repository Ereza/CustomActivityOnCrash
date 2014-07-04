CustomActivityOnCrash
=====================

Sample project to demonstrate how to launch a custom activity when your app crashes, instead of showing the hated "App has stopped unexpectedly" dialog.

How to use
=====================

This is just a sample. Take a look at it and implement it in your project as needed, making any tweaks you want in order to fit your needs.

This sample relies on the Thread.setDefaultUncaughtExceptionHandler method and AlarmManager to start a new PendingIntent to your Activity, then kills the current process. Look at the code and you will see how it works (it's all in the Application class).

The sample passes the stacktrace to the ErrorActivity, but you can customize it as you wish. Keep in mind that the Intent limits are around 1MB and you may get a TransactionTooLargeException. Reduce the passed data to fit those limits.

If you already have Acra, Crashlytics or any similar library bundled into your app, it is likely that you can make them coexist. You'll just have to report the exception as a handled exception, or do it after the user takes action on the ErrorActivity.

Disclaimers
=====================

I have tested this on devices with Android 4.0.3, 4.4.4 and L preview. It works fine on those devices, but I haven't applied it to any production code, so be aware that errors may arise.

* There is no guarantee that this will work on your device
* There is no guarantee that this will work on Android <4.0.3, but maybe it does (I haven't tried it, I currently develop for API 15+)
* This will most likely not work if your app deals with any native libraries.
* Make sure that your error-handling activities don't crash ever! If they do, you'll end up in an endless loop (although you could try to filter errors from those activities when handling the Throwable).
* This does not avoid ANRs from happening, they will be handled as normal.
* There is no guarantee that this sample app will make you toast for breakfast :)
* Of course, no refunds accepted! ;)

Contributing
=====================

I have limited time but any contribution in order to make this sample better will be welcome!


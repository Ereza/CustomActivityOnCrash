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

package cat.ereza.sample.customactivityoncrash;

import android.app.Application;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class SampleCrashingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //You can uncomment any of the lines below, and test the results.
        //If you comment out the install one, the library will not work and the default Android crash dialog will be shown.

        //This makes the library not launch the error activity when the app crashes while it is in background.
//        CustomActivityOnCrash.setLaunchErrorActivityWhenInBackground(false);

        //This sets the restart activity.
        //If you set this, this will be used. However, you can also set it with an intent-filter:
        //  <action android:name="cat.ereza.customactivityoncrash.RESTART" />
        //If none are set, the default launch activity will be used.
//        CustomActivityOnCrash.setRestartActivityClass(MainActivity.class);

        //This hides the "error details" button in the error activity, thus hiding the stack trace
//        CustomActivityOnCrash.setShowErrorDetails(false);

        //This avoids the app from using the "Restart app" button and displaying a "Close app" button directly.
        //Be careful, even with restart app enabled, the Close app can still be displayed if no suitable
        //restart activity is found!
//        CustomActivityOnCrash.setEnableAppRestart(false);

        //This sets a custom error activity class instead of the default one.
        //If you set this, this will be used. However, you can also set it with an intent-filter:
        //  <action android:name="cat.ereza.customactivityoncrash.ERROR" />
        //If none are set, the default launch activity will be used.
        //Comment it (and disable the intent filter) to see the customization effects on the default error activity.
        //Uncomment to use the custom error activity
//        CustomActivityOnCrash.setErrorActivityClass(CustomErrorActivity.class);

        //This enables CustomActivityOnCrash
        CustomActivityOnCrash.install(this);

        //In a normal app, you would now initialize your error handler as normal.
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);
    }
}

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
import cat.ereza.sample.customactivityoncrash.activity.MainActivity;

public class SampleCrashingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //You can uncomment any of the lines below, and test the results.
        //If you comment out the install one, the library will not work and the default Android crash dialog will be shown.

        //This makes the library not launch the error activity when the app crashes while it is in background.
        CustomActivityOnCrash.setLaunchActivityEvenIfInBackground(false);

        //This sets the restart activity. If you don't do this, the "Restart app" button will change to "Close app".
        //You can define class with Intent Filter cat.ereza.customactivityoncrash.ERROR or it will get default launcher app
        //otherwise you can set class as follwing.
//        CustomActivityOnCrash.setRestartActivityClass(MainActivity.class);

        //This hides the "error details" button, thus hiding the stack trace
//        CustomActivityOnCrash.setShowErrorDetails(false);

        //This sets a custom error activity class instead of the default one.
        //Comment it to see the customization effects on the default error activity.
        //Uncomment to use the custom error activity
//        CustomActivityOnCrash.setErrorActivityClass(CustomErrorActivity.class);

        //This enables CustomActivityOnCrash
        CustomActivityOnCrash.install(this);

        //In a normal app, you would now initialize your error handler as normal.
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);
    }
}

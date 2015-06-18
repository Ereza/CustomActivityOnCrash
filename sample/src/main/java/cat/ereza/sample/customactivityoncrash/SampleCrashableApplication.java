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

public class SampleCrashableApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //Install CustomActivityOnCrash
        CustomActivityOnCrash.init(this, ErrorActivity.class, false);

        //Now initialize your error handlers as normal, they will most likely keep a reference to the original exception handler
        //i.e., ACRA.init(this);
        //or Crashlytics.start(this);
    }
}

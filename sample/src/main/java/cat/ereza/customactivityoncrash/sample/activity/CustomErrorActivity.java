/*
 * Copyright 2014-2017 Eduard Ereza Martínez
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

package cat.ereza.customactivityoncrash.sample.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import cat.ereza.customactivityoncrash.sample.R;

public class CustomErrorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_error);

        //**IMPORTANT**
        //The custom error activity in this sample is uglier than the default one and just
        //for demonstration purposes, please don't copy it to your project!
        //We recommend taking the original library's DefaultErrorActivity as a basis.
        //Of course, you are free to implement it as you wish in your application.

        //These four methods are available for you to use:
        //CustomActivityOnCrash.getStackTraceFromIntent(getIntent()): gets the stack trace as a string
        //CustomActivityOnCrash.getActivityLogFromIntent(getIntent()): gets the activity log as a string
        //CustomActivityOnCrash.getAllErrorDetailsFromIntent(context, getIntent()): returns all error details including stacktrace as a string
        //CustomActivityOnCrash.getConfigFromIntent(getIntent()): returns the config of the library when the error happened

        //Now, treat here the error as you wish. If you allow the user to restart or close the app,
        //don't forget to call the appropriate methods.
        //Otherwise, if you don't finish the activity, you will get the CustomErrorActivity on the activity stack and it will be visible again under some circumstances.
        //Also, you will get multiprocess problems in API<17.

        TextView errorDetailsText = findViewById(R.id.error_details);
        errorDetailsText.setText(CustomActivityOnCrash.getStackTraceFromIntent(getIntent()));

        Button restartButton = findViewById(R.id.restart_button);

        final CaocConfig config = CustomActivityOnCrash.getConfigFromIntent(getIntent());

        if (config == null) {
            //This should never happen - Just finish the activity to avoid a recursive crash.
            finish();
            return;
        }

        if (config.isShowRestartButton() && config.getRestartActivityClass() != null) {
            restartButton.setText(R.string.restart_app);
            restartButton.setOnClickListener(v -> CustomActivityOnCrash.restartApplication(CustomErrorActivity.this, config));
        } else {
            restartButton.setOnClickListener(v -> CustomActivityOnCrash.closeApplication(CustomErrorActivity.this, config));
        }
    }
}

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

package cat.ereza.sample.customactivityoncrash.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.sample.customactivityoncrash.R;

public class CustomErrorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_error);

        //**IMPORTANT**
        //The custom error activity in this sample is uglier than the default one and just
        //for demonstration purposes, please don't copy it to your project!
        //We recommend taking the original library's DefaultErrorActivity as a basis.
        //Of course, you are free to implement it as you wish in your application.

        //These three methods are available for you to use:
        //CustomActivityOnCrash.getStackTraceFromIntent(getIntent()): gets the stack trace as a string
        //CustomActivityOnCrash.getAllErrorDetailsFromIntent(context, getIntent()): returns all error details including stacktrace as a string
        //CustomActivityOnCrash.getRestartActivityClassFromIntent(getIntent()): returns the class of the restart activity to launch, or null if none

        //Now, treat here the error as you wish. If you allow the user to restart the app,
        //don't forget to finish the activity, otherwise you will get the CustomErrorActivity
        //on the activity stack and it will be visible again under some circumstances.

        TextView errorDetailsText = (TextView) findViewById(R.id.error_details);
        errorDetailsText.setText(CustomActivityOnCrash.getStackTraceFromIntent(getIntent()));

        Button restartButton = (Button) findViewById(R.id.restart_button);

        final Class<? extends Activity> restartActivityClass = CustomActivityOnCrash.getRestartActivityClassFromIntent(getIntent());

        if (restartActivityClass != null) {
            restartButton.setText(R.string.restart_app);
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CustomErrorActivity.this, restartActivityClass);
                    CustomActivityOnCrash.restartApplicationWithIntent(CustomErrorActivity.this, intent);
                }
            });
        } else {
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomActivityOnCrash.closeApplication(CustomErrorActivity.this);
                }
            });
        }
    }
}

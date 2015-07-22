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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class ErrorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.error_title);
        setContentView(R.layout.activity_error);

        //Treat here the error as you wish. If you allow the user to restart the app,
        //don't forget to finish the activity, otherwise you will get the ErrorActivity
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
                    Intent intent = new Intent(ErrorActivity.this, restartActivityClass);
                    finish();
                    startActivity(intent);
                }
            });
        } else {
            restartButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}

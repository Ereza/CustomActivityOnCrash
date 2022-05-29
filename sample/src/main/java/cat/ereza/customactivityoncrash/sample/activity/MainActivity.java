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

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import cat.ereza.customactivityoncrash.sample.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button crashMainThreadButton = findViewById(R.id.button_crash_main_thread);
        Button crashBgThreadButton = findViewById(R.id.button_crash_bg_thread);
        Button crashWithDelayButton = findViewById(R.id.button_crash_with_delay);

        crashMainThreadButton.setOnClickListener(view -> {
            throw new RuntimeException("I'm a cool exception and I crashed the main thread!");
        });

        //For demo purposes we don't care about leaks nor deprecation
        //noinspection StaticFieldLeak,deprecation
        crashBgThreadButton.setOnClickListener(view -> new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                throw new RuntimeException("I'm also cool, and I crashed the background thread!");
            }
        }.execute());

        //For demo purposes we don't care about leaks nor deprecation
        //noinspection StaticFieldLeak,deprecation
        crashWithDelayButton.setOnClickListener(view -> new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    //meh
                }
                throw new RuntimeException("I am a not so cool exception, and I am delayed, so you can check if the app crashes when in background!");
            }
        }.execute());
    }
}

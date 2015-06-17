package cat.ereza.customactivityoncrash.sample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import cat.ereza.example.customactivityoncrash.R;


public class MainActivity extends Activity {

    Button crashMainThreadButton;
    Button crashBgThreadButton;
    Button crashWithDelayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(SampleCrashableApplication.TAG, "Entering MainActivity");
        setContentView(R.layout.activity_main);

        crashMainThreadButton = (Button) findViewById(R.id.button_crash_main_thread);
        crashBgThreadButton = (Button) findViewById(R.id.button_crash_bg_thread);
        crashWithDelayButton = (Button) findViewById(R.id.button_crash_with_delay);

        crashMainThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(SampleCrashableApplication.TAG, "Crashing main thread");
                throw new RuntimeException("I'm a cool exception and I crashed the main thread!");
            }
        });

        crashBgThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Log.d(SampleCrashableApplication.TAG, "Crashing bg thread");
                        throw new RuntimeException("I'm also cool, and I crashed the background thread!");
                    }
                }.execute();
            }
        });

        crashWithDelayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            //meh
                        }
                        Log.d(SampleCrashableApplication.TAG, "Crashing bg thread after the delay");
                        throw new RuntimeException("I AM ERROR!");
                    }
                }.execute();
            }
        });
    }
}

package cat.ereza.example.customactivityoncrash;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity {

    Button crashMainThreadButton;
    Button crashBgThreadButton;
    Button crashSeveralTimesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(CrashableApplication.TAG, "Entering MainActivity");
        setContentView(R.layout.activity_main);

        crashMainThreadButton = (Button) findViewById(R.id.button_crash_main_thread);
        crashBgThreadButton = (Button) findViewById(R.id.button_crash_bg_thread);
        crashSeveralTimesButton = (Button) findViewById(R.id.button_crash_several_times);

        crashMainThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(CrashableApplication.TAG, "Crashing main thread");
                throw new RuntimeException("I'm a cool exception and I crashed the main thread!");
            }
        });

        crashBgThreadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Log.d(CrashableApplication.TAG, "Crashing bg thread");
                        throw new RuntimeException("I'm also cool, and I crashed the background thread!");
                    }
                }.execute();
            }
        });

        crashSeveralTimesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            //meh
                        }
                        Log.d(CrashableApplication.TAG, "Crashing bg thread 1");
                        throw new RuntimeException("I AM ERROR 1");
                    }
                }.execute();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            //meh
                        }
                        Log.d(CrashableApplication.TAG, "Crashing bg thread 2");
                        throw new RuntimeException("I AM ERROR 2");
                    }
                }.execute();
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            Thread.sleep(6000);
                        } catch (InterruptedException e) {
                            //meh
                        }
                        Log.d(CrashableApplication.TAG, "Crashing bg thread 3");
                        throw new RuntimeException("I AM ERROR 3");
                    }
                }.execute();
            }
        });
    }
}

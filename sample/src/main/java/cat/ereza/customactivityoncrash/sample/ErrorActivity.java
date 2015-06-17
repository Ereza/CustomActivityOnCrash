package cat.ereza.customactivityoncrash.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cat.ereza.example.customactivityoncrash.R;

public class ErrorActivity extends Activity {

    public final static String EXTRA_EXCEPTION = "EXCEPTION";

    TextView errorDetailsText;
    Button restartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(SampleCrashableApplication.TAG, "Entering ErrorActivity");

        setTitle(R.string.error_title);
        setContentView(R.layout.activity_error);

        //Treat here the error as you wish. If you allow the user to restart the app,
        //don't forget to finish the activity, otherwise you will get the ErrorActivity
        //on the activity stack and it will be visible again under some circumstances.

        errorDetailsText = (TextView) findViewById(R.id.error_details);
        restartButton = (Button) findViewById(R.id.restart_button);

        errorDetailsText.setText(getIntent().getStringExtra(EXTRA_EXCEPTION));
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ErrorActivity.this, MainActivity.class);
                finish();
                Log.d(SampleCrashableApplication.TAG, "Exiting ErrorActivity");
                startActivity(intent);
            }
        });
    }
}

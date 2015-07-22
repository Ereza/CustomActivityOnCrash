package cat.ereza.customactivityoncrash.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toolbar;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.R;

/**
 * Created by a557114 on 22/07/2015.
 */
public class DefaultErrorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customactivityoncrash_default_error_activity);

        checkToolbar();

        //Treat here the error as you wish. If you allow the user to restart the app,
        //don't forget to finish the activity, otherwise you will get the ErrorActivity
        //on the activity stack and it will be visible again under some circumstances.

        TextView errorDetailsText = (TextView) findViewById(R.id.error_details);

        errorDetailsText.setText(getIntent().getStringExtra(CustomActivityOnCrash.EXTRA_STACK_TRACE));
    }

    private void checkToolbar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setActionBar(toolbar);
        }
    }
}

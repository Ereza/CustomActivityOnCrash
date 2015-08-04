package cat.ereza.customactivityoncrash.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

/**
 * Created by zhy on 15/8/4.
 */
public class ClearStack extends Activity
{


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent().getParcelableExtra(CustomActivityOnCrash.KEY_CURRENT_INTENT);
        startActivity(intent);
        finish();
        Runtime.getRuntime().exit(0);
    }
}

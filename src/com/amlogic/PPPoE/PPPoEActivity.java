package com.amlogic.PPPoE;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class PPPoEActivity extends Activity {
    private final String TAG = "PPPoEActivity";
    private PppoeConfigDialog mPppoeConfigDialog;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.d(TAG, "Create PppoeConfigDialog");
        mPppoeConfigDialog = new PppoeConfigDialog(this);

        Log.d(TAG, "Show PppoeConfigDialog");
        mPppoeConfigDialog.show();
    }
}

package com.amlogic.PPPoE;

import com.amlogic.PPPoE.R;

import android.app.Activity;
import android.os.Bundle;

public class PPPoEActivity extends Activity {
    private PppoeConfigDialog mPppoeConfigDialog;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	mPppoeConfigDialog = new PppoeConfigDialog(this);
    	mPppoeConfigDialog.show();
    }
}
package com.droidlogic.PPPoE;

import com.droidlogic.pppoe.PppoeManager;
import com.droidlogic.pppoe.IPppoeManager;
import com.droidlogic.pppoe.PppoeStateTracker;
import com.droidlogic.pppoe.PppoeDevInfo;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.view.Gravity;
import android.provider.Settings;
import android.os.ServiceManager;


public class PPPoEActivity extends Activity {
    private final String TAG = "PPPoEActivity";
    private PppoeConfigDialog mPppoeConfigDialog;
    private PppoeDevInfo mPppoeInfo;
    private PppoeManager mPppoeManager;
    public static final int MSG_START_DIAL = 0xabcd0000;
    public static final int MSG_MANDATORY_DIAL = 0xabcd0010;
    public static final int MSG_CONNECT_TIMEOUT = 0xabcd0020;
    public static final int MSG_DISCONNECT_TIMEOUT = 0xabcd0040;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Log.d(TAG, "Create PppoeConfigDialog");
        mPppoeConfigDialog = new PppoeConfigDialog(this);

        ConnectivityManager cm = (ConnectivityManager)this.getSystemService
                                        ( Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null) {
           Log.d(TAG, info.toString());
        }

        IBinder b = ServiceManager.getService("pppoe");
        IPppoeManager PppoeService = IPppoeManager.Stub.asInterface(b);
        mPppoeManager = new PppoeManager(PppoeService, this);

        mPppoeInfo = mPppoeManager.getSavedPppoeConfig();
        if (mPppoeInfo != null) {
            Log.d(TAG, "IP: " + mPppoeInfo.getIpAddress());
            Log.d(TAG, "MASK: " + mPppoeInfo.getNetMask());
            Log.d(TAG, "GW: " + mPppoeInfo.getRouteAddr());
            Log.d(TAG, "DNS: " + mPppoeInfo.getDnsAddr());
        }

        Log.d(TAG, "Show PppoeConfigDialog");
        mPppoeConfigDialog.show();
    }
}

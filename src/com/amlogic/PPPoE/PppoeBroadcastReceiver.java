package com.amlogic.PPPoE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.net.ethernet.EthernetStateTracker;
import android.net.ethernet.EthernetManager;
import android.content.SharedPreferences;
import com.amlogic.pppoe.PppoeOperation;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class PppoeBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "PppoeBroadcastReceiver";
    private static final String ACTION_BOOT_COMPLETED =
        "android.intent.action.BOOT_COMPLETED";

    private static final String ACTION_NETWORK_STAT_CHANGED=
        "com.amlogic.mediacenter.NETWORK_STATE_CHANGED";

    private Handler mHandler;
    private boolean mAutoDialFlag = false;
    private String mInterfaceSelected = null;
    private String mUserName = null;
    private String mPassword = null;
    private PppoeOperation operation = null;

	private String getNetworkInterfaceSelected(Context context)
	{
		SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
		if(sharedata != null && sharedata.getAll().size() > 0)
		{
			return sharedata.getString(PppoeConfigDialog.INFO_NETWORK_INTERFACE_SELECTED, null); 
		}
        return null;
	}


	private boolean getAutoDialFlag(Context context)
	{
		SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
		if(sharedata != null && sharedata.getAll().size() > 0)
		{
			return sharedata.getBoolean(PppoeConfigDialog.INFO_AUTO_DIAL_FLAG, false); 
		}
        return false;
	}

	private String getUserName(Context context)
	{
		SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
		if(sharedata != null && sharedata.getAll().size() > 0)
		{
			return sharedata.getString(PppoeConfigDialog.INFO_USERNAME, null); 
		}
        return null;
	}

	private String getPassword(Context context)
	{
		SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
		if(sharedata != null && sharedata.getAll().size() > 0)
		{
			return sharedata.getString(PppoeConfigDialog.INFO_PASSWORD, null); 
		}
        return null;
	}


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG , ">>>>>onReceive :" +intent.getAction());
        mInterfaceSelected = getNetworkInterfaceSelected(context);
        Log.d(TAG , "InterfaceSelected = " + mInterfaceSelected);
        mAutoDialFlag = getAutoDialFlag(context);
        Log.d(TAG , "AutoDialFlag = " + mAutoDialFlag);

        mUserName = getUserName(context);
        Log.d(TAG , "UserName = " + mUserName);
        mPassword = getPassword(context);
        Log.d(TAG , "Password = " + mPassword);
        
        if (null == mInterfaceSelected
            || !mAutoDialFlag
            || null == mUserName
            || null == mPassword)
            return;
        
        if (EthernetManager.ETH_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            if (!mInterfaceSelected.startsWith("eth"))
                return;

			int event = intent.getIntExtra(EthernetManager.EXTRA_ETH_STATE, -1);
            if (event == EthernetStateTracker.EVENT_HW_DISCONNECTED ) {
                Log.d(TAG , "EVENT_HW_DISCONNECTED");
                //context.stopService(new Intent(context, PppoeService.class));
            }
            else if (event == EthernetStateTracker.EVENT_HW_PHYCONNECTED ) {
                Log.d(TAG , "EVENT_HW_PHYCONNECTED");
            	operation = new PppoeOperation();
                operation.disconnect();

                mHandler = new PppoeHandler();
                mHandler.sendEmptyMessageDelayed(0, 5000);

                //context.startService(new Intent(context, PppoeService.class));
            }
            else if (event == EthernetStateTracker.EVENT_HW_CONNECTED )
                Log.d(TAG , "EVENT_HW_CONNECTED");
            else
                Log.d(TAG , "EVENT=" + event);
        }
    }


    private class PppoeHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Log.d(TAG, "handleMessage");

			operation.connect(mInterfaceSelected, mUserName, mPassword);
        }
    }
}


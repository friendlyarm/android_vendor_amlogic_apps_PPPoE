package com.droidlogic.PPPoE;

import java.util.Timer;
import java.util.TimerTask;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.content.SharedPreferences;
import com.amlogic.pppoe.PppoeOperation;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemProperties;

public class PppoeBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "PppoeBroadcastReceiver";
    private static final String ACTION_BOOT_COMPLETED =
        "android.intent.action.BOOT_COMPLETED";
    public static final String ETH_STATE_CHANGED_ACTION =
        "android.net.ethernet.ETH_STATE_CHANGED";
	public static final String EXTRA_ETH_STATE = "eth_state";
	public static final int EVENT_HW_PHYCONNECTED = 5;
	public static final int EVENT_HW_DISCONNECTED                   = 4;
    public static final int EVENT_HW_CONNECTED                      = 3;
    private Handler mHandler = null;
    private boolean mAutoDialFlag = false;
    private String mInterfaceSelected = null;
    private String mUserName = null;
    private String mPassword = null;
    private PppoeOperation operation = null;
    private static boolean mFirstAutoDialDone = false;
    private Timer mMandatoryDialTimer = null;

    private String getNetworkInterfaceSelected(Context context)
    {
        SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
        if (sharedata != null && sharedata.getAll().size() > 0)
        {
            return sharedata.getString(PppoeConfigDialog.INFO_NETWORK_INTERFACE_SELECTED, null);
        }
        return null;
    }


    private boolean getAutoDialFlag(Context context)
    {
        SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
        if (sharedata != null && sharedata.getAll().size() > 0)
        {
            return sharedata.getBoolean(PppoeConfigDialog.INFO_AUTO_DIAL_FLAG, false);
        }
        return false;
    }

    private String getUserName(Context context)
    {
        SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
        if (sharedata != null && sharedata.getAll().size() > 0)
        {
            return sharedata.getString(PppoeConfigDialog.INFO_USERNAME, null);
        }
        return null;
    }

    private String getPassword(Context context)
    {
        SharedPreferences sharedata = context.getSharedPreferences("inputdata", 0);
        if (sharedata != null && sharedata.getAll().size() > 0)
        {
            return sharedata.getString(PppoeConfigDialog.INFO_PASSWORD, null);
        }
        return null;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        mInterfaceSelected = getNetworkInterfaceSelected(context);
        mAutoDialFlag = getAutoDialFlag(context);

        mUserName = getUserName(context);
        mPassword = getPassword(context);
        if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                context.startService(new Intent(context,
                     MyPppoeService.class));
                mFirstAutoDialDone = true;
        }

        if (null == mInterfaceSelected
            || !mAutoDialFlag
            || null == mUserName
            || null == mPassword) {
            mFirstAutoDialDone = false;
            return;

        }

        if (mHandler == null) {
            mHandler = new PppoeHandler();
        }
        if (operation == null) {
            operation = new PppoeOperation();
        }
        Log.d(TAG , ">>>>>onReceive :" +intent.getAction());
        if ("com.droidlogic.linkchange".equals(action) || mFirstAutoDialDone) {
            mFirstAutoDialDone = false;
            if (!mInterfaceSelected.startsWith("eth") )
                return;
            int event = intent.getIntExtra(EXTRA_ETH_STATE, -1);
            if (event == EVENT_HW_PHYCONNECTED) {
                Log.d(TAG, "EVENT_HW_PHYCONNECTED");
                if (mMandatoryDialTimer != null) {
                    mMandatoryDialTimer.cancel();
                    mMandatoryDialTimer = null;
                }

                Log.d(TAG, "EVENT_HW_PHYCONNECTED trigger AUTO DIAL");

                set_pppoe_running_flag();
                operation.terminate();
                operation.disconnect();
                mHandler.sendEmptyMessageDelayed(PPPoEActivity.MSG_START_DIAL, 30000);
            }
            else {
                if (event == EVENT_HW_DISCONNECTED ) {
                    Log.d(TAG, "EVENT_HW_DISCONNECTED");
                }
                else if (event == EVENT_HW_CONNECTED )
                    Log.d(TAG, "EVENT_HW_CONNECTED");
                else
                    Log.d(TAG, "EVENT=" + event);

                if (event != EVENT_HW_DISCONNECTED) {
                    Log.d(TAG, "EVENT_HW_PHYCONNECTED LOST");
                    mMandatoryDialTimer = new Timer();
                    TimerTask check_task = new TimerTask() {
                        public void run()
                        {
                            Message message = new Message();
                            message.what = PPPoEActivity.MSG_MANDATORY_DIAL;
                            Log.d(TAG, "Send MSG_MANDATORY_DIAL");
                            mHandler.sendMessage(message);
                        }
                    };

                    //Timeout after 5 seconds
                    mMandatoryDialTimer.schedule(check_task, 5000);
                }
            }
        }
    }
    public static boolean isEthConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        State mEthState = connManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).getState();
        if (State.CONNECTED == mEthState) {
            return true;
        }else{
            return false;
        }
    }

    void set_pppoe_running_flag()
    {
        SystemProperties.set(PppoeConfigDialog.ethernet_dhcp_repeat_flag, "disabled");

        SystemProperties.set(PppoeConfigDialog.pppoe_running_flag, "100");
        String propVal = SystemProperties.get(PppoeConfigDialog.pppoe_running_flag);
        int n = 0;
        if (propVal.length() != 0) {
            try {
                n = Integer.parseInt(propVal);
                Log.d(TAG, "set_pppoe_running_flag as " + n);
            } catch (NumberFormatException e) {}
        } else {
            Log.d(TAG, "failed to set_pppoe_running_flag");
        }

        return;
    }

    private class PppoeHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
            case PPPoEActivity.MSG_MANDATORY_DIAL:
                Log.d(TAG, "handleMessage: MSG_MANDATORY_DIAL");
                set_pppoe_running_flag();
                operation.terminate();

                operation.disconnect();

                mHandler.sendEmptyMessageDelayed(PPPoEActivity.MSG_START_DIAL, 2000);
                break;

            case PPPoEActivity.MSG_START_DIAL:
                Log.d(TAG, "handleMessage: MSG_START_DIAL");
                operation.connect(mInterfaceSelected, mUserName, mPassword);
                break;

            default:
                Log.d(TAG, "handleMessage: " + msg.what);
                break;
            }


        }
    }
}


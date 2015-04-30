package com.droidlogic.PPPoE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import com.amlogic.pppoe.PppoeOperation;
import com.android.server.net.BaseNetworkObserver;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;


public class MyPppoeService extends Service
{
    private static final String TAG = "MyPppoeService";
    private NotificationManager mNM;
    private Handler mHandler;
    private PppoeOperation operation = null;
    private InterfaceObserver mInterfaceObserver;
    private INetworkManagementService mNMService;

    @Override
    public void onCreate() {
        Log.d(TAG, ">>>>>>onCreate");
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mHandler = new DMRHandler();
        mInterfaceObserver = new InterfaceObserver();
        try {
            mNMService.registerObserver(mInterfaceObserver);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not register InterfaceObserver " + e);
        }

        /* start check after 5s */
        mHandler.sendEmptyMessageDelayed(0, 5000);

        IntentFilter f = new IntentFilter();

        f.addAction(Intent.ACTION_SHUTDOWN);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mShutdownReceiver, new IntentFilter(f));
    }

    @Override
    public void onDestroy() {
        //unregisteReceiver
        unregisterReceiver(mShutdownReceiver);
        // Cancel the persistent notification.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void updateInterfaceState(String iface, boolean up) {
        Intent intent = new Intent("com.droidlogic.linkchange");

        if (!iface.contains("eth0")) {
            return;
        }
        if (up) {
            this.sendBroadcast(intent);
        }
    }

    private class InterfaceObserver extends BaseNetworkObserver {
        @Override
        public void interfaceLinkStateChanged(String iface, boolean up) {
            updateInterfaceState(iface, up);
        }

    }

    private class DMRHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Log.d(TAG, "handleMessage");
            /* check per 10s */
            mHandler.sendEmptyMessageDelayed(0, 1000000);
        }
    }


private BroadcastReceiver mShutdownReceiver =
    new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG , "onReceive :" +intent.getAction());

        if ((Intent.ACTION_SCREEN_OFF).equals(intent.getAction())) {
            operation = new PppoeOperation();
            operation.disconnect();
        }
        if ((Intent.ACTION_SCREEN_ON).equals(intent.getAction())) {
            updateInterfaceState("eth0", true);
        }

    }
};

}


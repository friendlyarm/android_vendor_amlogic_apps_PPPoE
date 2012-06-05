package com.amlogic.PPPoE;

import java.util.Timer;
import java.util.TimerTask;
import android.os.Message;
import android.os.Handler;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton; 
import android.widget.EditText;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;  
import android.widget.ArrayAdapter;  
import android.widget.Spinner;  
import android.widget.TextView;

import android.net.ethernet.EthernetManager;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.ArrayList;
import java.net.SocketException;

import android.net.pppoe.PppoeManager;
import android.net.pppoe.PppoeStateTracker;

import com.amlogic.pppoe.PppoeOperation;

public class PppoeConfigDialog extends AlertDialog implements DialogInterface.OnClickListener
{
    private static final String PPPOE_DIAL_RESULT_ACTION =
            "PppoeConfigDialog.PPPOE_DIAL_RESULT";

    private static final int PPPOE_STATE_DISCONNECTED = 1;
    private static final int PPPOE_STATE_CONNETCING = 2;
    private static final int PPPOE_STATE_CONNECT_FAILED = 4;
	private static final int PPPOE_STATE_CONNECTED = 8;

	private static final int MSG_CONNECT_TIMEOUT = 0xabcd0000;
	private static final int MSG_DISCONNECT_TIMEOUT = 0xabcd0010;
	private static final int MSG_DISCONNECT_BEFORE_CONNECT_TIMEOUT = 0xabcd0020;
    
    private static final String EXTRA_NAME_STATUS = "status";
    private static final String EXTRA_NAME_ERR_CODE = "err_code";
	
	private final String TAG = "PppoeCfgDlg";
	private View mView;
	private EditText mPppoeName;
    private EditText mPppoePasswd;
    private String user_name = null;
    private String user_passwd = null;
    private ProgressDialog waitDialog = null;
    private PppoeOperation operation = null;
    Context context = null;
    private AlertDialog alertDia = null;
    private PppoeReceiver pppoeReceiver = null;
    
    private CheckBox mCbAutoDial;

	Timer connect_timer = null;   
	Timer disconnect_timer = null;   
	Timer disconnect_before_connect_timer = null;   

	private final String pppoe_running_flag = "net.pppoe.running";

	private TextView mNetworkInterfaces;
	private Spinner spinner;
	private ArrayAdapter<String> adapter;
	private EthernetManager mEthManager;
	private ArrayList<String> network_if_list;
	private String network_if_selected;
	private String tmp_name, tmp_passwd;
	private boolean is_pppoe_running()
	{
		String propVal = SystemProperties.get(pppoe_running_flag);
		int n = 0;
		if (propVal.length() != 0) {
			try {
				n = Integer.parseInt(propVal);
			} catch (NumberFormatException e) {}
		} else {
			Log.d(TAG, "net.pppoe.running not FOUND");
		}

		return (n != 0); 
	}



	private void set_pppoe_running_flag()
	{
		SystemProperties.set(pppoe_running_flag, "100");
		String propVal = SystemProperties.get(pppoe_running_flag);
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


	private void clear_pppoe_running_flag()
	{
		SystemProperties.set(pppoe_running_flag, "0");
		String propVal = SystemProperties.get(pppoe_running_flag);
		int n = 0;
		if (propVal.length() != 0) {
			try {
				n = Integer.parseInt(propVal);
				Log.d(TAG, "clear_pppoe_running_flag as " + n);
			} catch (NumberFormatException e) {}
		} else {
			Log.d(TAG, "failed to clear_pppoe_running_flag");
		}

		return;
	}


	public PppoeConfigDialog(Context context)
	{
		super(context);
		this.context = context;
		operation = new PppoeOperation();
		buildDialog(context);
		waitDialog = new ProgressDialog(this.context); 
	}

	class SpinnerSelectedListener implements OnItemSelectedListener{  
	    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,  
                long arg3) {
			network_if_selected = network_if_list.get(arg2);
			Log.d(TAG, "interface selected: " + network_if_selected);
		}  
  
		public void onNothingSelected(AdapterView<?> arg0) {  
		}  
	}

	private void buildDialog(Context context)
	{
		Log.d(TAG, "buildDialog");
		setTitle(R.string.pppoe_config_title);
		this.setView(mView = getLayoutInflater().inflate(R.layout.pppoe_configure, null));
		mPppoeName = (EditText)mView.findViewById(R.id.pppoe_name_edit);
		mPppoePasswd = (EditText)mView.findViewById(R.id.pppoe_passwd_edit);
		mCbAutoDial = (CheckBox)mView.findViewById(R.id.auto_dial_checkbox);
		mCbAutoDial.setVisibility(View.VISIBLE);
        mCbAutoDial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){ 
                    @Override 
                    public void onCheckedChanged(CompoundButton buttonView, 
                            boolean isChecked) { 
                        // TODO Auto-generated method stub 
                        if(isChecked){ 
                			Log.d(TAG, "Selected");
                        }else{ 
                			Log.d(TAG, "NO Selected");
                        } 
                    } 
                }); 

		mPppoeName.setEnabled(true);
		mPppoePasswd.setEnabled(true);
		this.setInverseBackgroundForced(true);

		network_if_list=new ArrayList<String>();

		try {
			for(Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces(); list.hasMoreElements();)
			{
				NetworkInterface netIf = list.nextElement();
				//Log.d(TAG, "network_interface: " + netIf.getDisplayName());
				if (!netIf.isLoopback() && !netIf.isPointToPoint())                
					network_if_list.add(netIf.getDisplayName());                
			}
		} catch (SocketException e) {
			return;
		}

		if (network_if_list.size() == 0 )
			network_if_list.add("eth0");                

		network_if_selected = network_if_list.get(0);

        /*
		boolean eth_found = false;
		for(int i = 0; i < network_if_list.size(); i++) {
			if (network_if_list.get(i).startsWith("eth"))
				eth_found = true;
		}

		if (!eth_found) {
				network_if_list.add("eth0");                
		}
        */
        
		/*
		mEthManager = (EthernetManager) context.getSystemService(Context.ETH_SERVICE);
		String[] network_if_list = mEthManager.getDeviceNameList();
		*/

		mNetworkInterfaces = (TextView) mView.findViewById(R.id.network_interface_list_text);
		spinner = (Spinner) mView.findViewById(R.id.network_inteface_list_spinner); 
		adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_spinner_item,network_if_list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new SpinnerSelectedListener());  
		spinner.setVisibility(View.VISIBLE);

		if(connectStatus() != PppoeOperation.PPP_STATUS_CONNECTED)
		{
			this.setButton(BUTTON_POSITIVE, context.getText(R.string.pppoe_dial), this);
		}
		else {
			Log.d(TAG, "connectStatus is CONNECTED");

			//hide network interfaces
			mNetworkInterfaces.setVisibility(View.GONE);
			spinner.setVisibility(View.GONE);


			//hide Username
			mView.findViewById(R.id.user_pppoe_text).setVisibility(View.GONE);
			mPppoeName.setVisibility(View.GONE);

			//hide Password
			mView.findViewById(R.id.passwd_pppoe_text).setVisibility(View.GONE);
			mPppoePasswd.setVisibility(View.GONE);
			this.setButton(BUTTON_POSITIVE, context.getText(R.string.pppoe_disconnect), this);

			/*
			if (!is_pppoe_running()) {
				showAlertDialog(context.getResources().getString(R.string.pppoe_ppp_interface_occupied));
				return;
			}
			*/
		}
		
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);

        mCbAutoDial.setChecked(isAutoDial());
	}
	


	@Override
	public void show()
	{
        Log.d(TAG, "show");
		getInfoData();
		if(user_name != null 
		  && user_passwd != null
	      && user_name.equals("")== false)
        {
        	mPppoeName.setText(user_name);
        	mPppoePasswd.setText(user_passwd);
        }
        else
        {
        	mPppoeName.setText("");
        	mPppoePasswd.setText("");
        }
		
		super.show();
	}
	
	void showWaitDialog(int id)
	{
		waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); 
		waitDialog.setTitle(""); 
		waitDialog.setMessage(this.context.getResources().getString(id));
		waitDialog.setIcon(null); 
		waitDialog.setButton(android.content.DialogInterface.BUTTON_POSITIVE,this.context.getResources().getString(R.string.menu_cancel),clickListener); 
		waitDialog.setIndeterminate(false); 
		waitDialog.setCancelable(true); 
		waitDialog.show();
          
		Button button = waitDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
		button.setFocusable(true);
		button.setFocusableInTouchMode(true);
		button.requestFocus();
		button.requestFocusFromTouch();
	}

	private void saveInfoData()
	{
		SharedPreferences.Editor sharedata = this.context.getSharedPreferences("inputdata", 0).edit();
		sharedata.clear();
		sharedata.putString("name", mPppoeName.getText().toString());
		sharedata.putString("passwd", mPppoePasswd.getText().toString()); 
		sharedata.putString("network_if_selected", network_if_selected); 
		sharedata.commit();  
	}

	private void getInfoData()
	{
		SharedPreferences sharedata = this.context.getSharedPreferences("inputdata", 0);
		if(sharedata != null && sharedata.getAll().size() > 0)
		{
			user_name = sharedata.getString("name", null);   
			user_passwd = sharedata.getString("passwd", null); 
			network_if_selected = sharedata.getString("network_if_selected", null); 
		}
		else
		{
			user_name = null;
			user_passwd = null;
		}
	}
	
	private int connectStatus()
	{
		if (null == network_if_selected){
			Log.d(TAG, "network_if_selected is null");
			return PppoeOperation.PPP_STATUS_DISCONNECTED;
		}
        
		return operation.status(network_if_selected);
	}
	
	private boolean isAutoDial() {
		return false;//operation.isAutoDial();
	}
	
	private void setAutoDial() {
		boolean ad = mCbAutoDial.isChecked();
//		operation.setAutoDial(ad);
	}

	private void showAlertDialog(final String msg)
	{
        Log.d(TAG, "showAlertDialog");
		AlertDialog.Builder ab = new AlertDialog.Builder(context); 
		alertDia = ab.create();  
		alertDia.setTitle(" "); 
		alertDia.setMessage(msg);
		alertDia.setIcon(null); 
		
		alertDia.setButton(android.content.DialogInterface.BUTTON_POSITIVE,this.context.getResources().getString(R.string.amlogic_ok),AlertClickListener); 

		alertDia.setCancelable(true); 
		alertDia.setInverseBackgroundForced(true);
		alertDia.show();

		Button button = alertDia.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
		button.setFocusable(true);
		button.setFocusableInTouchMode(true);
		button.requestFocus();
		button.requestFocusFromTouch();
	}
	
	OnClickListener AlertClickListener = new OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which) 
		{
			switch (which) {
	        case android.content.DialogInterface.BUTTON_POSITIVE:
	        	{
	        		alertDia.cancel();
	        		clearSelf();
	        	}
	            break;
	        case android.content.DialogInterface.BUTTON_NEGATIVE:
	            break;
	        default:
	            break;
			}
			
		}
	};
	
	private void handleStartDial()
	{

		pppoeReceiver = new PppoeReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PppoeManager.PPPOE_STATE_CHANGED_ACTION);
		context.registerReceiver(pppoeReceiver, filter);
		
		tmp_name = mPppoeName.getText().toString();
		tmp_passwd = mPppoePasswd.getText().toString();
		if(tmp_name != null && tmp_passwd != null)
		{
			saveInfoData();
			
			final Handler handler = new Handler() {
				public void handleMessage(Message msg) {
	                    switch (msg.what) {
	                    case MSG_CONNECT_TIMEOUT:
							waitDialog.cancel();
							showAlertDialog(context.getResources().getString(R.string.pppoe_connect_failed));
							break;

	                    case MSG_DISCONNECT_BEFORE_CONNECT_TIMEOUT:
							operation.connect(network_if_selected, tmp_name, tmp_passwd);
							break;

	                    }

	                    super.handleMessage(msg);
	            }
	        };

			connect_timer = new Timer();   
			TimerTask check_task = new TimerTask()
			{   
				public void run() 
				{   
					 Message message = new Message();
	                 message.what = MSG_CONNECT_TIMEOUT;
	                 handler.sendMessage(message);
				}   
			};


			disconnect_before_connect_timer = new Timer();   
			TimerTask disconnect_before_connect_check_task = new TimerTask()
			{   
				public void run() 
				{   
					 Message message = new Message();
	                 message.what = MSG_DISCONNECT_BEFORE_CONNECT_TIMEOUT;
	                 handler.sendMessage(message);
				}   
			};

			disconnect_before_connect_timer.schedule(disconnect_before_connect_check_task, 5000);

			connect_timer.schedule(check_task, 35000);

			showWaitDialog(R.string.pppoe_dial_waiting_msg);
			set_pppoe_running_flag();
			operation.disconnect();
		}
	}
	
	private void handleStopDial()
	{
		boolean result = operation.disconnect();
		
		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
                    switch (msg.what) {
                    case MSG_DISCONNECT_TIMEOUT:
						waitDialog.cancel();
						showAlertDialog(context.getResources().getString(R.string.pppoe_disconnect_ok));
						clear_pppoe_running_flag();
						break;
                    }
					
                    super.handleMessage(msg);
            }
        };

		disconnect_timer = new Timer();   
		TimerTask check_task = new TimerTask()
		{   
			public void run() 
			{   
				 Message message = new Message();
                 message.what = MSG_DISCONNECT_TIMEOUT;
                 handler.sendMessage(message);
			}   
		};

		//Timeout after 5 seconds
		disconnect_timer.schedule(check_task, 5000);
		
		showWaitDialog(R.string.pppoe_hangup_waiting_msg);
	}


	private void handleCancelDial()
	{
		operation.disconnect();
	}
	

	OnClickListener clickListener = new OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which) 
		{
			//If do not cancel the timer, then discard when disconnecting
			//will cause APK crash
			Log.d(TAG, "#####################################");
			if (disconnect_timer != null)
				disconnect_timer.cancel();
			
			handleCancelDial();
			waitDialog.cancel();
			clearSelf();
		}
	};

	//@Override
	public void onClick(DialogInterface dialog, int which) 
	{
		switch (which) {
        case BUTTON_POSITIVE:
        	if(connectStatus() == PppoeOperation.PPP_STATUS_CONNECTED)
        		handleStopDial();
        	else
        		handleStartDial();
            break;
        case BUTTON_NEGATIVE:
        	clearSelf();
            break;
        default:
            break;
		}
	}

	public class PppoeReceiver extends BroadcastReceiver 
    {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			Log.d(TAG, "#####PppoeReceiver: " + action);

	        if(action.equals(PppoeManager.PPPOE_STATE_CHANGED_ACTION)) {
				int event = intent.getIntExtra(PppoeManager.EXTRA_PPPOE_STATE,PppoeManager.PPPOE_STATE_UNKNOWN);
				Log.d(TAG, "#####event " + event);
				if(event == PppoeStateTracker.EVENT_CONNECTED)
				{
					waitDialog.cancel();
					connect_timer.cancel();
					showAlertDialog(context.getResources().getString(R.string.pppoe_connect_ok));
				}

				if(event == PppoeStateTracker.EVENT_DISCONNECTED)
				{
					//waitDialog.cancel();
					//showAlertDialog(context.getResources().getString(R.string.pppoe_connect_failed));
				}

        	}
		}
	}
	
	private void clearSelf()
	{
		if(pppoeReceiver != null)
			context.unregisterReceiver(pppoeReceiver);
		((PPPoEActivity)context).finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			clearSelf();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
}

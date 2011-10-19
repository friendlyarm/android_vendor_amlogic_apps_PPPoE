package com.amlogic.PPPoE;

import java.util.Timer;
import java.util.TimerTask;
import com.amlogic.PPPoE.R;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import com.amlogic.pppoe.PppoeOperation;

public class PppoeConfigDialog extends AlertDialog implements DialogInterface.OnClickListener
{
	private View mView;
	private EditText mPppoeName;
    private EditText mPppoePasswd;
    private String user_name = null;
    private String user_passwd = null;
    private ProgressDialog waitDialog = null;
    private PppoeOperation operation = null;
    Context context = null;
    private AlertDialog alertDia = null;
    private dialRslReceiver dialRsl = null;
    
    private boolean dia_action_failed = false;
    private CheckBox mCbAutoDial;

	public PppoeConfigDialog(Context context)
	{
		super(context);
		this.context = context;
		operation = new PppoeOperation();
		buildDialog(context);
		waitDialog = new ProgressDialog(this.context); 
	}

	private void buildDialog(Context context)
	{
		setTitle(R.string.pppoe_config_title);
		this.setView(mView = getLayoutInflater().inflate(R.layout.pppoe_configure, null));
		mPppoeName = (EditText)mView.findViewById(R.id.pppoe_name_edit);
		mPppoePasswd = (EditText)mView.findViewById(R.id.pppoe_passwd_edit);
		mCbAutoDial = (CheckBox)mView.findViewById(R.id.auto_dial);
		mPppoeName.setEnabled(true);
		mPppoePasswd.setEnabled(true);
		mCbAutoDial.setVisibility(View.GONE);
		this.setInverseBackgroundForced(true);
		if(connectStatus() != PppoeOperation.PPP_STATUS_CONNECTED)
			this.setButton(BUTTON_POSITIVE, context.getText(R.string.pppoe_dial), this);
		else{
			mView.findViewById(R.id.user_pppoe_text).setVisibility(View.GONE);
			mPppoeName.setVisibility(View.GONE);
			mView.findViewById(R.id.passwd_pppoe_text).setVisibility(View.GONE);
			mPppoePasswd.setVisibility(View.GONE);
			this.setButton(BUTTON_POSITIVE, context.getText(R.string.pppoe_disconnect), this);
		}
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        mCbAutoDial.setChecked(isAutoDial());
	}
	
	@Override
	public void show()
	{
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
	
	void showWaitDialog()
	{
		waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); 
		waitDialog.setTitle(""); 
		waitDialog.setMessage(this.context.getResources().getString(R.string.pppoe_waiting_msg));
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
		sharedata.putString("name",mPppoeName.getText().toString());
		sharedata.putString("passwd",mPppoePasswd.getText().toString()); 
		sharedata.commit();  
	}
	private void getInfoData()
	{
		SharedPreferences sharedata = this.context.getSharedPreferences("inputdata", 0);
		if(sharedata != null && sharedata.getAll().size() > 0)
		{
			user_name = sharedata.getString("name", null);   
			user_passwd = sharedata.getString("passwd", null); 
		}
		else
		{
			user_name = null;
			user_passwd = null;
		}
	}
	
	private int connectStatus()
	{
		return operation.status();
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
		dia_action_failed = false;
		dialRsl = new dialRslReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("PppoeConfigDialog.Result_Dial");
		context.registerReceiver(dialRsl, filter);
		
		String name = mPppoeName.getText().toString();
		String passwd = mPppoePasswd.getText().toString();
		if(name != null && passwd != null)
		{
			saveInfoData();
			Timer check_timer = new Timer();   
			TimerTask check_task = new TimerTask()
			{   
				public void run() 
				{   
					if(connectStatus() != PppoeOperation.PPP_STATUS_CONNECTED)
						dia_action_failed = true;
				}   
			};
			check_timer.schedule(check_task, 30000);  
			showWaitDialog();
			operation.connect(name, passwd);
			
			dialThread dial = new dialThread();
			dial.start();
		}
	}
	
	class dialThread extends Thread
	{
		public void run()
		{
			while(dia_action_failed == false)
			{
				if(connectStatus() == PppoeOperation.PPP_STATUS_CONNECTED)
				{
					Intent intent2 = new Intent("PppoeConfigDialog.Result_Dial");
			    	intent2.putExtra("dial_result", true);
			    	context.sendBroadcast(intent2);
					return;
				}
				waitOk(1000);
			}
			
			Intent intent2 = new Intent("PppoeConfigDialog.Result_Dial");
	    	intent2.putExtra("dial_result", false);
	    	context.sendBroadcast(intent2);
		}
	}
	
	private void waitOk(final int ms)
	{
		try {Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	private void hanleStopDial()
	{
		boolean result = operation.disconnect();
		if(result == true)
		{
			showAlertDialog(this.context.getResources().getString(R.string.pppoe_disconnect_ok));
		}
		else
		{
			showAlertDialog(this.context.getResources().getString(R.string.pppoe_disconnect_failed));
		}

	}
	private void hanleCancelDial()
	{
		operation.disconnect();
	}
	

	OnClickListener clickListener = new OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which) 
		{
			hanleCancelDial();
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
        		hanleStopDial();
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

	public class dialRslReceiver extends BroadcastReceiver 
    {
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			if(intent.getBooleanExtra("dial_result", false) == true)
			{
				waitDialog.cancel();
				showAlertDialog(context.getResources().getString(R.string.pppoe_connect_ok));
			}
			else
			{
				operation.disconnect();
				waitDialog.cancel();
				showAlertDialog(context.getResources().getString(R.string.pppoe_connect_failed));
			}
		}
	}
	
	private void clearSelf()
	{
		if(dialRsl != null)
			context.unregisterReceiver(dialRsl);
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

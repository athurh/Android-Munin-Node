package com.monitoring.munin_node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Main_View extends Activity{
	private static final String TAG = "MuninNode";
	private static final boolean DEBUG = false;
	public static final String PREFS_NAME = "Munin_Node";
	public String Update_Interval = null;
	public String Update_Interval_New = null;
	public String Server = null;
	public String ServerW = null;
	public String ssid = null;
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	Server = settings.getString("Server", "http://");
	ServerW = settings.getString("ServerW", "");
	ssid = settings.getString("ssid", "");
	Update_Interval = settings.getString("Update_Interval", "10");
	setContentView(R.layout.main_view);
	Spinner spinner = (Spinner) findViewById(R.id.spinner1);
	ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.time_array, android.R.layout.simple_spinner_item);
	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	spinner.setAdapter(adapter);
	spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
	final EditText server_text = (EditText) findViewById(R.id.Server);
	final EditText serverw_text = (EditText) findViewById(R.id.ServerW);
	final EditText ssid_text = (EditText) findViewById(R.id.ssid);
	final TextView android_id = (TextView) findViewById(R.id.ANDROID_ID);
	android_id.setText("Android ID: "+Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID));
	final Button save = (Button) findViewById(R.id.Save1);
	server_text.setText(Server);
	serverw_text.setText(ServerW);
	ssid_text.setText(ssid);
	if (DEBUG) Log.d(TAG, "server=" + Server + " server_wifi=" + ServerW + " SSID=" + ssid + " update_interval=" + Update_Interval);
	if (Update_Interval.contentEquals("5")){
		spinner.setSelection(0, true);
	}
	else if (Update_Interval.contentEquals("10")){
		spinner.setSelection(1, true);
	}
	else if (Update_Interval.contentEquals("15")){
		spinner.setSelection(2,true);
	}

	save.setOnClickListener(new View.OnClickListener() {
		final Handler test_handler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if (DEBUG) Log.d(TAG, "Recieved message");
				super.handleMessage(msg);
				Bundle bundle = (Bundle)msg.obj;
				Toast toast = Toast.makeText(Main_View.this, bundle.getString("result") , Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM, -30, 50);
				toast.show();
			}
		};
		public void onClick(View v) {
			Pattern URL = Pattern.compile("http\\:\\/\\/.+\\/$");
			ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo mWifiInfo = wifiManager.getConnectionInfo();
			final Boolean mWifiConnected = mWifi.isConnected();
			final String mWifiSSID = mWifiInfo.getSSID();
			ssid = ssid_text.getText().toString();
			Matcher match1;
			if (ssid.length() == 0) {
				ssid = mWifiSSID;
			}
			if (mWifiConnected && mWifiSSID.equals(ssid)) {
				match1 = URL.matcher(serverw_text.getText().toString());
			} else {
				match1 = URL.matcher(server_text.getText().toString());
			}
			Boolean ok = false;
			while (match1.find()) {
				ok = true;
			}
			if (ok){
				new Thread(new Runnable(){
					public void run(){
						Server = server_text.getText().toString();
						ServerW = serverw_text.getText().toString();
						if (Server.equals("http://") && ServerW.length() > 0) {
							Server = ServerW;
						} else if (ServerW.length() == 0) {
							ServerW = Server;
						}
						Test_Settings test = new Test_Settings();
						Integer test_value;
						if (mWifiConnected && mWifiSSID.equals(ssid)) {
							test_value = test.Run_Test(ServerW);
						} else {
							test_value = test.Run_Test(Server);
						}
						String result = null;
						if (DEBUG) Log.d(TAG, "test=" + test_value);
						switch(test_value){
						case Test_Settings.OK:
							save_settings();
							result = "Saved Successfully";
							break;
						case Test_Settings.FAILURE:
							result = "General Failure, Check URL and try again";
							break;
						}
						Bundle bundle = new Bundle();
						bundle.putString("result", result);
						Message msg = Message.obtain(test_handler, 42, bundle);
						test_handler.sendMessage(msg);
					}
				}
				).start();
			} else {
				Toast toast = Toast.makeText(Main_View.this,"URL is Invalid" , Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM, -30, 50);
				toast.show();
			}
		}
	});
	CheckBox onboot = (CheckBox) findViewById(R.id.onBoot);
	onboot.setChecked(settings.getBoolean("onBoot", false));
	onboot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("onBoot", isChecked);
			editor.commit();
		}
	});
	CheckBox notification = (CheckBox) findViewById(R.id.notification);
	notification.setChecked(settings.getBoolean("notification", false));
	notification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("notification", isChecked);
			editor.commit();
		}
	});
    }

	public class MyOnItemSelectedListener implements OnItemSelectedListener {

		public void onItemSelected(AdapterView<?> parent,View view, int pos, long id) {
			Update_Interval_New = parent.getItemAtPosition(pos).toString();
			if (DEBUG) Log.d(TAG, "update interval=" + Update_Interval_New);
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}

	private void save_settings(){
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("Server", Server);
		editor.putString("ServerW", ServerW);
		editor.putString("ssid", ssid);
		editor.putString("Update_Interval", Update_Interval_New);
		editor.commit();
		if (DEBUG) Log.d(TAG, "server=" + Server + " server_wifi=" + ServerW + " SSID=" + ssid + " update_interval=" + Update_Interval_New);
	}
}

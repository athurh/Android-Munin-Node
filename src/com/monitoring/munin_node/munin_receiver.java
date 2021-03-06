package com.monitoring.munin_node;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class munin_receiver extends BroadcastReceiver {
	private static final String TAG = "MuninNodeReceiver";
	private static final boolean DEBUG = false;
	Intent service = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		if(service == null){
			service = new Intent(context, munin_service.class);
		}
		context.startService(service);
		if (DEBUG) Log.d(TAG, "Receiver ending");
	}

}

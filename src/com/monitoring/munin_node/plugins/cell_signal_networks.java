package com.monitoring.munin_node.plugins;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.monitoring.munin_node.plugin_api.Plugin_API;

public class cell_signal_networks implements Plugin_API {
	ContextWrapper context = null;

	@Override
	public String getName() {
		return "Cell signal networks";
	}

	@Override
	public String getCat() {
		return "Android Phone";
	}

	@Override
	public Boolean needsContext() {
		return true;
	}

	@Override
	public Void setContext(Context newcontext) {
		context = new ContextWrapper(newcontext);
		return null;
	}

	@Override
	public Void run(final Handler handler) {
		final TelephonyManager TelManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final PhoneStateListener mSignalListener = new PhoneStateListener() {

			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				int ASU = -1;
				if (signalStrength.isGsm()) {
					ASU = signalStrength.getGsmSignalStrength();
				} else {
					int strength = -1;
					if (signalStrength.getEvdoDbm() < 0)
						strength = signalStrength.getEvdoDbm();
					else if (signalStrength.getCdmaDbm() < 0)
						strength = signalStrength.getCdmaDbm();
					if (strength < 0){
						// convert to asu
						ASU = Math.round((strength + 113f) / 2f);
					}
				}
				super.onSignalStrengthsChanged(signalStrength);
				final int netType = TelManager.getNetworkType();
				final StringBuilder config = new StringBuilder();
				final StringBuilder update = new StringBuilder();
				final String[] net = {"UNKNOWN", "GPRS", "EDGE", "UMTS", "CDMA", "EVDO_0", "EVDO_A", "RTT",
						"HSDPA", "HSUPA", "HSPA", "IDEN", "EVDO_B", "LTE", "EHRPD", "HSPAP"};
				final String[] colour = {"D30000", "FFE200", "CCF600", "FFBE00", "ABF000", "78E700", "2DD700", "DCF900",
						"009B95", "009B95", "009B95", "FF8000", "00C90D", "133AAC", "133AAC", "0C5AA6",
						"4711AE", "680BAB", "9702A7"};

				config.append("graph_title Signal strength\n");
				config.append("graph_args -l 0\n");
				config.append("graph_scale no\n");
				config.append("graph_vlabel ASU\n");
				config.append("graph_category Android Phone\n");
				config.append("graph_info This graph shows cellular signal in ASU\n");
				config.append("graph_order UNKNOWN IDEN UMTS GPRS RTT EDGE CDMA EVDO_0 EVDO_A EVDO_B HSDPA HSUPA HSPA HSPAP EHRPD LTE\n");
				config.append("graph_total Signal\n");
				for (int i = 0; i < net.length; i++) {
					config.append(net[i] + ".label " + net[i] + "\n");
					config.append(net[i] + ".colour " + colour[i] + "\n");
					config.append(net[i] + ".draw AREASTACK\n");
					if (netType == i) {
						update.append(net[i] + ".value " + ASU + "\n");
					} else {
						update.append(net[i] + ".value 0\n");
					}
				}

				final Bundle bundle = new Bundle();
				bundle.putString("name", "Cell signal networks");
				bundle.putString("config", config.toString());
				bundle.putString("update", update.toString());
				final Message msg = Message.obtain(handler, 42, bundle);
				handler.sendMessage(msg);
			}
		};

		TelManager.listen(mSignalListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		Timer Timer = new Timer();
		TimerTask TimerTask = new TimerTask(){
			@Override
			public void run(){
				TelManager.listen(mSignalListener, PhoneStateListener.LISTEN_NONE);
			}
		};
		Timer.schedule(TimerTask, 1000);
		return null;
	}
}

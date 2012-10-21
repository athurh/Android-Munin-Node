package com.monitoring.munin_node.plugins;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.monitoring.munin_node.plugin_api.Plugin_API;

public class cpuspeed implements Plugin_API {

	public String getName() {
		return "CPU frequency";
	}
	public String getCat() {
		return "System";
	}

	@Override
	public Boolean needsContext() {
		return false;
	}

	@Override
	public Void setContext(Context context) {
		return null;
	}

	@Override
	public Void run(Handler handler) {
		final Pattern timeState_regex = Pattern.compile("\\s+");
		BufferedReader in = null;
		String update;
		String[] items;
		String str;
		Long data = 0L;

		final StringBuilder config = new StringBuilder();
		config.append("graph_args --base 1000 -l 0\n");
		config.append("graph_info This graph shows the average speeds at which the CPUs are running\n");
		config.append("graph_vlabel Hz\n");
		config.append("graph_title CPU frequency scaling\n");
		config.append("graph_category system\n");
		config.append("cpu.label CPU\n");
		config.append("cpu.type DERIVE\n");
		config.append("cpu.min 0\n");

		/*File folder = new File("/sys/devices/system/cpu");
		String[] listFiles = folder.list();
		Pattern regex = Pattern.compile("^cpu+[0-9]+");
		Matcher match = null;
		int cpun = 0;
		for (String file : listFiles) {
			match = regex.matcher(file);
			if (match.find()) {
				config.append("cpu " + cpun + ".label CPU" + cpun + "\n");
				config.append("cpu " + cpun + ".type DERIVE\n");
				config.append("cpu " + cpun + ".min 0\n");
				cpun++;
			}
		}*/

		try {
			in = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state"));
			while ((str = in.readLine()) != null) {
				items = timeState_regex.split(str);
				data = Long.parseLong(items[0]) * Long.parseLong(items[1]) + data;
			}
			update = "cpu.value " + data.toString() + "0";
		} catch (IOException e) {
			update = "cpu.value U";
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {}
		}

		final Bundle bundle = new Bundle();
		bundle.putString("name", this.getName());
		bundle.putString("config", config.toString());
		bundle.putString("update", update);
		final Message msg = Message.obtain(handler, 42, bundle);
		handler.sendMessage(msg);
		return null;
	}
}

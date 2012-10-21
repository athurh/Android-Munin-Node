package com.monitoring.munin_node.plugins;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.monitoring.munin_node.plugin_api.Plugin_API;

public class irqstats implements Plugin_API {
	Map<String, String[]> irqinfo = new HashMap<String, String[]>();

	@Override
	public String getName() {
		return "IRQstats";
	}

	@Override
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
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("/proc/interrupts"));
			String str;
			str = in.readLine();
			Pattern cpu_pattern = Pattern.compile("CPU\\d+");
			Matcher matcher = cpu_pattern.matcher(str);
			int cpu_count = 0;
			while (matcher.find()){
				cpu_count++;
			}
			StringBuilder pattern = new StringBuilder();
			pattern.append("([\\w\\d]+):[\\s]+");
			for(int i = 0;i < cpu_count;i++){
				pattern.append("([\\d]+)[\\s]+");
			}
			pattern.append("([\\w\\s-:,]+)");
			Pattern line_match = Pattern.compile(pattern.toString());
			while ((str = in.readLine()) != null) {
				matcher = line_match.matcher(str);
				if(matcher.find()){
					// TODO get multicore data (often sleeping)
					String[] temp = {matcher.group(2),matcher.group(cpu_count+2)};
					irqinfo.put(matcher.group(1), temp);
				}
			}
		} catch (IOException e) {
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {}
		}
		StringBuilder output = new StringBuilder();
		StringBuilder output2 = new StringBuilder();
		output.append("graph_title Individual interrupts\n");
		output.append("graph_args --base 1000 -l 0\ngraph_vlabel interrupts / ${graph_period}\ngraph_category system\n");
		//TODO Fix Graph order
		for (Map.Entry<String, String[]> entry : irqinfo.entrySet()) {
			output.append("i"+entry.getKey()+".label "+entry.getValue()[1]);
			output.append("\ni"+entry.getKey()+".info Interrupt "+entry.getKey()+", for devices(s): "+entry.getValue()[1]);
			output.append("\ni"+entry.getKey()+".type DERIVE");
			output.append("\ni"+entry.getKey()+".min 0\n");
			output2.append("\ni"+entry.getKey()+".value "+entry.getValue()[0]);
		}
		Bundle bundle = new Bundle();
		bundle.putString("name", this.getName());
		bundle.putString("config", output.toString());
		bundle.putString("update", output2.toString());
		Message msg = Message.obtain(handler, 42, bundle);
		handler.sendMessage(msg);
		return null;
	}

}

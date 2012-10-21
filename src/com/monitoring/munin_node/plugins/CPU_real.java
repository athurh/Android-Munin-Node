package com.monitoring.munin_node.plugins;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.monitoring.munin_node.plugin_api.Plugin_API;

public class CPU_real implements Plugin_API{
	ContextWrapper context = null;

	@Override
	public String getName(){
		return "CPU real";
	}

	@Override
	public String getCat(){
		return "System";
	}

	@Override
	public Boolean needsContext(){
		return true;
	}

	@Override
	public Void setContext(Context newcontext) {
		context = new ContextWrapper(newcontext);
		return null;
	}

	@Override
	public Void run(Handler handler) {
		final Pattern split_regex = Pattern.compile("\\s+");
		BufferedReader in = null;
		String[] items;
		String[] freqs = new String[16];
		String cpuInfo = "";
		String str;
		long freqRaw = 0;
		int fc = 0;

		try {
			in = new BufferedReader(new FileReader("/proc/stat"));
			cpuInfo = in.readLine();
		} catch (IOException e) {
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {}
		}
		try {
			in = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state"));
			while ((str = in.readLine()) != null) {
				items = split_regex.split(str);
				freqs[fc] = items[0];
				freqRaw = Long.parseLong(items[0]) * Long.parseLong(items[1]) + freqRaw;
				fc++;
			}
		} catch (FileNotFoundException e) {
			freqs[0] = "1";
		} catch (IOException e) {
			freqs[0] = "1";
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {}
		}

		Pattern regex = Pattern.compile("^cpu +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+");
		Matcher match = regex.matcher(cpuInfo);
		boolean extinfo = false;
		if (match.find()) {
			extinfo = true;
		}
		regex = Pattern.compile("^cpu +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+");
		match = regex.matcher(cpuInfo);
		boolean extextinfo = false;
		if (match.find()) {
			extextinfo = true;
		}

		/*final File folder = new File("/sys/devices/system/cpu");
		final String[] list = folder.list();
		regex = Pattern.compile("^cpu+[0-9]+");
		int cpus = 0;
		for (String file : list) {
			match = regex.matcher(file);
			if (match.find()) {
				cpus++;
			}
		}*/

		final StringBuilder config = new StringBuilder();
		config.append("graph_title CPU usage\n");
		config.append("graph_order system user nice idle");
		if (extinfo) {
			config.append(" iowait irq softirq");
			if (extextinfo) {
				config.append(" steal\n");
			} else {
				config.append("\n");
			}
		} else {
			config.append("\n");
		}
		config.append("graph_args --base 1000 -r --lower-limit 0 --upper-limit 100\n");
		config.append("graph_vlabel %\n");
		config.append("graph_scale no\n");
		config.append("graph_info This graph shows how CPU time is spent.\n");
		config.append("graph_category system\n");
		config.append("graph_period second\n");
		config.append("system.label system\n");
		config.append("system.draw AREA\n");
		config.append("system.min 0\n");
		config.append("system.info CPU time spent by the kernel in system activities\n");
		config.append("user.label user\n");
		config.append("user.draw STACK\n");
		config.append("user.min 0\n");
		config.append("user.info CPU time spent by normal programs and daemons\n");
		config.append("nice.label nice\n");
		config.append("nice.draw STACK\n");
		config.append("nice.min 0\n");
		config.append("nice.info CPU time spent by nice(1)d programs\n");
		config.append("idle.label idle\n");
		config.append("idle.draw STACK\n");
		config.append("idle.min 0\n");
		config.append("idle.info Idle CPU time");
		if (extinfo) {
			config.append("\niowait.label iowait\n");
			config.append("iowait.draw STACK\n");
			config.append("iowait.min 0\n");
			config.append("iowait.info CPU time spent waiting for I/O operations to finish when there is nothing else to do.\n");
			config.append("irq.label irq\n");
			config.append("irq.draw STACK\n");
			config.append("irq.min 0\n");
			config.append("irq.info CPU time spent handling interrupts\n");
			config.append("softirq.label softirq\n");
			config.append("softirq.draw STACK\n");
			config.append("softirq.min 0\n");
			config.append("softirq.info CPU time spent handling batched interrupts");
			if (extextinfo) {
				config.append("\nsteal.label steal\n");
				config.append("steal.draw STACK\n");
				config.append("steal.min 0\n");
				config.append("steal.info The time that a virtual CPU had runnable tasks, but the virtual CPU itself was not running");
			}
		}

		final SharedPreferences settings = context.getSharedPreferences("Munin_Node.CPU_real", 0);
		final SharedPreferences settingsMain = context.getSharedPreferences("Munin_Node", 0);
		final SharedPreferences.Editor editor = settings.edit();
		final StringBuilder update = new StringBuilder();
		items = split_regex.split(cpuInfo);
		long user = Long.parseLong(items[1]);
		long nice = Long.parseLong(items[2]);
		long system = Long.parseLong(items[3]);
		long idle = Long.parseLong(items[4]);
		long oldUser = settings.getLong("user", -1);
		long oldNice = settings.getLong("nice", -1);
		long oldSystem = settings.getLong("system", -1);
		long oldIdle = settings.getLong("idle", -1);
		long iowait = 0;
		long irq = 0;
		long softirq = 0;
		long steal = 0;
		long oldIowait = 0;
		long oldIrq = 0;
		long oldSoftirq = 0;
		long oldSteal = 0;
		editor.putLong("user", user);
		editor.putLong("nice", nice);
		editor.putLong("system", system);
		editor.putLong("idle", idle);
		if (extinfo) {
			iowait = Long.parseLong(items[5]);
			irq = Long.parseLong(items[6]);
			softirq = Long.parseLong(items[7]);
			oldIowait = settings.getLong("iowait", -1);
			oldIrq = settings.getLong("irq", -1);
			oldSoftirq = settings.getLong("softirq",-1);
			editor.putLong("iowait", iowait);
			editor.putLong("irq", irq);
			editor.putLong("softirq", softirq);
			if (extextinfo) {
				steal = Long.parseLong(items[8]);
				oldSteal = settings.getLong("steal", -1);
				editor.putLong("steal", steal);
			}
		}
		long oldFreqRaw = settings.getLong("freqValue", 0);
		editor.putLong("freqValue", freqRaw);
		editor.commit();

		long newFreq;
		if (oldFreqRaw < freqRaw) {
			newFreq = freqRaw - oldFreqRaw;
		} else {
			newFreq = freqRaw;
		}
		final int updateInterval = 60 * Integer.parseInt(settingsMain.getString("Update_Interval", "10"));
		final float freqMax = Float.parseFloat(freqs[0]) * 100;
		final float freqScale = newFreq / freqMax / updateInterval;

		user = user - oldUser;
		nice = nice - oldNice;
		system = system - oldSystem;
		idle = idle - oldIdle;
		float total = user + nice + system + idle;
		if (extinfo) {
			iowait = iowait - oldIowait;
			irq = irq - oldIrq;
			softirq = softirq - oldSoftirq;
			total = total + iowait + irq + softirq;
			if (extextinfo) {
				steal = steal - oldSteal;
				total = total + steal;
			}
		}
		total = total / 100;
		float userValue = 0;
		float niceValue = 0;
		float systemValue = 0;
		float idleValue = 0;
		float iowaitValue = 0;
		float irqValue = 0;
		float softirqValue = 0;
		float stealValue = 0;
		if (oldUser == -1) {
			userValue = 0;
		} else {
			userValue = user / total * freqScale;
		}
		if (oldNice == -1) {
			niceValue = 0;
		} else {
			niceValue = nice / total * freqScale;
		}
		if (oldSystem == -1) {
			systemValue = 0;
		} else {
			systemValue = system / total * freqScale;
		}
		if (extinfo) {
			if (oldIowait == -1) {
				iowaitValue = 0;
			} else {
				iowaitValue = iowait / total * freqScale;
			}
			if (oldIrq == -1) {
				irqValue = 0;
			} else {
				irqValue = irq / total * freqScale;
			}
			if (oldSoftirq == -1) {
				softirqValue = 0;
			} else {
				softirqValue = softirq / total * freqScale;
			}
			if (extextinfo) {
				if (oldSteal == -1) {
					stealValue = 0;
				} else {
					stealValue = steal / total * freqScale;
				}
			}
		}
		total = userValue + niceValue + systemValue;
		if (extinfo) {
			total = total + iowaitValue + irqValue + softirqValue;
			if (extextinfo) {
				total = total + stealValue;
			}
		}
		if (oldIdle == -1) {
			idleValue = 0;
		} else {
			idleValue = 100 - total; // assume sleep as idle
		}

		update.append("user.value " + userValue);
		update.append("\nnice.value " + niceValue);
		update.append("\nsystem.value " + systemValue);
		update.append("\nidle.value " + idleValue);
		if (extinfo) {
			update.append("\niowait.value " + iowaitValue);
			update.append("\nirq.value " + irqValue);
			update.append("\nsoftirq.value " + softirqValue);
			if (extextinfo) {
				update.append("\nsteal.value " + stealValue);
			}
		}

		final Bundle bundle = new Bundle();
		bundle.putString("name", this.getName());
		bundle.putString("config", config.toString());
		bundle.putString("update", update.toString());
		final Message msg = Message.obtain(handler, 42, bundle);
		handler.sendMessage(msg);
		return null;
	}

}

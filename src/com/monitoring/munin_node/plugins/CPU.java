package com.monitoring.munin_node.plugins;

import java.io.BufferedReader;
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

public class CPU implements Plugin_API{
	ContextWrapper context = null;
	public String getName(){
		return "CPU";
	}
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
		BufferedReader in = null;
		String cpu = null;
		try {
			in = new BufferedReader(new FileReader("/proc/stat"));
			cpu = in.readLine();
		} catch (IOException e) {
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {}
		}
		Pattern regex = Pattern.compile("^cpu +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+");
		Matcher match = regex.matcher(cpu);
		boolean extinfo = false;
		if (match.find()) {
			extinfo = true;
		}
		regex = Pattern.compile("^cpu +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+ +[0-9]+");
		match = regex.matcher(cpu);
		boolean extextinfo = false;
		if (match.find()) {
			extextinfo = true;
		}
		StringBuilder output = new StringBuilder();
		output.append("graph_title CPU usage\n");
		output.append("graph_order system user nice idle");
		if(extinfo == true){
			output.append(" iowait irq softirq\n");
		} else {
			output.append("\n");
		}
		output.append("graph_args --base 1000 -r --lower-limit 0 --upper-limit 100\n");
		output.append("graph_vlabel %\n");
		output.append("graph_scale no\n");
		output.append("graph_info This graph shows how CPU time is spent.\n");
		output.append("graph_category system\n");
		output.append("graph_period second\n");
		output.append("system.label system\n");
		output.append("system.draw AREA\n");
		output.append("system.min 0\n");
		output.append("system.info CPU time spent by the kernel in system activities\n");
		output.append("user.label user\n");
		output.append("user.draw STACK\n");
		output.append("user.min 0\n");
		output.append("user.info CPU time spent by normal programs and daemons\n");
		output.append("nice.label nice\n");
		output.append("nice.draw STACK\n");
		output.append("nice.min 0\n");
		output.append("nice.info CPU time spent by nice(1)d programs\n");
		output.append("idle.label idle\n");
		output.append("idle.draw STACK\n");
		output.append("idle.min 0\n");
		output.append("idle.info Idle CPU time");
		if (extinfo == true) {
			output.append("\niowait.label iowait\n");
			output.append("iowait.draw STACK\n");
			output.append("iowait.min 0\n");
			output.append("iowait.info CPU time spent waiting for I/O operations to finish when there is nothing else to do.\n");
			output.append("irq.label irq\n");
			output.append("irq.draw STACK\n");
			output.append("irq.min 0\n");
			output.append("irq.info CPU time spent handling interrupts\n");
			output.append("softirq.label softirq\n");
			output.append("softirq.draw STACK\n");
			output.append("softirq.min 0\n");
			output.append("softirq.info CPU time spent handling batched interrupts");
			if (extextinfo == true) {
				output.append("\nsteal.label steal\n");
				output.append("steal.draw STACK\n");
				output.append("steal.min 0\n");
				output.append("steal.info The time that a virtual CPU had runnable tasks, but the virtual CPU itself was not running");
			}
		}

		StringBuilder output2 = new StringBuilder();
		Pattern split_regex = Pattern.compile("\\s+");
		String[] items = split_regex.split(cpu);
		final SharedPreferences settings = context.getSharedPreferences("Munin_Node.CPU", 0);
		SharedPreferences.Editor editor = settings.edit();
		Long oldUser = settings.getLong("user", -1L);
		Long oldNice = settings.getLong("nice", -1L);
		Long oldSystem = settings.getLong("system", -1L);
		Long oldIdle = settings.getLong("idle", -1L);
		Long oldIowait = settings.getLong("iowait", -1L);
		Long oldIrq = settings.getLong("irq", -1L);
		Long oldSoftirq = settings.getLong("softirq", -1L);
		Long oldSteal = settings.getLong("steal", -1L);
		editor.putLong("user", Long.parseLong(items[1]));
		editor.putLong("nice", Long.parseLong(items[2]));
		editor.putLong("system", Long.parseLong(items[3]));
		editor.putLong("idle", Long.parseLong(items[4]));
		if (extinfo == true) {
			editor.putLong("iowait", Long.parseLong(items[5]));
			editor.putLong("irq", Long.parseLong(items[6]));
			editor.putLong("softirq", Long.parseLong(items[7]));
			if(extextinfo == true){
				editor.putLong("steal", Long.parseLong(items[8]));
			}
		}
		editor.commit();
		Long Total = (Long.parseLong(items[1])-oldUser)+(Long.parseLong(items[2])-oldNice)+(Long.parseLong(items[3])-oldSystem)+(Long.parseLong(items[4])-oldIdle);
		if (extinfo == true) {
			Total = Total+(Long.parseLong(items[5])-oldIowait)+(Long.parseLong(items[6])-oldIrq)+(Long.parseLong(items[7])-oldSoftirq);
			if (extextinfo == true) {
				Total = Total+(Long.parseLong(items[8])-oldSteal);
			}
		}
		if (oldUser == -1) {
			output2.append("user.value U\n");
		} else {
			Float user = new Float((100*(Long.parseLong(items[1])-oldUser)))/new Float(Total);
			output2.append("user.value "+user.toString()+"\n");
		}
		if (oldNice == -1) {
			output2.append("nice.value U\n");
		} else {
			Float nice = new Float((100*(Long.parseLong(items[2])-oldNice)))/new Float(Total);
			output2.append("nice.value "+nice.toString()+"\n");
		}
		if (oldSystem == -1) {
			output2.append("system.value U\n");
		} else {
			Float system = new Float((100*(Long.parseLong(items[3])-oldSystem)))/new Float(Total);
			output2.append("system.value "+system.toString()+"\n");
		}
		if (oldIdle == -1) {
			output2.append("idle.value U");
		} else {
			Float idle = new Float((100*(Long.parseLong(items[4])-oldIdle)))/new Float(Total);
			output2.append("idle.value "+idle.toString());
		}
		if (extinfo == true) {
			if (oldIowait == -1) {
				output2.append("\niowait.value U\n");
			} else {
				Float iowait = new Float((100*(Long.parseLong(items[5])-oldIowait)))/new Float(Total);
				output2.append("\niowait.value "+iowait.toString()+"\n");
			}
			if (oldIrq == -1) {
				output2.append("irq.value U\n");
			} else {
				Float irq = new Float((100*(Long.parseLong(items[6])-oldIrq)))/new Float(Total);
				output2.append("irq.value "+irq.toString()+"\n");
			}
			if (oldSoftirq == -1) {
				output2.append("softirq.value U");
			} else {
				Float softirq = new Float((100*(Long.parseLong(items[7])-oldSoftirq)))/new Float(Total);
				output2.append("softirq.value "+softirq.toString());
			}
			if (extextinfo == true) {
				if (oldSteal == -1) {
					output2.append("\nsteal.value U");
				} else {
					Float steal = new Float((100*(Long.parseLong(items[8])-oldSteal)))/new Float(Total);
					output2.append("\nsteal.value "+steal.toString());
				}
			}
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

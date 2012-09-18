package com.monitoring.munin_node.plugins;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.monitoring.munin_node.plugin_api.Plugin_API;

//TODO cleanup new lines so that there are no extras throught the output.
public class KSM implements Plugin_API{
	public String getName(){
		return "KSM";
	}
	public String getCat(){
		return "System";
	}

	@Override
	public Boolean needsContext() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public Void setContext(Context context) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Void run(Handler handler) {

		StringBuffer output = new StringBuffer();
		output.append("graph_args --base 1000 -l 0\n");
		output.append("graph_vlabel pages\n");
		output.append("graph_title KSM usage\n");
		output.append("graph_category system\n");
		output.append("pages_shared.label pages_shared\n");
		output.append("pages_shared.draw LINE\n");
		output.append("pages_sharing.label pages_sharing\n");
		output.append("pages_sharing.draw LINE\n");
		output.append("pages_unshared.label pages_unshared\n");
		output.append("pages_unshared.draw LINE\n");
		output.append("pages_volatile.label pages_volatile\n");
		output.append("pages_volatile.draw LINE");

		StringBuffer output2 = new StringBuffer();
		try {
			BufferedReader pages_shared = new BufferedReader(new FileReader("/sys/kernel/mm/ksm/pages_shared"));
			output2.append("pages_shared.value " + pages_shared.readLine() + "\n");
			pages_shared.close();
		} catch (IOException e) {}
		try {
			BufferedReader pages_sharing = new BufferedReader(new FileReader("/sys/kernel/mm/ksm/pages_sharing"));
			output2.append("pages_sharing.value " + pages_sharing.readLine() + "\n");
			pages_sharing.close();
		} catch (IOException e) {}
		try {
			BufferedReader pages_unshared = new BufferedReader(new FileReader("/sys/kernel/mm/ksm/pages_unshared"));
			output2.append("pages_unshared.value " + pages_unshared.readLine() + "\n");
			pages_unshared.close();
		} catch (IOException e) {}
		try {
			BufferedReader pages_volatile = new BufferedReader(new FileReader("/sys/kernel/mm/ksm/pages_volatile"));
			output2.append("pages_volatile.value " + pages_volatile.readLine());
			pages_volatile.close();
		} catch (IOException e) {}

		Bundle bundle = new Bundle();
		bundle.putString("name", this.getName());
		bundle.putString("config", output.toString());
		bundle.putString("update", output2.toString());
		Message msg = Message.obtain(handler, 42, bundle);
		handler.sendMessage(msg);
		return null;
	}
}

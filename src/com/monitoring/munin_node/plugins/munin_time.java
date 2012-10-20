package com.monitoring.munin_node.plugins;

import android.content.Context;
import android.os.Handler;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;

import com.monitoring.munin_node.plugin_api.Plugin_API;

public class munin_time implements Plugin_API {
	ContextWrapper context = null;

	@Override
	public String getName() {
		return "Munin Time";
	}

	@Override
	public String getCat() {
		return "Munin";
	}

	@Override
	public Void run(Handler handler) {
		final SharedPreferences settings = context.getSharedPreferences("Munin_Node", 0);
		final long serviceTime = settings.getLong("service_time", 0);
		final long pluginsTime = settings.getLong("plugins_time", 0);
		final long uploadTime = settings.getLong("upload_time", 0);
		StringBuilder output = new StringBuilder();
		output.append("graph_title Munin Processing Time\n");
		output.append("graph_vlabel ms\n");
		output.append("graph_info This graph shows how much time is spent running the munin service, its data is delayed by one run.\n");
		output.append("graph_category munin\n");
		output.append("graph_scale no\n");
		output.append("time.label Service\n");
		output.append("time.draw LINE\n");
		output.append("time.info Time Spent running the munin service\n");
		output.append("plugin.label Plugin\n");
		output.append("plugin.draw LINE\n");
		output.append("plugin.info Time Spent running the munin plugins\n");
		output.append("upload.label Upload\n");
		output.append("upload.draw LINE\n");
		output.append("upload.info Time Spent uploading munin data\n");
		Bundle bundle = new Bundle();
		bundle.putString("name", this.getName());
		bundle.putString("config", output.toString());
		bundle.putString("update", "time.value " + serviceTime + "\nplugin.value " + pluginsTime + "\nupload.value " + uploadTime);
		Message msg = Message.obtain(handler, 42, bundle);
		handler.sendMessage(msg);
		return null;
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

}

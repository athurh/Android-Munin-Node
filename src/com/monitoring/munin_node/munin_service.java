package com.monitoring.munin_node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings.Secure;
import android.util.Log;

import com.monitoring.munin_node.protos.Plugins;
import com.monitoring.munin_node.plugin_api.LoadPlugins;
import com.monitoring.munin_node.plugin_api.PluginFactory;
import com.monitoring.munin_node.plugin_api.Plugin_API;

public class munin_service extends Service{
    private static final String TAG = "MuninNodeService";
    final int MUNIN_NOTIFICATION = 1;
    List<Plugin_API> plugin_objects;
    long pluginsTime = 0;
    long startPluginsTime = 0;
    long startUploadTime = 0;

    @Override
    public void onDestroy() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancel(MUNIN_NOTIFICATION);
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Munin Wake Lock");
		wakeLock.acquire();
		final long startTime = System.currentTimeMillis();
		Log.d(TAG, "Service started");
		final ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		final WifiInfo mWifiInfo = wifiManager.getConnectionInfo();
		final boolean mWifiConnected = mWifi.isConnected();
		final String mWifiSSID = mWifiInfo.getSSID();
		final SharedPreferences settings = this.getSharedPreferences("Munin_Node", 0);
		final Editor editor = settings.edit();
		final String mSSID = settings.getString("ssid", "");
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final Notification mNotification = new Notification(R.drawable.notification, "Munin Node Started", startTime);
		final boolean mNotificationCheckOut = settings.getBoolean("notification", false);
		final Context context = getApplicationContext();
		if (mNotificationCheckOut) {
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,  new Intent(this, munin_node.class), 0);
			mNotification.setLatestEventInfo(context, "Munin Node", "Just letting you know I am running", contentIntent);
			mNotification.flags |= Notification.FLAG_NO_CLEAR;
			mNotificationManager.notify(MUNIN_NOTIFICATION, mNotification);
		}
		class Count{
			int ran = 0;
			int done = 0;
			public void ranincrement(){
				ran++;
			}
			public void doneincrement(){
				done++;
			}
			public Boolean Done(){
				if(done == ran){
					return true;
				}
				else{
					return false;
				}
			}
			public void Reset(){
				ran = 0;
				done = 0;
			}
		}

		final Count count = new Count();
		final Plugins.Builder plugins = Plugins.newBuilder();
		final Handler service_Handler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				if(msg.what == 42){
					final Bundle bundle = (Bundle)msg.obj;
					final Plugins.Plugin.Builder plugin = Plugins.Plugin.newBuilder();
					plugin.setName(bundle.getString("name")).setConfig(bundle.getString("config")).setUpdate(bundle.getString("update"));
					plugins.addPlugin(plugin);
					count.doneincrement();
					if(count.Done()){
						count.Reset();
						pluginsTime = System.currentTimeMillis() - startPluginsTime;
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						GZIPOutputStream gzipped = null;
						try {
							gzipped = new GZIPOutputStream(out);
							plugins.build().writeTo(gzipped);
						} catch (IOException e) {
							Log.w(TAG, e);
						} finally {
							try {
								if (gzipped != null) {
									gzipped.close();
									plugins.clear();
								}
							} catch (IOException e) {}
						}
						String Server;
						if (mWifiConnected && mWifiSSID.equals(mSSID)) {
							Server = settings.getString("ServerW", "");
						} else {
							Server = settings.getString("Server", "");
						}
						Log.d(TAG, "Uploading data to " + Server);
						Server = Server+Secure.getString(getBaseContext().getContentResolver(), Secure.ANDROID_ID);
						startUploadTime = System.currentTimeMillis();
						new UploadURL(this,Server,out).start();
						try {
							if (out != null) {
								out.close();
							}
						} catch (IOException e) {
							Log.w(TAG, e);
						}
					}
				}
				else if (msg.what == 43){
					final long uploadTime = System.currentTimeMillis() - startUploadTime;
					if (mNotificationCheckOut) {
						mNotificationManager.cancel(MUNIN_NOTIFICATION);
					}
					final long serviceTime = System.currentTimeMillis() - startTime;
					editor.putLong("plugins_time", pluginsTime);
					editor.putLong("upload_time", uploadTime);
					editor.putLong("service_time", serviceTime);
					editor.commit();
					Log.d(TAG, "Service finished");
					wakeLock.release();
				}
			}
		};
		LoadPlugins loadplugins = new LoadPlugins();
		List<String> plugin_list = loadplugins.getPluginList(context);
		startPluginsTime = System.currentTimeMillis();
		boolean enabled;
		if (plugin_objects == null) {
			Plugin_API plugin;
			plugin_objects = new ArrayList<Plugin_API>();
			for (String p :plugin_list){
				plugin = (Plugin_API)PluginFactory.getPlugin(p);
				enabled = settings.getBoolean(plugin.getName(), true);
				if(plugin.needsContext()){
					plugin.setContext(this);
				}
				if(enabled){
					count.ranincrement();
					plugin.run(service_Handler);
				}
				plugin_objects.add(plugin);
			}
		} else {
			for(Plugin_API plugin : plugin_objects){
				enabled = settings.getBoolean(plugin.getName(), true);
				if(enabled){
					count.ranincrement();
					plugin.run(service_Handler);
				}
			}
		}
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}

package com.monitoring.munin_node.plugin_api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

public class LoadPlugins {
    private static final String TAG = "MuninNodeLoadPlugins";
    private static final boolean DEBUG = false;
    List<Plugin_API> plugins = new ArrayList<Plugin_API>();
    List<String> plugin_names = new ArrayList<String>();
    String[] catarray = {};
    String[][] childcatarray = {{}};
	public LoadPlugins(Context context){
		getPluginList(context);
		for(final String s : plugin_names){
			plugins.add((Plugin_API)PluginFactory.getPlugin(s));
		}
	}
	public LoadPlugins(){
		for(final String s : plugin_names){
			plugins.add((Plugin_API)PluginFactory.getPlugin(s));
		}
	}
	public List<String> getPluginList(Context context){
		InputStream plugin_stream = context.getResources().openRawResource( context.getResources().getIdentifier("raw/plugins", "id", context.getPackageName()));
        BufferedReader plugin_reader = new BufferedReader(new InputStreamReader(plugin_stream));
        String line;
        try{
        while ((line = plugin_reader.readLine()) != null) {
            plugin_names.add(line);
        }
        }
        catch(IOException e){
        	//afd
        }
        return plugin_names;
	}
	public void genCats(){
		if (DEBUG) Log.d(TAG, "Getting categories");
		Map<String, String[]> Cats = new HashMap<String, String[]>();
		for(Plugin_API p : plugins){
			if (DEBUG) Log.d(TAG, "category=" + p.getCat() + " name=" + p.getName());
			if(Cats.containsKey(p.getCat())){
				String[] plugs = Cats.get(p.getCat());
				String[] temp = new String[plugs.length+1];
				System.arraycopy(plugs,0,temp,0,plugs.length);
				temp[plugs.length] = p.getName();
				Cats.put(p.getCat(), temp);
				}
			else{
				String[] temp = {p.getName()};
				Cats.put(p.getCat(), temp);
			}
		}
		for (Map.Entry<String, String[]> entry : Cats.entrySet())
		{
			String[] temp = new String[catarray.length+1];
			System.arraycopy(catarray,0,temp,0,catarray.length);
			temp[catarray.length] = entry.getKey();
			catarray = temp;
			String[] childtemp = entry.getValue();
			String[][] temp1 = new String[catarray.length][];
			System.arraycopy(childcatarray,0,temp1,0,childcatarray.length);
			temp1[catarray.length-1] = childtemp;
			childcatarray = temp1;
		}
		if (DEBUG) Log.d(TAG, "Done");
	}
	public String[] getCats(){
		return catarray;
	}
	public String[][] getchildCats(){
		return childcatarray;
	}

}

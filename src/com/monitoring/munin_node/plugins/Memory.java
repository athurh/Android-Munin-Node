package com.monitoring.munin_node.plugins;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import com.monitoring.munin_node.plugin_api.Plugin_API;


public class Memory implements Plugin_API{
	public String getName(){
		return "Memory";
	}
	public String getCat(){
		return "System";
	}
	public String getConfig(){
		Map<String, String> meminfo = new HashMap<String, String>();
		Pattern meminfo_regex = Pattern.compile("([:\\s]+)");
		try {
			BufferedReader in = new BufferedReader(new FileReader("/proc/meminfo"));
			String str;
			while ((str = in.readLine()) != null) {
				String[] items = meminfo_regex.split(str);
				Long data = Long.parseLong(items[1])*1024;
				meminfo.put(items[0], data.toString());
			}
			in.close();
		}
		catch (IOException e) {}
		StringBuffer output = new StringBuffer();

		output.append("graph_args --base 1024 -l 0 --upper-limit "+meminfo.get("MemTotal")+"\n");
		output.append("graph_vlabel Bytes\ngraph_title Memory usage\ngraph_category system\ngraph_info This graph shows what the machine uses memory for.\n");
		output.append("graph_order apps ");
		if (meminfo.containsKey("PageTables")){
			output.append("page_tables ");
		}
		if (meminfo.containsKey("SwapCached")){

			output.append("swap_cache ");
		}
		if (meminfo.containsKey("Slab")){
			output.append("slab ");
		}
		output.append("cached buffers free swap\n");
		output.append("apps.label apps\napps.draw AREA\napps.info Memory used by user-space applications.\nbuffers.label buffers\nbuffers.draw STACK\n");
		output.append("buffers.info Block device (e.g. harddisk) cache. Also where \"dirty\" blocks are stored until written.\nswap.label swap\nswap.draw STACK\n");
		output.append("swap.info Swap space used.\ncached.label cache\ncached.draw STACK\ncached.info Parked file data (file content) cache.\nfree.label unused\n");
		output.append("free.draw STACK\nfree.info Wasted memory. Memory that is not used for anything at all.\n");
		if (meminfo.containsKey("Slab")){
			output.append("slab.label slab_cache\nslab.draw STACK\nslab.info Memory used by the kernel (major users are caches like inode, dentry, etc).\n");
		}
		if (meminfo.containsKey("SwapCached")){

			output.append("swap_cache.label swap_cache\nswap_cache.draw STACK\nswap_cache.info A piece of memory that keeps track of pages that have been fetched from swap but not yet been modified.\n");
		}
		{
			output.append("page_tables.label page_tables\npage_tables.draw STACK\npage_tables.info Memory used to map between virtual and physical memory addresses.\n");
		}
		if (meminfo.containsKey("VmallocUsed")){
			output.append("vmalloc_used.label vmalloc_used\nvmalloc_used.draw LINE2\nvmalloc_used.info 'VMalloc' (kernel) memory used\n");
		}
		if (meminfo.containsKey("Committed_AS")) {
			output.append("committed.label committed\ncommitted.draw LINE2\ncommitted.info The amount of memory allocated to programs. Overcommitting is normal, but may indicate memory leaks.\n");
		}
		if (meminfo.containsKey("Mapped")) {
			output.append("mapped.label mapped\nmapped.draw LINE2\nmapped.info All mmap()ed pages.\n");
		}
		if (meminfo.containsKey("Active")) {
			output.append("active.label active\nactive.draw LINE2\nactive.info Memory recently used. Not reclaimed unless absolutely necessary.\n");
		}
		if (meminfo.containsKey("ActiveAnon")) {
			output.append("active_anon.label active_anon\nactive_anon.draw LINE1\n");
		}
		if (meminfo.containsKey("ActiveCache")){
			output.append("active_cache.label active_cache\nactive_cache.draw LINE1\n");
		}
		if (meminfo.containsKey("Inactive")) {
			output.append("inactive.label inactive\ninactive.draw LINE2\ninactive.info Memory not currently used.\n");
		}
		if (meminfo.containsKey("Inact_dirty")) {
			output.append("inact_dirty.label inactive_dirty\ninact_dirty.draw LINE1\ninact_dirty.info Memory not currently used, but in need of being written to disk.\n");
		}
		if (meminfo.containsKey("Inact_laundry")) {
			output.append("inact_laundry.label inactive_laundry\ninact_laundry.draw LINE1\n");
		}
		if (meminfo.containsKey("Inact_clean")) {
			output.append("inact_clean.label inactive_clean\ninact_clean.draw LINE1\ninact_clean.info Memory not currently used.\n");
		}
		return output.toString();
	}
	public String getUpdate(){
		Map<String, String> meminfo = new HashMap<String, String>();
		Pattern meminfo_regex = Pattern.compile("([:\\s]+)");
		try {
			BufferedReader in = new BufferedReader(new FileReader("/proc/meminfo"));
			String str;
			while ((str = in.readLine()) != null) {
				String[] items = meminfo_regex.split(str);
				Long data = Long.parseLong(items[1])*1024;
				meminfo.put(items[0], data.toString());
			}
			in.close();
		}
		catch (IOException e) {}
		StringBuffer output = new StringBuffer();

		if (meminfo.containsKey("Slab")){
			output.append("slab.value "+meminfo.get("Slab")+"\n");
		}
		else {
			output.append("slab.value 0\n");
		}
		if (meminfo.containsKey("SwapCached")) {
			output.append("swap_cache.value "+meminfo.get("SwapCached")+"\n");
		}
		else {
			output.append("swap_cache.value 0\n");
		}
		if (meminfo.containsKey("PageTables")) {
			output.append("page_tables.value "+meminfo.get("PageTables")+"\n");
		}
		else {
			output.append("page_tables.value 0\n");
		}
		if (meminfo.containsKey("VmallocUsed")){
			output.append("vmalloc_used.value "+meminfo.get("VmallocUsed")+"\n");
		}
		else{
			output.append("vmalloc_used.value 0\n");
		}
		Long appvalue = Long.parseLong(meminfo.get("MemTotal"))
		-Long.parseLong(meminfo.get("MemFree"))
		-Long.parseLong(meminfo.get("Buffers"))
		-Long.parseLong(meminfo.get("Cached"))
		-Long.parseLong(meminfo.get("Slab"))
		-Long.parseLong(meminfo.get("PageTables"))
		-Long.parseLong(meminfo.get("SwapCached"));
		output.append("apps.value "+appvalue+"\n");
		output.append("free.value "+meminfo.get("MemFree")+"\n");
		output.append("buffers.value "+meminfo.get("Buffers")+"\n");
		output.append("cached.value "+meminfo.get("Cached")+"\n");
		output.append("swap.value "+((Long.parseLong(meminfo.get("SwapTotal")))-(Long.parseLong(meminfo.get("SwapFree"))))+"\n");
		if (meminfo.containsKey("Committed_AS")) {
			output.append("committed.value "+meminfo.get("Committed_AS")+"\n");
		}
		if (meminfo.containsKey("Mapped")) {
			output.append("mapped.value "+meminfo.get("Mapped")+"\n");
		}
		if (meminfo.containsKey("Active")) {
			output.append("active.value "+meminfo.get("Active")+"\n");
		}
		if (meminfo.containsKey("ActiveAnon")) {
			output.append("active_anon.value "+meminfo.get("ActiveAnon")+"\n");
		}
		if (meminfo.containsKey("ActiveCache")) {
			output.append("active_cache.value "+meminfo.get("ActiveCache")+"\n");
		}
		if (meminfo.containsKey("Inactive")) {
			output.append("inactive.value "+meminfo.get("Inactive")+"\n");
		}
		if (meminfo.containsKey("Inact_dirty")) {
			output.append("inact_dirty.value "+meminfo.get("Inact_dirty")+"\n");
		}
		if (meminfo.containsKey("Inact_laundry")) {
			output.append("inact_laundry.value "+meminfo.get("Inact_laundry")+"\n");
		}
		if (meminfo.containsKey("Inact_clean")) {
			output.append("inact_clean.value "+meminfo.get("Inact_clean")+"\n");
		}
		return output.toString();

	}
}
package net.alkalus.changelog;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import gtPlusPlus.core.lib.CORE;
import net.alkalus.api.objects.data.AutoMap;
import net.alkalus.api.objects.misc.AcLog;
import net.alkalus.core.util.Utils;
import net.alkalus.core.util.data.ArrayUtils;
import net.alkalus.core.util.data.FileUtils;

public class MainClass {

	private static final MainClass instance;
	private boolean running = false;
	private final String versionString;
	
	static {
		instance = new MainClass(CORE.VERSION);		
	}
	
	public MainClass(String aVersionString) {		
		running = true;
		versionString = aVersionString;		
	}
	
	private void run() {
		AutoMap<String> aTemp = gatherChangelogData() ;
		aDataSet = organiseMasterData(aTemp);		
		if (aDataSet.isEmpty()) {
			stop();
		}
		else {
			processDataIntoNewFile();
		}
	}
	
	public static void main(String[] args) {
		if (instance != null) {
			instance.run();
			while (instance.running) {
				if (!instance.running) {
					break;
				}
			}
		}
	}
	
	private void stop() {
		stop(1);
	}
	
	private void stop(int code) {
		if (code > 0) {
			AcLog.INFO("Error in Changelog script, stopping.");			
		}
		running = false;
		System.exit(0);		
	}
	
	
	private HashMap<String, AutoMap<String>> aDataSet = new HashMap<String, AutoMap<String>>();
	
	
	/**
	 * Obtains the changelog specified via the version string on startup.
	 * @return - An AutoMap, populated with everything from the changelog.
	 */
	private AutoMap<String> gatherChangelogData() {
		//CHANGELOG Basic 1.7.02.89-debug.txt
		File aChangelog = FileUtils.getFile("D:/Coding/Java/Projects/GTplusplus", "CHANGELOG Basic "+versionString, "txt");
		if (aChangelog == null) {
			AcLog.INFO("Did not find basic changelog for version "+versionString+", Consider updating CORE.VERSION in GT++.");
			stop();
			return null;
		}
		else {
			AcLog.INFO("Found Changelog to sort for version "+versionString+".");
			return FileUtils.readLines(aChangelog);			
		}	
	}
	
	/**
	 * Sorts the provided AutoMap into 5 seperate HashMaps, then combines this for the result.
	 * @return - A HashMap, containing 5 inner Maps. These inner maps are keyed as (+, -, %, $, ?), 
	 * These symbols represent Additions, Removals, Changes, Fixes and Other. Other is data unable to be sorted.
	 */
	private LinkedHashMap<String, AutoMap<String>> organiseMasterData(AutoMap<String> aData) {

		AutoMap<String> additions = new AutoMap<String>();
		AutoMap<String> removals = new AutoMap<String>();
		AutoMap<String> changes = new AutoMap<String>();
		AutoMap<String> fixes = new AutoMap<String>();
		AutoMap<String> other = new AutoMap<String>();
		
		
		
		for (String g : aData) {
			g = g.trim();
			if (g != null && g.length() > 0) {			
				
				/*
				 * String Formatting Fixes
				 */
				//Weird Locale issue caused by Changelog generator, let's ammend.
				if (g.contains("&#39;")) {
					g = g.replaceAll("&#39;", "\'");
				}
				if (g.contains("&gt;")) {
					g = g.replaceAll("&gt;", ">");
				}
				if (g.contains("&amp;")) {
					g = g.replaceAll("&amp;", "&");
				}			
				
				if (g.startsWith("+")) {
					g = g.substring(2);
					additions.put(g);
					continue;
				}
				else if (g.startsWith("-")) {
					g = g.substring(2);
					removals.put(g);
					continue;
				}
				else if (g.startsWith("%")) {
					g = g.substring(2);
					changes.put(g);
					continue;
				}
				else if (g.startsWith("$")) {
					g = g.substring(2);
					fixes.put(g);
					continue;
				}
				else {
					other.put(g);
					continue;					
				}
			}			
		}

		additions = new AutoMap<String>(ArrayUtils.sortMapByValues(additions.getAsMap()));
		removals = new AutoMap<String>(ArrayUtils.sortMapByValues(removals.getAsMap()));
		changes = new AutoMap<String>(ArrayUtils.sortMapByValues(changes.getAsMap()));
		fixes = new AutoMap<String>(ArrayUtils.sortMapByValues(fixes.getAsMap()));
		other = new AutoMap<String>(ArrayUtils.sortMapByValues(other.getAsMap()));	
		
		LinkedHashMap<String, AutoMap<String>> aValue = new LinkedHashMap<String, AutoMap<String>>();
		aValue.put("+", additions);
		AcLog.INFO("Sorted "+additions.size()+" additions.");
		aValue.put("-", removals);
		AcLog.INFO("Sorted "+removals.size()+" removals.");
		aValue.put("%", changes);
		AcLog.INFO("Sorted "+changes.size()+" changes.");
		aValue.put("$", fixes);
		AcLog.INFO("Sorted "+fixes.size()+" fixes.");
		aValue.put("?", other);	
		AcLog.INFO("Sorted "+other.size()+" other.");	
		return aValue;
	}
	
	/**
	 * Processes stored data, outputs a file.
	 */
	private void processDataIntoNewFile() {
				
		if (aDataSet.isEmpty()) {
			stop();
		}
		else {			
			int aSegment = 0;
			
			File aNewChangelog;			
			if (FileUtils.removeFile("CHANGELOG_SORTED_"+versionString, "txt")) {
				AcLog.INFO("Found existing changelog, removing.");
			}
			aNewChangelog = FileUtils.createFile("CHANGELOG_SORTED_"+versionString, "txt");			
			
			AutoMap<String> aInfo = new AutoMap<String>();
			AutoMap<String> aLinebreak = new AutoMap<String>();
			String[] aTitles = new String[] {"Additions", "Removals", "Changes", "Fixes", "Other", "", "", ""};
			String aDate = Utils.getCurrentTimeAndDate();
			String aBlankLine = "";
			String aDashLine = "==================================================================================";

			aLinebreak.put(aBlankLine);
			aLinebreak.put(aDashLine);
			aLinebreak.put(aBlankLine);

			aInfo.put("GT++ "+versionString+" | Sorted Changelog");
			aInfo.put("Generated at: "+aDate);
			aInfo.put(aBlankLine);
			aInfo.put(aBlankLine);
			
			FileUtils.appendListToFile(aNewChangelog, aInfo);
			for (AutoMap<String> h : aDataSet.values()) {
				FileUtils.appendListToFile(aNewChangelog, aLinebreak);
				FileUtils.appendLineToFile(aNewChangelog, aTitles[aSegment]);
				FileUtils.appendListToFile(aNewChangelog, aLinebreak);					
				FileUtils.appendListToFile(aNewChangelog, h);			
				aSegment++;
			}
			FileUtils.appendListToFile(aNewChangelog, aLinebreak);			
		}	
		AcLog.INFO("Finished, Changelog generated.");
		stop(0);
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}

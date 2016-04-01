package com.ibm.zos.svt.units;

import java.util.ArrayList;
import java.util.Hashtable;

import com.ibm.zos.svt.JobRunner;
import com.ibm.zos.svt.StringPair;

/**
 * The ProjectX run context
 * @author Stone WANG
 *
 */
public class Context {
	//The current run directory
	public static ArrayList<StringPair> Directory = new ArrayList<StringPair>();
	//The current run jobs
	public static ArrayList<JobRunner> runJobs = new ArrayList<JobRunner>();
	//A flag indicates if ProjectX need to exit
	public static boolean Exit = false;
	//A flag indicates if there's a logger error now
	public static boolean LoggerError = false;
	//The ProjectX settings
	public static Hashtable<String, String> Settings = new Hashtable<String,String>();
	
	/**
	 * The top unit of the current directory
	 * @return	The top unit
	 */
	public static StringPair top() {
		if(Directory.size() == 0 )
			return null;
		else
			return Directory.get(0);
	}
	
	/**
	 * Pops up the top of the current directory and return
	 * @return	The top unit
	 */
	public static StringPair pop() {
		StringPair topNode = top();
		if(topNode != null) {
			Directory.remove(0);
		}
		return topNode;
	}
}

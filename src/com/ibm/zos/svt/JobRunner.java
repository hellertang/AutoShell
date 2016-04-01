package com.ibm.zos.svt;

import java.util.ArrayList;

import com.ibm.zos.svt.units.*;

/**
 * Runner of Job
 * @author Stone WANG
 *
 */
public class JobRunner extends Thread {
	public String fileName;
	public ArrayList<String> args = new ArrayList<String>();
	public int status;
	public boolean run = false; 
	private Unit unit;
	
	/**
	 * Create a new JobRunner
	 * @param unit
	 */
	public JobRunner(Unit unit) {
		this.unit = unit;
	}
	
	/**
	 * Run the thread
	 */
	public void run() {
		unit.run();
	} 
}

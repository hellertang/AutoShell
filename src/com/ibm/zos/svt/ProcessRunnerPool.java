package com.ibm.zos.svt;

import java.util.ArrayList;

import com.ibm.zos.svt.units.Context;

/**
 * A pool of ProcessRunners
 * @author Stone WANG
 *
 */
public class ProcessRunnerPool {
	private ArrayList<ProcessRunner> runners = new ArrayList<ProcessRunner>();
	
	/**
	 * Create a new ProcessRunnerPool
	 */
	public ProcessRunnerPool() {
		//Set the debug port of ProcessRunner if exist
		String debugPort = Context.Settings.get("DebugPort");
		if((debugPort != null) && (debugPort != "")) {
			ProcessRunner.debugPort = Integer.parseInt(debugPort);
		}
	}
	
	/**
	 * Run a Process with a new created ProcessRunner
	 * @param procName	The name of the Process
	 * @param args	The arguments of the Process
	 */
	public void runProcess(String procName, ArrayList<String> args) {
		ProcessRunner runner = new ProcessRunner(procName,args);
		runner.start();
		//Add the created ProcessRunner to the pool
		runners.add(runner);
	}
	
	/**
	 * Check if all ProcessRunners finish run
	 * @return	True if all finished, otherwise false
	 */
	public boolean allDone() {
		boolean flag = true;
		for(int i=0; i <runners.size(); i++) {
			if(runners.get(i).isAlive()) {
				flag = false;
			}
		}
		return flag;
	}
	
	/**
	 * Terminate all ProcessRunners
	 */
	public void terminate()
	{
		for(int i=0; i <runners.size(); i++) {
			if(runners.get(i).run) {
				runners.get(i).terminate();
			}
		}
	}
}
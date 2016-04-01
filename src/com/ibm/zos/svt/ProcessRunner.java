package com.ibm.zos.svt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.ibm.zos.svt.units.Context;

/**
 * Runner of Process
 * @author Stone WANG
 *
 */
class ProcessRunner extends Thread {
	public String procName;
	public ArrayList<String> args = new ArrayList<String>();
	public Process proc;
	public int status;
	public boolean run = false;
	public static int debugPort = 0;
	private Logger logger = Logger.getConsoleLogger();
	
	/**
	 * Create a new ProcessRunner
	 */
	public ProcessRunner() {
		run = false;
	}
	
	/**
	 * Create a new ProcessRunner
	 * @param procName	The name of the Process
	 * @param args		The arguments of the Process
	 */
	public ProcessRunner(String procName, ArrayList<String> args) {
		this.args.addAll(args);
		this.procName = procName;
		run = true;
	}
	
	/**
	 * Run the thread
	 */
	public void run() {
		List<String> list = new ArrayList<String>();
		//Java
		String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
	    list.add(java);
	    //Debug options if debug port provided
		if(debugPort > 0) {
			list.add("-Xdebug");
		    list.add("-Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=" + (++debugPort));
		}
		//JVM options
		//Default JVM options is "-Xms10m -Xmx10m"
	    String jvmOptions = Context.Settings.get("JVMOptions");	    
	    if(jvmOptions == null)
	    	jvmOptions = "-Xms10m -Xmx10m";
		if((jvmOptions != null) && (jvmOptions != "")) {
			String[] options = jvmOptions.split(" +");
			for(int i = 0 ; i < options.length; i++) {
				list.add(options[i]);
			}
		}
		
		//Class Path
		String classpath = System.getProperty("java.class.path");
	    list.add("-classpath");
	    list.add(classpath);
	    //Java class
	    list.add(ProcessMain.class.getName());
	    //Java class arguments
	    for(int i=0;i<args.size(); i++) {
	    	list.add(args.get(i));
	    }
	    
		ProcessBuilder builder = new ProcessBuilder(list);
		builder.redirectErrorStream(true);
		BufferedReader inputStream = null;
		BufferedReader errorStream = null;
		String line = "";
		try {
			proc = builder.start();
			inputStream = new BufferedReader(
					new InputStreamReader(proc.getInputStream()));
			errorStream = new BufferedReader(
					new InputStreamReader(proc.getErrorStream()));
			while(true) {
				try {
					status = proc.exitValue();
					break;
				} catch(IllegalThreadStateException ex) {
					if(proc.getErrorStream().available() > 0) {
						while((line = errorStream.readLine()) != null) {
							//If the count of bytes in the stream is larger than 10000, it means the log speed is slow. 
							//We will read and throw the bytes of the stream.
							if(line != "" && Logger.LogConsole && (proc.getErrorStream().available() < 10000))
								logger.norm(line);
						}
					}
					if(proc.getInputStream().available() > 0) {
						while((line = inputStream.readLine()) != null) {
							//If the count of bytes in the stream is larger than 10000, it means the log speed is slow. 
							//We will read and throw the bytes of the stream.
							if(line != "" && Logger.LogConsole && (proc.getInputStream().available() < 10000))
								logger.norm(line);
						}
					}
				}
			}
		} catch (IOException e1) {
			// Child process already exited
			;
		} 
		//Close the streams
		try {
			if(inputStream != null)
				inputStream.close();
			if(errorStream != null)
				errorStream.close();
		} catch (IOException e1) {
			// Child process already exited
			;
		} 
	} 
	
	/**
	 * Terminate the Process
	 */
	public void terminate() {
		if(proc != null) {
			try {
				proc.exitValue();
			} catch(IllegalThreadStateException ex) {
				logger.norm("Process with name " + this.procName +  " is going to be terminated by ProjectX.");
				BufferedWriter bw = new BufferedWriter(
						new OutputStreamWriter(proc.getOutputStream()));
				try {
					//This
					bw.write("Terminate");
					bw.newLine();
					bw.close();
				} catch (IOException e) {
					//Process may already finish run
					logger.warn("Process with name " + this.procName + " already exit.");
				}
			}
			
			run = false;
		}
	}
}

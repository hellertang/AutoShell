package com.ibm.zos.svt.steps;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.ibm.zos.svt.*;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of execute executable file
 * @author Stone WANG
 *
 */
public class Exec extends AbstractStep{
	private String[] parms = null;
	private int status = -1;

	/**
	 * Create a new Exec
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public Exec(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Run the step
	 * @param params The parameters of the step
	 * @return True if success, otherwise false
	 */
	public boolean run(String params) {
		String[] parms = params.split(" +");
		
		if((parms == null)) {
			logger.error("Invalid parameters for Exec Step:" + params + ".");
			logger.error("Executable file path must be provided.");
			return false;
		}
		this.parms = parms;
		
		List<String> list = new ArrayList<String>();
		for(int i =0 ;i < parms.length; i++) {
			list.add(parms[i]);
		}
		
		ProcessBuilder builder = new ProcessBuilder(list);
		builder.redirectErrorStream(true);
		try {
				Process proc = builder.start();
				status = proc.waitFor();
				logger.norm("Status:" + status);
				//Print out the standard output of the executable file
				if(proc.getInputStream().available() > 0) {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(proc.getInputStream()));
					String line = "";
					while((line = br.readLine()) != null) {
						logger.norm(line);
					}
					br.close();
				}
				proc.getOutputStream().close();
			} catch (IOException e1) {
				logger.error("Exec run met IO Exception.");
				logger.debug(e1.getMessage());
				return false;
			} catch (InterruptedException e) {
				logger.error("Exec is terminated externally.");
				logger.debug(e.getMessage());
				return false;
			}
		return true;
	}

	/**
	 * Validate the step
	 * @return True if success, otherwise false
	 */
	public boolean validate() {
		if(status != 0) {
			logger.error("Exec run failure.");
			return false;
		} else { 
			logger.norm("Exec run successfully.");
			return true;
		}
	}
}

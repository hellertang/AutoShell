package com.ibm.zos.svt.steps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.ibm.zos.svt.Logger;

/**
 * Abstract class for steps
 * @author Stone WANG
 *
 */
public abstract class AbstractStep {
	/**
	 * Run the step with parameters.
	 * @param parms	The step parameters.
	 * @return	true if success, false otherwise.
	 */
	public abstract boolean run(String parms);
	
	/**
	 * Validate the step.
	 * @return true if success, false otherwise.
	 */
	public abstract boolean validate();
	
	/**
	 * Execute the command.
	 * @param cmd	The command to execute.
	 * @param printError	The switch to control if messages will be printed out for errors.
	 * @return	The lines of string of the command standard output.
	 */
	protected String[] shell(String cmd, boolean printError)
	{
		try {
			logger.debug("shell command:" + cmd);
			//Execute command
			Process proc = Runtime.getRuntime().exec(cmd);
			//Print error messages if exist
			if(proc.waitFor() != 0) {
				if(printError && proc.getErrorStream().available() > 0) {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(proc.getErrorStream()));
					String line = "";
					while((line = br.readLine()) != null) {
						logger.error(line);
					}
					br.close();
				}
				return null;
			}
			//Collect standard output messages and return
			ArrayList<String> outputLines = new ArrayList<String>();
			if(proc.getInputStream().available() > 0) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));
				String line = "";
				while((line = br.readLine()) != null) {
					outputLines.add(line);
				}
				br.close();
			}
			return outputLines.toArray(new String[0]);
		} catch (InterruptedException e) {
			logger.error("Shell command :" + cmd + " is interrupted.");
			return null;
		} catch (IOException e) {
			logger.error("Shell command :" + cmd + " met IO Exception.");
			logger.error(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Execute a command by "sh -c".
	 * @param cmd	The command to execute.
	 * @param printError The switch to control if messages will be printed out for errors.
	 * @return	The lines of string of the command standard output.
	 */
	protected String[] sh(String cmd, boolean printError)
	{
		try {
			logger.debug("sh command:" + cmd);
			//Execute command
			String[] cmdArray = new String[3];
			cmdArray[0] = "sh";
			cmdArray[1] = "-c";
			cmdArray[2] = cmd;
			Process proc = Runtime.getRuntime().exec(cmdArray);
			//Print error messages if exist
			if(proc.waitFor() != 0) {
				if(printError && proc.getErrorStream().available() > 0) {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(proc.getErrorStream()));
					String line = "";
					while((line = br.readLine()) != null) {
						logger.error(line);
					}
					br.close();
				}
				return null;
			}
			//Collect standard output messages and return
			ArrayList<String> outputLines = new ArrayList<String>();
			if(proc.getInputStream().available() > 0) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));
				String line = "";
				while((line = br.readLine()) != null) {
					outputLines.add(line);
				}
				br.close();
			}
			return outputLines.toArray(new String[0]);
		} catch (InterruptedException e) {
			logger.error("Shell command :" + cmd + " is interrupted.");
			return null;
		} catch (IOException e) {
			logger.error("Shell command :" + cmd + " met IO Exception.");
			logger.error(e.getMessage());
			return null;
		} 
	}
	
	/**
	 * Calculate the digits count of specific integer
	 * @param integer The integer to calculate
	 * @return	The digits count
	 */
	protected int getDigits(int integer) {
		int digits = 0;
		int remain = integer;
		while(remain > 0 ) {
			remain /= 10;
			++digits;
		}
		return digits;
	}
	
	/**
	 * The message logger
	 */
	protected Logger logger = null;
}

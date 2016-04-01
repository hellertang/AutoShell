package com.ibm.zos.svt.steps;

import java.io.File;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of create directory using shell command
 * @author Stone WANG
 *
 */
public class CreateDirEx extends AbstractStep{
	private String[] parms = null;
	
	/**
	 * Create a new CreateDirEx
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public CreateDirEx(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Create directory with specified path name and count
	 * @param dirName	The directory path name
	 * @param dirCount	The directory count
	 * @return	True if success, otherwise false
	 */
	private boolean createDir(String dirName, int dirCount) {
		//Check possible MVS attributes
		String attributes = "";
		int index = dirName.indexOf(",");
		if(index > 0) {
			attributes = dirName.substring(index);
			dirName = dirName.substring(0, index);
		}
		String curDirName = "";
		int digits = getDigits(dirCount);
		//Create directories in a loop until directory count reached
		for(int i = 0; i < dirCount; i++) {
			curDirName = dirName;
			if(dirCount > 1) { 
				curDirName += String.format("%0" + digits + "d", (i + 1));
			}
			//MVS attributes
			if(attributes != "") {
				curDirName += attributes;
			}
			File dir = new File(curDirName);
			//File or directory already exist
			if(dir.exists()) {
				if(dir.isFile()) {
					logger.error("File" + curDirName + " with same name exists.");
					return false;
				} else {
					logger.error("Directory " + curDirName + " already exists.");
					return false;
				}
			}

			//Create directory
			if(sh("mkdir \"" + curDirName + "\"", true) == null) {
				logger.error("Directory " + curDirName + "creation failed.");
				return false;
			}	
		}
		return true;
	}
	
	/**
	 * Run the step
	 * @param params The parameters of the step
	 * @return True if success, otherwise false
	 */
	public boolean run(String params) {
		String[] parms = params.split(" +");
		if((parms == null) || (parms.length < 1)) {
			logger.error("Invalid parameters for CreateFile Step:" + params + ".");
			logger.error("Directory Name must be provided.");
			return false;
		}
		this.parms = parms;
		
		String dirName = parms[0];
		int dirCount = 1;
		if(parms.length > 2) {
			dirCount = Integer.parseInt(parms[1]);
		}
		
		if(dirCount <= 0 ) {
			logger.error("Directory Count must be larger than 0.");
			return false;
		}
		
		return createDir(dirName, dirCount);
	}

	/**
	 * Validate the step
	 * @return True if success, otherwise false
	 */
	public boolean validate() {
		String dirName = parms[0];
		int dirCount = 1;
		if(parms.length > 1) {
			dirCount = Integer.parseInt(parms[1]);
		}
		int digits = getDigits(dirCount);
		//Ignore MVS attributes
		int index = dirName.indexOf(",");
		if(index > 0) {
			dirName = dirName.substring(0, index);
		}
		String curDirName = "";
		boolean flag = true;
		//Validate created directories in a loop until directory count reached
		for(int i = 0; i < dirCount; i++) {
			boolean innerFlag = true;
			curDirName = dirName;
			if(dirCount > 1) { 
				curDirName += String.format("%0" + digits + "d", (i + 1));
			}
			
			File dir = new File(curDirName);
			//Created directory doesn't exist
			if(!dir.exists()) {
				logger.error("Directory " + curDirName + " is not created.");
				flag = false;
				innerFlag = false;
			}
			//Created directory is not a directory
			if(!dir.isDirectory()) {
				logger.error(curDirName + " is not a directory.");
				flag = false;
				innerFlag = false;
			}
			if(innerFlag) {
				logger.norm("Directory " + curDirName + " is created successfully.");
			}
		}
		return flag;
	}
	
//	private boolean shell(String cmd)
//	{
//		try {
//			Process proc = Runtime.getRuntime().exec(cmd);
//			if(proc.waitFor() != 0) {
//				if(proc.getErrorStream().available() > 0) {
//					BufferedReader br = new BufferedReader(
//							new InputStreamReader(proc.getErrorStream()));
//					String line = "";
//					while((line = br.readLine()) != null) {
//						logger.error(line);
//					}
//					br.close();
//				}
//				return false;
//			}
//			return true;
//		} catch (InterruptedException e) {
//			logger.error("Shell command :" + cmd + " is interrupted.");
//			return false;
//		} catch (IOException e) {
//			logger.error("Shell command :" + cmd + " met IO Exception.");
//			logger.error(e.getMessage());
//			return false;
//		}
//	}
}

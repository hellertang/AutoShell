package com.ibm.zos.svt.steps;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of delete file using shell command
 * @author Stone WANG
 *
 */
public class DeleteFileEx extends AbstractStep{
	private String[] parms = null;

	/**
	 * Create a new DeleteFileEx
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public DeleteFileEx(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Delete files with the specified file path
	 * @param filePath	The full file path
	 * @return	True if success, otherwise false
	 */
	private boolean deleteFiles(String filePath) {
		
		if(sh("rm -r " + filePath, true) == null) {
			logger.error("Delete file " + filePath + " failed.");
			return false;
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
			logger.error("Invalid parameters for DeleteFile Step:" + params + ".");
			logger.error("File name must be provided.");
			return false;
		}
		this.parms = parms;	
		String fileName = parms[0];
		return deleteFiles(fileName);
	}
	
	/**
	 * Validate the step
	 * @return True if success, otherwise false
	 */
	public boolean validate() {
		String filePath = parms[0];
		String[] lines = shell("ls -al " + filePath, false);
		if((lines == null) || (lines.length == 0)) {
			logger.norm(filePath + " is deleted successfully.");
			return true;
		} else {
			logger.error(filePath + " is not deleted but still on disk.");
			return false;
		}
	}
}

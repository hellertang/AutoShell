package com.ibm.zos.svt.steps;

import java.io.*;
import java.util.regex.*;

import com.ibm.zos.svt.*;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of delete file using Java API
 * @author Stone WANG
 *
 */
public class DeleteFile extends AbstractStep{
	private String[] parms = null;

	/**
	 * Create a new DeleteFile
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public DeleteFile(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Delete file with the specified file path name
	 * @param fileName	The file full path name
	 * @return	True if success, otherwise false
	 */
	private boolean deleteFile(String fileName) {
		File file = new File(fileName);
		//File doesn't exist
		if(!file.exists()) {
			logger.error("Delete file " + file.getAbsolutePath() + " failed.");
			logger.error("File " + file.getAbsolutePath() + "doesn't exist.");
			return false;
		}
		//File is not a file
		if(!file.isFile()) {
			logger.error("Delete file " + file.getAbsolutePath() + " failed.");
			logger.error(file.getAbsolutePath() + " is not a file.");
			return false;
		}
		//Delete the file
		if(!file.delete()) {
			logger.error("Delete file " + file.getAbsolutePath() + " failed.");
			return false;
		} else {
			logger.norm("Delete file " + file.getAbsolutePath() + " succeeded.");
			return true;
		}
	}
	
	/**
	 * Delete directories with specified path name
	 * @param dirName	The directory path name
	 * @param regExp	The wildchar string
	 * @param deleteSelf	Indicates if delete directory itself
	 * @return	True if success, otherwise false
	 */
	private boolean deleteDir(String dirName, String regExp, boolean deleteSelf) {
		File dirFile = new File(dirName);
		File[] files = dirFile.listFiles();
		Pattern pattern = null;
		if(regExp != "") {
			pattern = Pattern.compile(regExp);
		}
		for(int i = 0 ;i < files.length; i++) {
			//Ignore file names that doesn't match the wildchar string
			if(regExp != "") {
				if(!pattern.matcher(files[i].getName()).find()) {
					continue;
				}
			}
			//files[i] is a file, delete it
			if(files[i].isFile()) {
				deleteFile(files[i].getAbsolutePath());
			//files[i] is a directory, delete it
			} else {
				deleteDir(files[i].getAbsolutePath(), "", true);
			}
		}
		//Return directly if don't need to delete directory itself
		if(!deleteSelf)
			return true;
		//Delete directory itself
		if(!dirFile.delete()) {
			logger.error("Delete directory " + dirFile.getAbsolutePath() + " failed.");
			return false;
		} else {
			logger.norm("Delete directory " + dirFile.getAbsolutePath() + " succeeded.");
			return true;
		}
	}
	
	/**
	 * Delete files with specified file path name
	 * @param fileName	The file full path name
	 * @return	True if success, otherwise false
	 */
	private boolean deleteFiles(String fileName) {
		String regExp = "";
		File file = new File(fileName);
		boolean deleteSelf = true;
		//Check if Wildchar * is contained
		String[] tokens = fileName.split(File.separator.replaceAll("\\\\", "\\\\\\\\"));
		String lastPart = tokens[tokens.length - 1];
		if(lastPart.contains("*")) {
			fileName = fileName.substring(0, fileName.lastIndexOf(File.separator) + 1);
			regExp = lastPart;
			file = new File(fileName);
			deleteSelf = false;
		}
		//File doesn't exist
		if(!file.exists()) {
			logger.error("Delete file " + fileName + " failed.");
			logger.error("File " + fileName + " doesn't exist.");
			return false;
		}
		if(regExp != "") {
			regExp = "^" + regExp + "$";
			regExp = regExp.replaceAll("[*]", ".*");
		}
		if(file.isDirectory()) {
			return deleteDir(fileName, regExp, deleteSelf);
		} else {
			return deleteFile(fileName);
		}
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
		String fileName = parms[0];
		File file = new File(fileName);
		String regExp = "";
		boolean flag = true;
		boolean deleteSelf = true;
		//Check if Wildchar * is contained
		String[] tokens = fileName.split(File.separator.replaceAll("\\\\", "\\\\\\\\"));
		String lastPart = tokens[tokens.length - 1];
		if(lastPart.contains("*")) {
			String dirName = fileName.substring(0, fileName.lastIndexOf(File.separator) + 1);
			regExp = lastPart;
			file = new File(dirName);
			deleteSelf = false;
		}
		
		if(regExp != "") {
			regExp = "^" + regExp + "$";
			regExp = regExp.replaceAll("[*]", ".*");
		}
		//Check if file itself is deleted
		if(deleteSelf) {
			if(file.exists()) {
				logger.error("File " + fileName + " is not deleted but still on disk.");
				return false;
			} else {
				logger.norm("File " + fileName + " is deleted successfully.");
			}
		//Check if the wildchar matched files are all deleted
		} else {
			File[] files = file.listFiles();
			Pattern pattern = null;
			if(regExp != "") {
				pattern = Pattern.compile(regExp);
			}
			for(int i = 0 ; i < files.length; i++) {
				//Check if regular expression matched
				if(regExp != "") {
					if(!pattern.matcher(files[i].getName()).find()) {
						continue;
					}
				}
				//files[i] doesn't exist. It's deleted successfully.
				if(files[i].exists()) {
					logger.error("File " + files[i].getAbsolutePath() + " is not deleted but still on disk.");
					flag = false;
				} else {
					logger.norm("File " + files[i].getAbsolutePath() + " is deleted successfully.");
				}
			}
		}
		return flag;
	}
}

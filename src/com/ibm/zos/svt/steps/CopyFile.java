package com.ibm.zos.svt.steps;

import java.io.*;
import java.util.regex.*;

import com.ibm.zos.svt.*;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of copy files using JAVA API
 * @author Stone WANG
 *
 */
public class CopyFile extends AbstractStep{
	private String[] parms = null;
	private boolean targetExist = false;
	
	/**
	 * Create a new CopyFile
	 * @param location The step location
	 * @param parent	The step's parent unit
	 */
	public CopyFile(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Copy directories from source to destination
	 * @param src	The source directory path
	 * @param dst	The destination directory path
	 * @param srcReg	The wildchar string of source directory
	 * @return	True for success, otherwise false
	 */
	private boolean copyDir(String src, String dst, String srcReg) {
		//Parse to retrieve MVS attributes if exist
		String attributes = "";
		int index = dst.indexOf(",");
		if(index > 0) {
			attributes = dst.substring(index);
			dst = dst.substring(0, index);
		}
		File srcDir = new File(src);
		File dstDir = new File(dst + attributes);
		//Source directory doesn't exist
		if(!srcDir.exists()) {
			logger.error("Source directory " + src + " doesn't exist.");
			return false;
		}
		//Source is not a directory
		if(!srcDir.isDirectory()) {
			logger.error("Source " + src + " is not a directory.");
			return false;
		}
		//Destination directory doesn't exist
		if(!dstDir.exists()) {
			//Create the destination directory
			if(!dstDir.mkdir()) {
				logger.error("Destination directory " + dstDir.getAbsolutePath() + " creation failed.");
				return false;
			}
		}
		//Destination is not a directory
		if(!dstDir.isDirectory()) {
			logger.error("Destination " + dst + " is not a directory.");
			return false;
		}
		
		//Replace '*' with ".*"
		if(srcReg!="") {
			srcReg = "^" + srcReg + "$";
			srcReg = srcReg.replaceAll("[*]", ".*");
		}
		
		//Copy files and directories under source directory
		String[] files = srcDir.list();
		Pattern pattern = null;
		if(srcReg != "") {
			pattern = Pattern.compile(srcReg);
		}
		for (int i =0; i < files.length; i++) {
			//Ignore files that don't match the wildchar string
			if(srcReg != "") {
				if(!pattern.matcher(files[i]).find()) {
					continue;
				}
			}
			File file = new File(src + File.separator + files[i]);
			//files[i] is a directory
			if(file.isDirectory()) {
				String newDst = dst;
				if(attributes != "") {
					newDst += attributes;
				}
				if(!copyDir(file.getAbsolutePath(), newDst, "")) {
					return false;
				}
			//files[i] is a file
			} else {
				String newDst = dst + File.separator + files[i];
				if(attributes != "") {
					newDst += attributes;
				}
				if(!copyFile(file.getAbsolutePath(), newDst)) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Copy file from source to destination
	 * @param src	The source file path
	 * @param dst	The destination file path
	 * @return	True if success, otherwise false
	 */
	private boolean copyFile(String src, String dst) {
		try {
			//Parse to retrieve MVS attributes if exist
			String attributes = "";
			int index = dst.indexOf(",");
			if(index > 0) {
				attributes = dst.substring(index);
				dst = dst.substring(0, index);
			}
			File srcFile = new File(src);
			File dstFile = new File(dst);
			//Source file doesn't exist
			if(!srcFile.exists()) {
				logger.error("Source file " + src + " doesn't exist.");
				return false;
			}
			//Source is not a file
			if(!srcFile.isFile()) {
				logger.error("Source " + src + " is not a file.");
				return false;
			}
			//Destination exist and is a directory, re-construct destination file path
			if(dstFile.exists() && dstFile.isDirectory()) {
				dst += File.separator + srcFile.getName();
			} 
			//Re-create file object to append the MVS dataset attributes
			if(attributes != "") {
				dst += attributes;
				dstFile = new File(dst);
			}
			
			//Copy source file content to destination
			FileInputStream in = new FileInputStream(srcFile);
			FileOutputStream out = new FileOutputStream(dstFile);
			int length = 1440;
			byte[] buffer = new byte[length];
			while(true) {
				int bytes = in.read(buffer);
				if(bytes == -1) {
					break;
				}
				out.write(buffer, 0, bytes);
			}
			in.close();
			out.close();
			
		} catch (IOException e) {
			logger.error("Copy file " + src + " to " + dst + " met IO Exception.");
			return false;
		}
		logger.norm("Copy file " + src + " to " + dst + " succeeded.");
		return true;
	}
	
	/**
	 * Copy files or directory from source to destination
	 * @param srcPath	The source file or directory path
	 * @param dstPath	The destination file or directory path
	 * @return	True if success, otherwise false
	 */
	private boolean copyFiles(String srcPath, String dstPath)
	{
		//Parse to retrieve the wildchar string if exist
		String[] tokens = srcPath.split(File.separator.replaceAll("\\\\", "\\\\\\\\"));
		String lastPart = tokens[tokens.length - 1];
		String regExp = "";
		if(lastPart.contains(("*"))) {
			srcPath = srcPath.substring(0, srcPath.lastIndexOf(File.separator) + 1);
			regExp = lastPart;
		}
		
		//Parse to retrieve the MVS attributes if exist
		String attributes = "";
		int index = dstPath.indexOf(",");
		if(index > 0) {
			attributes = dstPath.substring(index);
			dstPath = dstPath.substring(0, index);
		}
				
		//Check source for existent
		File srcFile = new File(srcPath);
		if(!srcFile.exists()) {
			logger.error("Source " + srcFile.getAbsolutePath() + " doesn't exist.");
			return false;
		}
		
		//Check destination for existent
		File dstFile = new File(dstPath);
		
		if(dstFile.exists()) {
			targetExist = true;
		} else {
			targetExist = false;
		}
		
		//Source is a directory
		if(srcFile.isDirectory()) {
			//Source is a directory and target already exist, re-construct destination path
			//Example, /u/src to /u/dst, the constructed destination path will be /u/dst/src
			if((regExp == "") && targetExist) {
				dstPath = dstPath + File.separator + (new File(srcPath)).getName();
			}
			return copyDir(srcPath,dstPath + attributes, regExp);
		//Source is a file
		} else {
			return copyFile(srcPath,dstPath + attributes);
		}
	}
	
	/**
	 * Run the step
	 * @param params	The step parameters
	 * @return	True if success, otherwise false
	 */
	public boolean run(String params) {
		String[] parms = params.split(" +");
		if((parms == null) || (parms.length != 2)) {
			logger.error("Invalid input for CopyFile Step:" + params + ".");
			logger.error("Source Path and Destination Path must be provided.");
			return false;
		}
		this.parms = parms;
		
		String srcPath = parms[0];
		String dstPath = parms[1];
		return copyFiles(srcPath, dstPath);
	}

	/**
	 * Validate the directory copy
	 * @param src	The source directory path
	 * @param dst	The destination directory path
	 * @param regExp	The wildchar string of the source
	 * @return	True if success, otherwise false
	 */
	private boolean validateDir(String src, String dst, String regExp) {
		//Trim MVS attributes because we can't check them
		int index = dst.indexOf(",");
		if(index > 0) {
			dst = dst.substring(0, index);
		}
		File srcDir = new File(src);
		File dstDir = new File(dst);
		//Source directory doesn't exist
		if(!srcDir.exists()) {
			logger.error("Source directory " + src + " doesn't exist.");
			return false;
		}
		//Source is not a directory
		if(!srcDir.isDirectory()) {
			logger.error("Source " + src + " is not a directory.");
			return false;
		}
		//Destination directory doesn't exist
		if(!dstDir.exists()) {
			logger.error("Destination directory " + dst + " doesn't exist.");
			return false;
		}
		//Destination is not a directory
		if(!dstDir.isDirectory()) {
			logger.error("Destination " + dst + " is not a directory.");
			return false;
		}
		
		//Replace '*' with ".*"
		if(regExp!="") {
			regExp = "^" + regExp + "$";
			regExp = regExp.replaceAll("[*]", ".*");
		} 
		
		//Copy files and directories under source directory
		String[] files = srcDir.list();
		Pattern pattern = null;
		if(regExp != "") {
			pattern = Pattern.compile(regExp);
		}
		for (int i =0; i < files.length; i++) {
			//Ignore files that don't match the wildchar string
			if(regExp != "") {
				if(!pattern.matcher(files[i]).find()) {
					continue;
				}
			}
			File file = new File(src + File.separator + files[i]);
			//files[i] is a directory
			if(file.isDirectory()) {
				String newDst = dst;
				if(!validateDir(file.getAbsolutePath(), newDst, "")) {
					return false;
				}
			//files[i] is a file
			} else {
				String newDst = dst + File.separator + files[i];
				if(!validateFile(file.getAbsolutePath(), newDst)) {
					return false;
				}
			}
		}
		
		logger.norm("Directory " + src + " is copied successfully.");
		return true;
	}
	
	/**
	 * Validate the file copy
	 * @param src	The source file path
	 * @param dst	The destination file path
	 * @return	True if success, otherwise false
	 */
	private boolean validateFile(String src, String dst) {
		//Trim MVS attributes because we can't check them
		int index = dst.indexOf(",");
		if(index > 0) {
			dst = dst.substring(0, index);
		}
				
		File srcFile = new File(src);
		File dstFile = new File(dst);
		//Source file doesn't exist
		if(!srcFile.exists()) {
			logger.error("Source file " + src + " doesn't exist.");
			return false;
		}
		//Source is not a file
		if(!srcFile.isFile()) {
			logger.error("Source " + src + " is not a file.");
			return false;
		}
		//Destination file doesn't exist
		if(!dstFile.exists()) {
			logger.error("Destination file " + dst + " doesn't exist.");
			return false;
		} else {
			logger.debug("Source file " + src + " is copied to " + dst);
		}
		
		//Destination is a directory, re-construct destination file path.
		//Example, /u/src to /u/dst, constructed destination file path is /u/dst/src
		if(dstFile.isDirectory()) {
			dst += File.separator + srcFile.getName();
			dstFile = new File(dst);
		}
		
		//Check if file sizes are the same
		long srcFileLen = srcFile.length();
		long dstFileLen = dstFile.length();
		if(srcFileLen != dstFileLen) {
			logger.error("Source file " + srcFile.getAbsolutePath() + " size is " +
					srcFileLen + ",but destination file " + dstFile.getAbsolutePath() + 
					" size is " + dstFileLen + ".");
			return false;
		}
		
		//Check if file contents are the same
		try {
			FileInputStream srcStream = new FileInputStream(srcFile);
			FileInputStream dstStream = new FileInputStream(dstFile);
			byte[] srcBuf = new byte[1440];
			byte[] dstBuf = new byte[1440];
			while(true) {
				int srcBytes = srcStream.read(srcBuf);
				int dstBytes = dstStream.read(dstBuf);
				//Source file size is different with destination
				if(srcBytes != dstBytes) {
					logger.error("Source file " + srcFile.getAbsolutePath() + 
							" size is different with destination file " +
							 dstFile.getAbsolutePath() + ".");
					return false;
				}
				if((srcBytes == -1) || (dstBytes == -1)) {
					break;
				}
				//Source file content is different with destination
				for(int i = 0 ; i < srcBytes; i++) {
					if(srcBuf[i] != dstBuf[i]) {
						logger.error("Source file " + srcFile.getAbsolutePath() + 
								" content is different with destination file " +
								 dstFile.getAbsolutePath() + ".");
						return false;
					}
				}
			}
			srcStream.close();
			dstStream.close();
		}catch(IOException ex) {
			logger.error("Exception occured while validate files " + 
					srcFile.getAbsolutePath() + " and " + dstFile.getAbsolutePath() + ".");
			return false;
		}
		logger.debug("Source file " + src + " has the same content with copied file " + dst + ".");
		logger.norm("File " + src + " is copied successfully.");
		return true;
	}
	
	/**
	 * Validate the step
	 */
	public boolean validate() {
		String srcPath = parms[0];
		String dstPath = parms[1];
		String regExp = "";
		//Parse to retrieve wildchar string if exist
		String[] tokens = srcPath.split(File.separator.replaceAll("\\\\", "\\\\\\\\"));
		String lastPart = tokens[tokens.length - 1];
		if(lastPart.contains(("*"))) {
			srcPath = srcPath.substring(0, srcPath.lastIndexOf(File.separator) + 1);
			regExp = lastPart;
		}
		
		File srcFile = new File(srcPath);
		//Source is a directory and target already exist, re-construct destination path
		//Example, /u/src to /u/dst, the constructed destination path will be /u/dst/src
		if(srcFile.isDirectory() && (regExp == "") && targetExist) {
			dstPath = dstPath + File.separator + (new File(srcPath)).getName();
		}
		
		if(srcFile.isDirectory()) {
			return validateDir(srcPath,dstPath, regExp);
		} else {
			return validateFile(srcPath,dstPath);
		}
	}
}

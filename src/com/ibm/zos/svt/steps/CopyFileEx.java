package com.ibm.zos.svt.steps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of copy files using shell command
 * @author Stone WANG
 *
 */
public class CopyFileEx extends AbstractStep{
	private String[] parms = null;
	private boolean targetExist = false;
	
	
	/**
	 * Create a new CopyFileEx
	 * @param location	The step location
	 * @param parent	The 
	 */
	public CopyFileEx(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Copy files from source to destination
	 * @param srcPath	The source file path
	 * @param dstPath	The destination file path
	 * @return	True if success, otherwise false
	 */
	private boolean copyFiles(String srcPath, String dstPath)
	{
		String[] lines = null;
		String fullSrcPath = srcPath;
		String[] tokens = srcPath.split(File.separator.replaceAll("\\\\", "\\\\\\\\"));
		String lastPart = tokens[tokens.length - 1];
		if(lastPart.contains(("*"))) {
			srcPath = srcPath.substring(0, srcPath.lastIndexOf(File.separator) + 1);
		}
		
		//If there's MVS attributes, use double quotes to bracket them
		int index = dstPath.indexOf(",");
		if(index > 0) {
			dstPath = "\"" + dstPath + "\"";
		}
		
		//Check source for existent
		lines = shell("ls -ald " + srcPath, true);
		if((lines == null) || (lines.length == 0)) {
			logger.error("Source " + srcPath + " doesn't exist.");
			return false;
		}
		
		//Check destination for existent
		lines = shell("ls -ald " + dstPath, false);
		if((lines != null) && (lines.length > 0)) {
			targetExist = true;
		} else {
			targetExist = false;
		}
		//Copy files using shell
		if(sh("cp -r " + fullSrcPath + " " + dstPath, true) == null) {
			logger.error("Copy file " + fullSrcPath + " to " + dstPath + " failed.");
			return false;
		}
		return true;
	}
	
	/**
	 * Validate the directories copy
	 * @param src	The source directory
	 * @param dst	The destination directory
	 * @param regExp	The wildchar string of the source
	 * @return	True if success, otherwise false
	 */
	private boolean validateDir(String src, String dst, String regExp) {
		String lines[] = null;
		String tokens[] = null;
		String srcPerm = "";
		String srcCount = "";
		String srcSize = "";
		String dstPerm = "";
		String dstCount = "";
		String dstSize = "";
		
		//Ignore MVS attributes because we can't check them
		int index = dst.indexOf(",");
		if(index > 0) {
			dst = dst.substring(0, index);
		}
		
		//Check source directory for existent
		lines = shell("ls -ald " + src, true);
		if((lines == null) || (lines.length == 0)) {
			logger.error("Source file " + src + " doesn't exist.");
			return false;
		}
		//Retrieve source information
		//Example: drwxr-xr-x 2 stone stone 4096 May 27 15:44 Downloads/
		tokens = lines[0].split(" +");
		//File Permission
		srcPerm = tokens[0];
		//File count
		srcCount = tokens[1];
		//File size
		srcSize = tokens[4];
		
		//Source directory itself is copied
		if(regExp == "")  {
			//Check destination directory for existent
			lines = shell("ls -ald " + dst, true);
			if((lines == null) || (lines.length == 0)) {
				logger.error("Destination file " + dst + " doesn't exist.");
				return false;
			}
			//Retrieve source information
			//Example: drwxr-xr-x 2 stone stone 4096 May 27 15:44 Downloads/
			//File permission
			dstPerm = tokens[0];
			//File count
			dstCount = tokens[1];
			//File size
			dstSize = tokens[4];
			
			boolean flag = true;
			//Check if file permission is the same 
			if(!dstPerm.equals(srcPerm)) {
				flag = false;
				logger.error("Destination directory " + dst + " has different permission with " + src + ".");
			}
			//Check if file count is the same
			if(!dstCount.equals(srcCount)) {
				flag = false;
				logger.error("Destination directory " + dst + " has different count with " + src + ".");
			}
			//Check if file size is the same
			if(!dstSize.equals(srcSize)) {
				flag = false;
				logger.error("Destination directory " + dst + " has different size with " + src + ".");
			}
			if(!flag)
				return false;
		}
			
		lines = shell("ls -al " + src, true);
		if((lines == null) || (lines.length == 0)) {
			logger.error("Source file " + src + " doesn't exist.");
			return false;
		}
		
		Pattern pattern = null;
		if(regExp != "") {
			pattern = Pattern.compile(regExp);
		}
		
		String fileName = "";
		String[] lines1 = null;
		//Start index from 1 to skip the first line which contains summary information
		for(int i = 1; i < lines.length; i++) {
			tokens = lines[i].split(" +");
			fileName = tokens[tokens.length - 1];
			//Ignore . and ..
			if(fileName.equals(".") || fileName.equals(".."))
				continue;
			//Ignore unmatched files
			if(regExp != "") {
				if(!pattern.matcher(fileName).find()) {
					continue;
				}
			}
			//Retrieve file information
			//Example: drwxr-xr-x 2 stone stone 4096 May 27 15:44 Downloads/
			lines1 = shell("ls -ald " + src + File.separator + fileName, true);
			if((lines1 == null) || (lines1.length == 0)) {
				logger.error("List source file/directory" + src + File.separator + fileName + " met issue.");
				return false;
			}
			//lines[i] is a directory
			if(lines1[0].startsWith("d")) {
				if(!validateDir(src + File.separator + fileName, dst + File.separator + fileName, "")) {
					return false;
				}
			//lines[i] is a file
			} else {
				if(!validateFile(src + File.separator + fileName, dst + File.separator + fileName)) {
					return false;
				}
			}
		}
		logger.norm("Directory " + src + " is copied successfully.");
		return true;
	}
	
	/**
	 * Validate the files copy 
	 * @param src	The source file path
	 * @param dst	The destination file path
	 * @return	True if success, otherwise false
	 */
	private boolean validateFile(String src, String dst) {
		String lines[] = null;
		String tokens[] = null;
		String srcPerm = "";
		String srcCount = "";
		String srcSize = "";
		String dstPerm = "";
		String dstCount = "";
		String dstSize = "";
		
		//Ignore MVS attributes because we can't check them
		int index = dst.indexOf(",");
		if(index > 0) {
			dst = dst.substring(0, index);
		}
		
		//Check source file for existent
		lines = shell("ls -al " + src, true);
		if((lines == null) || (lines.length == 0)) {
			logger.error("Source file " + src + " doesn't exist.");
			return false;
		}	
		//Retrieve source information
		//Example: drwxr-xr-x 2 stone stone 4096 May 27 15:44 Downloads/
		tokens = lines[0].split(" +");
		srcPerm = tokens[0];
		srcCount = tokens[1];
		srcSize = tokens[4];
		
		
		//Check destination file for existent
		lines = shell("ls -ald " + dst, true);
		if((lines == null) || (lines.length == 0)) {
			logger.error("Destination file " + dst + " doesn't exist.");
			return false;
		}
		//Reconstruct destination file path if it's a directory 
		if(lines[0].startsWith("d")) {
			dst = dst + File.separator + (new File(src)).getName();
			lines = shell("ls -ald " + dst, true);
			if((lines == null) || (lines.length == 0)) {
				logger.error("Destination file " + dst + " doesn't exist.");
				return false;
			}
		}
		
		//Retrieve destination information
		//Example: drwxr-xr-x 2 stone stone 4096 May 27 15:44 Downloads/
		tokens = lines[0].split(" +");
		//File permission
		dstPerm = tokens[0];
		//File count
		dstCount = tokens[1];
		//File size
		dstSize = tokens[4];
		 
		boolean flag = true;
		//Check if file permission is the same
		if(!dstPerm.equals(srcPerm)) {
			flag = false;
			logger.error("Destination file " + dst + " has different permission with " + src + ".");
		}
		//Check if file count is the same
		if(!dstCount.equals(srcCount)) {
			flag = false;
			logger.error("Destination file " + dst + " has different count with " + src + ".");
		}
		//Check if file size is the same
		if(!dstSize.equals(srcSize)) {
			flag = false;
			logger.error("Destination file " + dst + " has different size with " + src + ".");
		}
		if(!flag)
			return false;
		
		//Check if file contents are the same
		try {
			FileInputStream srcStream = new FileInputStream(src);
			FileInputStream dstStream = new FileInputStream(dst);
			byte[] srcBuf = new byte[1440];
			byte[] dstBuf = new byte[1440];
			while(true) {
				int srcBytes = srcStream.read(srcBuf);
				int dstBytes = dstStream.read(dstBuf);
				if(srcBytes != dstBytes) {
					logger.error("Source file " + src + 
							" size is different with destination file " +
							 dst + ".");
					return false;
				}
				if((srcBytes == -1) || (dstBytes == -1)) {
					break;
				}
				for(int i = 0 ; i < srcBytes; i++) {
					if(srcBuf[i] != dstBuf[i]) {
						logger.error("Source file " + src + 
								" content is different with destination file " +
								 dst + ".");
						return false;
					}
				}
			}
			srcStream.close();
			dstStream.close();
		}catch(IOException ex) {
			logger.error("Exception occured while validate files " + 
					src + " and " + dst + ".");
			return false;
		}
		
		logger.norm("File " + src + " is copied successfully.");
		return true;
	}
	
	/**
	 * Run the step
	 * @param params The parameters of the step
	 * @return True if success, otherwise false
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
	 * Validate the step
	 * @return True if success, otherwise false
	 */
	public boolean validate() {
		String src = parms[0];
		String dst = parms[1];
		String regExp = "";
		String srcName = "";
		String lines[] = null;
		//Ignore MVS attributes because we can't check them
		int index = dst.indexOf(",");
		if(index > 0) {
			dst = dst.substring(0, index);
		}
		
		srcName = (new File(src)).getName();
		if(srcName.contains("*")) {
			src = (new File(src)).getParent();
			regExp = srcName;
			regExp = "^" + regExp + "$";
			//Replace '*' with ".*"
			regExp = regExp.replaceAll("[*]", ".*");
		}
		
		//Check source directory/file for existent
		lines = shell("ls -ald " + src, true);
		if((lines == null) || (lines.length == 0)) {
			logger.error("Source directory " + src + " doesn't exist.");
			return false;
		}
		
		//Source is a directory and target already exist, re-construct destination path
		//Example, /u/src to /u/dst, the constructed destination path will be /u/dst/src
		if(lines[0].startsWith("d") && (regExp == "") && targetExist) {
			dst = dst + File.separator + srcName;
		}
		
		if(lines[0].startsWith("d")) {
			return validateDir(src, dst, regExp);
		} else {
			return validateFile(src, dst);
		}
	}
}

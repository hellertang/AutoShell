package com.ibm.zos.svt.steps;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.TelnetSession;
import com.ibm.zos.svt.Utils;
import com.ibm.zos.svt.units.Context;
import com.ibm.zos.svt.units.Unit;

public class CreateFileEx extends AbstractStep{
	private String[] parms = null;
	private Unit parent = null;
	
	/**
	 * Create a new CreateFileEx
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public CreateFileEx(String location, Unit parent) {
		logger = Logger.getInstance(location);
		this.parent = parent;
	}
	
//	private boolean createFiles(String fileName, int fileSize, int randomSize, int fileCount) {
//		String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,.";
//		int digits = getDigits(fileCount);
//
//		String curFileName = "";
//
//		// Check possible MVS attributes
//		String attributes = "";
//		int index = fileName.indexOf(",");
//		if (index > 0) {
//			attributes = fileName.substring(index);
//			fileName = fileName.substring(0, index);
//		}
//		for (int i = 0; i < fileCount; i++) {
//			curFileName = fileName;
//			if (fileCount > 1) {
//				curFileName += String.format("%0" + digits + "d", (i + 1));
//			}
//			
//			if (sh("touch \"" + curFileName + attributes + "\"", true) == null) {
//				logger.error("Create file " + curFileName + " failed.");
//				return false;
//			}
//
//			int remainBytes = fileSize;
//			int charSize = 0;
//			boolean firstLoop = true;
//			String cmd = "";
//			while (remainBytes > 0) {
//				if (remainBytes > randomSize) {
//					charSize = randomSize;
//				} else {
//					charSize = remainBytes;
//				}
//				StringBuffer dataBuf = new StringBuffer();
//				int charIndex = (int) (Math.random() * allChar.length());
//				for (int j = 0; j < charSize; j++) {
//					dataBuf.append(allChar.charAt(charIndex));
//				}
//				
//				if(firstLoop) {
//					cmd = "echo '" + dataBuf + "\\c' > " + curFileName;
//					firstLoop = false;
//				} else {
//					cmd = "echo '" + dataBuf + "\\c' >> " + curFileName;
//				}
//				if (sh(cmd, true) == null) {
//					logger.error("Echo to append content to file " + curFileName
//							+ " failed.");
//					return false;
//				}
//				remainBytes -= charSize;
//			}
//		}
//		return true;
//	}
	
	/**
	 * Run the step
	 * @param The parameters of the step
	 * @return True if success, otherwise false
	 */
	public boolean run(String params) {
		String[] parms = params.split(" +");
		
		if((parms == null) || (parms.length < 3)) {
			logger.error("Invalid parameters for CreateFile Step:" + params + ".");
			logger.error("File Name, File Size, Randome Size must be provided.");
			return false;
		}
		this.parms = parms;
		
		String fileName = parms[0];
		
		long fileSize = 0;
		try {
			fileSize = Utils.getSize(parms[1]);
			if(fileSize <= 0) {
				logger.error("File Size can't be less than or equal to 0:" + parms[1] + ".");
				return false;
			}
		}catch(NumberFormatException ex) {
			logger.error("File size must be a number:" + parms[1] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		int randomSize = 0;
		try {
			randomSize = (int)Utils.getSize(parms[2]);
			if(randomSize <= 0) {
				logger.error("Random Size can't be less than or equal to 0:" + parms[2] + ".");
				return false;
			}
		}catch(NumberFormatException ex) {
			logger.error("Random size must be a number:" + parms[2] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		
		
		int fileCount = 1;
		if(parms.length > 3) {
			fileCount = Integer.parseInt(parms[3]);
		}
		
		if(fileCount <= 0) {
			logger.error("File Count must be larger than 0.");
			return false;
		}
		
		//Establish telnet session
		String account = Context.Settings.get("Account");
		String password = Context.Settings.get("Password");
		String shellPrompt = Context.Settings.get("ShellPrompt");
		if((account == null) || (password==null) || (shellPrompt==null)) {
			logger.error("Account Name, Account Password, Shell Prompt are required for establishing Telnet session.");
			logger.error("Please configure them inside the configuration file.");
			return false;
		}
		if(parent.telnetSession == null) {
			parent.telnetSession = new TelnetSession(logger, account, password, shellPrompt);
			parent.telnetSession.login();
		}
		// Check possible MVS attributes
		String attributes = "";
		int index = fileName.indexOf(",");
		if (index > 0) {
			attributes = fileName.substring(index);
			fileName = fileName.substring(0, index);
		}
		String curFileName = "";
		int digits = getDigits(fileCount);
		//Create file one by one using established telnet session
		for (int i = 0; i < fileCount; i++) {
			curFileName = fileName;
			if (fileCount > 1) {
				curFileName += String.format("%0" + digits + "d", (i + 1));
			}
			// MVS attributes
			if (attributes != "") {
				curFileName += attributes;
			}
			if(!parent.telnetSession.createFile(curFileName, fileSize, randomSize)) {
				logger.error("Create file " + curFileName + " failed.");
				return false;
			}
		}
		return true;
	}

	/**
	 * Validate the step
	 * @return True if success, otherwise false
	 */
	public boolean validate() {
		String fileName = parms[0];
		long fileSize = 0;
		try {
			fileSize = Utils.getSize(parms[1]);
			if(fileSize <= 0) {
				logger.error("File Size can't be less than or equal to 0:" + parms[1] + ".");
				return false;
			}
		}catch(NumberFormatException ex) {
			logger.error("File size must be a number:" + parms[1] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		
		int fileCount = 1;
		if(parms.length > 3) {
			fileCount = Integer.parseInt(parms[3]);
		}
		int digits = getDigits(fileCount);
		//Ignore attributes because we can't check
		int index = fileName.indexOf(",");
		String attributes = "";
		if(index > 0) {
			attributes = fileName.substring(index);
			fileName = fileName.substring(0, index);
		}
		//Special process for MVS dataset attribute lrecl.
		//This attribute takes effect to decide the record length when recfm is "fb".
		//Record is the basic unit of a dataset. The size of the dataset always can be divided by record length.
		//So the calibrated file size is the original file size rounded up by the record length.
		if(attributes != "") {
			Pattern pattern = Pattern.compile("lrecl\\(([^\\)]*)\\)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(attributes);
			boolean lreclProvided = false;
			long recordLength = 1;
			if(matcher.find()) {
				lreclProvided = true;
				recordLength = Long.parseLong(matcher.group(1));
			}
			String recordFormat = "";
			pattern = Pattern.compile("recfm\\(([^\\)]*)\\)", Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(attributes);
			if(matcher.find()) {
				recordFormat = matcher.group(1);
			}
			
			//If lrecl is not provided in attributes, use the value in Context settings.
			if(!lreclProvided && (Context.Settings.get("lrecl") != null)) {
				recordLength = Long.parseLong(Context.Settings.get("lrecl"));
			}
			//Calibrate the file size
			if( recordFormat.toLowerCase().trim().equals("fb") &&
				(fileSize % recordLength > 0) ) {
				fileSize = (fileSize / recordLength + 1) * recordLength;
			}
		}
		
		String curFileName = "";
		String[] lines = null;
		String[] tokens = null;
		String perm = "";
		String size = "";
		boolean flag = true;
		for(int i = 0; i < fileCount; i++) {
			boolean innerFlag = true;
			curFileName = fileName;
			if(fileCount > 1) { 
				curFileName += String.format("%0" + digits + "d", (i + 1));
			}
			
			//Check created file for existent
			lines = shell("ls -ald " + curFileName, true);
			if((lines == null) || (lines.length == 0)) {
				flag = false;
				innerFlag = false;
				logger.error("File " + curFileName + " doesn't exist.");
				continue;
			}
			//Retrieve created file information
			//Example: drwxr-xr-x 2 stone stone 4096 May 27 15:44 Downloads/
			tokens = lines[0].split(" +");
			//File permission
			perm = tokens[0];
			//File size
			size = tokens[4];
			//Created file is a directory
			if(perm.startsWith("d")) {
				flag = false;
				innerFlag = false;
				logger.error(curFileName + " isn't a file but a directory.");
			}
			//Created file's size is not correct
			if(Long.parseLong(size) != fileSize) {
				flag = false;
				innerFlag = false;
				logger.error("File " + curFileName + " size is " + size + " but not " + fileSize + ".");
			}
			
			if(innerFlag) {
				logger.norm("File " + curFileName + " is created successfully.");
			}
		}
		return flag;
	}
}

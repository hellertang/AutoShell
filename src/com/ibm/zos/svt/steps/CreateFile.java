package com.ibm.zos.svt.steps;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.zos.svt.*;
import com.ibm.zos.svt.units.Context;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of create file using Java API
 * @author Stone WANG
 *
 */
public class CreateFile extends AbstractStep{
	private String[] parms = null;
	
	/**
	 * Create a new CreateFile
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public CreateFile(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Create files with specified parameters
	 * @param fileName	The file full path name
	 * @param fileSize	The file size
	 * @param randomSize	The random character duplicate size
	 * @param fileCount	The file count
	 * @return	True if success, otherwise false
	 */
	private boolean createFiles(String fileName, long fileSize, int randomSize, int fileCount) {
		String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,.";
		byte[] bytes = new byte[randomSize];
		int digits = Utils.getDigits(fileCount);
		
		String curFileName = "";
		try {
			//Check possible MVS attributes
			String attributes = "";
			int index = fileName.indexOf(",");
			if(index > 0) {
				attributes = fileName.substring(index);
				fileName = fileName.substring(0, index);
			}
			for(int i = 0; i < fileCount; i++) {
				curFileName = fileName;
				if(fileCount > 1) { 
					curFileName += String.format("%0" + digits + "d", (i + 1));
				}
				//MVS attributes
				if(attributes != "") {
					curFileName += attributes;
				}
				if(new File(curFileName).exists())
					logger.warn("File " + curFileName + " already exist. It will be truncated to fill with new content.");
				FileOutputStream out = new FileOutputStream(curFileName);
				//Truncate the file size to 0
				out.getChannel().truncate(0);
				long remainBytes = fileSize;
				while(remainBytes > 0) {
					//Duplicate fill the array with the random character to randomSize
					Arrays.fill(bytes, allChar.getBytes()[(int)(Math.random() * allChar.length())]);
					if(remainBytes > randomSize) {
						out.write(bytes, 0, (int)randomSize);
						remainBytes -= randomSize;
					} else {
						out.write(bytes, 0, (int)remainBytes);
						remainBytes = 0;
					}
				}
				out.close();
			}
		}
		catch (IOException e) {
			logger.error("File " + curFileName + " creation failed.");
			logger.debug(e.getMessage());
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
		
		return createFiles(fileName, fileSize, randomSize, fileCount);
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
				logger.error("Invalid file size or file Size is less than or equal to 0:" + parms[1]);
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
		int digits = Utils.getDigits(fileCount);
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
		boolean flag = true;
		for(int i = 0; i < fileCount; i++) {
			boolean innerFlag = true;
			curFileName = fileName;
			if(fileCount > 1) { 
				curFileName += String.format("%0" + digits + "d", (i + 1));
			}
			
			File file = new File(curFileName);
			//Created file doesn't exist
			if(!file.exists()) {
				logger.error("File " + curFileName + " doesn't exist.");
				flag = false;
				innerFlag = false;
			}
			//Created file size is not correct
			if(file.length() != fileSize) {
				logger.error("File " + curFileName + " size is " + file.length() + " but not " + fileSize + ".");
				flag = false;
				innerFlag = false;
			}
			if(innerFlag) {
				logger.norm("File " + curFileName + " is created successfully.");
			}
		}
		return flag;
	}
}

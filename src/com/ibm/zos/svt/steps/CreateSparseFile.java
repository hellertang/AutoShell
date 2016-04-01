package com.ibm.zos.svt.steps;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.zos.svt.*;
import com.ibm.zos.svt.units.Context;
import com.ibm.zos.svt.units.Unit;

/**
 * Step of create sparse file using Java API
 * @author Stone WANG
 *
 */
public class CreateSparseFile extends AbstractStep{
	private String[] parms = null;
	private long fileOffset = 0;
	
	/**
	 * Create a new CreateSparseFile
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public CreateSparseFile(String location, Unit parent) {
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
	private boolean createFiles(String fileName, long fileSize, int randomSize, int fileCount, String holePos, long holeSize) {
		String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,.";
		byte[] bytes = new byte[randomSize];
		int digits = Utils.getDigits(fileCount);
		if(holeSize > fileSize) {
			logger.error("File hole size must be less than or equal to file size.");
			return false;
		}
		//Hole position could be "begin", "middle", "end"
		if(holePos.toLowerCase().equals("begin")) {
			fileOffset = 0;
		}
		if(holePos.toLowerCase().equals("middle")) {
			fileOffset = (fileSize - holeSize) / 2;
		}
		if(holePos.toLowerCase().equals("end")) {
			fileOffset = fileSize - holeSize;
		}
		if(holePos.toLowerCase().equals("random")) {
			fileOffset = (long)(Math.random() * (fileSize - holeSize));
		}
		
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
				RandomAccessFile file = new RandomAccessFile(curFileName,"rw");
				//Truncate the file size to 0
				file.getChannel().truncate(0);
				//Fill the file with the hole
				long remainBytes = holeSize;
				file.seek(fileOffset);
				while(remainBytes > 0) {
					//Duplicate fill the array with the random character to randomSize
					Arrays.fill(bytes, allChar.getBytes()[(int)(Math.random() * allChar.length())]);
					if(remainBytes > randomSize) {
						file.write(bytes, 0, randomSize);
						remainBytes -= randomSize;
					} else {
						file.write(bytes, 0, (int)remainBytes);
						remainBytes = 0;
					}
				}
				//Seal the file to specific file size with one byte of value 0 if the hole position is not "end" and
				//Hole Size is less than File Size.
				if(!holePos.toLowerCase().equals("end") && (holeSize < fileSize)) {
					file.seek(fileSize - 1);
					file.write(0);
				}
				file.close();
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
		
		if((parms == null) || (parms.length < 5)) {
			logger.error("Invalid parameters for CreateFile Step:" + params + ".");
			logger.error("File Name, File Size, Random Size, Hole Position, Hole Size must be provided.");
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
			long tmpSize = Utils.getSize(parms[2]);
			if(tmpSize > Integer.MAX_VALUE) {
				logger.error("Random size can't be larger than " + Integer.MAX_VALUE + ".");
				return false;
			}
			randomSize = (int)tmpSize;
			if(randomSize <= 0) {
				logger.error("Random size can't be less than or equal to 0:" + parms[2] + ".");
				return false;
			}
		}catch(NumberFormatException ex) {
			logger.error("Random size must be a number:" + parms[2] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		String holePos = parms[3];
		long holeSize = 0;
		try {
			holeSize = Utils.getSize(parms[4]);
			if(holeSize <= 0) {
				logger.error("Hole Size can't be less than or equal to 0:" + parms[4]);
				return false;
			}
		}catch(NumberFormatException ex) {
			logger.error("Hole size must be a number:" + parms[4] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		int fileCount = 1;
		if(parms.length > 5) {
			fileCount = Integer.parseInt(parms[5]);
		}
		
		if( !holePos.toLowerCase().equals("begin") &&
			!holePos.toLowerCase().equals("middle") &&
			!holePos.toLowerCase().equals("end") &&
			!holePos.toLowerCase().equals("random")) {
			logger.error("Hole Position must be \"begin\" or \"middle\" or \"end\"");
			return false;
		}
		
		if(holeSize > fileSize) {
			logger.error("Hole Size must be less than or equal to File Size.");
			return false;
		}
		if(fileCount <= 0) {
			logger.error("File Count must be larger than 0.");
			return false;
		}
		
		return createFiles(fileName, fileSize, randomSize, fileCount, holePos, holeSize);
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
		long holeSize = 0;
		try {
			holeSize = Utils.getSize(parms[4]);
			if(holeSize <= 0) {
				logger.error("Hole Size can't be less than or equal to 0:" + parms[4] + ".");
				return false;
			}
		}catch(NumberFormatException ex) {
			logger.error("Hole size must be a number:" + parms[4] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		int fileCount = 1;
		if(parms.length > 5) {
			fileCount = Integer.parseInt(parms[5]);
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
		
		if(holeSize > fileSize) {
			logger.error("File hole size must be less than or equal to file size.");
			return false;
		}
		
		String curFileName = "";
		boolean flag = true;
		for(int i = 0; i < fileCount; i++) {
			boolean innerFlag = true;
			curFileName = fileName;
			if(fileCount > 1) { 
				curFileName += String.format("%0" + digits + "d", (i + 1));
			}
			
			try {
				RandomAccessFile file = new RandomAccessFile(curFileName, "r");
				//Created file size is not correct
				if(file.length() != fileSize) {
					logger.error("File " + curFileName + " size is " + file.length() + " but not " + fileSize + ".");
					flag = false;
					innerFlag = false;
					continue;
				}
				//Check if file hole is created with correct size.
				file.seek(fileOffset);
				for(int j = 0; j < holeSize; j++) {
					if(file.readByte() == 0) {
						logger.error("File hole content is wrongly formated. 0 contains.");
						flag = false;
						innerFlag = false;
						break;
					}
				}
			} catch (FileNotFoundException e) {
				//Created file doesn't exist
				logger.error("File " + curFileName + " doesn't exist.");
				flag = false;
				innerFlag = false;
			} catch (IOException e) {
				logger.error("IO exception occured while validating file " + curFileName + ".");
				logger.debug(e.getMessage());
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

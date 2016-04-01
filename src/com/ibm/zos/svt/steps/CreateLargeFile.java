package com.ibm.zos.svt.steps;

import java.io.*;
import java.util.Random;
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
public class CreateLargeFile extends AbstractStep{
	private String[] parms = null;
	private final String beginStr =  "<This is the beginning of the file>.............................";
	private final String middleStr = "<ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz>";
	private final String endStr =    "<This is the end of the file>...................................";
	private boolean verfiyContentFlag =false;  //added to support File content verification
	                                           //Default value is false to keep backward compatible
	                                           //added by sunhwei @2016.1.5
	private long randSeed =(long)(Math.random() *1000); 
	/**
	 * Create a new CreateSparseFile
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public CreateLargeFile(String location, Unit parent) {
		logger = Logger.getInstance(location);
	}
	
	/**
	 * Create files with specified parameters
	 * @param fileName	The file full path name
	 * @param fileSize	The file size
	 * @param bufferSize	The buffer size
	 * @param fileCount	The file count
	 * @return	True if success, otherwise false
	 */
	/* To support File content verification createFiles must be synchronized with readFiles
	*/
	private boolean createFiles(String fileName, long fileSize, long bufferSize, String position, int fileCount) {
		byte[] bytes = new byte[(int)bufferSize];
		byte[] readBytes = new byte[(int)bufferSize];
		byte[] beginBytes = beginStr.getBytes();
		byte[] middleBytes = middleStr.getBytes();
		byte[] endBytes = endStr.getBytes();
		
		int digits = Utils.getDigits(fileCount);
		long fileOffset = 0;

		boolean immVerifySuccFlag= true;
		
		//Hole position could be "begin", "middle", "end"
		if(position.toLowerCase().equals("begin")) {
			fileOffset = 0;
		}
		if(position.toLowerCase().equals("middle")) {
			fileOffset = fileSize / 2 - bufferSize;
		}
		if(position.toLowerCase().equals("end")) {
			fileOffset = fileSize - bufferSize;
		}
		if(position.toLowerCase().equals("random")) {
			java.util.Random writeRand= new Random();
			writeRand.setSeed(randSeed);
			fileOffset = (long)(writeRand.nextDouble() * (fileSize - bufferSize)/bufferSize) * bufferSize;
			
		}
		logger.debug("File will be written from offset: "+Long.toString(fileOffset));
 
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
				long remainBytes = fileSize;
				int seqNo = 1;
				while(remainBytes > 0) {
					
					byte[] fillBytes = null;
					if(fileOffset == 0) {
						fillBytes = beginBytes;
					} else if(fileOffset == (fileSize - bufferSize)) {
						fillBytes = endBytes;
					} else {
						fillBytes = middleBytes;
					}
					int j = 0;
					for(; j < bytes.length/beginBytes.length; ++j) {
						System.arraycopy(fillBytes, 0, bytes, j*fillBytes.length, beginBytes.length);
					}
					if((bytes.length % beginBytes.length) != 0)
						System.arraycopy(fillBytes, 0, bytes, j*fillBytes.length, bytes.length % fillBytes.length);
					
					bytes[bytes.length-1] = '\n';
					String seqNumStr = String.format("Sequence Number:%010d\n", seqNo++);
					System.arraycopy(seqNumStr.getBytes(), 0, bytes, 0, seqNumStr.getBytes().length);
					
					file.seek(fileOffset);
					file.write(bytes);
					//add new content verify logic for write --write & read immediately
					
					if (verfiyContentFlag)
					{
						file.seek(fileOffset);
						file.read(readBytes);
						
						
						for (int offset=0; offset<bytes.length ;offset++)
						{
							if  (readBytes[offset] != bytes[offset])
							{
								immVerifySuccFlag =false; 
								logger.error("Content Verifycation failed:"+curFileName +"Offset1 ="+ Long.toString(fileOffset) + " Offset2="+Integer.toString(offset) );
								logger.error("Expect to write byte is ("+Byte.toString(bytes[offset])+") ,but immediately read byte is ("+Byte.toString(readBytes[offset]) +")");
								break;
								}
							}
						}
					// end new read& write immediately 
					
					remainBytes -= bufferSize;
					//Turn back for one buffer size
					if(position.toLowerCase().equals("end")) {
						fileOffset -= bufferSize;
						continue;
					}
					//Turn back to the beginning
					if(fileOffset == (fileSize - bufferSize)) {
						fileOffset = 0;
						continue;
					}
					fileOffset+=bufferSize;
				}
				file.close();
				logger.norm("File " + curFileName + " creation successed.");
			}
		}
		catch (IOException e) {
			logger.error("File " + curFileName + " creation failed.");
			logger.debug(e.getMessage());
			return false;
		}
	
		return immVerifySuccFlag;
	}
	/* readFiles function keep same logical as the createFiles, it will be invoked when the verfiyContentFlag 
	*  is set to true, so if there are any change on createFile, readFiles must be synchronized 
	*/
	private boolean readFiles(String fileName, long fileSize, long bufferSize, String position, int fileCount) {
		byte[] bytes = new byte[(int)bufferSize];
		byte[] readBytes = new byte[(int)bufferSize];
		byte[] beginBytes = beginStr.getBytes();
		byte[] middleBytes = middleStr.getBytes();
		byte[] endBytes = endStr.getBytes();
		
		int digits = Utils.getDigits(fileCount);
		long fileOffset = 0;
	
		boolean verifyResult= true;
		
		logger.debug("File size in readfiles,File name:"+ fileName +" size="+ Long.toString(fileSize) );
		//Hole position could be "begin", "middle", "end"
		if(position.toLowerCase().equals("begin")) {
			fileOffset = 0;
		}
		if(position.toLowerCase().equals("middle")) {
			fileOffset = fileSize / 2 - bufferSize;
		}
		if(position.toLowerCase().equals("end")) {
			fileOffset = fileSize - bufferSize;
		}
		if(position.toLowerCase().equals("random")) {
			java.util.Random readRand= new Random();
			readRand.setSeed(randSeed);
			fileOffset = (long)(readRand.nextDouble() * (fileSize - bufferSize)/bufferSize) * bufferSize;
						
		}
		
		logger.debug("File will be read from offset: "+Long.toString(fileOffset));
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
				if(!(new File(curFileName).exists()))
				{
					logger.error("File " + curFileName + " does not exist. content verifycation failed.");
				return false;
				}
				
				RandomAccessFile file = new RandomAccessFile(curFileName,"r");
				//Truncate the file size to 0
				//file.getChannel().truncate(0);
				
				//Fill the file with the hole
				long remainBytes = fileSize;
				int seqNo = 1;
				while(remainBytes > 0) {
					
					byte[] fillBytes = null;
					if(fileOffset == 0) {
						fillBytes = beginBytes;
					} else if(fileOffset == (fileSize - bufferSize)) {
						fillBytes = endBytes;
					} else {
						fillBytes = middleBytes;
					}
					int j = 0;
					for(; j < bytes.length/beginBytes.length; ++j) {
						System.arraycopy(fillBytes, 0, bytes, j*fillBytes.length, beginBytes.length);
					}
					if((bytes.length % beginBytes.length) != 0)
						System.arraycopy(fillBytes, 0, bytes, j*fillBytes.length, bytes.length % fillBytes.length);
					
					bytes[bytes.length-1] = '\n';
					String seqNumStr = String.format("Sequence Number:%010d\n", seqNo++);
					System.arraycopy(seqNumStr.getBytes(), 0, bytes, 0, seqNumStr.getBytes().length);

					//add new content verify logic for write --write & read immediately 
					file.seek(fileOffset);
					file.read(readBytes);
					////////////////
				
					for (int offset=0; offset<bytes.length ;offset++){
					
						if  (readBytes[offset] != bytes[offset]){
							verifyResult =false; 
							logger.error("Content Verifycation failed:"+curFileName +" Offset1 ="+ Long.toString(fileOffset) + " Offset2="+Integer.toString(offset) );
							logger.error("Expect to write byte is ("+Byte.toString(bytes[offset])+") ,but read byte is ("+Byte.toString(readBytes[offset]) +")");
							break;
						}
					}
				
				
					// end new read& write immediately 
					
					remainBytes -= bufferSize;
					//Turn back for one buffer size
					if(position.toLowerCase().equals("end")) {
						fileOffset -= bufferSize;
						continue;
					}
					//Turn back to the beginning
					if(fileOffset == (fileSize - bufferSize)) {
						fileOffset = 0;
						continue;
					}
					fileOffset+=bufferSize;
				}
				file.close();
				if (verifyResult)
				{
					logger.debug("File contents verfication is successed on File: "+curFileName);
				}else {
					logger.error("File contents verfication is failed on File: "+curFileName);
				}
			}
		}
		catch (IOException e) {
			logger.error("File " + curFileName + " Verify failed.");
			logger.debug(e.getMessage());
			return false;
		}
		
	
		return verifyResult;
	}
	/**
	 * Run the step
	 * @param params The parameters of the step
	 * @return True if success, otherwise false
	 */
	public boolean run(String params) {
		String[] parms = params.split(" +");
		
		if((parms == null) || (parms.length < 4)) {
			logger.error("Invalid parameters for CreateLargeFile Step:" + params + ".");
			logger.error("File Name, File Size, BufferSize, Position must be provided.");
			return false;
		}
		this.parms = parms;
		for (int i=0;i<parms.length;i++)
		{
			logger.debug("CreatelargeFile parmaters("+i+")="+parms[i]);
		}
		
		
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
		long bufferSize = 0;
		try {
		bufferSize = Utils.getSize(parms[2]);
		if(bufferSize <= 0) {
			logger.error("Buffer Size can't be less than or equal to 0:" + parms[2]);
			return false;
		}
		}catch(NumberFormatException ex) {
			logger.error("Buffer size must be a number:" + parms[2] + ".");
			logger.debug(ex.getMessage());
			return false;
		}
		String position = parms[3];
		int fileCount = 1;
		if(parms.length > 4) {
			fileCount = Integer.parseInt(parms[4]);
		}
		if((fileSize % bufferSize) != 0) {
			logger.error("File Size must be divided by Buffer Size.");
			return false;
		}
		if( !position.toLowerCase().equals("begin") &&
			!position.toLowerCase().equals("middle") &&
			!position.toLowerCase().equals("end") &&
			!position.toLowerCase().equals("random")) {
			logger.error("Position must be \"begin\" or \"middle\" or \"end\" or \"random\"");
			return false;
		}
		if(fileCount <= 0) {
			logger.error("File Count must be larger than 0.");
			return false;
		}
		if(parms.length > 5) {
			String verifyFlag= parms[5];
			if (verifyFlag.toUpperCase().equals("CONTENTVERIFY"))
			{
				verfiyContentFlag =true;
			}else {
				logger.error("Parameter '"+parms[5]+"' is not supported, expected value is 'CONTENTVERIFY'");
				return false;
			}
			
		}
		boolean createRet= createFiles(fileName, fileSize, bufferSize, position, fileCount);
		if ( createRet && verfiyContentFlag)
		{
		return readFiles(fileName, fileSize, bufferSize, position, fileCount);
		}else {
			return createRet;
			}
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
		if(parms.length > 4) {
			fileCount = Integer.parseInt(parms[4]);
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
			
			try {
				RandomAccessFile file = new RandomAccessFile(curFileName, "r");
				//Created file size is not correct
				if(file.length() != fileSize) {
					logger.error("File " + curFileName + " size is " + file.length() + " but not " + fileSize + ".");
					flag = false;
					innerFlag = false;
					continue;
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

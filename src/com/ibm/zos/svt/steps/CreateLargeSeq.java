package com.ibm.zos.svt.steps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.Utils;
import com.ibm.zos.svt.units.Context;
import com.ibm.zos.svt.units.Unit;

public class CreateLargeSeq extends AbstractStep{ 
	private String[] parms = null;
	private final String beginStr =  "<This is the beginning of the Large Sequential File>............";
	private final String middleStr = "<ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz>";
	private final String endStr =    "<This is the end of Large Sequential File>......................";
	private boolean verfiyContentFlag =false;  //added to support File content verification
	                                           //Default value is false to keep backward compatible
	                                           //added by sunhwei @2016.1.20
	private long randSeed =(long)(Math.random() *1000); 
	/**
	 * Create a new CreateSparseFile
	 * @param location	The step location
	 * @param parent	The parent unit
	 */
	public CreateLargeSeq(String location, Unit parent) {
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
	
		byte[] fixedBuffer = new byte[5000];
		//long fixedOffset = 2147483648L; --- this number is from option1 input file
		long fixedOffset = 1048576L;
		int fixedSeqWriteNumber=8;
		int seqWriteNumber=8;
		
		byte[] writeBuffer = new byte[(int)bufferSize];
		byte[] readBuffer = new byte[(int)bufferSize];
		
		String writeString="a";
        byte[] writeBytes = writeString.getBytes();
        long writeStartOffset=Long.parseLong(position);

		int digits = Utils.getDigits(fileCount);
		long fileOffset = Long.parseLong(position);

		boolean immVerifySuccFlag= true;
		
	/*
		if(position.toLowerCase().equals("random")) {
			java.util.Random writeRand= new Random();
			writeRand.setSeed(randSeed);
			
			fileOffset = (long)(writeRand.nextDouble() * (fileSize - bufferSize)/bufferSize) * bufferSize;
		}
		remove random part only for test 
		*/
		logger.debug("File will be written from offset: "+Long.toString(writeStartOffset));
 
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
				
				///start the fixed write oprtion just for OA49052 APAR fix verify  
				
			    int j1 = 0;
				
				for(; j1 < fixedBuffer.length; ++j1) {
					System.arraycopy(writeBytes, 0, fixedBuffer, j1 , writeBytes.length);
				}
				file.seek(fixedOffset);    // only set fileOffset one time 
				logger.debug("OA49051  fixed required step --file.seek successful.");
				for (int i1=0; i1<fixedSeqWriteNumber;i1++)
				{
					file.write(fixedBuffer);
				}
				logger.debug("OA49051 fixed required step --t sequence write successful");
	            file.getFD().sync();	
	            logger.debug("OA49051 fixed required step -- file IO sync successful.");     
				
				//Fill the file with the hole---start the configuration test logic
				long remainBytes = fileSize - writeStartOffset;   // only write forward 
				int seqNo = 1;
				fileOffset =writeStartOffset;
				file.seek(fileOffset);    // only set fileOffset one time
				logger.debug("OA49051 test first file.seek successful.");
				
				while(remainBytes > 0) {
									
					int j2 = 0;
					for(; j2 < writeBuffer.length; ++j2) {
						System.arraycopy(writeBytes, 0, writeBuffer, j2 , writeBytes.length);
					}
					writeBuffer[writeBuffer.length-1] = '\n';
					String seqNumStr = String.format("Sequence Number:%010d\n", seqNo);
					System.arraycopy(seqNumStr.getBytes(), 0, writeBuffer, 0, seqNumStr.getBytes().length);
		
				   file.write(writeBuffer);
				   
				   logger.debug(curFileName +" Sequence write successful: " +Integer.toString(seqNo));
				   seqNo =seqNo+1;   
				   remainBytes -= bufferSize;
				
				}

				file.getFD().sync();       	
	    						
				file.close();
				logger.debug("File " + curFileName + " creation successful.");
			}
		}
		catch (IOException e) {
			logger.error("File " + curFileName + " creation failed.");
			logger.error(e.getMessage());
			return false;
		}
	
		return immVerifySuccFlag;
	}
	
	private boolean createFilesOpt1(String fileName, long fileSize, long bufferSize, String position, int fileCount) {
	//hardcode opt1 files to simulate the field issue vwm OA49051
		/*
		 * truncate(0 0)
pseqwrite(0 2897195008, 0, 40096, a, 8, 1)
fsync
pseqwrite(0 2901917696, 0, 49152, a, 8, 1)
pseqwrite(0 2901966848, 0, 16384, a, 8, 1)
pseqwrite(0 2901983232, 0, 32768, a, 8, 1)
		 * 
		 */
		/* opt1*/
		byte[] fixedBuffer = new byte[5000];
		//long fixedOffset = 2147483648L;
		long fixedOffset = 1048576L;
		int seqWriteNumber=8;
		
		byte[] writeBuffer = new byte[(int)bufferSize];
		byte[] readBuffer = new byte[(int)bufferSize];
		
		String writeString="a";
        byte[] writeBytes = writeString.getBytes();
        long writeStartOffset=Long.parseLong(position);


		
		
		int digits = Utils.getDigits(fileCount);
	        
		boolean immVerifySuccFlag= true;
		logger.norm("Enter the special test case for OA49051 Opt1.");	
		
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
				////start pseqwrite 1 
				int seqNo = 1;
				int j1 = 0;
				
				for(; j1 < fixedBuffer.length; ++j1) {
					System.arraycopy(writeBytes, 0, fixedBuffer, j1 , writeBytes.length);
				}
				file.seek(fixedOffset);    // only set fileOffset one time 
				logger.norm("OA49051  Opt1  step1 --file.seek successful.");
				for (int i1=0; i1<seqWriteNumber;i1++)
				{
					logger.norm("OA49051  Opt1  step1 --begin start sequence write " +Integer.toString(i1));
					file.write(fixedBuffer);
				}
				
				
				logger.norm("OA49051  Opt1  step1 --file IO sync begin.");
	                 file.getFD().sync();	
	                 logger.norm("OA49051  Opt1  step1 -- file IO sync End.");      
	    //////////write step2 
	         ///pseqwrite(0 2901917696, 0, 49152, a, 8, 1)        
	             	int j2 = 0;
					for(; j2 < writeBuffer.length; ++j2) {
						System.arraycopy(writeBytes, 0, writeBuffer, j2 , writeBytes.length);
					}
					file.seek( writeStartOffset);    // only set fileOffset one time 
					logger.norm("OA49051  Opt1  step2 --file.seek successful.");
					for (int i1=0; i1<seqWriteNumber;i1++)
					{
						logger.norm("OA49051  Opt1  step2 --begin start sequence write " +Integer.toString(i1));
						file.write(writeBuffer);
					}    
					
					logger.norm("OA49051  Opt1  step2 --file IO sync begin.");
	                 file.getFD().sync();	
	                 logger.norm("OA49051  Opt1  step2 -- file IO sync End.");
				               					
	    //////////////////last one ///////////////             
	                 file.close();
			
				}
				
				logger.norm(" OA49051-- File " + curFileName + " creation successed.");
			
		}
		catch (IOException e) {
			logger.error("OA49051 --File " + curFileName + " creation failed.");
			logger.debug(e.getMessage());
			return false;
		}
		logger.norm("Exit  the special test case for OA49051  Opt1.");
		return immVerifySuccFlag;
	}
	/* readFiles function keep same logical as the createFiles, it will be invoked when the verfiyContentFlag 
	*  is set to true, so if there are any change on createFile, readFiles must be synchronized 
	*/
	private boolean readFiles(String fileName, long fileSize, long bufferSize, String position, int fileCount) {
		byte[] bytes = new byte[(int)bufferSize];
		byte[] readBuffer = new byte[(int)bufferSize];
	
		String readString="a";
        byte[] readBytes = readString.getBytes(); 
		
	
		long fileReadOffset = Long.parseLong(position);
		long fileOffset = 0;
	
		boolean verifyResult= true;
		int digits = Utils.getDigits(fileCount);
		
		logger.debug("File size in readfiles,File name:"+ fileName +" size="+ Long.toString(fileSize) );
		//Hole position could be "begin", "middle", "end"
	/*/
		if(position.toLowerCase().equals("random")) {
			java.util.Random readRand= new Random();
			readRand.setSeed(randSeed);
			
			fileOffset = (long)(readRand.nextDouble() * (fileSize - bufferSize)/bufferSize) * bufferSize;
			
		}
		*/
		fileOffset = fileReadOffset;
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
				long remainBytes = fileSize -fileOffset;
				int seqNo = 1;
				file.seek(fileOffset);
				while(remainBytes > 0) {
			        
					int j2 = 0;
					for(; j2 < readBuffer.length; ++j2) {
						System.arraycopy(readBytes, 0, readBuffer, j2 , readBytes.length);
					}
					readBuffer[readBuffer.length-1] = '\n';
					String seqNumStr = String.format("Sequence Number:%010d\n", seqNo);
					System.arraycopy(seqNumStr.getBytes(), 0, readBuffer, 0, seqNumStr.getBytes().length);
		
									
					file.read(bytes);
					////////////////
				
					for (int offset=0; offset<bytes.length ;offset++){
					
						if  (bytes[offset] != readBuffer[offset]){
							verifyResult =false; 
							logger.error("Content Verifycation failed:"+curFileName +" Offset1 ="+ Long.toString(fileOffset) + " Offset2="+Integer.toString(offset) );
							logger.error("Expect to write byte is ("+Byte.toString(readBuffer[offset])+") ,but read byte is ("+Byte.toString(bytes[offset]) +")");
							break;
						}
					}
				
				  // end new read& write immediately 
					
					remainBytes -= bufferSize;
					
					fileOffset+=bufferSize;
					seqNo=seqNo+1;
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
			if(fileSize <= 10485760L) {
				logger.error("File Size can't be less than or equal to 10MB:" + parms[1] + ".");
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
		/*if( !position.toLowerCase().equals("begin") &&
			!position.toLowerCase().equals("middle") &&
			!position.toLowerCase().equals("end") &&
			!position.toLowerCase().equals("random"))*/ 
		if (Long.parseLong(position) <0  ){
			logger.error("Position(offset) must be greater than or equal ZERO !");
			return false;
		}
		if( fileSize -Long.parseLong(position) <= 0) {
			logger.error("Position can't be greater than or equal to filesize, Filesize:"+ Long.toString(fileSize)+" Postion:"+ position );
			return false;
		}
		if(((fileSize- Long.parseLong(position)) % bufferSize) != 0) {
			logger.error("File Size - offset of start write must be divided by Buffer Size.");
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
	//	boolean createRet= createFilesOpt1(fileName, fileSize, bufferSize, position, fileCount);
		
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

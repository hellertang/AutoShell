package com.ibm.zos.svt;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.util.Arrays;

import org.apache.commons.net.telnet.TelnetClient;

/**
 * Represent a telnet session.
 * This session can be used to create file using shell command vi or cat.
 * @author Stone WANG
 *
 */
public class TelnetSession {
	TelnetClient client = null;
	private InputStream in = null;
	private PrintStream out = null;
	//Server settings
	private String ip = "localhost";
	private String port = "23";
	private String userName = "";
	private String password = "";
	private String prompts = "";
	private Logger logger = null;
	private boolean isLogon = false;
	
	/**
	 * Create a new TelnetSession
	 * @param logger 	The message logger
	 * @param userName	The user name
	 * @param password	The password
	 * @param prompts	The shell prompts
	 */
	public TelnetSession(Logger logger, String userName, String password, String prompts) {
		client = new TelnetClient();
		this.logger = logger;
		this.userName = userName;
		this.password = password;
		this.prompts = prompts;
	}
	
	/**
	 * Parse to retrieve the response until meets the specific end string
	 * @param endString	The end string to meet
	 * @return	True if success, otherwise false
	 * @throws IOException
	 */
	private String readResponse(String endString) throws IOException {
		StringBuffer sb = new StringBuffer();
		while (true) {
			char ch = (char) in.read();
			sb.append(ch);
			if (sb.toString().endsWith(endString))
				break;
		}
		return sb.toString();
	}
	
	private String readResponse(int length) throws IOException {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i < length; i++) {
			char ch = (char) in.read();
			sb.append(ch);
		}
		return sb.toString();
	}

	/**
	 * Login a telnet session
	 * @return True if success, otherwise false
	 */
	public boolean login() {
		try {
			//Already logon
			if(isLogon)
				return true;
			client.connect(ip, Integer.parseInt(port));
			in = client.getInputStream();
			out = new PrintStream(client.getOutputStream());
			readResponse(":");
			out.write((userName + "\r\n").getBytes("ISO-8859-1"));
			out.flush();
			readResponse(":");
			out.write((password + "\r\n").getBytes("ISO-8859-1"));
			out.flush();
			readResponse(prompts);
			//new BytesReader(in).start();
			isLogon = true;
			logger.debug("telnet session established.");
			return true;
		} catch (IOException e) {
			logger.error("Couldn't establish telnet session on local host.");
			logger.error(e.getMessage());
			return false;
		}
	}
	
	/**
	 * Build a string with specific length using one character
	 * @param ch	The character to build string
	 * @param length	The length of the string to build
	 * @return	The builded string
	 */
	private String buildString(char ch, int length) {
		StringBuffer sb = new StringBuffer();
		for(int i = 0 ; i < length; i++)
			sb.append(ch);
		return sb.toString();
	}
	
	/**
	 * Create file using CAT
	 * @param filePath The file full path
	 * @param fileSize	The file size
	 * @param randomSize	The random character repeat count
	 * @return	True if success, otherwise false
	 */
	private boolean cat(String filePath, long fileSize, int randomSize) {
		String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,.";
		byte[] bytes = new byte[randomSize];
		if(fileSize < 2) {
			logger.error("File size must be larger than 1.");
			return false;
		}
		//Telnet session must do logon first
		if(!isLogon) {
			logger.error("Telnet session must be established first to proceed creating file.");
			return false;
		}
		try {
			out.write(("cat > \"" + filePath + "\" <<EOF\r\n").getBytes("ISO-8859-1"));
			out.flush();
			readResponse(">");
			// insert file content
			long remainBytes = fileSize -1;
			
			int charIndex = -1;
			while (remainBytes > 0) {
				charIndex = (int) (Math.random() * allChar.length());
				byte bt = allChar.getBytes("ISO-8859-1")[charIndex];
				Arrays.fill(bytes, bt);
				if (remainBytes > randomSize) {
					out.write(bytes, 0, randomSize);
					out.flush();
					
					remainBytes -= randomSize;
					out.write("\r\n".getBytes("ISO-8859-1"));
					out.flush();
					readResponse(">");
					//\r\n takes up 1 byte
					remainBytes -= 1;
				} else {
					out.write(bytes, 0, (int)remainBytes);
					out.flush();
					remainBytes = 0;
				}	
			}
			out.write("\r\n".getBytes("ISO-8859-1"));
			out.flush();
			readResponse(">");
			// EOF
			out.write(("EOF" + "\r\n").getBytes("ISO-8859-1"));
			out.flush();
			readResponse(prompts);
			return true;
		} catch (IOException e1) {
			logger.error("Couldn't write on current telnet session.");
			logger.error(e1.getMessage());
			//Telnet session time out or disconnected, re-login
			isLogon = false;
			if(!login()) {
				return false;
			}
			if(!createFile(filePath, fileSize, randomSize)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Create file using VI
	 * @param filePath	The file full path
	 * @param fileSize	The file size
	 * @param randomSize	The random character repeat count
	 * @return	True if success, otherwise false
	 */
	private boolean vi(String filePath, long fileSize, int randomSize) {
		String allChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ,.";
		byte[] bytes = new byte[randomSize];
		if(fileSize < 2) {
			logger.error("File size must be larger than 1.");
			return false;
		}
		//Telnet session must do logon first
		if(!isLogon) {
			logger.error("Telnet session must be established first to proceed creating file.");
			return false;
		}
		try {
			out.write(("vi \"" + filePath + "\"\r\n").getBytes("ISO-8859-1"));
			out.flush();
			readResponse("vi \"" + filePath + "\"");
			// insert mode
			out.write("i".getBytes("ISO-8859-1"));
			out.flush();
			// insert file content
			long remainBytes = fileSize -1;
			
			int charIndex = -1;
			while (remainBytes > 0) {
				charIndex = (int) (Math.random() * allChar.length());
				byte bt = allChar.getBytes("ISO-8859-1")[charIndex];
				Arrays.fill(bytes, bt);
				if (remainBytes > randomSize) {
					out.write(bytes, 0, randomSize);
					out.flush();
					
					remainBytes -= randomSize;
					readResponse(buildString(allChar.charAt(charIndex), randomSize));
				} else {
					out.write(bytes, 0, (int)remainBytes);
					out.flush();
					readResponse(buildString(allChar.charAt(charIndex), (int)remainBytes));
					remainBytes = 0;
				}	
			}
			// Esc
			out.write(0x1b);
			out.flush();
			// wq
			out.write(":wq\r\n".getBytes("ISO-8859-1"));
			out.flush();
			readResponse(prompts);
			return true;
		} catch (IOException e1) {
			logger.error("Couldn't write on current telnet session.");
			logger.error(e1.getMessage());
			//Telnet session time out or disconnected, re-login
			isLogon = false;
			if(!login()) {
				return false;
			}
			if(!createFile(filePath, fileSize, randomSize)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Create file
	 * @param filePath	The file full path
	 * @param fileSize	The file size
	 * @param randomSize	The random character repeat count
	 * @return	True if success, otherwise false
	 */
	public boolean createFile(String filePath, long fileSize, int randomSize) {
		if(!System.getProperty("os.name").equals("z/OS")) {
			return vi(filePath, fileSize, randomSize);
		} else {
			return cat(filePath, fileSize, randomSize);
		}
	}
	
	/**
	 * Log off a telnet session
	 * @return
	 */
	public boolean logoff() {
		try {
			//Telnet session is not established yet
			if(!isLogon)
				return true;
			out.write("exit\r\n".getBytes("ISO-8859-1"));
			out.flush();
			in.close();
			out.close();
			isLogon = false;
			logger.debug("telnet session destoryed.");
			return true;
		} catch (IOException e) {
			logger.error("Disconnect telnet session failed.");
			logger.error(e.getMessage());
			return false;
		}
	}
}

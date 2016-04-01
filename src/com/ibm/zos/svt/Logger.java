package com.ibm.zos.svt;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.zos.svt.units.Context;

/**
 * Customized Levels
 * @author Stone WANG
 *
 */
class CustomLevel extends Level {
	private static final long serialVersionUID = -9204202598447720937L;
	
	public static final Level NORM = new CustomLevel("NORM",1100, "sun.util.logging.resources.logging");
	public static final Level INFO = new CustomLevel("INFO",1200, "sun.util.logging.resources.logging");
	public static final Level WARN = new CustomLevel("WARN",1300, "sun.util.logging.resources.logging");
	public static final Level ERROR = new CustomLevel("ERROR",1400, "sun.util.logging.resources.logging");
	public static final Level DEBUG = new CustomLevel("DEBUG",1500, "sun.util.logging.resources.logging"); 
	
	/**
	 * Create a new CustomLevel
	 * @param name	The level name
	 * @param value	The level value
	 * @param resourceBundleName	The resource bundle name
	 */
	CustomLevel(String name, int value, String resourceBundleName) {
		super(name, value,resourceBundleName);
	}
}

/**
 * The message logger
 * @author Stone WANG
 *
 */
public class Logger {
	private java.util.logging.Logger logger = null;
	private String location = "";
	private boolean onlyConsole = false;
	private boolean isLogging = false;
	public static int LogFileSize = 1024 * 1024;
	public static int LogFileCount = 1;
	public static boolean LogConsole = true;
	public static String LogPath = "";
	private static byte[] lock = new byte[0];
	private static HashMap<String,Logger> loggers = new HashMap<String,Logger>();
	private static Logger consoleLogger = null;
	private static java.util.logging.Logger errorLogger = null;
	
	/**
	 * Create a new Logger
	 * @param logger	The low level logger
	 * @param location	The location
	 */
	private Logger(java.util.logging.Logger logger, String location)
	{
		this.logger = logger;
		this.location = location;
	}
	
	/**
	 * Get Logger instance by location
	 * @param location	The location path of the unit in the XML configuration file.
	 * 					Example:
	 * 					Process1/Job1/Step1
	 * @return
	 */
	public static Logger getInstance(String location) {
		synchronized(lock) {
			if(loggers.containsKey(location)) {
				return loggers.get(location);
			} else {
				String className = location.replace('/', '.');
				className = "projectx." + className;
				java.util.logging.Logger logger = java.util.logging.Logger.getLogger(className);
				loggers.put(location, new Logger(logger, location));
				return loggers.get(location);
			} 
		}
	}
	
	/**
	 * Get logger to write all error messages to a file. 
	 * The file is named errors.txt under LogPath.
	 * @return	The error messages logger
	 */
	public static java.util.logging.Logger getErrorLogger() {
		synchronized(lock) {
			if(errorLogger == null) {
				errorLogger = java.util.logging.Logger.getLogger(Logger.class.getName() + ".Error");
				errorLogger.setUseParentHandlers(false);
				for(int i = errorLogger.getHandlers().length - 1 ; i > 0; i--) {
					errorLogger.removeHandler(errorLogger.getHandlers()[i]);
				}
				
				try {
					FileHandler fileHandler = new FileHandler(LogPath + File.separator + "errors.txt",LogFileSize,LogFileCount);
					Formatter fileFormatter = new Formatter(){
						@Override
						public String format(LogRecord record) {
							return record.getMessage() + "\n"; 
						}};
					fileHandler.setFormatter(fileFormatter);
					//fileHandler.setFormatter(new SimpleFormatter());
					fileHandler.setLevel(Level.FINEST);
					errorLogger.addHandler(fileHandler);
				} catch (SecurityException e) {
					System.err.println("Create logger on " + LogPath + File.separator + "log.txt failed.");
					Context.LoggerError = true;
					System.exit(-1);
				} catch (IOException e) {
					System.err.println("Create logger on " + LogPath + File.separator + "log.txt failed.");
					Context.LoggerError = true;
					System.exit(-1);
				}
				
			}
			return errorLogger;
		}
	}
	
	/**
	 * Get logger to write messages to console.
	 * @return	The console logger
	 */
	public static Logger getConsoleLogger() {
		synchronized(lock) {
			if(consoleLogger == null) {
				java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Logger.class.getName() + ".Console");
				logger.setUseParentHandlers(false);
				ConsoleHandler consoleHandler = new ConsoleHandler();
				Formatter consoleFormatter = new Formatter(){
					@Override
					public String format(LogRecord record) {
						return record.getMessage() + "\n"; 
					}};
				consoleHandler.setFormatter(consoleFormatter);
				consoleHandler.setLevel(Level.FINEST);
				for(int i = logger.getHandlers().length - 1 ; i > 0; i--) {
					logger.removeHandler(logger.getHandlers()[i]);
				}
				logger.addHandler(consoleHandler);
				consoleLogger = new Logger(logger, "");
				consoleLogger.onlyConsole = true;
			}
			
			return consoleLogger;
		}
	}
	
	/**
	 * Build handlers of the Logger
	 */
	private void buildHandlers()
	{
		if(!isLogging && !onlyConsole) {
			//Create log files root path if non-exist
			File dir = new File(LogPath);
			if(!dir.exists() && !dir.mkdirs()) {
				System.err.println("Create log directory " + dir.getAbsolutePath() + " failed");
				Context.LoggerError = true;
				System.exit(-1);
			}
			//Create sub directory under root path if non-exist
			File subdir = new File(LogPath + File.separator +  location);
			if(!subdir.exists() && !subdir.mkdirs()) {
				System.err.println("Create log directory " + subdir.getAbsolutePath() + " failed");
				Context.LoggerError = true;
				System.exit(-1);
			}
		
			FileHandler fileHandler;
			ConsoleHandler consoleHandler;
			try {
				logger.setUseParentHandlers(false);
				//Create console handler
				consoleHandler = new ConsoleHandler();
				final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				Formatter consoleFormatter = new Formatter(){
					@Override
					public String format(LogRecord record) {
						return df.format(new Date()) + " " + record.getLevel().getName() + " " + record.getMessage() + "\n"; 
					}};
				consoleHandler.setFormatter(consoleFormatter);
				consoleHandler.setLevel(Level.FINEST);
				for(int i = logger.getHandlers().length - 1 ; i > 0; i--) {
					logger.removeHandler(logger.getHandlers()[i]);
				}
				logger.addHandler(consoleHandler);
				//Create file handler
				fileHandler = new FileHandler(LogPath + File.separator + location + File.separator + "log.txt",LogFileSize,LogFileCount);
				Formatter fileFormatter = new Formatter(){
					@Override
					public String format(LogRecord record) {
						return df.format(new Date()) + " " + record.getLevel().getName() + " " + record.getMessage() + "\n"; 
					}};
				fileHandler.setFormatter(fileFormatter);
				//fileHandler.setFormatter(new SimpleFormatter());
				fileHandler.setLevel(Level.FINEST);
				logger.addHandler(fileHandler);
			} catch (SecurityException e) {
				System.err.println("Create logger on " + LogPath + File.separator + "log.txt failed.");
				Context.LoggerError = true;
				System.exit(-1);
			} catch (IOException e) {
				System.err.println("Create logger on " + LogPath + File.separator + "log.txt failed.");
				Context.LoggerError = true;
				System.exit(-1);
			}	
			isLogging = true;
		}
	}
	
	/**
	 * Log a warning message
	 * @param msg	The warning message
	 */
	public void warn(String msg)
	{
		buildHandlers();
		logger.log(CustomLevel.WARN, msg);
	}
	
	/**
	 * Log a error message
	 * @param msg	The error message
	 */
	public void error(String msg)
	{
		buildHandlers();
		logger.log(CustomLevel.ERROR, msg);
	}
	
	/**
	 * Log a informational message
	 * @param msg	The informational message
	 */
	public void info(String msg)
	{
		buildHandlers();
		logger.log(CustomLevel.INFO, msg);
	}
	
	/**
	 * Log a normal message
	 * @param msg	The normal message
	 */
	public void norm(String msg)
	{
		synchronized(lock) {
			buildHandlers();
			logger.log(CustomLevel.NORM, msg);
			if(this.location.equals("")  ) {
				String parts[]  = msg.split(" +");
				if((parts.length > 1) && (parts[1].equals("ERROR"))) {
					getErrorLogger().log(CustomLevel.ERROR, msg);
				}
			}
		}
	}
	
	/**
	 * Log a debug message
	 * @param msg	The debug message
	 */
	public void debug(String msg)
	{
		String debug = Context.Settings.get("Debug");
	    if((debug != null) && debug.toUpperCase().equals("ON")) {
			buildHandlers();
			logger.log(CustomLevel.DEBUG, msg);
	    }
	}
}

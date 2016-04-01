package com.ibm.zos.svt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Node;

import com.ibm.zos.svt.units.Context;
import com.ibm.zos.svt.units.Process;
import com.ibm.zos.svt.units.ProjectX;
import com.ibm.zos.svt.xml.Reader;


/**
 * Separate thread to check if all sub processes finish run
 * @author Stone WANG
 *
 */
class AllDone extends Thread {  
	private ProjectXMain projectx = null;
	
	/**
	 * Create a AllDone
	 * @param projectx	The ProjectXMain object
	 */
    public AllDone(ProjectXMain projectx) {  
        super("AllDone Handler");  
        this.projectx = projectx;
    }  
    
    /**
     * Run the thread
     */
    public void run() {
    	while(true) { 
    		//If all sub processes finish run, ProjectX exits.
	    	if(projectx.runnerPool.allDone()) {
	    		System.exit(0);
	    	}
    	}
    }  
}

/**
 * Separate thread to check if the exit conditions meet.
 * If run duration exceeds, ProjectX will need to exit.
 * If run end datetime passes, ProjectX will need to exit.
 * @author Stone WANG
 *
 */
class ExitTask extends TimerTask {
	private int duration = -1;
	private String end = "";
	private int period = -1;
	private Timer timer = null;
	private ProjectXMain projectx = null;
	public Logger logger = null;
	
	/**
	 * Create a new ExitTask
	 * @param projectx	The ProjectXMain object
	 * @param timer		The timer
	 * @param period	The period of the timer
	 * @param duration	The run duration in seconds
	 * @param end		The end date time string
	 */
	public ExitTask(ProjectXMain projectx, Timer timer, int period, String duration, String end) {
		this.timer = timer;
		if(duration != "")
			this.duration = convertDuration(duration);
		this.end = end;
		this.period = period;
		this.projectx = projectx;
	}
	
	/**
	 * Convert duration to seconds
	 * @param duration	The duration
	 * @return	The seconds
	 */
	private int convertDuration(String duration) {
		int index = duration.indexOf("d");
		if(index > 0) {
			return 3600 * 24 * Integer.parseInt(duration.substring(0, index));
		}
		index = duration.indexOf("h");
		if(index > 0) {
			return 3600 * Integer.parseInt(duration.substring(0, index));
		}
		index = duration.indexOf("m");
		if(index > 0) {
			return 60 * Integer.parseInt(duration.substring(0, index));
		}
		index = duration.indexOf("s");
		if(index > 0) {
			return Integer.parseInt(duration.substring(0, index));
		}
		return Integer.parseInt(duration);
	}
	
	/**
	 * Run the thread
	 */
	@Override
	public void run() {
		//All sub processes finish run, end of thread
		if(projectx.runnerPool.allDone()) {
			timer.cancel();
			return;
		}
		//Check the end datetime reached
		if(end != "") {
			Date date = new Date();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String curDate = df.format(date);
			//yyyy-MM-ddTHH:mm:ss compared to yyyy-MM-dd HH:mm:ss
			end = end.replace('T', ' ');
			if(curDate.compareTo(end) >= 0) {
				System.exit(0);
			}
		}
		//Check if the run duration exhausted
		if(duration > 0) {
			duration -= period;
			if(duration <= 0) {
				System.exit(0);
			}
		}
	}
	
}

/**
 * The ProjectX main class
 * @author Stone WANG
 *
 */
public class ProjectXMain {
	public ProcessRunnerPool runnerPool = new ProcessRunnerPool();
	private Logger logger = Logger.getInstance("");
	public ProjectX projectx = null;
	
	/**
	 * Exit ProjectX when signals like SIGTERM,SIGKILL received.
	 * @author optistone
	 *
	 */
	private class ExitHandler extends Thread {  
		/**
		 * Create a new ExitHandler
		 */
	    public ExitHandler() {  
	        super("Exit Handler");  
	    }  
	    
	    /**
	     * Run the thread
	     */
	    public void run() {  
	    	//If logger error happens, ProjectX will exit soon. We don't need this thread anymore.
	    	if(Context.LoggerError) {
	    		return;
	    	}
	    	logger.norm("ProjectX Exit.");
	    	//Setup failure, exit directly
	    	if(Context.Exit) {
	    		if(projectx != null) {
    	    		if((projectx.cleanup != null) && !projectx.cleanup.run()) {
    	    			logger.error("ProjectX cleanup run failed.");
    	    		}
    	    	}
	    		return;
	    	}
	    	//Terminate processes
	    	runnerPool.terminate();
	    	//Run Cleanup for each process
	    	while(true) {
	    		
	    		if(runnerPool.allDone()) {
	    			if(projectx != null) {
	    	    		if((projectx.cleanup != null) && !projectx.cleanup.run()) {
	    	    			logger.error("ProjectX cleanup run failed.");
	    	    		}
	    	    		return;
	    	    	}
	    		}
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error("Sleep is interrupted.");
					logger.debug(e.getMessage());
				}
	    	}
	    }  
	}
	
	/**
	 * Create a new ProjectXMain
	 */
	public ProjectXMain() 
	{
		//Add ExitHanlder to be the hook of shutdown
		Runtime.getRuntime().addShutdownHook(new ExitHandler());
	}
	
	/**
	 * Run the tasks
	 * @param stringPairs	The string pairs that contain the information of what tasks to run
	 * @param logPath		The full path of the log files root folder
	 * @param logSize		The log file size in bytes
	 * @param logCount		The log file count
	 * @return	True if success, otherwise false
	 */
	public boolean run(String stringPairs, String logPath, int logSize, int logCount )
	{
		try {
			logger.norm("ProjectX start.");  
			ArrayList<StringPair> pairs = StringPair.parse(stringPairs, " +", "[= ]+");
			//Currently, we only support one pair.
			//This means ProjectX will run all processes defined in the configuration file
			if(pairs.size() != 1) {
				logger.error(stringPairs + " is not supported.");
				return false;
			}
			if(!pairs.get(0).getFirst().equals("File")) {
				logger.error("The first node must be of type File.");
				return false;
			}
			//Read input XML configuration file
			Reader reader = new Reader(logger);
			Node rootNode = reader.read(new File(pairs.get(0).getSecond()));
			if(rootNode == null) {
				logger.error("Read XML configuration file " + pairs.get(0).getSecond() + " failed.");
				return false;
			}
			//Parse input XML configuration file to get a list of Proc
			ProjectX procList = new ProjectX("");
			procList.parse(rootNode);
			Process[] dummy = new Process[1];
			Process[] procs = (Process[])procList.elems.toArray(dummy);
			//Build arguments for each Proc
			//Start Proc one by one with the builded arguments
			ArrayList<String> args = new ArrayList<String>();
			for(int i=0; i < procs.length; i++) {
				args.clear();
				args.add(pairs.get(0).toString());
				args.add("Process=" + procs[i].name);	
				args.add("LogPath=" + logPath);
				args.add("LogSize=" + logSize);
				args.add("LogCount=" + logCount);
				runnerPool.runProcess(procs[i].name, args);
			}
			//Start the thread to detect if all processes finish run
			new AllDone(this).start();
		} catch (Exception e) {
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Main method
	 * @param args	The arguments
	 */
	public static void main(String[] args)
	{
		//Arguments InputXMLFile and LogPath are required. 
		//Others are optional
		if(args.length < 2) {
			System.err.println("Argument error: You must provide paths for input XML file and logging.");
			System.err.println("Format: Scheduler [InputXmlFile] [LogPath] [LogFileSize] [LogFileCount]");
			return;
		}
		//Input XML file path
		String xmlFilePath = args[0];
		//Log files root folder path
		String logPath = args[1];
		//Create sub folder for this run of ProjectX with the current date time 
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
		//Set Logger's log file path
		Logger.LogPath = logPath + File.separator + df.format(date);
		//Parse to get LogFileSize if exist
		if(args.length > 2) {
			try {
				long tmpSize = Utils.getSize(args[2]);
				if(tmpSize > Integer.MAX_VALUE) {
					System.err.println("LogFileSize can't be larger than " + Integer.MAX_VALUE + ".");
					return;
				}
				Logger.LogFileSize = (int)tmpSize;
				if(Logger.LogFileSize <= 0) {
					System.err.println("LogFileSize can't be less than or equal to 0:" + args[2] + ".");
					return;
				}
			}catch(NumberFormatException ex) {
				System.err.println("LogFileSize must be a number:" + args[2] + ".");
				return;
			}
		}
		//Parse to get LogFileCount if exist
		if(args.length > 3) {
			try {
				//Set Logger's log files count
				Logger.LogFileCount = Integer.parseInt(args[3]);
				if(Logger.LogFileCount <= 0) {
					System.err.println("Argument error: LogFileCount must be larger than 0.");
					return;
				}
			} catch (NumberFormatException ex) {
				System.err.println("Argument error: LogFileCount must be an integer.");
				System.err.println("Format: Scheduler [InputXmlFile] [LogPath] [LogFileSize] [LogFileCount]");
				return;
			}
		}
		//Parse input XML configuration file to get the definiation of ProjectX
		Reader reader = new Reader(Logger.getConsoleLogger());
		Node rootNode = reader.read(new File(xmlFilePath));
		if(rootNode == null) {
			return;
		}
		ProjectX projectx = new ProjectX("");
		projectx.parse(rootNode);
		
		ProjectXMain projectXMain = new ProjectXMain();
		projectXMain.projectx = projectx;
		//Run ProjectX Setup step.
		//If failed, run Cleanup step.
		if((projectx.setup != null) && !projectx.setup.run()) {
			System.err.println("ProjectX Setup run failed.");
			if((projectx.cleanup != null) && !projectx.cleanup.run()) {
				System.err.println("ProjectX Cleanup run failed.");
			}
			Context.Exit = true;
			return;
		}
		//Run all Processes defined in the XML configuration file
		if(!projectXMain.run("File=" + xmlFilePath, Logger.LogPath, Logger.LogFileSize, Logger.LogFileCount))
			return;
		//Start thread to monitor if ProjectX end condition meet
		if((projectx.duration != "") || (projectx.end != "")) {
			Timer myTimer = new Timer();
			ExitTask task = new ExitTask(projectXMain, myTimer, 1,projectx.duration, projectx.end);
			myTimer.schedule(task, 1000, 1000);
		}
		
		//Console logging is toggle switched by enter key
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		String line = "";

		while(true) {
			try {
				line = inputReader.readLine();
				
				if(projectXMain.runnerPool.allDone()) 
					break;
				if(line.equals("")) {
					Logger.LogConsole = !Logger.LogConsole;
					if(Logger.LogConsole)
						System.out.println("Console logging is switched on.");
					else
						System.out.println("Console logging is switched off.");
				}
				
				if(projectXMain.runnerPool.allDone()) 
					break;
				
			} catch (IOException e) {
				System.err.println("Reading standard input met IO Exception.");
				System.err.println(e.getMessage());
				break;
			}
		}
	}
}

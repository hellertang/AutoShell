package com.ibm.zos.svt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.w3c.dom.Node;

import com.ibm.zos.svt.units.Context;
import com.ibm.zos.svt.units.Process;
import com.ibm.zos.svt.units.ProjectX;
import com.ibm.zos.svt.xml.Reader;

/**
 * Separate thread to check if the end conditions meet.
 * If run duration exceeds, Process will need to exit.
 * If run end datetime passes, Process will need to exit.
 * @author Stone WANG
 *
 */
class EndTask extends TimerTask {
	private int duration = -1;
	private String end = "";
	private int period = -1;
	private Timer timer = null;
	public Logger logger = null;
	
	/**
	 * Create a new EndTask
	 * @param timer		The timer
	 * @param period	The period of the timer
	 * @param duration	The run duration
	 * @param end		The end date time string
	 */
	public EndTask(Timer timer, int period, String duration, String end) {
		this.timer = timer;
		if(duration != "")
			this.duration = convertDuration(duration);
		this.end = end;
		this.period = period;
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
		//If all run jobs finish, Process will end soon. We don't need this thread anymore.
		if(Context.runJobs.size() > 0) {
			boolean isAlive = false;
			for(int i = 0 ; i < Context.runJobs.size(); i++) {
				if(Context.runJobs.get(i).isAlive()) {
					isAlive = true;
					break;
				}
			}	
			// All jobs run finished
			if(!isAlive) {
				timer.cancel();
				return;
			}
		}
		//Process will end soon. We don't need this thread anymore.
		if(Context.Exit) {
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
 * The Process main class
 * @author Stone WANG
 *
 */
public class ProcessMain {
	private Logger logger = null;
	
	/**
	 * Exit Process when signals like SIGTERM,SIGKILL received.
	 * @author Stone WANG
	 *
	 */
	private class ExitHandler extends Thread {
		public ExitHandler() {  
	        super("Exit Handler");  
	    }  
	    public void run() { 
    		//If logger error happens, Process will exit soon. We don't need this thread anymore.
			if(Context.LoggerError) {
				return;
			}
			logger.norm("Process terminated.");
			boolean isAlive = false;
			for(int i = 0 ; i < Context.runJobs.size(); i++) {
				if(Context.runJobs.get(i).isAlive()) {
					isAlive = true;
					break;
				}
			}	
			
			//Terminate Process if there's still jobs running
			if(isAlive) {
				logger.debug("There are still jobs running.");
				return;
			}
	    }
	}
	
	/**
	 * Monitor the standard input for "Terminate" command issued by ProjectX
	 * @author Stone WANG
	 *
	 */
	private class InterProcessHandler extends Thread {  
		/**
		 * Create a InterProcessHandler
		 */
	    public InterProcessHandler() {  
	        super("Inter Process Handler");  
	    }  
	    
	    /**
	     * Run the thread
	     */
	    public void run() { 
			try {
				while(true) {
					// If all jobs finish run, Process will exit soon. We don't need this thread anymore.
					if(Context.runJobs.size() > 0 ) {
						boolean isAlive = false;
						for(int i = 0 ; i < Context.runJobs.size(); i++) {
							if(Context.runJobs.get(i).isAlive()) {
								isAlive = true;
								break;
							}
						}	
						if(!isAlive) {
							return;
						}
					}
					//Process will exit soon. We don't need this thread anymore.
					if(Context.Exit) {
						return;
					}
					//Read standard input for string "Terminate" 
					if(System.in.available() > 0) {
						BufferedReader wr = new BufferedReader(
								new InputStreamReader(System.in));
						String line = wr.readLine();
						logger.debug("Runner readed String:" + line);
						if(line.equals("Terminate")) {
			    			logger.norm("Process is terminated by ProjectX.");
			    			Context.Exit = true;
							return;
				    	}
						wr.close();
					}
					Thread.sleep(1000);
				}
			} catch (IOException e1) {
				logger.norm("IOException during communicating with ProjectX." + e1.getMessage());
			} 
			catch (Exception e) {
				logger.norm("Exception during communicating with ProjectX." + e.getMessage());
			}
	    }  
	}
	
	private ProjectX projectx;
	public InterProcessHandler interProcessHandler;
	public ExitHandler exitHandler;
	
	/**
	 * Create a new ProcessMain
	 */
	public ProcessMain() 
	{
	}
	
	/**
	 * Start monitors
	 */
	private void startMonitors()
	{
		//Start exit handler thread to monitor Exit event
		exitHandler = new ExitHandler();
		Runtime.getRuntime().addShutdownHook(exitHandler);
		//Start Inter-Process communication thread to monitor ProjectX command
		interProcessHandler = new InterProcessHandler();
		interProcessHandler.start();
	}
	
	/**
	 * Run the Process
	 * @param args	The arguments
	 * @return	True if success, otherwise false
	 * @throws Exception	Parse StringPair exception
	 */
	public boolean run(String[] args) throws Exception
	{
		if((args == null) || (args.length < 2)) {
			return false;
		}
		
		//File
		StringPair fileName = new StringPair(args[0]);
		if(!fileName.getFirst().equals("File")) {
			//logger.error("The correct format is File=XXX");
			return false;
		}
	
		//Process
		StringPair procName = new StringPair(args[1]);
		if(!procName.getFirst().equals("Process")) {
			return false;
		}
		
		//LogPath
		StringPair logPath = new StringPair(args[2]);
		if(!logPath.getFirst().equals("LogPath")) {
			return false;
		}
		
		//LogSize
		StringPair logSize = new StringPair(args[3]);
		if(!logSize.getFirst().equals("LogSize")) {
			return false;
		}
		
		//LogCount
		StringPair logCount = new StringPair(args[4]);
		if(!logCount.getFirst().equals("LogCount")) {
			return false;
		}
		//Build logger
		Logger.LogPath = logPath.getSecond();
		try {
			long tmpSize = Utils.getSize(logSize.getSecond());
			if(tmpSize > Integer.MAX_VALUE) {
				System.err.println("LogFileSize can't be larger than " + Integer.MAX_VALUE + ".");
				return false;
			}
			Logger.LogFileSize = (int)tmpSize;
			if(Logger.LogFileSize <= 0) {
				System.err.println("LogFileSize can't be less than or equal to 0:" + logSize.getSecond() + ".");
				return false;
			}
		}catch(NumberFormatException ex) {
			System.err.println("LogFileSize must be a number:" + logSize.getSecond() + ".");
			return false;
		}
		
		Logger.LogFileCount = Integer.parseInt(logCount.getSecond());
		logger = Logger.getInstance(File.separator + procName.getSecond());
		
		//Start monitors
		startMonitors();
		
		//Parse to retrieve ProjectX and Process
		Reader reader = new Reader(logger);
		Node rootNode = reader.read(new File(fileName.getSecond()));
		if(rootNode == null) {
			return false;
		}
		projectx = new ProjectX("");
		projectx.parse(rootNode);
		Process proc = projectx.getProcessByName(procName.getSecond());
		//Start thread to monitor if Process end condition meet
		if((proc.duration != "") || (proc.end != "")) {
			Timer myTimer = new Timer();
			EndTask task = new EndTask(myTimer, 1,proc.duration, proc.end);
			task.logger = logger;
			myTimer.schedule(task, 1000, 1000);
		}
		//Build the context path to run Process
		Context.Directory.add(new StringPair(args[1]));
		return projectx.run();
	}
	
	/*
	public void terminate() throws Exception {
		//Build the context path
		
		Context.Directory.add(new StringPair(runPaths[1]));
		
		if(projectx != null) {
			logger.info("ProcList terminated.");
			projectx.terminate();
		}
	}
	*/
	
	/**
	 * Main method
	 * @param args	The arguments
	 */
	public static void main(String[] args)
	{	
		try {
			ProcessMain runner = new ProcessMain();
			runner.run(args);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}

package com.ibm.zos.svt.units;

import java.io.File;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.Logger;

/**
 * The process working unit
 * @author Stone WANG
 *
 */
public class Process extends Unit{
	public String name;
	private int jobs = -1;
	public String duration = "";
	public String end = "";
	private Setup setup;
	private Cleanup cleanup;
	private GroupList groupList;
	private JobList jobList;
	
	/**
	 * Create a new Process
	 * @param location	The unit location
	 */
	public Process(String location) {
		this.location = location;
	}
	
	/**
	 * Parse the XML node	
	 * @param node the XML node
	 * @return True if success, otherwise false
	 */
	@Override
	public boolean parse(Node node) {
    	//Process attributes
    	NamedNodeMap attrs = node.getAttributes();
    	Node attr = null;
    	//Required attribute "name"
    	attr = attrs.getNamedItem("name");
    	if(attr != null) {
    		name = attr.getNodeValue();
    	} else {
    		return false;
    	}
    	//Optional attribute "jobs"
    	attr = attrs.getNamedItem("jobs");
    	if(attr != null) {
    		jobs = Integer.parseInt(attr.getNodeValue());
    	}
    	//Optional attribute "duration"
    	attr = attrs.getNamedItem("duration");
    	if(attr != null) {
    		duration = attr.getNodeValue();
    	}
    	//Optional attribute "end"
    	attr = attrs.getNamedItem("end");
    	if(attr != null) {
    		end = attr.getNodeValue();
    	}
    	this.location += File.separator + this.name;
    	//Process can contains a Setup
    	Setup temp1 = new Setup(this.location);
    	NodeList setupNodes = selectNodes("Setup", node);
    	if((setupNodes.getLength() != 0) && temp1.parse(setupNodes.item(0))) {
    		setup = temp1;
    	}
    	//Process can contains a Cleanup
    	Cleanup temp2 = new Cleanup(this.location);
    	NodeList cleanupNodes = selectNodes("Cleanup", node);
    	if((cleanupNodes.getLength() != 0) && temp2.parse(cleanupNodes.item(0))) {
    		cleanup = temp2;
    	}
    	//Process can contains a GroupList
    	GroupList temp3 = new GroupList(this.location);
    	temp3.jobCount = jobs;
    	if(temp3.parse(node)) {
    		groupList = temp3;
    	}
    	//Process can contains a JobList
    	JobList temp4 = new JobList(this.location);
    	temp4.jobCount = jobs;
    	if(temp4.parse(node)) {
    		jobList = temp4;
    	}
    	//Get the message logger
    	logger = Logger.getInstance(this.location);
		return true;
	}
	
	/**
	 * Run the unit
	 * @return True if success, otherwise false
	 */
	@Override
	public boolean run() {
		// Run setup
		if((setup != null) && !setup.run()) {
			logger.error("Setup run failed.");
			//Setup failed, exit from run
			Context.Exit = true;
			return false;
		}
		//Run groups
		if((groupList != null) && !groupList.run()) {
			logger.error("Groups run failed.");
			return false;
		}
		//Run jobs
		if((jobList != null) && !jobList.run()) {
			logger.error("Jobs run failed.");
			return false;
		}
		//Wait until all jobs finish run
		while(true) {
			boolean isAlive = false;
			for(int i = 0 ; i < Context.runJobs.size(); i++) {
				if(Context.runJobs.get(i).isAlive()) {
					isAlive = true;
					break;
				}
			}	
			// All jobs run finished
			if(!isAlive) {
				break;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error("Waiting for jobs finish run is interrupted.");
			}
		}
		
		
		//Sleep to make directory state is the latest
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			logger.error("Sleep is interrupted before cleanup.");
		}
		//Make sure cleanup must run
		Context.Exit = false;
		if((cleanup != null) && !cleanup.run()) {
			logger.error("Cleanup run failed.");
			return false;
		}
			
		return true;
	}
	
	/**
	 * Terminate the unit
	 */
	public void terminate() {
		logger.info("Process " + name + " terminates.");
		if(groupList!= null)
			groupList.terminate();
		if(jobList!=null)
			jobList.terminate();
		if((cleanup != null)  && !cleanup.run()) {
			logger.error("Cleanup run failed.");
		}
	}
}

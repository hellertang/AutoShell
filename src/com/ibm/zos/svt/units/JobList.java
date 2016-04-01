package com.ibm.zos.svt.units;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.JobRunner;
import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.StringPair;

/**
 * Job list unit
 * @author Stone WANG
 *
 */
public class JobList extends Unit{
	private ArrayList<Unit> elems = new ArrayList<Unit>();
	public int jobCount = -1;

	/**
	 * Create a new JobList
	 * @param location	The unit location
	 */
	public JobList(String location) {
		this.location = location;
	}
	
	/**
	 * Parse the XML node
	 * @param The XML node
	 * @return True if success, otherwise false
	 */
	public boolean parse(Node node) {
		//Get the message logger
		logger = Logger.getInstance(this.location);
		//JobList contains a list of Jobs
		NodeList jobNodes = selectNodes("Job", node);
		//There's no Job, return directly
		if(jobNodes.getLength() == 0)
			return false;
		//If job count is not provided, we use the Job nodes count
		if(jobCount == -1)
			jobCount = jobNodes.getLength();
		//Calculate job count digits count
		int count = 0;
		int digits = 0;
		int remain = jobCount;
		while(remain > 0 ) {
			remain /= 10;
			++digits;
		}
		//Create Jobs based on the Job node in a loop until the job count reached.
		while(true) {
			for(int i =0; i< jobNodes.getLength(); i++) {
				if(++count > jobCount) {
					return true;
				}
				Job job = null;
				if(jobCount > jobNodes.getLength()) {
					job = new Job(this.location, String.format("%0" + digits + "d", count));
				} else {
					job = new Job(this.location);
				}
				if(job.parse(jobNodes.item(i))) {
					elems.add(job);
				}
	        }
		}
	}

	/**
	 * Run the unit
	 * @return True if success, otherwise false
	 */
	public boolean run() {
		// Check if specific Job is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			if(!curNode.getFirst().equals("Job")) {
				logger.error("Job pair is expected but not " + curNode.getFirst());
				return false;
			}
			boolean found = false;
			for(int i = 0; i < elems.size(); i++) {
				Job job = (Job)elems.get(i);
				if(job.name.equals(curNode.getSecond())) {
					found = true;		
					JobRunner runner = new JobRunner(elems.get(i));
					runner.start();
					Context.runJobs.add(runner);
					//How to collect Thread running result?
				}
			}
			if(!found) {
				logger.error("Job with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return false;
			}
			return true;
		}
		//There's no specific Job provided, we run each Job inside.
		boolean success = true;
		for(int i=0; i < elems.size() && !Context.Exit; i++) {
			JobRunner runner = new JobRunner(elems.get(i));
			runner.start();
			Context.runJobs.add(runner);
		}
		return success;
	}
	
	/**
	 * Terminate the unit
	 */
	public void terminate() {
		// Check if specific Job is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			if(!curNode.getFirst().equals("Job")) {
				logger.error("Job pair is expected but not " + curNode.getFirst());
				return;
			}
			boolean found = false;
			for(int i = 0; i < elems.size(); i++) {
				Job job = (Job)elems.get(i);
				if(job.name.equals(curNode.getSecond())) {
					elems.get(i).terminate();
					found = true;		
					//How to collect Thread running result?
				}
			}
			if(!found) {
				logger.error("Job with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return;
			}
		}
		//There's no specific Job provided, we terminate each Job inside.
		for(int i=0; i < elems.size(); i++) {
			elems.get(i).terminate();
		}
	}
}

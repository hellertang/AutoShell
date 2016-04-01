package com.ibm.zos.svt.units;

import java.io.File;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.zos.svt.Logger;

/**
 * Group unit
 * @author Stone WANG
 *
 */
public class Group extends Unit{
	public String name;
	private String include;
	private String exclude;
	public int percent;
	private JobList jobs;
	private GroupList groups;
	public int jobCount;
	
	/**
	 * Create a new Group
	 * @param location	The unit location
	 */
	public Group(String location) {
		this.location = location;
	}

	/**
	 * Parse the XML node
	 * @param node The XML node
	 * @return True if success, otherwise false
	 */
	public boolean parse(Node node) {
    	NamedNodeMap attrs = node.getAttributes();
    	Node attr = null;
    	//Required attribute "name"
    	attr = attrs.getNamedItem("name");
    	if(attr != null) {
    		name = attr.getNodeValue();
    	} else {
    		return false;
    	}
    	//Optional attribute "include"
    	attr = attrs.getNamedItem("include");
    	if(attr != null) {
    		include = attr.getNodeValue();
    	}
    	//Optional attribute "exclude"
    	attr = attrs.getNamedItem("exclude");
    	if(attr != null) {
    		exclude = attr.getNodeValue();
    	}
    	//Optional attribute "percent"
    	attr = attrs.getNamedItem("percent");
    	if(attr != null) {
    		percent = Integer.parseInt(attr.getNodeValue());
    	}
    	//Re-calculate job count based on Group "percent"
    	jobCount = jobCount * percent / 100;
    	this.location += File.separator + this.name;
    	//Group can contains a GroupList
    	GroupList groupList = new GroupList(this.location);
    	groupList.jobCount = jobCount;
    	if(groupList.parse(node)) {
    		groups = groupList;
    		return true;
    	}
    	//Or it can contains a JobList alternatively
    	JobList jobList = new JobList(this.location);
    	jobList.jobCount = jobCount;
    	if(jobList.parse(node)) {
    		jobs = jobList;
    	}
    	//Get the message logger
    	logger = Logger.getInstance(this.location);
		return true;
	}

	/**
	 * Run the unit
	 * @return True if success, otherwise false
	 */
	public boolean run() {
		if(groups != null) {
			return groups.run();
		}
		if(jobs != null) {
			return jobs.run();
		}
		return false;
	}
	
	/**
	 * Terminate the unit
	 */
	public void terminate() {
		logger.info("Group " + name + " terminate.");
		if(groups != null) {
			groups.terminate();
			return;
		}
		if(jobs != null) {
			jobs.terminate();
			return;
		}
	}
}

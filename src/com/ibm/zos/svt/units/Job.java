package com.ibm.zos.svt.units;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.StringPair;

/**
 * The job unit
 * @author Stone WANG
 *
 */
public class Job extends Unit{
	public String name;
	private int count = 1;
	private ArrayList<Step> steps;
	private String jobNum = "";
	
	/**
	 * Create a new Job
	 * @param location	The unit location
	 */
	public Job(String location) {
		steps = new ArrayList<Step>();
		this.location = location;
	}
	
	/**
	 * Create a new Job
	 * @param location	The unit location
	 * @param jobNum	The job number
	 */
	public Job(String location, String jobNum) {
		steps = new ArrayList<Step>();
		this.location = location;
		this.jobNum = jobNum;
	}
	
	/**
	 * Parse the XML node
	 * @param node The XML node
	 * @return True if success, otherwise false
	 */
	public boolean parse(Node node) {
		//Job attributes
    	NamedNodeMap attrs = node.getAttributes();
    	Node attr = null;
    	//Required attribute "name"
    	attr = attrs.getNamedItem("name");
    	if(attr != null) {
    		name = attr.getNodeValue();
    	} else {
    		return false;
    	}
    	//Optional attribute "loop"
    	attr = attrs.getNamedItem("loop");
    	if(attr != null) {
    		count = Integer.parseInt(attr.getNodeValue());
    	}
    	//Build the unit location
    	this.location += File.separator + this.name;
    	if(jobNum != "") {
    		this.location += File.separator + this.jobNum;
    	}
    	
    	//Job can contains a list of Steps
    	NodeList childNodes = node.getChildNodes();
    	if(childNodes.getLength() == 0)
			return false;
		for(int i =0; i< childNodes.getLength(); i++) {
			String nodeName = childNodes.item(i).getNodeName();
			short nodeType = childNodes.item(i).getNodeType();
			if(nodeType!=Node.ELEMENT_NODE) {
				continue;
			}
			if(nodeName.equals("Step")) {
				Step step = new Step(this.location, this);
				if(step.parse(childNodes.item(i))) {
					steps.add(step);
				}
			} else {
				logger.warn("Unknown Node Type " + nodeName + "under Job Node.");
			}
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
		if(steps == null) {
			logger.warn("There's no step or exec defined inside Job " + name);
			return false;
		}
		// Check if specific Step is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			boolean found = false;
			for(int i = 0; i < steps.size(); i++) {
				Unit unit = steps.get(i);
				if(unit instanceof Step) {
					Step step = (Step)steps.get(i);
					if(step.name.equals(curNode.getSecond())) {
						found = true;
						return steps.get(i).run();
					}
				} else {
					logger.norm("Unexpected class.");
					return false;
				}
			}
			if(!found) {
				logger.error("Job with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return false;
			}
			return true;
		}
		
		//There's no specific Step provided, we run each Step inside.
		boolean success = true;
		//Run job endless
		if(count == -1) {
			while(true) {
				for(int j=0; j < steps.size() && !Context.Exit; j++) {
					if(!steps.get(j).run()) {
						success = false;
						if(steps.get(j).failure.equals("cancel")) {
							//Logoff telnet session if exist
							if(this.telnetSession != null) {
								this.telnetSession.logoff();
							}
							return false;
						}
					}
				}
				if(Context.Exit)
					break;
			}
		}
		//Run job in a loop with specified count
		for(int i = 0 ; i < count && !Context.Exit; i++) {
			for(int j=0; j < steps.size() && !Context.Exit; j++) {
				if(!steps.get(j).run()) {
					success = false;
					if(steps.get(j).failure.equals("cancel")) {
						//Logoff telnet session if exist
						if(this.telnetSession != null) {
							this.telnetSession.logoff();
						}
						return false;
					}
				}
			}
		}
		//Logoff telnet session if exist
		if(this.telnetSession != null) {
			this.telnetSession.logoff();
		}
		return success;
	}
	
	/**
	 * Terminate the unit
	 */
	public void terminate() {
		logger.info("Job " + name +" terminate.");
		if(steps == null) {
			logger.warn("There's no step or exec defined inside Job " + name);
			return;
		}
		//Logoff telnet session if exist
		if(this.telnetSession != null)
			this.telnetSession.logoff();
		// Check if specific Step is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			boolean found = false;
			for(int i = 0; i < steps.size(); i++) {
				Unit unit = steps.get(i);
				if(unit instanceof Step) {
					Step step = (Step)steps.get(i);
					if(step.name.equals(curNode.getSecond())) {
						found = true;
						steps.get(i).terminate();
						return;
					}
				} else {
					logger.norm("Unexpected class.");
					return;
				}
			}
			if(!found) {
				logger.error("Job with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return;
			}
		}
			
		//There's no specific Step provided, we terminate each Step inside.
		for(int i=0; i < steps.size(); i++) {
			steps.get(i).terminate();
		}
	}

}

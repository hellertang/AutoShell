package com.ibm.zos.svt.units;

import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.StringPair;

/**
 * The project working unit
 * @author Stone
 *
 */
public class ProjectX extends Unit{
	public ArrayList<Unit> elems;
	public String duration = "";
	public String end="";
	public Setup setup;
	public Cleanup cleanup;
	
	/**
	 * Create a new ProjectX
	 * @param location	The unit location
	 */
	public ProjectX(String location) {
		elems = new ArrayList<Unit>();
		this.location = location;
	}
	
	/**
	 * Parse the XML node
	 * @param node The XML node
	 * @return true if success, otherwise false
	 */
	public boolean parse(Node node) {
		NamedNodeMap attrs = node.getAttributes();
    	Node attr = null;
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
    	//ProjectX can contains a Setup
    	Setup temp1 = new Setup(this.location);
    	NodeList setupNodes = selectNodes("Setup", node);
    	if((setupNodes.getLength() != 0) && temp1.parse(setupNodes.item(0))) {
    		setup = temp1;
    	}
    	//ProjectX can contains a Cleanup
    	Cleanup temp2 = new Cleanup(this.location);
    	NodeList cleanupNodes = selectNodes("Cleanup", node);
    	if((cleanupNodes.getLength() != 0) && temp2.parse(cleanupNodes.item(0))) {
    		cleanup = temp2;
    	}
    	//ProjectX can contains lots of Processes
		NodeList processNodes = selectNodes("Process", node);
		if(processNodes.getLength() == 0)
			return false;
		for(int i =0; i< processNodes.getLength(); i++) {
			Process proc = new Process(this.location);
			if(proc.parse(processNodes.item(i))) {
				elems.add(proc);
			}
        }
		//Get the message logger
		logger = Logger.getInstance(this.location);
		return true;
	}
	
	/**
	 * Get a Process by its name
	 * @param procName	The name of the Process
	 * @return	The Process with specified name
	 */
	public Process getProcessByName(String procName) {
		for(int i = 0; i < elems.size(); i++) {
			Process proc = (Process)elems.get(i);
			if(proc.name.equals(procName)) {
				
				return (Process)(elems.get(i));
			}
		}
		return null;
	}
	
	/**
	 * Run the unit
	 */
	public boolean run() {
		// Check if specific Process is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			if(!curNode.getFirst().equals("Process")) {
				logger.error("Process pair is expected but not " + curNode.getFirst());
				return false;
			}
			boolean found = false;
			for(int i = 0; i < elems.size(); i++) {
				Process proc = (Process)elems.get(i);
				if(proc.name.equals(curNode.getSecond())) {
					found = true;
					return elems.get(i).run();
				}
			}
			if(!found) {
				logger.error("Proc with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return false;
			}
			return true;
		}
		//There's no specific Process provided, we run each Process inside.
		boolean success = true;
		for(int i=0; i < elems.size(); i++) {
			if(!elems.get(i).run() && success) {
				success = false;
			}
		}
		return success;
	}

	/**
	 * Terminate the unit
	 */
	public void terminate() {
		// Check if specific Process is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			if(!curNode.getFirst().equals("Process")) {
				logger.error("Process pair is expected but not " + curNode.getFirst());
				return;
			}
			boolean found = false;
			for(int i = 0; i < elems.size(); i++) {
				Process proc = (Process)elems.get(i);
				if(proc.name.equals(curNode.getSecond())) {
					found = true;
					elems.get(i).terminate();
					return; 
				}
			}
			if(!found) {
				logger.error("Proc with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return;
			}
		}
		//There's no specific Process provided, we run each Process inside.
		for(int i=0; i < elems.size(); i++) {
			elems.get(i).terminate();
		}
	}
}

package com.ibm.zos.svt.units;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.Logger;

/**
 * Cleanup unit
 * @author Stone WANG
 *
 */
public class Cleanup extends Unit{
	private ArrayList<Unit> elems;
	
	/**
	 * Create a new Cleanup
	 * @param location 	The unit location
	 */
	public Cleanup(String location) {
		elems = new ArrayList<Unit>();
		this.location = location;
	}
	
	/**
	 * Parse the XML node
	 * @param node The XML node
	 * @return True if success, otherwise false
	 */
	public boolean parse(Node node) {
		//Build the unit location
    	this.location += File.separator + "Cleanup";
    	
    	NodeList childNodes = node.getChildNodes();
    	//There's no Step, return directly
    	if(childNodes.getLength() == 0)
			return false;
    	//Cleanup can contains a list of Steps
		for(int i =0; i< childNodes.getLength(); i++) {
			String nodeName = childNodes.item(i).getNodeName();
			short nodeType = childNodes.item(i).getNodeType();
			if(nodeType!=Node.ELEMENT_NODE) {
				continue;
			}
			if(nodeName.equals("Step")) {
				Step step = new Step(this.location, this);
				if(step.parse(childNodes.item(i))) {
					elems.add(step);
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
		if(elems == null) {
			return false;
		}
					
		//Run each Step inside.
		boolean success = true;
		for(int i=0; i < elems.size(); i++) {
			if(!elems.get(i).run() && success) {
				success = false;
			}
		}
		//Logoff telnet vi session if exist
		if(this.telnetSession != null) {
			this.telnetSession.logoff();
		}
		return success;
	}
	
	/**
	 * Terminate the unit
	 */
	public void terminate() {
		logger.info("Cleanup terminate.");
		if(elems == null) {
			return;
		}
		//Release vi session if exist
		if(this.telnetSession != null)
			this.telnetSession.logoff();
		
		//Terminate each Step inside.
		for(int i=0; i < elems.size(); i++) {
			elems.get(i).terminate();
		}
	}

}

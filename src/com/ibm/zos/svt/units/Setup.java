package com.ibm.zos.svt.units;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.Logger;

/**
 * The working unit for setup
 * @author Stone
 *
 */
public class Setup extends Unit{
	private ArrayList<Unit> elems;
	
	/**
	 * Create a new Setup
	 * @param location	The unit location
	 */
	public Setup(String location) {
		elems = new ArrayList<Unit>();
		this.location = location;
	}
	
	/**
	 * Parse the XML node
	 */
	@Override
	public boolean parse(Node node) {
		//There's no attribute for Setup
    	this.location += File.separator + "Setup";
    	
    	NodeList childNodes = node.getChildNodes();
    	if(childNodes.getLength() == 0)
			return false;
    	//Setup can contains a list of Step
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
	 */
	@Override
	public boolean run() {
		if(elems == null) {
			logger.warn("There's no step or exec defined inside Setup");
			return false;
		}
					
		//Run each Step or Exec inside.
		boolean success = true;
		for(int i=0; i < elems.size(); i++) {
			if(!elems.get(i).run() && success) {
				success = false;
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
		logger.info("Setup terminate.");
		if(elems == null) {
			logger.warn("There's no step or exec defined inside Setup");
			return;
		}
		//Logoff telnet session if exist
		if(this.telnetSession != null)
			this.telnetSession.logoff();
		
		//Terminate each Step or Exec inside.
		for(int i=0; i < elems.size(); i++) {
			elems.get(i).terminate();
		}
	}

}

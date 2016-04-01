package com.ibm.zos.svt.units;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.StringPair;

/**
 * Group list unit
 * @author Stone WANG
 *
 */
public class GroupList extends Unit {
	private ArrayList<Unit> elems = new ArrayList<Unit>();
	public int jobCount = 0;

	/**
	 * Create a GroupList
	 * @param location	The unit location
	 */
	public GroupList(String location) {
		this.location = location;
	}
	
	/**
	 * Parse the XML node
	 * @param node The XML node
	 * @return True if success, otherwise false
	 */
	public boolean parse(Node node) {
		NodeList groupNodes = selectNodes("Group", node);
		//There's no Group, return directly
		if(groupNodes.getLength() == 0)
			return false;
		boolean success = true;
		int totalPercent = 0;
		//GroupList can contains a list of Groups
		for(int i =0; i< groupNodes.getLength(); i++) {
			Group group = new Group(this.location);
			group.jobCount = jobCount;
			if(group.parse(groupNodes.item(i))) {
				totalPercent+=group.percent;
				elems.add(group);
			} else {
				if(success)
					success = false;
			}
        }
		//Total percentage is larger than 100%
		if(totalPercent > 100) {
			logger.error("Groups under " + Context.Directory + "define percentage more than 100.");
			return false;
		}
		//Get the message logger
		logger = Logger.getInstance(this.location);
		return success;
	}

	/**
	 * Run the unit
	 * @return True if success, otherwise false
	 */
	public boolean run() {
		// Check if specific Group is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			if(!curNode.getFirst().equals("Group")) {
				logger.error("Group pair is expected but not " + curNode.getFirst());
				return false;
			}
			boolean found = false;
			for(int i = 0; i < elems.size(); i++) {
				Group group = (Group)elems.get(i);
				if(group.name.equals(curNode.getSecond())) {
					found = true;
					return elems.get(i).run();
				}
			}
			if(!found) {
				logger.error("Group with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return false;
			}
			return true;
		}
		//There's no specific Group provided, we run each Group inside.
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
		logger.info("GroupList terminate.");
		// Check if specific Group is provided
		StringPair curNode = Context.pop();
		if(curNode != null) {
			if(!curNode.getFirst().equals("Group")) {
				logger.error("Group pair is expected but not " + curNode.getFirst());
				return;
			}
			boolean found = false;
			for(int i = 0; i < elems.size(); i++) {
				Group group = (Group)elems.get(i);
				if(group.name.equals(curNode.getSecond())) {
					found = true;
					elems.get(i).terminate();
					return;
				}
			}
			if(!found) {
				logger.error("Group with name " + curNode.getFirst() + " value " + curNode.getSecond() + " wasn't found.");
				return;
			}
		}
		//There's no specific Group provided, we terminate each Group inside.
		for(int i=0; i < elems.size(); i++) {
			elems.get(i).terminate();
		}
	}
}

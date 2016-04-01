package com.ibm.zos.svt.units;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.zos.svt.Logger;

/**
 * The basic working unit of ProjectX
 * @author Stone
 *
 */
public class Step extends Unit{
	public String name;
	private String type;
	private String parms;
	private int count = 1;
	private String delays = "";
	public String failure = "continue";
	private Unit parent = null;
	
	/**
	 * Create a new Step
	 * @param location	The Unit location
	 * @param parent	The parent Unit
	 */
	public Step(String location, Unit parent) {
		this.location = location;
		this.parent = parent;
	}

	/**
	 * Parse the XML node
	 */
	@Override
	public boolean parse(Node node) {
		//Step attributes
    	NamedNodeMap attrs = node.getAttributes();
    	Node attr = null;
    	//Required attribute "name"
    	attr = attrs.getNamedItem("name");
    	if(attr != null) {
    		name = attr.getNodeValue();
    	} else {
    		return false;
    	}
    	//Required attribute "type"
    	attr = attrs.getNamedItem("type");
    	if(attr != null) {
    		type = attr.getNodeValue();
    	} else {
    		return false;
    	}
    	//Optional attribute "parms"
    	attr = attrs.getNamedItem("parms");
    	if(attr != null) {
    		parms = attr.getNodeValue();
    	}
    	//Optional attribute "loop"
    	attr = attrs.getNamedItem("loop");
    	if(attr != null) {
    		count = Integer.parseInt(attr.getNodeValue());
    	}
    	//Optional attribute "delays"
    	attr = attrs.getNamedItem("delays");
    	if(attr != null) {
    		delays = attr.getNodeValue();
    	}
    	//Optional attribute "failure"
    	attr = attrs.getNamedItem("failure");
    	if(attr != null) {
    		failure = attr.getNodeValue();
    	}
    	//Get the message logger by unit location
    	logger = Logger.getInstance(this.location);
		return true;
	}

	/**
	 * Run the unit
	 */
	@Override
	public boolean run() {
		int beforeDelay = 0;
		int validateDelay = 0;
		int afterDelay = 0;
		
		//Parse to retrieve the delays(before,validate,after)
		String[] tokens = delays.split(",");
		beforeDelay = seconds(tokens[0]);
		if(tokens.length > 1)
			validateDelay = seconds(tokens[1]);
		if(tokens.length > 2)
			afterDelay = seconds(tokens[2]);
		
		//Get step class for the given type
		Class stepClass = null;
		com.ibm.zos.svt.steps.AbstractStep stepObj = null;
		try {
			stepClass = Class.forName("com.ibm.zos.svt.steps." + type);
		} catch(ClassNotFoundException ex) {
			logger.error("Step with type " + type + " is not defined yet.");
			return false;
		}
		//Construct step instance
		try {
			Constructor stepCtor = stepClass.getConstructor(String.class, Unit.class);
			stepObj = (com.ibm.zos.svt.steps.AbstractStep)stepCtor.newInstance(location, parent);
		} catch (NoSuchMethodException e) {
			logger.error("Step with type " + type + " construction failed.");
			return false;
		} catch (SecurityException e) {
			logger.error("Step with type " + type + " construction failed.");
			return false;
		} catch (InstantiationException e) {
			logger.error("Step with type " + type + " construction failed.");
			return false;
		} catch (IllegalAccessException e) {
			logger.error("Step with type " + type + " construction failed.");
			return false;
		} catch (IllegalArgumentException e) {
			logger.error("Step with type " + type + " construction failed.");
			return false;
		} catch (InvocationTargetException e) {
			logger.error("Step with type " + type + " construction failed.");
			return false;
		}
		
		boolean result = true;
		//Run step until specified count reached or Context Exit flag is set.
		for(int i = 0;i < count && !Context.Exit; i++) {
			//Start to run
			if(count > 1)
				logger.norm("Step " + name + "(" + (i+1) + ") starts to run.");
			else
				logger.norm("Step " + name + " starts to run.");
			//Delay before run
			if(beforeDelay > 0) {
				try {
					Thread.sleep(1000 * beforeDelay);
				} catch (InterruptedException e) {
					logger.error("Step before delay is interrupted by other thread.");
					return false;
				}
			}
			
			//Run step
			if(!stepObj.run(parms)) {
				result = false;
				if(count > 1)
					logger.error("Step " + name + "(" + (i+1) + ") run failure.");
				else
					logger.error("Step " + name + " run failure.");
				if(this.failure.equals("cancel"))
					return false;
				else
					continue;
			}
			
			//Delay before validate
			if(validateDelay > 0) {
				try {
					Thread.sleep(1000 * validateDelay);
				} catch (InterruptedException e) {
					logger.error("Step validate delay is interrupted by other thread.");
					return false;
				}
			}
			
			//Validate step
			if(!stepObj.validate()) {
				result = false;
				if(count > 1)
					logger.error("Step " + name + "(" + (i+1) + ") validate failure.");
				else
					logger.error("Step " + name + " validate failure.");
				if(this.failure.equals("cancel"))
					return false;
				else
					continue;
			}
			
			//Finish run
			if(count > 1)
				logger.norm("Step " + name + "(" + (i+1) + ") run success.");
			else
				logger.norm("Step " + name + " run success.");
			
			//Delay after run
			if(afterDelay > 0) {
				try {
					Thread.sleep(1000 * afterDelay);
				} catch (InterruptedException e) {
					logger.error("Step after delay is interrupted by other thread.");
					return false;
				}
			}
		}
		return result;
	}

	/**
	 * Terminate the unit
	 */
	@Override
	public void terminate() {
		logger.info("Step " + name + " terminate.");
		// TODO Auto-generated method stub
	}
	
	/**
	 * Calculate seconds from the input time string
	 * @param timeStr	The input time string
	 * @return	The seconds
	 */
	private int seconds(String timeStr) {
		int multiplier = 1;
		int count = 0;
		
		if(timeStr.trim().length() == 0)
			return 0;
		
		char unit = timeStr.charAt(timeStr.length() - 1); 
		//There's no unit specified. The default is "s" which means seconds.
		if((unit != 'd') && (unit != 'h') && (unit != 'm') && (unit != 's')) {
			try {
				count = Integer.parseInt(timeStr);
			} catch(NumberFormatException ex) {
				logger.warn("The input delay " + timeStr + " is not a valid integer.");
			}
		} else {
			try {
				count = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
			} catch(NumberFormatException ex) {
				logger.warn("The input delay " + timeStr + " is not a valid integer.");
			}
		}
		//Seconds of day is 24*60*60
		//Seconds of hour is 60*60
		//Seconds of minute is 60
		if(unit == 'd') 
			multiplier = 24 * 60 * 60;
		if(unit == 'h') 
			multiplier = 60 * 60;
		if(unit == 'm') 
			multiplier = 60;
		
		return count * multiplier;
	}
}

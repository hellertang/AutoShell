package com.ibm.zos.svt.units;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.zos.svt.TelnetSession;
import com.ibm.zos.svt.Logger;

/**
 * The basic working unit
 * @author Stone
 *
 */
public abstract class Unit {
	protected boolean runnable;
	protected String location;
	public TelnetSession telnetSession = null;
	protected Logger logger = Logger.getInstance("");
	
	/**
	 * Parse the unit
	 * @param node	The XML node
	 * @return	True if success, otherwise false
	 */
	public abstract boolean parse(Node node);
	
	/**
	 * Run the unit
	 * @return	True if success, otherwise false
	 */
	public abstract boolean run();
	
	/**
	 * Terminate the unit
	 */
	public abstract void terminate();
	
	/**
	 * Select XML Nodes using XPath.
	 * @param express	The XPath expression
	 * @param source	The source XML Node
	 * @return	The matched XML nodes or Null
	 */
	protected NodeList selectNodes(String express, Object source) {
        NodeList result=null;
        XPathFactory xpathFactory=XPathFactory.newInstance();
        XPath xpath=xpathFactory.newXPath();
        try {
            result=(NodeList) xpath.evaluate(express, source, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        
        return result;
    }
}

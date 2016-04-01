package com.ibm.zos.svt.xml;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class hintError {

	/**
	 * Read XML configuration file.
	 * 
	 * @param xmlFile
	 *            The XML configuration file
	 * @return The XML root node
	 */
	public Node read(File xmlFile) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Element root = null;
		try {
			factory.setIgnoringElementContentWhitespace(true);

			factory.setValidating(true);

			factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
					XMLConstants.W3C_XML_SCHEMA_NS_URI);
			factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource",
					new InputSource(Reader.class.getResourceAsStream("/ProjectX.xsd")));

			DocumentBuilder db = factory.newDocumentBuilder();
			Document xmldoc = db.parse(xmlFile);

			root = xmldoc.getDocumentElement();
			judgeStr(root);
		} catch (ParserConfigurationException e) {
			e.getMessage();
			return null;
		} catch (SAXException e) {
			e.getMessage();
			return null;
		} catch (IOException e) {
			e.getMessage();
			return null;
		}
		return root;
	}

	/**
	 * Judge XML configuration file
	 * 
	 * @param root
	 *            The XML root node
	 * 
	 */
	private void judgeStr(Node root) {
		NodeList nodes = selectNodes("Process/Job", root);
		if (nodes.getLength() > 0) {
			System.out.println(nodes.getLength());
			for (int i = 0; i < nodes.getLength(); i++) {
				String job = nodes.item(i).getAttributes().getNamedItem("name").getTextContent();
				String loop = nodes.item(i).getAttributes().getNamedItem("loop").getTextContent();
				System.out.println("nodes==============>" + job);
				System.out.println("loop===============>" + loop);
			}
		}
		
		nodes = selectNodes("Process/Job/Step", root);
		if (nodes.getLength() > 0) {
			System.out.println(nodes.getLength());
			for (int i = 0; i < nodes.getLength(); i++) {
				String parms = nodes.item(i).getAttributes().getNamedItem("parms").getTextContent();
				System.out.println("parms============>" + parms);
			}
		}
	}

	/**
	 * Select XML Nodes using XPath.
	 * 
	 * @param express
	 *            The XPath expression
	 * @param source
	 *            The source XML Node
	 * @return The matched XML nodes or Null
	 */
	private NodeList selectNodes(String express, Object source) {
		NodeList result = null;
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		try {
			result = (NodeList) xpath.evaluate(express, source, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.getMessage();
		}
		return result;
	}

	public static void main(String[] args) {
		File file = new File("Stress1.xml");
		hintError t = new hintError();
		t.read(file);
	}
}

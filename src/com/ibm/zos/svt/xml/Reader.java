package com.ibm.zos.svt.xml;

import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.ibm.zos.svt.Logger;
import com.ibm.zos.svt.units.Context;

/**
 * The XML file reader
 * 
 * @author Stone
 *
 */
public class Reader {
	private Logger logger = null;

	public Reader(Logger logger) {
		this.logger = logger;
	}

	/**
	 * The XML parsing error handler
	 * 
	 * @author Stone
	 *
	 */
	private class MyErrorHandler implements ErrorHandler {
		/**
		 * Create a new MyErrorHandler
		 */
		MyErrorHandler() {
		}

		/**
		 * Receive notification of a warning.
		 */
		public void warning(SAXParseException spe) throws SAXException {
			logger.warn(getParseExceptionInfo(spe));
		}

		/**
		 * Receive notification of a recoverable error.
		 */
		public void error(SAXParseException spe) throws SAXException {
			String message = "Error: " + getParseExceptionInfo(spe);
			throw new SAXException(message);
		}

		/**
		 * Receive notification of a non-recoverable error.
		 */
		public void fatalError(SAXParseException spe) throws SAXException {
			String message = "Fatal Error: " + getParseExceptionInfo(spe);
			throw new SAXException(message);
		}
	}

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
			db.setErrorHandler(new MyErrorHandler());
			Document xmldoc = db.parse(xmlFile);

			root = xmldoc.getDocumentElement();
			updateSettings(root);
		} catch (ParserConfigurationException e) {
			logger.error(e.getMessage());
			return null;
		} catch (SAXException e) {
			logger.error(e.getMessage());
			return null;
		} catch (IOException e) {
			logger.error("File " + xmlFile + " can't be found.");
			return null;
		}
		return root;
	}

	/**
	 * Update settings in Context.
	 * 
	 * @param root
	 *            The XML root node
	 */
	private void updateSettings(Node root) {
		Context.Settings.clear();
		NodeList nodes = selectNodes("Settings/Account", root);
		if (nodes.getLength() > 0) {
			String account = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("Account", account);
			System.out.println("account============>" + account);
		}
		nodes = selectNodes("Settings/Password", root);
		if (nodes.getLength() > 0) {
			String password = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("Password", password);
			System.out.println("password==========>" + password);
		}
		nodes = selectNodes("Settings/ShellPrompt", root);
		if (nodes.getLength() > 0) {
			String shellPrompt = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("ShellPrompt", shellPrompt);
			System.out.println("============>" + shellPrompt);
		}
		nodes = selectNodes("Settings/JVMOptions", root);
		if (nodes.getLength() > 0) {
			String jvmOptions = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("JVMOptions", jvmOptions);
			System.out.println("jvmOptions==============>" + jvmOptions);
		}
		nodes = selectNodes("Settings/Debug", root);
		if (nodes.getLength() > 0) {
			String debug = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("Debug", debug);
			System.out.println("debug============>" + debug);
		}
		nodes = selectNodes("Settings/DebugPort", root);
		if (nodes.getLength() > 0) {
			String debugPort = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("DebugPort", debugPort);
			System.out.println("debugPort=========>" + debugPort);
		}
		nodes = selectNodes("Settings/lrecl", root);
		if (nodes.getLength() > 0) {
			String lrecl = nodes.item(0).getFirstChild().getNodeValue();
			Context.Settings.put("lrecl", lrecl);
			System.out.println("lrecl===============>" + lrecl);
		}

//		nodes = selectNodes("Process/Job", root);
//		if (nodes.getLength() > 0) {
//			System.out.println(nodes.getLength());
//			String job = nodes.item(0).getAttributes().getNamedItem("name").getTextContent();
//			String loop=nodes.item(0).getAttributes().getNamedItem("loop").getTextContent();
//			System.out.println("nodes==============>" + job);
//			System.out.println("loop===============>" + loop);
//		}
//		
//		nodes =selectNodes("Process/Job/Step",root);
//		if(nodes.getLength()>0){
//			System.out.println(nodes.getLength());
//			String parms=nodes.item(0).getAttributes().getNamedItem("parms").getTextContent();
//			String parms1=nodes.item(1).getAttributes().getNamedItem("parms").getTextContent();
//			System.out.println("parms============>"+parms);
//			System.out.println("parms1===========>"+parms1);
//		}
		

	}

	// public boolean validate(Document doc) {
	//
	// DOMSource xmlSource = new DOMSource(doc);
	// SchemaFactory factory =
	// SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	// try {
	// System.out.println(DOMSource.FEATURE);
	// Schema schema =
	// factory.newSchema(Reader.class.getResource("/ProjectX.xsd"));
	// //Schema schema = factory.newSchema(new File("/u/wangzx/ProjectX.xsd"));
	//
	// Validator validator = schema.newValidator();
	// System.out.println("before " +
	// validator.getFeature("http://apache.org/xml/features/validation/schema-full-checking"));
	// System.out.println("before " +
	// validator.getFeature("http://xml.org/sax/features/namespaces"));
	// System.out.println("before " +
	// validator.getFeature("http://xml.org/sax/features/namespace-prefixes"));
	// System.out.println("before " +
	// validator.getFeature("http://apache.org/xml/features/validation/schema-full-checking"));
	// validator.setFeature("http://apache.org/xml/features/validation/schema-full-checking",
	// false);
	// System.out.println("before " +
	// validator.getProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation"));
	// validator.setErrorHandler(null);
	// validator.validate(xmlSource);
	// } catch (SAXException e) {
	// logger.error("XML configuration file validation failed.");
	// logger.error(e.getMessage());
	// return false;
	// } catch (IOException e) {
	// logger.error("Schema file content error.");
	// return false;
	// }
	// return true;
	// }

	/**
	 * Retrieve paring error information
	 * 
	 * @param spe
	 *            The SAXParseException object
	 * @return The error information
	 */
	private String getParseExceptionInfo(SAXParseException spe) {
		String systemId = spe.getSystemId();

		if (systemId == null) {
			systemId = "null";
		}

		String info = systemId + " Line=" + spe.getLineNumber() + ": " + spe.getMessage();

		return info;
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
			logger.error(e.getMessage());
		}

		return result;
	}
}

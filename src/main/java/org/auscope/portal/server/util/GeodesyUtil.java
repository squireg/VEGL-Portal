package org.auscope.portal.server.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GeodesyUtil {
	
	private static final Log logger = LogFactory.getLog(GeodesyUtil.class);
	
	public static List<String> getSelectedGPSFiles(String xmlUrlText){

		List<String> urlsList = new ArrayList<String>();
		// Extract the url values
		// from the xmlText sent in the request parameter.
		try {
			logger.debug("xml text to transfer is " + xmlUrlText);
			Document doc = getXmlDocumentParser(xmlUrlText);
			NodeList nodeLst = doc.getElementsByTagName("fileUrl");

			for (int s = 0; s < nodeLst.getLength(); s++) {
					
				Node urlNode = nodeLst.item(s);
					
				if (urlNode.getNodeType() == Node.ELEMENT_NODE) {
					String urlValue = getUrlValueFromXmlNode(urlNode);
					//logger.debug("The url value during transfer is " + urlValue);
					urlsList.add(urlValue);
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
		if (urlsList.size() == 0) {
			return null;
		}
	   	return urlsList;
	}	
	/**
	 * @param xmlUrlText
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getXmlDocumentParser(String xmlUrlText)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = factory.newDocumentBuilder();
		InputSource inStream = new InputSource();
		inStream.setCharacterStream(new StringReader(xmlUrlText));
		Document doc = db.parse(inStream);

		doc.getDocumentElement().normalize();
		return doc;
	}
	
	/**
	 * This function extracts url values from
	 * a dst:url node.
	 * @param urlNode
	 * @return
	 */
	private static String getUrlValueFromXmlNode(Node urlNode) {
		Element urlElmnt = (Element) urlNode;
		String urlValue = urlElmnt.getFirstChild().getNodeValue();
		//logger.debug("The url value during transfer is " + urlValue);
		
		return urlValue;
	}
	
	
}

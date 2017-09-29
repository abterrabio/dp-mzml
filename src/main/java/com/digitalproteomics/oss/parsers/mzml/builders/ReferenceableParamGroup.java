package com.digitalproteomics.oss.parsers.mzml.builders;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

/**
 * Captures only cvParam tags and its attributes and stores the elements in a byte array of a ReferenceableParamGroup
 *  within an mzML file. The params are pulled via a {@code XMLStreamReader} using {@code getParams()} method.
 */
public class ReferenceableParamGroup implements FromXMLStreamBuilder<ReferenceableParamGroup> {
	
	
	public ReferenceableParamGroup(XMLStreamReader xr){
		this.id = xr.getAttributeValue(null, "id");
		
		this.baos = new ByteArrayOutputStream();
		try {
			this.xmlw = OUT_FACTORY.createXMLStreamWriter(this.baos);
			this.xmlw.writeStartElement(xr.getLocalName());
		} catch (XMLStreamException e) {
			LOGGER.log(Level.ERROR, e.toString());
		}
	}
	
	@Override
	public void accept(XMLStreamReader xr) {
		if( xr.getEventType() == XMLStreamConstants.START_ELEMENT 
				&& xr.getLocalName().equals("cvParam")){
			try {
				this.xmlw.writeStartElement(xr.getLocalName());
				for(int i = 0; i < xr.getAttributeCount(); i++){
					this.xmlw.writeAttribute(xr.getAttributeLocalName(i), xr.getAttributeValue(i));
				}
				this.xmlw.writeEndElement();
			} catch (XMLStreamException e) {
				LOGGER.log(Level.ERROR, e.toString());
			}
		}
	}

	@Override
	public ReferenceableParamGroup build() {
		try {
			this.xmlw.writeEndElement();
			this.xmlw.close();
			this.baos.flush();
		} catch (IOException | XMLStreamException e) {
			LOGGER.log(Level.ERROR, e.toString());
			return null;
		}
		
		return this;
	}
	
	/** 
	 * Gets referenced param group as a new stream of XML elements 
	 * 
	 **/
	public XMLStreamReader getParams(){
		try {
			return IN_FACTORY.createXMLStreamReader(new ByteArrayInputStream(this.baos.toByteArray()));
		} catch (XMLStreamException | FactoryConfigurationError e) {
			LOGGER.log(Level.ERROR, e.toString());
			return null;
		}
	}
	
	public String getId(){
		return this.id;
	}

	/** name of value used in group **/
	protected String id;
	
	/** byte stream contains XML elements **/
	private ByteArrayOutputStream baos;
	/** xml writer to the bytestream **/
	private XMLStreamWriter xmlw;
	
	final static Logger LOGGER = LogManager.getLogger(MzMLStAXParser.class);
	final static XMLInputFactory IN_FACTORY = XMLInputFactory.newFactory();
	final static XMLOutputFactory OUT_FACTORY = XMLOutputFactory.newFactory();
}

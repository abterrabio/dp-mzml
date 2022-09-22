/******************************************************************************
   Copyright 2017 Digital Proteomics, LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
******************************************************************************/
package com.digitalproteomics.oss.parsers.mzml;

import com.digitalproteomics.oss.parsers.mzml.builders.FromXMLStreamBuilder;
import com.digitalproteomics.oss.parsers.mzml.builders.ReferenceableParamGroup;
import com.digitalproteomics.oss.parsers.mzml.builders.SpectrumIndexer;
import com.digitalproteomics.oss.parsers.mzml.builders.XMLChromatogramBuilder;
import com.digitalproteomics.oss.parsers.mzml.model.Chromatogram;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.*;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A generic parser of a mzML file that iterates over chromatogram list.
 */
public class MzMLChromatogramStAXParser implements Iterable<Chromatogram> {

	/**
	 * @param xml path to mzml file
	 */
	public MzMLChromatogramStAXParser(Path xml) {
		this.xml = xml;
	}

	/** Used for debugging xml elements **/
	static void printElementState(XMLStreamReader xr) {
		StringBuilder sb = new StringBuilder();
		sb.append("ElemState: ")
			.append(xr.getEventType())
			.append("\t")
			.append(xr.hasName())
			.append("\t")
			.append(xr.isStartElement())
			.append("\t")
			.append(xr.isEndElement())
			.append("\t")
			.append(xr.isCharacters());
	
		if(xr.hasName()){
			sb.append("\t").append(xr.getName());
		}
		if(xr.hasText()){
			sb.append("\t").append(xr.getText());
		}

		System.out.println(sb.toString());
	}

	/**
	 * Iterator over chromatogram tags, and parses FromXMLStreamBuilder built instances
	 */
	protected class FromXMLStreamIterator implements Iterator<Chromatogram> {
		XMLStreamReader xr;
		
		public FromXMLStreamIterator() throws XMLStreamException {
			XMLInputFactory fac = XMLInputFactory.newInstance();
			try { 
				this.xr = fac.createXMLStreamReader(Files.newInputStream(MzMLChromatogramStAXParser.this.xml, StandardOpenOption.READ));
			} catch (FactoryConfigurationError | IOException e) {
				LOGGER.log(Level.ERROR, e.getMessage());
				System.exit(-1);
			}
			if (!this.moveToNextChromatogram()){
				LOGGER.log(Level.WARN,  "no chromatogram found in mzml file");
			}
		}
		
		/**
		 * Jumps to first chromatogram xml tag.
		 * @return
		 * @throws XMLStreamException
		 */
		public boolean moveToNextChromatogram() throws XMLStreamException {
			while (this.xr.hasNext()) {
				this.xr.next();
				if (this.xr.getEventType() == XMLStreamConstants.START_ELEMENT
						&& this.xr.getLocalName().equals("chromatogram")) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public boolean hasNext() {
			return this.xr.getEventType() == XMLStreamConstants.START_ELEMENT 
					&& this.xr.getLocalName().equals("chromatogram");
		}
	
		@Override
		public Chromatogram next() {
			// assumes inside chromatogram based on initialization and hasNext
			FromXMLStreamBuilder<Chromatogram> consumer = new XMLChromatogramBuilder(MzMLChromatogramStAXParser.this.xml.toString(),
					this.xr);
			try {
				while (this.xr.hasNext()) {
					this.xr.next();
				
					if (this.xr.getEventType() == XMLStreamConstants.END_ELEMENT
							&& this.xr.getLocalName().equals("chromatogram")){
						this.moveToNextChromatogram();
						return consumer.build();
					}

					consumer.accept(this.xr);
				}
			} catch(XMLStreamException e){
				LOGGER.log(Level.ERROR, e.toString());
				// if xml parsing error, tries the next record.
				return this.next();
			}
			// hasNext should be called before next to ensure there is another spectrum to process
			return null;
		}
	}
	
	@Override
	public Iterator<Chromatogram> iterator() {
		try {
			return new FromXMLStreamIterator();
		} catch (XMLStreamException e){
			LOGGER.log(Level.ERROR, e.toString());
			return Collections.emptyIterator();
		}
	}

	/** path to mzml file **/
	protected Path xml;

	final static Logger LOGGER = LogManager.getLogger(MzMLChromatogramStAXParser.class);
}

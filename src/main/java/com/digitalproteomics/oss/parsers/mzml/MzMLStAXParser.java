package com.digitalproteomics.oss.parsers.mzml;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.digitalproteomics.oss.parsers.mzml.builders.ReferenceableParamGroup;
import com.digitalproteomics.oss.parsers.mzml.builders.SpectrumIndexer;

/**
 * A generic parser of an mzml file. The parser is an iterable over spectrum tags,
 * and can random access to a spectrum tag using the indexedMzML's index tags. 
 *
 * @param <T> an instance that can be built by a {@code FromXMLStreamBuilder}
 */
public class MzMLStAXParser<T> implements Iterable<T>, Closeable {

	
	/** 
	 * @param xml path to mzml file
	 * @param factory method reference to a spectrum builder's constructor. 
	 * @param parseIndex required for random access
	 * @param indexScanTimes scan times are indexed for random access
	 */
	public MzMLStAXParser(Path xml, 
			FromXMLStreamBuilderFactory<T> factory,
			boolean parseIndex,
			boolean indexScanTimes){
		this.xml = xml;
		this.factory = factory;
		this.refParams = new HashMap<String, ReferenceableParamGroup>();
		
		try {
			if(parseIndex){
				this.seekable = Files.newByteChannel(this.xml, StandardOpenOption.READ);
				this.parseIndex(indexScanTimes);
			}
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.toString());
			System.exit(-1);
		} catch (XMLStreamException e) {
			LOGGER.log(Level.WARN, "No random access to spectra. " + e.getMessage());
			e.printStackTrace();
			this.spectrumOffsets = null;
		}
	}

	/**
	 * @param xml
	 * @param factory
	 */
	public MzMLStAXParser(Path xml, FromXMLStreamBuilderFactory<T> factory){
		this(xml, factory, true, false);
	}

	@Override
	public void close() throws IOException {
		if(this.seekable != null){
			this.seekable.close();	
		}
	}
	
	/** Used for debugging xml elements **/
	static void printElementState(XMLStreamReader xr){
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
	 * Accepts states of an xml stream, and builds instances from nested xml tags
	 * 
	 * @param <S> the type of object built
	 */
	public interface FromXMLStreamBuilder<S> extends Consumer<XMLStreamReader>{
		/** Constructs an instance of type T after consuming xml states **/
		S build();
	}

	/**
	 * Factory for creating a xml stream handler that builds instances of a given type
	 */
	public interface FromXMLStreamBuilderFactory<S> {

		/**
		 * @param fileName - origin of xml
		 * @param xr - the {@code XMLStreamConstants.START_ELEMENT} state of the first element for construction
		 * @return
		 */
		FromXMLStreamBuilder<S> create(String fileName, XMLStreamReader xr);
	}
		
	/**
	 * Iterator over spectrum tags, and parses FromXMLStreamBuilder built instances 
	 */
	protected class FromXMLStreamIterator implements Iterator<T> {
		XMLStreamReader xr;
		
		public FromXMLStreamIterator() throws XMLStreamException {
			XMLInputFactory fac = XMLInputFactory.newInstance();
			try { 
				this.xr = fac.createXMLStreamReader(Files.newInputStream(MzMLStAXParser.this.xml, StandardOpenOption.READ));
			} catch (FactoryConfigurationError | IOException e) {
				LOGGER.log(Level.ERROR, e.getMessage());
				System.exit(-1);
			}
			if (!this.forwardToNextSpectrum()){
				LOGGER.log(Level.WARN,  "no spectrum found in mzml file");
			}
		}
		
		/**
		 * Jumps to first spectrum xml, and parses referenceable param groups elements along the way
		 * @return
		 * @throws XMLStreamException
		 */
		public boolean forwardToNextSpectrum() throws XMLStreamException {
			ReferenceableParamGroup currGroup = null;
			
			while(this.xr.hasNext()){
				this.xr.next();
				if(this.xr.getEventType() == XMLStreamConstants.START_ELEMENT){
					if(this.xr.getLocalName().equals("referenceableParamGroup")){
						currGroup = new ReferenceableParamGroup(this.xr);
					} else if(this.xr.getLocalName().equals("spectrum")){
						return true;
					} else if(currGroup != null){
						currGroup.accept(this.xr);
					}
				} else if(this.xr.getEventType() == XMLStreamConstants.END_ELEMENT 
						&& this.xr.getLocalName().equals("referenceableParamGroup")){
					MzMLStAXParser.this.refParams.put(currGroup.getId(), currGroup.build());
					currGroup = null;
				}
			}
			return false;
		}
		
		@Override
		public boolean hasNext() {
			return this.xr.getEventType() == XMLStreamConstants.START_ELEMENT 
					&& this.xr.getLocalName().equals("spectrum");
		}
	
		@Override
		public T next() {
			// assumes inside spectrum based on initialization and hasNext
			FromXMLStreamBuilder<T> consumer = MzMLStAXParser.this.factory.create(MzMLStAXParser.this.xml.toString(), 
					this.xr);
			try {
				while(this.xr.hasNext()){
					this.xr.next();
				
					if(this.xr.getEventType() == XMLStreamConstants.END_ELEMENT 
							&& this.xr.getLocalName().equals("spectrum")){
						this.forwardToNextSpectrum();
						return consumer.build();
					}
					
					if(this.xr.getEventType() == XMLStreamConstants.START_ELEMENT 
							&& this.xr.getLocalName().equals("referenceableParamGroupRef")){
						ReferenceableParamGroup group = MzMLStAXParser.this.refParams.get(this.xr.getAttributeValue(null, "ref"));
						if(group == null){
							LOGGER.log(Level.ERROR, "ReferencableParamGroup id :" + this.xr.getAttributeValue(null, "ref") + " was not found in file");
						} else {
							XMLStreamReader refXr = group.getParams();
							while(refXr.hasNext()){
								refXr.next();
								consumer.accept(refXr);
							}
						}
						
						
					} else {
						consumer.accept(this.xr);
					}
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
	public Iterator<T> iterator() {
		try {
			return new FromXMLStreamIterator();
		} catch (XMLStreamException e){
			LOGGER.log(Level.ERROR, e.toString());
			return Collections.emptyIterator();
		}
	}
	
	private class StartElementFilter implements javax.xml.stream.StreamFilter {
		@Override
		public boolean accept(XMLStreamReader reader) {
			return reader.getEventType() == XMLStreamConstants.START_ELEMENT;
		}
	}
	
	/**
	 * Loads {@code indexToOffset} and {@code idToOffset} for random access to spectra. 
	 * 
	 *  1) Checks for an indexedmzML,
	 *  2) Finds indexList by backtracking from end of file
	 *  3) Parses indexList to construct {@code spectrumOffsets} Indexer member
	 *  4) Iterates over the xml file to gather scanTimes [optional]  
	 */
	protected void parseIndex(boolean indexScanTimes) throws XMLStreamException {
		XMLInputFactory xmlFac = XMLInputFactory.newInstance();
		
		// 1) find indexedmzML
		try(InputStream is = Files.newInputStream(this.xml, StandardOpenOption.READ)) {
			XMLStreamReader xr = xmlFac.createFilteredReader(xmlFac.createXMLStreamReader(is), new StartElementFilter());
			switch (xr.getLocalName()) {
				case "indexedmzML":
					break;
				case "mzML":
					throw new XMLStreamException("mzML file with no indexing");
				default:
					throw new XMLStreamException("No indexedmzML tag found.");
			}
			
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.toString());
			return;
		}

		// 2) find indexList and sets seekable to be at the new offset
		XMLStreamReader xr = null;
		boolean hasIndexList = false;
		long offsetFromStart = 0;
		
		ByteBuffer bb = ByteBuffer.allocate(4096);
		Pattern indexListTag = Pattern.compile("<indexList[ >]");
		Matcher indexListFind;
		
		endFilePass : for(long offsetFromEnd = 1024; offsetFromEnd <= MzMLStAXParser.MAX_MEGABYTE_FROM_END * (1024 * 1024); offsetFromEnd = offsetFromEnd << 1 ) {
			// finds first "<indexList" character sequence			
			try {
				offsetFromStart = Math.max(0, this.seekable.size() - offsetFromEnd);
				
				this.seekable = this.seekable.position(offsetFromStart);
				while(this.seekable.read(bb) > 0 ){
					bb.flip();
					
					indexListFind = indexListTag.matcher(Charset.forName("UTF-8").decode(bb));
					if(indexListFind.find()){
						offsetFromStart += indexListFind.start();
						hasIndexList = true;
						this.seekable = this.seekable.position(offsetFromStart);
						break endFilePass;
					}
					
					offsetFromStart += bb.limit();
					bb.rewind();
				}
			} catch (IOException e){
				LOGGER.log(Level.ERROR, e.toString());
				return;	
			}
		}
		
		if(!hasIndexList) {
			throw new XMLStreamException("Could not find indexList starting at the end of file.");
		}
		
		// 3) parse and set indexer
		SpectrumIndexer indexer = null;
		InputStream is = Channels.newInputStream(this.seekable);
		xr = xmlFac.createXMLStreamReader(is);
		hasIndexList = false;
		xmlPass : while (xr.hasNext()) {
			xr.next();
			
			if(indexer != null) {
				indexer.accept(xr);
			}
			
			eventPass : switch (xr.getEventType()) {
				case XMLStreamConstants.START_ELEMENT:
					switch (xr.getLocalName()) {
						case "indexList":
							hasIndexList = true;
							break eventPass;
						case "index":
							indexer = new SpectrumIndexer(xr);
							break eventPass;
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					switch (xr.getLocalName()) {
						case "indexList":
							hasIndexList = false;
							break xmlPass;
						case "index":
							if(indexer.getName().equals("spectrum")) {
								this.spectrumOffsets = indexer;
							}
							indexer = null;	
							break eventPass;
					}
					break;
			}
		}
		
		// 4) sets the scan time offsets available
		if(indexScanTimes && this.spectrumOffsets != null){
			try {
				this.spectrumOffsets.setScanTimeToOffsets(this.xml);
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.toString());
				return;
			}
		}
		
	}

	private T getNextSpectrumFromSeekable() {
		FromXMLStreamBuilder<T> spectrumBuilder = null;
		try {
			InputStream is = Channels.newInputStream(this.seekable);
			XMLStreamReader xr = XMLInputFactory.newInstance()
				.createXMLStreamReader(is);
			
			while (xr.hasNext()) {
				xr.next();
				
				if (spectrumBuilder != null) {
					spectrumBuilder.accept(xr);
				}

				switch (xr.getEventType()) {
					case XMLStreamReader.START_ELEMENT:
						if(xr.getLocalName().equals("spectrum")) {
							spectrumBuilder = this.factory.create(this.xml.toString(), xr);
						} else if( xr.getLocalName().equals("referenceableParamGroupRef")) {
							LOGGER.log(Level.WARN, "Random access to spectra will not parse referenceable params");
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if(xr.getLocalName().equals("spectrum")) {
							return spectrumBuilder.build();
						}
						break;
				}
			}
		} catch (XMLStreamException | FactoryConfigurationError e) {
			LOGGER.log(Level.ERROR, e.toString());
		} 

		return null;
	}
	
	/**
	 * Grabs a spectrum using random access to a file
	 *  
	 * @param index of spectrum in the indexList
	 * @return new instance of T using factory passed into the constructor
	 */
	public T getSpectrumByIndex(int index) {
		try {
			this.seekable = this.seekable.position(this.spectrumOffsets.getOffsets().get(index));
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.toString());
			return null;
		} catch (NullPointerException e1 ) {
			LOGGER.log(Level.ERROR, "No index was set for seekable file. " + e1.toString());
			return null;
		}
		return this.getNextSpectrumFromSeekable();
	}
	
	/**
	 * Grabs a spectrum using the complete reference id string that must match between the
	 *  spectrum tag's attribute, and the indexList offset's attribute.
	 *  
	 * @param refId complete reference id string
	 * @return new instance of T using factory passed into the constructor
	 */
	public T getSpectrumById(String refId) {	
		try {
			this.seekable = this.seekable.position(this.spectrumOffsets.getIdToOffsets().get(refId));
		} catch (IOException e) {
			LOGGER.log(Level.ERROR, e.toString());
			return null;
		} catch (NullPointerException e1 ) {
			LOGGER.log(Level.ERROR, "ID was not found or no index was set for seekable file. " + e1.toString());
			return null;
		}
		return this.getNextSpectrumFromSeekable();
	}
	
	/**
	 * Gets a list of spectra within a scanTime range using random access. 
	 *  
	 * 
	 * @param low scan time inclusive
	 * @param high scan time inclusive
	 * @throws IllegalStateException if no scan time index was parsed by the constructor
	 * @return new instances of T using factory passed into the constructor
	 */
	public List<T> getSpectrumByScanTimeRange(double low, double high) throws IllegalStateException {
		if(this.spectrumOffsets.getScanTimesToOffsets() == null){
			throw new IllegalStateException("No scan time index was set. Cannot random access by scan time range");			
		}
		
		List<T> spectra = new ArrayList<T>();
		for(Long offset : this.spectrumOffsets.getScanTimesToOffsets().subMap(low, true, high, true).values()){
			try {
				this.seekable = this.seekable.position(offset);
			} catch (IOException e) {
				LOGGER.log(Level.ERROR, e.toString());
				return null;
			} catch (NullPointerException e1 ) {
				LOGGER.log(Level.ERROR, "Scan times was not set or no index was set for seekable file. " + e1.toString());
				return null;
			}
			spectra.add(this.getNextSpectrumFromSeekable());	
		}
		return spectra;
	}
	
	/** 
	 * maximum number of bytes from file end containing indexList.  
	 **/
	private final static int MAX_MEGABYTE_FROM_END = 32;
	
	/** path to mzml file **/
	protected Path xml;
	
	/** file handle used for random access **/
	protected SeekableByteChannel seekable;
	
	/** data structures for indexing **/
	protected SpectrumIndexer spectrumOffsets;
	
	/** accumulates XML reader events by id **/
	protected Map<String, ReferenceableParamGroup> refParams;
	
	/** factory for constructing objects **/
	private FromXMLStreamBuilderFactory<T> factory;
	
	final static Logger LOGGER = LogManager.getLogger(MzMLStAXParser.class);
}

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
package com.digitalproteomics.oss.parsers.mzml.builders;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

/**
 * A class to map spectrum references to byte offsets within an mzml file. The class parses xml events of 
 * a index xml element with spectrum attribute located at the end of an mzml file. 
 * The offsets can be called by spectrum index, spectrum reference id, or scan time (see {@code setScanTimeToOffsets}). 
 */
public class SpectrumIndexer implements FromXMLStreamBuilder<SpectrumIndexer> {
	protected Map<String, Long> idToOffsets;
	protected List<Long> offsets;
	protected NavigableMap<Double,Long> scanTimeToOffsets;
	protected String name;
	
	private String currId;
	private long currOffset;
	
	final static Logger LOGGER = LogManager.getLogger(SpectrumIndexer.class);
	
	public SpectrumIndexer(XMLStreamReader xr) {
		this.name = xr.getAttributeValue(null, "name");
		this.idToOffsets = new HashMap<String, Long>();
		this.offsets = new ArrayList<Long>();
	}
	
	@Override
	public void accept(XMLStreamReader xr) {
		if(xr.getEventType() == XMLStreamConstants.START_ELEMENT
				&& xr.getLocalName().equals("offset")) {
			
			this.currId = xr.getAttributeValue(null, "idRef");
			
		} else if(xr.getEventType() == XMLStreamConstants.CHARACTERS
				&& this.currId != null){
			
			this.currOffset = Long.valueOf(xr.getText());
			
		} else if(xr.getEventType() == XMLStreamConstants.END_ELEMENT
				&& xr.getLocalName().equals("offset")){
			
			this.idToOffsets.put(this.currId, this.currOffset);
			this.offsets.add(this.currOffset);
			this.currId = null;
			this.currOffset = -1;
			
		}
	}
	
	/** 
	 * Holds Id and ScanTime for finding offsets 
	 **/
	private class RefIdAndScanTime {
		final String refId;
		double scanTime;
		
		public RefIdAndScanTime(String refId){
			this.refId = refId;
			this.scanTime = 0.0;
		}
		
		public void setRT(double rt){
			this.scanTime = rt;
		}
		
		public Long getOffset(){
			return SpectrumIndexer.this.idToOffsets.get(this.refId);
		}
		
		public double getScanTime(){
			return this.scanTime;
		}
	}
	
	/**
	 * Collates ScanTime and Id information by consuming spectrum xml elements. 
	 */
	private class XMLSpectrumScanTimeBuilder implements FromXMLStreamBuilder<RefIdAndScanTime> {

		protected RefIdAndScanTime header;

		public XMLSpectrumScanTimeBuilder(String fileName, XMLStreamReader xr) {
			this.header = new RefIdAndScanTime(xr.getAttributeValue(null, "id"));
		}

		@Override
		public void accept(XMLStreamReader xr) {
			if(xr.getEventType() == XMLStreamConstants.START_ELEMENT 
				&& xr.getLocalName().equals("cvParam") 
				&&  xr.getAttributeValue(null, "accession").equals("MS:1000016")){
				double norm = xr.getAttributeValue(null, "unitAccession")
						.equals("UO:0000031") 
					? 60.0 
					: 1.0;
				this.header.setRT(norm * Double.valueOf(xr.getAttributeValue(null, "value")));
			}
		}

		@Override
		public RefIdAndScanTime build(){
			return this.header;
		}
	}
	
	/** 
	 * Sets offsets for start scan times by iterating over all spectrum xml elements.
	 * 
	 * Only scan times that match an index will be recorded!
	 * 
	 * @throws IOException 
	 **/
	public void setScanTimeToOffsets(Path xml) throws IOException{
		this.scanTimeToOffsets = new TreeMap<Double,Long>();
		
		MzMLStAXParser<RefIdAndScanTime> parser = new MzMLStAXParser<RefIdAndScanTime>(xml, 
				XMLSpectrumScanTimeBuilder::new, 
				false, 
				false);
		
		for(RefIdAndScanTime r : parser){
			if(r.getOffset() != null){
				this.scanTimeToOffsets.put(r.getScanTime(), r.getOffset());	
			} else {
				LOGGER.log(Level.WARN, "IndexList is not complete. Scan start time " 
						+ r.getScanTime() 
						+ " was not found, but no matching reference id in index.");
			}
		}
		
		parser.close();
	}
	
	@Override
	public SpectrumIndexer build() {
		return this;
	}

	public Map<String, Long> getIdToOffsets() {
		return this.idToOffsets;
	}

	public List<Long> getOffsets() {
		return this.offsets;
	}
	
	public NavigableMap<Double, Long> getScanTimesToOffsets(){
		return this.scanTimeToOffsets;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int size(){
		return this.offsets.size();
	}
}
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

import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser.FromXMLStreamBuilder;

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
	
	public SpectrumIndexer(XMLStreamReader xr) {
		this.name = xr.getAttributeValue(null, "name");
		this.idToOffsets = new HashMap<String, Long>();
		this.offsets = new ArrayList<Long>();
	}
	
	@Override
	public void accept(XMLStreamReader xr) {
		switch(xr.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				if(xr.getLocalName().equals("offset")) {
					this.currId = xr.getAttributeValue(null, "idRef");
				}
				break;
			case XMLStreamConstants.CHARACTERS:
				if(this.currId != null) {
					this.currOffset = Long.valueOf(xr.getText());
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				if(xr.getLocalName().equals("offset")) {
					this.idToOffsets.put(this.currId, this.currOffset);
					this.offsets.add(this.currOffset);
					this.currId = null;
					this.currOffset = -1;
				}
				break;
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
	private class XMLSpectrumScanTimeBuilder implements MzMLStAXParser.FromXMLStreamBuilder<RefIdAndScanTime> {

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
	 * @throws IOException 
	 **/
	public void setScanTimeToOffsets(Path xml) throws IOException{
		this.scanTimeToOffsets = new TreeMap<Double,Long>();
		
		MzMLStAXParser<RefIdAndScanTime> parser = new MzMLStAXParser<RefIdAndScanTime>(xml, 
				XMLSpectrumScanTimeBuilder::new, 
				false, 
				false);
		
		for(RefIdAndScanTime r : parser){
			this.scanTimeToOffsets.put(r.getScanTime(), r.getOffset());
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
package com.digitalproteomics.oss.parsers.mzml.builders;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.digitalproteomics.oss.parsers.mzml.BinaryDataArray;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;
import com.digitalproteomics.oss.parsers.mzml.model.Spectrum;
import com.digitalproteomics.oss.parsers.mzml.model.SpectrumHeader;
import com.digitalproteomics.oss.parsers.mzml.model.Spectrum.Peak;

/**
 * Collates binary array data for constructing a spectrum 
 */
public class XMLSpectrumBuilder implements MzMLStAXParser.FromXMLStreamBuilder<Spectrum>{
	final static Logger LOGGER = LogManager.getLogger(XMLSpectrumBuilder.class);
	
	/**
	 * binary data associated with a spectrum
	 */
	protected List<BinaryDataArray> data;
	/**
	 * a parser for building the spectrum header 
	 */
	protected XMLSpectrumHeaderBuilder headerBuilder;
	
	/** private members are used to retain nesting state **/
	private BinaryDataArray currData;
	private boolean inBinaryNesting;
	
	public XMLSpectrumBuilder(String fileName, XMLStreamReader xr) {
		this.headerBuilder = new XMLSpectrumHeaderBuilder(fileName, xr);
	}
	
	@Override
	public void accept(XMLStreamReader xr) {
		this.headerBuilder.accept(xr);
		
		switch(xr.getEventType()){
			case XMLStreamConstants.START_ELEMENT:
				switch (xr.getLocalName()) {
					case "binaryDataArrayList":
						this.data = new ArrayList<BinaryDataArray>(Integer.valueOf(xr.getAttributeValue(null, "count")));
						return;
					case "binaryDataArray":
						this.currData = new BinaryDataArray();
						this.currData.setEncodedLength(Integer.valueOf(xr.getAttributeValue(null, "encodedLength")));
						return;
					case "cvParam":
						if(this.currData != null){
							this.currData.setMemberByAccession(xr.getAttributeValue(null, "accession"));		
						}
						return;
					case "binary":
						if(this.currData != null){
							this.inBinaryNesting = true;
						}
						return;
				}
				return;
			case XMLStreamConstants.CHARACTERS:
				if(this.inBinaryNesting){
					this.currData.appendToEncodedData(xr.getText());
				}
				return;
			case XMLStreamConstants.END_ELEMENT:
				switch (xr.getLocalName()) {
					case "binaryDataArray":
						this.data.add(this.currData);
						this.currData = null;
						return;
					case "binary":
						this.inBinaryNesting = false;
						return;
				}
				return;
		}	
	}
	
	/** 
	 * Grabs binary data array for unit testing and debugging
	 * @return
	 */
	public List<BinaryDataArray> getData(){
		return this.data;
	}
	
	@Override
	public Spectrum build(){
		Number[] masses = null;
		Number[] intensities = null;
		
		for(BinaryDataArray arr : this.data){
			switch(arr.getDataType()){
				case INTENSITY:
					intensities = arr.getDataAsDecodedNumberArray();
					break;
				case MZ_VALUES:
					masses = arr.getDataAsDecodedNumberArray();
					break;
				default:
					break;
			}
		}
		
		SpectrumHeader h = this.headerBuilder.build();
		
		if(masses == null || intensities == null){
			LOGGER.log(Level.WARN, "m/z array and intensity was not present for spectrum: " + h.getId());
			return new Spectrum(h, new Number[0], new Number[0]);
		} else {
			List<Peak> peaks = new ArrayList<Peak>(masses.length);
			for(int i=0; i < masses.length; i++){
				peaks.add(new Peak(masses[i].doubleValue(), intensities[i].doubleValue()));
			}
			return new Spectrum(h, peaks);
		}
	}
}

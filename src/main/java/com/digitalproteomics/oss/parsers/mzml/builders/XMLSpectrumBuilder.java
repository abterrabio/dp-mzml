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

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.digitalproteomics.oss.parsers.mzml.BinaryDataArray;
import com.digitalproteomics.oss.parsers.mzml.model.Peak;
import com.digitalproteomics.oss.parsers.mzml.model.Spectrum;
import com.digitalproteomics.oss.parsers.mzml.model.SpectrumHeader;

/**
 * Collates binary array data for constructing a spectrum 
 */
public class XMLSpectrumBuilder implements FromXMLStreamBuilder<Spectrum>{
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
		
		if(xr.getEventType() == XMLStreamConstants.START_ELEMENT) {
			if(xr.getLocalName().equals("binaryDataArrayList")){

				this.data = new ArrayList<BinaryDataArray>(Integer.valueOf(xr.getAttributeValue(null, "count")));
				
			} else if(xr.getLocalName().equals("binaryDataArray")){
				
				this.currData = new BinaryDataArray();
				this.currData.setEncodedLength(Integer.valueOf(xr.getAttributeValue(null, "encodedLength")));
				
			} else if(xr.getLocalName().equals("cvParam") 
					&& this.currData != null) {
				
				this.currData.setMemberByAccession(xr.getAttributeValue(null, "accession"));
				
			} else if(xr.getLocalName().equals("binary")
					&& this.currData != null){
				
				this.inBinaryNesting = true;
				
			}
		} else if(xr.getEventType() == XMLStreamConstants.CHARACTERS 
				&& this.inBinaryNesting){
			
			this.currData.appendToEncodedData(xr.getText());
			
		} else if(xr.getEventType() == XMLStreamConstants.END_ELEMENT){
			if(xr.getLocalName().equals("binaryDataArray")){
				
				this.data.add(this.currData);
				this.currData = null;
				
			} else if(xr.getLocalName().equals("binary")) {
				
				this.inBinaryNesting = false;
				
			}
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

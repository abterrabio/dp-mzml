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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.digitalproteomics.oss.parsers.mzml.model.SpectrumHeader;

/**
 * Collates {@code SpectrumHeader} information by consuming XML elements. 
 * 
 * {@code Activation.ETD} is set as the highest priority activation
 */
public class XMLSpectrumHeaderBuilder implements FromXMLStreamBuilder<SpectrumHeader> {

	protected SpectrumHeader currHeader;

	/** 
	 * Constructor for a SpectrumHeader
	 * 
	 * @param fileName - origin of xml data
	 * @param xr - initial state used for building SpectrumHeader
	 */
	public XMLSpectrumHeaderBuilder(String fileName, XMLStreamReader xr) {
		this.currHeader = new SpectrumHeader(fileName,  
				Integer.valueOf(xr.getAttributeValue(null, "index")),
				xr.getAttributeValue(null, "id"));
	}

	@Override
	public void accept(XMLStreamReader xr) {
		if((xr.getEventType() == XMLStreamConstants.START_ELEMENT) 
				&& xr.getLocalName().equals("cvParam")) {
			
			if (xr.getAttributeValue(null, "accession").equals("MS:1000511")) {
				this.currHeader.setMsLevel(Integer.valueOf(xr.getAttributeValue(null, "value")));
			} else if(xr.getAttributeValue(null, "accession").equals("MS:1000016")) {
				// normalize to seconds if unit is a minute
				double norm = xr.getAttributeValue(null, "unitAccession")
						.equals("UO:0000031") 
					? 60.0 
					: 1.0;
				this.currHeader.setScanStartTime(norm * Double.valueOf(xr.getAttributeValue(null, "value")));
			} else if(xr.getAttributeValue(null, "accession").equals("MS:1000744")) {
				this.currHeader.setSelectedIonMz(Double.valueOf(xr.getAttributeValue(null, "value")));
			} else if(xr.getAttributeValue(null, "accession").equals("MS:1000041")) {
				this.currHeader.setSelectedIonCharge(Integer.valueOf(xr.getAttributeValue(null, "value")));
			} else if(xr.getAttributeValue(null, "accession").equals("MS:1000042")) {
				this.currHeader.setSelectedIonIntensity(Double.valueOf(xr.getAttributeValue(null, "value")));
			}
		}
	}

	@Override
	public SpectrumHeader build(){
		return this.currHeader;
	}
}

package com.digitalproteomics.core.spectra.io;

import java.util.LinkedList;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import com.digitalproteomics.core.Activation;
import com.digitalproteomics.core.SpectrumHeader;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

/**
 * Collates {@code SpectrumHeader} information by consuming XML elements. 
 * 
 * {@code Activation.ETD} is set as the highest priority activation
 */
public class XMLSpectrumHeaderBuilder implements MzMLStAXParser.FromXMLStreamBuilder<SpectrumHeader> {

	protected SpectrumHeader header;

	private LinkedList<Activation> activations;
	
	/** 
	 * Constructor for a SpectrumHeader
	 * 
	 * @param fileName - origin of xml data
	 * @param xr - initial state used for building SpectrumHeader
	 */
	public XMLSpectrumHeaderBuilder(String fileName, XMLStreamReader xr) {
		this.header = new SpectrumHeader(fileName, 
				Integer.valueOf(xr.getAttributeValue(null, "index")));
		this.header.setId(xr.getAttributeValue(null, "id"));
		this.header.setIntensity(-1);
		this.header.setCharge(0);
		this.header.setMZ(-1);
	}

	@Override
	public void accept(XMLStreamReader xr) {
		switch(xr.getEventType()) {
			case XMLStreamConstants.START_ELEMENT: 
				if(xr.getLocalName().equals("cvParam")){
					switch (xr.getAttributeValue(null, "accession")){
						case "MS:1000511":
							this.header.setMSLevel(Integer.valueOf(xr.getAttributeValue(null, "value")));
							break;
						case "MS:1000016":
							// normalize to seconds if unit is a minute
							double norm = xr.getAttributeValue(null, "unitAccession")
									.equals("UO:0000031") 
								? 60.0 
								: 1.0;
							this.header.setRT(norm * Double.valueOf(xr.getAttributeValue(null, "value")));
							break;
						case "MS:1000744":
							this.header.setMZ(Double.valueOf(xr.getAttributeValue(null, "value")));
							break;
						case "MS:1000041":
							this.header.setCharge(Integer.valueOf(xr.getAttributeValue(null, "value")));
							break;
						case "MS:1000042":
							this.header.setIntensity(Double.valueOf(xr.getAttributeValue(null, "value")));
							break;
						case "MS:1000133":
							this.activations.addLast(Activation.CID);
							break;
						case "MS:1000598":
							this.activations.addFirst(Activation.ETD);
							break;
						case "MS:1000422":
							this.activations.addFirst(Activation.HCD);
							break;
							
					}
				} else if(xr.getLocalName().equals("activation")) {
					this.activations = new LinkedList<Activation>();
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				if(xr.getLocalName().equals("activation") && this.activations.size() > 0){
					this.header.setActivation(this.activations.get(0));
				}
		}
	}

	@Override
	public SpectrumHeader build(){
		return this.header;
	}
}

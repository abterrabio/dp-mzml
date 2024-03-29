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
package com.digitalproteomics.oss.parsers.mzml.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Spectrum container with a header, a mz array, and an intensity array. 
 * The container members must be set on construction. 
 *
 */
public class Spectrum {
	
	/** container for spectrum header **/
	private final SpectrumHeader header;
	
	private List<Peak> peaks;
	
	/**
	 * Create a Spectrum instance containing a header and peaks
	 * 
	 * @param header - meta data for the spectrum
	 * @param mz - m/z array, accession="MS:1000514"
	 * @param intensities - intensity array, accession="MS:1000515"
	 * @throws IllegalArgumentException thrown when the two arrays are not of the same length.
	 */
	public Spectrum(SpectrumHeader header, 
					  Number[] mz, 
					  Number[] intensities) {
		this.header = header;
		
		if(mz.length != intensities.length){
			throw new IllegalArgumentException("Cannot create spectrum from unequal lengths of m/z and intensities");
		}
		
		this.peaks = IntStream.range(0, mz.length)
				.mapToObj(i -> new Peak(mz[i].doubleValue(), intensities[i].doubleValue()))
				.collect(Collectors.toList());
	}
	
	public Spectrum(SpectrumHeader header, List<Peak> peaks) {
		this.header = header;
		this.peaks = peaks;
	}

	public List<Peak> getPeaks() {
		return this.peaks;
	}
	
	public SpectrumHeader getHeader() {
		return header;
	}

	/** 
	 * Gets a m/z array, accession="MS:1000514"
	 * @return list of m/z 
	 */
	public List<Double> getMz() {
		return this.peaks.stream().map(p -> p.getMz()).collect(Collectors.toList());
	}

	/**
	 * Gets intensities, accession="MS:1000515"
	 * @return list of intensity
	 */
	public List<Double> getIntensities() {
		return this.peaks.stream().map(p -> p.getI()).collect(Collectors.toList());
	}
}

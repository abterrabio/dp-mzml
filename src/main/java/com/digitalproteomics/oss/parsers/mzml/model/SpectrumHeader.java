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

/**
 * Container for header spectrum data. Index starts with 0.
 */
public class SpectrumHeader{
	
	/** spectrum file */
	private String fileName;	
	/** index of spectrum in a file **/
	private int index;
	/** reference id for each spectrum **/
	private String id;
	/**
	 * selected ion m/z, accession="MS:1000744"
	 */
	private double selectedIonMz;
	/**
	 * selected ion charge, accession="MS:1000041"
	 */
	private int selectedIonCharge;
	/**
	 * selected ion intensity, accession-"MS:1000042"
	 */
	private double selectedIonIntensity;
	/**
	 * start of scan time (in seconds), accession="MS:1000016" 
	 */
	private double scanStartTime;
	/**
	 * level of ms, accession="MS:1000511" 
	 */
	private int msLevel;
	
	public SpectrumHeader(String fileName, int index, String id) {
		this(fileName, index, id, 0.0, 0, 0.0, 0.0, 0);
	}

	public SpectrumHeader(String fileName, 
			int index,
			String id, 
			double selectedIonMz,
			int selectedIonCharge,
			double selectedIonIntensity,
			double scanStartTime,
			int msLevel) {
		this.fileName = fileName;
		this.index = index;
		this.id = id;
		this.selectedIonMz = selectedIonMz;
		this.selectedIonCharge = selectedIonCharge;
		this.selectedIonIntensity = selectedIonIntensity;
		this.scanStartTime = scanStartTime;
		this.msLevel = msLevel;
	}

	public String getFileName() {
		return fileName;
	}

	public int getIndex() {
		return index;
	}

	public String getId() {
		return id;
	}

	public double getSelectedIonMz() {
		return selectedIonMz;
	}

	public void setSelectedIonMz(double selectedIonMz) {
		this.selectedIonMz = selectedIonMz;
	}

	public int getSelectedIonCharge() {
		return selectedIonCharge;
	}

	public void setSelectedIonCharge(int selectedIonCharge) {
		this.selectedIonCharge = selectedIonCharge;
	}

	public double getSelectedIonIntensity() {
		return selectedIonIntensity;
	}

	public void setSelectedIonIntensity(double selectedIonIntensity) {
		this.selectedIonIntensity = selectedIonIntensity;
	}

	public double getScanStartTime() {
		return scanStartTime;
	}

	public void setScanStartTime(double scanStartTime) {
		this.scanStartTime = scanStartTime;
	}

	public int getMsLevel() {
		return msLevel;
	}

	public void setMsLevel(int msLevel) {
		this.msLevel = msLevel;
	}
}


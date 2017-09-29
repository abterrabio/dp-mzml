package com.digitalproteomics.oss.parsers.mzml.model;

/** 
 * Helper class that wraps a mz and intensity as a peak
 **/
public class Peak {
	double mz;
	double i;
	
	public Peak(double mz, double i){
		this.mz = mz;
		this.i = i;
	}
	
	public double getMz(){
		return this.mz;
	}
	
	public double getI(){
		return this.i;
	}
}
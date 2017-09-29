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
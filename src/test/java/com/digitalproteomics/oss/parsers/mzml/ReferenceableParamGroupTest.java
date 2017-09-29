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
package com.digitalproteomics.oss.parsers.mzml;

import junit.framework.TestCase;

import java.net.URISyntaxException;

/**
import uk.ac.ebi.jmzml.model.mzml.Chromatogram;
import uk.ac.ebi.jmzml.model.mzml.IndexList;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
**/

import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

import com.digitalproteomics.oss.parsers.mzml.model.SpectrumHeader;
import com.digitalproteomics.oss.parsers.mzml.builders.XMLSpectrumHeaderBuilder;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

public class ReferenceableParamGroupTest extends TestCase {

	@Test
    public void testMsLevels(){
        URL url = this.getClass().getClassLoader().getResource("lipid.mzML");
        assertNotNull(url);

        MzMLStAXParser<SpectrumHeader> p  = null;
		try {
			p = new MzMLStAXParser<SpectrumHeader>(Paths.get(url.toURI()), 
					XMLSpectrumHeaderBuilder::new,
					false, 
					false);
		} catch (URISyntaxException e) {
			fail(e.toString());
		}
       
       for(SpectrumHeader h : p){
    	   assertEquals(2, h.getMsLevel());
       }
    }

}

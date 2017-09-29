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

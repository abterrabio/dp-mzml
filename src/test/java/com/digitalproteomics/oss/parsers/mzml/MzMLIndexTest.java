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

import com.digitalproteomics.oss.parsers.mzml.model.SpectrumHeader;
import com.digitalproteomics.oss.parsers.mzml.builders.XMLSpectrumHeaderBuilder;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

public class MzMLIndexTest extends TestCase {

    public void testIndexedmzML(){
        URL url = this.getClass().getClassLoader().getResource("tiny.pwiz.err.idx.mzML");
        assertNotNull(url);

        MzMLStAXParser<SpectrumHeader> p  = null;
		try {
			p = new MzMLStAXParser<SpectrumHeader>(Paths.get(url.toURI()), 
					XMLSpectrumHeaderBuilder::new);
		} catch (URISyntaxException e) {
			fail(e.toString());
		}
       
        //IndexList index = um.getMzMLIndex();
        //assertNotNull(index);

        // check that we have as many index entries as we expect
        // we expect 2 entries: 'chromatogram' and 'spectrum'
        //assertEquals(2, index.getCount().intValue());
        //assertEquals(index.getCount().intValue(), index.getIndex().size());

        SpectrumHeader spectrum = p.getSpectrumById("scan=19");
        assertNotNull(spectrum);

        spectrum = p.getSpectrumById("scan=21");
        assertNotNull(spectrum);

        spectrum = p.getSpectrumById("scan=22");
        assertNotNull(spectrum);

        //spectrum = um.getSpectrumBySpotId("A1,42x42,4242x4242");
        //assertNotNull(spectrum);


        //Chromatogram chr = um.getChromatogramByRefId("tic");
        //assertNotNull(chr);

        //chr = um.getChromatogramByRefId("sic");
        //assertNotNull(chr);

        ///// ///// ///// ///// ///// ///// ///// ///// ///// /////
        // negative testing
        // (tests that are not supposed to return useful results)

        // here we check the bahaviour if we search for a id that is not in the index
        spectrum = p.getSpectrumById("nonexist");
        assertNull(spectrum);

        // here we try to retrieve a spectrum by scanTime, but no according entry exists in the index
        //spectrum = um.getSpectrumByScanTime(12345);
        //assertNull(spectrum);

        // here we introduced a offset mismatch in the mzML index of the test file to
        // test the behaviour of the unmarshaller in case of a index offset mismatch
        spectrum = p.getSpectrumById("scan=20");
        assertNull(spectrum);
    }

}

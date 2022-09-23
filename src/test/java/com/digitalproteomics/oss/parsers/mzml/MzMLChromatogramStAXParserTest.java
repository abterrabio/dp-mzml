package com.digitalproteomics.oss.parsers.mzml;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import com.digitalproteomics.oss.parsers.mzml.model.Chromatogram;
import junit.framework.TestCase;

public class MzMLChromatogramStAXParserTest extends TestCase {

    public void testTICFromMzML() {
        URL url = this.getClass().getClassLoader().getResource("small.pwiz.1.1.mzML");
        assertNotNull(url);

        MzMLChromatogramStAXParser p = null;
        try {
            p = new MzMLChromatogramStAXParser(Paths.get(url.toURI()));
        } catch (URISyntaxException e) {
            fail(e.toString());
        }

        int c = 0;
        for (Chromatogram chr : p) {
            assertEquals("total ion current chromatogram", chr.getId());

            c += 1;
        }
        assertEquals(1, c);
    }
}
package com.digitalproteomics.core.spectra.io;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.digitalproteomics.core.SpectrumHeader;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

public class MzMLSpectrumHeaderParser extends MzMLStAXParser<SpectrumHeader> implements SpecFileHeaderParser {

	public MzMLSpectrumHeaderParser(Path xml) {
		super(xml, XMLSpectrumHeaderBuilder::new, true, false);
	}

	@Override
	public int getSpectraCount() {
		return this.spectrumOffsets.size();
	}

	@Override
	public String getOriginalFileName() {
		return this.xml.toString();
	}

	@Override
	public List<SpectrumHeader> getSpectrumHeaders() {
		return StreamSupport.stream(this.spliterator(), false).collect(Collectors.toList());
	}

}

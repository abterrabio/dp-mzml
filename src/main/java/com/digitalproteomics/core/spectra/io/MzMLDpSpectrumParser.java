package com.digitalproteomics.core.spectra.io;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.digitalproteomics.core.dpSpectrum;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

public class MzMLDpSpectrumParser extends MzMLStAXParser<dpSpectrum> implements SequentialMSParser, ChromatogramParser {

	public MzMLDpSpectrumParser(Path xml) {
		super(xml, XMLDpSpectrumBuilder::new, true, true);
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
	public double getXIC(double minRT, double maxRT, double precursorMZ, double daMzTol) {
		double runningTotal = 0;
		for(dpSpectrum spec : this.getSpectrumByScanTimeRange(minRT, maxRT)){
			if(spec.getMSLevel() == 1){
				runningTotal += spec.getTotalIntensity(precursorMZ, daMzTol);
			}
		}
		return runningTotal;
	}

	@Override
	public double getTIC(double rtTol, double daMzTol) {
		double tic = 0.0;
		for(dpSpectrum spec : this){
			if(spec.getMSLevel() != 2){
				continue;
			}
			double rt = spec.getRT();
			tic += this.getXIC(Math.max(0, rt - rtTol), rt + rt, spec.getMZ(), daMzTol);
		}
		return tic;
	}

	@Override
	@Deprecated
	public List<Integer> getSpecIndexesInRTRange(double minRT, double maxRT) {
		return this.getSpectrumByScanTimeRange(minRT, maxRT).stream()
				.map(sp -> sp.getSpecIndex())
				.collect(Collectors.toList());
	}

	@Override
	@Deprecated
	public int getMsLevelFromIndex(int index) {
		return this.getSpectrumByIndex(index).getMSLevel();
	}

	@Override
	@Deprecated
	public double getRTFromIndex(int index) {
		return this.getSpectrumByIndex(index).getRT();
	}
}

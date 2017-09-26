package com.digitalproteomics.core.spectra.io.cli;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.digitalproteomics.core.SpectrumHeader;
import com.digitalproteomics.core.dpSpectrum;
import com.digitalproteomics.core.spectra.io.MzMLDpSpectrumParser;
import com.digitalproteomics.core.spectra.io.MZMLParser;
import com.digitalproteomics.core.spectra.io.XMLSpectrumHeaderBuilder;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

public class Benchmark {
	
	public static class Row {
		final double mz;
		final double intensity;
		final int msLevel;
		final double scanTime;
		final int peakCount;
		
		public Row(dpSpectrum spec) {
			this.mz = spec.getMSLevel() == 1 ? 0 : spec.getMZ();
			this.intensity = spec.getIntensity();
			this.msLevel = spec.getMSLevel();
			this.scanTime = spec.getRT();
			this.peakCount = spec.getPeaks().size();
		}
		
		public Row(SpectrumHeader spec) {
			this.mz = spec.getMSLevel() == 1 ? 0 : spec.getMZ();
			this.intensity = spec.getIntensity();
			this.msLevel = spec.getMSLevel();
			this.scanTime = spec.getRT();
			this.peakCount = 0;
		}
		
		public Row(uk.ac.ebi.jmzml.model.mzml.Spectrum spec) {
			double mz = 0.0;
			double intensity = 0.0;
			int msLevel = 1;
			double scanTime = 0.0;
			
			for(CVParam param : spec.getCvParam()) {
				switch(param.getAccession()){
					case "MS:1000511":
						msLevel = Integer.parseInt(param.getValue());
						break;
					case "MS:1000042":
						intensity = Double.parseDouble(param.getValue());
						break;
					case "MS:1000744":
						mz = Double.parseDouble(param.getValue());
						break;
					case "MS:1000016":
						scanTime = Double.parseDouble(param.getValue());
						break;
				}
			}
			this.mz = mz;
			this.intensity = intensity;
			this.msLevel = msLevel;
			this.scanTime = scanTime;
			this.peakCount = spec.getBinaryDataArrayList()
					.getBinaryDataArray()
					.get(0)
					.getBinaryDataAsNumberArray()
					.length;
		}
		
		public String toString() {
			return String.format("%1$2f\t%2$5f\t%3$d\t%4$5f\t%5$d", this.mz, this.intensity, this.msLevel, this.scanTime, this.peakCount);
		}
	}
	
	@FunctionalInterface
	public interface RandomAccess {
		Row getByIndex(int index);
	}
	
	public static void main( String[] args ) throws MalformedURLException {
    	CommandLineParser clParser = new DefaultParser();		
		Options opts = new Options();
		opts.addOption("m", "mzml", true, "mzml file");
		opts.addOption("p", "parser", true, "type of parser to use choices are j, dp, stax. (default is dp)");
		
		Option indexOpts = Option.builder("i")
				.hasArgs()
				.desc("index of spectra")
				.argName("index")
				.build();
		opts.addOption(indexOpts);
		
		CommandLine cmd = null;
		try {
			cmd = clParser.parse(opts, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		Path mzml = cmd.hasOption("m") ? Paths.get(cmd.getOptionValue("m")) : null;
		if(mzml == null ) {
			System.err.println("MZML file was not specified");
			System.exit(-1);
		}
			
    	List<Integer> indices = cmd.hasOption("i") 
    			? Stream.of(cmd.getOptionValues("i")).map(s -> Integer.parseInt(s)).collect(Collectors.toList())
    			: Collections.EMPTY_LIST;
		
    	Iterable<Row> p = null;
    	RandomAccess r = null;
    	
    	String parserType = cmd.hasOption("p") ? cmd.getOptionValue("p") : "dp";
    	
    	switch (parserType) {
    		case "j":
    			MzMLUnmarshaller um =  new MzMLUnmarshaller(mzml.toUri().toURL(), false);
    			p = () -> um.unmarshall()
    					.getRun()
    					.getSpectrumList()
    					.getSpectrum()
    					.stream()
    					.map(s -> new Row(s))
    					.iterator();
    			r = (i) -> {
					try {
						return new Row(um.getSpectrumById(um.getSpectrumIDFromSpectrumIndex(i)));
					} catch (MzMLUnmarshallerException e) {
						System.err.println(e.toString());
						return null;
					}
				};
    			break;
    		case "dp":
    			MZMLParser parser2 = new MZMLParser(mzml.toFile());
    			p = () -> StreamSupport.stream(parser2.spliterator(), false)
    					.map(s -> new Row(s))
    					.iterator();
    			r = (i) -> new Row(parser2.getSpectrumByIndex(i));
    			break;
    		case "stax":
    			MzMLDpSpectrumParser parser3 = new MzMLDpSpectrumParser(mzml);
    			p = () -> StreamSupport.stream(parser3.spliterator(), false)
    					.map(s -> new Row(s))
    					.iterator();
    			r = (i) -> new Row(parser3.getSpectrumByIndex(i));
    			break;
    		case "stax-h":
    			MzMLStAXParser<SpectrumHeader> parser4 = new MzMLStAXParser<SpectrumHeader>(mzml, XMLSpectrumHeaderBuilder::new);
    			p = () -> StreamSupport.stream(parser4.spliterator(), false)
    					.map(s -> new Row(s))
    					.iterator();
    			r = (i) -> new Row(parser4.getSpectrumByIndex(i));
    			break;
    			
    	}
    			
    	if(indices.size() == 0) {
    		for(Row row : p) {
    			System.out.println(row.toString());
    		}
    	} else {
    		for(int i : indices){
    			System.out.println(r.getByIndex(i));
    		}
    	}
    			
    }
}

package com.digitalproteomics.oss.parsers.mzml.cli;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;
import com.digitalproteomics.oss.parsers.mzml.builders.XMLSpectrumBuilder;
import com.digitalproteomics.oss.parsers.mzml.model.Spectrum;

/**
 * Command line interface to demo parsing a mzML file and creating instances of Spectrum 
 */
public class Example {
	
	/**
	 * See class {@code Example} 
	 * @param args
	 * @throws MalformedURLException
	 */
	public static void main( String[] args ) throws MalformedURLException, IOException {
    	CommandLineParser clParser = new DefaultParser();		
		Options opts = new Options();

		opts.addOption("h", "help", false, "display help");
		opts.addOption("m", "mzml", true, "mzml file");
		
		Option indexOpts = Option.builder("i")
				.hasArgs()
				.desc("indices of spectra to report")
				.argName("index")
				.build();
		opts.addOption(indexOpts);
		
		CommandLine cmd = null;
		HelpFormatter formatter = new HelpFormatter();
		try {
			cmd = clParser.parse(opts, args);
			if( cmd.hasOption("help") || (cmd.getOptions().length == 0)) {
				formatter.printHelp("help", opts);
				System.exit(-1);
			}
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
		
    	// Creates a parser producing Spectrum instances and uses XMLSpectrumBuilder via method referencing
		MzMLStAXParser<Spectrum> parser = new MzMLStAXParser<Spectrum>(mzml, XMLSpectrumBuilder::new);
    	
    	if(indices.size() == 0) {
    		// if no indices supplied, sequentially iterate over the file
    		int maxCount = 10;
    		for(Spectrum spectrum : parser) {
    			if(maxCount == 0)
    				return;
    			
    			System.out.println(spectrum.toString());
    			maxCount--;
    		}
    	} else {
    		// if indices are supplied, access spectra by jumping to the position in file
    		for(int i : indices){
    			System.out.println(parser.getSpectrumByIndex(i).toString());
    		}
    	}
    	
    	parser.close();
    }
}

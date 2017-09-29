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

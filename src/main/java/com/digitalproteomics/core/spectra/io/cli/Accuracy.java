package com.digitalproteomics.core.spectra.io.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
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
import com.digitalproteomics.core.spectra.io.MZMLParser;
import com.digitalproteomics.core.spectra.io.XMLDpSpectrumBuilder;
import com.digitalproteomics.core.spectra.io.XMLSpectrumHeaderBuilder;
import com.digitalproteomics.oss.parsers.mzml.MzMLStAXParser;

public class Accuracy {
	
	@FunctionalInterface
	interface GrabByIndex<T> {
		T getByIndex(int index);
	}
	
	public static <T> boolean compareIterators(Supplier<Compare<T>> fac, Stream<T> pA, Stream<T> pB){
		boolean isEquals = true;
		
		Iterator<T> iterA = pA.iterator();
		Iterator<T> iterB = pB.iterator();
		Compare<T> comp;
		while(iterA.hasNext() && iterB.hasNext()){
			comp = fac.get();
			comp.setA(iterA.next());
			comp.setB(iterB.next());
			
			isEquals &= comp.compareAndReport();
		}
		
		if(iterA.hasNext() != iterB.hasNext()){
			System.out.println("Iterator A  or iterator B has remaining elements " + iterA.hasNext() + " " + iterB.hasNext());
		}
		return isEquals;
	}
	
	public static <T> boolean compareIndices(Supplier<Compare<T>> fac, GrabByIndex<T> pA, GrabByIndex<T> pB, List<Integer> indices){
		boolean isEquals = true;
		
		Compare<T> comp;
		for(Integer index : indices){
			comp = fac.get();
			comp.setA(pA.getByIndex(index));
			comp.setB(pB.getByIndex(index));
			isEquals &= comp.compareAndReport();
		}
		return isEquals;
	}
	
	static class SpectrumHeaderCompare extends Compare<SpectrumHeader>{
		
		public SpectrumHeaderCompare() {
			super();
		}

		public boolean compareAndReport() {
			boolean isEquals = true;
			super.compareAndReport();
			isEquals &= this.compareAndReport(s -> s.getActivation(), "activation");
			//isEquals &= this.compareAndReport(s -> s.getCharge(), "charge");
			isEquals &= this.compareAndReport(s -> s.getFileName(), "filename");
			isEquals &= this.compareAndReport(s -> s.getId(), "id");
			isEquals &= this.compareAndReport(s -> s.getIntensity(), "intensity");
			isEquals &= this.compareAndReport(s -> s.getMSLevel(), "mslevel");
			isEquals &= this.compareAndReport(s -> s.getMZ(), "mz");
			isEquals &= this.compareAndReport(s -> s.getRT(), "rt");
			isEquals &= this.compareAndReport(s -> s.getSpecIndex(), "index");
			return isEquals;
		}
	}
	
	static class SpectrumCompare extends Compare<dpSpectrum> {
		public SpectrumCompare(){
			super();
		}
		@Override
		public boolean compareAndReport() {
			boolean isEquals = true;
			super.compareAndReport();
			isEquals &= this.compareAndReport(s -> s.getPeaks().size(), "peakCount");
			
			if(isEquals && this.a.getPeaks().size() > 1){
				final int n = this.a.getPeaks().size();
				isEquals &= this.compareAndReport(s -> s.getPeaks().get(0).getIntensity(), "0peakIntensity");
				isEquals &= this.compareAndReport(s -> s.getPeaks().get(0).getMZ(), "0peakMZ");
				
				
				isEquals &= this.compareAndReport(s -> s.getPeaks().get(n - 1).getIntensity(), "-1peakIntensity");
				isEquals &= this.compareAndReport(s -> s.getPeaks().get(n - 1).getMZ(), "-1peakMZ");
				
				isEquals &= this.compareAndReport(s -> s.getPeaks().get(n / 2).getIntensity(), "0.5peakIntensity");
				isEquals &= this.compareAndReport(s -> s.getPeaks().get(n / 2).getMZ(), "0.5peakMZ");
			}
				
			return isEquals;
		}
	}
	
	static abstract class Compare<S> {
		S a; 
		S b;
		public Compare(){
			this.a = null;
			this.b = null;
		}
		
		public void setA(S a) {
			this.a = a;
		}
		
		public void setB(S b) {
			this.b = b;
		}
		
		public <T> boolean compareAndReport(Function<S,T> func, String att){
			T va = func.apply(this.a);
			T vb = func.apply(this.b);
			
			if(va == null && vb == null){
				return true;
			} else if(va == null || vb == null || !va.equals(vb)){
				System.out.println("A:" 
						+ this.a.toString() 
						+ " B:" 
						+ this.b.toString()
						+ " "
						+ att
						+ " vA:" 
						+ va 
						+ " vB:" 
						+ vb);
				return false;
			}
			return true;	
		}	
		
		public boolean compareAndReport() {
			System.out.println("----" + this.a.toString() + "-" + this.b.toString() + "----");
			return true;
		};
	}
	
	
	public static void main( String[] args ) throws Exception {
    	CommandLineParser clParser = new DefaultParser();		
		Options opts = new Options();
		opts.addOption("m", "mzml", true, "mzml file");
		opts.addOption("p", "peaks", false, "compare peaks");
		
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
		
		final MZMLParser pA = new MZMLParser(mzml.toFile());
		final MzMLStAXParser<SpectrumHeader> pB = new MzMLStAXParser<SpectrumHeader>(mzml, XMLSpectrumHeaderBuilder::new);
		final MzMLStAXParser<dpSpectrum> pBPeaks = new MzMLStAXParser<dpSpectrum>(mzml, XMLDpSpectrumBuilder::new);
		
		if(indices.size() == 0){
			if(cmd.hasOption("p")){		
				compareIterators(SpectrumCompare::new,
						StreamSupport.stream(pA.spliterator(), false), 
						StreamSupport.stream(pBPeaks.spliterator(), false));
			} else {
				compareIterators(SpectrumHeaderCompare::new,
						StreamSupport.stream(pA.spliterator(), false).map(s -> s.getHeader()), 
						StreamSupport.stream(pB.spliterator(), false));
			}
		} else {
			if( cmd.hasOption("p")){
				compareIndices(SpectrumCompare::new,
						i -> pA.getSpectrumByIndex(i), 
						i -> pBPeaks.getSpectrumByIndex(i),
						indices);
			} else {
				compareIndices(SpectrumHeaderCompare::new,
						i -> pA.getSpectrumByIndex(i).getHeader(), 
						i -> pB.getSpectrumByIndex(i),
						indices);
			}
			
		}
		pA.close();
		pB.close();
    	
    }
}

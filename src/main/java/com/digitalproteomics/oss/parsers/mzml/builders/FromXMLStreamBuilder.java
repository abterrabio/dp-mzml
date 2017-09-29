package com.digitalproteomics.oss.parsers.mzml.builders;

import java.util.function.Consumer;

import javax.xml.stream.XMLStreamReader;

/**
 * Accepts states of an xml stream, and builds instances from nested xml tags
 * 
 * @param <S> the type of object built
 */
public interface FromXMLStreamBuilder<S> extends Consumer<XMLStreamReader> {
	/** Constructs an instance of type T after consuming xml states **/
	S build();
}
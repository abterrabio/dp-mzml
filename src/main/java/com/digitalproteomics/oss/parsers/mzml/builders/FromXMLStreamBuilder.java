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

	/**
	 * Parses spectrum tags.
	 */
	default boolean buildsFromSpectrumTags() {
		return true;
	}

	default boolean buildsFromChromatogramTags() {
		return false;
	}
}
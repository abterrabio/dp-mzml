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
package com.digitalproteomics.oss.parsers.mzml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

//import uk.ac.ebi.jmzml.model.mzml.utilities.MSNumpress;
import ms.numpress.MSNumpress;


/**
 * The structure into which encoded binary data goes. Byte ordering is always little endian 
 * (Intel style). 
 * Computers using a different endian style must convert to/from little endian when writing/reading mzML
 */

public class BinaryDataArray {

    public BinaryDataArray(){
    	this.compression = null;
    	this.precision = null;
    	this.dataType = DataType.UNKNOWN;
    	this.encodedData = new StringBuilder();
    }
    
    /** appends to encoded data **/
    public void appendToEncodedData(String s){
    	this.encodedData.append(s);
    }

    /**
     * Gets the value of the encodedLength property.
     */
    public Integer getEncodedLength() {
        return encodedLength;
    }

    /**
     * Sets the value of the encodedLength property.
     *
     * @param value allowed object is
     *              {@link Integer }
     */
    public void setEncodedLength(Integer value) {
        this.encodedLength = value;
    }

    public Precision getPrecision() {
    	return this.precision;
    }
    
    public Compression getCompression(){
    	return this.compression;
    }
    
    public DataType getDataType(){
    	return this.dataType;
    }
    
    ///// ///// ///// ///// ///// ///// ///// ///// ///// /////
    // adjusted Getter/Setter and public convenience methods

    /**
     * Sets data type, compression, or precision enum based on PSI controlled vocabulary
     * @param acc
     * @return
     */
    public boolean setMemberByAccession(String acc) {
    	if(this.dataType == DataType.UNKNOWN 
    			&& ACCESSION_TO_DATA_TYPE.containsKey(acc)){
    		this.dataType = ACCESSION_TO_DATA_TYPE.get(acc);
    		return true;
    	}
    	
    	if(this.compression == null 
    			&& ACCESSION_TO_COMPRESSION.containsKey(acc)){
    		this.compression = ACCESSION_TO_COMPRESSION.get(acc);
    		return true;
    	}
    	
    	if(this.precision == null
    			&& ACCESSION_TO_PRECISION.containsKey(acc)){
    		this.precision = ACCESSION_TO_PRECISION.get(acc);
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * Checks if decompressing is needed
     */
    protected boolean needsDecompressing() {
    	switch(this.compression) {
    		case NONE:
    			return false;
    		case ZLIB:
    		case NUMPRESS_LINEAR:
    		case NUMPRESS_PIC:
    		case NUMPRESS_SLOF:
    			return true;
    		default: 
    			throw new IllegalStateException("Compression accession cannot be handled");	
    	}
    }

    /**
     * Retrieves the encoded data from a string builder as array of decoded numeric values. 
     * 
     * @return a Number array representation of the binary data.
     */
    public Number[] getDataAsDecodedNumberArray() {
    	if(this.encodedData.length() == 0){
    		return new Number[0];
    	}
    	
    	byte[] encoded = Base64.getDecoder().decode(this.encodedData.toString()); 
    	
        // 2. Decompression of the data (if required)
        byte[] data = this.needsDecompressing() ? BinaryDataArray.decompress(encoded) : encoded;
        
        // 3a. if data has been numpress compressed then do the decompression...
        switch(this.compression) {
        	case NUMPRESS_LINEAR:
        		return BinaryDataArray.numpressDecode(MSNumpress::decodeLinear, data, data.length * 2);
        	case NUMPRESS_PIC:
        		return BinaryDataArray.numpressDecode(MSNumpress::decodePic, data, (data.length - 8) / 2);
        	case NUMPRESS_SLOF:
        		return BinaryDataArray.numpressDecode(MSNumpress::decodeSlof, data, data.length * 2);
        	default:
        		break;
        }
        
        // return the result
        return this.decode(data);
    }

	/**
     * Converts the binary data representing the "null-terminated ASCII string"
     * into a Java String.
     * The method to use if the attached CVParam defines the binary data
     * as "null-terminated ASCII string".
     *
     * @return the String constructed from the binary data.
     * @throws UnsupportedEncodingException if the expected encoding (ASCII) is not supported.
     * @throws IllegalStateException        if the method is used on binary data and the accompanying
     *                                      CVParams state that the data does not represent a "null-terminated ASCII string".
     * @see #getPrecision()
     */
    public String getDataAsDecodedString() throws UnsupportedEncodingException {
        // check if we have the right binary data
        if (this.precision != Precision.NTSTRING) {
            throw new IllegalStateException("This method has to be used with data " +
                    "according to Precision " + Precision.NTSTRING + "!");
        }

    	byte[] encoded = Base64.getDecoder().decode(this.encodedData.toString()); 
    	
        // 2. Decompression of the data (if required)
        byte[] data = this.needsDecompressing() ? BinaryDataArray.decompress(encoded) : encoded;

        // 3. convert the binary data into a String
        // since we are dealing with a "null terminated string" as defined
        // in the mzML specification, we have to first get rid of the null
        // byte before we can convert the data into a Java String.
        byte[] stringData = new byte[data.length - 1]; // one byte less than data
        System.arraycopy(data, 0, stringData, 0, stringData.length);

        return new String(stringData, "ASCII");
    }


  
    ///// ///// ///// ///// ///// ///// ///// ///// ///// /////
    // private helper methods

    private Number[] decode(byte[] data) {
        int step;
        switch (this.precision) {
            case FLOAT64BIT: // fall through
            case INT64BIT:
                step = 8;
                break;
            case FLOAT32BIT: // fall through
            case INT32BIT:
                step = 4;
                break;
            default:
            	throw new IllegalStateException("Cannot convert data with format by CV " + this.precision);
        }
        // create a Number array of sufficient size
        Number[] resultArray = new Number[data.length / step];
        // create a buffer around the data array for easier retrieval
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN); // the order is always LITTLE_ENDIAN
        // progress in steps of 4/8 bytes according to the set step
        for (int indexOut = 0; indexOut < data.length; indexOut += step) {
            // Note that the 'getFloat(index)' and getInt(index) methods read the next 4 bytes
            // and the 'getDouble(index)' and getLong(index) methods read the next 8 bytes.
            Number num;
            switch (this.precision) {
                case FLOAT64BIT:
                    num = bb.getDouble(indexOut);
                    break;
                case INT64BIT:
                    num = bb.getLong(indexOut);
                    break;
                case FLOAT32BIT:
                    num = bb.getFloat(indexOut);
                    break;
                case INT32BIT:
                    num = bb.getInt(indexOut);
                    break;
                default:
                    num = null;
            }
            resultArray[indexOut / step] = num;
        }
        return resultArray;
    }

    public static byte[] decompress(byte[] compressedData) {
        byte[] decompressedData;

        // using a ByteArrayOutputStream to not having to define the result array size beforehand
        Inflater decompressor = new Inflater();

        decompressor.setInput(compressedData);
        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                int count = decompressor.inflate(buf);
                if (count == 0 && decompressor.needsInput()) {
                    break;
                }
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
                throw new IllegalStateException("Encountered wrong data format " +
                        "while trying to decompress binary data!", e);
            }
        }
        try {
            bos.close();
        } catch (IOException e) {
            // ToDo: add logging
            e.printStackTrace();
        }
        // Get the decompressed data
        decompressedData = bos.toByteArray();

        if (decompressedData == null) {
            throw new IllegalStateException("Decompression of binary data produced no result (null)!");
        }
        return decompressedData;
    }
    
    @FunctionalInterface
    public interface MSNumpressDecoder {
    	int decode(byte[] data, int dataSize, double[] result);
    }
    
    /**
     * Convenience method for decoding binary data encoded by MSNumpress.
     * @param decoder  pass in a MSNumpress decode method
     * @param data 
     * @return data as double
     */
    public static Double[] numpressDecode(MSNumpressDecoder decoder, byte[] data, int dataSize){
    	double[] buffer = new double[dataSize];
    	int length = decoder.decode(data, data.length, buffer);
    	Double[] result = new Double[length];
    	System.arraycopy(buffer, 0, result, 0, length);
    	return result;
    }

    /**
     * Compressed source data using the Deflate algorithm.
     * @param uncompressedData Data to be compressed
     * @return Compressed data
     */
    public static byte[] compress(byte[] uncompressedData) {
        byte[] data = null; // Decompress the data

        // create a temporary byte array big enough to hold the compressed data
        // with the worst compression (the length of the initial (uncompressed) data)
        // EDIT: if it turns out this byte array was not big enough, then double its size and try again.
        byte[] temp = new byte[uncompressedData.length / 2];
        int compressedBytes = temp.length;
        while (compressedBytes == temp.length) {
            // compress
            temp = new byte[temp.length * 2];
            Deflater compresser = new Deflater();
            compresser.setInput(uncompressedData);
            compresser.finish();
            compressedBytes = compresser.deflate(temp);
        }      
        
        // create a new array with the size of the compressed data (compressedBytes)        
        data = new byte[compressedBytes];
        System.arraycopy(temp, 0, data, 0, compressedBytes);

        return data;
    }
    
    /**
     * Enumeration defining the allowed precision cases for the binary data
     * as defined in the mzML specifications and the PSI-MS ontology.
     */
    public enum Precision {
        /**
         * Corresponds to the PSI-MS ontology term "MS:1000521" / "32-bit float"
         * and binary data will be represented in the Java primitive: float
         */
        FLOAT32BIT("MS:1000521"),

        /**
         * Corresponds to the PSI-MS ontology term "MS:1000523" / "64-bit float"
         * and binary data will be represented in the Java primitive: double
         */
        FLOAT64BIT("MS:1000523"), 

        /**
         * Corresponds to the PSI-MS ontology term "MS:1000519" / "32-bit integer"
         * and binary data will be represented in the Java primitive: int
         */
        INT32BIT("MS:1000519"),

        /**
         * Corresponds to the PSI-MS ontology term "MS:1000522" / "64-bit integer"
         * and binary data will be represented in the Java primitive: long
         */
        INT64BIT("MS:1000522"),

        /**
         * Corresponds to the PSI-MS ontology term "MS:1001479" / "null-terminated ASCII string"
         * and binary data will be represented in the Java type: String
         */
        NTSTRING("MS:1001479");
        
        private final String acc;
    	
    	Precision(String acc){
    		this.acc = acc;
    	}
    	
    	/** Gets the accession associated with enum **/
    	public String getAccession(){
    		return this.acc;
    	}
    }
    
    /** 
     * Enumeration of compression types {@code BinaryDataArray} can process 
     */
    public enum Compression {
    
    	NONE("MS:1000576"),
    	ZLIB("MS:1000574"),
    	NUMPRESS_LINEAR(MSNumpress.ACC_NUMPRESS_LINEAR),
    	NUMPRESS_PIC(MSNumpress.ACC_NUMPRESS_PIC),
    	NUMPRESS_SLOF(MSNumpress.ACC_NUMPRESS_SLOF);
    	
	    private final String acc;
		
		Compression(String acc){
			this.acc = acc;
		}
		
		public String getAccession(){
			return acc;
		}
    }

    /**
     * Enumeration defining the data types that can/should be present in
     * the binary data arrays.
     */
    public enum DataType {
        /**
         * Based on the constant MS_INTENSITY_AC to define the intensities related to the m/z values of a spectrum.
         * PSI-MS ontology term "MS:1000515"
         */
        INTENSITY("MS:1000515"),

        /**
         * Based on the constant MS_MZ_VALUE_AC to define the m/z values of a spectrum.
         * PSI-MS ontology term "MS:1000514"
         */
        MZ_VALUES("MS:1000514"),
       
        /**
         * Relative time offset values from a reference time. 
         * PSI-MS ontology term "MS:1000595"
         */
        TIME_ARRAY("MS:1000595"),
        
        /**
         * Defines the charges related to the m/z values of a spectrum.
         * PSI-MS ontology term "MS:1000516"
         */
        CHARGE_ARRAY("MS:1000516"), 
        
        /**
         * Used if no other DataType could be determined.
         * Possible reasons: other data encoded, other vocabulary terms used, data not present, ...
         */
        UNKNOWN;
       
        private final String acc;
        private final String name;
    	
        DataType(){
        	this("", "");
        }
        
    	DataType(String acc){
    		this(acc, "");
    	}
    	
    	DataType(String acc, String name){
    		this.acc = acc;
    		this.name = name;
    	}
    	
    	/** Gets the accession associated with enum **/
    	public String getAccession(){
    		return this.acc;
    	}
    	
    	/** Gets the name of the accession **/
    	public String getName(){
    		return this.name;
    	}
    }

    /**
     * Grabs precision enum by accession
     */
    public static final Map<String, Precision> ACCESSION_TO_PRECISION = new HashMap<String, Precision>(){{
    	put(Precision.FLOAT32BIT.getAccession(), Precision.FLOAT32BIT);
    	put(Precision.FLOAT64BIT.getAccession(), Precision.FLOAT64BIT);
    	put(Precision.INT32BIT.getAccession(), Precision.INT32BIT);
    	put(Precision.INT64BIT.getAccession(), Precision.INT64BIT);
    	put(Precision.NTSTRING.getAccession(), Precision.NTSTRING);
    }};
    
    /**
     * Grabs compression enum by accession
     */
    public static final Map<String, Compression> ACCESSION_TO_COMPRESSION = new HashMap<String, Compression>(){{
    	put(Compression.NONE.getAccession(), Compression.NONE);
    	put(Compression.ZLIB.getAccession(), Compression.ZLIB);
    	put(Compression.NUMPRESS_LINEAR.getAccession(), Compression.NUMPRESS_LINEAR);
    	put(Compression.NUMPRESS_PIC.getAccession(), Compression.NUMPRESS_PIC);
    	put(Compression.NUMPRESS_SLOF.getAccession(), Compression.NUMPRESS_SLOF);
    }};
    
    /**
     * Grabs data type enum by accession
     */
    public static final Map<String, DataType> ACCESSION_TO_DATA_TYPE = new HashMap<String, DataType>(){{
    	put(DataType.INTENSITY.getAccession(), DataType.INTENSITY);
    	put(DataType.MZ_VALUES.getAccession(), DataType.MZ_VALUES);
    	put(DataType.TIME_ARRAY.getAccession(), DataType.TIME_ARRAY);
    	put(DataType.CHARGE_ARRAY.getAccession(), DataType.CHARGE_ARRAY);
    }};

    /** raw array of data **/
    protected StringBuilder encodedData;
    protected Integer encodedLength;
    
    /**
     * raw data without compression
     */
    protected byte[] rawDecodedData;
    
    /** type of compression of binary **/
    private Compression compression;
    /** type of precision of binary **/
    private Precision precision;
    /** type of data **/
    private DataType dataType;
}


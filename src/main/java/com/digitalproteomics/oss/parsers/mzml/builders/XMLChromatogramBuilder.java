package com.digitalproteomics.oss.parsers.mzml.builders;

import com.digitalproteomics.oss.parsers.mzml.BinaryDataArray;
import com.digitalproteomics.oss.parsers.mzml.model.Chromatogram;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class XMLChromatogramBuilder implements FromXMLStreamBuilder<Chromatogram> {

    protected Chromatogram currChromatogram;

    /**
     * binary data associated with a spectrum
     */
    protected List<BinaryDataArray> data;
    /** private members are used to retain nesting state **/
    private BinaryDataArray currData;
    private boolean inBinaryNesting;

    /**
     * Constructor for building a chromatogram
     *
     * @param xr - initial state used for starting a chromatogram
     */
    public XMLChromatogramBuilder(String filename, XMLStreamReader xr) {
        this.currChromatogram = new Chromatogram(xr.getAttributeValue(null, "id"),
                null,
                null);
    }

    @Override
    public boolean buildsFromChromatogramTags() {
        return true;
    }

    private double[] toDoubleArray(Number[] arr) {
        double[] v = new double[arr.length];
        IntStream.range(0, arr.length).forEach(i -> v[i] = arr[i].doubleValue());
        return v;
    }

    @Override
    public Chromatogram build() {
        double[] times = null;
        double[] intensities = null;

        for(BinaryDataArray arr : this.data){
            switch(arr.getDataType()){
                case INTENSITY:
                    intensities = this.toDoubleArray(arr.getDataAsDecodedNumberArray());
                    break;
                case TIME_ARRAY:
                    times = this.toDoubleArray(arr.getDataAsDecodedNumberArray());
                    break;
                default:
                    break;
            }
        }
        this.currChromatogram.setTimes(times);
        this.currChromatogram.setIntensities(intensities);

        return this.currChromatogram;
    }

    @Override
    public void accept(XMLStreamReader xr) {
        if ((xr.getEventType() == XMLStreamConstants.START_ELEMENT)
                && xr.getLocalName().equals("cvParam")) {

            if (xr.getAttributeValue(null, "accession").equals("MS:1000235")) {
                this.currChromatogram.setId(xr.getAttributeValue(null, "name"));
            }
        }
        if(xr.getEventType() == XMLStreamConstants.START_ELEMENT) {
            if(xr.getLocalName().equals("binaryDataArrayList")){

                this.data = new ArrayList<>(Integer.parseInt(xr.getAttributeValue(null, "count")));

            } else if(xr.getLocalName().equals("binaryDataArray")){

                this.currData = new BinaryDataArray();
                this.currData.setEncodedLength(Integer.valueOf(xr.getAttributeValue(null, "encodedLength")));

            } else if(xr.getLocalName().equals("cvParam")
                    && this.currData != null) {

                this.currData.setMemberByAccession(xr.getAttributeValue(null, "accession"));

            } else if(xr.getLocalName().equals("binary")
                    && this.currData != null){

                this.inBinaryNesting = true;

            }
        } else if(xr.getEventType() == XMLStreamConstants.CHARACTERS
                && this.inBinaryNesting){

            this.currData.appendToEncodedData(xr.getText());

        } else if(xr.getEventType() == XMLStreamConstants.END_ELEMENT){
            if(xr.getLocalName().equals("binaryDataArray")){

                this.data.add(this.currData);
                this.currData = null;

            } else if(xr.getLocalName().equals("binary")) {

                this.inBinaryNesting = false;

            }
        }
    }
}

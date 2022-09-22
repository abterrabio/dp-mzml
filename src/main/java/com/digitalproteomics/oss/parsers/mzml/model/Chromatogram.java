package com.digitalproteomics.oss.parsers.mzml.model;

/**
 * Stores a chromatogram (retention time and counts).
 */
public class Chromatogram {

    /**
     * Identifier for chromatogram.
     */
    String id;
    /**
     * Retention time in minutes.
     */
    double[] times;

    /**
     * Intensity values associated with time array.
     */
    double[] intensities;

    /**
     * Full constructor for storing chromatogram values.
     * @param id identifier of the chromatagram data
     * @param times retention time in minutes
     * @param intensities values associated with time array
     */
    public Chromatogram(String id, double[] times, double[] intensities) {
        this.id = id;
        this.times = times;
        this.intensities = intensities;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double[] getTimes() {
        return times;
    }

    public void setTimes(double[] times) {
        this.times = times;
    }

    public double[] getIntensities() {
        return intensities;
    }

    public void setIntensities(double[] intensities) {
        this.intensities = intensities;
    }
}

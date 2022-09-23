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
     * Pressure values associated with time array.
     */
    double[] pressures;

    /**
     * Full constructor for storing chromatogram values.
     * @param id identifier of the chromatagram data
     */
    public Chromatogram(String id) {
        this.id = id;
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

    public double[] getPressures() {
        return pressures;
    }

    public void setPressures(double[] pressures) {
        this.pressures = pressures;
    }
}

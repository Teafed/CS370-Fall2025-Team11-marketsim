package com.etl;

/**
 * Minimal model for Finnhub /stock/candle JSON responses.
 * Fields match the API: c (close), h (high), l (low), o (open), t (timestamps),
 * v (volumes), s (status)
 */
public class CandleResponse {
    /** Array of close prices. */
    private double[] c;
    /** Array of high prices. */
    private double[] h;
    /** Array of low prices. */
    private double[] l;
    /** Array of open prices. */
    private double[] o;
    /** Array of timestamps. */
    private long[] t;
    /** Array of volumes. */
    private double[] v;
    /** Status of the response. */
    private String s;

    /**
     * Gets the close prices.
     * 
     * @return Array of close prices.
     */
    public double[] getC() {
        return c;
    }

    /**
     * Sets the close prices.
     * 
     * @param c Array of close prices.
     */
    public void setC(double[] c) {
        this.c = c;
    }

    /**
     * Gets the high prices.
     * 
     * @return Array of high prices.
     */
    public double[] getH() {
        return h;
    }

    /**
     * Sets the high prices.
     * 
     * @param h Array of high prices.
     */
    public void setH(double[] h) {
        this.h = h;
    }

    /**
     * Gets the low prices.
     * 
     * @return Array of low prices.
     */
    public double[] getL() {
        return l;
    }

    /**
     * Sets the low prices.
     * 
     * @param l Array of low prices.
     */
    public void setL(double[] l) {
        this.l = l;
    }

    /**
     * Gets the open prices.
     * 
     * @return Array of open prices.
     */
    public double[] getO() {
        return o;
    }

    /**
     * Sets the open prices.
     * 
     * @param o Array of open prices.
     */
    public void setO(double[] o) {
        this.o = o;
    }

    /**
     * Gets the timestamps.
     * 
     * @return Array of timestamps.
     */
    public long[] getT() {
        return t;
    }

    /**
     * Sets the timestamps.
     * 
     * @param t Array of timestamps.
     */
    public void setT(long[] t) {
        this.t = t;
    }

    /**
     * Gets the volumes.
     * 
     * @return Array of volumes.
     */
    public double[] getV() {
        return v;
    }

    /**
     * Sets the volumes.
     * 
     * @param v Array of volumes.
     */
    public void setV(double[] v) {
        this.v = v;
    }

    /**
     * Gets the status.
     * 
     * @return Status string.
     */
    public String getS() {
        return s;
    }

    /**
     * Sets the status.
     * 
     * @param s Status string.
     */
    public void setS(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return "CandleResponse{s=" + s + ", points=" + (t == null ? 0 : t.length) + '}';
    }
}

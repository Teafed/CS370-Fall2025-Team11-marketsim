package com.etl;

/**
 * Minimal model for Finnhub /stock/candle JSON responses.
 * Fields match the API: c (close), h (high), l (low), o (open), t (timestamps), v (volumes), s (status)
 */
public class CandleResponse {
    private double[] c;
    private double[] h;
    private double[] l;
    private double[] o;
    private long[] t;
    private double[] v;
    private String s;

    public double[] getC() { return c; }
    public void setC(double[] c) { this.c = c; }

    public double[] getH() { return h; }
    public void setH(double[] h) { this.h = h; }

    public double[] getL() { return l; }
    public void setL(double[] l) { this.l = l; }

    public double[] getO() { return o; }
    public void setO(double[] o) { this.o = o; }

    public long[] getT() { return t; }
    public void setT(long[] t) { this.t = t; }

    public double[] getV() { return v; }
    public void setV(double[] v) { this.v = v; }

    public String getS() { return s; }
    public void setS(String s) { this.s = s; }

    @Override
    public String toString() {
        return "CandleResponse{s=" + s + ", points=" + (t == null ? 0 : t.length) + '}';
    }
}


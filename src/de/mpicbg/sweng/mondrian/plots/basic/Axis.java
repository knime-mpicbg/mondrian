package de.mpicbg.sweng.mondrian.plots.basic;

import de.mpicbg.sweng.mondrian.util.StatUtil;

//


public class Axis {

    private double min;
    private double max;
    public double tickM, tickMM, tickMMM, tickMMMM;
    private double firstM;
    private double firstMM;
    private double firstMMM;
    private double lastM;
    private double lastMM;
    private double lastMMM;
    private int numM;
    private int numMM;
    private int numMMM;


    public Axis(double min, double max) {

        this.min = min;
        this.max = max;

        double range = max - min;

        double d = Math.floor(Math.log(range) / Math.log(10));

        double r10 = range / Math.pow(10, d);

        if (r10 < 1.3) {
            tickM = 1;
            tickMM = 0.5;
            tickMMM = 0.25;
            tickMMMM = 0.2;
        } else if (r10 < 2.6) {
            tickM = 2;
            tickMM = 1;
            tickMMM = 0.5;
            tickMMMM = 0.25;
        } else if (r10 < 7) {
            tickM = 5;
            tickMM = 2.5;
            tickMMM = 2;
            tickMMMM = 1;
        } else {
            tickM = 10;
            tickMM = 5;
            tickMMM = 2.5;
            tickMMMM = 2;
        }
        tickM = StatUtil.round(tickM * Math.pow(10, d - 1), (int) (3 - d));
        if (min % tickM != 0)
            firstM = (Math.floor(min / tickM)) * tickM;
        else
            firstM = min;
        lastM = Math.floor(max / tickM) * tickM;
        numM = (int) ((lastM - firstM) / tickM) + 1;

        tickMM = StatUtil.round(tickMM * Math.pow(10, d - 1), (int) (3 - d));
        if (min % tickMM != 0)
            firstMM = (Math.floor(min / tickMM)) * tickMM;
        else
            firstMM = min;
        lastMM = Math.floor(max / tickMM) * tickMM;
        numMM = (int) ((lastMM - firstMM) / tickMM) + 1;

        tickMMM = StatUtil.round(tickMMM * Math.pow(10, d - 1), (int) (3 - d));
        if (min % tickMMM != 0)
            firstMMM = (Math.floor(min / tickMMM)) * tickMMM;
        else
            firstMMM = min;
        lastMMM = Math.floor(max / tickMMM) * tickMMM;
        numMMM = (int) ((lastMMM - firstMMM) / tickMMM) + 1;

        tickMMMM = StatUtil.round(tickMMMM * Math.pow(10, d - 1), (int) (3 - d));
        double firstMMMM;
        if (min % tickMMMM != 0)
            firstMMMM = (Math.floor(min / tickMMMM)) * tickMMMM;
        else
            firstMMMM = min;
        double lastMMMM = Math.floor(max / tickMMMM) * tickMMMM;
        int numMMMM = (int) ((lastMMMM - firstMMMM) / tickMMMM) + 1;

        this.print();
    }


    void print() {
        System.out.println("Min: " + min + ", 1st: " + firstM + ", Tick: " + tickM + " last:" + lastM + ", Max: " + max + " Num: " + numM);
        System.out.println("Min: " + min + ", 1st: " + firstMM + ", Tick: " + tickMM + " last:" + lastMM + ", Max: " + max + " Num: " + numMM);
        System.out.println("Min: " + min + ", 1st: " + firstMMM + ", Tick: " + tickMMM + " last:" + lastMMM + ", Max: " + max + " Num: " + numMMM);
    }
}

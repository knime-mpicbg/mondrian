package org.rosuda.mondrian.core;

import org.rosuda.mondrian.BufferTokenizer;
import org.rosuda.mondrian.util.Qsort;
import org.rosuda.mondrian.util.Util;

import java.sql.ResultSet;
import java.sql.Statement;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Variable {

    private int catThres;
    private int dimThres = 1000;
    protected String[] levelA = new String[dimThres];
    protected int[] grpSize = new int[dimThres];
    public int[] permA;
    public int[] IpermA;
    public int levelP = 0;
    protected boolean alpha;
    public boolean isCategorical = true;
    public boolean forceCategorical = false;
    public boolean phoneNumber = false;
    public boolean isPolyID = false;
    public String name;
    public double[] data;
    public int numMiss = 0;
    public int[] sortI;
    public boolean missing[];
    public double min = 1e+100, max = -1e+100;
    protected boolean minSet = false, maxSet = false, levelsSet = false;
    private DataSet dataSet;


    Variable(DataSet dataSet, boolean alpha, String name) {
        this.dataSet = dataSet;
        this.alpha = alpha;
        this.name = name;
        if (name.substring(0, 2).equals("/P"))
            isCategorical = false;


        catThres = (dataSet.n > 800) ? (15 * Math.max(1, (int) (Math.log(dataSet.n) / Math.log(10)) - 1)) : ((int) (1.5 * Math.sqrt(dataSet.n)));
    }


    Variable(DataSet dataSet, int n, boolean alpha, String name) {
        this.dataSet = dataSet;
        this.alpha = alpha;
        this.name = name;
        if (name.length() > 1)
            if (name.substring(0, 2).equals("/P"))
                isCategorical = false;
        data = new double[n];
        missing = new boolean[n];
        catThres = (dataSet.n > 800) ? (15 * Math.max(1, (int) (Math.log(dataSet.n) / Math.log(10)) - 1)) : ((int) (1.5 * Math.sqrt(dataSet.n)));
    }


    Variable(DataSet dataSet, BufferTokenizer BT, int col) {
        this.dataSet = dataSet;
        this.alpha = !BT.numericalColumn[col];
        this.name = new String(BT.head[col]);
        this.isCategorical = BT.isDiscret[col];
        data = new double[BT.lines];
        missing = new boolean[BT.lines];

        System.arraycopy(BT.item[col], 0, data, 0, BT.lines);
        System.arraycopy(BT.NA[col], 0, missing, 0, BT.lines);
        BT.item[col] = null;

        if (!isCategorical)
            sortData();
        else {
            //System.out.println(" new line is "+BT.newLineBreaker+"<-");
            levelP = BT.wordStackSize[col];
            levelA = new String[levelP];
            grpSize = new int[levelP];
            for (int j = 0; j < levelP; j++) {
                if (alpha)
                    levelA[j] = new String(BT.word[col][j]);
                else
                    levelA[j] = Double.toString(Double.valueOf(BT.discretValue[col][j] + "").doubleValue());
                grpSize[j] = BT.wordCount[col][j];
//System.out.println(levelP+" -|- >"+levelA[j]+"< ("+grpSize[j]+")");
            }
            sortLevels();
        }
        catThres = (dataSet.n > 800) ? (15 * Math.max(1, (int) (Math.log(dataSet.n) / Math.log(10)) - 1)) : ((int) (1.5 * Math.sqrt(dataSet.n)));
    }


    public String getName() {
        if (name.length() > 1 && name.substring(0, 1).equals("/"))
            return name.substring(2);
        else
            return name;
    }


    public boolean isPolyID() {
        return isPolyID;
    }


    public double isLevel(String name) {
        if (!levelsSet && dataSet.isDB)
            maintainDBVariable();
        if (isCategorical) {
            for (int i = 0; i < levelP; i++) {
                if (levelA[i].equals(name)) {
                    grpSize[i]++;
                    return i;
                }
            }
//        System.out.println(">>>>>>>>>>>>"+(grpSize[levelP])+"  "+levelP+"   "+grpSize.length);
            grpSize[levelP]++;
            levelA[this.levelP++] = name;
            if ((this.levelP >= catThres || this.levelP > dimThres - 2) && !forceCategorical && !alpha)
                isCategorical = false;
            if ((alpha || forceCategorical) && this.levelP > dimThres - 2)
                expand();
            return this.levelP - 1;
        } else {
            isCategorical = false;
            return -1;
        }
    }


    public double Level(String name) {
        if (!levelsSet && dataSet.isDB)
            maintainDBVariable();
        if (isCategorical) {
            if (IpermA == null)
                sortLevels();
            for (int i = 0; i < levelP; i++) {
//System.out.println(name+" <-> "+levelA[i]);
                if (levelA[i].equals(name)) {
                    return i; //permA[i];
                }
            }
            return 3.1415926;
        } else {
            return -1;
        }
    }


    public int getNumLevels() {
        if (!levelsSet && dataSet.isDB)
            maintainDBVariable();
        return levelP;
    }


    public String[] getLevels() {
        String[] returnA = new String[levelP];
        for (int i = 0; i < levelP; i++) {
            if (!levelA[permA[i]].equals("1.7976931348623157E308"))   // We rely on the IEEE double floating point system here !!!!!!!!!!
                returnA[i] = levelA[permA[i]];
            else
                returnA[i] = "NA";
//System.out.println(name+" <-> "+returnA[i]);
        }

        boolean allDotNull = true;
        for (int i = 0; i < returnA.length; i++)
            if (!returnA[i].endsWith(".0") && !returnA[i].equals("NA"))
                allDotNull = false;
        if (allDotNull)
            for (int i = 0; i < returnA.length; i++)
                if (returnA[i].endsWith(".0"))
                    returnA[i] = returnA[i].substring(0, returnA[i].length() - 2);

        return returnA;
    }


    public String getLevel(int id) {
        return levelA[permA[id]];
    }


    public void shrink() {
        if (!isCategorical)
            levelP = 10;
        levelA = (String[]) Util.resizeArray(levelA, levelP);
        grpSize = (int[]) Util.resizeArray(grpSize, levelP);
        if (!isCategorical) {
            levelP = 0;
            dimThres = 10;
        }
    }


    public void expand() {
        dimThres = (int) (1.5 * dimThres);
        System.out.println("-- Expand to: " + dimThres);
        levelA = (String[]) Util.resizeArray(levelA, dimThres);
        grpSize = (int[]) Util.resizeArray(grpSize, dimThres);
    }


    public void sortData() {
        System.out.println("--------- Real Sort --------: " + name);
        double[] sA = new double[dataSet.n];
        System.arraycopy(data, 0, sA, 0, dataSet.n);
        sortI = Qsort.qsort(sA, 0, dataSet.n - 1);
    }


    public void sortLevels() {
        System.out.println("------ Discret Sort --------: " + name);
        if (!alpha) {

            double[] ss = new double[levelP];
            for (int i = 0; i < levelP; i++) {
//System.out.println( Double.valueOf( levelA[i] ).doubleValue() );
                ss[i] = Double.valueOf(levelA[i]).doubleValue();
            }
            permA = Qsort.qsort(ss, 0, levelP - 1);
        } else {

            String[] sA = new String[levelP];
            for (int i = 0; i < levelP; i++)
                sA[i] = levelA[i].toUpperCase();
            permA = Qsort.qsort(sA, 0, levelP - 1);
        }
        IpermA = new int[levelP];
        for (int i = 0; i < levelP; i++) {
            IpermA[permA[i]] = i;
        }
    }


    public int getGroupSize(int grp) {
//      System.out.println( "grp: "+grp+" grpSize: "+getNumLevels() );
        return grpSize[grp];
    }


    public double Min() {
        if (!minSet)
            if (dataSet.isDB) {
                try {
                    Statement stmt = dataSet.con.createStatement();
                    String query = "select min(" + name + ") from " + dataSet.Table;
                    ResultSet rs = stmt.executeQuery(query);

                    if (rs.next())
                        this.min = Util.atod(rs.getString(1));
                    rs.close();
                    stmt.close();
                    System.out.println("query: " + query + " ---> " + this.min);
                } catch (Exception ex) {
                    System.out.println("DB Exception: get min ... " + ex);
                }
            } else
                for (int i = 0; i < data.length; i++)
                    this.min = Math.min(data[i], this.min);
        minSet = true;
        return this.min;
    }


    public double SelMin() {
        double SM = Double.MAX_VALUE;
        for (int i = 0; i < data.length; i++)
            if (dataSet.selectionArray[i] > 0 && !missing[i])
                SM = Math.min(SM, data[i]);
        return SM;
    }


    public double Max() {
        if (!maxSet)
            if (dataSet.isDB) {
                try {
                    Statement stmt = dataSet.con.createStatement();
                    String query = "select max(" + name + ") from " + dataSet.Table;
                    ResultSet rs = stmt.executeQuery(query);

                    if (rs.next())
                        this.max = Util.atod(rs.getString(1));
                    rs.close();
                    stmt.close();
                    System.out.println("query: " + query + " ---> " + this.max);
                } catch (Exception ex) {
                    System.out.println("DB Exception: get max ... " + ex);
                }
            } else if (!alpha)
                if (!isCategorical)
                    this.max = data[sortI[data.length - numMiss - 1]];
                else if (numMiss == 0)
                    this.max = Double.valueOf(levelA[permA[levelP - 1]]).doubleValue();
                else
                    this.max = Double.valueOf(levelA[permA[levelP - 2]]).doubleValue();
            else
                for (int i = 0; i < dataSet.n - numMiss; i++)
                    this.max = Math.max(data[i], this.max);
        maxSet = true;
        return this.max;
    }


    public double SelMax() {
        double SM = Double.MIN_VALUE;
        for (int i = 0; i < data.length; i++)
            if (dataSet.selectionArray[i] > 0 && !missing[i])
                SM = Math.max(SM, data[i]);
//System.out.println("Return Max: "+SM);
        return SM;
    }


    public double Mean() {
        double sum = 0;
        for (int i = 0; i < dataSet.n; i++)
            if (!missing[i])
                sum += data[i];
        return sum / (dataSet.n - numMiss);
    }


    public double selMean() {
        double sum = 0;
        int counter = 0;
        for (int i = 0; i < dataSet.n; i++)
            if (dataSet.selectionArray[i] > 0 && !missing[i]) {
                sum += data[i];
                counter++;
            }
        return sum / counter;
    }


    public double SDev() {
        double sum2 = 0;
        for (int i = 0; i < dataSet.n; i++)
            if (!missing[i])
                sum2 += data[i] * data[i];
        return Math.pow((sum2 - Math.pow(Mean(), 2) * (dataSet.n - numMiss)) / ((dataSet.n - numMiss) - 1), 0.5);
    }


    public double selSDev() {
        double sum2 = 0;
        int counter = 0;
        for (int i = 0; i < dataSet.n; i++)
            if (dataSet.selectionArray[i] > 0 && !missing[i]) {
                sum2 += data[i] * data[i];
                counter++;
            }
        return Math.pow((sum2 - Math.pow(selMean(), 2) * counter) / (counter - 1), 0.5);
    }


    public double getQuantile(double q) {
        if (!dataSet.filterON)
            if (!isCategorical) {
                int ind = (int) ((dataSet.n - numMiss - 1) * q);
                double remainder = ((dataSet.n - numMiss - 1) * q) - ind;
//System.out.println(" Q: "+q+" INDEX:"+ind);
                if (ind < dataSet.n - 1)
                    return data[sortI[ind]] * (1 - remainder) + data[sortI[ind + 1]] * remainder;
                else
                    return data[sortI[ind]];
            } else
                return 0;
        else {
            int count = 0;
            int i = 0;
            if (q == 0) {
                while (dataSet.filterA[sortI[i]] != dataSet.filterVal) {
                    i++;
//System.out.println("filter Val: "+filterVal+" filterVar: "+filterVar+" i:"+i+" - "+filterA[sortI[i]]);
                }
                return data[sortI[i]];
            }
            if (q == 1) {
                i = dataSet.n - numMiss - 1;
                while (dataSet.filterA[sortI[i]] != dataSet.filterVal) {
                    i--;
                }
                return data[sortI[i]];
            }
//        System.out.println("filterGrp: "+filterGrp+" filterGrps: "+filterGrpSize.length);
//System.out.println("filter Val: "+filterVal+" filterVar: "+filterVar+" GroupSize. "+filterGrpSize[filterGrp]+" Group: "+filterGrp);
            int stop = (int) (q * (dataSet.filterGrpSize[dataSet.filterGrp] - 1));
            while (count <= stop && i < dataSet.n) {
                if (dataSet.filterA[sortI[i]] == dataSet.filterVal && !missing[sortI[i]]) {
                    count++;
//System.out.println("i: "+i+" filter Val: "+filterVal+" testVal: "+filterA[sortI[i]]+" GroupSize. "+filterGrpSize[filterGrp]);
                }
                i++;
            }
            i--;
//            System.out.println("q: "+q+" Count: "+count+" Value: "+ data[sortI[i-1]]);
            if (count < dataSet.filterGrpSize[dataSet.filterGrp] && stop + 0.000001 < (q * (dataSet.filterGrpSize[dataSet.filterGrp] - 1))) {   // get next for linear combi of two values ...
                int j = i + 1;
                while (dataSet.filterA[sortI[j]] != dataSet.filterVal || missing[sortI[j]]) {
                    j++;
                }
                j--;
                double remainder = (q * (dataSet.filterGrpSize[dataSet.filterGrp] - 1)) - stop;
//          System.out.println(" GET NEXT :"+i+" <-> "+j);
                return data[sortI[i]] * (1 - remainder) + data[sortI[j]] * remainder;
            } else
                return data[sortI[i]];
        }
    }


    public double getSelQuantile(int var, double q) {
        int count = 0;
        int i = 0;
        if (!dataSet.filterON) {
            if (!isCategorical) {
                if (q == 0) {
                    while (dataSet.selectionArray[sortI[i++]] == 0) {
                    }
                    return data[sortI[i - 1]];
                }
                if (q == 1) {
                    i = dataSet.n - numMiss - 1;
                    while (i >= 0 && dataSet.selectionArray[sortI[i]] == 0) {
                        i--;
                    }
                    return data[sortI[i]];
                }
                int stop = (int) (q * (dataSet.countSelection(var) - 1));
                while (count <= stop && i < dataSet.n) {
                    if (dataSet.selectionArray[sortI[i]] > 0 && !missing[sortI[i]])
                        count++;
                    i++;
                }
                i--;
                //System.out.println(" Sel: "+countSelection(var)+" q: "+q+" i: "+i+" Stop: "+stop+" Count: "+count);
                if (count < dataSet.countSelection(var) && stop + 0.000001 < (q * (dataSet.countSelection(var) - 1))) {   // get next for linear combi of two values ...
                    int j = i + 1;
                    while (dataSet.selectionArray[sortI[j]] == 0 || missing[sortI[j]]) {
                        j++;
                    }
                    if (j != i + 1)
                        j--;
                    double remainder = (q * (dataSet.countSelection(var) - 1)) - stop;
                    //          System.out.println(" GET NEXT :"+i+" <-> "+j);
                    return data[sortI[i]] * (1 - remainder) + data[sortI[j]] * remainder;
                } else
                    return data[sortI[i]];
            } else
                return 0;
        } else {
            if (q == 0) {
                while ((dataSet.selectionArray[sortI[i]] == 0) || (dataSet.filterA[sortI[i]] != dataSet.filterVal) || missing[sortI[i]]) {
                    i++;/*System.out.println("i: "+i+" "+selectionArray[sortI[i]]+" "+filterA[sortI[i]]+" "+filterVal);*/
                }
                return data[sortI[i]];
            }
            if (q == 1) {
                i = dataSet.n - numMiss - 1;
                while (dataSet.selectionArray[sortI[i]] == 0 || dataSet.filterA[sortI[i]] != dataSet.filterVal || missing[sortI[i]]) {
                    i--;/*System.out.println("i: "+i);*/
                }
                return data[sortI[i]];
            }
            int stop = (int) (q * (dataSet.filterSelGrpSize[dataSet.filterGrp] - 1));
//System.out.println("in q grpSize: "+filterSelGrpSize[filterGrp]);
            while (count <= stop) {
                if (dataSet.selectionArray[sortI[i]] > 0 && dataSet.filterA[sortI[i]] == dataSet.filterVal && !missing[sortI[i]])
                    count++;
                i++;
            }
            i--;
//System.out.println(" Sel: "+filterSelGrpSize[filterGrp]+" q: "+q+" i: "+i+" Stop: "+stop+" Count: "+count+" filterVal: "+filterVal);
            if (count < dataSet.filterSelGrpSize[dataSet.filterGrp] && stop + 0.000001 < (q * (dataSet.filterSelGrpSize[dataSet.filterGrp] - 1))) {   // get next for linear combi of two values ...
                int j = i + 1;
                while ((dataSet.selectionArray[sortI[j]] == 0) || (dataSet.filterA[sortI[j]] != dataSet.filterVal) || missing[sortI[j]]) {
                    j++;
                }
                //          {System.out.println(" j: "+j+" Filter: "+filterA[sortI[j]]+" Sel: "+selectionArray[sortI[j]]+" miss: "+missing[sortI[j]]);j++;}
                j--;
                double remainder = (q * (dataSet.filterSelGrpSize[dataSet.filterGrp] - 1)) - stop;
//          System.out.println(" GET NEXT :"+i+" <-> "+j+" ("+remainder+" - "+(1-remainder)+")");
                return data[sortI[i]] * (1 - remainder) + data[sortI[j]] * remainder;
            } else
                return data[sortI[i]];
        }
    }


    public double getFirstGreater(double g) {
        int i = 0;
        if (!dataSet.filterON) {
            double ret = data[sortI[i]];
            while ((ret = data[sortI[i]]) < g)
                i++;
            return ret;
        } else {
            while (i < dataSet.n - 1 && (data[sortI[i]]) < g)
                i++;
            while (i < dataSet.n - 1 && dataSet.filterA[sortI[i]] != dataSet.filterVal)
                i++;
            return data[sortI[i]];
        }
    }


    public double getFirstSelGreater(double g) {
        int i = 0;
        if (!dataSet.filterON) {
            while (i < dataSet.n - 1 && (data[sortI[i]]) < g)
                i++;
            while (i < dataSet.n - 1 && dataSet.selectionArray[sortI[i]] == 0)
                i++;
            return data[sortI[i]];
        } else {
            while (i < dataSet.n - 1 && (data[sortI[i]]) < g)
                i++;
            while (i < dataSet.n - 1 && (dataSet.selectionArray[sortI[i]] == 0 || dataSet.filterA[sortI[i]] != dataSet.filterVal))
                i++;
            return data[sortI[i]];
        }
    }


    public double getFirstSmaller(double s) {
        int i = dataSet.n - 1;
        if (!dataSet.filterON) {
            double ret = data[sortI[i]];
            while ((ret = data[sortI[i]]) > s)
                i--;
            return ret;
        } else {
            while (i > 0 && (data[sortI[i]]) > s)
                i--;
            while (i > 0 && dataSet.filterA[sortI[i]] != dataSet.filterVal)
                i--;
            return data[sortI[i]];
        }
    }


    public double getFirstSelSmaller(double s) {
        int i = dataSet.n - 1;
        if (!dataSet.filterON) {
            while (i > 0 && (data[sortI[i]]) > s)
                i--;
            while (i > 0 && dataSet.selectionArray[sortI[i]] == 0)
                i--;
            return data[sortI[i]];
        } else {
            while (i > 0 && (data[sortI[i]]) > s)
                i--;
            while (i > 0 && (dataSet.selectionArray[sortI[i]] == 0 || dataSet.filterA[sortI[i]] != dataSet.filterVal))
                i--;
            return data[sortI[i]];
        }
    }


    public double[] getAllSmaller(double s) {
        int i = 0;
        if (!dataSet.filterON) {
            while ((data[sortI[i++]]) < s) {
            }
            double[] ret = new double[i - 1];
            for (int j = 0; j < i - 1; j++)
                ret[j] = data[sortI[j]];
            return ret;
        } else {
            int count = 0;
            while (i < dataSet.n && data[sortI[i]] < s)
                if (dataSet.filterA[sortI[i++]] == dataSet.filterVal)
                    count++;
            //while( i<n && filterA[sortI[i++]] != filterVal ) {}
            //count++;
            if (count > 0) {
                double[] ret = new double[count];
                count = 0;
                for (int j = 0; j < i; j++)
                    if (dataSet.filterA[sortI[j]] == dataSet.filterVal)
                        ret[count++] = data[sortI[j]];
                return ret;
            } else {
                return new double[0];
            }
        }
    }


    public double[] getAllSelSmaller(double s) {
        int i = 0;
        int count = 0;
        if (!dataSet.filterON) {
            while (i < dataSet.n && data[sortI[i]] < s)
                if (dataSet.selectionArray[sortI[i++]] > 0 && !missing[sortI[i]])
                    count++;
//        while( i<n && selectionArray[sortI[i++]] == 0 && !missing[sortI[i]] ) {}
//        count++;
            if (count > 0) {
                double[] ret = new double[count];
                count = 0;
                for (int j = 0; j < i; j++)
                    if (dataSet.selectionArray[sortI[j]] > 0 && !missing[sortI[j]])
                        ret[count++] = data[sortI[j]];
                return ret;
            } else {
                return new double[0];
            }
        } else {
            while (i < dataSet.n && data[sortI[i]] < s) {
                if (dataSet.filterA[sortI[i]] == dataSet.filterVal && dataSet.selectionArray[sortI[i]] > 0)
                    count++;
                i++;
            }
            if (count > 0) {
                double[] ret = new double[count];
                count = 0;
                for (int j = 0; j < i; j++)
                    if (dataSet.filterA[sortI[j]] == dataSet.filterVal && dataSet.selectionArray[sortI[j]] > 0)
                        ret[count++] = data[sortI[j]];
                return ret;
            } else {
                return new double[0];
            }
        }
    }


    public double[] getAllGreater(double g) {
        int i = dataSet.n - numMiss - 1;
        if (!dataSet.filterON) {
            while ((data[sortI[i--]]) > g) {
            }
            double[] ret = new double[dataSet.n - numMiss - i - 2];
            for (int j = dataSet.n - numMiss - 1; j > i + 1; j--)
                ret[dataSet.n - numMiss - j - 1] = data[sortI[j]];
            return ret;
        } else {
            int count = 0;
            while (i >= 0 && (data[sortI[i]]) > g)
                if (dataSet.filterA[sortI[i--]] == dataSet.filterVal)
                    count++;
            if (count > 0) {
                double[] ret = new double[count];
                count = 0;
                for (int j = dataSet.n - numMiss - 1; j > i; j--)
                    if (dataSet.filterA[sortI[j]] == dataSet.filterVal && count < ret.length)
                        ret[count++] = data[sortI[j]];
                return ret;
            } else {
                return new double[0];
            }
        }
    }


    public double[] getAllSelGreater(double g) {
        int i = dataSet.n - numMiss - 1;
        int count = 0;
        if (!dataSet.filterON) {
            while (i >= 0 && (data[sortI[i]]) >= g) {
                if (dataSet.selectionArray[sortI[i]] > 0 && !missing[sortI[i]])
                    count++;
                i--;
            }
//      while( i>=0 && selectionArray[sortI[i--]] == 0 ) {}
//      count++;
            if (count > 0) {
                double[] ret = new double[count];
                count = 0;
                for (int j = dataSet.n - numMiss - 1; j > i; j--)
                    if (dataSet.selectionArray[sortI[j]] > 0 && !missing[sortI[j]])
                        ret[count++] = data[sortI[j]];
                return ret;
            } else {
                return new double[0];
            }
        } else {
            while (i >= 0 && (data[sortI[i]]) > g) {
                if (dataSet.filterA[sortI[i]] == dataSet.filterVal && dataSet.selectionArray[sortI[i]] > 0)
                    count++;
                i--;
            }
            if (count > 0) {
                double[] ret = new double[count];
                count = 0;
                for (int j = dataSet.n - numMiss - 1; j > i; j--)
                    if (dataSet.filterA[sortI[j]] == dataSet.filterVal && dataSet.selectionArray[sortI[j]] > 0)
                        ret[count++] = data[sortI[j]];
                return ret;
            } else {
                return new double[0];
            }
        }
    }


    void maintainDBVariable() {
        try {
            levelP = 0;
            Statement stmt = dataSet.con.createStatement();
//        String query = "select "+name+" from "+Table+" where "+name+" is not null group by trim("+name+ ") order by trim(" + name +")";
            String query = "select " + name + " from " + dataSet.Table + " group by trim(" + name + ") order by trim(" + name + ")";
            System.out.println("Processing: " + name + " " + query);
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                if (rs.getString(1) != null) {
//            System.out.println("Level: "+(rs.getString(1)).trim());
                    levelA[levelP++] = (rs.getString(1)).trim();
                } else {
//            System.out.println("Level: NA");
                    levelA[levelP++] = "NA";
                }
            }
            permA = new int[levelP];
            IpermA = new int[levelP];
            for (int i = 0; i < levelP; i++) {
                permA[i] = i;
                IpermA[permA[i]] = i;
            }

            rs.close();
            stmt.close();
            levelsSet = true;
        } catch (Exception ex) {
            System.out.println("DB Exception in Maintain: " + ex);
        }
    }
}

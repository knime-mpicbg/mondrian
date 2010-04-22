package org.rosuda.mondrian.core;

import org.rosuda.mondrian.*;
import org.rosuda.mondrian.io.ScanException;
import org.rosuda.mondrian.io.UnacceptableFormatException;
import org.rosuda.mondrian.io.db.Query;
import org.rosuda.mondrian.util.Util;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DataSet {

    public Vector<Variable> data = new Vector<Variable>(256, 256);
    //  protected Vector name = new Vector(256,256);
    protected boolean[] alpha = {true};
    protected int[] NAcount = {0};
    protected double[] selectionArray;
    public byte[] colorArray;
    protected Color[] brushColors;
    public boolean colorBrush = false;
    protected double[] filterA;
    public int[] filterGrpSize;
    public int[] filterSelGrpSize;
    protected boolean groupsSet = false;
    protected boolean filterON = false;
    public int filterVar = -1;
    public int target;
    public double filterVal;
    public int filterGrp;
    public int n = 0;
    public int k = 0;
    public boolean hasMissings = false;
    public boolean isDB;
    public String setName;
    public Connection con;
    public String Table;
    public Query sqlConditions = new Query();
    public int graphicsPerf = 0;
    int counter;
    public boolean selChanged;
    private int[][] RGBs;


    public DataSet(String setName) {
        defineColors();
        this.isDB = false;
        this.setName = setName;
    }


    public DataSet(Connection con, String DB, String Table) {
        defineColors();
        this.isDB = true;
        this.setName = Table;
        this.con = con;
        this.Table = Table;

        try {
            Statement stmt = con.createStatement();
            String query = "show fields from " + Table + " from " + DB;
            ResultSet rs = stmt.executeQuery(query);

            this.k = 0;
            while (rs.next())
                this.k++;
            rs.close();
            stmt.close();

            stmt = con.createStatement();
            query = "show fields from " + Table + " from " + DB;
            rs = stmt.executeQuery(query);

            alpha = new boolean[k];
            String[] columnType = new String[k];
            for (int j = 0; j < k; j++) {
                if (rs.next()) {
                    String varName = rs.getString(1);
//          name.addElement(varName);
                    columnType[j] = rs.getString(2);
                    alpha[j] = columnType[j].startsWith("varchar") || columnType[j].startsWith("enum") || columnType[j].startsWith("char");

                    Variable Var = new Variable(this, alpha[j], varName);
                    if (!alpha[j])
                        Var.isCategorical = false;
                    data.addElement(Var);
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception ex) {
            System.out.println("DB Exception: get fields ... " + ex);
        }

        try {
            Statement stmt = con.createStatement();
            String query = "select count(*) from " + Table;
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                this.n = (int) Util.atod(rs.getString(1));
//            selectionArray = new double[this.n];
            }
            rs.close();
            stmt.close();
        } catch (Exception ex) {
            System.out.println("DB Exception: get size ... " + ex);
        }
    }


    public void defineColors() {
        RGBs = new int[11][];
/*    RGBs[1]  = new int[]{ 50, 106, 157};
    RGBs[2]  = new int[]{199, 106, 149};
    RGBs[3]  = new int[]{102, 154, 103};
    RGBs[4]  = new int[]{255, 122,   0};
    RGBs[5]  = new int[]{159,  64, 255};
    RGBs[6]  = new int[]{255,   0, 255};
    RGBs[7]  = new int[]{159, 255,  64};
    RGBs[8]  = new int[]{255, 210,   0};
    RGBs[9]  = new int[]{  0, 255, 255};
    RGBs[10] = new int[]{210,   0,   0};
    RGBs[11] = new int[]{  0, 255,   0};
    RGBs[12] = new int[]{  0,   0, 210};    */

// Color Brewer: 12, qualitative, Set3
        RGBs[1] = new int[]{128, 177, 211};
        RGBs[2] = new int[]{188, 128, 189};
        RGBs[3] = new int[]{179, 222, 105};
        RGBs[4] = new int[]{253, 180, 98};
        RGBs[5] = new int[]{252, 205, 229};
        RGBs[6] = new int[]{141, 211, 199};
        RGBs[7] = new int[]{251, 128, 114};
        RGBs[8] = new int[]{204, 235, 197};
        RGBs[9] = new int[]{255, 237, 111};
        RGBs[10] = new int[]{190, 186, 218};
    }


    public void addVariable(String name, boolean alpha, boolean categorical, double[] data, boolean[] miss) {

        if (this.n == 0) { // this DataSet was never initialized - use data length to do that
            this.n = data.length;
            selectionArray = new double[n];
            colorArray = new byte[n];
            for (int i = 0; i < n; i++)
                colorArray[i] = 0;
            filterA = new double[n];
        }
        Variable Var = new Variable(this, this.n, alpha, name);
        System.arraycopy(data, 0, Var.data, 0, data.length);
        System.arraycopy(miss, 0, Var.missing, 0, data.length);
        for (int i = 0; i < this.n; i++)
            if (miss[i]) {
                Var.numMiss++;
//        System.out.println(" Here is a missing at: "+i);
            }
        Var.forceCategorical = false;
        Var.isCategorical = categorical;
        this.alpha = (boolean[]) Util.resizeArray(this.alpha, ++this.k);
        this.NAcount = (int[]) Util.resizeArray(this.NAcount, this.k);
        NAcount[NAcount.length - 1] = Var.numMiss;
        this.alpha[k - 1] = alpha;
        if (Var.isCategorical) {
            for (int l = 0; l < Var.grpSize.length; l++)
                Var.grpSize[l] = 0;
            Var.forceCategorical = true;
            Var.isCategorical = true;
            for (int j = 0; j < this.n; j++)
                Var.isLevel(Double.toString(Var.data[j]));
            Var.sortLevels();
        } else
            Var.sortData();
        this.data.addElement(Var);
    }


    public boolean[] sniff(BufferedReader br) {

        String line, dummy;

        try {
            line = br.readLine();

            StringTokenizer head = new StringTokenizer(line, "\t");

            k = head.countTokens();

            alpha = new boolean[this.k];

            for (int j = 0; j < this.k; j++)
                alpha[j] = false;

            try {
                while (((line = br.readLine()) != null) && !((line.trim()).equals(""))) {
                    n++;
                    StringTokenizer dataLine = new StringTokenizer(line, "\t");
                    for (int j = 0; j < this.k; j++) {
                        if (!alpha[j]) {
                            alpha[j] = false;
                            try {
                                dummy = dataLine.nextToken();
                                Float fdummy = Float.valueOf(dummy);
                            }
                            catch (NumberFormatException e) {
                                alpha[j] = true;
                            }
                        } else
                            dummy = dataLine.nextToken();
                    }
                }
            }
            catch (NoSuchElementException ignored) {
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
            System.exit(1);
        }

        selectionArray = new double[n];
        colorArray = new byte[n];
        for (int i = 0; i < n; i++)
            colorArray[i] = 0;
        filterA = new double[n];

        return alpha;
    }


    public void read(BufferedReader br, boolean[] alpha, JProgressBar progBar) {

        Variable Var;
        String line, varName;

        try {
            line = br.readLine();

            StringTokenizer head = new StringTokenizer(line, "\t");

            for (int j = 0; j < this.k; j++) {
                varName = head.nextToken();
//        name.addElement(varName);
//System.out.println("adding Variable: "+varName);
                Var = new Variable(this, this.n, alpha[j], varName);
                if (varName.length() > 1) {
                    if (varName.substring(0, 2).equals("/T"))
                        Var.phoneNumber = true;
                    if (varName.substring(0, 2).equals("/P")) {
                        Var.isPolyID = true;
                        Var.forceCategorical = true;
                    }
                    if (varName.substring(0, 2).equals("/C"))
                        Var.isCategorical = false;
                    if (varName.substring(0, 2).equals("/D"))
                        Var.forceCategorical = true;
                    if (varName.substring(0, 2).equals("/U")) {
                        Matcher m = Pattern.compile("/U(.*)<([^>]+)>(.*)").matcher(varName);
                        if (m.matches()) {
                            Var.forceCategorical = true;
                            Var.name = m.group(2);
                            Util.registerHTMLTemplate(Var.name, m.group(1) + "$var" + m.group(3));
                        } else {
                            System.err.println("Unknown Url for column: " + varName);
                            Var.forceCategorical = true;
                        }
                    }

                }
                data.addElement(Var);
            }

            try {
                //progBar.setIndeterminate(true);  //
                progBar.setValue(0);
                for (int i = 0; i < this.n; i++) {
                    if ((i % Math.max(n / 20, 1) == 0) && (n > 1000)) {
                        progBar.setValue(i);
                        progBar.repaint();
//System.out.println("Reading Line: "+i);
                    }
                    line = br.readLine();
                    StringTokenizer dataLine = new StringTokenizer(line, "\t");
                    for (int j = 0; j < this.k; j++) {
                        Var = data.elementAt(j);
                        if (alpha[j])
                            Var.data[i] = Var.isLevel(dataLine.nextToken().trim());
                        else {
                            //    token = dataLine.nextToken();
                            //    if( token.equals(last[j]) )
                            //      Var.data[i] = Var.data[i-1];
                            //    else {
                            //    Var.data[i] = Double.valueOf(token).doubleValue();
                            Var.data[i] = Double.valueOf(dataLine.nextToken());
                            Var.isLevel(Double.toString(Var.data[i]));
                            //    }
                            //    last[j] = token;
                        }
                    }
                }
            }
            catch (NoSuchElementException ignored) {
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
            System.exit(1);
        }
        for (int i = 0; i < k; i++) {
            Var = data.elementAt(i);
            if (Var.isCategorical)
                Var.sortLevels();
            else if (!Var.alpha)
                Var.sortData();
            Var.shrink();
//System.out.println("Shrinking Arrays to: "+ Var.getNumLevels()+"  Memory: "+Runtime.getRuntime().freeMemory());
        }
        Runtime.getRuntime().gc();
    }


    public String turboRead(String fileName, MonFrame joint) {
        try {
            BufferTokenizer BT = new BufferTokenizer(10, 5, fileName, joint);
            this.n = BT.lines;
            this.k = BT.columns;

            NAcount = new int[this.k];
            alpha = new boolean[this.k];
            selectionArray = new double[n];
            colorArray = new byte[n];
            for (int i = 0; i < n; i++)
                colorArray[i] = 0;
            filterA = new double[n];

            for (int j = 0; j < k; j++) {
                NAcount[j] = BT.NACount[j];
                alpha[j] = !BT.numericalColumn[j];

                if (BT.isPhoneNum[j])
                    System.out.println("Var No: " + j + " is a phone number");

                Variable Var = new Variable(this, BT, j);
                Var.numMiss = NAcount[j];
                if (Var.numMiss > 0)
                    hasMissings = true;
                String varName = Var.getName();
                if (BT.polygonID == j) {
                    Var.isPolyID = true;
                    System.out.println("varName: " + varName + " polygonName: " + BT.polygonName);
                }
                data.addElement(Var);
            }
            if (BT.isPolygonAvailable) {
                System.out.println(" Has Polygon: " + BT.polygonName + "<-");
                return BT.polygonName;
            } else
                return "";
        }
        catch (ScanException e) {
            return "ERROR" + e.getMessage();
        }
        catch (UnacceptableFormatException ignored) {
        }
        return "";
    }


    public void numToCat(int i) {
        Variable Var = data.elementAt(i);

        if (!alpha[i] || !Var.isCategorical) {
            for (int l = 0; l < Var.grpSize.length; l++)
                Var.grpSize[l] = 0;
            Var.forceCategorical = true;
            Var.isCategorical = true;
            for (int j = 0; j < this.n; j++)
                Var.isLevel(Double.toString(Var.data[j]));
            Var.sortLevels();
        }
    }


    public void catToNum(int i) {
        Variable Var = data.elementAt(i);
        if (!alpha[i] || Var.isCategorical) {
            Var.forceCategorical = false;
            Var.isCategorical = false;
            Var.sortData();
        }
    }


    public Table discretize(String name, int dvar, double start, double width, int weight) {

        int tablelength = (int) Stat.round((this.getMax(dvar) - start) / width, 8) + 1;
        int[] vars = new int[1];
        vars[0] = dvar;
        double[] bdtable = new double[tablelength];    // !
        int[] tableDim = new int[tablelength];    // !
        String[][] lnames = new String[1][tablelength];    // !
        double[] datacopy = this.getRawNumbers(dvar);
        int[] sorts = this.getSort(dvar);
        int[] varlevels = new int[1];            // !
        varlevels[0] = tablelength;
        String[] varnames = new String[1];        // !
        varnames[0] = this.getName(dvar);
        int[][] Ids = new int[tablelength][];    // !
        int[] pointers = new int[tablelength];
        int round = (int) Math.max(0, 3 - Math.round((Math.log(getMax(dvar) - getMin(dvar)) / Math.log(10))));
        Query initialQuery = new Query();

        if (isDB) {
            try {
                Statement stmt = con.createStatement();
                Query query = new Query();
                String itemStr = "CASE ";
                String filler = "                                                                                          ";
                itemStr += "WHEN " + getName(dvar) + "<" + Stat.roundToString(start, round) + " THEN '[" + filler.substring(0, tablelength) + Stat.roundToString(start, round) + ", " + Stat.roundToString(start + 1 * width, round) + ")' ";
                for (int i = 0; i < tablelength; i++) {
                    itemStr += "WHEN " + getName(dvar) + ">=" + Stat.roundToString(start + i * width, round) + " AND " + getName(dvar) + "<" + Stat.roundToString(start + (i + 1) * width, round) + " THEN '[" + filler.substring(0, tablelength - i) + Stat.roundToString(start + i * width, round) + ", " + Stat.roundToString(start + (i + 1) * width, round) + ")' ";
                }
                itemStr += "ELSE '[" + Stat.roundToString(start + tablelength * width, round) + ", " + Stat.roundToString(start + (tablelength + 1) * width, round) + ")' END AS category42";

//System.out.println(itemStr); 
                query.addItem(itemStr);
                query.addTable(setName);
                query.addCondition("AND", getName(dvar) + " IS NOT NULL ");
                query.addGroup("category42");
                query.addOrder("category42");
                System.out.print(" Initial setup:");
                query.print();

                ResultSet rs = stmt.executeQuery(query.makeQuery());
                int i = 0;
                while (rs.next()) {
                    System.out.println(" i: " + i + " String:" + rs.getString(1).trim() + " Value: " + rs.getInt(2));
                    lnames[0][i] = rs.getString(1).trim();
                    String tmp = lnames[0][i].substring(1, lnames[0][i].length()).trim();
                    lnames[0][i] = "[" + tmp.substring(0, tmp.indexOf(',')) + ", " + tmp.substring(tmp.indexOf(',') + 1, tmp.length()).trim();
                    bdtable[i] = rs.getInt(2);
                    i++;
                }
                rs.close();
                stmt.close();
                initialQuery = query;
            } catch (Exception ex) {
                System.out.println("DB Exception: get histo breakdown ... " + ex);
            }
        } else {
            if (weight == -1)
                for (int i = 0; i < getN(dvar); i++) {
                    int index = (int) ((float) ((datacopy[sorts[i]] - start) / width));
//System.out.println("value: "+datacopy[sorts[i]]+"  index: "+(datacopy[sorts[i]]-start)/width+"  INDEX: "+index+" Maxindex: "+tablelength);

                    bdtable[index]++;
                    tableDim[index]++;
                }
            else {
                double[] weights = getRawNumbers(weight);
                boolean[] miss = getMissings(weight);

                for (int i = 0; i < getN(dvar); i++) {
                    int index = (int) ((datacopy[sorts[i]] - start) / width);
                    if (!miss[sorts[i]])
                        bdtable[index] += weights[sorts[i]];
                    tableDim[index]++;
                }
            }
        }

        for (int i = 0; i < tablelength; i++) {
            if (!isDB) {
                lnames[0][i] = "[" + Stat.roundToString(start + i * width, round) + ", " + Stat.roundToString(start + (i + 1) * width, round) + ")";
                Ids[i] = new int[tableDim[i]];
            } else
                Ids[i] = new int[1];
            pointers[i] = 0;
        }

        int index;

        if (!isDB) {
            for (int i = 0; i < getN(dvar); i++) {
                index = (int) ((float) ((datacopy[sorts[i]] - start) / width));
                Ids[index][pointers[index]++] = sorts[i];
            }
        } else
            for (int i = 0; i < tablelength; i++)
                Ids[i][0] = i;

        Table tmpTable = new Table(name, bdtable, 1, varlevels, varnames, lnames, vars, Ids, this, weight);
        tmpTable.initialQuery = initialQuery;
        return (tmpTable);
    }


    public Table discretize2D(String name, int xVar, double xStart, double xEnd, int nX, int yVar, double yStart, double yEnd, int nY) {

        double xWidth = (xEnd - xStart) / nX;
        double yWidth = (yEnd - yStart) / nY;
//System.out.println("x: "+x_num+"y: "+y_num);
        int tablelength = nX * nY;
        int[] vars = new int[2];
        vars[0] = xVar;
        vars[1] = yVar;
        double[] bdtable = new double[tablelength];    // !
        String[][] lnames = new String[2][];    // !
        lnames[0] = new String[nX];
        lnames[1] = new String[nY];
        double[] datacopyX = this.getRawNumbers(xVar);
        double[] datacopyY = this.getRawNumbers(yVar);
        int[] varlevels = new int[2];            // !
        varlevels[0] = nX;
        varlevels[1] = nY;
        String[] varnames = new String[2];        // !
        varnames[0] = this.getName(xVar);
        varnames[1] = this.getName(yVar);
        int[][] Ids = new int[tablelength][];    // !
        int[] pointers = new int[tablelength];
        int roundX = (int) Math.max(0, 3 - Math.round((Math.log(xEnd - xStart) / Math.log(10))));
        int roundY = (int) Math.max(0, 3 - Math.round((Math.log(yEnd - yStart) / Math.log(10))));
        Query initialQuery = new Query();

        if (isDB) {
            System.out.println("DB not yet implemented");
        } else {
            for (int i = 0; i < this.n; i++)
                if (datacopyX[i] < xEnd && datacopyX[i] >= xStart && datacopyY[i] < yEnd && datacopyY[i] >= yStart)
                    bdtable[(int) ((datacopyX[i] - xStart) / xWidth) * nY + (int) ((datacopyY[i] - yStart) / yWidth)]++;
        }

        for (int i = 0; i < nX; i++) {
            if (!isDB) {
                lnames[0][i] = "[" + Stat.roundToString(xStart + i * xWidth, roundX) + ", " + Stat.roundToString(xStart + (i + 1) * xWidth, roundX) + ")";
            }
        }
        for (int i = 0; i < nY; i++) {
            if (!isDB) {
                lnames[1][i] = "[" + Stat.roundToString(yStart + i * yWidth, roundY) + ", " + Stat.roundToString(yStart + (i + 1) * yWidth, roundY) + ")";
            }
        }
        for (int i = 0; i < tablelength; i++) {
            if (!isDB) {
                Ids[i] = new int[(int) bdtable[i]];
            } else
                Ids[i] = new int[1];
            pointers[i] = 0;
        }

        int index;
        if (!isDB)
            for (int i = 0; i < this.n; i++) {
                if (datacopyX[i] < xEnd && datacopyX[i] >= xStart && datacopyY[i] < yEnd && datacopyY[i] >= yStart) {
                    index = (int) ((datacopyX[i] - xStart) / xWidth) * nY + (int) ((datacopyY[i] - yStart) / yWidth);
                    Ids[index][pointers[index]++] = i;
                }
            }
        else
            for (int i = 0; i < tablelength; i++)
                Ids[i][0] = i;

        Table tmpTable = new Table(name, bdtable, 2, varlevels, varnames, lnames, vars, Ids, this, -1);
        tmpTable.initialQuery = initialQuery;
        return (tmpTable);
    }


    public Table breakDown(String name, int[] vars, int count) {

        int tablelength = 1;
        int index;
        double[] bdtable;
        String[][] lnames = new String[vars.length][];
        double[][] datacopy = new double[vars.length][];
        int[] varlevels = new int[vars.length];
        String[] varnames = new String[vars.length];
        int[] plevels = new int[vars.length];
        int[][] Ids;
        int[] dimA;
        Query newQ = new Query();

        for (int j = 0; j < vars.length; j++) {
            varlevels[j] = getNumLevels(vars[j]);
            tablelength *= varlevels[j];
            varnames[j] = this.getName(vars[j]);
            lnames[j] = this.getLevels(vars[j]);
            if (isDB) {
                newQ.addItem(varnames[j]);
                newQ.addGroup(varnames[j]);
                newQ.addOrder(varnames[j]);
            } else
                datacopy[j] = this.getNumbers(vars[j]);
        }
        plevels[vars.length - 1] = 1;
        for (int j = vars.length - 2; j >= 0; j--) {
            plevels[j] = varlevels[j + 1] * plevels[j + 1];
        }
//        for (int j = 0; j < vars.length; j++) {
//          System.out.println("Tablelength: "+tablelength+"  Name: "+ varnames[j]+"  Levels: "+lnames[j][0]+"..."+"   Plevels: "+plevels[j]);
//        }
        Ids = new int[tablelength][];
        dimA = new int[tablelength];

        bdtable = new double[tablelength];

        if (isDB) {
            try {
                newQ.addTable(Table);
                System.out.println(newQ.makeQuery());
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(newQ.makeQuery());

                while (rs.next()) {
                    index = 0;
                    for (int j = 0; j < vars.length; j++) {
                        String LString = rs.getString(j + 1);
                        if (LString == null)
                            LString = "NA";
                        index += plevels[j] * data.elementAt(vars[j]).Level((LString).trim());
                    }
                    bdtable[index] = rs.getInt(vars.length + 1);
                }
                rs.close();
                stmt.close();
            } catch (Exception ex) {
                System.out.println("DB Exception: " + ex);
            }
            for (int j = 0; j < tablelength; j++) {
                Ids[j] = new int[1];
                Ids[j][0] = j;
            }
        }    // no DB
        else {
            for (int i = 0; i < this.n; i++) {
                index = 0;
                for (int j = 0; j < vars.length; j++)
                    index += plevels[j] * datacopy[j][i];

//System.out.println("Index: "+index);

                if (count == -1)
                    bdtable[index]++;
                else {
                    dimA[index]++;
                    if (!(this.getMissings(count))[i])
                        bdtable[index] += (this.getRawNumbers(count))[i];
                }
            }

            for (int j = 0; j < tablelength; j++) {
                if (count == -1)
                    Ids[j] = new int[(int) bdtable[j]];
                else
                    Ids[j] = new int[dimA[j]];
                if (Ids[j].length > 0)
                    Ids[j][0] = -1;
            }

            int[] topIndex = new int[tablelength];
            for (int j = 0; j < tablelength; j++)
                topIndex[j] = 0;

            for (int i = 0; i < this.n; i++) {
                index = 0;
                for (int j = 0; j < vars.length; j++)
                    index += plevels[j] * datacopy[j][i];
                Ids[index][topIndex[index]++] = i;
            }
        }
//System.out.println("Tablelength: "+tablelength+"  Name: "+ varnames[0]+"  Levels: "+bdtable[0]+"... "+bdtable[1]+"... "+bdtable[2]+"... "+bdtable[3]+"... "+"   Plevels: "+plevels[0]);
        Table tmpTable = new Table(name, bdtable, vars.length, varlevels, varnames, lnames, vars, Ids, this, count);
        tmpTable.initialQuery = newQ;
        return (tmpTable);
    }


    public double[] regress(int k, int l, boolean excludeSel) {

        double Sxx, Sxy, a, b, r2;
        double sumx = 0;
        double sumy = 0;
        double sumxx = 0;
        double sumxy = 0;
        double sumyy = 0;
        int count = 0;

        double[] x = this.getRawNumbers(k);
        double[] y = this.getRawNumbers(l);

        for (int i = 0; i < this.n; i++) {
            if (x[i] < Double.MAX_VALUE && y[i] < Double.MAX_VALUE)
                if (!(selectionArray[i] > 0 && excludeSel)) {
                    count++;
                    sumx += x[i];
                    sumy += y[i];
                    sumxx += x[i] * x[i];
                    sumyy += y[i] * y[i];
                    sumxy += x[i] * y[i];
                }
        }
        Sxx = sumxx - sumx * sumx / count;
        Sxy = sumxy - sumx * sumy / count;
        b = Sxy / Sxx;
        a = (sumy - b * sumx) / count;
        r2 = b * (count * sumxy - sumx * sumy) / (count * sumyy - sumy * sumy);

        return new double[]{a, b, r2};
    }


    public double[] selRegress(int k, int l) {

        if (this.countSelection() < 2)
            return new double[]{0, 0, 0};

        double Sxx, Sxy, a, b, r2;
        double sumx = 0;
        double sumy = 0;
        double sumxx = 0;
        double sumxy = 0;
        double sumyy = 0;
        int count = 0;

        double[] x = this.getRawNumbers(k);
        double[] y = this.getRawNumbers(l);

        for (int i = 0; i < this.n; i++) {
            if (selectionArray[i] > 0 && x[i] < Double.MAX_VALUE && y[i] < Double.MAX_VALUE) {
                count++;
                sumx += x[i];
                sumy += y[i];
                sumxx += x[i] * x[i];
                sumyy += y[i] * y[i];
                sumxy += x[i] * y[i];
            }
        }

        Sxx = sumxx - sumx * sumx / count;
        Sxy = sumxy - sumx * sumy / count;
        b = Sxy / Sxx;
        a = (sumy - b * sumx) / count;
        r2 = b * (count * sumxy - sumx * sumy) / (count * sumyy - sumy * sumy);

        return new double[]{a, b, r2};
    }


    public boolean alpha(int i) {
        return alpha[i];
    }


    public boolean categorical(int i) {
        return data.elementAt(i).isCategorical;
    }


    public boolean phoneNumber(int i) {
        return data.elementAt(i).phoneNumber;
    }


    public boolean isPolyID(int i) {
        return data.elementAt(i).isPolyID();
    }


    public String getName(int i) {
        return data.elementAt(i).getName();
    }


    public int getNumLevels(int i) {
        return data.elementAt(i).getNumLevels();
    }


    public String[] getLevels(int i) {
        return data.elementAt(i).getLevels();
    }


    public String getLevelName(int i, double val) {
        Variable v = data.elementAt(i);
        String[] LevelString = v.getLevels();
        if (alpha[i])
            return LevelString[(int) val];
        else
            return LevelString[v.IpermA[(int) v.Level(Double.toString(val))]];
    }


    public double[] getNumbers(int i) {
        Variable v = data.elementAt(i);
        if (!v.isCategorical)
            return v.data;
        else {
            if (v.IpermA == null)
                v.sortLevels();
            double[] retA = new double[this.n];
            if (alpha[i]) {
                for (int j = 0; j < this.n; j++)
                    retA[j] = (double) v.IpermA[(int) v.data[j]];
            } else {
                for (int j = 0; j < this.n; j++) {
                    retA[j] = (double) v.IpermA[(int) v.Level(Double.toString(v.data[j]))];
                }
            }
            return retA;
        }
    }


    public double[] getRawNumbers(int i) {
        Variable v = data.elementAt(i);
        return v.data;
    }


    public boolean[] getMissings(int i) {
        Variable v = data.elementAt(i);
        return v.missing;
    }


    public int getNumMissings(int i) {
        return NAcount[i];
    }


    public int[] getSort(int i) {
        Variable v = data.elementAt(i);
        return v.sortI;
    }


    public int[] getRank(int i) {
        Variable v = data.elementAt(i);
        int[] ranks = new int[this.n];
        if (!categorical(i))
            for (int j = 0; j < this.n; j++)
                ranks[v.sortI[j]] = j;
        else
            for (int j = 0; j < this.n; j++)
                ranks[j] = v.permA[(int) v.data[j]];
        return ranks;
    }


    public int getN(int i) {
        return this.n - NAcount[i];
    }


    public double getMin(int i) {
        return data.elementAt(i).Min();
    }


    public double getSelMin(int i) {
        return data.elementAt(i).SelMin();
    }


    public double getMax(int i) {
        return data.elementAt(i).Max();
    }


    public double getSelMax(int i) {
        return data.elementAt(i).SelMax();
    }


    public double getMean(int i) {
        return data.elementAt(i).Mean();
    }


    public double getSelMean(int i) {
        return data.elementAt(i).selMean();
    }


    public double getSDev(int i) {
        return data.elementAt(i).SDev();
    }


    public double getSelSDev(int i) {
        return data.elementAt(i).selSDev();
    }


    public Color getColor(int i) {
        return brushColors[colorArray[i]];
    }


    public Color getColorByID(int id) {
        return brushColors[id];
    }


    public int getNumColors() {
        return brushColors.length;
    }


    public void setColor(int i, int c) {
        colorArray[i] = (byte) c;
    }


    public void setColors(int k, int mode) {
        if (k < 256) {
            colorBrush = true;
            brushColors = new Color[k + 1];
//      System.out.println("Setting "+k+" Colors");
            brushColors[0] = MFrame.objectColor;
        } else
            return;

        switch (mode) {
            case 0:
                // Linear Colors
                for (int i = 1; i <= k; i++) {
                    brushColors[i] = Color.getHSBColor((float) i / (float) k * 1.0F, 0.5F, 1.0F);
//          System.out.println("Color: "+brushColors[i]);
                }
                break;
            case 1:
                // Fixed (linear) Colors
                int j = 1;
                double step = 0.0;
                double offset = 0.0;
                for (int r = 0; r <= Math.log(k) / Math.log(2); r++) {
                    step = 1.0 / Math.pow(2, r);
                    offset = step / 2;
                    for (int s = 0; s < Math.pow(2, r); s++) {
//            System.out.println("Power: "+ r + " - "+ s + " Position: "+(offset+s*step));
                        if (j < k)
                            brushColors[j++] = Color.getHSBColor((float) (offset + s * step), 0.5F, 1.0F);
                        else
                            return;
                    }
                }
                break;
            case 2:
                // Static Colors
                int nCol = RGBs.length - 1;
                for (int i = 0; i < k; i++)
                    brushColors[i + 1] = new Color(RGBs[(i % nCol) + 1][0], RGBs[(i % nCol) + 1][1], RGBs[(i % nCol) + 1][2]);
                break;
        }
    }


    public int addColor(int c) {
        Color newColor = new Color(RGBs[c][0], RGBs[c][1], RGBs[c][2]);
        if (!colorBrush) {
            colorBrush = true;
            brushColors = new Color[2];
            brushColors[0] = MFrame.objectColor;
            brushColors[1] = newColor;
        } else {
            for (int i = 0; i < brushColors.length; i++)
                if (brushColors[i].equals(newColor))
                    return i;
            brushColors = (Color[]) Util.resizeArray(brushColors, brushColors.length + 1);
            brushColors[brushColors.length - 1] = newColor;
        }
        return brushColors.length - 1;
    }


    public void colorsOff() {
        colorBrush = false;
        brushColors = null;
        for (int i = 0; i < n; i++)
            colorArray[i] = 0;
    }


    public double[] getSelection() {
        return selectionArray;
    }


    public double getSelected(int i) {
        return selectionArray[i];
    }


    public void setSelection(int i, double s, int mode) {
        if (filterON && filterA[i] != filterVal)
            s = 0;
//      if( s == 0 )
//        selectionArray[i] = 0;
//      else
//        return;
//    else {	
//    if( (filterVar != -1 && filterA[i] == filterVal) || filterVar == -1 ) {
        selChanged = true;
        switch (mode) {
            case Selection.MODE_STANDARD:
                selectionArray[i] = s;
//System.out.println("REPLACE at: "+i+" with: "+s);
                break;
            case Selection.MODE_AND:
//System.out.print("AND at: "+i+" from: "+selectionArray[i]);
                selectionArray[i] *= s;
//System.out.println(" to: "+selectionArray[i]);
                break;
            case Selection.MODE_OR:
                selectionArray[i] = Math.max(s, selectionArray[i]);
                break;
            case Selection.MODE_XOR:
                if (s > 0)
                    if (selectionArray[i] > 0)
                        selectionArray[i] = 0;
                    else
                        selectionArray[i] = s;
                break;
            case Selection.MODE_NOT:
                if (s > 0)
                    selectionArray[i] = 0;
//      }
        }
    }


    public void setFilter(String grp) {

//System.out.println(" filterVar: "+filterVar+" Grp: "+grp+ " <-- "+ filterGrp +" --> "+filterVal); 
        filterON = true;
//    if( Util.isNumber(grp) )           // Make sure that numeric values get the representation of a Java numeric (123 -> 123.0)!!
        if (!alpha(filterVar) && Util.isNumber(grp))           // Make sure that numeric values get the representation of a Java numeric (123 -> 123.0)!!
            filterGrp = (int) (((Variable) data.elementAt(filterVar)).Level(Double.toString(Double.valueOf(grp).doubleValue())));
        else
            filterGrp = (int) (((Variable) data.elementAt(filterVar)).Level(grp));
        if ((data.elementAt(filterVar).alpha))
            filterVal = filterGrp;
        else if (!grp.equals("NA"))
            filterVal = Util.atod(grp);
        else {
            filterVal = Util.atod(Double.MAX_VALUE + "");
            filterGrp = (int) (((Variable) data.elementAt(filterVar)).Level(Double.MAX_VALUE + ""));
        }
    }


    public void updateFilter() {
        if (filterVar == -1)
            return;
        for (int i = 0; i < filterGrpSize.length; i++) {
            filterGrpSize[i] = 0;
            filterSelGrpSize[i] = 0;
        }
        if ((data.elementAt(filterVar).alpha)) {
            for (int i = 0; i < n; i++)
                if (!getMissings(target)[i]) {
                    int index = (int) (((Variable) data.elementAt(filterVar)).data[i]);
                    filterGrpSize[index]++;
                    if (selectionArray[i] > 0)
                        filterSelGrpSize[index]++;
                }
        } else
            for (int i = 0; i < n; i++)
                if (!getMissings(target)[i]) {
                    int index = (int) (((Variable) data.elementAt(filterVar)).Level("" + (((Variable) data.elementAt(filterVar)).data)[i]));
                    filterGrpSize[index]++;
                    if (selectionArray[i] > 0)
                        filterSelGrpSize[index]++;
                }
//    for( int i=0; i<filterGrpSize.length; i++ )
//      System.out.println("i: "+i+" GrpSize: "+filterGrpSize[i]+" Selected: "+filterSelGrpSize[i]+" unsort: "+(((Variable)data.elementAt(filterVar)).grpSize)[i]);
    }


    public void defineFilter(int var, int target) {

        filterVar = var;
        this.target = target;
        filterON = true;
        for (int i = 0; i < this.n; i++) {
            filterA[i] = (data.elementAt(filterVar).data)[i];
        }
        filterGrpSize = new int[data.elementAt(var).getNumLevels()];
        filterSelGrpSize = new int[data.elementAt(var).getNumLevels()];
//    for( int i=0; i<filterGrpSize.length; i++ ) 
//      filterGrpSize[i] = (((Variable)data.elementAt(var)).grpSize)[i];
        updateFilter();
        filterON = false;
    }


    public void resetFilter() {
        filterON = false;
    }


    public void filterOff() {
        filterON = false;
        filterVar = -1;
        filterVal = 0;
        filterGrp = -1;
    }


    public void selectAll() {
        for (int i = 0; i < this.n; i++)
            setSelection(i, 1, Selection.MODE_STANDARD);
    }


    public void toggleSelection() {
        for (int i = 0; i < this.n; i++)
//      setSelection(i, 1, Selection.MODE_STANDARD );
            setSelection(i, 1, Selection.MODE_XOR);
    }


    public int countSelection() {
        if (this.isDB) {
            if (sqlConditions.getConditions().equals(""))
                return 0;
            else {
                try {
                    Statement stmt = con.createStatement();
                    String query = "SELECT COUNT(*) FROM " + setName + " WHERE " + sqlConditions.getConditions();
                    ResultSet rs = stmt.executeQuery(query);
                    rs.next();
                    int returner = rs.getInt(1);
                    rs.close();
                    stmt.close();
                    return returner;
                } catch (Exception ex) {
                    System.out.println("DB Exception: get num hilited ... " + ex);
                }
                return 0;
            }
        } else {
            if (selChanged) {
                counter = 0;
                for (int i = 0; i < this.n; i++)
                    if (selectionArray[i] > 0)
                        counter++;
                selChanged = false;
                return counter;
            } else {
                return counter;
            }
        }
    }


    public int countSelection(int j) {
        Variable v = data.elementAt(j);
        counter = 0;
        for (int i = 0; i < getN(j); i++)
            if (selectionArray[v.sortI[i]] > 0)
                counter++;
        return counter;
    }


    public void clearSelection() {
        selectionArray = new double[n];
    }


    public double getQuantile(int i, double q) {
        return data.elementAt(i).getQuantile(q);
    }


    public double getSelQuantile(int i, double q) {
        return data.elementAt(i).getSelQuantile(i, q);
    }


    public double getFirstGreater(int i, double q) {
        return data.elementAt(i).getFirstGreater(q);
    }


    public double getFirstSelGreater(int i, double q) {
        return data.elementAt(i).getFirstSelGreater(q);
    }


    public double getFirstSmaller(int i, double q) {
        return data.elementAt(i).getFirstSmaller(q);
    }


    public double getFirstSelSmaller(int i, double q) {
        return data.elementAt(i).getFirstSelSmaller(q);
    }


    public double[] getAllSmaller(int i, double q) {
        return data.elementAt(i).getAllSmaller(q);
    }


    public double[] getAllSelSmaller(int i, double q) {
        return data.elementAt(i).getAllSelSmaller(q);
    }


    public double[] getAllGreater(int i, double q) {
        return data.elementAt(i).getAllGreater(q);
    }


    public double[] getAllSelGreater(int i, double q) {
        return data.elementAt(i).getAllSelGreater(q);
    }


}

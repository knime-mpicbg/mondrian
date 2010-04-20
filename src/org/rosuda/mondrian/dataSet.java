package org.rosuda.mondrian;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Exception;
import java.lang.Float;
import java.lang.NumberFormatException;
import java.lang.String;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class dataSet {

    protected Vector data = new Vector(256, 256);
    //  protected Vector name = new Vector(256,256);
    protected boolean[] alpha = {true};
    protected int[] NAcount = {0};
    protected double[] selectionArray;
    public byte[] colorArray;
    protected Color[] brushColors;
    public boolean colorBrush = false;
    protected double[] filterA;
    protected int[] filterGrpSize;
    protected int[] filterSelGrpSize;
    protected boolean groupsSet = false;
    protected boolean filterON = false;
    public int filterVar = -1;
    public int target;
    public double filterVal;
    public int filterGrp;
    private String[] columnType = {""};
    public int n = 0;
    public int k = 0;
    public boolean hasMissings = false;
    public boolean isDB;
    public String setName;
    private Driver d;
    public Connection con;
    private String DB;
    private String Table;
    public Query sqlConditions = new Query();
    public int graphicsPerf = 0;
    int counter;
    boolean selChanged;
    private int[][] RGBs;


    public dataSet(String setName) {
        defineColors();
        this.isDB = false;
        this.setName = setName;
    }


    public dataSet(Driver d, Connection con, String DB, String Table) {
        defineColors();
        this.isDB = true;
        this.setName = Table;
        this.d = d;
        this.con = con;
        this.DB = DB;
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
            columnType = new String[k];
            for (int j = 0; j < k; j++) {
                if (rs.next()) {
                    String varName = rs.getString(1);
//          name.addElement(varName);
                    columnType[j] = rs.getString(2);
                    if (columnType[j].startsWith("varchar") || columnType[j].startsWith("enum") || columnType[j].startsWith("char")) {
                        alpha[j] = true;
                    } else
                        alpha[j] = false;
                    Variable Var = new Variable(alpha[j], varName);
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

        if (this.n == 0) { // this dataSet was never initialized - use data length to do that
            this.n = data.length;
            selectionArray = new double[n];
            colorArray = new byte[n];
            for (int i = 0; i < n; i++)
                colorArray[i] = 0;
            filterA = new double[n];
        }
        Variable Var = new Variable(this.n, alpha, name);
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

        String line, dummy = "";

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
            catch (NoSuchElementException e) {
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
        String[] last = new String[k];

        try {
            line = br.readLine();

            StringTokenizer head = new StringTokenizer(line, "\t");

            for (int j = 0; j < this.k; j++) {
                varName = head.nextToken();
//        name.addElement(varName);
//System.out.println("adding Variable: "+varName);
                Var = new Variable(this.n, alpha[j], varName);
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
                double x;
                String token;
//progBar.setIndeterminate(true);  //
                progBar.setValue(0);
                for (int i = 0; i < this.n; i++) {
                    if ((i % (int) (Math.max(n / 20, 1)) == 0) && (n > 1000)) {
                        progBar.setValue(i);
                        progBar.repaint();
//System.out.println("Reading Line: "+i);
                    }
                    line = br.readLine();
                    StringTokenizer dataLine = new StringTokenizer(line, "\t");
                    for (int j = 0; j < this.k; j++) {
                        Var = (Variable) data.elementAt(j);
                        if (alpha[j])
                            Var.data[i] = Var.isLevel(dataLine.nextToken().trim());
                        else {
                            //    token = dataLine.nextToken();
                            //    if( token.equals(last[j]) )
                            //      Var.data[i] = Var.data[i-1];
                            //    else {
                            //    Var.data[i] = Double.valueOf(token).doubleValue();
                            Var.data[i] = Double.valueOf(dataLine.nextToken()).doubleValue();
                            Var.isLevel(Double.toString(Var.data[i]));
                            //    }
                            //    last[j] = token;
                        }
                    }
                }
            }
            catch (NoSuchElementException e) {
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
            System.exit(1);
        }
        for (int i = 0; i < k; i++) {
            Var = (Variable) data.elementAt(i);
            if (Var.isCategorical)
                Var.sortLevels();
            else if (!Var.alpha)
                Var.sortData();
            Var.shrink();
//System.out.println("Shrinking Arrays to: "+ Var.getNumLevels()+"  Memory: "+Runtime.getRuntime().freeMemory());
        }
        Runtime.getRuntime().gc();
    }


    public String turboRead(String fileName, Join joint) {
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

                Variable Var = new Variable(BT, j);
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
        catch (UnacceptableFormatException e) {
        }
        return "";
    }


    public void numToCat(int i) {
        Variable Var = ((Variable) data.elementAt(i));
        if (alpha[i] && Var.isCategorical)
            return;
        else {
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
        Variable Var = ((Variable) data.elementAt(i));
        if (alpha[i] && !Var.isCategorical)
            return;
        else {
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
                Ids[i] = new int[(int) tableDim[i]];
            } else
                Ids[i] = new int[1];
            pointers[i] = 0;
        }

        int index = 0;

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

        int x_num = nX;
        int y_num = nY;
        double xWidth = (xEnd - xStart) / nX;
        double yWidth = (yEnd - yStart) / nY;
//System.out.println("x: "+x_num+"y: "+y_num);
        int tablelength = x_num * y_num;
        int[] vars = new int[2];
        vars[0] = xVar;
        vars[1] = yVar;
        double[] bdtable = new double[tablelength];    // !
        String[][] lnames = new String[2][];    // !
        lnames[0] = new String[x_num];
        lnames[1] = new String[y_num];
        double[] datacopyX = this.getRawNumbers(xVar);
        double[] datacopyY = this.getRawNumbers(yVar);
        int[] varlevels = new int[2];            // !
        varlevels[0] = x_num;
        varlevels[1] = y_num;
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
                    bdtable[(int) ((datacopyX[i] - xStart) / xWidth) * y_num + (int) ((datacopyY[i] - yStart) / yWidth)]++;
        }

        for (int i = 0; i < x_num; i++) {
            if (!isDB) {
                lnames[0][i] = "[" + Stat.roundToString(xStart + i * xWidth, roundX) + ", " + Stat.roundToString(xStart + (i + 1) * xWidth, roundX) + ")";
            }
        }
        for (int i = 0; i < y_num; i++) {
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

        int index = 0;
        if (!isDB)
            for (int i = 0; i < this.n; i++) {
                if (datacopyX[i] < xEnd && datacopyX[i] >= xStart && datacopyY[i] < yEnd && datacopyY[i] >= yStart) {
                    index = (int) ((datacopyX[i] - xStart) / xWidth) * y_num + (int) ((datacopyY[i] - yStart) / yWidth);
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
        String query = "";
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
        for (int j = 0; j < vars.length; j++) {
//     System.out.println("Tablelength: "+tablelength+"  Name: "+ varnames[j]+"  Levels: "+lnames[j][0]+"..."+"   Plevels: "+plevels[j]);
        }
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
                        index += plevels[j] * ((Variable) data.elementAt(vars[j])).Level((LString).trim());
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
        return ((Variable) data.elementAt(i)).isCategorical;
    }


    public boolean phoneNumber(int i) {
        return ((Variable) data.elementAt(i)).phoneNumber;
    }


    public boolean isPolyID(int i) {
        return ((Variable) data.elementAt(i)).isPolyID();
    }


    public String getName(int i) {
        return ((Variable) data.elementAt(i)).getName();
    }


    public int getNumLevels(int i) {
        return ((Variable) data.elementAt(i)).getNumLevels();
    }


    public String[] getLevels(int i) {
        return ((Variable) data.elementAt(i)).getLevels();
    }


    public String getLevelName(int i, double val) {
        Variable v = (Variable) data.elementAt(i);
        String[] LevelString = v.getLevels();
        if (alpha[i])
            return LevelString[(int) val];
        else
            return LevelString[v.IpermA[(int) v.Level(Double.toString(val))]];
    }


    public double[] getNumbers(int i) {
        Variable v = (Variable) data.elementAt(i);
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
        Variable v = (Variable) data.elementAt(i);
        return v.data;
    }


    public boolean[] getMissings(int i) {
        Variable v = (Variable) data.elementAt(i);
        return v.missing;
    }


    public int getNumMissings(int i) {
        return NAcount[i];
    }


    public int[] getSort(int i) {
        Variable v = (Variable) data.elementAt(i);
        return v.sortI;
    }


    public int[] getRank(int i) {
        Variable v = (Variable) data.elementAt(i);
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
        return ((Variable) data.elementAt(i)).Min();
    }


    public double getSelMin(int i) {
        return ((Variable) data.elementAt(i)).SelMin();
    }


    public double getMax(int i) {
        return ((Variable) data.elementAt(i)).Max();
    }


    public double getSelMax(int i) {
        return ((Variable) data.elementAt(i)).SelMax();
    }


    public double getMean(int i) {
        return ((Variable) data.elementAt(i)).Mean();
    }


    public double getSelMean(int i) {
        return ((Variable) data.elementAt(i)).selMean();
    }


    public double getSDev(int i) {
        return ((Variable) data.elementAt(i)).SDev();
    }


    public double getSelSDev(int i) {
        return ((Variable) data.elementAt(i)).selSDev();
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
        if ((((Variable) data.elementAt(filterVar)).alpha))
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
        if ((((Variable) data.elementAt(filterVar)).alpha)) {
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
            filterA[i] = (((Variable) data.elementAt(filterVar)).data)[i];
        }
        filterGrpSize = new int[((Variable) data.elementAt(var)).getNumLevels()];
        filterSelGrpSize = new int[((Variable) data.elementAt(var)).getNumLevels()];
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
        Variable v = (Variable) data.elementAt(j);
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
        return ((Variable) data.elementAt(i)).getQuantile(q);
    }


    public double getSelQuantile(int i, double q) {
        return ((Variable) data.elementAt(i)).getSelQuantile(i, q);
    }


    public double getFirstGreater(int i, double q) {
        return ((Variable) data.elementAt(i)).getFirstGreater(q);
    }


    public double getFirstSelGreater(int i, double q) {
        return ((Variable) data.elementAt(i)).getFirstSelGreater(q);
    }


    public double getFirstSmaller(int i, double q) {
        return ((Variable) data.elementAt(i)).getFirstSmaller(q);
    }


    public double getFirstSelSmaller(int i, double q) {
        return ((Variable) data.elementAt(i)).getFirstSelSmaller(q);
    }


    public double[] getAllSmaller(int i, double q) {
        return ((Variable) data.elementAt(i)).getAllSmaller(q);
    }


    public double[] getAllSelSmaller(int i, double q) {
        return ((Variable) data.elementAt(i)).getAllSelSmaller(q);
    }


    public double[] getAllGreater(int i, double q) {
        return ((Variable) data.elementAt(i)).getAllGreater(q);
    }


    public double[] getAllSelGreater(int i, double q) {
        return ((Variable) data.elementAt(i)).getAllSelGreater(q);
    }


    class Variable {

        private int catThres = (n > 800) ? (15 * Math.max(1, (int) (Math.log(n) / Math.log(10)) - 1)) : ((int) (1.5 * Math.sqrt(n)));
        private int dimThres = 1000;
        protected String[] levelA = new String[dimThres];
        protected int[] grpSize = new int[dimThres];
        protected int[] permA;
        protected int[] IpermA;
        protected int levelP = 0;
        protected boolean alpha;
        public boolean isCategorical = true;
        public boolean forceCategorical = false;
        public boolean phoneNumber = false;
        public boolean isPolyID = false;
        private String name;
        public double[] data;
        public int numMiss = 0;
        public int[] sortI;
        public boolean missing[];
        public double min = 1e+100, max = -1e+100;
        protected boolean minSet = false, maxSet = false, levelsSet = false;


        Variable(boolean alpha, String name) {
            this.alpha = alpha;
            this.name = name;
            if (name.substring(0, 2).equals("/P"))
                isCategorical = false;
        }


        Variable(int n, boolean alpha, String name) {
            this.alpha = alpha;
            this.name = name;
            if (name.length() > 1)
                if (name.substring(0, 2).equals("/P"))
                    isCategorical = false;
            data = new double[n];
            missing = new boolean[n];
        }


        Variable(BufferTokenizer BT, int col) {
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
            if (!levelsSet && isDB)
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
            if (!levelsSet && isDB)
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
            if (!levelsSet && isDB)
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
            double[] sA = new double[n];
            System.arraycopy(data, 0, sA, 0, n);
            sortI = Qsort.qsort(sA, 0, n - 1);
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
                if (isDB) {
                    try {
                        Statement stmt = con.createStatement();
                        String query = "select min(" + name + ") from " + Table;
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
                if (selectionArray[i] > 0 && !missing[i])
                    SM = Math.min(SM, data[i]);
            return SM;
        }


        public double Max() {
            if (!maxSet)
                if (isDB) {
                    try {
                        Statement stmt = con.createStatement();
                        String query = "select max(" + name + ") from " + Table;
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
                    for (int i = 0; i < n - numMiss; i++)
                        this.max = Math.max(data[i], this.max);
            maxSet = true;
            return this.max;
        }


        public double SelMax() {
            double SM = Double.MIN_VALUE;
            for (int i = 0; i < data.length; i++)
                if (selectionArray[i] > 0 && !missing[i])
                    SM = Math.max(SM, data[i]);
//System.out.println("Return Max: "+SM);  
            return SM;
        }


        public double Mean() {
            double sum = 0;
            for (int i = 0; i < n; i++)
                if (!missing[i])
                    sum += data[i];
            return sum / (n - numMiss);
        }


        public double selMean() {
            double sum = 0;
            int counter = 0;
            for (int i = 0; i < n; i++)
                if (selectionArray[i] > 0 && !missing[i]) {
                    sum += data[i];
                    counter++;
                }
            return sum / counter;
        }


        public double SDev() {
            double sum2 = 0;
            for (int i = 0; i < n; i++)
                if (!missing[i])
                    sum2 += data[i] * data[i];
            return Math.pow((sum2 - Math.pow(Mean(), 2) * (n - numMiss)) / ((n - numMiss) - 1), 0.5);
        }


        public double selSDev() {
            double sum2 = 0;
            int counter = 0;
            for (int i = 0; i < n; i++)
                if (selectionArray[i] > 0 && !missing[i]) {
                    sum2 += data[i] * data[i];
                    counter++;
                }
            return Math.pow((sum2 - Math.pow(selMean(), 2) * counter) / (counter - 1), 0.5);
        }


        public double getQuantile(double q) {
            if (!filterON)
                if (!isCategorical) {
                    int ind = (int) ((n - numMiss - 1) * q);
                    double remainder = ((n - numMiss - 1) * q) - ind;
//System.out.println(" Q: "+q+" INDEX:"+ind);
                    if (ind < n - 1)
                        return data[sortI[ind]] * (1 - remainder) + data[sortI[ind + 1]] * remainder;
                    else
                        return data[sortI[ind]];
                } else
                    return 0;
            else {
                int count = 0;
                int i = 0;
                if (q == 0) {
                    while (filterA[sortI[i]] != filterVal) {
                        i++;
//System.out.println("filter Val: "+filterVal+" filterVar: "+filterVar+" i:"+i+" - "+filterA[sortI[i]]);
                    }
                    return data[sortI[i]];
                }
                if (q == 1) {
                    i = n - numMiss - 1;
                    while (filterA[sortI[i]] != filterVal) {
                        i--;
                    }
                    return data[sortI[i]];
                }
//        System.out.println("filterGrp: "+filterGrp+" filterGrps: "+filterGrpSize.length);
//System.out.println("filter Val: "+filterVal+" filterVar: "+filterVar+" GroupSize. "+filterGrpSize[filterGrp]+" Group: "+filterGrp);
                int stop = (int) (q * (filterGrpSize[filterGrp] - 1));
                while (count <= stop && i < n) {
                    if (filterA[sortI[i]] == filterVal && !missing[sortI[i]]) {
                        count++;
//System.out.println("i: "+i+" filter Val: "+filterVal+" testVal: "+filterA[sortI[i]]+" GroupSize. "+filterGrpSize[filterGrp]);
                    }
                    i++;
                }
                i--;
//            System.out.println("q: "+q+" Count: "+count+" Value: "+ data[sortI[i-1]]);
                if (count < filterGrpSize[filterGrp] && stop + 0.000001 < (q * (filterGrpSize[filterGrp] - 1))) {   // get next for linear combi of two values ...
                    int j = i + 1;
                    while (filterA[sortI[j]] != filterVal || missing[sortI[j]]) {
                        j++;
                    }
                    j--;
                    double remainder = (q * (filterGrpSize[filterGrp] - 1)) - stop;
//          System.out.println(" GET NEXT :"+i+" <-> "+j);
                    return data[sortI[i]] * (1 - remainder) + data[sortI[j]] * remainder;
                } else
                    return data[sortI[i]];
            }
        }


        public double getSelQuantile(int var, double q) {
            int count = 0;
            int i = 0;
            if (!filterON) {
                if (!isCategorical) {
                    if (q == 0) {
                        while (selectionArray[sortI[i++]] == 0) {
                        }
                        return data[sortI[i - 1]];
                    }
                    if (q == 1) {
                        i = n - numMiss - 1;
                        while (i >= 0 && selectionArray[sortI[i]] == 0) {
                            i--;
                        }
                        return data[sortI[i]];
                    }
                    int stop = (int) (q * (countSelection(var) - 1));
                    while (count <= stop && i < n) {
                        if (selectionArray[sortI[i]] > 0 && !missing[sortI[i]])
                            count++;
                        i++;
                    }
                    i--;
                    //System.out.println(" Sel: "+countSelection(var)+" q: "+q+" i: "+i+" Stop: "+stop+" Count: "+count);
                    if (count < countSelection(var) && stop + 0.000001 < (q * (countSelection(var) - 1))) {   // get next for linear combi of two values ...
                        int j = i + 1;
                        while (selectionArray[sortI[j]] == 0 || missing[sortI[j]]) {
                            j++;
                        }
                        if (j != i + 1)
                            j--;
                        double remainder = (q * (countSelection(var) - 1)) - stop;
                        //          System.out.println(" GET NEXT :"+i+" <-> "+j);
                        return data[sortI[i]] * (1 - remainder) + data[sortI[j]] * remainder;
                    } else
                        return data[sortI[i]];
                } else
                    return 0;
            } else {
                if (q == 0) {
                    while ((selectionArray[sortI[i]] == 0) || (filterA[sortI[i]] != filterVal) || missing[sortI[i]]) {
                        i++;/*System.out.println("i: "+i+" "+selectionArray[sortI[i]]+" "+filterA[sortI[i]]+" "+filterVal);*/
                    }
                    return data[sortI[i]];
                }
                if (q == 1) {
                    i = n - numMiss - 1;
                    while (selectionArray[sortI[i]] == 0 || filterA[sortI[i]] != filterVal || missing[sortI[i]]) {
                        i--;/*System.out.println("i: "+i);*/
                    }
                    return data[sortI[i]];
                }
                int stop = (int) (q * (filterSelGrpSize[filterGrp] - 1));
//System.out.println("in q grpSize: "+filterSelGrpSize[filterGrp]);
                while (count <= stop) {
                    if (selectionArray[sortI[i]] > 0 && filterA[sortI[i]] == filterVal && !missing[sortI[i]])
                        count++;
                    i++;
                }
                i--;
//System.out.println(" Sel: "+filterSelGrpSize[filterGrp]+" q: "+q+" i: "+i+" Stop: "+stop+" Count: "+count+" filterVal: "+filterVal);
                if (count < filterSelGrpSize[filterGrp] && stop + 0.000001 < (q * (filterSelGrpSize[filterGrp] - 1))) {   // get next for linear combi of two values ...
                    int j = i + 1;
                    while ((selectionArray[sortI[j]] == 0) || (filterA[sortI[j]] != filterVal) || missing[sortI[j]]) {
                        j++;
                    }
                    //          {System.out.println(" j: "+j+" Filter: "+filterA[sortI[j]]+" Sel: "+selectionArray[sortI[j]]+" miss: "+missing[sortI[j]]);j++;}
                    j--;
                    double remainder = (q * (filterSelGrpSize[filterGrp] - 1)) - stop;
//          System.out.println(" GET NEXT :"+i+" <-> "+j+" ("+remainder+" - "+(1-remainder)+")");
                    return data[sortI[i]] * (1 - remainder) + data[sortI[j]] * remainder;
                } else
                    return data[sortI[i]];
            }
        }


        public double getFirstGreater(double g) {
            int i = 0;
            if (!filterON) {
                double ret = data[sortI[i]];
                while ((ret = data[sortI[i]]) < g)
                    i++;
                return ret;
            } else {
                while (i < n - 1 && (data[sortI[i]]) < g)
                    i++;
                while (i < n - 1 && filterA[sortI[i]] != filterVal)
                    i++;
                return data[sortI[i]];
            }
        }


        public double getFirstSelGreater(double g) {
            int i = 0;
            if (!filterON) {
                while (i < n - 1 && (data[sortI[i]]) < g)
                    i++;
                while (i < n - 1 && selectionArray[sortI[i]] == 0)
                    i++;
                return data[sortI[i]];
            } else {
                while (i < n - 1 && (data[sortI[i]]) < g)
                    i++;
                while (i < n - 1 && (selectionArray[sortI[i]] == 0 || filterA[sortI[i]] != filterVal))
                    i++;
                return data[sortI[i]];
            }
        }


        public double getFirstSmaller(double s) {
            int i = n - 1;
            if (!filterON) {
                double ret = data[sortI[i]];
                while ((ret = data[sortI[i]]) > s)
                    i--;
                return ret;
            } else {
                while (i > 0 && (data[sortI[i]]) > s)
                    i--;
                while (i > 0 && filterA[sortI[i]] != filterVal)
                    i--;
                return data[sortI[i]];
            }
        }


        public double getFirstSelSmaller(double s) {
            int i = n - 1;
            if (!filterON) {
                while (i > 0 && (data[sortI[i]]) > s)
                    i--;
                while (i > 0 && selectionArray[sortI[i]] == 0)
                    i--;
                return data[sortI[i]];
            } else {
                while (i > 0 && (data[sortI[i]]) > s)
                    i--;
                while (i > 0 && (selectionArray[sortI[i]] == 0 || filterA[sortI[i]] != filterVal))
                    i--;
                return data[sortI[i]];
            }
        }


        public double[] getAllSmaller(double s) {
            int i = 0;
            if (!filterON) {
                while ((data[sortI[i++]]) < s) {
                }
                double[] ret = new double[i - 1];
                for (int j = 0; j < i - 1; j++)
                    ret[j] = data[sortI[j]];
                return ret;
            } else {
                int count = 0;
                while (i < n && data[sortI[i]] < s)
                    if (filterA[sortI[i++]] == filterVal)
                        count++;
                //while( i<n && filterA[sortI[i++]] != filterVal ) {}
                //count++;
                if (count > 0) {
                    double[] ret = new double[count];
                    count = 0;
                    for (int j = 0; j < i; j++)
                        if (filterA[sortI[j]] == filterVal)
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
            if (!filterON) {
                while (i < n && data[sortI[i]] < s)
                    if (selectionArray[sortI[i++]] > 0 && !missing[sortI[i]])
                        count++;
//        while( i<n && selectionArray[sortI[i++]] == 0 && !missing[sortI[i]] ) {}
//        count++;
                if (count > 0) {
                    double[] ret = new double[count];
                    count = 0;
                    for (int j = 0; j < i; j++)
                        if (selectionArray[sortI[j]] > 0 && !missing[sortI[j]])
                            ret[count++] = data[sortI[j]];
                    return ret;
                } else {
                    return new double[0];
                }
            } else {
                while (i < n && data[sortI[i]] < s) {
                    if (filterA[sortI[i]] == filterVal && selectionArray[sortI[i]] > 0)
                        count++;
                    i++;
                }
                if (count > 0) {
                    double[] ret = new double[count];
                    count = 0;
                    for (int j = 0; j < i; j++)
                        if (filterA[sortI[j]] == filterVal && selectionArray[sortI[j]] > 0)
                            ret[count++] = data[sortI[j]];
                    return ret;
                } else {
                    return new double[0];
                }
            }
        }


        public double[] getAllGreater(double g) {
            int i = n - numMiss - 1;
            if (!filterON) {
                while ((data[sortI[i--]]) > g) {
                }
                double[] ret = new double[n - numMiss - i - 2];
                for (int j = n - numMiss - 1; j > i + 1; j--)
                    ret[n - numMiss - j - 1] = data[sortI[j]];
                return ret;
            } else {
                int count = 0;
                while (i >= 0 && (data[sortI[i]]) > g)
                    if (filterA[sortI[i--]] == filterVal)
                        count++;
                if (count > 0) {
                    double[] ret = new double[count];
                    count = 0;
                    for (int j = n - numMiss - 1; j > i; j--)
                        if (filterA[sortI[j]] == filterVal && count < ret.length)
                            ret[count++] = data[sortI[j]];
                    return ret;
                } else {
                    return new double[0];
                }
            }
        }


        public double[] getAllSelGreater(double g) {
            int i = n - numMiss - 1;
            int count = 0;
            if (!filterON) {
                while (i >= 0 && (data[sortI[i]]) >= g) {
                    if (selectionArray[sortI[i]] > 0 && !missing[sortI[i]])
                        count++;
                    i--;
                }
//      while( i>=0 && selectionArray[sortI[i--]] == 0 ) {}
//      count++;
                if (count > 0) {
                    double[] ret = new double[count];
                    count = 0;
                    for (int j = n - numMiss - 1; j > i; j--)
                        if (selectionArray[sortI[j]] > 0 && !missing[sortI[j]])
                            ret[count++] = data[sortI[j]];
                    return ret;
                } else {
                    return new double[0];
                }
            } else {
                while (i >= 0 && (data[sortI[i]]) > g) {
                    if (filterA[sortI[i]] == filterVal && selectionArray[sortI[i]] > 0)
                        count++;
                    i--;
                }
                if (count > 0) {
                    double[] ret = new double[count];
                    count = 0;
                    for (int j = n - numMiss - 1; j > i; j--)
                        if (filterA[sortI[j]] == filterVal && selectionArray[sortI[j]] > 0)
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
                Statement stmt = con.createStatement();
//        String query = "select "+name+" from "+Table+" where "+name+" is not null group by trim("+name+ ") order by trim(" + name +")";    
                String query = "select " + name + " from " + Table + " group by trim(" + name + ") order by trim(" + name + ")";
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
}

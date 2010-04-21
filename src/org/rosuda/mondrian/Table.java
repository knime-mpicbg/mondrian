package org.rosuda.mondrian;

import org.rosuda.mondrian.core.DataSet;
import org.rosuda.mondrian.core.InteractionSet;
import org.rosuda.mondrian.core.Selection;
import org.rosuda.mondrian.core.Variable;
import org.rosuda.mondrian.io.db.Query;

import java.sql.ResultSet;
import java.sql.Statement;


public class Table implements Cloneable {

    public String name;
    public double[] table;
    public double[] hilite;
    public double[] exp;
    public int k;
    public String[] names;
    public String[][] lnames;
    public int[] levels;
    public InteractionSet Interactions;
    public double G2;
    public double X2;
    public int df;
    public double p;
    public int[] initialVars;
    public int[][] Ids;
    public DataSet data;
    public int count;        // == -1 if the tables needs to be "breaked down", the weighting variabe otherwise
    public Query initialQuery;    // The query which was used to set up the table ...
    public String lastQuery;    // The query which last issued to get the selected items ...


    public Table(String name, double[] table, int k, int[] levels, String[] names, String[][] lnames, int[] initialVars, int[][] Ids, DataSet data, int count) {
        this.name = name;
        this.table = table;
        this.k = k;
        this.levels = levels;
        this.names = names;
        this.lnames = lnames;
        this.initialVars = initialVars;
        this.Ids = Ids;
        this.data = data;
        this.count = count;
        this.hilite = new double[table.length];
        this.exp = new double[table.length];
        this.Interactions = new InteractionSet(k);
        this.df = table.length - 1;

        // initialize selection state
        for (int i = 0; i < table.length; i++)
            hilite[i] = 0;

        // Clean Numerical Labels 1.0 -> 1
        for (int j = 0; j < k; j++) {
            boolean allDotNull = true;
            for (int i = 0; i < lnames[j].length; i++)
                if (!lnames[j][i].endsWith(".0"))
                    allDotNull = false;
            if (allDotNull)
                for (int i = 0; i < lnames[j].length; i++)
                    lnames[j][i] = lnames[j][i].substring(0, lnames[j][i].length() - 2);
        }
    }


    public Object clone() {
        Table cloneTable = new Table(this.name, this.table, this.k, this.levels,
                this.names, this.lnames, this.initialVars, this.Ids,
                this.data, this.count);
        cloneTable.hilite = new double[table.length];
        System.arraycopy(this.hilite, 0, cloneTable.hilite, 0, table.length);
        cloneTable.exp = new double[table.length];
        System.arraycopy(this.exp, 0, cloneTable.exp, 0, table.length);
        cloneTable.Interactions = (InteractionSet) (Interactions.clone());
        cloneTable.G2 = this.G2;
        cloneTable.X2 = this.X2;
        cloneTable.df = this.df;
        cloneTable.p = this.p;
        cloneTable.initialQuery = this.initialQuery;
        cloneTable.lastQuery = this.lastQuery;

        return cloneTable;
    }


    public void rebreak() {
        Table tmpTable = data.breakDown(name, initialVars, count);
        this.table = tmpTable.table;
        this.names = tmpTable.names;
        this.lnames = tmpTable.lnames;
        this.levels = tmpTable.levels;
        this.Ids = tmpTable.Ids;
        this.hilite = tmpTable.hilite;
        this.exp = tmpTable.exp;

        tmpTable = null;
    }


    public void updateBins(double start, double width) {
        Table tmpTable = data.discretize(name, initialVars[0], start, width, count);
        this.table = tmpTable.table;
        this.names = tmpTable.names;
        this.lnames = tmpTable.lnames;
        this.levels = tmpTable.levels;
        this.Ids = tmpTable.Ids;
        this.hilite = tmpTable.hilite;
        this.exp = tmpTable.exp;
        this.lastQuery = tmpTable.lastQuery;
        this.initialQuery = tmpTable.initialQuery;

        tmpTable = null;
    }


    public void update2DBins(double xStart, double xEnd, int nX, double yStart, double yEnd, int nY) {
        Table tmpTable = data.discretize2D(name, initialVars[0], xStart, xEnd, nX, initialVars[1], yStart, yEnd, nY);
        this.table = tmpTable.table;
        this.names = tmpTable.names;
        this.lnames = tmpTable.lnames;
        this.levels = tmpTable.levels;
        this.Ids = tmpTable.Ids;
        this.hilite = tmpTable.hilite;
        this.exp = tmpTable.exp;
        this.lastQuery = tmpTable.lastQuery;
        this.initialQuery = tmpTable.initialQuery;
//System.out.println(" ******** Bins updated ********* "+xStart+" "+xEnd+" "+yStart+" "+yEnd);
        tmpTable = null;
    }


    public boolean addInteraction(int[] newInteraction, boolean update) {
        if (!Interactions.isMember(newInteraction)) {
            Interactions.newMember(newInteraction);
            if (update) {
                if (this.k > 1)
                    this.logLinear();
                else
                    System.arraycopy(this.table, 0, this.exp, 0, table.length);
            }
            return true;
        } else
            return false;
    }


    public boolean deleteInteraction(int[] delInteraction) {
        boolean in = Interactions.isMember(delInteraction);
        if (in && delInteraction.length > 1) {
            Interactions.deleteMember(delInteraction);
            this.logLinear();
        }
        return in;
    }


    public void logLinear() {

        int[][] interact = new int[Interactions.Ssize()][];
        for (int i = 0; i < Interactions.Ssize(); i++) {
            interact[i] = (int[]) Interactions.SmemberAt(i);
        }
        int n = this.table.length;
        int[][] permArray = new int[interact.length][this.k];
        int[][] ipermArray = new int[interact.length][this.k];
        int[] sumInd = new int[interact.length];
        for (int i = 0; i < interact.length; i++) {
            for (int j = 0; j < k; j++) {
                if (j < interact[i].length)
                    permArray[i][j] = interact[i][j];
                else
                    permArray[i][j] = -1;
            }
        }
        for (int i = 0; i < interact.length; i++) {
            for (int j = 0; j < this.k; j++) {
                int l = 0;
                while (permArray[i][l] != j)
                    if (permArray[i][l] == -1)
                        permArray[i][l] = j;
                    else
                        l++;
            }
        }
        for (int i = 0; i < interact.length; i++) {
            sumInd[i] = 1;
            for (int j = 0; j < this.k; j++) {
                ipermArray[i][permArray[i][j]] = j;
                if (!(j < interact[i].length))
                    sumInd[i] *= levels[permArray[i][j]];
            }
        }
        double[] lastIteration = new double[n];
        for (int i = 0; i < n; i++)
            this.exp[i] = 1;
        boolean converge = false;
        while (!converge) {
            System.out.println("");
            System.out.println("###########################");
            for (int i = interact.length - 1; i >= 0; i--) {
                System.out.println("Interaction No: " + i);
                this.permute(permArray[i]);
                double obs = 0.0;
                double exp = 0.0;
                double scale;
                for (int l = 0; l < n; l++) {
                    obs += this.table[l];
                    exp += this.exp[l];
                    if (((l + 1) % sumInd[i]) == 0) {
                        if (exp < 0.0001)
                            scale = 0;
                        else
                            scale = obs / exp;
                        obs = 0;
                        exp = 0;
                        for (int m = l - sumInd[i] + 1; m <= l; m++)
                            this.exp[m] *= scale;
                    }
                }
                this.permute(ipermArray[i]);
            }
            converge = true;
            for (int l = 0; l < n; l++)
                converge = converge && (Math.abs(lastIteration[l] - this.exp[l]) < 0.01);
            System.arraycopy(this.exp, 0, lastIteration, 0, n);
        }
        G2 = 0;
        X2 = 0;
        for (int l = 0; l < n; l++) {
            if (table[l] > 0) {
                G2 += 2 * table[l] * Math.log(table[l] / exp[l]);
                X2 += Math.pow(table[l] - exp[l], 2) / exp[l];
            }
        }
        this.df = table.length - 1;
        for (int i = 0; i < Interactions.size(); i++) {
            int mul = 1;
            for (int j = 0; j < (Interactions.memberAt(i)).length; j++)
                mul *= levels[(Interactions.memberAt(i))[j]] - 1;
            df -= mul;
        }
        for (int i = 0; i < Interactions.Ssize(); i++) {
            for (int j = 0; j < interact[i].length - 1; j++)
                System.out.print(names[interact[i][j]] + "*");
            System.out.println(names[interact[i][interact[i].length - 1]]);
        }
        this.p = 1 - Stat.pchisq(G2, df);
        System.out.println(" Df:" + df + "  G2: " + Stat.round(G2, 2) + "  X2: " + Stat.round(X2, 2) + "  p: " + Stat.round(p, 3));
    }


    public void permute(int[] perm) {

        //MTh: This is really very peinlich !!! ...    initialVars = perm;

        Interactions.permute(perm);

        int[] plevels = new int[k];
        int[][] index;
        // permuted pendants
        double[] p_table = new double[table.length];
        double[] p_exp = new double[table.length];
        double[] p_hilite = new double[table.length];
        int[][] p_Ids = new int[table.length][];
        int[] p_initialVars = new int[k];
        String[] p_names = new String[k];
        String[][] p_lnames = new String[k][];
        for (int i = 0; i < k; i++)
            p_lnames[i] = new String[lnames[perm[i]].length];
        int[] p_levels = new int[k];

        plevels[k - 1] = 0;
        plevels[k - 2] = levels[k - 1];        // calculate the number of cells covered by a
        // category in level k
        for (int i = k - 3; i >= 0; i--) {
            plevels[i] = plevels[i + 1] * levels[i + 1];
        }

        index = new int[table.length][k];

        int decompose;

        for (int i = 0; i < table.length; i++) {
            decompose = i;
            for (int j = 0; j < k - 1; j++) {
                index[i][j] = decompose / plevels[j];
                decompose -= index[i][j] * plevels[j];
            }
            index[i][k - 1] = decompose;
        }

        for (int i = 0; i < k; i++) {                  // permute the names: this is easy
            p_names[i] = names[perm[i]];
            p_levels[i] = levels[perm[i]];
            p_initialVars[i] = initialVars[perm[i]];
        }

        for (int i = 0; i < k; i++) {                  // and the level names
            for (int j = 0; j < (p_lnames[i].length); j++) {
                p_lnames[i][j] = lnames[perm[i]][j];
            }
        }

        names = p_names;                           // assigning the permuted arrays
        levels = p_levels;
        initialVars = p_initialVars;        // MTh: this was just forgotten and caused confusion .... look comment above !
        this.lnames = p_lnames;

        plevels[k - 2] = levels[k - 1];         // calculate the number of cells covered by a
        // category in level k
        for (int i = k - 3; i >= 0; i--) {
            plevels[i] = plevels[i + 1] * levels[i + 1];
        }

        for (int i = 0; i < table.length; i++) {
            decompose = 0;
            for (int j = 0; j < k - 1; j++) {
                decompose += index[i][perm[j]] * plevels[j];
            }
            decompose += index[i][perm[k - 1]];
            p_table[decompose] = table[i];
            p_exp[decompose] = exp[i];
            p_hilite[decompose] = hilite[i];
            p_Ids[decompose] = Ids[i];
        }

        table = p_table;
        exp = p_exp;
        hilite = p_hilite;
        Ids = p_Ids;

    } // end perm


    public double[] getSelection() {
        if (data.isDB) {
            if (!data.sqlConditions.getConditions().equals("")) {
                int[] plevels = new int[initialVars.length];
                int[] varlevels = new int[initialVars.length];

                for (int j = 0; j < initialVars.length; j++)
                    varlevels[j] = data.getNumLevels(initialVars[j]);
                plevels[initialVars.length - 1] = 1;
                for (int j = initialVars.length - 2; j >= 0; j--) {
                    plevels[j] = varlevels[j + 1] * plevels[j + 1];
                }
                try {
                    Query hiliteQ = new Query();
                    hiliteQ.addItem(initialQuery.getItems());
                    hiliteQ.addTable(initialQuery.getTables());
                    if (!data.sqlConditions.getConditions().equals(""))
                        hiliteQ.addCondition("", "(" + data.sqlConditions.getConditions() + ")");
                    if (!initialQuery.getConditions().equals(""))
                        hiliteQ.addCondition("AND", "(" + initialQuery.getConditions() + ")");
                    hiliteQ.addGroup(initialQuery.getGroups());
                    hiliteQ.addOrder(initialQuery.getOrder());
                    if (!hiliteQ.makeQuery().equals(lastQuery)) {
                        clearSelection();
//System.out.println(" In Table.java, in getSelection: "+hiliteQ.makeQuery());
                        Statement stmt = data.con.createStatement();
                        ResultSet rs = stmt.executeQuery(hiliteQ.makeQuery());

                        while (rs.next()) {
                            int index = 0;
                            for (int j = 0; j < initialVars.length; j++) {
//System.out.println((rs.getString(j+1)+" -> "+((Variable)data.data.elementAt(initialVars[j])).isLevel((rs.getString(j+1)).trim())));
                                if (initialQuery.getItems().indexOf("category42") == -1) {  // classical Table
                                    String tmp = rs.getString(j + 1);
                                    if (tmp == null)
                                        tmp = "NA";
                                    index += plevels[j] * ((Variable) data.data.elementAt(initialVars[j])).Level((tmp).trim());
                                } else // table for histogram !!!
                                    for (int i = 0; i < table.length; i++) {
//System.out.println(lnames[0][i]+" <-> "+rs.getString(j+1).trim());
                                        String tmp = rs.getString(j + 1).trim();
                                        tmp = tmp.substring(1, tmp.length()).trim();
                                        tmp = "[" + tmp.substring(0, tmp.indexOf(',')) + ", " + tmp.substring(tmp.indexOf(',') + 1, tmp.length()).trim();
                                        if (lnames[0][i].equals(tmp))
                                            index = i;
                                    }
                            }
//System.out.println("Index: "+index+" Table: "+table[index]+" Wert: "+rs.getInt(initialVars.length+1));
                            hilite[index] = rs.getInt(initialVars.length + 1) / table[index];
                        }
                        rs.close();
                        stmt.close();
                        lastQuery = hiliteQ.makeQuery();
                    }
                } catch (Exception ex) {
                    System.out.println("DB Exception get Table Hilite: " + ex);
                }
            } else {
                clearSelection();
                lastQuery = "";
            }
        } else {
            for (int i = 0; i < table.length; i++)
                hilite[i] = retrieve(i);
        }
        return hilite;
    }


    public double getSelected(int i) {
        if (!data.isDB)
            hilite[i] = retrieve(i);
        return hilite[i];
    }


    public void setSelection(int i, double s, int mode) {
        if (!data.isDB)
            propagate(i, s, mode);
        else
            switch (mode) {
                case Selection.MODE_STANDARD:
                    hilite[i] = s;
                    //System.out.println("REPLACE at: "+i+" with: "+s);
                    break;
                case Selection.MODE_AND:
                    //System.out.print("AND at: "+i+" from: "+selectionArray[i]);
                    hilite[i] *= s;
                    //System.out.println(" to: "+selectionArray[i]);
                    break;
                case Selection.MODE_OR:
                    hilite[i] = Math.max(s, hilite[i]);
                    System.out.println(" ------------> bar: " + i + " = " + hilite[i]);
                    break;
                case Selection.MODE_XOR:
                    if (s > 0)
                        if (hilite[i] > 0)
                            hilite[i] = 0;
                        else
                            hilite[i] = s;
                    break;
                case Selection.MODE_NOT:
                    if (s > 0)
                        hilite[i] = 0;
            }
    }


    public void clearSelection() {
        hilite = new double[this.table.length];
        data.clearSelection();
    }


    public void propagate(int i, double s, int mode) {
        for (int j = 0; j < Ids[i].length; j++)
            data.setSelection(Ids[i][j], s, mode);
    }


    public double retrieve(int i) {

        if (Ids[i].length > 0) {
            double sum = 0, total = 0;
            if (count != -1) {
                double[] counts = data.getRawNumbers(count);
                boolean[] miss = data.getMissings(count);
                for (int j = 0; j < Ids[i].length; j++) {
                    if (!miss[Ids[i][j]]) {
                        sum += data.getSelected(Ids[i][j]) * counts[Ids[i][j]];
                        total += counts[Ids[i][j]];
                    }
                }
                if (total != 0)
                    return sum / total;
                else
                    return 0;
            } else {
                for (int j = 0; j < Ids[i].length; j++)
                    sum += data.getSelected(Ids[i][j]);
                return sum / Ids[i].length;
            }
        } else
            return 0;
    }


    void print() {

        int[] plevels = new int[k];

        if (k >= 3) {
            plevels[k - 1] = 1;
            plevels[k - 2] = levels[k - 1];        // calculate the number of cells covered by a
            // category in level k
            for (int i = k - 3; i >= 0; i--) {
                plevels[i] = plevels[i + 1] * levels[i + 1];
            }
        } else {
            plevels[0] = levels[1];
            plevels[1] = 1;
        }


        for (int j = 0; j < k; j++) {
            System.out.print(names[j] + "\t");
        }
        System.out.println("Count");
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < k; j++) {
                System.out.print(lnames[j][(i / plevels[j]) % levels[j]] + "\t");
            }
            System.out.println(table[i]);
        }
    } // end print
} // end Table 


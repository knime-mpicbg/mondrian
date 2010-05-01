package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.Selection;
import de.mpicbg.sweng.mondrian.core.Table;
import de.mpicbg.sweng.mondrian.plots.basic.MyRect;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
import de.mpicbg.sweng.mondrian.util.GraphicsPerformance;
import de.mpicbg.sweng.mondrian.util.StatUtil;
import de.mpicbg.sweng.mondrian.util.Utils;
import de.mpicbg.sweng.mondrian.util.r.RService;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;


public class Scatter2DPlot extends DragBox {

    private Vector rects = new Vector(512, 512);    // Store the tiles.
    private int width, height;                   // The preferred size.
    protected int oldWidth, oldHeight;           // The last size for constructing the polygons.
    private int hiliteId = 0;
    private double xMin;
    private double xMax;
    private int shiftx, shifty;
    private double scalex, scaley;
    private DataSet data;
    private JComboBox Varlist;
    private int displayVar = -1;
    private Image bi, tbi, ttbi, tttbi, fi;            // four buffer: 1. double, 2. hilite, 3. labels, 4. filtered
    private MediaTracker media = new MediaTracker(this);
    private Graphics2D bg;
    private Graphics2D ttbg;
    private Graphics2D tttbg;
    private int[] Vars;
    private JList varList;
    private double[] xVal;
    private double[] yVal;
    private double[] coeffs;
    private double[] selCoeffs = {-10000, -10000, 0};
    private int radius = 3;            // radius of points
    private int[] alphas = {1, 2, 4, 8, 16, 32, 50, 68, 84, 92, 96, 98, 99};
    private int alphap;
    private int alpha = alphas[alphap];            // transparency of points
    private String displayMode = "Free";
    private String modeString = "bins";
    private String smoothF = "none";
    private boolean compareToAll = true;
    private int smoother = 5;
    private boolean connectLines = false;
    private int lastPointId = -1;
    private int byVar = -1;
    private int weightVar = -1;
    private int outside = 5;
    private int tick = 5;
    private boolean info = false;
    private int roundX;
    private int roundY;
    private Table binning;
    private boolean force = false;
    private boolean invert = false;
    private boolean alphaChanged = false;
    private boolean smoothChanged = false;


    /**
     * This constructor requires a Frame and a desired size
     */
    public Scatter2DPlot(MDialog frame, int width, int height, DataSet data, int[] Vars, JList varList, boolean matrix) {
        super(frame);
        boolean matrix1 = matrix;
        this.data = data;
        this.width = width;
        this.height = height;
        if (matrix) {
            super.printable = false;
            border = 15;
            xShift = 12;
            yShift = -12;
        } else {
            border = 30;
            xShift = 0;
            yShift = 0;
        }
        this.varList = varList;
        this.Vars = Vars;

        //    this.setBackground(new Color(255, 255, 152));
        //this.setBackground(new Color(0, 0, 0));

        // the events we are interested in.
        this.enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
        this.requestFocus();

        if (data.n < 50)
            alphap = 12;
        else if (data.n < 100)
            alphap = 10;
        else if (data.n < 500)
            alphap = 9;
        else if (data.n < 1000)
            alphap = 8;
        else if (data.n < 2000)
            alphap = 6;
        else
            alphap = 5;
        alpha = alphas[alphap];

        xMin = data.getMin(Vars[0]);
        xMax = data.getMax(Vars[0]);
        double yMin = data.getMin(Vars[1]);
        double yMax = data.getMax(Vars[1]);

        setCoordinates(xMin, yMin, xMax, yMax, -1);

        create();

        roundX = (int) Math.max(0, 2 - Math.round((Math.log(xMax - xMin) / Math.log(10))));
        roundY = (int) Math.max(0, 2 - Math.round((Math.log(yMax - yMin) / Math.log(10))));
    }


    public void maintainSelection(Selection S) {

        Rectangle sr = S.r;
        int mode = S.mode;

        if (sr.width < 4 && sr.height < 5 && S.status == Selection.KILLED) {
            // This is a oneClick selection -> we expand the rectangle
            if (modeString.equals("points")) {
                sr.x -= radius / 2 + 1;
                sr.y -= radius / 2 + 1;
                sr.width = radius;
                sr.height = radius;
            }
        }

        S.o = new floatRect(worldToUserX(S.r.x),
                worldToUserY(S.r.y),
                worldToUserX(S.r.x + S.r.width),
                worldToUserY(S.r.y + S.r.height));

        if (modeString.equals("points")) {
            for (int i = 0; i < data.n; i++) {
                if (sr.contains((int) userToWorldX(xVal[i]), (int) userToWorldY(yVal[i])))
                    data.setSelection(i, 1, mode);
                else
                    data.setSelection(i, 0, mode);
            }
        } else {
            for (int i = 0; i < rects.size(); i++) {
                MyRect r = (MyRect) rects.elementAt(i);
                if (r.intersects(sr)) {
//                S.condition.addCondition("OR", binning.names[0]+" = '"+binning.lnames[0][i]+"'");
                    if (binning.data.isDB)
                        binning.getSelection();
                    else {
                        double sum = 0, sumh = 0;
                        for (int j = 0; j < r.tileIds.size(); j++) {
                            int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                            // System.out.println("Id: "+id+":"+i);
                            binning.setSelection(id, 1, mode);
                            sumh += binning.getSelected(id) * binning.table[id];
                            sum += binning.table[id];
                        }
                        r.setHilite(sumh / sum);
                    }
                } else if (!binning.data.isDB)
                    for (int j = 0; j < r.tileIds.size(); j++) {
                        int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                        binning.setSelection(id, 0, mode);
                    }
            }
        }
    }


    public void updateSelection() {
        paint(this.getGraphics());
    }


    public void dataChanged(int var) {
        if (var == -1) {
            scaleChanged = true;
            create();
            paint(this.getGraphics());
        }
    }


    public String getToolTipText(MouseEvent e) {
        if (e.isControlDown() && !e.isAltDown()) {
            if (smoothF.equals("ls-line") && Math.abs((int) userToWorldY(worldToUserX(e.getX()) * coeffs[1] + coeffs[0]) - e.getY()) < 4) {
                String x = data.getName(Vars[1]) + " = " + data.getName(Vars[0]) + " * " + StatUtil.roundToString(coeffs[1], 4) + " + " + StatUtil.roundToString(coeffs[0], 4);
                x = x + "\n" + "R<sup>2</sup>\t " + StatUtil.roundToString(100 * coeffs[2], 1);
                return Utils.info2Html(x);
            } else if (smoothF.equals("ls-line") && Math.abs((int) userToWorldY(worldToUserX(e.getX()) * selCoeffs[1] + selCoeffs[0]) - e.getY()) < 4) {
                String x = data.getName(Vars[1]) + " = " + data.getName(Vars[0]) + " * " + StatUtil.roundToString(selCoeffs[1], 4) + " + " + StatUtil.roundToString(selCoeffs[0], 4);
                x = x + "\n" + "R<sup>2</sup>\t " + StatUtil.roundToString(100 * selCoeffs[2], 1);
                return Utils.info2Html(x);
            }
            if (modeString.equals("points")) {
                int minDist = 5000;
                int minId = 0;
                int minCount = 0;
                int maxOverplot = data.n;
                int restPoints = 0;
                int minIds[] = new int[maxOverplot];
                for (int i = 0; i < data.n; i++) {
                    int dist = (int) Math.pow(Math.pow(userToWorldX(xVal[i]) - e.getX(), 2)
                            + Math.pow(userToWorldY(yVal[i]) - e.getY(), 2), 0.5);
                    if (dist < minDist) {
                        minDist = dist;
                        minIds[minCount = 0] = i;
                        restPoints = 0;
                        minCount++;
                    } else if (dist == minDist) {
                        if (minCount < maxOverplot)
                            minIds[minCount++] = i;
                        else
                            restPoints++;
                    }
                }
                if (minDist < 5) {
                    String x = "";
                    int[] selectedIds;
                    if (e.isShiftDown())
                        selectedIds = varList.getSelectedIndices();
                    else
                        selectedIds = this.Vars;
                    if (selectedIds.length == 0)
                        selectedIds = Vars;


                    if (minCount == 1) {  // count == 1 no brackets
                        for (int sel = 0; sel < selectedIds.length; sel++) {
                            String label = data.getName(selectedIds[sel]);
                            String val = "NA";

                            if (!(data.getMissings(selectedIds[sel]))[minIds[0]]) {
                                if (data.categorical(selectedIds[sel])) {
                                    if (data.alpha(selectedIds[sel]))
                                        val = data.getLevelName(selectedIds[sel], (data.getNumbers(selectedIds[sel]))[minIds[0]]);
                                    else
                                        val = data.getLevelName(selectedIds[sel], (data.getRawNumbers(selectedIds[sel]))[minIds[0]]);
                                } else
                                    val = Double.toString((data.getRawNumbers(selectedIds[sel]))[minIds[0]]);
                            }

                            // check to see if there is a template to modify values for this field
                            val = Utils.getHTMLValue(label, val);

                            x = x + "\n" + label + "\t " + val;
                        }
                    } else {   // count > 1 add brackets
                        x = " Count\t " + minCount + " ";
                        double Mins[] = new double[selectedIds.length];
                        double Maxs[] = new double[selectedIds.length];
                        HashSet Levels[] = new HashSet[selectedIds.length];
                        for (int sel = 0; sel < selectedIds.length; sel++) {
                            Levels[sel] = new HashSet();
                            Mins[sel] = Double.MAX_VALUE;
                            Maxs[sel] = -Double.MAX_VALUE;
                        }
                        for (int ids = 0; ids < minCount; ids++) {
                            for (int sel = 0; sel < selectedIds.length; sel++) {
                                if (data.categorical(selectedIds[sel]))
                                    if (data.alpha(selectedIds[sel]))
                                        Levels[sel].add(data.getLevelName(selectedIds[sel], (data.getNumbers(selectedIds[sel]))[minIds[ids]]));
                                    else
                                        Levels[sel].add(data.getLevelName(selectedIds[sel], (data.getRawNumbers(selectedIds[sel]))[minIds[ids]]));
                                else {
                                    if (!(data.getMissings(selectedIds[sel]))[minIds[ids]]) {
                                        Mins[sel] = Math.min(Mins[sel], (data.getRawNumbers(selectedIds[sel]))[minIds[ids]]);
                                        Maxs[sel] = Math.max(Maxs[sel], (data.getRawNumbers(selectedIds[sel]))[minIds[ids]]);
                                    }
                                }
                            }
                        }

                        //Create info string with name "\t" values "\n"
                        for (int sel = 0; sel < selectedIds.length; sel++) {
                            String[] Names = {""};
                            Names = (String[]) Levels[sel].toArray(Names);
                            if (data.categorical(selectedIds[sel])) {
                                String label = data.getName(selectedIds[sel]);
                                x = x + "\n" + label + "\t {";
                                if (Names.length > 1)
                                    for (int i = 0; i < Names.length - 1; i++) {
                                        x = x + Utils.getHTMLValue(label, Names[i]) + ", ";
                                        if (((i + 1) % 3) == 0)
                                            x = x + "\n \t ";
                                    }
                                x = x + Utils.getHTMLValue(label, Names[Names.length - 1]) + "} ";
                            } else {
                                String label = data.getName(selectedIds[sel]);
                                if (Mins[sel] == Maxs[sel])
                                    x = x + "\n" + label + "\t " + Utils.getHTMLValue(label, Double.toString(Mins[sel]));
                                else if (Mins[sel] != Double.MAX_VALUE && Maxs[sel] != -Double.MAX_VALUE)
                                    x = x + "\n" + label + "\t "
                                            + " [" + Utils.getHTMLValue(label, Double.toString(Mins[sel]))
                                            + ", " + Utils.getHTMLValue(label, Double.toString(Maxs[sel])) + "] ";
                                else
                                    x = x + "\n" + label + "\t NA";
                            }
                        }
                    }
                    return Utils.info2Html(x);
                } else
                    return null;
            } else {
                for (int i = 0; i < rects.size(); i++) {
                    MyRect r = (MyRect) rects.elementAt(i);
                    if (r.contains(e.getX(), e.getY())) {
                        return Utils.info2Html(r.getLabel());
                    }
                }
                return null;
            }
        } else
            return null;
    }


    public void processMouseEvent(MouseEvent e) {

        if (e.isPopupTrigger() && !e.isShiftDown())
            super.processMouseEvent(e);  // Pass other event types on.
        if (changePop) {
            changePop = false;
            return;
        }

        if (e.getID() == MouseEvent.MOUSE_PRESSED ||
                e.getID() == MouseEvent.MOUSE_RELEASED) {
            if (e.isPopupTrigger() && !e.isShiftDown()) {
                info = false;
                if (!info) {
                    if (smoothF.equals("ls-line") && Math.abs((int) userToWorldY(worldToUserX(e.getX()) * coeffs[1] + coeffs[0]) - e.getY()) < 4) {
                        //System.out.println(data.getName(Vars[1])+" = "+data.getName(Vars[0])+" * "+coeffs[1]+" + "+coeffs[0]);
                        JPopupMenu line = new JPopupMenu();
                        JMenuItem formula = new JMenuItem(data.getName(Vars[1]) + " = " + data.getName(Vars[0]) + " * " + StatUtil.roundToString(coeffs[1], 4) + " + " + StatUtil.roundToString(coeffs[0], 4));
                        line.add(formula);
                        JMenuItem r2 = new JMenuItem("R^2: " + StatUtil.roundToString(100 * coeffs[2], 1));
                        line.add(r2);
                        line.show(e.getComponent(), e.getX(), e.getY());
                    } else if (smoothF.equals("ls-line") && Math.abs((int) userToWorldY(worldToUserX(e.getX()) * selCoeffs[1] + selCoeffs[0]) - e.getY()) < 4) {
                        JPopupMenu line = new JPopupMenu();
                        JMenuItem formula = new JMenuItem(data.getName(Vars[1]) + " = " + data.getName(Vars[0]) + " * " + StatUtil.roundToString(selCoeffs[1], 4) + " + " + StatUtil.roundToString(selCoeffs[0], 4));
                        line.add(formula);
                        JMenuItem r2 = new JMenuItem("R^2: " + StatUtil.roundToString(100 * selCoeffs[2], 1));
                        line.add(r2);
                        line.show(e.getComponent(), e.getX(), e.getY());
                    } else {
                        JPopupMenu mode = new JPopupMenu();
                        if (displayMode.equals("Fixed")) {
                            JMenuItem free = new JMenuItem("free aspect ratio");
                            mode.add(free);
                            free.setActionCommand("Free");
                            free.addActionListener(this);
                        } else {
                            JMenuItem fixed = new JMenuItem("fixed aspect ratio");
                            mode.add(fixed);
                            fixed.setActionCommand("Fixed");
                            fixed.addActionListener(this);
                        }
                        JMenuItem axes = new JMenuItem("flip axes");
                        mode.add(axes);
                        axes.setActionCommand("axes");
                        axes.addActionListener(this);

                        JMenu pointSize = new JMenu("point size");
                        mode.add(pointSize);
                        JCheckBoxMenuItem[] radians = new JCheckBoxMenuItem[20];
                        for (int k = 0; k < radians.length; k++) {
                            radians[k] = new JCheckBoxMenuItem("" + (k * 2 + 1));
                            if (radius == (k * 2 + 1))
                                radians[k].setState(true);
                            else
                                radians[k].setState(false);
                            radians[k].setActionCommand("" + (k * 2 + 1));
                            radians[k].addActionListener(this);
                            pointSize.add(radians[k]);
                        }

                        JMenu alphaVal = new JMenu("alpha");
                        mode.add(alphaVal);
                        JCheckBoxMenuItem[] alphians = new JCheckBoxMenuItem[20];
                        for (int k = 0; k < alphas.length; k++) {
                            alphians[k] = new JCheckBoxMenuItem(StatUtil.roundToString((double) alphas[k] / 100, 3));
                            if (alpha == alphas[k])
                                alphians[k].setState(true);
                            else
                                alphians[k].setState(false);
                            alphians[k].setActionCommand("-" + k);
                            alphians[k].addActionListener(this);
                            alphaVal.add(alphians[k]);
                        }

                        JMenu smoothers = new JMenu("smoothers");

                        JCheckBoxMenuItem nosmooth = new JCheckBoxMenuItem("none");
                        smoothers.add(nosmooth);
                        nosmooth.setActionCommand("none");
                        nosmooth.addActionListener(this);
                        if (smoothF.equals("none")) {
                            nosmooth.setSelected(true);
                            nosmooth.setEnabled(false);
                        }
                        JCheckBoxMenuItem lsline = new JCheckBoxMenuItem("ls-line");
                        smoothers.add(lsline);
                        lsline.setActionCommand("ls-line");
                        lsline.addActionListener(this);
                        if (smoothF.equals("ls-line")) {
                            lsline.setSelected(true);
                            lsline.setEnabled(false);
                        }
                        JCheckBoxMenuItem loess = new JCheckBoxMenuItem("loess (" + StatUtil.round(3.75 / smoother, 2) + ")");
                        smoothers.add(loess);
                        loess.setActionCommand("loess");
                        loess.addActionListener(this);
                        if (smoothF.equals("loess")) {
                            loess.setSelected(true);
                            loess.setEnabled(false);
                        }
                        JCheckBoxMenuItem splines = new JCheckBoxMenuItem("splines (" + smoother + ")");
                        smoothers.add(splines);
                        splines.setActionCommand("splines");
                        splines.addActionListener(this);
                        if (smoothF.equals("splines")) {
                            splines.setSelected(true);
                            splines.setEnabled(false);
                        }
                        JCheckBoxMenuItem locfit = new JCheckBoxMenuItem("locfit (" + StatUtil.round(3.5 / smoother, 2) + ")");
//              smoothers.add(locfit);
                        locfit.setActionCommand("locfit");
                        locfit.addActionListener(this);
                        if (smoothF.equals("locfit")) {
                            locfit.setSelected(true);
                            locfit.setEnabled(false);
                        }

                        smoothers.addSeparator();

                        JCheckBoxMenuItem compareAll = new JCheckBoxMenuItem("compare to all");
                        if (compareToAll)
                            compareAll.setSelected(true);
                        else
                            compareAll.setSelected(false);
                        if (smoothF.equals("none"))
                            compareAll.setEnabled(false);
                        compareAll.setActionCommand("compare");
                        compareAll.addActionListener(this);
                        smoothers.add(compareAll);

                        smoothers.addSeparator();

                        JCheckBoxMenuItem rougher = new JCheckBoxMenuItem("rougher");
                        rougher.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.SHIFT_MASK));
                        smoothers.add(rougher);
                        rougher.setActionCommand("rougher");
                        rougher.addActionListener(this);

                        JCheckBoxMenuItem smoother = new JCheckBoxMenuItem("smoother");
                        smoother.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.SHIFT_MASK));
                        smoothers.add(smoother);
                        smoother.setActionCommand("smoother");
                        smoother.addActionListener(this);
                        if (smoothF.equals("none") || smoothF.equals("ls-line")) {
                            smoother.setEnabled(false);
                            rougher.setEnabled(false);
                        }

                        if (!RService.hasR()) {
                            loess.setEnabled(false);
                            splines.setEnabled(false);
                            locfit.setEnabled(false);
                        }

                        mode.add(smoothers);

                        JMenu disMode = new JMenu("Mode");
                        if (modeString.equals("bins")) {
                            JMenuItem points = new JMenuItem("force points");
                            disMode.add(points);
                            points.setActionCommand("points");
                            points.addActionListener(this);
                        } else {
                            JMenuItem bins = new JMenuItem("force bins");
                            disMode.add(bins);
                            bins.setActionCommand("bins");
                            bins.addActionListener(this);
                        }
                        if (force) {
                            JMenuItem auto = new JMenuItem("auto");
                            disMode.add(auto);
                            auto.setActionCommand("auto");
                            auto.addActionListener(this);
                        }
                        mode.add(disMode);

/*              JMenu weight = new JMenu("weight by");
              for(int i=0; i<data.k; i++) {
                if( !data.categorical(i) ) {
                  JCheckBoxMenuItem item = new JCheckBoxMenuItem(data.getName(i));
                  weight.add(item);
                  item.setActionCommand("weight"+i);
                  item.addActionListener(this);
                  if( i == weightVar )
                    item.setSelected(true);
                }
              }
              mode.add(weight);            */

                        JMenuItem invert = new JMenuItem("invert plot");
                        mode.add(invert);
                        invert.setActionCommand("invert");
                        invert.addActionListener(this);

                        JMenu conlines = new JMenu("add lines by");
                        JCheckBoxMenuItem off = new JCheckBoxMenuItem("no lines");
                        conlines.add(off);
                        off.setActionCommand("nobyvar");
                        if (byVar < 0)
                            off.setSelected(true);
                        off.addActionListener(this);
                        for (int i = 0; i < data.k; i++) {
                            JCheckBoxMenuItem item = new JCheckBoxMenuItem(data.getName(i));
                            conlines.add(item);
                            item.setActionCommand("byvar" + i);
                            item.addActionListener(this);
                            if (i == byVar)
                                item.setSelected(true);
                        }
                        mode.add(conlines);

                        mode.add(new JMenuItem("dismiss"));

                        mode.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            } else
                super.processMouseEvent(e);  // Pass other event types on.
        } else
            super.processMouseEvent(e);  // Pass other event types on.
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("Fixed") || command.equals("Free") || command.equals("axes") || command.equals("invert") || command.equals("none") || command.equals("ls-line") || command.equals("loess") || command.equals("splines") || command.equals("locfit") || command.equals("nobyvar") || command.substring(0, Math.min(5, command.length())).equals("byvar") || command.equals("points") || command.equals("bins") || command.equals("auto")) {
            if (command.equals("Fixed") || command.equals("Free")) {
                displayMode = command;
            } else if (command.equals("bins") || command.equals("points")) {
                modeString = command;
                force = true;
            } else if (command.equals("auto")) {
                force = false;
            } else if (command.equals("invert")) {
                if (invert)
                    invert = false;
                else
                    invert = true;
            } else if (command.equals("none") || command.equals("ls-line") || command.equals("loess") || command.equals("splines") || command.equals("locfit")) {
                smoothF = command;
                smoothChanged = true;
            } else if (command.equals("nobyvar")) {
                connectLines = false;
                byVar = -1;
            } else if (command.substring(0, Math.min(5, command.length())).equals("byvar")) {
                connectLines = true;
//System.out.println(" ........................ by var "+command.substring(5,command.length()));
                byVar = (int) Utils.atod(command.substring(5, command.length()));
            } else if (command.equals("axes")) {
                int tmp = Vars[1];
                Vars[1] = Vars[0];
                Vars[0] = tmp;

                tmp = roundX;
                roundX = roundY;
                roundY = tmp;

                for (int i = 0; i < Selections.size(); i++) {
                    Selection S = (Selection) Selections.elementAt(i);
                    Rectangle sr = S.r;
                    double tmp1 = ((floatRect) S.o).x1;
                    ((floatRect) S.o).x1 = ((floatRect) S.o).y2;
                    ((floatRect) S.o).y2 = tmp1;
                    tmp1 = ((floatRect) S.o).x2;
                    ((floatRect) S.o).x2 = ((floatRect) S.o).y1;
                    ((floatRect) S.o).y1 = tmp1;
                }
                rects.removeAllElements();
                // swap axes ...
                flipAxes();
            }

            scaleChanged = true;
            create();
            Graphics g = this.getGraphics();
            paint(g);
            g.dispose();
        } else if (command.equals("rougher") || command.equals("smoother") || command.equals("compare")) {
            if (command.equals("smoother")) {
                if (smoother > 1) {
                    smoother -= 1;
                    smoothChanged = true;
                }
            } else if (command.equals("rougher")) {
                if (smoother < 30) {
                    smoother += 1;
                    smoothChanged = true;
                }
            } else if (command.equals("compare")) {
                compareToAll = !compareToAll;
                smoothChanged = true;
            }
            paint(this.getGraphics());
        } else if (Utils.isNumber(command)) {
            System.out.println("Command: " + Double.valueOf(command));
            boolean hit = false;
            double dd = Double.parseDouble(command);
            for (int i = -100; i <= 100; i++)
                if (dd == i) {
                    hit = true;
                    if (i <= 0) {
                        alphap = -i;
                        alpha = alphas[alphap];
                    } else
                        radius = i;
                }
            if (hit) {
                alphaChanged = true;
                paint(this.getGraphics());
            } else
                super.actionPerformed(e);
        } else
            super.actionPerformed(e);
    }


    public void processMouseMotionEvent(MouseEvent e) {

        Graphics2D g = (Graphics2D) this.getGraphics();
        FontMetrics fm = bg.getFontMetrics();
        tttbg = (Graphics2D) tttbi.getGraphics();
        tttbg.drawImage(ttbi, 0, 0, null);

        drawSelections(ttbg);

        if ((e.getID() == MouseEvent.MOUSE_MOVED)) {

            if ((e.getModifiers() == ALT_DOWN)) {

                frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

                info = true;
                tttbg.setColor(ColorManager.backgroundColor);

                // Draw x-Label for CRTL_DOWN event
                int egetX = e.getX();

                if (egetX < userToWorldX(getLlx()))
                    egetX = (int) userToWorldX(getLlx());
                if (egetX > userToWorldX(getUrx()))
                    egetX = (int) userToWorldX(getUrx());

                double ratioX = (worldToUserX(egetX) - getLlx()) / (getUrx() - getLlx());
                int minWidth = fm.stringWidth(StatUtil.roundToString(getLlx(), roundX));
                int maxWidth = fm.stringWidth(StatUtil.roundToString(getUrx(), roundX));

                if (egetX <= userToWorldX(getLlx()) + minWidth + 4)
                    tttbg.fillRect((int) userToWorldX(getLlx()), (int) userToWorldY(getLly()) + outside + tick + 1,
                            minWidth + 4, fm.getMaxAscent() + fm.getMaxDescent());
                if (egetX >= userToWorldX(getUrx()) - maxWidth - 4)
                    tttbg.fillRect((int) userToWorldX(getUrx()) - maxWidth - 4, (int) userToWorldY(getLly()) + outside + tick + 1,
                            maxWidth + 4, fm.getMaxAscent() + fm.getMaxDescent());

                tttbg.setColor(ColorManager.lineColor);

                tttbg.drawLine(egetX, (int) userToWorldY(getLly()) + outside,
                        egetX, (int) userToWorldY(getLly()) + outside + tick);
                tttbg.drawString(StatUtil.roundToString(worldToUserX(egetX), roundX),
                        egetX - fm.stringWidth(StatUtil.roundToString(worldToUserX(egetX), roundX)) / 2
                                - (int) (fm.stringWidth(StatUtil.roundToString(worldToUserX(egetX), roundX)) *
                                (ratioX - 0.5)),
                        (int) userToWorldY(getLly()) + outside + tick + fm.getMaxAscent() + fm.getMaxDescent());

                // Draw y-Label for CRTL_DOWN event
                int egetY = e.getY();
                // Attention: Y-axis is head to toe!
                if (egetY < userToWorldY(getUry()))
                    egetY = (int) userToWorldY(getUry());
                if (egetY > userToWorldY(getLly()))
                    egetY = (int) userToWorldY(getLly());

                double ratioY = (worldToUserY(egetY) - getLly()) / (getUry() - getLly());
                minWidth = fm.stringWidth(StatUtil.roundToString(getUry(), roundY));
                maxWidth = fm.stringWidth(StatUtil.roundToString(getLly(), roundY));

                tttbg.setColor(ColorManager.backgroundColor);
                if (egetY < userToWorldY(getUry()) + minWidth + 4)
                    tttbg.fillRect(0, (int) userToWorldY(getUry()),
                            (int) userToWorldX(getLlx()) - outside - tick, minWidth + 4);
                if (egetY > userToWorldY(getLly()) - maxWidth - 4)
                    tttbg.fillRect(0, (int) userToWorldY(getLly()) - maxWidth - 4,
                            (int) userToWorldX(getLlx()) - outside - tick, maxWidth + 4);

                // Fadenkreuz
                tttbg.setColor(Color.lightGray);
                tttbg.drawLine(egetX - outside, egetY,
                        (int) userToWorldX(getLlx()), egetY);
                tttbg.drawLine(egetX, egetY + outside,
                        egetX, (int) userToWorldY(getLly()));

                tttbg.setColor(ColorManager.lineColor);
                tttbg.drawLine((int) userToWorldX(getLlx()) - outside, egetY,
                        (int) userToWorldX(getLlx()) - outside - tick, egetY);
                tttbg.rotate(-Math.PI / 2);
                tttbg.drawString(StatUtil.roundToString(worldToUserY(egetY), roundY),
                        (int) (-egetY - ratioY * fm.stringWidth(StatUtil.roundToString(worldToUserY(egetY), roundY))),
                        (int) userToWorldY(getUry()) - fm.getMaxAscent() - tick + 1 + (xShift - yShift));
                tttbg.rotate(Math.PI / 2);
                g.drawImage(tttbi, 0, 0, Color.black, null);
                tttbg.dispose();
            } else {
                if (info) {
                    frame.setCursor(Cursor.getDefaultCursor());
                    paint(this.getGraphics());
                    info = false;
                }
            }
        }
        super.processMouseMotionEvent(e);  // Pass other event types on.
    }


    public void processKeyEvent(KeyEvent e) {

        if (e.getID() == KeyEvent.KEY_PRESSED && (e.getKeyCode() == KeyEvent.VK_UP
                || e.getKeyCode() == KeyEvent.VK_DOWN
                || e.getKeyCode() == KeyEvent.VK_UP && e.isShiftDown()
                || e.getKeyCode() == KeyEvent.VK_DOWN && e.isShiftDown()
                || e.getKeyCode() == KeyEvent.VK_LEFT
                || e.getKeyCode() == KeyEvent.VK_RIGHT)) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN && !e.isShiftDown()) {
                if (radius > 1) {
                    radius -= 2;
                    scaleChanged = true;
                } else
                    return;
            }
            if (e.getKeyCode() == KeyEvent.VK_UP && !e.isShiftDown()) {
                if (radius < width / 2) {
                    radius += 2;
                    scaleChanged = true;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (alphap > 0) {
                    alpha = alphas[--alphap];
                    alphaChanged = true;
                } else
                    return;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if (alphap < alphas.length - 1) {
                    alpha = alphas[++alphap];
                    alphaChanged = true;
                } else
                    return;
            }
            if (e.getKeyCode() == KeyEvent.VK_UP && e.isShiftDown()) {
                if (smoother < 30) {
                    smoother += 1;
                    smoothChanged = true;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_DOWN && e.isShiftDown()) {
                if (smoother > 1) {
                    smoother -= 1;
                    smoothChanged = true;
                }
            }
            paint(this.getGraphics());
        }
        super.processKeyEvent(e);  // Pass other event types on.
    }


    public void paint(Graphics2D g) {

        frame.setCursor(Cursor.getDefaultCursor());

        int pF = 1;
        if (printing)
            pF = printFactor;

        if (displayMode.equals("Fixed"))
            setAspect(1);
        else
            setAspect(-1);

        Dimension size = this.getSize();

        if (oldWidth != size.width || oldHeight != size.height || scaleChanged || frame.getBackground() != ColorManager.backgroundColor) {
            frame.setBackground(ColorManager.backgroundColor);
            this.width = size.width;
            this.height = size.height;

            // dispose old background image after size changed
            if (bg != null) {
                System.out.println("Dispose BG");
                bg.dispose();
                bg = null;
            }
            updateScale();
            int num = 0;
            for (int i = 0; i < data.n; i++)           // Check how many points we need to render !
                if (xVal[i] >= getLlx() && xVal[i] < getUrx() && yVal[i] >= getLly() && yVal[i] < getUry())
                    num++;
            if (!force)
                if (num > GraphicsPerformance.getPerformance(frame)) {
                    modeString = "bins";
                } else
                    modeString = "points";

//      size = this.getSize();
            for (int i = 0; i < Selections.size(); i++) {
                Selection S = (Selection) Selections.elementAt(i);
                S.r.x = (int) userToWorldX(((floatRect) S.o).x1);
                S.r.y = (int) userToWorldY(((floatRect) S.o).y1);
                S.r.width = (int) userToWorldX(((floatRect) S.o).x2) - (int) userToWorldX(((floatRect) S.o).x1);
                S.r.height = (int) userToWorldY(((floatRect) S.o).y2) - (int) userToWorldY(((floatRect) S.o).y1);
            }

            oldWidth = size.width;
            oldHeight = size.height;
            scaleChanged = false;
        }

        long start = new Date().getTime();

        if (bg == null || alphaChanged || printing) {

            Graphics2D fg;
            if (printing) {
//        System.out.println("Setting Graphics for Printing");
                bg = g;
                fg = g;
                ttbg = g;
                tttbg = g;
            } else {
                fi = createImage(size.width, size.height);
                bi = createImage(size.width, size.height);
                tbi = createImage(size.width, size.height);
                ttbi = createImage(size.width, size.height);
                tttbi = createImage(size.width, size.height);
                fg = (Graphics2D) fi.getGraphics();
                bg = (Graphics2D) bi.getGraphics();
                ttbg = (Graphics2D) ttbi.getGraphics();
                tttbg = (Graphics2D) tttbi.getGraphics();
            }
            FontMetrics fm = bg.getFontMetrics();

            border = border * pF;

            if (!alphaChanged)
                create();
            else
                alphaChanged = false;

            bg.setColor(ColorManager.lineColor);
            if (invert) {
//        Properties p = new Properties(System.getProperties());
//        p.setProperty("com.apple.macosx.AntiAliasedGraphicsOn", "false");
//        System.setProperties(p);

                fg.setColor(Color.gray);
                fg.fillRect(0, 0, size.width, size.height);
                fg.setColor(Color.white);
            }
            Graphics2D pg;
            Image ti;
            if (modeString.equals("points")) {
                if (invert)
                    pg = fg;
                else
                    pg = bg;
                pg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) alpha / 100)));

                Color[] cols = new Color[1];
                if (data.colorBrush) {
                    cols = new Color[data.getNumColors()];
                    for (int j = 0; j < data.getNumColors(); j++)
                        cols[j] = data.getColorByID(j);
                }
                for (int i = 0; i < data.n; i++) {
                    if (data.colorBrush)
                        if (data.colorArray[i] > 0)
                            pg.setColor(cols[data.colorArray[i]]);
                        else
                            pg.setColor(ColorManager.lineColor);
                    if (xVal[i] >= getLlx() && xVal[i] <= getUrx() && yVal[i] >= getLly() && yVal[i] <= getUry())
                        pg.fillOval((int) userToWorldX(xVal[i]) - (radius * pF - 1) / 2, (int) userToWorldY(yVal[i]) - (radius * pF - 1) / 2, radius * pF, radius * pF);
                }

                if (invert) {
                    media.addImage(bi, 0);
                    try {
                        media.waitForID(0);
                        ti = Utils.makeColorTransparent(fi, Color.gray);
                        bg.drawImage(ti, 0, 0, Color.black, null);
                    }
                    catch (InterruptedException ignored) {
                    }
                    pg.dispose();
                }
            } else {
                for (int i = 0; i < rects.size(); i++) {
                    MyRect r = (MyRect) rects.elementAt(i);
//          bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    if (invert) {
                        bg.setColor(Color.black);
                        bg.fillRect(r.x, r.y, r.w, r.h);
                        r.setColor(Color.white);
                    } else {
                        bg.setColor(Color.white);
                        bg.fillRect(r.x, r.y, r.w, r.h);
                        r.setColor(Color.black);
                    }
                    bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) Math.min(1, r.obs / 100 * alpha))));
                    r.draw(bg);
                    r.setColor(Color.black);
                }
            }
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            bg.setColor(ColorManager.lineColor);

            // x axis
            bg.drawLine((int) userToWorldX(getLlx()), (int) userToWorldY(getLly()) + outside * pF,
                    (int) userToWorldX(getUrx()), (int) userToWorldY(getLly()) + outside * pF);
            // x-ticks
            bg.drawLine((int) userToWorldX(getLlx()), (int) userToWorldY(getLly()) + outside * pF,
                    (int) userToWorldX(getLlx()), (int) userToWorldY(getLly()) + outside * pF + tick * pF);

            bg.drawLine((int) userToWorldX(getUrx()), (int) userToWorldY(getLly()) + outside * pF,
                    (int) userToWorldX(getUrx()), (int) userToWorldY(getLly()) + outside * pF + tick * pF);

            bg.drawString(StatUtil.roundToString(getLlx(), roundX),
                    (int) userToWorldX(getLlx()),
                    (int) userToWorldY(getLly()) + outside * pF + tick * pF + fm.getMaxAscent() + fm.getMaxDescent());

            bg.drawString(StatUtil.roundToString(getUrx(), roundX),
                    (int) userToWorldX(getUrx()) - fm.stringWidth(StatUtil.roundToString(getUrx(), roundX)),
                    (int) userToWorldY(getLly()) + outside * pF + tick * pF + fm.getMaxAscent() + fm.getMaxDescent());

            // y-axis
            bg.drawLine((int) userToWorldX(getLlx()) - outside * pF, (int) userToWorldY(getLly()),
                    (int) userToWorldX(getLlx()) - outside * pF, (int) userToWorldY(getUry()));
            // y-ticks
            bg.drawLine((int) userToWorldX(getLlx()) - outside * pF, (int) userToWorldY(getLly()),
                    (int) userToWorldX(getLlx()) - outside * pF - tick * pF, (int) userToWorldY(getLly()));

            bg.drawLine((int) userToWorldX(getLlx()) - outside * pF, (int) userToWorldY(getUry()),
                    (int) userToWorldX(getLlx()) - outside * pF - tick * pF, (int) userToWorldY(getUry()));

            bg.rotate(-Math.PI / 2);
            bg.drawString(StatUtil.roundToString(getLly(), roundY),
                    -(int) userToWorldY(getLly()),
                    (int) userToWorldY(getUry()) - fm.getMaxAscent() - tick * pF + pF + (xShift - yShift) * pF);
            bg.drawString(StatUtil.roundToString(getUry(), roundY),
                    -(int) userToWorldY(getUry()) - fm.stringWidth(StatUtil.roundToString(getUry(), roundY)),
                    (int) userToWorldY(getUry()) - fm.getMaxAscent() - tick * pF + pF + (xShift - yShift) * pF);
            bg.rotate(Math.PI / 2);
        } // end, new background graphics

        Graphics tbg;
        if (!printing) {
            tbg = tbi.getGraphics();
            tbg.drawImage(bi, 0, 0, Color.black, null);
        } else
            tbg = g;


        tbg.setColor(DragBox.hiliteColor);

        if (modeString.equals("points")) {

            double[] selection = data.getSelection();

            // add lines by third variable
            if (connectLines) {

                tbg.setColor(DragBox.hiliteColor);
                double[] byVal = data.getRawNumbers(byVar);
                for (int i = 1; i < data.n; i++) {
                    if (selection[i] > 0) {
                        int j = i - 1;
                        while (j > 0 && (byVal[i] != byVal[j] || selection[j] == 0))
                            j--;
                        if (byVal[i] == byVal[j] && selection[j] > 0)
                            tbg.drawLine((int) userToWorldX(xVal[j]),
                                    (int) userToWorldY(yVal[j]),
                                    (int) userToWorldX(xVal[i]),
                                    (int) userToWorldY(yVal[i]));
                    }
                }
            }

/*      for( int i=0; i<data.n; i++) {
  if( xVal[i]>=getLlx() && xVal[i]<=getUrx() && yVal[i]>=getLly() && yVal[i]<=getUry() )
    if( selection[i] > 0 ) {
      tbg.setColor(ColorManager.backgroundColor);
      tbg.fillOval( (int)userToWorldX( xVal[i] )-(radius-1)/2, (int)userToWorldY( yVal[i] )-(radius-1)/2, radius*pF, radius*pF);
    }
}*/
            tbg.setColor(DragBox.hiliteColor);
            if (frame.getAlphaHi())
                ((Graphics2D) tbg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) Math.pow((float) alpha / 100, 0.75))));

            for (int i = 0; i < data.n; i++) {
                if (xVal[i] >= getLlx() && xVal[i] <= getUrx() && yVal[i] >= getLly() && yVal[i] <= getUry())
                    if (selection[i] > 0) {
                        tbg.fillOval((int) userToWorldX(xVal[i]) - (radius - 1) / 2, (int) userToWorldY(yVal[i]) - (radius - 1) / 2, radius * pF, radius * pF);
                    }
            }
        } else {
            for (int i = 0; i < rects.size(); i++) {
                MyRect r = (MyRect) rects.elementAt(i);
                bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) Math.min(1, r.obs / 100 * alpha))));
                int id = ((Integer) (r.tileIds.elementAt(0))).intValue();
                double weight;
                if ((weight = binning.getSelected(id)) > 0) {
                    r.setHilite(weight);
                    r.draw(tbg);
                }
            }
        }

        if (!printing)
            ttbg.drawImage(tbi, 0, 0, Color.black, null);

        if (smoothChanged || true) { // add regression lines
            smoothChanged = false;
            ttbg.setColor(Color.black);

            if (smoothF.equals("ls-line")) {
                if (compareToAll)
                    coeffs = data.regress(Vars[0], Vars[1], false);
                else
                    coeffs = data.regress(Vars[0], Vars[1], true);
                xMin = data.getMin(Vars[0]);
                xMax = data.getMax(Vars[0]);
                ttbg.setColor(ColorManager.lineColor);
                ttbg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(xMin * coeffs[1] + coeffs[0]),
                        (int) userToWorldX(xMax), (int) userToWorldY(xMax * coeffs[1] + coeffs[0]));
            }

            if (smoothF.equals("loess") || smoothF.equals("splines") || smoothF.equals("locfit")) {

                try {
                    RConnection c = new RConnection();
                    if (smoothF.equals("splines"))
                        c.voidEval("library(splines)");
                    if (smoothF.equals("locfit"))
                        c.voidEval("library(locfit)");

                    c.assign("x", data.getRawNumbers(Vars[0]));
                    c.assign("y", data.getRawNumbers(Vars[1]));

                    c.assign("sel", data.getSelection());               // exclude selected from the base smoother!!! This is a new paradigm!

                    if (compareToAll)
                        c.voidEval("ids <- x<1e300&y<1e300");
                    else
                        c.voidEval("ids <- x<1e300&y<1e300&sel==0");
                    c.voidEval("x<-x[ids]");
                    c.voidEval("y<-y[ids]");
                    double[] range = new double[2];
                    range = c.eval("range(x)").asDoubles();
                    double xSMin = range[0];
                    double xSMax = range[1];
                    double[] xForFit = new double[200 + 1];
                    double step = (xSMax - xSMin) / 200;
                    for (int f = 0; f < 200 + 1; f++)
                        xForFit[f] = xSMin + step * (double) f;
                    c.assign("xf", xForFit);

                    double[] fitted = {0};
                    double[] CIl = {0};
                    double[] CIu = {0};
                    if (smoothF.equals("loess"))
//            fitted = c.eval("predict(lowess(x, y, f=1/"+smoother+"), data.frame(x=xf))").asDoubleArray();
                        fitted = c.eval("predict(loess(y ~ x, span=3.75/" + smoother + ", degree = 1, family = \"symmetric\", control = loess.control(iterations=3)), data.frame(x=xf))").asDoubles();
                    if (smoothF.equals("locfit")) {
                        RList sL = c.eval("sL <- preplot(locfit.raw(x, y, alpha=3.5/" + smoother + "), xf, band=\"global\")").asList();
                        fitted = sL.at("fit").asDoubles();
                        CIl = new double[fitted.length];
                        CIu = new double[fitted.length];
                        double[] se = sL.at("se.fit").asDoubles();
                        for (int f = 0; f <= 200; f++) {
                            CIl[f] = fitted[f] - se[f];
                            CIu[f] = fitted[f] + se[f];
                        }
                    }
//            fitted = c.eval("predict(locfit(y~x), data.frame(x=xf))").asDoubleArray();
                    if (smoothF.equals("splines")) {
                        c.voidEval("sP <- predict(lm(y~ns(x," + smoother + ")), interval=\"confidence\", data.frame(x=xf))");
                        fitted = c.eval("sP[,1]").asDoubles();
                        CIl = c.eval("sP[,2]").asDoubles();
                        CIu = c.eval("sP[,3]").asDoubles();
                    }
                    ttbg.setColor(ColorManager.lineColor);
                    if (smoothF.equals("splines") || smoothF.equals("locfit")) {
                        Polygon CI = new Polygon();
                        for (int f = 0; f < 200 + 1; f++) {
                            CI.addPoint((int) userToWorldX(xSMin + step * (double) f), (int) userToWorldY(CIl[f]));
                        }
                        for (int f = 200; f >= 0; f--) {
                            CI.addPoint((int) userToWorldX(xSMin + step * (double) f), (int) userToWorldY(CIu[f]));
                        }
                        ttbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.25)));
                        ttbg.fillPolygon(CI);
                        ttbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 1.0)));
                    }

                    for (int f = 0; f < 200; f++) {
                        ttbg.drawLine((int) userToWorldX(xSMin + step * (double) f), (int) userToWorldY(fitted[f]),
                                (int) userToWorldX(xSMin + step * (double) (f + 1)), (int) userToWorldY(fitted[f + 1]));
//            System.out.println(fitted[f]+" / "+fitted[f+1]);
                    }

                    c.close();
                } catch (RserveException rse) {
                    System.out.println("Rserve exception: " + rse.getMessage());
                }
                catch (REXPMismatchException mme) {
                    System.out.println("Mismatch exception : " + mme.getMessage());
                }
                catch (REngineException ren) {
                    System.out.println("REngine exception : " + ren.getMessage());
                }
            }
            int nSel = data.countSelection();
            if (nSel > 1) {
                ttbg.setColor(DragBox.hiliteColor);
                if (smoothF.equals("ls-line")) {
                    selCoeffs = data.selRegress(Vars[0], Vars[1]);
                    System.out.println("Coeffs: " + selCoeffs[0] + "  " + selCoeffs[1]);
                    if (!Double.isNaN(selCoeffs[0]) && !Double.isNaN(selCoeffs[1]))
                        ttbg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(xMin * selCoeffs[1] + selCoeffs[0]),
                                (int) userToWorldX(xMax), (int) userToWorldY(xMax * selCoeffs[1] + selCoeffs[0]));
                }
                if (smoothF.equals("loess") || smoothF.equals("splines") || smoothF.equals("locfit")) {
                    try {
                        RConnection c = new RConnection();
                        if (smoothF.equals("splines"))
                            c.voidEval("library(splines)");
                        if (smoothF.equals("locfit"))
                            c.voidEval("library(locfit)");

                        double[] selX = new double[nSel];
                        double[] selY = new double[nSel];
                        double[] selection = data.getSelection();
                        int k = 0;
                        for (int i = 0; i < data.n; i++)
                            if (selection[i] > 0) {           // && xVal[i]<Double.MAX_VALUE && yVal[i]<Double.MAX_VALUE
                                selX[k] = xVal[i];
                                selY[k] = yVal[i];
                                k++;
                            }

                        if (k > 0) {                           // Not all selected cases are missing in at least one dimension
                            c.assign("x", selX);
                            c.assign("y", selY);

                            c.voidEval("ids <- x<1e300&y<1e300");
                            c.voidEval("x<-x[ids]");
                            c.voidEval("y<-y[ids]");

                            double xSelMin = Double.MAX_VALUE;
                            double xSelMax = Double.MIN_VALUE;

                            if (!data.categorical(Vars[0])) {
                                xSelMin = data.getSelQuantile(Vars[0], 0.0);
                                xSelMax = data.getSelQuantile(Vars[0], 1.0);
                            } else {
                                xSelMin = data.getSelMin(Vars[0]);
                                xSelMax = data.getSelMax(Vars[0]);
                            }

                            double[] xForFit = new double[200 + 1];
                            double step = (xSelMax - xSelMin) / 200;
                            for (int f = 0; f < 200 + 1; f++)
                                xForFit[f] = xSelMin + step * (double) f;
                            c.assign("xf", xForFit);

                            double[] fitted = {0};
                            double[] CIl = {0};
                            double[] CIu = {0};
                            if (smoothF.equals("loess"))
                                fitted = c.eval("predict(loess(y~x, span=3.75/" + smoother + ", family = \"symmetric\", control = loess.control(iterations=3)), data.frame(x=xf))").asDoubles();
                            if (smoothF.equals("locfit")) {
                                RList sL = c.eval("sL <- preplot(locfit.raw(x, y, alpha=3.5/" + smoother + "), xf, band=\"global\")").asList();
                                fitted = sL.at("fit").asDoubles();
                                CIl = new double[fitted.length];
                                CIu = new double[fitted.length];
                                double[] se = sL.at("se.fit").asDoubles();
                                for (int f = 0; f <= 200; f++) {
                                    CIl[f] = fitted[f] - se[f];
                                    CIu[f] = fitted[f] + se[f];
                                }
                            }
                            //						fitted = c.eval("predict(locfit(y~x), data.frame(x=xf))").asDoubleArray();
                            if (smoothF.equals("splines")) {
                                c.voidEval("sP <- predict(lm(y~ns(x," + smoother + ")), interval=\"confidence\", data.frame(x=xf))");
                                fitted = c.eval("sP[,1]").asDoubles();
                                CIl = c.eval("sP[,2]").asDoubles();
                                CIu = c.eval("sP[,3]").asDoubles();
                            }
                            if (smoothF.equals("splines") || smoothF.equals("locfit")) {
                                Polygon CI = new Polygon();
                                for (int f = 0; f <= 200; f++) {
                                    CI.addPoint((int) userToWorldX(xSelMin + step * (double) f), (int) userToWorldY(CIl[f]));
                                }
                                for (int f = 200; f >= 0; f--) {
                                    CI.addPoint((int) userToWorldX(xSelMin + step * (double) f), (int) userToWorldY(CIu[f]));
                                }
                                ttbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.25)));
                                ttbg.fillPolygon(CI);
                                ttbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 1.0)));
                            }

                            for (int f = 0; f < 200; f++)
                                ttbg.drawLine((int) userToWorldX(xSelMin + step * (double) f), (int) userToWorldY(fitted[f]),
                                        (int) userToWorldX(xSelMin + step * (double) (f + 1)), (int) userToWorldY(fitted[f + 1]));
                        }
                        c.close();
                    } catch (RserveException rse) {
                        System.out.println("Rserve exception: " + rse.getMessage());
                    }
                    catch (REXPMismatchException mme) {
                        System.out.println("Mismatch exception : " + mme.getMessage());
                    }
                    catch (REngineException ren) {
                        System.out.println("REngine exception : " + ren.getMessage());
                    }
                }
            }
        }

        ttbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
        ttbg.setColor(Color.black);
        if (!printing) {
            ttbg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
            drawSelections(ttbg);
            g.setColor(Color.black);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
            g.drawImage(ttbi, 0, 0, Color.black, null);
            tbg.dispose();
//      ttbg.dispose();
        }

        long stop = new Date().getTime();
        //System.out.println("Time for points: "+(stop-start)+"ms");
    }


    public void drawSelections(Graphics g) {

        for (int i = 0; i < Selections.size(); i++) {
            Selection S = (Selection) Selections.elementAt(i);
            drawBoldDragBox(g, S);
        }
    }


    public void adjustmentValueChanged(AdjustmentEvent e) {
    }


    public void scrollTo(int id) {
    }


    class floatRect {

        double x1, y1, x2, y2;


        public floatRect(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }


    public void create() {

        if (rects.size() == 0) {

            xVal = data.getRawNumbers(Vars[0]);
            yVal = data.getRawNumbers(Vars[1]);

            setName("Scatterplot(x: " + data.getName(Vars[0]) + " y: " + data.getName(Vars[1]) + ")");
            //(15-radius)*15
            binning = data.discretize2D("Dummy", Vars[0], getLlx(), getUrx() + 0.01 * (getUrx() - getLlx()), width / radius,
                    Vars[1], getLly(), getUry() + 0.01 * (getUry() - getLly()), width / radius);
        } else {
            binning.update2DBins(getLlx(), getUrx() + 0.01 * (getUrx() - getLlx()), width / radius,
                    getLly(), getUry() + 0.01 * (getUry() - getLly()), width / radius);
        }

        //System.out.println(getLlx()+ " - " +getUrx()+ " - " + getLly()+ " - " +getUry());
        //binning.print();

        rects.removeAllElements();
        int X = (int) userToWorldX(getLlx());
        int nextX, Y;
        for (int i = 0; i < binning.levels[0]; i++) {
            nextX = (int) userToWorldX(getLlx() + (i + 1) * (getUrx() - getLlx()) / (width / radius));
            int lastY = (int) userToWorldY(getLly());
            for (int j = 0; j < binning.levels[1]; j++) {
                int index = i * (binning.levels[1]) + j;
                Vector tileIds = new Vector(1, 0);
                tileIds.addElement(index);
                Y = (int) userToWorldY(getLly() + (j + 1) * (getUry() - getLly()) / (width / radius));
                if (binning.table[index] > 0)
                    rects.addElement(new MyRect(true, 'f', "Observed", X, Y, nextX - X, lastY - Y, binning.table[index],
                            binning.table[index], 1.0, 0.0, binning.names[0] + ": " + binning.lnames[0][i] + "\n" + binning.names[1] + ": " + binning.lnames[1][j] + '\n', tileIds, binning));
                lastY = Y;
            }
            X = nextX;
        }
//    System.out.println(" Llx: "+getLlx()+" Lly: "+getLly());
//    System.out.println(" Num Bins: "+(binning.levels[0]*binning.levels[1])+" Num Tiles: "+rects.size());
    }
}

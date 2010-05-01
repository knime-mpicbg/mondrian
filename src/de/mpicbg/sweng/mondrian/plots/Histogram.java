package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.core.*;
import de.mpicbg.sweng.mondrian.io.db.Query;
import de.mpicbg.sweng.mondrian.plots.basic.Axis;
import de.mpicbg.sweng.mondrian.plots.basic.MyRect;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
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
import java.awt.event.*;
import java.util.StringTokenizer;
import java.util.Vector;


public class Histogram extends DragBox implements ActionListener {

    private Vector rects = new Vector(256, 0);                // Store the tiles.
    private Vector labels = new Vector(256, 0);               // Store the labels.
    private int width, height, realHeight, startX;           // The preferred size.
    protected int oldWidth, oldHeight;                       // The last size for constructing the bars.
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private int outside = 5;
    private int tick = 5;
    private double bStart, bWidth;            // Anker and Width of the Bins
    private Table tablep;                                    // The datatable to deal with.
    private Image bi, tbi;
    private Graphics2D bg, tbg;
    private int k;
    public String displayMode = "Histogram";
    public boolean densityMode = false;
    public boolean scaleSelD = true;
    public boolean CDPlot = false;
    private DataSet data;
    private double[] add;
    private double totalSum = 0;
    private int weight;
    private int dvar;
    private int round;                    // percision for labels ...
    private boolean coordsSet = false;
    private boolean info = false;
    private int eventID;


    public Histogram(MFrame frame, int width, int height, Table tablep, double bStart, double bWidth, int weight) {
        super(frame);
        this.tablep = tablep;
        this.name = tablep.name;
        this.levels = tablep.levels;
        this.names = tablep.names;
        this.lnames = tablep.lnames;
        this.bStart = bStart;
        this.bWidth = bWidth;
        this.width = width;
        this.height = height;
        this.weight = weight;

        frame.getContentPane().add(this);

        border = 20;
        yShift = -10;

        data = tablep.data;
        dvar = tablep.initialVars[0];
        this.k = levels[0];
        round = (int) Math.max(0, 3 - Math.round((Math.log(data.getMax(dvar) - data.getMin(dvar)) / Math.log(10))));

        Font SF = new Font("SansSerif", Font.PLAIN, 11);
        frame.setFont(SF);

        String titletext;
        if (weight == -1)
            titletext = "Histogram(" + names[0] + ")";
        else
            titletext = "Histogram(" + names[0] + "|" + data.getName(weight) + ")";

        setName(titletext);

        //this.setBackground(new Color(255, 255, 152));

        xMin = tablep.data.getMin(tablep.initialVars[0]);
        xMax = tablep.data.getMax(tablep.initialVars[0]);
        double range = xMax - xMin;
        yMin = 0;
        yMax = 1 / range * 4.25;

        if (rects.size() == 0) {
            setCoordinates(xMin - range * 0.05, yMin, xMax + range * 0.05, yMax, -1);
//      System.out.println("Vor:  xMin: "+(xMin-range*0.05)+" yMin: "+ yMin +" xMax: "+(xMax+range*0.05)+" yMax: "+ yMax +" "+ -1);
            coordsSet = false;
//      System.out.println("Nach: xMin: "+this.getLlx()+" yMin: "+ yMin +" xMax: "+this.getUrx()+" yMax: "+ yMax +" "+ -1);
//      setCoordinates(this.getLlx(), yMin, this.getUrx(), yMax, -1);
        }

        // We use low-level events, so we must specify
        // which events we are interested in.
        this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.requestFocus();

        evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();
    }


    public void addDataListener(DataListener l) {
        listener = l;
    }


    public void processEvent(AWTEvent evt) {
        if (evt instanceof DataEvent) {
            if (listener != null)
                listener.dataChanged(eventID);
        } else super.processEvent(evt);
    }


    public void maintainSelection(Selection S) {

        Rectangle sr = S.r;
        int mode = S.mode;

        S.o = new floatRect(worldToUserX(S.r.x),
                worldToUserY(S.r.y),
                worldToUserX(S.r.x + S.r.width),
                worldToUserY(S.r.y + S.r.height));

        S.condition = new Query();
        for (int i = 0; i < rects.size(); i++) {
            StringTokenizer interval = new StringTokenizer(tablep.lnames[0][i].substring(1, tablep.lnames[0][i].length() - 1), ",");
            MyRect r = (MyRect) rects.elementAt(i);
            if (r.intersects(sr)) {
                S.condition.addCondition("OR", tablep.names[0] + " >= " + interval.nextToken() + " AND " + tablep.names[0] + " < " + interval.nextToken());
                if (tablep.data.isDB)
                    tablep.getSelection();
                else {
                    double sum = 0, sumh = 0;
                    for (int j = 0; j < r.tileIds.size(); j++) {
                        int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                        tablep.setSelection(id, 1, mode);
                        sumh += tablep.getSelected(id) * tablep.table[id];
                        sum += tablep.table[id];
                    }
                    r.setHilite(sumh / sum);
                }
            } else if (!tablep.data.isDB)
                for (int j = 0; j < r.tileIds.size(); j++) {
                    int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                    tablep.setSelection(id, 0, mode);
                }
        }
        for (int i = 0; i < data.n; i++)
            if ((data.getMissings(dvar))[i])
                data.setSelection(i, 0, mode);
    }


    public void updateSelection() {
        paint(this.getGraphics());
    }


    public void paint(Graphics2D g) {

        frame.setBackground(ColorManager.backgroundColor);

        tablep.getSelection();

//      Dimension size = this.getViewportSize();
        Dimension size = this.getSize();

        if (oldWidth != size.width || oldHeight != size.height) {
            this.width = size.width;
            this.height = size.height;
//        updateScale();
            create(border, border, size.width - border, size.height - border, "");
//        this.setSize( size.width, size.height);
            size = this.getSize();
            oldWidth = size.width;
            oldHeight = size.height;
        }

        if (scaleChanged) {
//        updateScale();
            create(border, border, size.width - border, size.height - border, "");
        }

        if (printing) {
            bg = g;
            tbg = g;
        } else {
            if (bi != null) {
                if (bi.getWidth(null) != size.width || bi.getHeight(null) != size.height) {
                    bg.dispose();
                    bi = null;
                    bi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
                    tbi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
                }
            } else {
                bi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
                tbi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
            }
            bg = (Graphics2D) bi.getGraphics();
            tbg = (Graphics2D) bi.getGraphics();
            bg.clearRect(0, 0, size.width, size.height);
        }
        FontMetrics fm = bg.getFontMetrics();

        outside = height / 65;
        if (!printing) {
            outside = Math.min(outside, 6);
            outside = Math.max(outside, 2);
        }
        tick = outside;

        bg.setColor(ColorManager.lineColor);

        // x-axis
        bg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(0) + outside,
                (int) userToWorldX(xMax), (int) userToWorldY(0) + outside);
        // x-ticks
        bg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(0) + outside,
                (int) userToWorldX(xMin), (int) userToWorldY(0) + outside + tick);

        bg.drawLine((int) userToWorldX(xMax), (int) userToWorldY(0) + outside,
                (int) userToWorldX(xMax), (int) userToWorldY(0) + outside + tick);

        bg.drawString(StatUtil.roundToString(xMin, round),
                (int) userToWorldX(xMin),
                (int) userToWorldY(0) + outside + tick + fm.getMaxAscent() + fm.getMaxDescent());

        bg.drawString(StatUtil.roundToString(xMax, round),
                (int) userToWorldX(xMax) - fm.stringWidth(StatUtil.roundToString(xMax, round)),
                (int) userToWorldY(0) + outside + tick + fm.getMaxAscent() + fm.getMaxDescent());

        if (CDPlot) {
            // y-axis
            bg.drawLine((int) userToWorldX(xMin) - outside, (int) userToWorldY(0),
                    (int) userToWorldX(xMin) - outside, (int) userToWorldY(yMax));
            // y-ticks
            bg.drawLine((int) userToWorldX(xMin) - outside - tick, (int) userToWorldY(0),
                    (int) userToWorldX(xMin) - outside, (int) userToWorldY(0));

            bg.drawLine((int) userToWorldX(xMin) - outside - tick, (int) userToWorldY(yMax),
                    (int) userToWorldX(xMin) - outside, (int) userToWorldY(yMax));

            bg.drawString("0.0",
                    (int) userToWorldX(xMin) - outside - tick - fm.stringWidth("0.0"),
                    (int) userToWorldY(0));

            bg.drawString("1.0",
                    (int) userToWorldX(xMin) - outside - tick - fm.stringWidth("1.0"),
                    (int) userToWorldY(yMax) + fm.getMaxAscent());
            // Grid Lines
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.5)));
            bg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(yMax / 2),
                    (int) userToWorldX(xMax), (int) userToWorldY(yMax / 2));
            bg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(yMax),
                    (int) userToWorldX(xMax), (int) userToWorldY(yMax));
        }

        if (densityMode)
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.3)));
        if (CDPlot)
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.1)));

//      System.out.println("Density: "+densityMode+" CDPlot: "+CDPlot);

        boolean stillEmpty = true;               // Flag to avoid heading empty bins
        for (int i = 0; i < levels[0]; i++) {
            MyRect r = (MyRect) rects.elementAt(i);
            double sum = 0, sumh = 0;
            for (int j = 0; j < r.tileIds.size(); j++) {
                int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                sumh += tablep.getSelected(id) * tablep.table[id];
                sum += add[id];
            }
            if (sum > 0)
                stillEmpty = false;
            if (!stillEmpty) {
                r.setHilite(sumh / add[i]);
                r.draw(bg);
            }
        }

        if (densityMode) {
            bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 1.0)));

            try {
                RConnection c = new RConnection();
                double[] xVal = data.getRawNumbers(tablep.initialVars[0]);
                double[] weights = {1.0};
                double[] copyW = {1.0};
                boolean[] missW = {true};
                if (weight > -1) {
                    weights = data.getNumbers(weight);
                    copyW = new double[data.getN(dvar)];
                    missW = data.getMissings(weight);
                }
                double[] copyVal = new double[data.getN(dvar)];
                boolean[] missing = data.getMissings(dvar);
                int k = 0;
                for (int i = 0; i < data.n; i++)
                    if (!missing[i]) {
                        copyVal[k++] = xVal[i];
                        if (weight > -1)
                            if (!missW[i])
                                copyW[k - 1] = weights[i];
                            else
                                copyW[k - 1] = 1;
                    }
                c.assign("x", copyVal);
                if (weight > -1)
                    c.assign("w", copyW);

                RList l;
                if (weight == -1)
                    l = c.eval("density(x, bw=" + bWidth + ", from=" + xMin + ", to=" + xMax + ")").asList();
                else
                    l = c.eval("density(x, bw=" + bWidth + ", weights=w/sum(w, na.rm=T), from=" + xMin + ", to=" + xMax + ")").asList();
                double[] dx = l.at("x").asDoubles();
                double[] dy = l.at("y").asDoubles();

                Polygon pD;
                if (displayMode.equals("Histogram") && !CDPlot) {
                    pD = new Polygon();
                    for (int f = 0; f < dx.length; f++)
                        pD.addPoint((int) userToWorldX(dx[f]), (int) userToWorldY(dy[f]));

                    bg.drawPolyline(pD.xpoints, pD.ypoints, pD.npoints);
                }
                int nSel = data.countSelection(dvar);
                double wSum = 0;
                double wSSum = 0;
                if (nSel > 1) {
                    double[] selX = new double[nSel];
                    double[] selW = new double[nSel];
                    double[] selection = data.getSelection();
                    k = 0;
                    for (int i = 0; i < data.n; i++) {
                        if (selection[i] > 0 && !missing[i]) {
                            selX[k++] = xVal[i];
                            if (weight > -1)
                                if (!missW[i]) {
                                    selW[k - 1] = weights[i];
                                    wSSum += weights[i];
                                } else
                                    selW[k - 1] = 0;
                        }
                        if (weight > -1 && !missW[i])
                            wSum += weights[i];
                    }
                    c.assign("x", selX);
                    if (weight > -1)
                        c.assign("w", selW);

                    if (weight == -1)
                        l = c.eval("density(x, bw=" + bWidth + ", from=" + xMin + ", to=" + xMax + ")").asList();
                    else
                        l = c.eval("density(x, bw=" + bWidth + ", weights=w/sum(w, na.rm=T), from=" + xMin + ", to=" + xMax + ")").asList();
                    double[] dsx = l.at("x").asDoubles();
                    double[] dsy = l.at("y").asDoubles();

                    bg.setColor(getHiliteColor());

                    double totalY = 0;
                    if (!displayMode.equals("Histogram"))
                        for (int f = 0; f < dy.length - 1; f++)
                            totalY += dy[f];

                    double sumY = 0;
                    double fac = 1;
                    if (scaleSelD)
                        if (weight == -1)
                            fac = (double) nSel / (double) data.getN(dvar);
                        else
                            fac = wSSum / wSum;
                    pD = new Polygon();
                    if (displayMode.equals("Histogram"))
                        if (!CDPlot)
                            for (int f = 0; f < dx.length; f++)
                                pD.addPoint((int) userToWorldX(dsx[f]), (int) userToWorldY(dsy[f] * fac));
                        else
                            for (int f = 0; f < dx.length; f++)
                                pD.addPoint((int) userToWorldX(dsx[f]), (int) userToWorldY(yMax * dsy[f] * fac / dy[f]));
                    else
                        for (int f = 0; f < dx.length; f++) {
                            pD.addPoint((int) userToWorldX(xMin + sumY / totalY * (xMax - xMin)), (int) userToWorldY(yMax * dsy[f] * fac / dy[f]));
                            sumY += dy[f];
                        }
                    bg.drawPolyline(pD.xpoints, pD.ypoints, pD.npoints);
                    if (CDPlot) {
                        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.5)));
                        bg.drawLine((int) userToWorldX(xMin), (int) userToWorldY(yMax * fac),
                                (int) userToWorldX(xMax), (int) userToWorldY(yMax * fac));
                    }
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
        if (!printing) {
            drawSelections(bg);
            g.drawImage(bi, 0, 0, null);
            bg.dispose();
        }
    }


    public void drawSelections(Graphics bg) {

        for (int i = 0; i < Selections.size(); i++) {
            Selection S = (Selection) Selections.elementAt(i);
            drawBoldDragBox(bg, S);
        }
    }


    public void home() {
        yMax = 0;
        for (int i = 0; i < k; i++) {
            yMax = Math.max(add[i] / totalSum / bWidth, yMax);
//        System.out.println("yMax   "+yMax);
        }
        yMax *= 1.1;

        setCoordinates(this.getLlx(), yMin, this.getUrx(), yMax, -1);
        coordsSet = true;
    }


    public void processKeyEvent(KeyEvent e) {

        if (e.getID() == KeyEvent.KEY_PRESSED && (e.getKeyCode() == KeyEvent.VK_UP
                || e.getKeyCode() == KeyEvent.VK_DOWN
                || e.getKeyCode() == KeyEvent.VK_LEFT
                || e.getKeyCode() == KeyEvent.VK_RIGHT
                || (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                && (e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0))
                || (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                && e.getKeyCode() == KeyEvent.VK_R)
                || (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                && e.getKeyCode() == KeyEvent.VK_E)
                || (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                && e.getKeyCode() == KeyEvent.VK_B)
                || (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                && e.getKeyCode() == KeyEvent.VK_D))) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (bWidth > 0) {
                    tablep.updateBins(bStart, bWidth -= bWidth * 0.1);
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (bWidth < (data.getMax(dvar) - data.getMin(dvar)) * 1.1) {
                    tablep.updateBins(bStart, bWidth += bWidth * 0.1);
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (bStart > data.getMin(dvar) - bWidth) {
                    tablep.updateBins(bStart = Math.max(data.getMin(dvar) - bWidth, bStart - bWidth * 0.1), bWidth);
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if (bStart < data.getMin(dvar)) {
                    tablep.updateBins(bStart = Math.min(data.getMin(dvar), bStart + bWidth * 0.1), bWidth);
                }
            }

            if ((e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0)
                    && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
                home();
            }
            if (e.getKeyCode() == KeyEvent.VK_R && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
                if (displayMode.equals("Histogram"))
                    displayMode = "Spinogramm";
                else
                    displayMode = "Histogram";
            }
            if (e.getKeyCode() == KeyEvent.VK_B && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
                // Set colors for color brushing
                data.setColors(k, 0);
                for (int i = 0; i < k; i++)
                    for (int j = 0; j < (tablep.Ids[i]).length; j++)
                        data.setColor(tablep.Ids[i][j], 1 + i);
                eventID = -1;
                dataChanged(eventID);                                 // and is updated first!

                DataEvent de = new DataEvent(this);              // now the rest is informed ...
                evtq.postEvent(de);
            }
            if (e.getKeyCode() == KeyEvent.VK_D && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
                if (densityMode)
                    CDPlot = false;
                densityMode = !densityMode;
            }
            if (e.getKeyCode() == KeyEvent.VK_E && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
                CDPlot = !CDPlot;
                densityMode = true;
            }
            create(border, border, this.width - border, this.height - border, "");
            for (int i = 0; i < Selections.size(); i++) {
                Selection S = (Selection) Selections.elementAt(i);
                maintainSelection(S);
            }
            if (!RService.hasR()) {
                densityMode = false;
                CDPlot = false;
            }
            paint(this.getGraphics());
        } else
            super.processKeyEvent(e);  // Pass other event types on.
    }


    public void processMouseMotionEvent(MouseEvent e) {

        Graphics2D g = (Graphics2D) this.getGraphics();
        FontMetrics fm = bg.getFontMetrics();
        tbg = (Graphics2D) tbi.getGraphics();
        tbg.drawImage(bi, 0, 0, null);

        drawSelections(bg);

        if ((e.getID() == MouseEvent.MOUSE_MOVED)) {

            if ((e.getModifiers() == ALT_DOWN)) {

                frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

                info = true;
                tbg.setColor(ColorManager.backgroundColor);

                // Draw x-Label for CRTL_DOWN event
                int egetX = e.getX();
                int egetY = e.getY();

                String print = StatUtil.roundToString(worldToUserX(egetX), round);
                if (egetX < (int) userToWorldX(xMin)) {
                    egetX = (int) userToWorldX(xMin);
                    print = StatUtil.roundToString(xMin, round);
                }
                if (egetX > (int) userToWorldX(xMax)) {
                    egetX = (int) userToWorldX(xMax);
                    print = StatUtil.roundToString(xMax, round);
                }

                double ratioX = (worldToUserX(egetX) - getLlx()) / (getUrx() - getLlx());
                int minWidth = fm.stringWidth(StatUtil.roundToString(getLlx(), round));
                int maxWidth = fm.stringWidth(StatUtil.roundToString(getUrx(), round));

                if (egetX <= (int) userToWorldX(xMin) + minWidth + 4)
                    tbg.fillRect((int) userToWorldX(xMin), (int) userToWorldY(getLly()) + outside + tick + 1,
                            minWidth + 4, fm.getMaxAscent() + fm.getMaxDescent());
                if (egetX >= (int) userToWorldX(xMax) - maxWidth - 4)
                    tbg.fillRect((int) userToWorldX(xMax) - maxWidth - 4, (int) userToWorldY(getLly()) + outside + tick + 1,
                            maxWidth + 4, fm.getMaxAscent() + fm.getMaxDescent());

                tbg.setColor(ColorManager.lineColor);
                tbg.drawLine(egetX, (int) userToWorldY(getLly()) + outside,
                        egetX, (int) userToWorldY(getLly()) + outside + tick);
                tbg.drawString(print,
                        egetX - fm.stringWidth(print) / 2
                                - (int) (fm.stringWidth(print) *
                                (ratioX - 0.5)),
                        (int) userToWorldY(getLly()) + outside + tick + fm.getMaxAscent() + fm.getMaxDescent());

                // Fadenkreuz
                tbg.setColor(Color.white);
                tbg.drawLine(egetX, egetY + outside,
                        egetX, (int) userToWorldY(getLly()));

                g.drawImage(tbi, 0, 0, Color.black, null);
                tbg.dispose();
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


    public String getToolTipText(MouseEvent e) {

        if (e.isControlDown()) {

            for (int i = 0; i < rects.size(); i++) {
                MyRect r = (MyRect) rects.elementAt(i);
                if (r.contains(e.getX(), e.getY())) {
                    return Utils.info2Html(r.getLabel());
                }
            }
            // end FOR
            return null;
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

        boolean info = false;
        if (e.getID() == MouseEvent.MOUSE_PRESSED ||
                e.getID() == MouseEvent.MOUSE_RELEASED) {
            if (e.isPopupTrigger() && !e.isShiftDown()) {
                for (int i = 0; i < rects.size(); i++) {
                    MyRect r = (MyRect) rects.elementAt(i);
                    if (r.contains(e.getX(), e.getY() + sb.getValue())) {
                        info = true;
                        r.pop(this, e.getX(), e.getY());
                    }
                }
                if (!info) {
                    JPopupMenu mode = new JPopupMenu();
                    JMenuItem Spineplot;
                    if (displayMode.equals("Histogram")) {
                        Spineplot = new JMenuItem("Spinogram");
                        Spineplot.setActionCommand("Spinogram");
                    } else {
                        Spineplot = new JMenuItem("Histogram");
                        Spineplot.setActionCommand("Histogram");
                    }
                    Spineplot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

                    mode.add(Spineplot);
                    Spineplot.addActionListener(this);

                    JCheckBoxMenuItem CDPlotM = new JCheckBoxMenuItem("CDPlot");
                    CDPlotM.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

                    if (CDPlot)
                        CDPlotM.setSelected(true);
                    else
                        CDPlotM.setSelected(false);
                    mode.add(CDPlotM);

                    CDPlotM.setActionCommand("CDPlot");
                    CDPlotM.addActionListener(this);

                    JCheckBoxMenuItem Density = new JCheckBoxMenuItem("Density");
                    Density.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                    if (densityMode)
                        Density.setSelected(true);
                    else
                        Density.setSelected(false);
                    mode.add(Density);

                    if (!RService.hasR()) {
                        CDPlotM.setEnabled(false);
                        Density.setEnabled(false);
                    }

                    Density.setActionCommand("Density");
                    Density.addActionListener(this);

                    if (densityMode) {
                        JCheckBoxMenuItem scaleD = new JCheckBoxMenuItem("scale Density");
                        mode.add(scaleD);
                        if (scaleSelD)
                            scaleD.setSelected(true);
                        else
                            scaleD.setSelected(false);

                        scaleD.addItemListener(new ItemListener() {
                            public void itemStateChanged(ItemEvent e) {
                                scaleSelD = !scaleSelD;
                                Update();
                            }
                        });
                    }

                    final Axis axisW = new Axis(xMin, xMax);

                    JMenu menuWidth = new JMenu("Width");

                    mode.add(menuWidth);
                    JCheckBoxMenuItem[][] wdt = new JCheckBoxMenuItem[3][4];
                    for (int i = 2; i >= 0; i--) {
                        if (bWidth == axisW.tickM * Math.pow(10, (double) i))
                            wdt[i][0] = new JCheckBoxMenuItem("" + axisW.tickM * Math.pow(10, (double) i), true);
                        else
                            wdt[i][0] = new JCheckBoxMenuItem("" + axisW.tickM * Math.pow(10, (double) i), false);
                        if (bWidth == axisW.tickMM * Math.pow(10, (double) i))
                            wdt[i][1] = new JCheckBoxMenuItem("" + axisW.tickMM * Math.pow(10, (double) i), true);
                        else
                            wdt[i][1] = new JCheckBoxMenuItem("" + axisW.tickMM * Math.pow(10, (double) i), false);
                        if (bWidth == axisW.tickMMM * Math.pow(10, (double) i))
                            wdt[i][2] = new JCheckBoxMenuItem("" + axisW.tickMMM * Math.pow(10, (double) i), true);
                        else
                            wdt[i][2] = new JCheckBoxMenuItem("" + axisW.tickMMM * Math.pow(10, (double) i), false);
                        if (bWidth == axisW.tickMMMM * Math.pow(10, (double) i))
                            wdt[i][3] = new JCheckBoxMenuItem("" + axisW.tickMMMM * Math.pow(10, (double) i), true);
                        else
                            wdt[i][3] = new JCheckBoxMenuItem("" + axisW.tickMMMM * Math.pow(10, (double) i), false);

                        for (int j = 0; j < 4; j++) {
                            if (xMax - xMin > Utils.atod(wdt[i][j].getText())) {
                                menuWidth.add(wdt[i][j]);
                                wdt[i][j].addItemListener(new ItemListener() {
                                    public void itemStateChanged(ItemEvent e) {
                                        bWidth = Utils.atod(((JCheckBoxMenuItem) e.getItem()).getText());
                                        tablep.updateBins(bStart, bWidth);
                                        Update();
                                    }
                                });
                            }
                        }
                    }
                    JMenuItem wvalue = new JMenuItem("Value ...");
                    wvalue.setActionCommand("bwidth");
                    wvalue.addActionListener(this);
                    menuWidth.add(wvalue);

                    JMenu menuStart = new JMenu("Anchorpoint");

                    mode.add(menuStart);
                    JCheckBoxMenuItem[][] fst = new JCheckBoxMenuItem[3][4];
                    double maxEntry = -3.1415926e-100;
                    double starter = 0;
                    for (int i = 2; i >= 0; i--) {
                        for (int j = 0; j < 4; j++) {
                            int k = 0;
                            int insert = 0;
                            double ticker = Utils.atod(wdt[i][j].getText());
                            if (xMax - xMin > ticker) {
                                starter = StatUtil.round((Math.floor(xMin / ticker)) * ticker, 8);

                                while (k < menuStart.getItemCount()) {
                                    if (starter > Utils.atod(menuStart.getItem(k).getText()))
                                        insert = k + 1;
                                    k++;
                                }

                                if (insert == menuStart.getItemCount() && insert > 0 &&
                                        starter == Utils.atod(menuStart.getItem(insert - 1).getText()))
                                    insert = -1;
                                else if (insert < menuStart.getItemCount() &&
                                        starter == Utils.atod(menuStart.getItem(insert).getText()))
                                    insert = -1;

                                if (insert != -1) {
                                    if (bStart == starter)
                                        fst[i][j] = new JCheckBoxMenuItem("" + starter, true);
                                    else
                                        fst[i][j] = new JCheckBoxMenuItem("" + starter, false);
                                    maxEntry = Math.max(starter, maxEntry);
                                    if (insert < menuStart.getItemCount())
                                        menuStart.insert(fst[i][j], insert);
                                    else
                                        menuStart.add(fst[i][j]);
                                    fst[i][j].addItemListener(new ItemListener() {
                                        public void itemStateChanged(ItemEvent e) {
                                            bStart = Utils.atod(((JCheckBoxMenuItem) e.getItem()).getText());
                                            tablep.updateBins(bStart, bWidth);
                                            Update();
                                        }
                                    });
                                }
                            }
                        }
                    }
                    if (xMin != maxEntry) {
                        JCheckBoxMenuItem tmp;
                        if (bStart == xMin)
                            tmp = new JCheckBoxMenuItem("" + xMin, true);
                        else
                            tmp = new JCheckBoxMenuItem("" + xMin, false);
                        menuStart.add(tmp);
                        tmp.addItemListener(new ItemListener() {
                            public void itemStateChanged(ItemEvent e) {
                                bStart = Utils.atod(((JCheckBoxMenuItem) e.getItem()).getText());
                                tablep.updateBins(bStart, bWidth);
                                Update();
                            }
                        });
                    }
                    JMenuItem svalue = new JMenuItem("Value ...");
                    svalue.setActionCommand("bstart");
                    svalue.addActionListener(this);
                    menuStart.add(svalue);
                    JMenuItem homeView = new JMenuItem("Home View");
                    homeView.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                    homeView.setActionCommand("home");
                    homeView.addActionListener(this);
                    mode.add(homeView);

                    JMenuItem brush;
                    if (!tablep.data.colorBrush) {
                        brush = new JMenuItem("Color Brush");
                        brush.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                        brush.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                processKeyEvent(new KeyEvent(frame, KeyEvent.KEY_PRESSED, 0, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), KeyEvent.VK_B));
                            }


                        });
                    } else {
                        brush = new JMenuItem("Clear all Colors");
                        brush.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                        brush.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                frame.parentFrame.getController().clearColors();
                            }


                        });
                    }
                    mode.add(brush);

                    mode.add(new JMenuItem("Dismiss"));
                    mode.show(this, e.getX(), e.getY());
                }
            } else
                super.processMouseEvent(e);  // Pass other event types on.
        } else
            super.processMouseEvent(e);  // Pass other event types on.
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("Histogram") || command.equals("Spinogram")) {
            displayMode = command;
            Update();
        } else if (command.equals("bwidth") || command.equals("bstart")) {
            if (command.equals("bwidth"))
                bWidth = Utils.atod(JOptionPane.showInputDialog(this, "Set bin width to:"));
            if (command.equals("bstart"))
                bStart = Utils.atod(JOptionPane.showInputDialog(this, "Set anchor point to:"));
            tablep.updateBins(bStart, bWidth);
            Update();
        } else if (command.equals("home")) {
            home();
            Update();
        } else if (command.equals("Density")) {
            if (RService.hasR()) {
                densityMode = !densityMode;
                if (densityMode)
                    scaleSelD = true;
                Update();
            } else
                return;
        } else if (command.equals("CDPlot")) {
            if (RService.hasR()) {
                CDPlot = !CDPlot;
                densityMode = true;
                Update();
            } else
                return;
        } else
            super.actionPerformed(e);
    }


    public void Update() {
        rects.removeAllElements();
        create(border, border, width - border, height - border, "");
        Graphics2D g = (Graphics2D) this.getGraphics();
        paint(g);
        g.dispose();
    }


    public void create(int x1, int y1, int x2, int y2, String info) {

        //setCoordinates(this.getLlx(), yMin, this.getUrx(), yMax, -1);

        rects.removeAllElements();
        labels.removeAllElements();

        this.name = tablep.name;
        this.levels = tablep.levels;
        this.names = tablep.names;
        this.lnames = tablep.lnames;

        this.k = levels[0];
        totalSum = 0;
        double max = 0;
        Vector[] tileIds = new Vector[k];
        add = new double[k];

        for (int i = 0; i < k; i++) {
            add[i] = tablep.table[i];
            totalSum += add[i];
            max = Math.max(max, add[i]);
            tileIds[i] = new Vector(1, 0);
            tileIds[i].addElement(i);
        }

        Graphics g = this.getGraphics();
        FontMetrics FM = g.getFontMetrics();
        int fh = FM.getHeight();
        g.dispose();

        if (!coordsSet)
            home();

        if (displayMode.equals("Histogram")) {
            for (int i = 0; i < k; i++) {
                rects.addElement(new MyRect(true, 'y', "Observed",
                        (int) userToWorldX(bStart + i * bWidth),
                        (int) userToWorldY(add[i] / totalSum / bWidth),
                        (int) userToWorldX(bStart + (i + 1) * bWidth) - (int) userToWorldX(bStart + i * bWidth),
                        (int) userToWorldY(0) - (int) userToWorldY(add[i] / totalSum / bWidth),
                        add[i], add[i], 1, 0, lnames[0][i] + '\n', tileIds[i], tablep));
            }
        } else {                // Spinogram

            int lastX = (int) userToWorldX(xMin);    //(int)userToWorldX(bStart);
//        int fullRange = (int)userToWorldX((Math.floor(xMax/bWidth)+1)*bWidth ) - (int)userToWorldX(bStart);
            int fullRange = (int) userToWorldX(xMax) - (int) userToWorldX(xMin);
//        int fullRange = (int)userToWorldX(getUrx()) - (int)userToWorldX(getLlx());
            double probRange = getUrx() - getLlx();
            int leftId = -1;
            int rightId = -1;
            for (int i = 0; i < k; i++)
                if (bStart + i * bWidth >= getLlx() && leftId == -1)
                    leftId = i;
            for (int i = k - 1; i >= 0; i--)
                if (bStart + (i + 1) * bWidth <= getUrx() && rightId == -1)
                    rightId = i + 1;
            int leftX = (int) userToWorldX(bStart + leftId * bWidth);
            int rightX = (int) userToWorldX(bStart + rightId * bWidth);
            double[] breaksX = new double[k + 1];
            breaksX[0] = lastX;
/*        for(int i=0; i<k; i++ ) {
          int currX = lastX + (int)Math.round(tablep.table[i]/totalSum * fullRange); 
          rects.addElement(new MyRect( true, 'y', "Observed",
                                       lastX,
                                       (int)userToWorldY(yMax),
                                       currX-lastX,
                                       (int)userToWorldY(yMin)-(int)userToWorldY(yMax),
                                       tablep.table[i], tablep.table[i], 1, 0, lnames[0][i]+'\n', tileIds[i], tablep));
          lastX = currX; 
        }*/
            for (int i = 0; i < k; i++)
                breaksX[i + 1] = breaksX[i] + (int) Math.round(tablep.table[i] / totalSum * fullRange);
            // we now transform the breaks such that the first and last break within the zoomed window won't move when switched between hist and spine!!
            double shiftX = (int) userToWorldX(getLlx()) - breaksX[leftId];

            System.out.println(" leftId: " + leftId);

            double oldRange = breaksX[rightId] - breaksX[leftId];

            double shift1 = breaksX[0];
            for (int i = 0; i <= k; i++)
                breaksX[i] = breaksX[i] - shift1;

            double fac = (rightX - leftX) / oldRange;
            System.out.println(" Factor: " + fac);
            for (int i = 0; i <= k; i++)
                breaksX[i] = breaksX[i] * fac;

            double shift2 = leftX - breaksX[leftId];
            for (int i = 0; i <= k; i++)
                breaksX[i] = breaksX[i] + shift2;

            for (int i = 0; i < k; i++)
                rects.addElement(new MyRect(true, 'y', "Observed",
                        (int) breaksX[i],
                        (int) userToWorldY(yMax),
                        (int) breaksX[i + 1] - (int) breaksX[i],
                        (int) userToWorldY(yMin) - (int) userToWorldY(yMax),
                        tablep.table[i], tablep.table[i], 1, 0, lnames[0][i] + '\n', tileIds[i], tablep));
        }

        for (int i = 0; i < Selections.size(); i++) {
            Selection S = (Selection) Selections.elementAt(i);
            S.r.x = (int) userToWorldX(((floatRect) S.o).x1);
            S.r.y = (int) userToWorldY(((floatRect) S.o).y1);
            S.r.width = (int) userToWorldX(((floatRect) S.o).x2) - (int) userToWorldX(((floatRect) S.o).x1);
            S.r.height = (int) userToWorldY(((floatRect) S.o).y2) - (int) userToWorldY(((floatRect) S.o).y1);
        }
    }


    public void dataChanged(int var) {
        if (var == tablep.initialVars[0] || var == -1) {
            paint(this.getGraphics());
        }
    }


    public void scrollTo(int id) {
    }


    public void adjustmentValueChanged(AdjustmentEvent e) {
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


    private String name;          // the name of the table;
    private double table[];    // data in classical generalized binary order
    private int[] levels;        // number of levels for each variable
    private int[] plevels;        // reverse cummulative product of levels

    private String[] names;    // variable names
    private String[][] lnames;    // names of levels
    private DataListener listener;
    private static EventQueue evtq;
}

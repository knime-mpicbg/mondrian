package org.rosuda.mondrian.plots;

import org.rosuda.mondrian.MFrame;
import org.rosuda.mondrian.Table;
import org.rosuda.mondrian.core.*;
import org.rosuda.mondrian.plots.basic.MyRect;
import org.rosuda.mondrian.plots.basic.MyText;
import org.rosuda.mondrian.util.Qsort;
import org.rosuda.mondrian.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;


public class MissPlot extends DragBox implements ActionListener {

    private Vector rects = new Vector(256, 0);            // Store the tiles.
    private Vector labels = new Vector(256, 0);           // Store the labels.
    private Vector tables = new Vector(256, 0);           // Store the tables.
    private int width, height, realHeight, startX;       // The preferred size.
    protected int oldWidth, oldHeight;                   // The last size for constructing the bars.
    public String displayMode = "x";
    private boolean moving = false;
    private MyRect movingRectO;
    private MyRect movingRectM;
    private MyText movingText;
    private int movingId;
    private int oldY;
    protected DataSet data;
    protected int[] vars;
    protected int[] miss;
    protected int[] permA;
    protected int[] IpermA;
    protected double[] selected;
    protected boolean[] missings;
    private Image bi;
    private Graphics bg;
    private int k;


    public MissPlot(MFrame frame, DataSet data, int[] vars) {
        super(frame);

        this.vars = vars;
        this.data = data;

        this.k = vars.length;
        permA = new int[k];
        IpermA = new int[k];
        for (int j = 0; j < k; j++) {
            permA[j] = j;
            IpermA[permA[j]] = j;
        }
        sb.setUnitIncrement(22);
        sb.setBlockIncrement(22);

        frame.getContentPane().add(this);

        Font SF = new Font("SansSerif", Font.PLAIN, 11);
        frame.setFont(SF);

        border = 20;

        String titletext;
        titletext = "Missing Value Plot";

        for (int j = 0; j < this.k; j++) {
            boolean[] tmpMiss;
            double[] values = new double[2];
            int[][] Ids = new int[2][];
            tmpMiss = data.getMissings(vars[j]);
            values[0] = data.getN(vars[j]);
            values[1] = data.n - values[0];
            Ids[0] = new int[(int) values[0]];
            Ids[1] = new int[(int) values[1]];
            int obsPointer = 0;
            int NAPointer = 0;
            for (int i = 0; i < data.n; i++) {
                if (!tmpMiss[i])
                    Ids[0][obsPointer++] = i;
                else
                    Ids[1][NAPointer++] = i;
//        System.out.println("Variable: "+data.getName(vars[j])+" obsPointer: "+obsPointer+" NAPointer: "+NAPointer);
            }
            tables.addElement(new Table("Missing Table", values, 1, new int[]{2}, new String[]{data.getName(vars[j])}, new String[][]{{"Observed", "Missing"}}, new int[]{vars[j]}, Ids, data, -1));
        }

        frame.setTitle(titletext);

        evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();
    }


    public void addDataListener(DataListener l) {
        listener = l;
    }


    public void processEvent(AWTEvent evt) {
        if (evt instanceof DataEvent) {
            if (listener != null)
                listener.dataChanged(0);
        } else super.processEvent(evt);
    }


    public void maintainSelection(Selection S) {

        Rectangle sr = S.r;
        int mode = S.mode;

        Dimension size = this.getSize();

        double x1, x2, y1, y2;

        x2 = (double) (S.r.x + S.r.width - startX) / (size.width - startX - border);

        if (S.r.x < startX) {
            x1 = (double) (S.r.x - startX);
            if (S.r.x + S.r.width < startX)
                x2 = (double) (S.r.x - startX + S.r.width);
        } else
            x1 = (double) (S.r.x - startX) / (double) (size.width - startX - border);

        S.o = new floatRect(x1,
                (double) (S.r.y - border) / (double) (realHeight),
                x2,
                (double) (S.r.y + S.r.height - border) / (realHeight));

        boolean[] hits = new boolean[data.n];
        for (int i = 0; i < data.n; i++)
            hits[i] = false;

        for (int j = 0; j < rects.size(); j++) {
            MyRect r = (MyRect) rects.elementAt(j);
            if (r.intersects(sr)) {
                missings = data.getMissings(vars[permA[j / 2]]);
                for (int i = 0; i < data.n; i++)
                    if ((missings[i] && (j % 2) == 1) || (!missings[i] && (j % 2) == 0))
                        hits[i] = true;
            }
        }
        for (int i = 0; i < data.n; i++)
            if (hits[i])
                data.setSelection(i, 1, S.mode);
            else
                data.setSelection(i, 0, S.mode);
    }


    public void updateSelection() {
        paint(this.getGraphics());
    }


    public void dataChanged(int var) {

        paint(this.getGraphics());

    }


    public void adjustmentValueChanged(AdjustmentEvent e) {
        paint(this.getGraphics());
    }


    public void scrollTo(int id) {
        sb.setValue(id);
        if (!moving)
            paint(this.getGraphics());
    }


    public void paint(Graphics2D g) {

        frame.setBackground(MFrame.backgroundColor);

        Dimension size;
        if (!printing)
            size = this.getViewportSize();
        else
            size = this.getSize();

        if (oldWidth != size.width || oldHeight != size.height) {
            this.width = size.width;
            this.height = size.height;
            realHeight = create(border, border, size.width - border, size.height - border, "");
            this.setSize(size.width, realHeight + 2 * border);
            size = this.getSize();
            oldWidth = size.width;
            oldHeight = size.height;
        }

        if (printing)
            bg = g;
        else {
            if (bi != null) {
                if (bi.getWidth(null) != size.width || bi.getHeight(null) != size.height) {
                    bg.dispose();
                    bi = null;
                    //System.out.println("New Image!");
                    bi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
                }
            } else {
                bi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
            }
            bg = bi.getGraphics();
            bg.clearRect(0, 0, size.width, size.height);
            bg.translate(0, -sb.getValue());
        }

        int start = -1, stop = k - 1;

        bg.setColor(MFrame.lineColor);

        if (!printing) {
            for (int i = 0; i < labels.size(); i++) {
                MyText t = (MyText) labels.elementAt(i);
                if (t.y >= sb.getValue()) {
                    t.draw(bg, 0);
                    if (start == -1)
                        start = Math.max(0, i - 1);
                }
                if (t.y - sb.getValue() > size.height) {
                    stop = i - 1;
                    i = labels.size();
                }
            }
        } else {
            for (int i = 0; i < labels.size(); i++) {
                MyText t = (MyText) labels.elementAt(i);
                t.draw(bg, 0);
            }
            start = 0;
            stop = k - 1;
        }

        selected = data.getSelection();
        for (int j = stop; j >= start; j--) {
            MyRect rO = (MyRect) rects.elementAt(2 * j);
            MyRect rM = (MyRect) rects.elementAt(2 * j + 1);
            rM.setHiliteAlign(true);
            double sumO = 0, sumM = 0;
            missings = data.getMissings(vars[permA[j]]);
            for (int i = 0; i < data.n; i++)
                if (selected[i] > 0)
                    if (missings[i])
                        sumM++;
                    else
                        sumO++;
            if (miss[j] < data.n)
                rO.setHilite(sumO / (data.n - miss[j]));
            else
                rO.setHilite(0);
            rO.draw(bg);
            if (miss[j] > 0)
                rM.setHilite(sumM / miss[j]);
            else
                rM.setHilite(0);
            rM.draw(bg);
        }
        if (moving) {
            //        System.out.println("Moving in Barchart: paint");
            movingRectO.draw(bg);
            movingRectM.draw(bg);
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


    public String getToolTipText(MouseEvent e) {

        if (e.isControlDown()) {

            for (int i = 0; i < rects.size(); i++) {
                MyRect r = (MyRect) rects.elementAt(i);
                if (r.contains(e.getX(), e.getY() + sb.getValue())) {
                    return Util.info2Html(r.getLabel());
                }
            }
            // end FOR
            return null;
        } else
            return null;
    }


    public void processMouseMotionEvent(MouseEvent e) {

        boolean info = false;

        if (moving) {
            movingRectO.moveTo(-1, e.getY() + sb.getValue());
            movingRectM.moveTo(-1, e.getY() + sb.getValue());
            paint(this.getGraphics());
        }/*
       else {
         for( int i = 0;i < rects.size(); i++) {
           MyRect r = (MyRect)rects.elementAt(i);
           if ( r.contains( e.getX(), e.getY()+sb.getValue() )) {
             info = true;
             ToolTipManager.sharedInstance().setEnabled(true);
             this.setToolTipText(r.getLabel());
           }
         }
         if( !info ) {
           ToolTipManager.sharedInstance().setEnabled(false);
           this.setToolTipText("");
         }
       }*/
        else
            super.processMouseMotionEvent(e);  // Pass other event types on.
    }


    public void processMouseEvent(MouseEvent e) {

        if (e.isPopupTrigger() && !e.isShiftDown() && !moving)
            super.processMouseEvent(e);  // Pass other event types on.
        if (changePop) {
            changePop = false;
            return;
        }

        boolean info = false;
        if (e.getID() == MouseEvent.MOUSE_PRESSED ||
                e.getID() == MouseEvent.MOUSE_RELEASED) {
            if (e.isPopupTrigger() && e.getModifiers() != BUTTON1_DOWN + ALT_DOWN && !e.isShiftDown()) {
                //System.out.println("pop up trigger in Barchart!!!!"+e.getModifiers());
                for (int i = 0; i < rects.size(); i++) {
                    MyRect r = (MyRect) rects.elementAt(i);
                    if (r.contains(e.getX(), e.getY() + sb.getValue())) {
                        info = true;
                        r.pop(this, e.getX(), e.getY());
                    }
                }
                if (!info) {
                    JPopupMenu mode = new JPopupMenu();
                    JMenu sorts = new JMenu("Sort by ...");
                    JMenuItem frq = new JMenuItem("Missings count");
                    JMenuItem abs = new JMenuItem("Absolute selected");
                    JMenuItem rel = new JMenuItem("Relative selected");
                    JMenuItem lex = new JMenuItem("Lexicographic");
                    JMenuItem ini = new JMenuItem("Initial");
                    JMenuItem rev = new JMenuItem("Reverse");
                    sorts.add(frq);
                    sorts.add(abs);
                    sorts.add(rel);
                    sorts.add(lex);
                    sorts.addSeparator();
                    sorts.add(ini);
                    sorts.add(rev);
                    frq.setActionCommand("frq");
                    abs.setActionCommand("abs");
                    rel.setActionCommand("rel");
                    lex.setActionCommand("lex");
                    ini.setActionCommand("ini");
                    rev.setActionCommand("rev");
                    frq.addActionListener(this);
                    abs.addActionListener(this);
                    rel.addActionListener(this);
                    lex.addActionListener(this);
                    ini.addActionListener(this);
                    rev.addActionListener(this);
                    mode.add(sorts);

                    if (displayMode.equals("x")) {
                        JMenuItem Spineplot = new JMenuItem("Rotate");
                        Spineplot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                        mode.add(Spineplot);
                        Spineplot.setActionCommand("y");
                        Spineplot.addActionListener(this);
                    } else {
                        JMenuItem Barchart = new JMenuItem("Rotate");
                        Barchart.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                        mode.add(Barchart);
                        Barchart.setActionCommand("x");
                        Barchart.addActionListener(this);
                    }

                    JMenuItem diss = new JMenuItem("Dismiss");
                    mode.add(diss);

                    mode.show(this, e.getX(), e.getY());
                }
            } else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getModifiers() == BUTTON1_DOWN + ALT_DOWN) {
                for (int i = 0; i < rects.size(); i += 2) {
                    MyRect rO = (MyRect) rects.elementAt(i);
                    MyRect rM = (MyRect) rects.elementAt(i + 1);
                    if (rO.contains(e.getX(), e.getY() + sb.getValue()) || rM.contains(e.getX(), e.getY() + sb.getValue())) {
                        movingId = i;
                        System.out.println("Moooving ....................");
                        movingRectO = rO;
                        movingRectM = rM;
                        oldY = rO.getRect().y;
//                movingText = (MyText)labels.elementAt(i);
                        moving = true;
                        frame.setCursor(Frame.HAND_CURSOR);
                    }
                }
            } else if ((e.getID() == MouseEvent.MOUSE_RELEASED) && moving &&
                    ((e.getModifiers() == BUTTON1_UP + ALT_DOWN) ||
                            (e.getModifiers() == BUTTON1_UP))) {
                (this.getGraphics()).drawImage(bi, 0, 0, null);
                moving = false;
                movingRectO.moveTo(-1, oldY);
                movingRectM.moveTo(-1, oldY);
                frame.setCursor(Frame.DEFAULT_CURSOR);
                out:
                {
                    for (int i = 0; i < rects.size(); i += 2) {
                        MyRect rO = (MyRect) rects.elementAt(i);
                        MyRect rM = (MyRect) rects.elementAt(i + 1);
                        if (rO.contains(e.getX(), e.getY() + sb.getValue()) || rM.contains(e.getX(), e.getY() + sb.getValue())) {
                            //System.out.println("Exchange: "+movingId+" with: "+i);
                            int tmp = permA[movingId / 2];
                            permA[movingId / 2] = permA[i / 2];
                            permA[i / 2] = tmp;
                            break out;
                        }
                        if ((e.getY() + sb.getValue() < (rO.getRect()).y)) {
                            //System.out.println("Insert: "+movingId+" before: "+i);
                            if (movingId < i) {
                                int tmp = permA[movingId / 2];
                                for (int j = movingId / 2; j < (i / 2 - 1); j++) {
                                    permA[j] = permA[j + 1];
                                }
                                permA[i / 2 - 1] = tmp;
                            } else {
                                int tmp = permA[movingId / 2];
                                for (int j = movingId / 2; j > i / 2; j--) {
                                    permA[j] = permA[j - 1];
                                }
                                permA[i / 2] = tmp;
                            }
                            break out;
                        }
                    }
                    int tmp = permA[movingId / 2];                    // Insert AFTER the last Bin
                    for (int j = movingId / 2; j < (rects.size() / 2 - 2); j++) {
                        permA[j] = permA[j + 1];
                    }
                    permA[rects.size() / 2 - 2] = tmp;
                }
                for (int j = 0; j < IpermA.length; j++)
                    IpermA[permA[j]] = j;

                realHeight = create(border, border, width - border, height - border, "");
                paint(this.getGraphics());
            } else
                super.processMouseEvent(e);  // Pass other event types on.
        } else
            super.processMouseEvent(e);  // Pass other event types on.
    }


    public void processKeyEvent(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_R && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
            if (displayMode.equals("x"))
                displayMode = "y";
            else
                displayMode = "x";
            for (int j = 0; j < rects.size(); j++) {
                MyRect r = (MyRect) rects.elementAt(j);
                r.setDirection(displayMode);
            }
            Graphics g = this.getGraphics();
            paint(g);
            g.dispose();
        }
        super.processKeyEvent(e);  // Pass other event types on.
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("x") || command.equals("y")) {
            displayMode = command;
            for (int j = 0; j < rects.size(); j++) {
                MyRect r = (MyRect) rects.elementAt(j);
                r.setDirection(command);
            }
            paint(this.getGraphics());
        } else if (command.equals("abs") || command.equals("rel") || command.equals("lex") || command.equals("frq") || command.equals("ini") || command.equals("rev")) {
            if (command.equals("abs") || command.equals("rel") || command.equals("frq") || command.equals("lex")) {
                int[] perm;
                if (command.equals("abs") || command.equals("rel") || command.equals("frq")) {
                    double[] sortA = new double[k];
                    //
                    // filling arrays to sort them
                    //
                    if (command.equals("frq"))
                        for (int i = 0; i < sortA.length; i++)
                            sortA[i] = ((MyRect) rects.elementAt(2 * i + 1)).obs;
                    if (command.equals("abs"))
                        for (int i = 0; i < sortA.length; i++)
                            sortA[i] = ((MyRect) rects.elementAt(2 * i + 1)).getAbsHilite();
                    if (command.equals("rel"))
                        for (int i = 0; i < sortA.length; i++)
                            sortA[i] = ((MyRect) rects.elementAt(2 * i + 1)).getHilite();
                    perm = Qsort.qsort(sortA, 0, sortA.length - 1);
//        for( int j=0; j<perm.length; j++ )
//          System.out.println("Input: "+sortA[j]+" Rank: "+perm[j]);
                } else {
                    String[] sortA = new String[k];
                    for (int i = 0; i < sortA.length; i++)
                        sortA[i] = data.getName(vars[permA[i]]);
                    perm = Qsort.qsort(sortA, 0, sortA.length - 1);
                }
                int[] tperm = new int[perm.length];
                for (int i = 0; i < perm.length; i++)
                    tperm[i] = permA[perm[i]];
                for (int j = 0; j < perm.length; j++) {
                    permA[j] = tperm[j];
                    IpermA[permA[j]] = j;
                }
            } else if (command.equals("rev")) {
                for (int i = 0; i < permA.length / 2; i++) {
                    int save = permA[i];
                    permA[i] = permA[permA.length - i - 1];
                    permA[permA.length - i - 1] = save;
                }
            } else if (command.equals("ini")) {
                for (int j = 0; j < permA.length; j++) {
                    permA[j] = j;
                    IpermA[permA[j]] = j;
                }
            }
            realHeight = create(border, border, width - border, height - border, "");
            paint(this.getGraphics());
        } else
            super.actionPerformed(e);
    }


    public int create(int x1, int y1, int x2, int y2, String info) {

        //System.out.println(x1+" "+y1+" "+x2+" "+y2);

        rects.removeAllElements();
        labels.removeAllElements();
        Vector[] tileIds = new Vector[k];

        miss = new int[k];
        double sum = 0;
        double max = 0;

        for (int i = 0; i < k; i++) {
            miss[i] = data.n - data.getN(vars[permA[i]]);
            tileIds[i] = new Vector(1, 0);
            tileIds[i].addElement(new Integer(i));
        }

        int pF = 1;
        if (printing)
            pF = printFactor;

        int x = border;

        startX = x1 + x;
        int y = 0;

        int w;
        double h, hi;

        for (int i = 0; i < k; i++) {
            h = (Math.max(y2 - y1, k * 20) + 15 * pF) / k - 15 * pF;
            w = (x2 - x1 - x);
            String shorty = data.getName(vars[permA[i]]);
            labels.addElement(new MyText(shorty.trim(), border, y1 + y - 2));
            hi = Math.max(12 * pF, h);

            char dir = 'x';
            if (displayMode.equals("y"))
                dir = 'y';
            int ww = (int) ((width - 2 * border) * (1.0F - (double) miss[i] / data.n));
/*    rects.addElement(new MyRect( true, dir, "Observed", border, y1 + y, ww, (int)hi,
                                data.n-miss[i], data.n-miss[i], 1, 0, data.getName(vars[permA[i]])+": observed\n", null, null));
    rects.addElement(new MyRect( true, dir, "Observed", border+((MyRect)rects.lastElement()).w, y1 + y, width-2*border - ww, (int)hi,
                                miss[i], miss[i], 1, 0, data.getName(vars[permA[i]])+": NA\n", null, null));
*/
            Table tablep = (Table) (tables.elementAt(permA[i]));
            System.out.println("Name: " + tablep.names[0] + " i: " + i);
            rects.addElement(new MyRect(true, dir, "Observed", border, y1 + y, ww, (int) hi,
                    data.n - miss[i], data.n - miss[i], 1, 0, data.getName(vars[permA[i]]) + ": observed\n", tileIds[0], tablep));
            rects.addElement(new MyRect(true, dir, "Observed", border + ((MyRect) rects.lastElement()).w, y1 + y, width - 2 * border - ww, (int) hi,
                    miss[i], miss[i], 1, 0, data.getName(vars[permA[i]]) + ": NA\n", tileIds[0], tablep));
            y += hi + 15 * pF;
        }

        realHeight = y - 15;

        Dimension size = this.getSize();

        for (int i = 0; i < Selections.size(); i++) {
            Selection S = (Selection) Selections.elementAt(i);
            if (((floatRect) S.o).x1 <= 0)
                S.r.x = (int) (startX + ((floatRect) S.o).x1);
            else
                S.r.x = startX + (int) (((floatRect) S.o).x1 * (double) (size.width - startX - border));

            if (((floatRect) S.o).x2 <= 0)
                S.r.width = -(int) (((floatRect) S.o).x1 - ((floatRect) S.o).x2);
            else
                S.r.width = (int) (((floatRect) S.o).x2 * (size.width - startX - border) + startX - S.r.x);

            S.r.y = border + (int) (((floatRect) S.o).y1 * (double) (realHeight));
            S.r.height = (int) (((floatRect) S.o).y2 * (realHeight) -
                    ((floatRect) S.o).y1 * (realHeight));

            //System.out.println("Out: "+S.r.y+" <-> "+S.r.height);
        }

        System.out.println("realHeight: " + realHeight);
        return realHeight;
    }


    private String name;          // the name of the table;
    private double table[];    // data in classical generalized binary order
    private int[] levels;        // number of levels for each variable
    private int[] plevels;        // reverse cummulative product of levels

    private String[] names;    // variable names
    private String[][] lnames;    // names of levels
    private DataListener listener;
    private static EventQueue evtq;


    class floatRect {

        double x1, y1, x2, y2;


        public floatRect(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}


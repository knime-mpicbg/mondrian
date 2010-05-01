package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.core.*;
import de.mpicbg.sweng.mondrian.io.db.Query;
import de.mpicbg.sweng.mondrian.plots.basic.MyRect;
import de.mpicbg.sweng.mondrian.plots.basic.MyText;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
import de.mpicbg.sweng.mondrian.util.Qsort;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.util.Vector;


public class Barchart extends DragBox implements ActionListener {

    private Vector rects = new Vector(256, 0);            // Store the tiles.
    private Vector labels = new Vector(256, 0);           // Store the labels.
    private int width, height, realHeight, startX;       // The preferred size.
    protected int oldWidth, oldHeight;                   // The last size for constructing the bars.
    private Table tablep;                                // The datatable to deal with.
    public String displayMode = "Barchart";
    private boolean moving = false;
    private MyRect movingRect;
    private MyText movingText;
    private int movingId = -1;
    private int oldY;
    private double max = 0;
    private double iniMax = 0;
    private String searchText = "";
    private int scaler = 0;
    private Image bi;
    private Graphics bg;
    private int k;
    private int eventID;
    private long startT = 0;
    private int globalStart = -1;


    public Barchart(MDialog frame, int width, int height, Table tablep) {
        super(frame);
        this.tablep = tablep;
        this.name = tablep.name;
        this.levels = tablep.levels;
        this.names = tablep.names;
        this.lnames = tablep.lnames;
        this.width = width;
        this.height = height;

        sb.setUnitIncrement(22);
        sb.setBlockIncrement(22);

        this.k = levels[0];
        for (int i = 0; i < k; i++)
            max = Math.max(max, tablep.table[i]);
        iniMax = max;

        setCoordinates(0, 0, max, 1, -1);

        this.setScrollX();
        frame.getContentPane().add(this, "Center");

        Font SF = new Font("SansSerif", Font.PLAIN, 11);
        frame.setFont(SF);

        border = 20;

        String titletext;
        if (tablep.count == -1)
            titletext = "Barchart(" + names[0] + ")";
        else
            titletext = "Barchart(" + names[0] + "|" + tablep.data.getName(tablep.count) + ")";

        frame.setTitle(titletext);

        evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();

//    sb.show();
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

        //System.out.println("In: "+((floatRect)S.o).y1+" <-> "+((floatRect)S.o).y2+" ... "+realHeight);

        S.condition = new Query();
        for (int i = 0; i < rects.size(); i++) {
            MyRect r = (MyRect) rects.elementAt(i);
            if (r.intersects(sr)) {
                if (!(tablep.lnames[0][i]).equals("NA"))
                    S.condition.addCondition("OR", tablep.names[0] + " = '" + tablep.lnames[0][i] + "'");
                else
                    S.condition.addCondition("OR", tablep.names[0] + " is null");
                if (tablep.data.isDB)
                    tablep.getSelection();
                else {
                    double sum = 0, sumh = 0;
                    for (int j = 0; j < r.tileIds.size(); j++) {
                        int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                        //System.out.println("Id: "+id+":"+i);
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
    }


    public void updateSelection() {
        paint(this.getGraphics());
    }


    public void dataChanged(int var) {

        //System.out.println("Changed: "+var);

        if (var == tablep.initialVars[0] || var == -1) {
            tablep.rebreak();
            rects.removeAllElements();
            realHeight = create(border, border, width - border, height - border, "");
            paint(this.getGraphics());
        }
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

        tablep.getSelection();

        frame.setBackground(ColorManager.backgroundColor);

        Dimension size;
        if (!printing)
            size = this.getViewportSize();
        else
            size = this.getSize();

//      System.out.println("1 Breite: "+size.width+" Scrollbar: "+(sb.getSize()).width+" isVisible: "+sb.isVisible());

        if (oldWidth != size.width || oldHeight != size.height || scaleChanged) {
            this.width = size.width;
            this.height = size.height;

            if (sb.isVisible() && oldWidth > 0)
                size.width -= (sb.getSize()).width;

            rects.removeAllElements();
            realHeight = create(border, border, size.width - border, size.height - border, "");
            this.setSize(size.width, realHeight + 2 * border);
            size = this.getSize();

            oldWidth = size.width;
            oldHeight = size.height;
            if (scaleChanged) {
                boolean scaleFixed = true;
                scaleChanged = false;
            }
        }

        if (printing)
            bg = g;
        else {
            if (bi != null) {
                if (bi.getWidth(null) != size.width || bi.getHeight(null) != size.height) {
                    bg.dispose();
                    bi = null;
//            System.gc();
                    //System.out.println("New Image!");
                    if (sb.isVisible())
                        bi = createImage(size.width - (sb.getSize()).width, size.height);    // double buffering from CORE JAVA p212
                    else
                        bi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
                }
            } else {
                bi = createImage(size.width - (sb.getSize()).width, size.height);    // double buffering from CORE JAVA p212
            }
            bg = bi.getGraphics();
            bg.clearRect(0, 0, size.width - (sb.getSize()).width, size.height);
            bg.translate(0, -sb.getValue());
        }

        int start = -1, stop = k - 1;

        bg.setColor(ColorManager.lineColor);
        if (!printing) {
            int sbVal = sb.getValue();
            for (int i = 0; i < labels.size(); i++) {
                MyText t = (MyText) labels.elementAt(i);
                if (t.y >= sbVal) {
/*            if( moving && movingId == i ) {
              ((Graphics2D)bg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75F));
              t.draw(bg, 1);
              ((Graphics2D)bg).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
            } else */
                    t.draw(bg, 1);
                    if (start == -1)
                        start = Math.max(0, i - 1);
                }
                if (t.y - sbVal > size.height && i != movingId) {
                    stop = i - 1;
                    i = labels.size();
                }
            }
        } else {
            for (int i = 0; i < labels.size(); i++) {
                MyText t = (MyText) labels.elementAt(i);
                t.draw(bg, 1);
            }
            start = 0;
            stop = k - 1;
        }

        for (int i = start; i <= stop; i++) {
            int index;
            if (moving)
                if (i == stop)
                    index = movingId;
                else if (i >= movingId)
                    index = i + 1;
                else
                    index = i;
            else
                index = i;

            MyRect r = (MyRect) rects.elementAt(index);
            double sum = 0, sumh = 0;
            for (int j = 0; j < r.tileIds.size(); j++) {
                int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                sumh += tablep.getSelected(id) * tablep.table[id];
                sum += tablep.table[id];
            }
            r.setHilite(sumh / sum);
            if (moving && index == movingId)
                r.setAlpha(0.5);
            r.draw(bg);
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
                    return Utils.info2Html(r.getLabel());
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
            movingRect.moveTo(-1, e.getY() + sb.getValue() - (movingRect.getRect()).height / 2);

            FontMetrics FM;
            FM = (this.getGraphics()).getFontMetrics();

            movingText.moveYTo(e.getY() + sb.getValue() + FM.getHeight() / 2 - 2);

            if (e.getY() <= 10) {
                scrollTo(sb.getValue() - 22);
            }
            if (e.getY() >= ((frame.getSize()).height - (frame.getInsets().top + frame.getInsets().bottom) - 10)) {
                scrollTo(sb.getValue() + 22);
            }

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
//              r.pop(this, e.getX(), e.getY());
                    }
                }
                if (!info) {
                    JPopupMenu mode = new JPopupMenu();
                    if (displayMode.equals("Barchart")) {
                        JMenuItem Spineplot = new JMenuItem("Spineplot");
                        Spineplot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                        mode.add(Spineplot);
                        Spineplot.setActionCommand("Spineplot");
                        Spineplot.addActionListener(this);
                    } else {
                        JMenuItem Barchart = new JMenuItem("Barchart");
                        Barchart.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
                        mode.add(Barchart);
                        Barchart.setActionCommand("Barchart");
                        Barchart.addActionListener(this);
                    }
                    JMenu sorts = new JMenu("Sort by ...");
                    JMenuItem frq = new JMenuItem("Count");
                    JMenuItem abs = new JMenuItem("Absolute selected");
                    JMenuItem rel = new JMenuItem("Relative selected");
                    JMenuItem lex = new JMenuItem("Lexicographic");
                    JMenuItem rev = new JMenuItem("Reverse");
                    sorts.add(frq);
                    sorts.add(abs);
                    sorts.add(rel);
                    sorts.add(lex);
                    sorts.addSeparator();
                    sorts.add(rev);
                    frq.setActionCommand("frq");
                    abs.setActionCommand("abs");
                    rel.setActionCommand("rel");
                    lex.setActionCommand("lex");
                    rev.setActionCommand("rev");
                    frq.addActionListener(this);
                    abs.addActionListener(this);
                    rel.addActionListener(this);
                    lex.addActionListener(this);
                    rev.addActionListener(this);
                    mode.add(sorts);

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

                    JMenuItem diss = new JMenuItem("Dismiss");
                    mode.add(diss);

                    mode.show(this, e.getX(), e.getY());
                }
            } else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.getModifiers() == BUTTON1_DOWN + ALT_DOWN) {
                for (int i = 0; i < rects.size(); i++) {
                    MyRect r = (MyRect) rects.elementAt(i);
                    MyText t = (MyText) labels.elementAt(i);
                    if (r.contains(e.getX(), e.getY() + sb.getValue()) || t.contains(e.getX(), e.getY() + sb.getValue(), (Graphics2D) this.getGraphics())) {
                        movingId = i;
                        System.out.println("Moooving ....................");
                        movingRect = r;
                        oldY = r.getRect().y;
                        movingText = (MyText) labels.elementAt(i);
                        moving = true;
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
            } else if ((e.getID() == MouseEvent.MOUSE_RELEASED) && moving &&
                    ((e.getModifiers() == BUTTON1_UP + ALT_DOWN) ||
                            (e.getModifiers() == BUTTON1_UP))) {
                //        (e.getModifiers() ==  ALT_DOWN)))
                //	 ((e.getModifiers() ==  BUTTON1_UP + CTRL_DOWN) || (e.getModifiers() ==  BUTTON1_UP))) {
                System.out.println("in Barchart up: e.getModifiers(): " + e.getModifiers() + "  BUTTON1_UP: " + BUTTON1_UP);
                (this.getGraphics()).drawImage(bi, 0, 0, null);
                moving = false;
                movingRect.moveTo(-1, oldY);
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                Variable v = tablep.data.data.elementAt(tablep.initialVars[0]);
                out:
                {
                    for (int i = 0; i < rects.size(); i++) {
                        MyRect r = (MyRect) rects.elementAt(i);
                        if (r.contains(e.getX(), e.getY() + sb.getValue())) {
                            //System.out.println("Exchange: "+movingId+" with: "+i);
                            int tmp = v.permA[movingId];
                            v.permA[movingId] = v.permA[i];
                            v.permA[i] = tmp;
                            break out;
                        }
                        if ((e.getY() + sb.getValue() < (r.getRect()).y)) {
                            //System.out.println("Insert: "+movingId+" before: "+i);
                            if (movingId < i) {
                                int tmp = v.permA[movingId];
                                for (int j = movingId; j < i - 1; j++) {
                                    v.permA[j] = v.permA[j + 1];
                                }
                                v.permA[i - 1] = tmp;
                            } else {
                                int tmp = v.permA[movingId];
                                for (int j = movingId; j > i; j--) {
                                    v.permA[j] = v.permA[j - 1];
                                }
                                v.permA[i] = tmp;
                            }
                            break out;
                        }
                    }
                    int tmp = v.permA[movingId];                    // Insert AFTER the last Bin
                    for (int j = movingId; j < rects.size() - 1; j++) {
                        v.permA[j] = v.permA[j + 1];
                    }
                    v.permA[rects.size() - 1] = tmp;
                }
                for (int j = 0; j < v.levelP; j++)
                    v.IpermA[v.permA[j]] = j;

                this.dataFlag = true;                            // this plot was responsible
                eventID = tablep.initialVars[0];
                dataChanged(eventID);              // and is updated first!

                DataEvent de = new DataEvent(this);              // now the rest is informed ...
                evtq.postEvent(de);
            } else
                super.processMouseEvent(e);  // Pass other event types on.
        } else
            super.processMouseEvent(e);  // Pass other event types on.
    }


    public void processKeyEvent(KeyEvent e) {

        if ((e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_PAGE_UP || e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)) {

            int nextId = -1;
            int count = 0;

            for (int i = 0; i < rects.size(); i++) {
                MyRect r = (MyRect) rects.elementAt(i);
                int sum = 0, sumh = 0;
                for (int j = 0; j < r.tileIds.size(); j++) {
                    int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                    sumh += tablep.getSelected(id) * tablep.table[id];
                    sum += tablep.table[id];
                }
                if (sum == sumh) {
                    count++;
                    if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)
                        if (i < rects.size() - 1)
                            nextId = i + 1;
                        else
                            nextId = 0;
                    else if (i > 0)
                        nextId = i - 1;
                    else
                        nextId = rects.size() - 1;
                }
            }
            if (count == 1) {
                tablep.data.clearSelection();
                MyRect r = (MyRect) rects.elementAt(nextId);
                int sum = 0, sumh = 0;
                for (int j = 0; j < r.tileIds.size(); j++) {
                    int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                    tablep.setSelection(id, 1, Selection.MODE_STANDARD);
                    sumh += tablep.getSelected(id) * tablep.table[id];
                    sum += tablep.table[id];
                }
                r.setHilite(sumh / sum);

                SelectionEvent se = new SelectionEvent(this);
                evtq.postEvent(se);
            }
        } else if (e.getKeyCode() == KeyEvent.VK_R && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) {
            if (displayMode.equals("Barchart"))
                displayMode = "Spineplot";
            else
                displayMode = "Barchart";
            rects.removeAllElements();
            realHeight = create(border, border, width - border, height - border, "");
            Graphics g = this.getGraphics();
            paint(g);
            g.dispose();
        } else if (e.getKeyCode() == KeyEvent.VK_B && e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && !e.isShiftDown()) {
            tablep.data.setColors(k, 2);
            double[] datas = tablep.data.getNumbers(tablep.initialVars[0]);
            for (int i = 0; i < tablep.data.n; i++)
                tablep.data.setColor(i, 1 + (int) datas[i]);
            eventID = -1;
            dataChanged(eventID);                                 // and is updated first!

            DataEvent de = new DataEvent(this);              // now the rest is informed ...
            evtq.postEvent(de);
        } else if (e.getID() == KeyEvent.KEY_PRESSED && e.isShiftDown()
                && (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN)) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN && scaler > 0)
                scaler--;
            if (e.getKeyCode() == KeyEvent.VK_UP)
                scaler++;
            setCoordinates(0, 0, iniMax / Math.pow(1.2, scaler), 1, -1);
            rects.removeAllElements();
            realHeight = create(border, border, width - border, height - border, "");
            Graphics g = this.getGraphics();
            paint(g);
            g.dispose();
        } else if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                && (e.getKeyCode() == KeyEvent.VK_0 || e.getKeyCode() == KeyEvent.VK_NUMPAD0)) {
            scaler = 0;
            setCoordinates(0, 0, iniMax / Math.pow(1.2, scaler), 1, -1);
            rects.removeAllElements();
            realHeight = create(border, border, width - border, height - border, "");
            Graphics g = this.getGraphics();
            paint(g);
            g.dispose();
        } else if (Character.isSpaceChar(e.getKeyChar()) || Character.isJavaLetterOrDigit(e.getKeyChar()) || (e.getKeyChar() == KeyEvent.VK_PERIOD) || (e.getKeyChar() == KeyEvent.VK_MINUS)) {
            if (searchText.equals(""))
                startT = new Date().getTime();
            if (new Date().getTime() < startT + 1000) {
                searchText += e.getKeyChar();
            } else
                searchText = "" + e.getKeyChar();
            startT = new Date().getTime();
            System.out.println("Search Text: " + searchText);
            if (!searchText.equals(""))
                for (int i = 0; i < labels.size(); i++) {
                    String tmp = (((MyText) labels.elementAt(i)).getText());
                    if ((tmp.toUpperCase()).startsWith((searchText.toUpperCase()))) {
                        scrollTo(((MyText) labels.elementAt(i)).y - 20);
                        i = labels.size();
//				System.out.println("Table Text: "+tmp.toUpperCase()+" test against "+ searchText );
                    }
                }
        }
        super.processKeyEvent(e);  // Pass other event types on.
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("Barchart") || command.equals("Spineplot")) {
            displayMode = command;
            rects.removeAllElements();
            realHeight = create(border, border, width - border, height - border, "");
            Graphics g = this.getGraphics();
            paint(g);
            g.dispose();
        } else if (command.equals("abs") || command.equals("rel") || command.equals("lex") || command.equals("frq") || command.equals("rev")) {
            if (command.equals("abs") || command.equals("rel") || command.equals("lex") || command.equals("frq")) {
                double[] sortA = new double[this.k];
                Variable v = tablep.data.data.elementAt(tablep.initialVars[0]);
                //
                // first get all highlighting fixed
                //
                for (int i = 0; i < sortA.length; i++) {
                    MyRect r = (MyRect) rects.elementAt(i);
                    double sum = 0, sumh = 0;
                    for (int j = 0; j < r.tileIds.size(); j++) {
                        int id = ((Integer) (r.tileIds.elementAt(j))).intValue();
                        sumh += tablep.getSelected(id) * tablep.table[id];
                        sum += tablep.table[id];
                    }
                    r.setHilite(sumh / sum);
                }
                //
                // filling arrays to sort them
                //
                if (command.equals("frq"))
                    for (int i = 0; i < sortA.length; i++)
                        sortA[i] = ((MyRect) rects.elementAt(i)).obs;
                if (command.equals("abs"))
                    for (int i = 0; i < sortA.length; i++)
                        sortA[i] = ((MyRect) rects.elementAt(i)).getAbsHilite();
                if (command.equals("rel"))
                    for (int i = 0; i < sortA.length; i++)
                        sortA[i] = ((MyRect) rects.elementAt(i)).getHilite();
                int[] perm = Qsort.qsort(sortA, 0, sortA.length - 1);
                int[] tperm = new int[perm.length];
                for (int i = 0; i < perm.length; i++)
                    tperm[i] = v.permA[perm[i]];
                for (int i = 0; i < perm.length; i++) {      // Store results, ...
                    v.permA[i] = tperm[i];
                    v.IpermA[v.permA[i]] = i;
                }
                for (int i = 0; i < v.permA.length / 2; i++) {// ... and reverse the order!
                    int save = v.permA[i];
                    v.permA[i] = v.permA[v.permA.length - i - 1];
                    v.permA[v.permA.length - i - 1] = save;
                }
                for (int i = 0; i < v.permA.length; i++) {
                    v.IpermA[v.permA[i]] = i;
                }
                if (command.equals("lex"))
                    v.sortLevels();
            } else if (command.equals("rev")) {
                Variable v = tablep.data.data.elementAt(tablep.initialVars[0]);
                for (int i = 0; i < v.permA.length / 2; i++) {
                    int save = v.permA[i];
                    v.permA[i] = v.permA[v.permA.length - i - 1];
                    v.permA[v.permA.length - i - 1] = save;
                }
                for (int i = 0; i < v.permA.length; i++) {
                    v.IpermA[v.permA[i]] = i;
                }
            }
            this.dataFlag = true;                            // this plot was responsible
            eventID = tablep.initialVars[0];
            dataChanged(eventID);              // and is updated first!

            DataEvent de = new DataEvent(this);              // now the rest is informed ...
            evtq.postEvent(de);
        } else
            super.actionPerformed(e);
    }


    public int create(int x1, int y1, int x2, int y2, String info) {

//  System.out.println("Create: "+x1+" "+y1+" "+x2+" "+y2);

//  rects.removeAllElements();

        if (!rects.isEmpty())
            return realHeight;

        labels.removeAllElements();

        this.name = tablep.name;
        this.levels = tablep.levels;
        this.names = tablep.names;
        this.lnames = tablep.lnames;

        double sum = 0;
        Vector[] tileIds = new Vector[k];

        double tMax = 0;
        for (int i = 0; i < k; i++) {
            sum += tablep.table[i];
            tMax = Math.max(tMax, tablep.table[i]);
            tileIds[i] = new Vector(1, 0);
            tileIds[i].addElement(i);
        }

        max = getUrx();

        int pF = 1;
        if (printing)
            pF = printFactor;

        Image ti = createImage(10, 10);
        Graphics g = ti.getGraphics();
        FontMetrics FM = g.getFontMetrics();
        int fh = FM.getHeight() * pF;
        g.dispose();

        int x = 0;
        for (int i = 0; i < k; i++)
            if (tablep.data.phoneNumber(tablep.initialVars[0]))
                x = Math.max(x, FM.stringWidth(Utils.toPhoneNumber(Utils.atod(lnames[0][i]))));
            else
                x = Math.max(x, FM.stringWidth(lnames[0][i]));

        x = Math.min(x * pF, 100 * pF);

//  if( scaleFixed )
//    x = 100*pF;

        startX = x1 + x;
        int y = 0;

        int w;
        double h, hi;

        for (int i = 0; i < k; i++) {
            h = (Math.max(y2 - y1, k * 22) + 10 * pF) / k - 10 * pF;
            w = (x2 - x1 - x);
            if (tablep.data.phoneNumber(tablep.initialVars[0]))
                labels.addElement(new MyText(Utils.toPhoneNumber(Utils.atod(lnames[0][i])), x1 + x - 10 * pF, y1 + y + (int) (h / 2) + fh / 2));
            else {
                String shorty = lnames[0][i];
                String addOn = "";
                while (FM.stringWidth(shorty) >= 100) {
                    shorty = shorty.substring(0, shorty.length() - 1);
                    addOn = "...";
                }
                String print = shorty.trim() + addOn;
                if (FM.stringWidth(lnames[0][i]) <= FM.stringWidth(print))
                    print = lnames[0][i];
                labels.addElement(new MyText(print, x1 + x - 8 * pF, y1 + y + (int) (h / 2) + fh / 2));
            }
            hi = Math.max(12 * pF, h);

            if (displayMode.equals("Barchart")) {
                h = hi;
                w *= tablep.table[i] / max;
            } else {
                h *= tablep.table[i] / tMax;
                w = (int) (w * tMax / max);
            }
            // In Java 1.4 Boxes with "0" dimension in x or y are NOT drawn !!!!
            w = Math.max(w, 1);
            h = Math.max(h, 1);
            if (tablep.data.phoneNumber(tablep.initialVars[0]))
                rects.addElement(new MyRect(true, 'x', "Observed", x1 + x, y1 + y, w, (int) h,
                        tablep.table[i], tablep.table[i], 1, 0,
                        Utils.toPhoneNumber(Utils.atod(lnames[0][i])) + '\n', tileIds[i], tablep));
            else
                rects.addElement(new MyRect(true, 'x', "Observed", x1 + x, y1 + y, w, (int) h,
                        tablep.table[i], tablep.table[i], 1, 0, lnames[0][i] + '\n', tileIds[i], tablep));
            y += hi + 10 * pF;
        }

        realHeight = y - 10;

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


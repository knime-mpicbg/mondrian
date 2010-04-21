package org.rosuda.mondrian.core;
//
// A drag box - for my good friend Marvin.
//


import org.rosuda.mondrian.*;
import org.rosuda.mondrian.core.SelectionEvent;
import org.rosuda.mondrian.core.DataEvent;
import org.rosuda.mondrian.core.DataListener;
import org.rosuda.mondrian.plots.Barchart;
import org.rosuda.mondrian.plots.Histogram;
import org.rosuda.mondrian.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Vector;
/*import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;*/


public
abstract class DragBox

        extends JPanel
        implements MouseListener, MouseMotionListener, AdjustmentListener, ActionListener, Printable

{

   public static Color hiliteColor = new Color(180, 96, 135);

    public static boolean extSelMode = false;  // AND selction via <SHIFT><ALT>

    public  Color background = Color.black;
    public Color dragboxcolor = Color.red;
    public Graphics dragboxgraphics = null;

    public MFrame frame;                               // The frame we are within.
    public JScrollBar sb;                             // We might need a scroll bar

    public int colorSet = -1;                         // The color which as assigned with meta-n

    public boolean selectFlag;                        // True if selection occured in this DragBox

    public boolean dataFlag;                          // True if change occured in this DragBox

    public boolean selectAll = false;                          // True if the user pressed META-A

    public boolean unSelect = false;                          // True if election needs to be removed

    public boolean toggleSelection = false;                    // True if the user pressed META-K

    public boolean deleteAll = false;                          // True if the user pressed META-BACKSPACE

    public boolean switchSel = false;                          // True if the user pressed META-M

    public boolean switchAlpha = false;                          // True if the user pressed META-L

    public boolean changePop = false;                                                // True if the Sel Seq Popup was triggered

    public boolean scaleChanged = false;              // To indicate paint the new scale (without using events)

    public boolean printable = true;                      // Flag, if we can print this (for SPLOMs and other ensembles)

    public boolean printing;                               // flag to avoid double buffering while printing ...

    public boolean painting = false;

    public PrinterJob pj;

    public int printFactor = 1;                                                        // Increase in resolution for printing

    public Dimension printerPage;                               // must be accessible in different paints ...

    public LimitDialog LD;

    public boolean resizeReady = true;

    //
    // The PC implementation may need two minor changes:
    //
    //    1) BUTTON1_DOWN = InputEvent.BUTTON1_MASK
    //    2) Exchange definitions for buttons 2 and 3??
    //
    // Modifiers, as seen during button press - button 1 is strange!
    //

    protected final static int BUTTON1_DOWN = InputEvent.BUTTON1_MASK;
    protected final static int BUTTON2_DOWN = InputEvent.BUTTON2_MASK;
    protected final static int BUTTON3_DOWN = InputEvent.BUTTON3_MASK;

    //
    // Modifiers, as seen during button release - notice button 1.
    //

    protected final static int BUTTON1_UP = InputEvent.BUTTON1_MASK;
    protected final static int BUTTON2_UP = InputEvent.BUTTON2_MASK;
    protected final static int BUTTON3_UP = InputEvent.BUTTON3_MASK;

    //
    // Modifiers, as seen during button release - notice button 1.
    //

    protected final static int SHIFT_DOWN = InputEvent.SHIFT_MASK;
    protected final static int CTRL_DOWN = InputEvent.CTRL_MASK;
    protected final static int META_DOWN = InputEvent.META_MASK;
    protected final static int ALT_DOWN = InputEvent.ALT_MASK;

    //
    // Mouse status.
    //

    protected final int AVAILABLE = 0;
    protected final int DRAGGING = 1;
    protected final int MOVING = 2;
    protected final int RESIZEN = 3;
    protected final int RESIZENE = 4;
    protected final int RESIZEE = 5;
    protected final int RESIZESE = 6;
    protected final int RESIZES = 7;
    protected final int RESIZESW = 8;
    protected final int RESIZEW = 9;
    protected final int RESIZENW = 10;
    protected final int CHANGE = 11;
    protected final int ZOOMING = 12;
    protected int mouse = AVAILABLE;

    //
    // System Type.
    //

    protected final int MAC = 32;
    protected final int WIN = 64;
    protected final int LNX = 128;
    protected final int NN = 0;

    protected int SYSTEM;

    protected int movingID = 0;

    //
    // Permanent arrays for drawing polygon.
    //

    protected int xcorner[] = {0, 0, 0, 0};
    protected int ycorner[] = {0, 0, 0, 0};

    protected int diffX;
    protected int diffY;

    protected int border = 0;
    protected int xShift = 0;
    protected int yShift = 0;

    public Vector Selections = new Vector(10, 0);

    protected Selection activeS;

    protected int minX = 0;
    protected int minY = 0;
    protected int maxX = 10000;
    protected int maxY = 10000;
    protected int maxWidth = 10000;
    protected int maxHeight = 10000;

    ///////////////////////// World - Screen Coordinates ///////////////////////////////////

    protected double hllx, llx;
    protected double hlly, lly;

    protected double hurx, urx;
    protected double hury, ury;

    protected double aspectRatio = -1;

    protected Vector zooms = new Vector(10, 0);

    ///////////////////////// Methods for Coordinates /////////////////////////////////////


    public void setCoordinates(double llx, double lly, double urx, double ury, double aspectRatio) {

        this.hllx = llx;
        this.hlly = lly;
        this.hurx = urx;
        this.hury = ury;
        this.aspectRatio = aspectRatio;
        reScale(llx, lly, urx, ury);
    }


    public void setAspect(double aspectRatio) {
        this.aspectRatio = aspectRatio;
        updateScale();
    }


    public double getAspect() {
        return aspectRatio;
    }


    public void flipAxes() {
        double tmp;
        tmp = hllx;
        hllx = hlly;
        hlly = tmp;
        tmp = hurx;
        hurx = hury;
        hury = tmp;
        tmp = llx;
        llx = lly;
        lly = tmp;
        tmp = urx;
        urx = ury;
        ury = tmp;

        aspectRatio = 1 / aspectRatio;

        zooms.removeAllElements();
        zooms.addElement(new double[]{hllx, hlly, hurx, hury});
        reScale(llx, lly, urx, ury);
    }


    public void reScale(double llx, double lly, double urx, double ury) {

        this.llx = llx;
        this.lly = lly;
        this.urx = urx;
        this.ury = ury;

        if (this instanceof Histogram)
            this.lly = 0;

        zooms.addElement(new double[]{this.llx, this.lly, this.urx, this.ury});
        updateScale();
    }


    public void updateScale() {
        double mllx = ((double[]) (zooms.elementAt(zooms.size() - 1)))[0];
        double mlly = ((double[]) (zooms.elementAt(zooms.size() - 1)))[1];
        double murx = ((double[]) (zooms.elementAt(zooms.size() - 1)))[2];
        double mury = ((double[]) (zooms.elementAt(zooms.size() - 1)))[3];

        Dimension size = this.getSize();
        size.width -= 2 * border;
        size.height -= 2 * border;
        if (size.width > 0 && size.height > 0)
            if (aspectRatio > 0)
                if ((double) size.width / (double) size.height < (murx - mllx) / (mury - mlly)) {
                    this.ury = ((double) size.height / (double) size.width) * ((murx - mllx) / (mury - mlly)) * (mury - mlly) + mlly;
                    this.urx = murx;
                } else {
                    this.ury = mury;
                    this.urx = ((double) size.width / (double) size.height) * ((mury - mlly) / (murx - mllx)) * (murx - mllx) + mllx;
                }
            else {
                this.llx = mllx;
                this.lly = mlly;
                this.urx = murx;
                this.ury = mury;
            }
        //System.out.println("Height: "+size.height+" Width: "+size.width+" llx: "+llx+" lly: "+lly+" urx: "+urx+" ury:"+ury+" #:"+zooms.size());
    }


    public void home() {
        llx = this.hllx;
        lly = this.hlly;
        urx = this.hurx;
        ury = this.hury;

        zooms.removeAllElements();
        zooms.addElement(new double[]{llx, lly, urx, ury});
    }


    public double userToWorldX(double x) {

        Dimension size = this.getSize();
        size.width -= 2 * border;
        //    return (x - llx) / (urx - llx) * size.width;
        return border + xShift + (x - llx) / (urx - llx) * size.width;
    }


    public double userToWorldY(double y) {

        Dimension size = this.getSize();
        size.height -= 2 * border;
        //    return size.height - (y - lly) / (ury - lly) * size.height;
        return border + yShift + size.height - (y - lly) / (ury - lly) * size.height;
    }


    public double worldToUserX(int x) {

        Dimension size = this.getSize();
        size.width -= 2 * border;
        //    return llx + (urx - llx) * x / size.width;
        return llx + (urx - llx) * (x - border - xShift) / size.width;
    }


    public double worldToUserY(int y) {

        Dimension size = this.getSize();
        size.height -= 2 * border;
        //    return lly + (ury - lly) * (size.height - y) / size.height;
        return lly + (ury - lly) * (size.height - (y - border - yShift)) / size.height;
    }


    public double getLlx() {
        return llx;
    }


    public double getLly() {
        return lly;
    }


    public double getUrx() {
        return urx;
    }


    public double getUry() {
        return ury;
    }
    ///////////////////////////////////////////////////////////////////////////


    public DragBox(MFrame frame) {

        this.frame = frame;

//      if( this instanceof PC )                     // Was thought to "unlive" the resize, but sync problems killed it!
//        frame.initComponents(this);

        if (((System.getProperty("os.name")).toLowerCase()).indexOf("mac") > -1)
            SYSTEM = MAC;
        else if (((System.getProperty("os.name")).toLowerCase()).indexOf("win") > -1)
            SYSTEM = WIN;
        else if (((System.getProperty("os.name")).toLowerCase()).indexOf("linux") > -1)
            SYSTEM = LNX;
        else
            SYSTEM = NN;

        ToolTipManager.sharedInstance().registerComponent(this);
        if (SYSTEM != MAC)
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        else
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);

        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        ToolTipManager.sharedInstance().setReshowDelay(0);
        this.setToolTipText("<HTML>hold CTRL to query objects<br>hold SHIRT+CTRL for extended query</HTML>");

        addMouseListener(this);
        addMouseMotionListener(this);

        evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();
        enableEvents(0);

        sb = new JScrollBar(Scrollbar.VERTICAL, 0, 300, 0, 300);
        sb.addAdjustmentListener(this);
        sb.setVisible(false);

        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.requestFocus();

        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });
    }


    public void setScrollX() {
        frame.getContentPane().add(sb, "East");
    }


    public void setDragBoxConstraints(int minX, int minY, int maxX, int maxY, int maxWidth, int maxHeight) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }


    public Dimension getViewportSize() {

        Dimension size;

        size = frame.getSize();
        size.height -= (frame.getInsets().top + frame.getInsets().bottom);
        size.width -= (frame.getInsets().right + frame.getInsets().left);

        return size;
    }


    public void setSize(int widthN, int heightN) {

        Dimension size;
        size = frame.getSize();
        size.height -= (frame.getInsets().top + frame.getInsets().bottom);
        size.width -= (frame.getInsets().right + frame.getInsets().left);

        if (heightN > size.height) {
            super.setSize(size.width, size.height);
            sb.setValues(sb.getValue(), size.height, 0, heightN);
            sb.setUnitIncrement(22);
            sb.setBlockIncrement((frame.getSize()).height);
            if (!sb.isVisible()) {
                sb.setVisible(true);
                frame.setVisible(true);
            }
        } else {
            super.setSize(size.width, size.height);
            sb.setValues(0, size.height, 0, size.height);
            sb.setVisible(false);
        }
    }


    public void update(Graphics g) {
        paint(g);
    }


    public Color getHiliteColor() {
        return hiliteColor;
    }


    public void addSelectionListener(SelectionListener l) {
        slistener = l;
    }


    public void addDataListener(DataListener l) {
        dlistener = l;
    }


    public void processEvent(AWTEvent evt) {
        if (evt instanceof SelectionEvent) {
            if (slistener != null)
                slistener.updateSelection();
        }
        if (evt instanceof DataEvent) {
            if (dlistener != null)
                dlistener.dataChanged(-1);
        } else super.processEvent(evt);
    }

    ///////////////////////////////////////////////////////////////////////////


    public void mouseDragged(MouseEvent e) {

        if (mouse != CHANGE) {
            dragBox(e.getX(), e.getY(), e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    public void mousePressed(MouseEvent e) {

//System.out.println("SYSTEM = "+SYSTEM);
//System.out.println("mouse = "+mouse);
        System.out.println("Mouse press: ... " + e.getModifiers() + " " + BUTTON1_DOWN + " " + SHIFT_DOWN + " " + ALT_DOWN + " " + CTRL_DOWN);
        System.out.println("getButton() = " + e.getButton());

        if (mouse == AVAILABLE) {
            if (e.getModifiers() == BUTTON1_DOWN ||
                    e.getModifiers() == BUTTON1_DOWN + SHIFT_DOWN ||
                    e.getModifiers() == BUTTON1_DOWN + SHIFT_DOWN + ALT_DOWN ||
                    e.getModifiers() == BUTTON1_DOWN + SHIFT_DOWN + ALT_DOWN + CTRL_DOWN)
                dragBegin(e.getX(), e.getY(), e);
            if (e.getModifiers() == BUTTON1_DOWN + META_DOWN && SYSTEM == MAC ||
                    e.getModifiers() == BUTTON2_DOWN && SYSTEM != MAC) {
                mouse = ZOOMING;
                System.out.println("Start ZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOMING");
                dragBegin(e.getX(), e.getY(), e);
            }
            if ((e.isPopupTrigger()) && !e.isShiftDown() && e.getModifiers() != 24) {  // || (e.getModifiers() ==  BUTTON3_DOWN && SYSTEM == WIN)
                mouse = CHANGE;
                System.out.println("Start CHANGGEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
                dragBegin(e.getX(), e.getY(), e);
            }
        }   // End if //
    }

    ///////////////////////////////////////////////////////////////////////////


    public void
    mouseReleased(MouseEvent e) {

        int ev;
        ev = e.getModifiers();

        if (e.isPopupTrigger() && SYSTEM == WIN && !e.isShiftDown()) {
            mouse = CHANGE;
            System.out.println("Start CHANGGEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
            dragBegin(e.getX(), e.getY(), e);
        }

        System.out.println("Mouse rel: ... " + ev + " " + BUTTON1_UP + " " + SHIFT_DOWN + " " + ALT_DOWN);
        if (mouse != AVAILABLE) {
            switch (ev) {
                case 0:
                case 1:
                case 2:
                    dragEnd(e);
                    mouse = AVAILABLE;
                    break;
                case BUTTON1_UP:
                case BUTTON1_UP + CTRL_DOWN:
                case BUTTON1_UP + SHIFT_DOWN:
                case SHIFT_DOWN + ALT_DOWN:
                case BUTTON1_UP + SHIFT_DOWN + ALT_DOWN:
                case BUTTON1_UP + META_DOWN:
                case BUTTON2_UP:
                case BUTTON3_UP:
                    if (mouse != CHANGE) {
                        System.out.println(" dragEnd! ");
                        dragEnd(e);
                    }
                    mouse = AVAILABLE;
                    break;
            }    // End switch //
        }   // End if //
    }

    ///////////////////////////////////////////////////////////////////////////


    public void mouseClicked(MouseEvent e) {
    }


    public void mouseEntered(MouseEvent e) {
    }


    public void mouseExited(MouseEvent e) {
    }


    public void mouseMoved(MouseEvent e) {
    }

    ///////////////////////////////////////////////////////////////////////////


    public void dragboxCallback(int x0, int y0, int x1, int y1, MouseEvent e) {

        SelectionEvent se;
        Selection S;
        Rectangle sr;

        int lx = Math.min(x0, x1);
        int ly = Math.min(y0, y1);
        int lw = Math.abs(x1 - x0);
        int lh = Math.abs(y1 - y0);

        sr = new Rectangle(lx, ly, lw, lh);

        int modifiers = e.getModifiers();
//System.out.println("modifiers callback = "+e.getModifiers());
//System.out.println("mouse "+mouse);

        if (modifiers == BUTTON1_UP ||
                modifiers == BUTTON1_UP + META_DOWN && SYSTEM == MAC ||
                modifiers == BUTTON2_UP && SYSTEM != MAC) {
            if (mouse != CHANGE && mouse != ZOOMING) {
                if (mouse == DRAGGING) {
                    S = new Selection(sr, null, 0, Selection.MODE_STANDARD, this);
                    activeS = S;
                    Selections.addElement(S);
                } else {
                    if (Selections.size() > 0) {
                        S = ((Selection) Selections.elementAt(movingID));
                        S.r = sr;
                    }
                }
                selectFlag = true;
                se = new SelectionEvent(this);
                evtq.postEvent(se);
            } else if (mouse == ZOOMING) {
                if (Math.abs(x0 - x1) < 5 && Math.abs(y0 - y1) < 5) {
                    if (zooms.size() > 1) {
                        zooms.removeElementAt(zooms.size() - 1);
                        reScale(((double[]) (zooms.elementAt(zooms.size() - 1)))[0],
                                ((double[]) (zooms.elementAt(zooms.size() - 1)))[1],
                                ((double[]) (zooms.elementAt(zooms.size() - 1)))[2],
                                ((double[]) (zooms.elementAt(zooms.size() - 1)))[3]);
                        zooms.removeElementAt(zooms.size() - 1);
                    }
                } else // Zoom in, and make sure a) not to invert the scale, b) not to leave the range
                    reScale(Math.max(hllx, worldToUserX(Math.min(x0, x1))),
                            Math.max(hlly, worldToUserY(Math.max(y0, y1))),
                            Math.min(hurx, worldToUserX(Math.max(x0, x1))),
                            Math.min(hury, worldToUserY(Math.min(y0, y1))));
                scaleChanged = true;
                update(this.getGraphics());
            }
        }
        if (modifiers == BUTTON1_UP + SHIFT_DOWN) {
            if (mouse == DRAGGING) {
                S = new Selection(sr, null, 0, Selection.MODE_XOR, this);
                activeS = S;
                Selections.addElement(S);
            } else {
                //            System.out.println("Painting ............");
                S = ((Selection) Selections.elementAt(movingID));
                S.r = sr;
                S.setMode(Selection.MODE_OR);
            }
            selectFlag = true;
            se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (modifiers == BUTTON1_UP + SHIFT_DOWN + ALT_DOWN || modifiers == SHIFT_DOWN + ALT_DOWN && SYSTEM == MAC) {
            if (mouse == DRAGGING) {
                if (extSelMode)               // Choose right extended selction via <SHIFT><ALT>
                    S = new Selection(sr, null, 0, Selection.MODE_OR, this);
                else
                    S = new Selection(sr, null, 0, Selection.MODE_AND, this);
                activeS = S;
                Selections.addElement(S);
            } else {
                S = ((Selection) Selections.elementAt(movingID));
                S.r = sr;
            }
            selectFlag = true;
            se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (modifiers == BUTTON1_UP + SHIFT_DOWN + ALT_DOWN + CTRL_DOWN) {
            if (mouse == DRAGGING) {
                S = new Selection(sr, null, 0, Selection.MODE_AND, this);
                activeS = S;
                Selections.addElement(S);
            } else {
                S = ((Selection) Selections.elementAt(movingID));
                S.r = sr;
            }
            selectFlag = true;
            se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    public abstract void maintainSelection(Selection s);


    public abstract void updateSelection();

    ///////////////////////////////////////////////////////////////////////////


    public abstract void dataChanged(int id);


    public abstract void paint(Graphics2D g);


    public void paint(Graphics g) {
        paint((Graphics2D) g);
    }


    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {

        if (pageIndex > 0) {
            return (NO_SUCH_PAGE);
        } else {
//         printFactor = 5;
            System.out.println(" P R I N T I N G at: " + pageFormat.getImageableWidth() + " by " + pageFormat.getImageableHeight() + "printFactor: " + printFactor);
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pageFormat.getImageableX() + pageFormat.getImageableWidth() * 0.05,
                    pageFormat.getImageableY() + pageFormat.getImageableHeight() * 0.05);
            g2d.scale(1.0 / printFactor, 1.0 / printFactor);
            // Turn off double buffering
            Dimension save = this.getViewportSize();
//        int setWidth  = (int)(pageFormat.getImageableWidth()*0.9)*printFactor;
//        int setHeight = (int)(pageFormat.getImageableHeight()*0.9)*printFactor;
            int setWidth = save.width;
            int setHeight = save.height;
            if (aspectRatio == -1)
                if ((double) setWidth / (double) setHeight
                        < (double) save.width / (double) save.height)
                    setHeight = (int) ((double) (setWidth * ((double) save.height / (double) save.width)));
                else
                    setWidth = (int) ((double) (setHeight * ((double) save.width / (double) save.height)));
            super.setSize(setWidth, setHeight);
            g2d.setFont(new Font("SansSerif", 0, 11 * printFactor));
            printing = true;
            this.paint(g2d);
            printing = false;
            this.setSize(save);
            // Turn double buffering back on
            return (PAGE_EXISTS);
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    public abstract void adjustmentValueChanged(AdjustmentEvent e);


    public abstract void scrollTo(int id);

    ///////////////////////////////////////////////////////////////////////////


    public void drawBoldDragBox(Graphics g, Selection S) {

        Rectangle r = S.r;

        g.setColor(new Color(255, 255, 255, 90));

        g.fillRect(r.x, r.y, r.width, r.height);

        g.setColor(new Color(255, 255, 255, 150));

        if (mouse != ZOOMING && mouse != DRAGGING && mouse != AVAILABLE)
            g.drawString(S.step + "", r.x + r.width / 2 - 3, r.y + r.height / 2 + 5);

        if (S == activeS)
            g.setColor(new Color(0, 0, 0, 150));
        else
            g.setColor(new Color(255, 255, 255, 90));

//      g.drawRect(r.x, r.y, r.width, r.height);
//      g.drawRect(r.x-1, r.y-1, r.width+2, r.height+2);

        g.fillRect(r.x - 4, r.y - 4, 4, 4);
        g.fillRect(r.x + r.width / 2 - 2, r.y - 4, 4, 4);
        g.fillRect(r.x + r.width, r.y - 4, 4, 4);

        g.fillRect(r.x - 4, r.y + r.height / 2 - 2, 4, 4);
        g.fillRect(r.x + r.width, r.y + r.height / 2 - 2, 4, 4);

        g.fillRect(r.x - 4, r.y + r.height, 4, 4);
        g.fillRect(r.x + r.width / 2 - 2, r.y + r.height, 4, 4);
        g.fillRect(r.x + r.width, r.y + r.height, 4, 4);
    }


    public void
    setColor(Color c) {

        dragboxcolor = c;
    }

    ///////////////////////////////////////////////////////////////////////////


    protected void
    drawDragBox() {

        if (xcorner[0] != xcorner[2] || ycorner[0] != ycorner[2]) {
            int[] drawYCorner = new int[4];
            for (int i = 0; i < 4; i++)
                drawYCorner[i] = ycorner[i] - sb.getValue();
            dragboxgraphics.drawPolygon(xcorner, drawYCorner, 4);
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    protected void dragBegin(int x, int y, MouseEvent e) {

        boolean inBox = false;
        Rectangle sr = null;

        int modifiers = e.getModifiers();

        if (dragboxgraphics == null) {
            dragboxgraphics = getGraphics();
            dragboxgraphics.setColor(dragboxcolor);
            dragboxgraphics.setXORMode(getBackground());
        }   // End if //

        System.out.println("Mouse Action before check: " + mouse);
        if (mouse != ZOOMING) {
            mouse = DRAGGING;
            for (int i = 0; i < Selections.size(); i++) {
                int locMouse = determineAction(((Selection) Selections.elementAt(i)).r, new Point(x, y + sb.getValue()));
                if ((locMouse <= 10) && (locMouse >= 2))
                    activeS = (Selection) Selections.elementAt(i);
                if (locMouse != DRAGGING && mouse != ZOOMING) {
                    movingID = i;
                    mouse = locMouse;
                    sr = ((Selection) Selections.elementAt(movingID)).r;
                    xcorner[0] = sr.x;
                    xcorner[1] = xcorner[0];
                    xcorner[2] = sr.x + sr.width;
                    xcorner[3] = xcorner[2];
                    ycorner[0] = sr.y;
                    ycorner[1] = sr.y + sr.height;
                    ycorner[2] = ycorner[1];
                    ycorner[3] = ycorner[0];
                }
            }
        } else
            mouse = ZOOMING;

        System.out.println("Mouse Action to check: " + mouse);

        switch (mouse) {

            case DRAGGING:
            case ZOOMING:
                if ((e.isPopupTrigger() || (e.getModifiers() == BUTTON3_DOWN && SYSTEM == WIN)) && !e.isShiftDown()) {
                    System.out.println(" pop up in nowhere !!");
                    mouse = AVAILABLE;
                } else {
                    xcorner[0] = x;
                    ycorner[0] = y + sb.getValue();
                    xcorner[2] = xcorner[0];
                    ycorner[2] = ycorner[0];
                    System.out.println("Mouse Action: DRAGGING");
                }
                break;
            case MOVING:
                if (modifiers == BUTTON1_DOWN) {
                    diffX = sr.x - x;
                    diffY = sr.y - (y + sb.getValue());
                } else if (e.isPopupTrigger() || (e.getModifiers() == BUTTON3_DOWN && SYSTEM == WIN) && !e.isShiftDown()) { // modifiers == BUTTON1_DOWN + SHIFT_DOWN ) {
                    mouse = CHANGE;
//System.out.println("Get/Change Info of Brush No: "+movingID);
                    JPopupMenu changeSelection = new JPopupMenu();
                    Selection S = ((Selection) Selections.elementAt(movingID));
                    int selStep = S.step;
                    JMenuItem Step = new JMenuItem("Step: " + selStep);
                    changeSelection.add(Step);

                    //	  PopupMenu mode = new PopupMenu(S.getModeString(S.mode));
                    JMenu mode = new JMenu(Selection.getModeString(S.mode));
                    JMenuItem modeM = new JMenuItem(Selection.getModeString(S.mode));

                    if (selStep > 1 || S.total == 1) {
                        if (S.mode != Selection.MODE_STANDARD) {
                            JMenuItem Replace = new JMenuItem(Selection.getModeString(Selection.MODE_STANDARD));
                            mode.add(Replace);
                            Replace.addActionListener(this);
                            Replace.setActionCommand(Selection.getModeString(Selection.MODE_STANDARD));
                        }
                        if (S.mode != Selection.MODE_AND && selStep > 1) {
                            JMenuItem And = new JMenuItem("And");
                            mode.add(And);
                            And.addActionListener(this);
                            And.setActionCommand(Selection.getModeString(Selection.MODE_AND));
                        }
                        if (S.mode != Selection.MODE_OR) {
                            JMenuItem Or = new JMenuItem("Or");
                            mode.add(Or);
                            Or.addActionListener(this);
                            Or.setActionCommand(Selection.getModeString(Selection.MODE_OR));
                        }
                        if (S.mode != Selection.MODE_XOR && selStep > 1) {
                            JMenuItem XOr = new JMenuItem("Xor");
                            mode.add(XOr);
                            XOr.addActionListener(this);
                            XOr.setActionCommand(Selection.getModeString(Selection.MODE_XOR));
                        }
                        if (S.mode != Selection.MODE_NOT) {
                            JMenuItem Not = new JMenuItem("Not");
                            mode.add(Not);
                            Not.addActionListener(this);
                            Not.setActionCommand(Selection.getModeString(Selection.MODE_NOT));
                        }
                        changeSelection.add(mode);
                    } else
                        changeSelection.add(modeM);

                    JMenuItem Delete = new JMenuItem("Delete");
                    changeSelection.add(Delete);
                    Delete.setAccelerator(KeyStroke.getKeyStroke(Event.BACK_SPACE, 0));

                    Delete.setActionCommand("Delete");
                    Delete.addActionListener(this);

                    JMenuItem DeleteAll = new JMenuItem("Delete All");
                    changeSelection.add(DeleteAll);
                    DeleteAll.setAccelerator(KeyStroke.getKeyStroke(Event.BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

                    DeleteAll.setActionCommand("DeleteAll");
                    DeleteAll.addActionListener(this);

                    frame.getContentPane().add(changeSelection);
                    changeSelection.show(e.getComponent(), e.getX(), e.getY());

                    changePop = true;

                    mouse = AVAILABLE;                            // We don't get a BOTTON_RELEASED Event on a popup
                }
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    protected void dragBox(int x, int y, MouseEvent e) {

        if ((mouse != MOVING) && (Math.abs(xcorner[1] - x) > maxWidth))
            x = xcorner[2];

        if (mouse == DRAGGING || mouse == ZOOMING)
            drawDragBox();

        if (y <= 10) {
            scrollTo(sb.getValue() - 22);
        }
        if (y >= ((frame.getSize()).height - (frame.getInsets().top + frame.getInsets().bottom) - 10)) {
            scrollTo(sb.getValue() + 22);
        }

        if (mouse != DRAGGING && mouse != ZOOMING) {
/*        EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();

        AWTEvent evt = null;
        AWTEvent pE = eq.peekEvent();

        while( (pE != null) && ((pE.getID() == MouseEvent.MOUSE_DRAGGED) ||
                                (pE.getID() == SelectionEvent.SELECTION_EVENT) ))
          try {
            evt = eq.getNextEvent();
            pE = eq.peekEvent();
//System.out.println("====> trashed Event!"+evt.toString());
          }
        catch( InterruptedException ex)
        {}  */

            switch (mouse) {
                case MOVING:
                    Rectangle sr = ((Selection) Selections.elementAt(movingID)).r;
                    xcorner[0] = x + diffX;
                    xcorner[1] = xcorner[0];
                    xcorner[2] = x + sr.width + diffX;
                    xcorner[3] = xcorner[2];
                    ycorner[0] = y + diffY + sb.getValue();
                    ycorner[1] = y + sr.height + diffY + sb.getValue();
                    ycorner[2] = ycorner[1];
                    ycorner[3] = ycorner[0];
                    break;
                case RESIZENW:
                    xcorner[0] = x;
                    ycorner[0] = y + sb.getValue();
                    xcorner[1] = x;
                    ycorner[3] = y + sb.getValue();
                    break;
                case RESIZEN:
                    ycorner[0] = y + sb.getValue();
                    ycorner[3] = y + sb.getValue();
                    break;
                case RESIZENE:
                    xcorner[3] = x;
                    ycorner[0] = y + sb.getValue();
                    xcorner[2] = x;
                    ycorner[3] = y + sb.getValue();
                    break;
                case RESIZEE:
                    xcorner[2] = x;
                    xcorner[3] = x;
                    break;
                case RESIZESE:
                    xcorner[2] = x;
                    ycorner[2] = y + sb.getValue();
                    xcorner[3] = x;
                    ycorner[1] = y + sb.getValue();
                    break;
                case RESIZES:
                    ycorner[1] = y + sb.getValue();
                    ycorner[2] = y + sb.getValue();
                    break;
                case RESIZESW:
                    xcorner[0] = x;
                    ycorner[2] = y + sb.getValue();
                    xcorner[1] = x;
                    ycorner[1] = y + sb.getValue();
                    break;
                case RESIZEW:
                    xcorner[0] = x;
                    xcorner[1] = x;
                    break;
            }
            dragboxCallback(xcorner[0], ycorner[0], xcorner[2], ycorner[2], e);
        } else {
            xcorner[1] = xcorner[0];
            ycorner[1] = y + sb.getValue();
            xcorner[2] = x;
            ycorner[2] = ycorner[1];
            xcorner[3] = xcorner[2];
            ycorner[3] = ycorner[0];

            drawDragBox();
        }
    }

    ///////////////////////////////////////////////////////////////////////////


    protected void
    dragEnd(MouseEvent e) {
        // MTh   drawDragBox();
        dragboxgraphics.dispose();
        dragboxgraphics = null;

        dragboxCallback(xcorner[0], ycorner[0], xcorner[2], ycorner[2], e);
    }

    ///////////////////////////////////////////////////////////////////////////


    public void processKeyEvent(KeyEvent e) {

        if (printable &&
                (e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_P) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {

            pj = PrinterJob.getPrinterJob();
            PageFormat pageFormat = pj.defaultPage();
            Dimension size = this.getSize();
            if (size.width > size.height)
                pageFormat.setOrientation(PageFormat.LANDSCAPE);
            else
                pageFormat.setOrientation(PageFormat.PORTRAIT);

            pageFormat = pj.pageDialog(pageFormat);
            if (pageFormat != null) {
                //        pageFormat = pj.validatePage(pageFormat);
                pj.setPrintable(this, pageFormat);

                if (pj.printDialog()) {
                    try {
                        pj.print();
                    }
                    catch (PrinterException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
        if (SYSTEM != MAC &&
                (e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_A) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {

            selectAll = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (SYSTEM != MAC &&
                (e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_K) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {

            toggleSelection = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_1) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 1;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_2) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 2;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_3) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 3;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_4) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 4;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_5) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 5;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_6) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 6;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_7) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 7;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_8) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 8;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) && (e.getKeyCode() == KeyEvent.VK_9) && (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 9;
            DataEvent de = new DataEvent(this);
            evtq.postEvent(de);
            unSelect = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (SYSTEM != MAC &&
                (e.getID() == KeyEvent.KEY_PRESSED) && e.getKeyCode() == KeyEvent.VK_B && e.getModifiers() == (InputEvent.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            colorSet = 999;
            DataEvent de = new DataEvent(this);              // now the rest is informed ...
            evtq.postEvent(de);
        }

        // Fire up min-max dialog
        if ((printable &&
                e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_J) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {

            LD = new LimitDialog(this);
            LD.setVisible(true);
        }
        if ((e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == Event.BACK_SPACE)) {
            if (Selections.size() > 0) {
                //  Selection S = (Selection)Selections.lastElement();
                int activeIndex = Selections.indexOf(activeS);

                Selections.removeElement(activeS);
                activeS.status = Selection.KILLED;
                if (activeIndex > 0) activeS = (Selection) Selections.elementAt(activeIndex - 1);

                SelectionEvent se = new SelectionEvent(this);
                evtq.postEvent(se);
            }
        }
        if (SYSTEM != MAC &&
                (e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == Event.BACK_SPACE) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            System.out.println("Delete All Selections");
            deleteAll = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (SYSTEM != MAC &&
                (e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_M) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            System.out.println("Switch Selection Mode");
            switchSel = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (SYSTEM != MAC &&                        // Global Shortcuts are handled by the menu on the Mac!!!
                (e.getID() == KeyEvent.KEY_PRESSED) &&
                (e.getKeyCode() == KeyEvent.VK_L) &&
                (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
            System.out.println("Switch Alpha Mode");
            switchAlpha = true;
            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
        if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_W) {
            frame.close();
        }
        if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_C) {
            // todo renable me
//            ImageSelection.copyComponent(this, false, true);
        }
    }


    public void keyPressed(KeyEvent e) {
        System.out.println("Key typed");
    }


    public void keyReleased(KeyEvent e) {
        System.out.println("Key typed");
    }


    public int determineAction(Rectangle r, Point p) {

        int tolerance = 6;

        if ((new Rectangle(r.x, r.y, r.width, r.height)).contains(p))
            return MOVING;

        if (((new HotRect(r.x - 3, r.y - 3, 4, 4)).larger(tolerance)).contains(p))
            return RESIZENW;

        if (((new HotRect(r.x + r.width / 2 - 2, r.y - 3, 4, 4)).larger(tolerance)).contains(p))
            return RESIZEN;

        if (((new HotRect(r.x + r.width, r.y - 3, 4, 4)).larger(tolerance)).contains(p))
            return RESIZENE;

        if (((new HotRect(r.x - 3, r.y + r.height / 2 - 2, 4, 4)).larger(tolerance)).contains(p))
            return RESIZEW;

        if (((new HotRect(r.x + r.width, r.y + r.height / 2 - 2, 4, 4)).larger(tolerance)).contains(p))
            return RESIZEE;

        if (((new HotRect(r.x - 3, r.y + r.height, 4, 4)).larger(tolerance)).contains(p))
            return RESIZESW;

        if (((new HotRect(r.x + r.width / 2 - 2, r.y + r.height, 4, 4)).larger(tolerance)).contains(p))
            return RESIZES;

        if (((new HotRect(r.x + r.width, r.y + r.height, 4, 4)).larger(tolerance)).contains(p))
            return RESIZESE;

        return DRAGGING;
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals("Apply") || command.equals("OK") || command.equals("Home") || command.equals("Cancel")) {
            if (command.equals("Apply") || command.equals("OK")) {
                double xMin = Util.atod(LD.tfXMinI.getText());
                double yMin = Util.atod(LD.tfYMinI.getText());
                double xMax = Util.atod(LD.tfXMaxI.getText());
                double yMax = Util.atod(LD.tfYMaxI.getText());
                double width = Math.max(150, Util.atod(LD.tfWidthI.getText()));
                double height = Math.max(100, Util.atod(LD.tfHeightI.getText()));

                frame.setSize((int) width, (int) height);
                if (xMax > xMin && yMax > yMin) {
                    this.reScale(xMin, yMin, xMax, yMax);
                    scaleChanged = true;
                    update(this.getGraphics());
                    if (command.equals("OK"))
                        LD.dispose();
                } else
                    Toolkit.getDefaultToolkit().beep();

            } else if (command.equals("Home")) {

                this.home();

                int roundX = (int) Math.max(0, 2 - Math.round((Math.log(urx - llx) / Math.log(10))));
                int roundY = (int) Math.max(0, 2 - Math.round((Math.log(ury - lly) / Math.log(10))));

                LD.tfXMinI.setText(Stat.roundToString(getLlx(), roundX));
                LD.tfXMaxI.setText(Stat.roundToString(getUrx(), roundX));
                LD.tfYMinI.setText(Stat.roundToString(getLly(), roundY));
                LD.tfYMaxI.setText(Stat.roundToString(getUry(), roundY));

                LD.tfWidthI.setText("" + frame.getWidth());
                LD.tfHeightI.setText("" + frame.getHeight());
                scaleChanged = true;
                update(this.getGraphics());
            }

            if (command.equals("Cancel")) {
                if (command.equals("Cancel")) {
                    while (LD.last < zooms.size())
                        zooms.remove(LD.last);
                    scaleChanged = true;
                    update(this.getGraphics());
                }
                LD.dispose();
            }

        } else {

            Selection S = ((Selection) Selections.elementAt(movingID));
            if (command.equals("Delete")) {    // die Abfrage nach der aktivierten Selektion kann man sich sparen - es ist sowieso nur moeglich auf aktivierte Elemente zuzugreifen
                Selections.removeElement(S);
                S.status = Selection.KILLED;

                if (Selections.size() > 1)
                    activeS = (Selection) Selections.elementAt(Selections.size() - 1);    // solange nur aktivierte Element geloescht werden koennen, reicht es
                // anschliessend das vorletzte Element zu selektieren
            }
            if (command.equals("DeleteAll")) {
                deleteAll = true;
            }
            if (command.equals(Selection.getModeString(Selection.MODE_STANDARD)))
                S.mode = Selection.MODE_STANDARD;
            if (command.equals(Selection.getModeString(Selection.MODE_AND)))
                S.mode = Selection.MODE_AND;
            if (command.equals(Selection.getModeString(Selection.MODE_OR)))
                S.mode = Selection.MODE_OR;
            if (command.equals(Selection.getModeString(Selection.MODE_XOR)))
                S.mode = Selection.MODE_XOR;
            if (command.equals(Selection.getModeString(Selection.MODE_NOT)))
                S.mode = Selection.MODE_NOT;

            SelectionEvent se = new SelectionEvent(this);
            evtq.postEvent(se);
        }
    }


    private SelectionListener slistener;
    private DataListener dlistener;
    private static EventQueue evtq;


    class HotRect extends Rectangle {

        public HotRect(int x, int y, int w, int h) {
            super.x = x;
            super.y = y;
            super.width = w;
            super.height = h;
        }


        public Rectangle larger(int t) {
            return new Rectangle(this.x - t, this.y - t, this.width + 2 * t, this.height + 2 * t);
        }
    }


    class LimitDialog extends JFrame {

        public JTextField tfXMinI;
        public JTextField tfXMaxI;
        public JTextField tfYMinI;
        public JTextField tfYMaxI;
        public JTextField tfWidthI;
        public JTextField tfHeightI;

        public int last = zooms.size();


        public LimitDialog(DragBox DB) {

            JPanel pnAllPanel;

            JPanel pnXPanel;
            JLabel lbXLabelMin;
            JLabel lbXLabelMax;

            JPanel pnYPanel;
            JLabel lbYLabelMin;
            JLabel lbYLabelMax;

            JPanel pnSPanel;
            JLabel lbWidth;
            JLabel lbHeight;

            JButton btCancel;
            JButton btOK;
            JButton btHome;
            JButton btApply;

            this.setTitle("Set Coordinates");

            int roundX = (int) Math.max(0, 2 - Math.round((Math.log(urx - llx) / Math.log(10))));
            int roundY = (int) Math.max(0, 2 - Math.round((Math.log(ury - lly) / Math.log(10))));

            pnAllPanel = new JPanel();
//        pnAllPanel.setBorder( BorderFactory.createTitledBorder( "Set Coordinates" ) );
            GridBagLayout gbAllPanel = new GridBagLayout();
            GridBagConstraints gbcAllPanel = new GridBagConstraints();
            pnAllPanel.setLayout(gbAllPanel);

            pnXPanel = new JPanel();
            pnXPanel.setBorder(BorderFactory.createTitledBorder("x limits"));
            GridBagLayout gbXPanel = new GridBagLayout();
            GridBagConstraints gbcXPanel = new GridBagConstraints();
            pnXPanel.setLayout(gbXPanel);

            lbXLabelMin = new JLabel("x-min");
            gbcXPanel.gridx = 0;
            gbcXPanel.gridy = 0;
            gbcXPanel.gridwidth = 1;
            gbcXPanel.gridheight = 1;
            gbcXPanel.fill = GridBagConstraints.BOTH;
            gbcXPanel.weightx = 1;
            gbcXPanel.weighty = 1;
            gbcXPanel.anchor = GridBagConstraints.NORTH;
            gbXPanel.setConstraints(lbXLabelMin, gbcXPanel);
            pnXPanel.add(lbXLabelMin);

            lbXLabelMax = new JLabel("x-max");
            gbcXPanel.gridx = 0;
            gbcXPanel.gridy = 1;
            gbcXPanel.gridwidth = 1;
            gbcXPanel.gridheight = 1;
            gbcXPanel.fill = GridBagConstraints.BOTH;
            gbcXPanel.weightx = 1;
            gbcXPanel.weighty = 1;
            gbcXPanel.anchor = GridBagConstraints.NORTH;
            gbXPanel.setConstraints(lbXLabelMax, gbcXPanel);
            pnXPanel.add(lbXLabelMax);

            tfXMinI = new JTextField(10);
            tfXMinI.setText(Stat.roundToString(llx, roundX));
            tfXMinI.selectAll();
            tfXMinI.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_PERIOD) || (c == KeyEvent.VK_MINUS) || (c == KeyEvent.VK_E)))) {
                        getToolkit().beep();
                        e.consume();
                    }
                }
            });
            tfXMinI.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    JTextField textField = (JTextField) e.getSource();
                    textField.selectAll();
                }
            });
            gbcXPanel.gridx = 1;
            gbcXPanel.gridy = 0;
            gbcXPanel.gridwidth = 1;
            gbcXPanel.gridheight = 1;
            gbcXPanel.fill = GridBagConstraints.BOTH;
            gbcXPanel.weightx = 1;
            gbcXPanel.weighty = 0;
            gbcXPanel.anchor = GridBagConstraints.NORTH;
            gbXPanel.setConstraints(tfXMinI, gbcXPanel);
            pnXPanel.add(tfXMinI);

            tfXMaxI = new JTextField(10);
            tfXMaxI.setText(Stat.roundToString(urx, roundX));
            tfXMaxI.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_PERIOD) || (c == KeyEvent.VK_MINUS) || (c == KeyEvent.VK_E)))) {
                        getToolkit().beep();
                        e.consume();
                    }
                }
            });
            tfXMaxI.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    JTextField textField = (JTextField) e.getSource();
                    textField.selectAll();
                }
            });
            gbcXPanel.gridx = 1;
            gbcXPanel.gridy = 1;
            gbcXPanel.gridwidth = 1;
            gbcXPanel.gridheight = 1;
            gbcXPanel.fill = GridBagConstraints.BOTH;
            gbcXPanel.weightx = 1;
            gbcXPanel.weighty = 0;
            gbcXPanel.anchor = GridBagConstraints.NORTH;
            gbXPanel.setConstraints(tfXMaxI, gbcXPanel);
            pnXPanel.add(tfXMaxI);

            if (DB instanceof PC) {
                tfXMinI.setEnabled(false);
                tfXMaxI.setEnabled(false);
            }

            gbcAllPanel.gridx = 0;
            gbcAllPanel.gridy = 0;
            gbcAllPanel.gridwidth = 2;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(pnXPanel, gbcAllPanel);
            pnAllPanel.add(pnXPanel);

            pnYPanel = new JPanel();
            pnYPanel.setBorder(BorderFactory.createTitledBorder("y limits"));
            GridBagLayout gbYPanel = new GridBagLayout();
            GridBagConstraints gbcYPanel = new GridBagConstraints();
            pnYPanel.setLayout(gbYPanel);
            gbcAllPanel.gridx = 3;
            gbcAllPanel.gridy = 0;
            gbcAllPanel.gridwidth = 2;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(pnYPanel, gbcAllPanel);
            pnAllPanel.add(pnYPanel);

            lbYLabelMin = new JLabel("y-min");
            gbcYPanel.gridx = 0;
            gbcYPanel.gridy = 0;
            gbcYPanel.gridwidth = 1;
            gbcYPanel.gridheight = 1;
            gbcYPanel.fill = GridBagConstraints.BOTH;
            gbcYPanel.weightx = 1;
            gbcYPanel.weighty = 1;
            gbcYPanel.anchor = GridBagConstraints.NORTH;
            gbYPanel.setConstraints(lbYLabelMin, gbcYPanel);
            pnYPanel.add(lbYLabelMin);

            lbYLabelMax = new JLabel("y-max");
            gbcYPanel.gridx = 0;
            gbcYPanel.gridy = 1;
            gbcYPanel.gridwidth = 1;
            gbcYPanel.gridheight = 1;
            gbcYPanel.fill = GridBagConstraints.BOTH;
            gbcYPanel.weightx = 1;
            gbcYPanel.weighty = 1;
            gbcYPanel.anchor = GridBagConstraints.NORTH;
            gbYPanel.setConstraints(lbYLabelMax, gbcYPanel);
            pnYPanel.add(lbYLabelMax);

            tfYMinI = new JTextField(10);
            tfYMinI.setText(Stat.roundToString(lly, roundY));
            tfYMinI.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_PERIOD) || (c == KeyEvent.VK_MINUS) || (c == KeyEvent.VK_E)))) {
                        getToolkit().beep();
                        e.consume();
                    }
                }
            });
            tfYMinI.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    JTextField textField = (JTextField) e.getSource();
                    textField.selectAll();
                }
            });
            gbcYPanel.gridx = 1;
            gbcYPanel.gridy = 0;
            gbcYPanel.gridwidth = 1;
            gbcYPanel.gridheight = 1;
            gbcYPanel.fill = GridBagConstraints.BOTH;
            gbcYPanel.weightx = 1;
            gbcYPanel.weighty = 0;
            gbcYPanel.anchor = GridBagConstraints.NORTH;
            gbYPanel.setConstraints(tfYMinI, gbcYPanel);
            pnYPanel.add(tfYMinI);

            tfYMaxI = new JTextField(10);
            tfYMaxI.setText(Stat.roundToString(ury, roundY));
            tfYMaxI.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_PERIOD) || (c == KeyEvent.VK_MINUS) || (c == KeyEvent.VK_E)))) {
                        getToolkit().beep();
                        e.consume();
                    }
                }
            });
            tfYMaxI.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    JTextField textField = (JTextField) e.getSource();
                    textField.selectAll();
                }
            });
            gbcYPanel.gridx = 1;
            gbcYPanel.gridy = 1;
            gbcYPanel.gridwidth = 1;
            gbcYPanel.gridheight = 1;
            gbcYPanel.fill = GridBagConstraints.BOTH;
            gbcYPanel.weightx = 1;
            gbcYPanel.weighty = 0;
            gbcYPanel.anchor = GridBagConstraints.NORTH;
            gbYPanel.setConstraints(tfYMaxI, gbcYPanel);
            pnYPanel.add(tfYMaxI);

            if (DB instanceof Barchart) {
                tfXMinI.setEnabled(false);
                tfYMinI.setEnabled(false);
                tfYMaxI.setEnabled(false);
            }


            pnSPanel = new JPanel();
            pnSPanel.setBorder(BorderFactory.createTitledBorder("window size"));
            GridBagLayout gbSPanel = new GridBagLayout();
            GridBagConstraints gbcSPanel = new GridBagConstraints();
            pnSPanel.setLayout(gbSPanel);
            gbcAllPanel.gridx = 0;
            gbcAllPanel.gridy = 1;
            gbcAllPanel.gridwidth = 5;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(pnSPanel, gbcAllPanel);
            pnAllPanel.add(pnSPanel);

            lbWidth = new JLabel("width");
            gbcSPanel.gridx = 0;
            gbcSPanel.gridy = 0;
            gbcSPanel.gridwidth = 1;
            gbcSPanel.gridheight = 1;
            gbcSPanel.fill = GridBagConstraints.BOTH;
            gbcSPanel.weightx = 1;
            gbcSPanel.weighty = 1;
            gbcSPanel.anchor = GridBagConstraints.NORTH;
            gbSPanel.setConstraints(lbWidth, gbcSPanel);
            pnSPanel.add(lbWidth);

            lbHeight = new JLabel("  height");
            gbcSPanel.gridx = 2;
            gbcSPanel.gridy = 0;
            gbcSPanel.gridwidth = 1;
            gbcSPanel.gridheight = 1;
            gbcSPanel.fill = GridBagConstraints.BOTH;
            gbcSPanel.weightx = 1;
            gbcSPanel.weighty = 1;
            gbcSPanel.anchor = GridBagConstraints.NORTH;
            gbSPanel.setConstraints(lbHeight, gbcSPanel);
            pnSPanel.add(lbHeight);

            tfWidthI = new JTextField(10);
            tfWidthI.setText(frame.getWidth() + "");
            tfWidthI.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)))) {
                        getToolkit().beep();
                        e.consume();
                    }
                }
            });
            tfWidthI.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    JTextField textField = (JTextField) e.getSource();
                    textField.selectAll();
                }
            });
            gbcSPanel.gridx = 1;
            gbcSPanel.gridy = 0;
            gbcSPanel.gridwidth = 1;
            gbcSPanel.gridheight = 1;
            gbcSPanel.fill = GridBagConstraints.BOTH;
            gbcSPanel.weightx = 1;
            gbcSPanel.weighty = 0;
            gbcSPanel.anchor = GridBagConstraints.NORTH;
            gbSPanel.setConstraints(tfWidthI, gbcSPanel);
            pnSPanel.add(tfWidthI);

            tfHeightI = new JTextField(10);
            tfHeightI.setText(frame.getHeight() + "");
            tfHeightI.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();
                    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)))) {
                        getToolkit().beep();
                        e.consume();
                    }
                }
            });
            tfHeightI.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    JTextField textField = (JTextField) e.getSource();
                    textField.selectAll();
                }
            });
            gbcSPanel.gridx = 3;
            gbcSPanel.gridy = 0;
            gbcSPanel.gridwidth = 1;
            gbcSPanel.gridheight = 1;
            gbcSPanel.fill = GridBagConstraints.BOTH;
            gbcSPanel.weightx = 1;
            gbcSPanel.weighty = 0;
            gbcSPanel.anchor = GridBagConstraints.NORTH;
            gbSPanel.setConstraints(tfHeightI, gbcSPanel);
            pnSPanel.add(tfHeightI);
            tfHeightI.setNextFocusableComponent(tfXMinI);


            btCancel = new JButton("Cancel");
            btCancel.setActionCommand("Cancel");
            btCancel.addActionListener(DB);
            gbcAllPanel.gridx = 0;
            gbcAllPanel.gridy = 2;
            gbcAllPanel.gridwidth = 1;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(btCancel, gbcAllPanel);
            pnAllPanel.add(btCancel);

            btOK = new JButton("OK");
            btOK.setActionCommand("OK");
            btOK.addActionListener(DB);
            gbcAllPanel.gridx = 4;
            gbcAllPanel.gridy = 2;
            gbcAllPanel.gridwidth = 1;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(btOK, gbcAllPanel);
            pnAllPanel.add(btOK);
            this.getRootPane().setDefaultButton(btOK);

            btHome = new JButton("Home");
            btHome.setActionCommand("Home");
            btHome.addActionListener(DB);
            gbcAllPanel.gridx = 1;
            gbcAllPanel.gridy = 2;
            gbcAllPanel.gridwidth = 1;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(btHome, gbcAllPanel);
            pnAllPanel.add(btHome);

            btApply = new JButton("Apply");
            btApply.setActionCommand("Apply");
            btApply.addActionListener(DB);
            gbcAllPanel.gridx = 3;
            gbcAllPanel.gridy = 2;
            gbcAllPanel.gridwidth = 1;
            gbcAllPanel.gridheight = 1;
            gbcAllPanel.fill = GridBagConstraints.BOTH;
            gbcAllPanel.weightx = 1;
            gbcAllPanel.weighty = 0;
            gbcAllPanel.anchor = GridBagConstraints.NORTH;
            gbAllPanel.setConstraints(btApply, gbcAllPanel);
            pnAllPanel.add(btApply);

            JScrollPane scpAllPanel = new JScrollPane(pnAllPanel);
            setContentPane(scpAllPanel);
//        setSize(360, 155);
            setSize(360, 255);
            setResizable(false);
            pack();

            // Center the baby ...
            int x, y;

            Point topLeft = frame.getLocationOnScreen();
            Dimension parentSize = frame.getSize();

            Dimension mySize = this.getSize();

            x = ((parentSize.width - mySize.width) / 2) + topLeft.x;

            if (parentSize.height > mySize.height)
                y = ((parentSize.height - mySize.height) / 2) + topLeft.y;
            else
                y = topLeft.y;

            setLocation(x, y);

            setVisible(true);

        }
    }
}

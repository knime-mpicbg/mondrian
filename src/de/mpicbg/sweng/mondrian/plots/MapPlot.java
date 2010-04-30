package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.Selection;
import de.mpicbg.sweng.mondrian.plots.basic.MyPoly;
import de.mpicbg.sweng.mondrian.util.StatUtil;
import de.mpicbg.sweng.mondrian.util.Util;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;


public class MapPlot extends DragBox {

    private Vector polys = new Vector(256, 256);  // Store the tiles.
    protected int oldWidth, oldHeight;           // The last size for constructing the polygons.
    private int hiliteId = 0;
    public double ratio;
    private DataSet data;
    private int borderAlpha = 5;
    private int[] alphas = {0, 10, 20, 40, 70, 100};

    private JPanel p;
    private JComboBox Varlist, Collist, ColMap;
    private JList allVarList;
    private JTextField minField, maxField;
    private int displayVar = -1;
    private boolean inverted = false;
    private boolean alphaChanged = false;
    private boolean colorChanged = false;
    private int[] match;
    private Color[] terrain;
    private Color[] heat;
    private Color[] topo;
    private Vector smallPolys = new Vector(256, 256);
    private Image bi, tbi;
    private Graphics bg;
    private int queryId = -1;
    private String scheme = "gray";
    private String colorMapping = "linear";

    private Vector NPAPolys = new Vector(256, 256);
    private Vector finalPolys = new Vector(256, 256);


    /**
     * This constructor requires a Frame and a desired size
     */
    public MapPlot(MFrame frame, int width, int height, DataSet data, Vector polys, JList varList) {
        super(frame);
        this.polys = polys;
        this.data = data;
        int width1 = width;
        int height1 = height;

        border = 20;
        if (varList.getSelectedIndices().length > 0)
            this.displayVar = varList.getSelectedIndices()[0];
        else
            this.displayVar = -1;
        allVarList = varList;

        frame.setTitle("Map(" + data.setName + ")");

        int xMin = ((MyPoly) polys.elementAt(0)).xpoints[0];
        int xMax = ((MyPoly) polys.elementAt(0)).xpoints[0];
        int yMin = ((MyPoly) polys.elementAt(0)).ypoints[0];
        int yMax = ((MyPoly) polys.elementAt(0)).ypoints[0];

        for (int i = 0; i < polys.size(); i++) {
            MyPoly p = (MyPoly) polys.elementAt(i);
            Rectangle box = p.getBounds();
            xMin = Math.min(box.x, xMin);
            xMax = Math.max(box.x + box.width, xMax);
            yMin = Math.min(box.y, yMin);
            yMax = Math.max(box.y + box.height, yMax);
            //System.out.println("Set Corrdinates: "+box.x+", "+box.width+", "+box.y+", "+box.height+" No: "+i+"");
        }
        setCoordinates(xMin, yMin, xMax, yMax, 1);

        ratio = (double) (xMax - xMin) / (double) (yMax - yMin);

        p = new JPanel(new FlowLayout());

        Varlist = new JComboBox();
        Varlist.addItem("- none -");
        for (int j = 0; j < data.k; j++) {
            Varlist.addItem(data.getName(j));
        }
        Varlist.setSelectedIndex(this.displayVar + 1);
        Varlist.setSize(200, (Varlist.getSize()).height);
        Varlist.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateMap();
            }
        });
        p.add(Varlist);

        if (frame.hasR()) {
            try {
                RConnection c = new RConnection();

                double[] reds = c.eval("col2rgb(terrain.colors(" + (polys.size()) + "))[1,]").asDoubles();
                double[] greens = c.eval("col2rgb(terrain.colors(" + (polys.size()) + "))[2,]").asDoubles();
                double[] blues = c.eval("col2rgb(terrain.colors(" + (polys.size()) + "))[3,]").asDoubles();

                terrain = new Color[polys.size()];
                for (int i = 0; i < polys.size(); i++)
                    terrain[i] = new Color((float) (reds[i] / 255), (float) (greens[i] / 255), (float) (blues[i] / 255));

                reds = c.eval("col2rgb(heat.colors(" + (polys.size()) + "))[1,]").asDoubles();
                greens = c.eval("col2rgb(heat.colors(" + (polys.size()) + "))[2,]").asDoubles();
                blues = c.eval("col2rgb(heat.colors(" + (polys.size()) + "))[3,]").asDoubles();

                heat = new Color[polys.size()];
                for (int i = 0; i < polys.size(); i++)
                    heat[i] = new Color((float) (reds[i] / 255), (float) (greens[i] / 255), (float) (blues[i] / 255));

                reds = c.eval("col2rgb(topo.colors(" + (polys.size()) + "))[1,]").asDoubles();
                greens = c.eval("col2rgb(topo.colors(" + (polys.size()) + "))[2,]").asDoubles();
                blues = c.eval("col2rgb(topo.colors(" + (polys.size()) + "))[3,]").asDoubles();

                topo = new Color[polys.size()];
                for (int i = 0; i < polys.size(); i++)
                    topo[i] = new Color((float) (reds[i] / 255), (float) (greens[i] / 255), (float) (blues[i] / 255));

                c.close();
            } catch (RserveException rse) {
                System.out.println("Rserve exception: " + rse.getMessage());
            }
            catch (REXPMismatchException mme) {
                System.out.println("Mismatch exception : " + mme.getMessage());
            }
        }

        Collist = new JComboBox();
        Collist.addItem("gray");
        Collist.addItem("red");
        Collist.addItem("green");
        Collist.addItem("blue");
        Collist.addItem("blue2red");
        Collist.addItem("blueWred");
        if (frame.hasR()) {
            Collist.addItem("heat");
            Collist.addItem("terrain");
            Collist.addItem("topo");
        }
        Collist.setSize(200, (Varlist.getSize()).height);
        p.add(Collist);
        Collist.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateMap();
            }
        });

/*    JCheckBox cbBorder = new JCheckBox("Outline", true);
    cbBorder.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) { drawBorder = !drawBorder; updateMap(); }
    });
    p.add("East", cbBorder); */

/*    JCheckBox cbRank = new JCheckBox("Rank", rank);
    cbRank.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) { rank = !rank; updateMap(); }
    });
    p.add("East", cbRank);*/

        ColMap = new JComboBox();
        ColMap.addItem("linear");
        ColMap.addItem("normal");
        ColMap.addItem("rank");
//    Collist.setSize(200, (Varlist.getSize()).height);
        p.add(ColMap);
        ColMap.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateMap();
            }
        });

        JCheckBox cbInvert = new JCheckBox("Invert", inverted);
        cbInvert.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                inverted = !inverted;
                updateMap();
            }
        });
        p.add(cbInvert);

        p.add(new JLabel(" Min:"));

        minField = new JTextField(5);
        minField.addActionListener(this);
        minField.setActionCommand("text");
        p.add(minField);

        p.add(new JLabel(" Max:"));

        maxField = new JTextField(5);
        maxField.addActionListener(this);
        maxField.setActionCommand("text");
        p.add(maxField);

        p.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));

        frame.getContentPane().setLayout(new BorderLayout());

        frame.getContentPane().add(p, BorderLayout.NORTH);

        frame.getContentPane().add(this, BorderLayout.CENTER);

        frame.pack();

        System.out.println(p.getMinimumSize() + " <-> " + p.getPreferredSize() + " <-> " + p.getSize() + " ... " + maxField.getY() + " " + maxField.getHeight());

        p.setPreferredSize(new Dimension(frame.getWidth(), 2 + maxField.getY() + maxField.getHeight()));

        frame.pack();
        frame.setVisible(true);

//    if( ((System.getProperty("os.name")).toLowerCase()).indexOf("win") > -1 ) {
        // Since Java 1.4+ Widgets eat up their events, we need to register every single focussable object on the Panel ...
        Varlist.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });
        Collist.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });
        ColMap.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });
//      cbBorder.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) {processKeyEvent(e);}});
        cbInvert.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });
//      cbRank.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) {processKeyEvent(e);}});
//    }
        maxField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });
        minField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                processKeyEvent(e);
            }
        });

        match = new int[polys.size()];
        boolean[] recMatch = new boolean[data.n];
        int pid = 0;
        while (!data.isPolyID(pid))
            pid++;
        double[] ids = data.getRawNumbers(pid);
        for (int i = 0; i < polys.size(); i++) {
            MyPoly P = (MyPoly) polys.elementAt(i);
            int j = 0;
            while (j < ids.length && (int) ids[j] != P.Id) {
                if ((int) ids[j] != P.Id)
                    j++;
            }
            if (j < ids.length) {
                match[i] = j;
                recMatch[j] = true;
            } else {
                P.Id = -1;
                match[i] = -1;
                System.out.println("Polygon " + P.Id + " not matched by any Record!");
            }
        }
        for (int i = 0; i < data.n; i++)
            if (!recMatch[i])
                System.out.println("Record " + i + " not matched by any Polygon!");

        //dump();
        //dumpNPAs();
        // the events we are interested in.
        this.enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
        this.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
        this.enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        this.enableEvents(AWTEvent.KEY_EVENT_MASK);
        this.requestFocus();
    }


    public void maintainSelection(Selection S) {

        Rectangle sr = S.r;
        int mode = S.mode;

        S.o = new floatRect(worldToUserX(S.r.x),
                worldToUserY(S.r.y),
                worldToUserX(S.r.x + S.r.width),
                worldToUserY(S.r.y + S.r.height));

        boolean mapSelect[] = new boolean[data.n];

        for (int i = 0; i < polys.size(); i++) {
            MyPoly p = (MyPoly) smallPolys.elementAt(i);
            if (p.intersects(sr) && p.Id != -1) { // -1 = dummy polygon, which has no data attached
                //System.out.println("Intersect: "+i);
                mapSelect[match[i]] = true;
                data.setSelection(match[i], 1, mode);
            } else if (match[i] > -1 && !mapSelect[match[i]])
                data.setSelection(match[i], 0, mode);
            //System.out.println("Multiple: "+i);
        }
    }


    public void updateSelection() {
        paint(this.getGraphics());
    }


    public void updateMap() {
        displayVar = Varlist.getSelectedIndex() - 1;
        scheme = (String) Collist.getSelectedItem();
        colorMapping = (String) ColMap.getSelectedItem();
        scaleChanged = true;
        paint(this.getGraphics());
    }


    public void dataChanged(int var) {
        if (var == displayVar || var == -1) {
            if (var == -1)
                create();
            paint(this.getGraphics());
        }
    }


    public void processKeyEvent(KeyEvent e) {

/*    if (e.getID() == KeyEvent.KEY_RELEASED && e.isControlDown() && false ) {
      this.setToolTipText("");
      ToolTipManager.sharedInstance().setEnabled(false);
    }
*/
        if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_F) {
            Graphics g = this.getGraphics();
            double[] selection = data.getSelection();
            for (int i = 0; i < polys.size(); i++) {
                MyPoly p = (MyPoly) smallPolys.elementAt(i);
                if (match[i] > -1 && selection[match[i]] > 0) {
                    Rectangle r = p.getBounds();
                    if (r.width < 20) {
                        r.x -= (20 - r.width) / 2;
                        r.width = 20;
                    }
                    if (r.height < 20) {
                        r.y -= (20 - r.height) / 2;
                        r.height = 20;
                    }
                    g.setColor(new Color(1.0F, 0.0F, 0.0F, 0.5F));
                    g.fillOval(r.x, r.y, r.width, r.height);
                }
            }
            long start = new Date().getTime();
            while (new Date().getTime() < start + 1000) {
            }
            g.drawImage(tbi, 0, 0, Color.black, null);
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                if (borderAlpha > 0)
                    borderAlpha--;
                else
                    return;
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                if (borderAlpha < 5)
                    borderAlpha++;
                else
                    return;
            }
            for (int i = 0; i < polys.size(); i++)
                ((MyPoly) smallPolys.elementAt(i)).setBorderColor(new Color(0, 0, 0, (int) (2.55 * alphas[borderAlpha])));
            alphaChanged = true;
            paint(this.getGraphics());
        }
        super.processKeyEvent(e);  // Pass other event types on.
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
                for (int i = 0; i < polys.size(); i++) {
                    MyPoly p = (MyPoly) smallPolys.elementAt(i);
                    if (p.contains(e.getX(), e.getY())) {
                        int[] selectedIds = allVarList.getSelectedIndices();
                        if (selectedIds.length == 0) {
                            for (int j = 0; j < data.k; j++)
                                if (data.getName(j).toLowerCase().indexOf("name") >= 0) {
                                    selectedIds = new int[1];
                                    selectedIds[0] = j;
                                }
                            //         selectedIds = new int[] { 0 };
                        }
                        JPopupMenu infoPop = new JPopupMenu();
                        for (int sel = 0; sel < selectedIds.length; sel++) {
                            JMenuItem x;
                            if (data.categorical(selectedIds[sel]))
                                if (data.alpha(selectedIds[sel]))
                                    x = new JMenuItem(data.getName(selectedIds[sel]) + ": "
                                            + data.getLevelName(selectedIds[sel], (data.getNumbers(selectedIds[sel]))[match[i]]));
                                else
                                    x = new JMenuItem(data.getName(selectedIds[sel]) + ": "
                                            + data.getLevelName(selectedIds[sel], (data.getRawNumbers(selectedIds[sel]))[match[i]]));
                            else
                                x = new JMenuItem(data.getName(selectedIds[sel]) + ": "
                                        + (data.getRawNumbers(selectedIds[sel]))[match[i]]);
                            infoPop.add(x);
                        }
                        infoPop.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            } else
                super.processMouseEvent(e);  // Pass other event types on.
        } else
            super.processMouseEvent(e);  // Pass other event types on.
    }


    public String getToolTipText(MouseEvent e) {

        if (e.isControlDown()) {

            for (int i = 0; i < smallPolys.size(); i++) {
                MyPoly p = (MyPoly) smallPolys.elementAt(i);
                if (p.contains(e.getX(), e.getY() + sb.getValue())) {
                    if (e.isShiftDown() && (allVarList.getSelectedIndices()).length != 0) {
                        int[] selectedIds = allVarList.getSelectedIndices();
                        String infoTxt = "<TR'><TD align=right><font size=4 face='courier'>";
                        String para = "";
                        String sep = ": <TD  align='left'> <font size=4 face='courier'>";
                        for (int sel = 0; sel < selectedIds.length; sel++) {
                            String val = "NA";
                            if (sel > 0)
                                para = " <TR height=5><TD align=right><font size=4 face='courier'>";
                            if (!(data.getMissings(selectedIds[sel]))[match[i]]) {
                                if (data.categorical(selectedIds[sel])) {
                                    if (data.alpha(selectedIds[sel]))
                                        val = data.getLevelName(selectedIds[sel], (data.getNumbers(selectedIds[sel]))[match[i]]);
                                    else
                                        val = data.getLevelName(selectedIds[sel], (data.getRawNumbers(selectedIds[sel]))[match[i]]);
                                } else if (match[i] > -1)
                                    val = "" + (data.getRawNumbers(selectedIds[sel]))[match[i]];
                                else
                                    return null;
                            }
                            infoTxt = infoTxt + para + data.getName(selectedIds[sel]) + sep + val;
                        }
                        // Tooltip Stuff
                        if (!p.getLabel().equals(""))
                            return "<HTML><TABLE border='0' cellpadding='0' cellspacing='0'><TR align='center' colspan=2><font size=4 face='courier'> " + p.getLabel() + " " + infoTxt + " </TABLE></html>";
                        else
                            return "<HTML><TABLE border='0' cellpadding='0' cellspacing='0'>" + infoTxt + " </TABLE></html>";
                    } else {
                        return "<html><font size=4 face='courier'> " + p.getLabel() + " </html>";
                    }
                }      // end IF contains
            }        // end FOR
            return null;
        } else
            return null;
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("text")) {
            colorChanged = true;
            paint(this.getGraphics());
        } else
            super.actionPerformed(e);
    }


    public void processMouseMotionEvent(MouseEvent e) {

        super.processMouseMotionEvent(e);  // Pass other event types on.
    }


    public void paint(Graphics2D g) {
        Dimension size = this.getSize();

        if (printing) {
            bg = g;
        }
        if (oldWidth != size.width || oldHeight != size.height || scaleChanged || colorChanged || frame.getBackground() != MFrame.backgroundColor) {
            frame.setBackground(MFrame.backgroundColor);
            p.setPreferredSize(new Dimension(frame.getWidth(), 2 + maxField.getY() + maxField.getHeight()));
            colorChanged = false;
            create();
            oldWidth = size.width;
            oldHeight = size.height;
            scaleChanged = false;
        }

/*      // Fix selection state of the polygons
      double[] selection = data.getSelection();
      for( int i=0; i<polys.size(); i++) {
        if( match[i]> -1 ) 
          ((MyPoly)smallPolys.elementAt(i)).setHilite( selection[match[i]] );
      }*/

        if (bg == null || printing || alphaChanged) {
            if (!printing) {
                bi = createImage(size.width, size.height);    // double buffering from CORE JAVA p212
                tbi = createImage(size.width, size.height);
                bg = bi.getGraphics();
                System.out.println("Creating Base Image");
            } else
                bg = g;
            alphaChanged = false;
            for (int i = 0; i < polys.size(); i++) {
                MyPoly p = (MyPoly) smallPolys.elementAt(i);
                p.setHilite(0);
                p.draw(bg);
            }
        }

        Graphics tbg;
        if (!printing)
            tbg = tbi.getGraphics();
        else
            tbg = g;
        if (!printing)
            tbg.drawImage(bi, 0, 0, null);
//      tbg.setColor(Color.green);
//      long start = new Date().getTime();
        double[] selection = data.getSelection();
        for (int i = 0; i < polys.size(); i++) {
            MyPoly p = (MyPoly) smallPolys.elementAt(i);
            if (match[i] > -1) {
                p.setHilite(selection[match[i]]);
                if (selection[match[i]] > 0)
                    p.draw(tbg);
            }
        }
        //    dumpNPAs(tbg);
        if (!printing) {
            tbg.setColor(Color.black);
            drawSelections(tbg);
            g.drawImage(tbi, 0, 0, Color.black, null);
//        tbg.dispose();
        }
//      long stop = new Date().getTime();
        //System.out.println("Time for polys: "+(stop-start)+"ms");
        //dump();
    }


    public void drawSelections(Graphics g) {
        for (int i = 0; i < Selections.size(); i++) {
            Selection S = (Selection) Selections.elementAt(i);
            drawBoldDragBox(g, S);
        }
    }


    public void create() {

        // System.out.println("size is: "+this.getSize());

        if (bg != null) {
            bg.dispose();
            bg = null;
        }
        smallPolys.removeAllElements();

        updateScale();
        int shiftx = (int) this.getLlx();
        int shifty = (int) (this.getLly() + (getUry() - getLly()));
        double scalex = (this.userToWorldX(this.getUrx()) - this.userToWorldX(this.getLlx())) / (this.getUrx() - this.getLlx());
        double scaley = (this.userToWorldY(this.getUry()) - this.userToWorldY(this.getLly())) / (this.getUry() - this.getLly());
        //System.out.println("Range User:"+(this.getUry() - this.getLly())+" Range World:"+(this.userToWorldY( this.getUry() ) - this.userToWorldY( this.getLly() )));
        double[] shade = {1.0};
        int[] shadeI = {1};
        double min = 0;
        double max = 0;
        if (displayVar >= 0) {
            if (colorMapping.equals("linear") || data.categorical(displayVar)) {
                shade = data.getRawNumbers(displayVar);
                if (((minField.getText()).trim()).equals(""))
                    min = data.getMin(displayVar);
                else
                    min = Util.atod(minField.getText());
                if (((maxField.getText()).trim()).equals(""))
                    max = data.getMax(displayVar);
                else
                    max = Util.atod(maxField.getText());
            } else {
                shadeI = data.getRank(displayVar);
                min = 0;
                max = data.n - 1;
            }
        }

        boolean[] miss = {true};
        if (displayVar >= 0)
            miss = data.getMissings(displayVar);

        for (int i = 0; i < polys.size(); i++) {
            MyPoly P = (MyPoly) polys.elementAt(i);
            MyPoly p = (MyPoly) P.clone();
            p.transform(shiftx, shifty, scalex, scaley);
            p.translate(border, border);
            if (displayVar < 0)
                if (p.Id > -1) {
                    if (data.colorBrush)
                        p.setColor(data.getColor(match[i]));
                    else
                        p.setColor(MFrame.objectColor);
                } else
                    p.setColor(MFrame.backgroundColor);
            else {
                float intensity = 0;
                if (match[i] > -1) {
                    if (colorMapping.equals("linear") || data.categorical(displayVar)) {
                        double value = Math.max(shade[match[i]], min);
                        value = Math.min(value, max);
                        if (inverted)
                            intensity = (float) (1 - (value - min) / (max - min));
                        else
                            intensity = (float) (1 - (value - max) / (min - max));
                    } else if (inverted)
                        intensity = (float) (1 - (shadeI[match[i]] - min) / (max - min));
                    else
                        intensity = (float) (1 - (shadeI[match[i]] - max) / (min - max));
                    if (colorMapping.equals("normal")) {
                        if (intensity == 0)
                            intensity = -3;
                        else if (intensity == 1)
                            intensity = 3;
                        else
                            intensity = (float) ((StatUtil.qnorm(intensity) + 3) / 6.0);
                        intensity = Math.max(0, intensity);
                        intensity = Math.min(1, intensity);
                    }

                }
//System.out.println("Intensity: "+intensity);
                if (p.Id == -1)
                    p.setColor(MFrame.backgroundColor);
                else if (miss[match[i]])
                    p.setColor(Color.white);
                else if (scheme.equals("gray"))
                    p.setColor(new Color(1 - intensity, 1 - intensity, 1 - intensity));                // gray
                else if (scheme.equals("blue2red"))
                    p.setColor(new Color(intensity, 0, 1 - intensity));                            // blue2red
                else if (scheme.equals("blueWred"))
                    if (intensity < 0.5)
                        p.setColor(new Color(2 * intensity, 2 * intensity, 1));                        // blueWred
                    else
                        p.setColor(new Color(1, (float) (1 - (intensity - 0.5) * 2), (float) (1 - (intensity - 0.5) * 2)));        // blueWred
                else if (scheme.equals("heat"))
                    p.setColor(heat[(int) ((polys.size() - 1) * intensity)]);                                                             // heat
                else if (scheme.equals("terrain"))
                    p.setColor(terrain[(int) ((polys.size() - 1) * intensity)]);                                                             // terrain
                else if (scheme.equals("topo"))
                    p.setColor(topo[(int) ((polys.size() - 1) * intensity)]);                                                             // topo
                else if (scheme.equals("red"))
                    p.setColor(new Color((float) (1 - Math.pow(intensity, 4) / 2), 1 - intensity, (float) (Math.pow(1 - intensity, 3) / 1.5 + 0.15)));  // red
                else if (scheme.equals("blue"))
                    p.setColor(new Color((float) (Math.pow(1 - intensity, 3) / 1.5 + 0.15), 1 - intensity, (float) (1 - Math.pow(intensity, 4) / 2)));  // blue
                else if (scheme.equals("green"))
                    p.setColor(new Color(1 - intensity, (float) (1 - Math.pow(intensity, 4) / 2), (float) (Math.pow(1 - intensity, 3) / 1.5 + 0.15)));  // green
            }
            p.setBorderColor(new Color(0, 0, 0, (int) (2.55 * alphas[borderAlpha])));
            smallPolys.addElement(p);
            for (int j = 0; j < data.k; j++)
                if (match[i] > -1 && data.getName(j).toLowerCase().indexOf("name") >= 0) {
                    p.setLabel(data.getLevelName(j, (data.getNumbers(j))[match[i]]));
                    j = data.k;
                }
        }

        for (int i = 0; i < Selections.size(); i++) {
            Selection S = (Selection) Selections.elementAt(i);
            S.r.x = (int) userToWorldX(((floatRect) S.o).x1);
            S.r.y = (int) userToWorldY(((floatRect) S.o).y1);
            S.r.width = (int) userToWorldX(((floatRect) S.o).x2) - (int) userToWorldX(((floatRect) S.o).x1);
            S.r.height = (int) userToWorldY(((floatRect) S.o).y2) - (int) userToWorldY(((floatRect) S.o).y1);
        }
    }


    public void adjustmentValueChanged(AdjustmentEvent e) {
    }


    public void scrollTo(int id) {
    }


    public void dump() {
/*      Table NPAs = this.data.breakDown("Dump",new int[] {2}, -1);
      Variable vNPA = (Variable)(data.data.elementAt(2));
      for( int j=0; j< NPAs.table.length; j++ )
        System.out.println("Level: "+NPAs.lnames[0][j]+" ("+
                           vNPA.Level(NPAs.lnames[0][j])+
                           ") -> "+NPAs.table[j]);

      double[] polyId = data.getRawNumbers(0); */
        double[] Id = data.getRawNumbers(2);
/*      double[] npa    = data.getNumbers(2);
      boolean[] drop  = new boolean[data.n];

      for( int i=0; i<data.n; i++ )
        for( int j=i+1; j<data.n; j++ )
          if( wireId[i] == wireId[j] && npa[i] != npa[j] ) {
            if( NPAs.table[(int)npa[i]] > NPAs.table[(int)npa[j]] )
              drop[i]=true;
            else
              drop[j]=true;
          }*/

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("/Users/theusm/dump"));
            String ws = "/PpolyId\twirecenter\tNPA\n";
/*              bw.write(ws, 0, ws.length());
              for( int i=0; i< data.n; i++)
                if( !drop[i] ) {
                  ws = Integer.toString((int)polyId[i])+'\t'+
                  Integer.toString((int)wireId[i])+'\t'+
                  NPAs.lnames[0][(int)npa[i]]+'\n';
                  bw.write(ws, 0, ws.length());
                }
*/

            for (int i = 0; i < polys.size(); i++) {
                MyPoly P = (MyPoly) (polys.elementAt(i));
//                    if( !drop[match[i]] ) {
                bw.write("\n", 0, 1);
                if (match[i] > -1)
                    ws = Integer.toString((int) Id[match[i]]) + "\t/PCountyFIPS" + '\t' +
                            Integer.toString(P.npoints) + '\n';
                bw.write(ws, 0, ws.length());
                for (int j = 0; j < P.npoints; j++) {
                    ws = Double.toString((double) P.xpoints[j] / 10000) + "\t" +
                            Double.toString((double) P.ypoints[j] / 10000) + '\n';
                    bw.write(ws, 0, ws.length());
                }
            }
//                  }
            bw.close();
        }
        catch (IOException ignored) {
        }
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
}

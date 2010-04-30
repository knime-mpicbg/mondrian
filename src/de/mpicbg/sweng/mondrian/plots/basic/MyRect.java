package de.mpicbg.sweng.mondrian.plots.basic;


import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.Table;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
import de.mpicbg.sweng.mondrian.util.StatUtil;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.Vector;


public class MyRect extends Rectangle implements ActionListener {

    public int x, y, w, h;
    private int plusX = 0;
    private int plusY = 1;
    private String info;
    private String mode;
    private Graphics g;
    private double alpha = 1;
    private boolean alphaSet = false;
    public boolean censored = false;
    public char dir;
    public double obs = 1;
    private double hilite = 0;
    private double exp;
    private double scale, max;
    private float p;
    private Color drawColor = Color.black;
    private JPanel panel;
    public Vector tileIds;
    private Table tablep;
    public Color rectColor = Color.lightGray;
    private boolean flip = false;                 // flip the direction of hilite and color brush
    // still needs fix for direction = 'y' (no application yet)
    private double[] Colors;


    public MyRect(boolean full, char dir, String mode,
                  int x, int y, int w, int h,
                  double obs, double exp, double scale, double p, String info, Vector tileIds, Table tablep) {
        super(x, y, w, h);
        boolean full1 = full;
        this.dir = dir;
        this.exp = exp;
        this.scale = scale;
        this.mode = mode;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.obs = obs;
        this.p = (float) p;
        this.info = info;
        this.tileIds = tileIds;
        this.tablep = tablep;
    }


    public void moveTo(int x, int y) {
        if (x != -1)
            this.x = x;
        if (y != -1)
            this.y = y;
    }


    public Rectangle getRect() {
        return (new Rectangle(x, y, w, h));
    }


    public void actionPerformed(ActionEvent e) {
        // Dummy, since we just use it for information display
    }


    public void setDirection(String d) {
        if (d.equals("x"))
            this.dir = 'x';
        else
            this.dir = 'y';
    }


    public void setAlpha(double alpha) {
        this.alpha = alpha;
        this.alphaSet = true;
    }


    public void setHilite(double hilite) {
        this.hilite = hilite;
    }


    public void setHiliteAlign(boolean flipper) {
        this.flip = flipper;
    }


    public void setMax(double max) {
        this.max = max;
    }


    public void setColor(Color color) {
        this.drawColor = color;
    }


    public double getHilite() {
        return hilite;
    }


    public double getAbsHilite() {
        return hilite * obs;
    }


    void colorBreakdown() {

        double[] Selection;
        int sels = 0;
        Selection = tablep.data.getSelection();
        Colors = new double[tablep.data.getNumColors() + 1];              // we need one more slot (the highest) for the hilite color
        for (int j = 0; j < tileIds.size(); j++) {
            int id = ((Integer) (tileIds.elementAt(j))).intValue();
            for (int l = 0; l < (tablep.Ids[id]).length; l++)
                if (Selection[tablep.Ids[id][l]] == 0) {
                    if (tablep.count == -1)
                        Colors[tablep.data.colorArray[tablep.Ids[id][l]]]++;
                    else
                        Colors[tablep.data.colorArray[tablep.Ids[id][l]]] += (tablep.data.getRawNumbers(tablep.count))[tablep.Ids[id][l]];
                } else if (tablep.count == -1)
                    sels++;
                else
                    sels += (tablep.data.getRawNumbers(tablep.count))[tablep.Ids[id][l]];
        }
        Colors[0] = sels;                              // number of selected cases
        Colors[Colors.length - 1] = (int) obs;                                        // number of cases WITHOUT any color
        for (int i = 0; i < Colors.length - 1; i++)
            Colors[Colors.length - 1] -= Colors[i];
//    for( int i=0; i<Colors.length; i++)
//      System.out.println("i: "+i+" Count: "+Colors[i]);
    }


    public void draw(Graphics g) {

        float currAlpha = ((AlphaComposite) ((Graphics2D) g).getComposite()).getAlpha();

        if (tablep != null && tablep.data.colorBrush)
            colorBreakdown();

        //System.out.println(residual);
        if (obs > 0) {
            if (dir != 'f') {
                boolean missCell = true;
                if (info.indexOf("¥") == -1 && info.indexOf(": NA\n") == -1 && !(info.length() > 2 ? (info.substring(0, 3)).equals("NA\n") : false))
                    missCell = false;
                if (!missCell)
                    g.setColor(ColorManager.objectColor);
                else
                    g.setColor(Color.white);
                if (tablep != null && tablep.data.colorBrush) {
                    if (dir == 'x') {
                        int[] ws = Utils.roundProportions(Colors, obs, Math.min(w, width));
                        int altp;
                        if (!flip)
                            altp = x;
                        else
                            altp = x + Math.min(w, width);
                        for (int i = 0; i < Colors.length; i++) {
                            if (i == Colors.length - 1)
                                if (!missCell)
                                    g.setColor(ColorManager.objectColor);
                                else
                                    g.setColor(Color.white);
                            else if (i == 0)
                                g.setColor(DragBox.hiliteColor);
                            else
                                g.setColor(tablep.data.getColorByID(i));
                            if (!flip) {
                                g.fillRect(altp, y + Math.max(0, h - height), ws[i], Math.min(h, height) + 1);
                                altp += ws[i];
                            } else {
                                g.fillRect(altp - ws[i], y + Math.max(0, h - height), ws[i], Math.min(h, height) + 1);
                                altp -= ws[i];
                            }
                        }
                    } else if (dir == 'y') {
                        int[] hs = Utils.roundProportions(Colors, obs, Math.min(h, height));
                        int altp = 0;
                        for (int i = 0; i < Colors.length; i++) {
                            if (i == Colors.length - 1)
                                if (!missCell)
                                    g.setColor(ColorManager.objectColor);
                                else
                                    g.setColor(Color.white);
                            else if (i == 0)
                                g.setColor(DragBox.hiliteColor);
                            else
                                g.setColor(tablep.data.getColorByID(i));
                            g.fillRect(x, y + Math.max(0, h - height) + Math.min(h, height) - altp - hs[i], Math.min(w, width), hs[i]);
                            altp += hs[i];
                        }
                    }
                } else {
                    if (alphaSet)
                        ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) alpha)));
                    g.fillRect(x, y + Math.max(0, h - height), Math.min(w, width), Math.min(h, height) + 1);
                }
            } else {
                g.setColor(drawColor);
                if (alphaSet)
                    ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
                g.fillRect(x, y, Math.max(1, w), h);
            }
        }
        if (mode.equals("Expected")) {
            int high = (int) (192 + 63 * (0.15 + StatUtil.pnorm((1 - p - 0.9) * 10)));
            int low = (int) (192 * (0.85 - StatUtil.pnorm((1 - p - 0.9) * 10)));
            //System.out.println(Stat.pnorm((1-p-0.9)*15));
            if (obs - exp > 0.00001)
                g.setColor(new Color(low, low, high));
            else if (obs - exp < -0.00001)
                g.setColor(new Color(high, low, low));
            else
                g.setColor(Color.lightGray);
            double resid = Math.abs((obs - exp) / Math.sqrt(exp));
//System.out.println("Cell: "+getLabel()+" resid: "+resid+" max "+max+ " scale "+scale);
            if (dir == 'x')
                g.fillRect(x, y, (int) ((double) w * resid / max), h);
            else if (dir == 'y')
                g.fillRect(x, y + h - (int) ((double) h * resid / max),
                        w, (int) ((double) h * resid / max));
        }

        if (hilite > 0 && (tablep == null || !tablep.data.colorBrush)) {   // draw hilite on none color-brushing ...
            Color c = g.getColor();
            g.setColor(DragBox.hiliteColor);
//System.out.println("w: "+w+" hilite:"+hilite+"wh: "+(int)((double)w*hilite));
            if (Math.min(w, width) > 2 && Math.min(h, height) > 2) {  // Mit Rahmen
                plusX = 1;
                plusY = 0;
            }
            if (dir == 'x') {
                int dw = (((int) ((double) w * hilite) == 0) ? 1 : (((int) ((double) w * hilite) == w - 1) && hilite < 1 && w > 2 ? w - 2 : (int) Math.min(width, ((double) w * hilite))));
                int dh = 1 + Math.min(h, height);

                g.fillRect(flip ? x + w - dw : x + plusX,
                        y + Math.max(0, h - height),
                        dw,
                        dh);
            } else if (dir == 'y')
                g.fillRect(x,
                        y + Math.max(0, h - height) + Math.min(h, height) - (((int) ((double) (h + plusY) * hilite) == 0) ? (1 - plusY) : (int) Math.min(height, ((double) h * hilite))),
                        Math.min(w, width),
                        (((int) ((double) (h + plusY) * hilite) == 0) ? 1 : (int) Math.min(height + plusY, ((double) (h + plusY) * hilite))));
            else {
                g.setColor(new Color(255, 0, 0, (int) (255 * hilite)));
                g.fillRect(x, y, w, h);
            }
            g.setColor(c);
        }

        ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currAlpha));

        if (obs == 0 || censored)
            g.setColor(Color.red);
        else
            g.setColor(ColorManager.lineColor);
        if (dir != 'f' && Math.min(w, width) > 2 && Math.min(h, height) > 2 || obs == 0 || censored)
            g.drawRect(x, y + Math.max(0, h - height), Math.min(w, width), Math.min(h, height));
    }


    public void pop(DragBox panel, int x, int y) {
        JPopupMenu popup = new JPopupMenu();

        String pinfo = getLabel().replaceAll("\t", ":");

        StringTokenizer info = new StringTokenizer(pinfo, "\n");

        while (info.hasMoreTokens()) {
            JMenuItem infoText = new JMenuItem(info.nextToken());
            popup.add(infoText);
            infoText.addActionListener(this);
            //      infoText.setEnabled(false);
        }

        popup.show(panel, x, y);
    }


    public String getLabel() {
        String pinfo = info;
        if (obs > 0)
            pinfo += "\n" + "Count\t " + obs;
        else
            pinfo += "\n" + "Empty Bin ";
        if (hilite > 0)
            pinfo += "\n" + "Hilited\t " + StatUtil.round(hilite * obs, 0) + " (" + StatUtil.round(100 * hilite, 2) + "%)";
        if (mode.equals("Expected")) {
            pinfo += "\n" + "Expected\t " + StatUtil.round(exp, 2);
            pinfo += "\n" + "Residual\t " + StatUtil.round(obs - exp, 3);
            pinfo += "\n" + "Scaled Res.\t" + StatUtil.round(Math.abs((obs - exp) / Math.sqrt(exp) * scale * 100), 1) + "%";
        }

        return pinfo;
    }
}



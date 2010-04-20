package org.rosuda.mondrian;//////////////////////////////////////////////////////////////////////
// Jlpr.java
// Printer  class to print other applets
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
//////////////////////////////////////////////////////////////////////

/**
 * Prints applets using the PSGr PostScript Graphics context
 * @author E.J. Friedman-Hill (C)1996
 * @author ejfried@ca.sandia.gov
 * @author http://herzberg.ca.sandia.gov
 */

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Object;
import java.lang.Runnable;
import java.lang.String;
import java.lang.Thread;
import java.lang.Throwable;
import java.net.URL;


public class Jlpr extends Object implements Runnable {

    String host, script, label, file;
    Thread menuThread, printThread;
    Component printThis;
    String fileName;


    /**
     * Constructor
     */

    public Jlpr(Component printThis, String fileName) {
        this.printThis = printThis;
        this.fileName = fileName;
    }


    public void run() {
        // determine what to print
        if (printThis == null)
            return;
        String psdata;
        try {
            // first get the postscript as a big string.
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000);

            // dfs
            // PSGr postscript = new PSGr(baos, getGraphics());
            // At the moment, we want our printed object to fit on
            // a postscript page, so we have to pass in the applet size
            PSGr postscript = new PSGr(baos, printThis.getGraphics()); //, printThis.size());

            // paint the applet and the components on top of it, recursively.
            paintInside(printThis, postscript);
            psdata = baos.toString();
        } catch (Throwable t) {
            System.out.println("Error during rendering: " + t);
            return;
        }

        file = fileName;

        try {
            PrintStream f = new PrintStream(new FileOutputStream(file));
            f.println(psdata);
            f.println("showpage");
            f.close();
            URL u;
            u = new URL("file://" + fileName);
            // display the postscript in a new frame
            System.out.println("Printing Complete.");
        } catch (Throwable t) {
            System.out.println("Error while saving file: " + fileName + "<<");
            System.out.println(t);
        }
    }


    public void paintInside(Component comp, PSGr g) {
        Point p = new Point(0, 0);
        p = comp.location();

        // set the origin for this component
        g.translate(p.x, -p.y);

        // draw this component
        System.out.println("Painting: " + comp.toString());
        updateComponent(comp, g);

        // now draw this component's children inside its coordinate system
        if (comp instanceof Container) {
            Component[] comps = ((Container) comp).getComponents();
            for (int i = 0; i < comps.length; i++)
                paintInside(comps[i], g);
        }

        // restore the coordinate system
        g.translate(-p.x, p.y);
    }


    public void updateComponent(Component c, PSGr g) {

//    g.setBackground(c.getBackground());
//    g.emitThis("%" + c.toString());

        // draw a few special types of Component
        Rectangle b = c.bounds();
        int halfheight = b.height / 2;

        if (c instanceof Button) {
            if (c.getFont() != null)
                g.setFont(c.getFont());
            g.setColor(Color.white);
            g.fillRoundRect(0, 0, b.width, b.height, 4, 4);
            g.setColor(Color.black);
            g.drawRoundRect(0, 0, b.width, b.height, 4, 4);
            g.drawString(((Button) c).getLabel(), 2, halfheight + 3);
        } else if (c instanceof Label) {
            if (c.getFont() != null)
                g.setFont(c.getFont());
            g.setColor(Color.black);
            if (((Label) c).getText() != null)
                g.drawString(((Label) c).getText(), 2, halfheight + 3);
        } else if (c instanceof Choice) {
            if (c.getFont() != null)
                g.setFont(c.getFont());
            g.setColor(Color.black);
            g.drawRect(0, 0, b.width + 1, b.height + 1);
            g.setColor(Color.white);
            g.fillRect(0, 0, b.width, b.height);
            g.setColor(Color.black);
            g.drawRect(0, 0, b.width, b.height);
            g.fillRect(b.width - 7, halfheight - 1, 6, 2);
            g.drawString(((Choice) c).getSelectedItem(), 2, halfheight + 3);
        } else if (c instanceof TextComponent) {
            // This one really needs work...
            g.gsave();
            g.clipRect(c.bounds().x, c.bounds().y, c.bounds().width, c.bounds().height);
            if (c.getFont() != null)
                g.setFont(c.getFont());
            g.setColor(Color.white);
            g.fillRect(0, 0, b.width, b.height);
            g.setColor(Color.black);
            g.drawRect(0, 0, b.width, b.height);
            if (((TextComponent) c).getText() != null)
                g.drawString(((TextComponent) c).getText().trim(), 2, halfheight + 3);
            g.grestore();
        } else {
            // let it draw itself

            c.update(g);
        }
    }
}







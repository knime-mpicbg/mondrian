package org.rosuda.mondrian.util;

import org.rosuda.mondrian.core.DragBox;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;


/**
 * A simple utility class that lets you very simply print an arbitrary component. Just pass the component to the
 * PrintUtilities.printComponent. The component you want to print doesn't need a print method and doesn't have to
 * implement any interface or do anything special at all.
 * <p/>
 * If you are going to be printing many times, it is marginally more efficient to first do the following: <PRE>
 * PrintUtilities printHelper = new PrintUtilities(theComponent); </PRE> then later do printHelper.print(). But this is
 * a very tiny difference, so in most cases just do the simpler PrintUtilities.printComponent(componentToBePrinted).
 * <p/>
 * 7/99 Marty Hall, http://www.apl.jhu.edu/~hall/java/ May be freely used or adapted.
 */

public class PrintUtilities implements Printable {

    private Component componentToBePrinted;


    public static void printComponent(Component c) {
        new PrintUtilities(c).print();
    }


    public PrintUtilities(Component componentToBePrinted) {
        this.componentToBePrinted = componentToBePrinted;
    }


    public void print() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(this);
        ((DragBox) componentToBePrinted).printerPage = new Dimension(10, 10);
        if (printJob.printDialog())
            try {
                printJob.print();
            } catch (PrinterException pe) {
                System.out.println("Error printing: " + pe);
            }
    }


    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) {
            return (NO_SUCH_PAGE);
        } else {
            Graphics2D g2d = (Graphics2D) g;
//      g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            ((DragBox) componentToBePrinted).printerPage = new Dimension((int) pageFormat.getWidth(), (int) pageFormat.getHeight());

            g2d.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());
            // scale to fill the page
            double dw = pageFormat.getImageableWidth();
            double dh = pageFormat.getImageableHeight();
            Dimension screenSize = new Dimension((int) pageFormat.getWidth(), (int) pageFormat.getHeight());
            double xScale = dw / screenSize.width;
            double yScale = dh / screenSize.height;
            double scale = Math.min(xScale, yScale);
            // center the chart on the page
            double tx = 0.0;
            double ty = 0.0;
            if (xScale > scale) {
                tx = 0.5 * (xScale - scale) * screenSize.width;
            } else {
                ty = 0.5 * (yScale - scale) * screenSize.height;
            }
            g2d.translate(tx, ty);
            g2d.scale(scale, scale);
            Dimension bounds = new Dimension((int) dw, (int) dh);

            disableDoubleBuffering(componentToBePrinted);
            componentToBePrinted.paint(g2d);
            enableDoubleBuffering(componentToBePrinted);

            return (PAGE_EXISTS);
        }
    }


    /**
     * The speed and quality of printing suffers dramatically if any of the containers have double buffering turned on.
     * So this turns if off globally.
     */
    public static void disableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(false);
    }


    /**
     * Re-enables double buffering globally.
     */

    public static void enableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(true);
    }
}

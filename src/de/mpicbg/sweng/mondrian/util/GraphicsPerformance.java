package de.mpicbg.sweng.mondrian.util;

import javax.swing.*;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class GraphicsPerformance {

    public static int graphicsPerf = 0;
    private static Integer performance;


    public static int testGraphicsPerformance() {

        int graphicsPerf = 0;
        Image testI = new JFrame().createImage(200, 200);        //
        Graphics2D gI = (Graphics2D) testI.getGraphics();
        gI.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.05)));
        long start = new java.util.Date().getTime();
        while (new java.util.Date().getTime() - start < 1000) {
            graphicsPerf++;
            gI.fillOval(10, 10, 3, 3);
        }
        System.out.println("Graphics Performance: " + graphicsPerf);

        return graphicsPerf;
    }


    public static int getPerformance() {
        if (performance == null) {
            performance = testGraphicsPerformance();
        }

        return performance;
    }
}

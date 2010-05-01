package de.mpicbg.sweng.mondrian.util;

import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class GraphicsPerformance {

    public static int graphicsPerf = 0;
    private static Integer performance;


    public static int testGraphicsPerformance(Component frame) {

        int graphicsPerf = 0;
        Image testI = frame.createImage(200, 200);        //
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


    public static int getPerformance(Component frame) {
        if (performance == null) {
            performance = testGraphicsPerformance(frame);
        }

        return performance;
    }
}

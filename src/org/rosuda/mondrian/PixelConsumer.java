package org.rosuda.mondrian;

import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;


/**
 * Receives image pixels for PSGr
 *
 * @author E.J. Friedman-Hill (C)1996
 * @author ejfried@ca.sandia.gov
 * @author http://herzberg.ca.sandia.gov
 */

public class PixelConsumer implements ImageConsumer {

    boolean complete = false;
    int xdim, ydim;
    int pix[][];


    PixelConsumer(Image picture) {
        int t;

        picture.getSource().startProduction(this);
        t = 1000;
        while (t > 0 && !complete) {
            try {
                Thread.sleep(100);
            } catch (Throwable ignored) {
            }
            t -= 100;
        }
    }


    public void setProperties(java.util.Hashtable param) {
    }


    public void setColorModel(ColorModel param) {
    }


    public void setHints(int param) {
    }


    public void imageComplete(int param) {
        complete = true;
    }


    public void setDimensions(int x, int y) {
        xdim = x;
        ydim = y;
        pix = new int[x][y];
    }


    public void setPixels(int x1, int y1, int w, int h,
                          ColorModel model, byte pixels[], int off, int scansize) {
        int x, y, x2, y2, sx, sy;
        // we're ignoring the ColorModel, mostly for speed reasons.
        x2 = x1 + w;
        y2 = y1 + h;
        sy = off;
        for (y = y1; y < y2; y++) {
            sx = sy;
            for (x = x1; x < x2; x++)
                pix[x][y] = pixels[sx++];
            sy += scansize;
        }
    }


    public void setPixels(int x1, int y1, int w, int h,
                          ColorModel model, int pixels[], int off, int scansize) {
        int x, y, x2, y2, sx, sy;
        // we're ignoring the ColorModel, mostly for speed reasons.
        x2 = x1 + w;
        y2 = y1 + h;
        sy = off;
        for (y = y1; y < y2; y++) {
            sx = sy;
            for (x = x1; x < x2; x++)
                pix[x][y] = pixels[sx++];
            sy += scansize;
        }
    }
}







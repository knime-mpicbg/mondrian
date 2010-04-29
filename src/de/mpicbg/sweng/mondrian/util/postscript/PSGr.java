package de.mpicbg.sweng.mondrian.util.postscript;

// PSGr.java by E.j. Friedman-Hill with minor modifications by David Binger
// Copyright (C) 1997 E.j. Friedman-Hill and David Binger
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, 
// Boston, MA  02111-1307, USA.

// Hacked to compile with JDK1.1.2 by David Binger.  
// This needs more JDK1.1-related work for more general usage.

// Version 2.1

//package gnu.GraphPanel;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.OutputStream;
import java.io.PrintWriter;


/**
 * PSGr is a Graphics subclass that images to PostScript.
 *
 * @author Ernest Friedman-Hill
 * @author ejfried@ca.sandia.gov
 * @author http://herzberg.ca.sandia.gov
 * @version 1.0
 */

class PSGr extends java.awt.Graphics {

    private final static int CLONE = 49;

    private final static int PAGEHEIGHT = 792;
    private final static int PAGEWIDTH = 612;
    protected final static int XOFFSET = 30;
    protected final static int YOFFSET = 30;

    /**
     * hexadecimal digits
     */

    private final static char[] hd = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * number of chars in a full row of pixel data
     */

    private final static int charsPerRow = 12 * 6;


    /**
     * Output stream where postscript goes
     */

    private PrintWriter os = new PrintWriter(System.out);

    /**
     * The current color
     */

    Color clr = Color.black;

    /**
     * The current font
     */

    Font font = new Font("Helvetica", Font.PLAIN, 12);

    Rectangle clippingRect = new Rectangle(0, 0, PAGEWIDTH, PAGEHEIGHT);

    private Graphics g;


    /**
     * Constructs a new PSGr Object. Unlike regular Graphics objects, PSGr contexts can be created directly.
     *
     * @param o Output stream for PostScript output
     * @see #create
     */

    public PSGr(OutputStream o, Graphics g) {
        os = new PrintWriter(o);
        this.g = g;
        emitProlog();
    }

    // This constructor added by David Binger.


    public PSGr(OutputStream o, Graphics g, Dimension s, Point origin, String[] extras) {
        os = new PrintWriter(o);
        this.g = g;
        int bbax = origin.x;
        int bbay = PAGEHEIGHT - origin.y - s.height;
        int bbbx = bbax + s.width;
        int bbby = bbay + s.height;
        prt("%!PS-Adobe-2.0 EPSF-1.2");
        prt("%%BoundingBox: " + bbax + " " + bbay + " " + bbbx + " " + bbby);
        if (extras != null)
            for (int j = 0; j < extras.length; j++)
                prt("%" + extras[j]);
        prt("% Output Generated by PSGr Java PostScript Context");
        //    translate(tx,ty);
        setFont(font);
        // cliprect(bbax,bbay,s.width,s.height);
    }


    public PSGr(OutputStream o, Graphics g, int what) {
        os = new PrintWriter(o);
        this.g = g;
        if (what != CLONE)
            emitProlog();
    }


    private PSGr(PrintWriter o, Graphics g, int what) {
        os = o;
        this.g = g;
        if (what != CLONE)
            emitProlog();
    }


    final void pr(String s) {
        os.write(s);
        os.flush();
    }


    final void prt(String s) {
        pr(s);
        os.println();
        os.flush();
    }


    public final void prt(String a, String b) {
        prt(a + " " + b);
    }


    public final void prt(String a, String b, String c) {
        prt(a + " " + b + " " + c);
    }


    public final void prt(String a, String b, String c, String d) {
        prt(a + " " + b + " " + c + " " + d);
    }


    public final void prt(double d, String s) {
        prt(d + " " + s);
    }


    final void prt(double d) {
        prt(String.valueOf(d));
    }


    final void prt(double d1, double d2, String s) {
        prt(d1 + " " + d2 + " " + s);
    }


    final void prt(double d1, double d2, double d3, String s) {
        prt(d1 + " " + d2 + " " + d3 + " " + s);
    }


    public final void prt(double d1, double d2, double d3, double d4, String s) {
        prt(d1 + " " + d2 + " " + d3 + " " + d4 + " " + s);
    }


    final void prt(double d1, double d2, double d3, double d4,
                   double d5, String s) {
        prt(d1 + " " + d2 + " " + d3 + " " + d4 + " " + d5 + " " + s);
    }


    /**
     * Creates a new PSGr Object that is a copy of the original PSGr Object.
     */
    public Graphics create() {
        PSGr psgr = new PSGr(os, g, CLONE);
        psgr.font = font;
        psgr.clippingRect = clippingRect;
        psgr.clr = clr;
        return psgr;
    }


    /**
     * Creates a new Graphics Object with the specified parameters, based on the original Graphics Object. This method
     * translates the specified parameters, x and y, to the proper origin coordinates and then clips the Graphics Object
     * to the area.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the area
     * @param height the height of the area
     * @see #translate
     */
    public Graphics create(int x, int y, int width, int height) {
        Graphics g = create();
        g.translate(x, y);
        g.clipRect(0, 0, width, height);
        return g;
    }


    /**
     * Translates the specified parameters into the origin of the graphics context. All subsequent operations on this
     * graphics context will be relative to this origin.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @see #scale
     */

    public void translate(int x, int y) {
        prt(x, y, "translate");
    }


    /**
     * Scales the graphics context. All subsequent operations on this graphics context will be affected.
     *
     * @param sx the scaled x coordinate
     * @param sy the scaled y coordinate
     * @see #translate
     */
    public void scale(float sx, float sy) {
        prt(sx, sy, "scale");
    }


    /**
     * Gets the current color.
     *
     * @see #setColor
     */
    public Color getColor() {
        return clr;
    }


    /**
     * Sets the current color to the specified color. All subsequent graphics operations will use this specified color.
     *
     * @param c the color to be set
     * @see Color
     * @see #getColor
     */

    public void setColor(Color c) {
        if (c != null) clr = c;
        double r = clr.getRed() / 255.0;
        double g = clr.getGreen() / 255.0;
        double b = clr.getBlue() / 255.0;
        prt(r, g, b, "setrgbcolor");
    }


    /**
     * Sets the default paint mode to overwrite the destination with the current color. PostScript has only paint mode.
     */
    public void setPaintMode() {
    }


    /**
     * Sets the paint mode to alternate between the current color and the new specified color. PostScript does not
     * support XOR mode.
     *
     * @param c1 the second color
     */
    public void setXORMode(Color c1) {
        System.err.println("Warning: PSGr does not support XOR mode");
    }


    /**
     * Gets the current font.
     *
     * @see #setFont
     */
    public Font getFont() {
        return font;
    }


    /**
     * Sets the font for all subsequent text-drawing operations.
     *
     * @param f the specified font
     * @see Font
     * @see #getFont
     * @see #drawString
     * @see #drawBytes
     * @see #drawChars
     */
    public void setFont(Font f) {
        if (f != null) {
            this.font = f;
            String javaName = font.getName();
            int javaStyle = font.getStyle();
            String psName;

            if (javaName.equals("Symbol"))
                psName = "Symbol";
                // The constant was "Times" in the next line,
                // but David Binger changed it to "TimesRoman".
            else if (javaName.equals("TimesRoman")) {
                psName = "Times-";
                switch (javaStyle) {
                    case Font.PLAIN:
                        psName += "Roman";
                        break;
                    case Font.BOLD:
                        psName += "Bold";
                        break;
                    case Font.ITALIC:
                        psName += "Italic";
                        break;
                    case (Font.ITALIC + Font.BOLD):
                        psName += "BoldItalic";
                        break;
                }
            } else if (javaName.equals("Helvetica") || javaName.equals("Courier")) {
                psName = javaName;
                switch (javaStyle) {
                    case Font.PLAIN:
                        break;
                    case Font.BOLD:
                        psName += "-Bold";
                        break;
                    case Font.ITALIC:
                        psName += "-Oblique";
                        break;
                    case (Font.ITALIC + Font.BOLD):
                        psName += "BoldOblique";
                        break;
                }
            } else
                psName = "Courier";
            String s = "/" + psName + " findfont ";
            s += font.getSize() + " scalefont setfont";
            prt(s);
        }
    }


    /**
     * Gets the current font metrics.
     *
     * @see #getFont
     */
    public FontMetrics getFontMetrics() {
        return getFontMetrics(getFont());
    }


    /**
     * Gets the current font metrics for the specified font.
     *
     * @param f the specified font
     * @see #getFont
     * @see #getFontMetrics
     */
    public FontMetrics getFontMetrics(Font f) {
        return g.getFontMetrics(f);
    }


    /**
     * Returns the bounding rectangle of the current clipping area.
     *
     * @see #clipRect
     */
    public Rectangle getClipBounds() {
        return clippingRect;
    }


    public Shape getClip() {
        return clippingRect;
    }


    public void drawPolyline(int xPoints[],
                             int yPoints[],
                             int nPoints) {
        throw new RuntimeException("drawPolyline not supported");
    }


    /**
     * Clips to a rectangle. The resulting clipping area is the intersection of the current clipping area and the
     * specified rectangle. Graphic operations have no effect outside of the clipping area.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #getClipBounds()
     */
    public void clipRect(int x, int y, int width, int height) {
        prt("%clipRect");
        y = transformY(y);
        clippingRect = new Rectangle(x, y, width, height);
        prt("initclip");
        prt(x, y, "moveto");
        prt(x + width, y, "lineto");
        prt(x + width, y - height, "lineto");
        prt(x, y - height, "lineto");
        prt("closepath eoclip newpath");
    }


    public void setClip(int x, int y, int width, int height) {
        clipRect(x, y, width, height);
    }


    public void setClip(Shape s) {
        throw new RuntimeException("setClip(Shape) not supported");
    }


    /**
     * Copies an area of the screen.
     *
     * @param x      the x-coordinate of the source
     * @param y      the y-coordinate of the source
     * @param width  the width
     * @param height the height
     * @param dx     the horizontal distance
     * @param dy     the vertical distance Note: copyArea not supported by PostScript
     */
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        throw new RuntimeException("copyArea not supported");
    }


    /**
     * Draws a line between the coordinates (x1,y1) and (x2,y2). The line is drawn below and to the left of the logical
     * coordinates.
     *
     * @param x1 the first point's x coordinate
     * @param y1 the first point's y coordinate
     * @param x2 the second point's x coordinate
     * @param y2 the second point's y coordinate
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        y1 = transformY(y1);
        y2 = transformY(y2);
        String s = x1 + " " + y1 + " moveto " + x2 + " " + y2 + " lineto stroke";
        prt(s);
    }


    void doRect(int x, int y, int width, int height, boolean fill) {
        y = transformY(y);
        prt(x, y, "moveto");
        prt(x + width, y, "lineto");
        prt(x + width, y - height, "lineto");
        prt(x, y - height, "lineto");
        prt(x, y, "lineto");
        if (fill) prt("eofill");
        else prt("stroke");
    }


    /**
     * Fills the specified rectangle with the current color.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #drawRect
     * @see #clearRect
     */
    public void fillRect(int x, int y, int width, int height) {
        prt("%fillRect");
        doRect(x, y, width, height, true);
    }


    /**
     * Draws the outline of the specified rectangle using the current color. Use drawRect(x, y, width-1, height-1) to
     * draw the outline inside the specified rectangle.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #fillRect
     * @see #clearRect
     */
    public void drawRect(int x, int y, int width, int height) {
        prt("%drawRect");
        doRect(x, y, width, height, false);
    }


    /**
     * Clears the specified rectangle by filling it with the current background color of the current drawing surface.
     * Which drawing surface it selects depends on how the graphics context was created.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #fillRect
     * @see #drawRect
     */
    public void clearRect(int x, int y, int width, int height) {
        prt("%clearRect");
        prt("gsave");
        prt("1 1 1 setrgbcolor");
        doRect(x, y, width, height, true);
        prt("grestore");
    }


    private void doRoundRect(int x, int y, int width, int height,
                             int arcWidth, int arcHeight, boolean fill) {
        // This code has not been tested.
        y = transformY(y);
        prt(x + arcHeight, y, "moveto");
        // bottom, left to right
        prt(x + width, y, x + width, y - height, arcHeight, "arcto 4 (pop) repeat");
        // right, top to bottom
        prt(x + width, y - height, x, y - height, arcHeight, "arcto 4 (pop) repeat");
        // top, left to right
        prt(x, y - height, x, y, arcHeight, "arcto 4 (pop) repeat");
        // left, top to bottom
        prt(x, y, x + width, y, arcHeight, "arcto 4 (pop) repeat");
        if (fill) prt("eofill");
        else prt("stroke");
    }


    /**
     * Draws an outlined rounded corner rectangle using the current color.
     *
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param width     the width of the rectangle
     * @param height    the height of the rectangle
     * @param arcWidth  the diameter of the arc
     * @param arcHeight the radius of the arc
     * @see #fillRoundRect
     */
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        prt("%drawRoundRect");
        doRoundRect(x, y, width, height, arcWidth, arcHeight, false);
    }


    /**
     * Draws a rounded rectangle filled in with the current color.
     *
     * @param x         the x coordinate
     * @param y         the y coordinate
     * @param width     the width of the rectangle
     * @param height    the height of the rectangle
     * @param arcWidth  the diameter of the arc
     * @param arcHeight the radius of the arc
     * @see #drawRoundRect
     */
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        prt("%fillRoundRect");
        doRoundRect(x, y, width, height, arcWidth, arcHeight, true);
    }


    /**
     * Draws a highlighted 3-D rectangle.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param raised a boolean that states whether the rectangle is raised or not
     */
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        prt("%draw3DRect");
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();

        setColor(raised ? brighter : darker);
        drawLine(x, y, x, y + height);
        drawLine(x + 1, y, x + width - 1, y);
        setColor(raised ? darker : brighter);
        drawLine(x + 1, y + height, x + width, y + height);
        drawLine(x + width, y, x + width, y + height);
        setColor(c);
    }


    /**
     * Paints a highlighted 3-D rectangle using the current color.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @param raised a boolean that states whether the rectangle is raised or not
     */
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        prt("%fill3DRect");
        Color c = getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();

        if (!raised) {
            setColor(darker);
        }
        fillRect(x + 1, y + 1, width - 2, height - 2);
        setColor(raised ? brighter : darker);
        drawLine(x, y, x, y + height - 1);
        drawLine(x + 1, y, x + width - 2, y);
        setColor(raised ? darker : brighter);
        drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        drawLine(x + width - 1, y, x + width - 1, y + height - 1);
        setColor(c);
    }


    /**
     * Draws an oval inside the specified rectangle using the current color.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #fillOval
     */
    public void drawOval(int x, int y, int width, int height) {
        prt("%drawOval");
        doArc(x, y, width, height, 0, 360, false);
    }


    /**
     * Fills an oval inside the specified rectangle using the current color.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #drawOval
     */
    public void fillOval(int x, int y, int width, int height) {
        prt("%fillOval");
        doArc(x, y, width, height, 0, 360, true);
    }


    private void doArc(int x, int y, int width, int height,
                       int startAngle, int arcAngle, boolean fill) {
        y = transformY(y);
        prt("gsave");

        // cx,cy is the center of the arc
        float cx = x + (float) width / 2;
        float cy = y - (float) height / 2;

        // translate the page to be centered there
        prt(cx, cy, "translate");

        // scale the coordinate system - this is the only way to directly draw
        // an eliptical arc in postscript. Calculate the scale:

        float yscale = (float) height / (float) width;
        prt(1.0, yscale, "scale");
        if (fill) {
            prt("0 0 moveto");
        }

        // now draw the arc.
        float endAngle = startAngle + arcAngle;
        prt(0, 0, width / 2.0, startAngle, endAngle, "arc");
        if (fill) {
            prt("closepath eofill");
        } else {
            prt("stroke");
        }

        // undo all the scaling!
        prt("grestore");

    }


    /**
     * Draws an arc bounded by the specified rectangle from startAngle to endAngle. 0 degrees is at the 3-o'clock
     * position.Positive arc angles indicate counter-clockwise rotations, negative arc angles are drawn clockwise.
     *
     * @param x          the x coordinate
     * @param y          the y coordinate
     * @param width      the width of the rectangle
     * @param height     the height of the rectangle
     * @param startAngle the beginning angle
     * @param arcAngle   the angle of the arc (relative to startAngle).
     * @see #fillArc
     */
    public void drawArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle) {
        prt("%drawArc");
        doArc(x, y, width, height, startAngle, arcAngle, false);
    }


    /**
     * Fills an arc using the current color. This generates a pie shape.
     *
     * @param x          the x coordinate
     * @param y          the y coordinate
     * @param width      the width of the arc
     * @param height     the height of the arc
     * @param startAngle the beginning angle
     * @param arcAngle   the angle of the arc (relative to startAngle).
     * @see #drawArc
     */
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        prt("%fillArc");
        doArc(x, y, width, height, startAngle, arcAngle, true);
    }


    private void doPoly(int xPoints[], int yPoints[], int nPoints, boolean fill) {
        if (nPoints < 2)
            return;

        int newYPoints[] = new int[nPoints];
        int i;

        for (i = 0; i < nPoints; i++)
            newYPoints[i] = transformY(yPoints[i]);

        prt(xPoints[0], newYPoints[0], "moveto");

        for (i = 0; i < nPoints; i++) {
            prt(xPoints[i], newYPoints[i], "lineto");
        }
        if (fill) prt("eofill");
        else prt("stroke");
    }


    /**
     * Draws a polygon defined by an array of x points and y points.
     *
     * @param xPoints an array of x points
     * @param yPoints an array of y points
     * @param nPoints the total number of points
     * @see #fillPolygon
     */
    public void drawPolygon(int xPoints[], int yPoints[], int nPoints) {
        prt("%drawPoly");
        doPoly(xPoints, yPoints, nPoints, false);
    }


    /**
     * Draws a polygon defined by the specified point.
     *
     * @param p the specified polygon
     * @see #fillPolygon
     */
    public void drawPolygon(Polygon p) {
        prt("%drawPoly");
        doPoly(p.xpoints, p.ypoints, p.npoints, false);
    }


    /**
     * Fills a polygon with the current color.
     *
     * @param xPoints an array of x points
     * @param yPoints an array of y points
     * @param nPoints the total number of points
     * @see #drawPolygon
     */
    public void fillPolygon(int xPoints[], int yPoints[], int nPoints) {
        prt("%fillPoly");
        doPoly(xPoints, yPoints, nPoints, true);
    }


    /**
     * Fills the specified polygon with the current color.
     *
     * @param p the polygon
     * @see #drawPolygon
     */
    public void fillPolygon(Polygon p) {
        prt("%fillPoly");
        doPoly(p.xpoints, p.ypoints, p.npoints, true);
    }


    /**
     * Draws the specified String using the current font and color. The x,y position is the starting point of the
     * baseline of the String.
     *
     * @param str the String to be drawn
     * @param x   the x coordinate
     * @param y   the y coordinate
     * @see #drawChars
     * @see #drawBytes
     */
    public void drawString(String str, int x, int y) {
        y = transformY(y);
        prt(x, y, "moveto (" + str + ") show stroke");
    }


    /**
     * Draws the specified String using the current font and color. The x,y position is the starting point of the
     * baseline of the String.
     *
     * @param atr the String to be drawn
     * @param x   the x coordinate
     * @param y   the y coordinate
     * @see #drawChars
     * @see #drawBytes
     */
    public void drawString(java.text.AttributedCharacterIterator atr, int x, int y) {
        y = transformY(y);
        String str = atr.toString();
        prt(x, y, "moveto (" + str + ") show stroke");
    }


    /**
     * Draws the specified characters using the current font and color.
     *
     * @param data   the array of characters to be drawn
     * @param offset the start offset in the data
     * @param length the number of characters to be drawn
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @see #drawString
     * @see #drawBytes
     */
    public void drawChars(char data[], int offset, int length, int x, int y) {
        drawString(new String(data, offset, length), x, y);
    }


    /**
     * Draws the specified bytes using the current font and color.
     *
     * @param img the data to be drawn
     * @param x   the x coordinate
     * @param y   the y coordinate
     * @see #drawString
     * @see #drawChars
     */
    //  public void drawBytes(byte data[], int offset, int length, int x, int y) {
    //    drawString(new String(data, 0, offset, length), x, y);
    //  }
    boolean doImage(Image img, int x, int y, int width, int height,
                    ImageObserver observer, Color bgcolor) {
        y = transformY(y);

        // This class fetches the pixels in its constructor.
        PixelConsumer pc = new PixelConsumer(img);

        prt("gsave");

        prt("% build a temporary dictionary");
        prt("20 dict begin");
        emitColorImageProlog(pc.xdim);

        prt("% lower left corner");
        prt(x);
        prt(" ");
        prt(y);
        prt(" translate");

        // compute image size. First of all, if width or height is 0, image is 1:1.
        if (height == 0 || width == 0) {
            height = pc.ydim;
            width = pc.xdim;
        }

        prt("% size of image");
        prt(width, height, "scale");

        prt(pc.xdim, pc.ydim, "8");

        prt("[" + pc.xdim + " 0 0 -" + pc.ydim + " 0 0 ]");
        prt("{currentfile pix readhexstring pop}");
        prt("false 3 colorimage");
        prt("");


        int offset, sleepyet = 0;
        // array to hold a line of pixel data
        char[] sb = new char[charsPerRow + 1];

        for (int i = 0; i < pc.ydim; i++) {
            offset = 0;
            ++sleepyet;
            if (bgcolor == null) {
                // real color image. We're deliberately duplicating code here
                // in the interest of speed - we don't want to check bgcolor
                // on every iteration.
                for (int j = 0; j < pc.xdim; j++) {
                    int n = pc.pix[j][i];

                    // put hex chars into string
                    // flip red for blue, to make postscript happy.

                    sb[offset++] = hd[(n & 0xF0) >> 4];
                    sb[offset++] = hd[(n & 0xF)];
                    sb[offset++] = hd[(n & 0xF000) >> 12];
                    sb[offset++] = hd[(n & 0xF00) >> 8];
                    sb[offset++] = hd[(n & 0xF00000) >> 20];
                    sb[offset++] = hd[(n & 0xF0000) >> 16];

                    if (offset >= charsPerRow) {
                        String s = String.copyValueOf(sb, 0, offset);
                        prt(s);
                        if (sleepyet > 5) {
                            try {
                                // let the screen update occasionally!
                                Thread.sleep(15);
                            } catch (java.lang.InterruptedException ex) {
                                // yeah, so?
                            }
                            sleepyet = 0;
                        }
                        offset = 0;
                    }
                }
            } else {
                prt("%FalseColor");
                // false color image.
                for (int j = 0; j < pc.xdim; j++) {
                    int bg =
                            bgcolor.getGreen() << 16 + bgcolor.getBlue() << 8 + bgcolor.getRed();
                    int fg =
                            clr.getGreen() << 16 + clr.getBlue() << 8 + clr.getRed();

                    int n = (pc.pix[j][i] == 1 ? fg : bg);

                    // put hex chars into string

                    sb[offset++] = hd[(n & 0xF0)];
                    sb[offset++] = hd[(n & 0xF)];
                    sb[offset++] = hd[(n & 0xF000)];
                    sb[offset++] = hd[(n & 0xF00)];
                    sb[offset++] = hd[(n & 0xF00000)];
                    sb[offset++] = hd[(n & 0xF0000)];

                    if (offset >= charsPerRow) {
                        String s = String.copyValueOf(sb, 0, offset);
                        prt(s);
                        if (sleepyet > 5) {
                            try {
                                // let the screen update occasionally!
                                Thread.sleep(15);
                            } catch (java.lang.InterruptedException ex) {
                                // yeah, so?
                            }
                            sleepyet = 0;
                        }
                        offset = 0;
                    }
                }
            }
            // print partial rows
            if (offset != 0) {
                String s = String.copyValueOf(sb, 0, offset);
                prt(s);
            }
        }

        prt("");
        prt("end");
        prt("grestore");

        return true;
    }


    /**
     * Draws the specified image at the specified coordinate (x, y). If the image is incomplete the image observer will
     * be notified later.
     *
     * @param img      the specified image to be drawn
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param observer notifies if the image is complete or not
     * @see Image
     * @see ImageObserver
     */

    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        prt("%drawImage-1");

        return doImage(img, x, y, 0, 0, observer, null);

    }


    /**
     * Draws the specified image inside the specified rectangle. The image is scaled if necessary. If the image is
     * incomplete the image observer will be notified later.
     *
     * @param img      the specified image to be drawn
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param width    the width of the rectangle
     * @param height   the height of the rectangle
     * @param observer notifies if the image is complete or not
     * @see Image
     * @see ImageObserver
     */
    public boolean drawImage(Image img, int x, int y,
                             int width, int height,
                             ImageObserver observer) {
        prt("%drawImage-2");
        return doImage(img, x, y, width, height, observer, null);
    }


    /**
     * Draws the specified image at the specified coordinate (x, y). If the image is incomplete the image observer will
     * be notified later.
     *
     * @param img      the specified image to be drawn
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param bgcolor  the background color
     * @param observer notifies if the image is complete or not
     * @see Image
     * @see ImageObserver
     */

    public boolean drawImage(Image img, int x, int y, Color bgcolor,
                             ImageObserver observer) {
        prt("%drawImage-3");
        return doImage(img, x, y, 0, 0, observer, bgcolor);
    }


    /**
     * Draws the specified image inside the specified rectangle. The image is scaled if necessary. If the image is
     * incomplete the image observer will be notified later.
     *
     * @param img      the specified image to be drawn
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param width    the width of the rectangle
     * @param height   the height of the rectangle
     * @param bgcolor  the background color
     * @param observer notifies if the image is complete or not
     * @see Image
     * @see ImageObserver NOTE: PSGr ignores the background color.
     */
    public boolean drawImage(Image img, int x, int y,
                             int width, int height, Color bgcolor,
                             ImageObserver observer) {
        prt("%drawImage-4");
        return doImage(img, x, y, width, height, observer, bgcolor);
    }


    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor, ImageObserver observer) {
        throw new RuntimeException("fancy drawImage not supported");
    }


    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer) {

        throw new RuntimeException("fancy drawImage not supported");
    }


    /**
     * Disposes of this graphics context.  The Graphics context cannot be used after being disposed of.
     *
     * @see #finalize
     */
    public void dispose() {
        prt("%dispose");
        os.flush();
    }


    /**
     * Disposes of this graphics context once it is no longer referenced.
     *
     * @see #dispose
     */
    public void finalize() {
        super.finalize();
        dispose();
    }


    /**
     * Returns a String object representing this Graphic's value.
     */
    public String toString() {
        return getClass().getName() + "[font=" + getFont() + ",color=" + getColor() + "]";
    }


    /**
     * Flip Y coords so Postscript looks like Java
     */

    int transformY(int y) {
        return PAGEHEIGHT - y;
    }


    /**
     * Top of every PS file
     */

    void emitProlog() {
        prt("%!PS-Adobe-2.0 Created by PSGr Java PostScript Context");
        prt("30 -30 translate");
        setFont(font);
    }


    void emitColorImageProlog(int xdim) {
        prt("% Color picture stuff, lifted from XV's PS files");

        prt("% define string to hold a scanline's worth of data");
        prt("/pix ");
        prt(xdim * 3);
        prt(" string def");

        prt("% define space for color conversions");
        prt("/grays ");
        prt(xdim);
        prt(" string def  % space for gray scale line");
        prt("/npixls 0 def");
        prt("/rgbindx 0 def");

        prt("% define 'colorimage' if it isn't defined");
        prt("%   ('colortogray' and 'mergeprocs' come from xwd2ps");
        prt("%     via xgrab)");
        prt("/colorimage where   % do we know about 'colorimage'?");
        prt("{ pop }           % yes: pop off the 'dict' returned");
        prt("{                 % no:  define one");
        prt("/colortogray {  % define an RGB->I function");
        prt("/rgbdata exch store    % call input 'rgbdata'");
        prt("rgbdata length 3 idiv");
        prt("/npixls exch store");
        prt("/rgbindx 0 store");
        prt("0 1 npixls 1 sub {");
        prt("grays exch");
        prt("rgbdata rgbindx       get 20 mul    % Red");
        prt("rgbdata rgbindx 1 add get 32 mul    % Green");
        prt("rgbdata rgbindx 2 add get 12 mul    % Blue");
        prt("add add 64 idiv      % I = .5G + .31R + .18B");
        prt("put");
        prt("/rgbindx rgbindx 3 add store");
        prt("} for");
        prt("grays 0 npixls getinterval");
        prt("} bind def");
        prt("");
        prt("% Utility procedure for colorimage operator.");
        prt("% This procedure takes two procedures off the");
        prt("% stack and merges them into a single procedure.");
        prt("");
        prt("/mergeprocs { % def");
        prt("dup length");
        prt("3 -1 roll");
        prt("dup");
        prt("length");
        prt("dup");
        prt("5 1 roll");
        prt("3 -1 roll");
        prt("add");
        prt("array cvx");
        prt("dup");
        prt("3 -1 roll");
        prt("0 exch");
        prt("putinterval");
        prt("dup");
        prt("4 2 roll");
        prt("putinterval");
        prt("} bind def");
        prt("");
        prt("/colorimage { % def");
        prt("pop pop     % remove 'false 3' operands");
        prt("{colortogray} mergeprocs");
        prt("image");
        prt("} bind def");
        prt("} ifelse          % end of 'false' case");

    }


    public void gsave() {
        prt("gsave");
    }


    public void grestore() {
        prt("grestore");
    }

}


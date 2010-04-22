package org.rosuda.mondrian;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.OutputStream;
import java.io.PrintStream;


/**
 * PSGr is a Graphics subclass that images to PostScript. (C) 1996 E.j. Friedman-Hill and Sandia National Labs
 *
 * @author Ernest Friedman-Hill
 * @author ejfried@ca.sandia.gov
 * @author http://herzberg.ca.sandia.gov
 * @version 1.0
 */

class PSGr_2 extends java.awt.Graphics {

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

    private PrintStream os = System.out;

    /**
     * The current color
     */

    private Color clr = Color.black;


    /**
     * The background color of the current widget. It's up to the client software to set this correctly!
     */

    private Color backClr = Color.white;

    /**
     * The current font
     */

    private Font font = new Font("Helvetica", Font.PLAIN, 12);

    private Rectangle clippingRect = new Rectangle(0, 0, PAGEWIDTH, PAGEHEIGHT);

    private Graphics g;


    /**
     * Constructs a new PSGr Object. Unlike regular Graphics objects, PSGr contexts can be created directly.
     *
     * @param o Output stream for PostScript output
     * @see #create
     */

    public PSGr_2(OutputStream o, Graphics g) {
        os = new PrintStream(o);
        this.g = g;
        emitProlog();
    }


    public PSGr_2(OutputStream o, Graphics g, int what) {
        os = new PrintStream(o);
        this.g = g;
        if (what != CLONE)
            emitProlog();
    }

//
// dfs; adding a constructor to pass in the dimensions of the object to be printed
//


    public PSGr_2(OutputStream o, Graphics g, Dimension appsz) {
        os = new PrintStream(o);
        this.g = g;
        emitProlog();

        // dfs
        // We want some additional prolog lines:
        os.println("/min {2 copy gt {exch} if pop} def");

        os.println("clippath pathbbox newpath");
        os.println("4 -1 roll exch 4 1 roll 4 copy");
        os.println("sub /width exch def");
        os.println("sub /height exch def");
        os.println("add 2 div /xcenter exch def");
        os.println("add 2 div /ycenter exch def");
        os.println("xcenter ycenter translate");
        os.println("/ht " + appsz.height + " def");
        os.println("/wid " + appsz.width + " def");
        os.println("wid ht gt { height width /height exch def /width exch def 90 rotate } if");
        // choose the minimum of height/ht and width/wd; multiply it
        // by .95; take the minimum of that value and 1.0; scale
        os.println("height ht div width wid div min .95 mul 1.0 min dup scale");
        os.println("wid 2 div neg ht 2 div neg translate");
        os.println("0 " + PAGEHEIGHT + " ht sub neg translate");
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
        os.println("%translate");
        os.print(x);
        os.print(" ");
        os.print(y);
        os.println(" translate");
    }


    /**
     * Scales the graphics context. All subsequent operations on this graphics context will be affected.
     *
     * @param sx the scaled x coordinate
     * @param sy the scaled y coordinate
     * @see #translate
     */
    public void scale(float sx, float sy) {
        os.println("%scale");
        os.print(sx);
        os.print(" ");
        os.print(sy);
        os.println(" scale");
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
     * Gets the current color.
     *
     * @see #setColor
     */
    public void setBackground(Color c) {
        backClr = c;
    }


    /**
     * Sets the current color to the specified color. All subsequent graphics operations will use this specified color.
     *
     * @param c the color to be set
     * @see Color
     * @see #getColor
     */

    public void setColor(Color c) {
        os.println("%setColor");
        if (c != null)
            clr = c;
        os.print(clr.getRed() / 255.0);
        os.print(" ");
        os.print(clr.getGreen() / 255.0);
        os.print(" ");
        os.print(clr.getBlue() / 255.0);
        os.println(" setrgbcolor");
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
        os.println("%setFont");
        if (f != null) {
            this.font = f;
            String javaName = font.getName();
            int javaStyle = font.getStyle();
            String psName;

            if (javaName.equals("Symbol"))
                psName = "Symbol";

            else if (javaName.startsWith("Times")) {
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

            os.println("/" + psName + " findfont");
            os.print(font.getSize());
            os.println(" scalefont setfont");
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
    public Rectangle getClipRect() {
        return clippingRect;
    }


    /**
     * Clips to a rectangle. The resulting clipping area is the intersection of the current clipping area and the
     * specified rectangle. Graphic operations have no effect outside of the clipping area.
     *
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @see #getClipRect
     */
    public void clipRect(int x, int y, int width, int height) {
        os.println("%clipRect");
        y = transformY(y);
        clippingRect = new Rectangle(x, y, width, height);
        os.println("initclip");

        os.println(x + " " + y + " moveto");
        os.println((x + width) + " " + y + " lineto");
        os.println((x + width) + " " + (y - height) + " lineto");
        os.println(x + " " + (y - height) + " lineto");
        os.println("closepath eoclip newpath");
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
        os.println("%drawLine");
        y1 = transformY(y1);
        y2 = transformY(y2);
        os.print(x1);
        os.print(" ");
        os.print(y1);
        os.print(" moveto ");
        os.print(x2);
        os.print(" ");
        os.print(y2);
        os.println(" lineto stroke");
    }


    void doRect(int x, int y, int width, int height, boolean fill) {
        System.out.println("DoRect: x:" + x + "  y:" + y + "  width:" + width + "  height:" + height);
        os.println("%doRect");
        y = transformY(y);
        os.print(x);
        os.print(" ");
        os.print(y);
        os.println(" moveto ");
        os.print(x + width);
        os.print(" ");
        os.print(y);
        os.println(" lineto ");
        os.print(x + width);
        os.print(" ");
        os.print(y - height);
        os.println(" lineto ");
        os.print(x);
        os.print(" ");
        os.print(y - height);
        os.println(" lineto ");
        os.print(x);
        os.print(" ");
        os.print(y);
        os.println(" lineto ");
        if (fill)
            os.println("eofill");
        else
            os.println("stroke");

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
        os.println("%fillRect");
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
        os.println("%drawRect");
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
        os.println("%clearRect");
        os.println("gsave");
        Color c = getColor();
        setColor(backClr);
        doRect(x, y, width, height, true);
        setColor(c);
        os.println("grestore");
    }


    private void doRoundRect(int x, int y, int width, int height,
                             int arcWidth, int arcHeight, boolean fill) {
        os.println("%doRoundRect");
        y = transformY(y);
        os.print(x + arcHeight);
        os.print(" ");
        os.print(y);
        os.println(" moveto");

        // top, left to right
        os.print(x + width);
        os.print(" ");
        os.print(y);
        os.print(" ");
        os.print(x + width);
        os.print(" ");
        os.print(y - height);
        os.print(" ");
        os.print(arcHeight);
        os.println(" arcto");
        os.println("4 {pop} repeat");

        // right, top to bottom
        os.print(x + width);
        os.print(" ");
        os.print(y - height);
        os.print(" ");
        os.print(x);
        os.print(" ");
        os.print(y - height);
        os.print(" ");
        os.print(arcHeight);
        os.println(" arcto");
        os.println("4 {pop} repeat");

        // top, left to right
        os.print(x);
        os.print(" ");
        os.print(y - height);
        os.print(" ");
        os.print(x);
        os.print(" ");
        os.print(y);
        os.print(" ");
        os.print(arcHeight);
        os.println(" arcto");
        os.println("4 {pop} repeat");

        // left, top to bottom
        os.print(x);
        os.print(" ");
        os.print(y);
        os.print(" ");
        os.print(x + width);
        os.print(" ");
        os.print(y);
        os.print(" ");
        os.print(arcHeight);
        os.println(" arcto");
        os.println("4 {pop} repeat");

        if (fill)
            os.println("eofill");
        else
            os.println("stroke");

    }

    /*  public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
    os.println("%drawImage -- not yet implemented");
    }*/


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
        os.println("%drawRoundRect");
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
        os.println("%fillRoundRect");
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
        os.println("%draw3DRect");
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
        os.println("%fill3DRect");
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
        os.println("%drawOval");
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
        os.println("%fillOval");
        doArc(x, y, width, height, 0, 360, true);
    }


    private void doArc(int x, int y, int width, int height,
                       int startAngle, int arcAngle, boolean fill) {
        os.println("%doArc");
        y = transformY(y);
        os.println("gsave");

        // cx,cy is the center of the arc
        float cx = x + (float) width / 2;
        float cy = y - (float) height / 2;

        // translate the page to be centered there
        os.print(cx);
        os.print(" ");
        os.print(cy);
        os.println(" translate");

        // scale the coordinate system - this is the only way to directly draw
        // an eliptical arc in postscript. Calculate the scale:

        float yscale = (float) height / (float) width;
        os.print(1.0);
        os.print(" ");
        os.print(yscale);
        os.println(" scale");

        if (fill) {
            os.println("0 0 moveto");
        }

        // now draw the arc.
        float endAngle = startAngle + arcAngle;
        os.print("0 0 ");
        os.print((float) width / 2.0);
        os.print(" ");
        os.print(startAngle);
        os.print(" ");
        os.print(endAngle);
        os.println(" arc");

        if (fill) {
            os.println("closepath eofill");
        } else {
            os.println("stroke");
        }

        // undo all the scaling!
        os.println("grestore");

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
        os.println("%drawArc");
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
        os.println("%fillArc");
        doArc(x, y, width, height, startAngle, arcAngle, true);
    }


    private void doPoly(int xPoints[], int yPoints[], int nPoints, boolean fill) {
        if (nPoints < 2)
            return;

        int newYPoints[] = new int[nPoints];
        int i;

        for (i = 0; i < nPoints; i++)
            newYPoints[i] = transformY(yPoints[i]);

        os.println(xPoints[0]);
        os.println(" ");
        os.println(newYPoints[0]);
        os.println(" moveto");

        for (i = 0; i < nPoints; i++) {
            os.println(xPoints[i]);
            os.println(" ");
            os.println(newYPoints[i]);
            os.println(" lineto");
        }

        if (fill)
            os.println("eofill");
        else
            os.println("stroke");

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
        os.println("%drawPoly");
        doPoly(xPoints, yPoints, nPoints, false);
    }


    /**
     * Draws a polygon defined by the specified point.
     *
     * @param p the specified polygon
     * @see #fillPolygon
     */
    public void drawPolygon(Polygon p) {
        os.println("%drawPoly");
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
        os.println("%fillPoly");
        doPoly(xPoints, yPoints, nPoints, true);
    }


    /**
     * Fills the specified polygon with the current color.
     *
     * @param p the polygon
     * @see #drawPolygon
     */
    public void fillPolygon(Polygon p) {
        os.println("%fillPoly");
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
    public void drawString(java.lang.String str, int x, int y) {
        os.println("%drawString");
        y = transformY(y);
        os.print(x);
        os.print(" ");
        os.print(y);
        os.print(" moveto (");
        os.print(str);
        os.println(") show stroke");
    }


    public void drawString(java.text.AttributedCharacterIterator str, int x, int y) {
        os.println("%drawString");
        y = transformY(y);
        os.print(x);
        os.print(" ");
        os.print(y);
        os.print(" moveto (");
        os.print(str);
        os.println(") show stroke");
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
        os.println("%drawChars");
        drawString(new String(data, offset, length), x, y);
    }


    /**
     * Draws the specified bytes using the current font and color.
     *
     * @param data   the data to be drawn
     * @param offset the start offset in the data
     * @param length the number of bytes that are drawn
     * @param x      the x coordinate
     * @param y      the y coordinate
     * @see #drawString
     * @see #drawChars
     */
    public void drawBytes(byte data[], int offset, int length, int x, int y) {
        os.println("%drawBytes");
        drawString(new String(data, 0, offset, length), x, y);
    }


    boolean doImage(Image img, int x, int y, int width, int height,
                    ImageObserver observer, Color bgcolor) {
        os.println("%doImage");
        y = transformY(y);

        // This class fetches the pixels in its constructor.
        PixelConsumer pc = new PixelConsumer(img);

        os.println("gsave");

        os.println("% build a temporary dictionary");
        os.println("20 dict begin");
        emitColorImageProlog(pc.xdim);

        os.println("% lower left corner");
        os.print(x);
        os.print(" ");
        os.print(y);
        os.println(" translate");

        // compute image size. First of all, if width or height is 0, image is 1:1.
        if (height == 0 || width == 0) {
            height = pc.ydim;
            width = pc.xdim;
        }

        os.println("% size of image");
        os.print(width);
        os.print(" ");
        os.print(height);
        os.println(" scale");

        os.print(pc.xdim);
        os.print(" ");
        os.print(pc.ydim);
        os.println(" 8");

        os.print("[");
        os.print(pc.xdim);
        os.print(" 0 0 -");
        os.print(pc.ydim);
        os.print(" 0 ");
        os.print(0);
        os.println("]");

        os.println("{currentfile pix readhexstring pop}");
        os.println("false 3 colorimage");
        os.println("");


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
                        os.println(s);
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
                os.println("%FalseColor");
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
                        os.println(s);
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
                os.println(s);
            }
        }

        os.println("");
        os.println("end");
        os.println("grestore");

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

    public boolean drawImage(Image img, int x, int y,
                             ImageObserver observer) {
        os.println("%drawImage-1");

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
        os.println("%drawImage-2");
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
        os.println("%drawImage-3");
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
        os.println("%drawImage-4");
        return doImage(img, x, y, width, height, observer, bgcolor);
    }


    /**
     * Disposes of this graphics context.  The Graphics context cannot be used after being disposed of.
     *
     * @see #finalize
     */
    public void dispose() {
        os.println("%dispose");
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
        os.println("%!PS-Adobe-2.0 Created by PSGr Java PostScript Context");
        os.println("% PSGr is (C) 1996 Ernest Friedman-Hill and Sandia National Labs");
        os.println("% Right to unrestricted personal and commerical use is granted");
        os.println("% if this acknowledgement is given on product or packing materials");
        // dfs: we're doing our own translate
        // os.println("30 -30 translate");
        setFont(font);
    }


    void emitColorImageProlog(int xdim) {
        os.println("% Color picture stuff, lifted from XV's PS files");

        os.println("% define string to hold a scanline's worth of data");
        os.print("/pix ");
        os.print(xdim * 3);
        os.println(" string def");

        os.println("% define space for color conversions");
        os.print("/grays ");
        os.print(xdim);
        os.println(" string def  % space for gray scale line");
        os.println("/npixls 0 def");
        os.println("/rgbindx 0 def");

        os.println("% define 'colorimage' if it isn't defined");
        os.println("%   ('colortogray' and 'mergeprocs' come from xwd2ps");
        os.println("%     via xgrab)");
        os.println("/colorimage where   % do we know about 'colorimage'?");
        os.println("{ pop }           % yes: pop off the 'dict' returned");
        os.println("{                 % no:  define one");
        os.println("/colortogray {  % define an RGB->I function");
        os.println("/rgbdata exch store    % call input 'rgbdata'");
        os.println("rgbdata length 3 idiv");
        os.println("/npixls exch store");
        os.println("/rgbindx 0 store");
        os.println("0 1 npixls 1 sub {");
        os.println("grays exch");
        os.println("rgbdata rgbindx       get 20 mul    % Red");
        os.println("rgbdata rgbindx 1 add get 32 mul    % Green");
        os.println("rgbdata rgbindx 2 add get 12 mul    % Blue");
        os.println("add add 64 idiv      % I = .5G + .31R + .18B");
        os.println("put");
        os.println("/rgbindx rgbindx 3 add store");
        os.println("} for");
        os.println("grays 0 npixls getinterval");
        os.println("} bind def");
        os.println("");
        os.println("% Utility procedure for colorimage operator.");
        os.println("% This procedure takes two procedures off the");
        os.println("% stack and merges them into a single procedure.");
        os.println("");
        os.println("/mergeprocs { % def");
        os.println("dup length");
        os.println("3 -1 roll");
        os.println("dup");
        os.println("length");
        os.println("dup");
        os.println("5 1 roll");
        os.println("3 -1 roll");
        os.println("add");
        os.println("array cvx");
        os.println("dup");
        os.println("3 -1 roll");
        os.println("0 exch");
        os.println("putinterval");
        os.println("dup");
        os.println("4 2 roll");
        os.println("putinterval");
        os.println("} bind def");
        os.println("");
        os.println("/colorimage { % def");
        os.println("pop pop     % remove 'false 3' operands");
        os.println("{colortogray} mergeprocs");
        os.println("image");
        os.println("} bind def");
        os.println("} ifelse          % end of 'false' case");

    }


    public void gsave() {
        os.println("gsave");
    }


    public void grestore() {
        os.println("grestore");
    }


    public void emitThis(String s) {
        os.println(s);
    }


    // dummies required for 1.1 -- now it doesn't quite compile under 1.0


    public Rectangle getClipBounds() {
        return new Rectangle();
    }


    public void setClip(int a, int b, int c, int d) {
    }


    public Shape getClip() {
        return new Rectangle();
    }


    public void setClip(Shape s) {
    }


    public void drawPolyline(int[] a, int[] b, int c) {
        os.println("%drawPolyline");
        doPoly(a, b, c, false);
    }


    public boolean drawImage(Image i, int a, int b, int c, int d, int e, int f, int g, int h, ImageObserver io) {
        return true;
    }


    public boolean drawImage(Image i, int a, int b, int c, int d, int e, int f, int g, int h, Color color, ImageObserver io) {
        return true;
    }


}



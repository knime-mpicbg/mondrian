package org.rosuda.mondrian;


import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Cloneable;
import java.lang.Object;
import java.lang.String;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;              //


public class MyPoly extends Polygon implements Cloneable {

    public int Id;
    public String name;
    public int npoints;
    protected Color color = Color.lightGray;
    protected Color borderColor = Color.black;
    protected double hilite = 0;
    public boolean[] flag;                       // This is a HACK !!!!


    public MyPoly() {
        super();
        this.flag = new boolean[1500];
    }


    public MyPoly(int xpoints[], int ypoints[], int npoints) {
        super(xpoints, ypoints, npoints);
        this.flag = new boolean[1500];
        this.npoints = npoints;
    }


    public Object clone() {
        MyPoly clone = new MyPoly(this.xpoints, this.ypoints, this.npoints);
        clone.Id = this.Id;
        clone.name = this.name;
        clone.color = this.color;
        clone.hilite = this.hilite;
        clone.flag = this.flag;

        return clone;
    }


    public void transform(int shiftx, int shifty, double scalex, double scaley) {
        for (int j = 0; j < this.npoints; j++) {
            this.xpoints[j] -= shiftx;
            this.xpoints[j] = (int) ((double) this.xpoints[j] * scalex);
            this.ypoints[j] -= shifty;
            this.ypoints[j] = (int) ((double) this.ypoints[j] * scaley);
        }
    }


    public MyPoly thinSoft() {

        MyPoly out = new MyPoly();

        for (int j = 0; j < this.npoints - 1; j++) {
            if (j < 10 || (this.xpoints[j] != this.xpoints[j - 1] ||
                    this.ypoints[j] != this.ypoints[j - 1])) {
                out.addPoint(this.xpoints[j], this.ypoints[j]);
                out.npoints++;
            }
        }
        System.out.println("Old Size: " + (this.npoints - 1) + "  Thinned size: " + out.npoints);
        return out;
    }


    public MyPoly thinHard() {

        MyPoly out = new MyPoly();
        int pHeight = this.getBounds().height;
        int pWidth = this.getBounds().width;

        for (int j = 0; j < this.npoints - 1; j++) {
            if (j < 10 || j > this.npoints - 10 || (Math.abs(this.xpoints[j] - this.xpoints[j - 1]) > pWidth / 1000 ||
                    Math.abs(this.ypoints[j] - this.ypoints[j - 1]) > pHeight / 1000)) {
                out.addPoint(this.xpoints[j], this.ypoints[j]);
                out.npoints++;
            }
        }
        out.Id = this.Id;
        out.name = this.name;
        out.color = this.color;
        out.hilite = this.hilite;
        out.flag = this.flag;
        System.out.println("Old Size: " + (this.npoints - 1) + "  Thinned size: " + out.npoints);
        return out;
    }


    public boolean closed() {
        return (this.xpoints[0] == this.xpoints[this.npoints - 1] &&
                this.ypoints[0] == this.ypoints[this.npoints - 1]);
    }


    public MyPoly join(MyPoly p2) {

        MyPoly out = new MyPoly();

        MyPoly p1 = this;

        if (p1.xpoints[0] == p2.xpoints[0] && p1.ypoints[0] == p2.ypoints[0]) {
            for (int i = p1.npoints - 1; i >= 0; i--) {
                out.addPoint(p1.xpoints[i], p1.ypoints[i]);
                out.npoints++;
            }
            for (int i = 1; i < p2.npoints; i++) {
                out.addPoint(p2.xpoints[i], p2.ypoints[i]);
                out.npoints++;
            }
        } else if (p1.xpoints[p1.npoints - 1] == p2.xpoints[0] && p1.ypoints[p1.npoints - 1] == p2.ypoints[0]) {
            for (int i = 0; i < p1.npoints; i++) {
                out.addPoint(p1.xpoints[i], p1.ypoints[i]);
                out.npoints++;
            }
            for (int i = 1; i < p2.npoints; i++) {
                out.addPoint(p2.xpoints[i], p2.ypoints[i]);
                out.npoints++;
            }
        } else if (p1.xpoints[0] == p2.xpoints[p2.npoints - 1] && p1.ypoints[0] == p2.ypoints[p2.npoints - 1]) {
            for (int i = 0; i < p2.npoints; i++) {
                out.addPoint(p2.xpoints[i], p2.ypoints[i]);
                out.npoints++;
            }
            for (int i = 1; i < p1.npoints; i++) {
                out.addPoint(p1.xpoints[i], p1.ypoints[i]);
                out.npoints++;
            }
        } else if (p1.xpoints[p1.npoints - 1] == p2.xpoints[p2.npoints - 1] && p1.ypoints[p1.npoints - 1] == p2.ypoints[p2.npoints - 1]) {
            for (int i = 0; i < p1.npoints - 1; i++) {
                out.addPoint(p1.xpoints[i], p1.ypoints[i]);
                out.npoints++;
            }
            for (int i = p2.npoints - 1; i >= 0; i--) {
                out.addPoint(p2.xpoints[i], p2.ypoints[i]);
                out.npoints++;
            }
        }
        return out;
    }


    public MyPoly comBorder(MyPoly p2) {

        MyPoly out = new MyPoly();

        MyPoly p1 = this;

        if ((p1.getBounds()).intersects(p2.getBounds())) {
            int direction = 0;
            int l = 0;
            int m = 0;
            int steps1 = 0;
            int steps2 = 0;
            while (Math.abs(steps2) < p2.npoints) {
                while (Math.abs(steps1) < p1.npoints) {
                    //  System.out.println(" l: "+l+"  m: "+m);
                    //	  if( p1.xpoints[l] == p2.xpoints[m] && p1.ypoints[l] == p2.ypoints[m] )
                    //System.out.println("Double! ("+l+") <-> ("+m+")");
                    switch (direction) {
                        case 0:
                            if (p1.xpoints[l] == p2.xpoints[m] && p1.ypoints[l] == p2.ypoints[m]) {
                                if (p1.xpoints[(l + 1) % (p1.npoints - 1)] == p2.xpoints[(m + 1) % (p2.npoints - 1)] &&
                                        p1.ypoints[(l + 1) % (p1.npoints - 1)] == p2.ypoints[(m + 1) % (p2.npoints - 1)]) {
                                    /*p1.flag[l] = true;
                                    p2.flag[m] = true;
                                    out.addPoint(p1.xpoints[l], p1.ypoints[l]);
                                    out.npoints++;*/
                                    direction = 1;
                                    l = (l + 1) % (p1.npoints - 1);
                                    steps1++;
                                    m = (m + 1) % (p2.npoints - 1);
                                    steps2++;
                                    p1.flag[l] = true;
                                    p2.flag[m] = true;
                                    out.addPoint(p1.xpoints[l], p1.ypoints[l]);
                                    out.npoints++;
                                    //System.out.println("Starting forward");
                                } else if (p1.xpoints[(p1.npoints - 1 + l - 1) % (p1.npoints - 1)] == p2.xpoints[(m + 1) % (p2.npoints - 1)] &&
                                        p1.ypoints[(p1.npoints - 1 + l - 1) % (p1.npoints - 1)] == p2.ypoints[(m + 1) % (p2.npoints - 1)]) {
                                    //System.out.println("Starting backward with: "+p1.xpoints[l]+"|"+p1.ypoints[l]);
                                    /*p1.flag[l] = true;
                                    p2.flag[m] = true;
                                    out.addPoint(p1.xpoints[l], p1.ypoints[l]);
                                    out.npoints++;*/
                                    direction = -1;
                                    m = (m + 1) % (p2.npoints - 1);
                                    steps2++;
                                    l = (p1.npoints - 1 + l - 1) % (p1.npoints - 1);
                                    steps1--;
                                    p1.flag[l] = true;
                                    p2.flag[m] = true;
                                    out.addPoint(p1.xpoints[l], p1.ypoints[l]);
                                    out.npoints++;
                                } else {
                                    l = (l + 1) % (p1.npoints - 1);
                                    steps1++;
                                }
                            } else {
                                l = (l + 1) % (p1.npoints - 1);
                                steps1++;
                            }
                            break;
                        case -1:
                            if (p1.xpoints[(p1.npoints - 1 + l - 1) % (p1.npoints - 1)] == p2.xpoints[(m + 1) % (p2.npoints - 1)] &&
                                    p1.ypoints[(p1.npoints - 1 + l - 1) % (p1.npoints - 1)] == p2.ypoints[(m + 1) % (p2.npoints - 1)]) {
                                //System.out.println("Moving backward");
                                m = (m + 1) % (p2.npoints - 1);
                                steps2++;
                                l = (p1.npoints - 1 + l - 1) % (p1.npoints - 1);
                                steps1--;
                                if (p1.xpoints[(p1.npoints - 1 + l - 1) % (p1.npoints - 1)] == p2.xpoints[(m + 1) % (p2.npoints - 1)] &&
                                        p1.ypoints[(p1.npoints - 1 + l - 1) % (p1.npoints - 1)] == p2.ypoints[(m + 1) % (p2.npoints - 1)]) {
                                    p1.flag[l] = true;
                                    p2.flag[m] = true;
                                    out.addPoint(p1.xpoints[l], p1.ypoints[l]);
                                    out.npoints++;
                                }
                            } else
                                direction = 0;
                            break;
                        case 1:
                            if (p1.xpoints[(l + 1) % (p1.npoints - 1)] == p2.xpoints[(m + 1) % (p2.npoints - 1)] &&
                                    p1.ypoints[(l + 1) % (p1.npoints - 1)] == p2.ypoints[(m + 1) % (p2.npoints - 1)]) {
                                l = (l + 1) % (p1.npoints - 1);
                                steps1++;
                                m = (m + 1) % (p2.npoints - 1);
                                steps2++;
                                if (p1.xpoints[(l + 1) % (p1.npoints - 1)] == p2.xpoints[(m + 1) % (p2.npoints - 1)] &&
                                        p1.ypoints[(l + 1) % (p1.npoints - 1)] == p2.ypoints[(m + 1) % (p2.npoints - 1)]) {
                                    p1.flag[l] = true;
                                    p2.flag[m] = true;
                                    out.addPoint(p1.xpoints[l], p1.ypoints[l]);
                                    out.npoints++;
                                }
                                break;
                            } else
                                direction = 0;
                    }
                }
                m = (m + 1) % (p2.npoints - 1);
                steps2++;
                steps1 = 0;
                l = 0;
            }
        } else {
            return out;
        }
        return out;
    }


    public boolean intersects(Rectangle r) {
        // System.out.println("Testing "+super.npoints+" Points");
        Rectangle boundingBox = this.getBounds();
        if (r.intersects(boundingBox)) {
            if (this.contains(new Point(r.x, r.y)) ||
                    this.contains(new Point(r.x + r.width, r.y)) ||
                    this.contains(new Point(r.x, r.y + r.height)) ||
                    this.contains(new Point(r.x + r.width, r.y + r.height)))
                return true;
            if (((r.x < boundingBox.x) && (r.x + r.width > boundingBox.x + boundingBox.width)) ||
                    ((r.y < boundingBox.y) && (r.y + r.height > boundingBox.y + boundingBox.height)))
                return true;
            else {
                int fac = this.npoints / 4;
                for (int i = 0; i < fac + 1; i++) {
                    if (r.contains(new Point(this.xpoints[i], this.ypoints[i])))
                        return true;
                    if (r.contains(new Point(this.xpoints[i + fac], this.ypoints[i + fac])))
                        return true;
                    if (r.contains(new Point(this.xpoints[i + 2 * fac], this.ypoints[i + 2 * fac])))
                        return true;
                    if (r.contains(new Point(this.xpoints[(i + 3 * fac) % this.npoints], this.ypoints[(i + 3 * fac) % this.npoints])))
                        return true;
                }
                return false;
            }
        } else
            return false;
    }


    public void draw(Graphics g) {
        if (hilite > 0)
            g.setColor(DragBox.hiliteColor);
        else
            g.setColor(color);
        g.fillPolygon(this);
        if (borderColor.getAlpha() > 2)
            g.setColor(borderColor);
        else {
            if (hilite > 0)
                g.setColor(DragBox.hiliteColor);
            else
                g.setColor(color);
        }
        g.drawPolygon(this);
    }


    public void setColor(Color c) {
        color = c;
    }


    public void setBorderColor(Color c) {
        borderColor = c;
    }


    public void setHilite(double hilite) {
        this.hilite = hilite;
    }


    public void setLabel(String name) {
        this.name = name;
    }


    public String getLabel() {
        return this.name;
    }


    public void read(BufferedReader br, double shiftX, double scaleX, double shiftY, double scaleY) {

        String line;

        try {
            line = br.readLine();

            StringTokenizer head = new StringTokenizer(line, "\t");

            try {
                this.Id = Integer.valueOf(head.nextToken().trim()).intValue();
                this.name = head.nextToken();
                // if the name is just a dummy (as in MANET), drop it !
                if (this.name.substring(0, 2).equals("/P")) {
                    this.name = "";
                }
                this.npoints = Integer.valueOf(head.nextToken().trim()).intValue();

                for (int i = 0; i < npoints; i++) {
                    line = br.readLine();
                    StringTokenizer coord = new StringTokenizer(line);
                    this.addPoint((int) (scaleX * (Float.valueOf(coord.nextToken()).floatValue() - shiftX)),
                            (int) (scaleY * (Float.valueOf(coord.nextToken()).floatValue() - shiftY)));
                }
            }
            catch (NoSuchElementException e) {
                System.out.println("Poly Read Error: " + line);
            }
        }
        catch (IOException e) {
            System.out.println("Error: " + e);
            System.exit(1);
        }
    }
}


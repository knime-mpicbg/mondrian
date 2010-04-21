package org.rosuda.mondrian.plots.basic;


import java.awt.*;               //


public class MyText {

    private String s;
    private int x;
    public int y;
    private int align = 0;
    private double angle = 0;
    private int extend = 10000;


    public MyText(String s, int x, int y) {

        this.s = s;
        this.x = x;
        this.y = y;
    }


    public MyText(String s, int x, int y, int align) {

        this.s = s;
        this.x = x;
        this.y = y;
        this.align = align;
    }


    public MyText(String s, int x, int y, double angle, int extend) {

        this.s = s;
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.extend = extend;
    }


    public String getText() {
        return (s);
    }


    public void draw(Graphics g) {
        this.draw(g, align);
    }


    public void moveYTo(int y) {
        this.y = y;
    }


    public void moveXTo(int x) {
        this.x = x;
    }


    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public void setAlign(int align) {
        this.align = align;
    }


    public boolean contains(int px, int py, Graphics2D g2d) {

        FontMetrics FM;
        FM = g2d.getFontMetrics();

        switch (align) {
            case 0:
                if (px >= x && px <= x + FM.stringWidth(s) &&
                        py >= y - 12 && py <= y)
                    return true;
            case 1:
                if (px >= x - FM.stringWidth(s) && px <= x &&
                        py >= y - 12 && py <= y)
                    return true;
            case 2:
                if (px >= x - FM.stringWidth(s) / 2 && px <= x + FM.stringWidth(s) / 2 &&
                        py >= y - 12 && py <= y)
                    return true;
        }
        return false;
    }


    public void draw(Graphics2D g2d) {

        FontMetrics FM;
        FM = g2d.getFontMetrics();

        if (FM.stringWidth(s) >= extend) {
            String shorty = s;
            String addOn = "";
            while (FM.stringWidth(shorty) > extend) {
                shorty = shorty.substring(0, shorty.length() - 1);
                addOn = "É";
            }
            if (shorty.length() + 1 < s.length())
                s = shorty.trim() + addOn;
        }
        // Draw string rotated clockwise angle degrees
        if (angle != 0) {
            g2d.rotate(angle);
            g2d.drawString(s, x - FM.stringWidth(s) / 2, y);
            g2d.rotate(-angle);
        } else
            g2d.drawString(s, x - FM.stringWidth(s) / 2, y);
    }


    public void draw(Graphics g, int align) {

        FontMetrics FM;
        Graphics2D g2d = (Graphics2D) g;
        switch (align) {
            case 0:                                                                               // Left
                g2d.drawString(s, x, y);
                break;
            case 1:                                                                               // Right
                FM = g.getFontMetrics();
                g2d.drawString(s, x - FM.stringWidth(s), y);
                break;
            case 2:                                                                               // Center
                FM = g.getFontMetrics();
                g2d.drawString(s, x - FM.stringWidth(s) / 2, y);
        }
    }
}

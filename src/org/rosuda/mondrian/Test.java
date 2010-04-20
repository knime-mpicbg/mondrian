package org.rosuda.mondrian;

import javax.swing.*;
import java.awt.*;


public class Test extends Canvas {

    protected JFrame frame;                         // The frame we are within.


    /**
     * This constructor requires a Frame and a desired size
     */
    public Test() {
        this.setSize(400, 400);
    }


    public void paint(Graphics g) {

        g.fillRect(10, 10, 300, 300);
    }
}








package de.mpicbg.sweng.mondrian.ui;

//
//  PreferencesFrame.java
//  Mandarin
//
//  Created by Simon Urbanek on Wed Jun 11 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

import de.mpicbg.sweng.mondrian.AppFrame;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.prefs.Preferences;         // for preferences


public class PreferencesFrame extends Frame implements WindowListener, MouseListener, ActionListener, ItemListener {

    private PrefCanvas pc;
    private AppFrame frame;
    private Choice cs;

    private static PreferencesFrame last = null;


    public static PreferencesFrame showPrefsDialog(AppFrame frame) {
        if (last == null)
            last = new PreferencesFrame();
        last.frame = frame;
        last.setVisible(true);
        return last;
    }


    private PreferencesFrame() {
        super("Preferences");
        setLayout(new BorderLayout());
        add(pc = new PrefCanvas());
        pc.addMouseListener(this);
        Panel p = new Panel();
        Panel pp = new Panel();
        pp.setLayout(new BorderLayout());
        Panel ppp = new Panel();
        pp.add(p, BorderLayout.SOUTH);
        pp.add(ppp);
        ppp.setLayout(new FlowLayout());
        ppp.add(new Label("Color scheme:"));
        ppp.add(cs = new Choice());
        cs.add("Custom ...");
        int i = 0;
        while (ColorManager.defaultColSchemes[i] != null) {
            cs.add(ColorManager.defaultColSchemes[i]);
            if (ColorManager.defaultColSchemes[i + 1].compareTo(Utils.color2hrgb(pc.c[0])) == 0 &&
                    ColorManager.defaultColSchemes[i + 2].compareTo(Utils.color2hrgb(pc.c[1])) == 0 &&
                    ColorManager.defaultColSchemes[i + 3].compareTo(Utils.color2hrgb(pc.c[2])) == 0 &&
                    ColorManager.defaultColSchemes[i + 4].compareTo(Utils.color2hrgb(pc.c[3])) == 0)
                cs.select(ColorManager.defaultColSchemes[i]);
            i += 5;
        }
        cs.addItemListener(this);
        p.setLayout(new FlowLayout());
        Button b = null;
        p.add(b = new Button("Save"));
        b.addActionListener(this);
        p.add(b = new Button("Apply"));
        b.addActionListener(this);
        p.add(b = new Button("Close"));
        b.addActionListener(this);
        add(pp, BorderLayout.SOUTH);
        pack();
        addWindowListener(this);
    }


    class PrefCanvas extends Canvas {

        Color c[];


        PrefCanvas() {
            setSize(250, 160);
            c = new Color[4];
            c[0] = ColorManager.backgroundColor;
            c[1] = ColorManager.objectColor;
            c[2] = ColorManager.lineColor;
            c[3] = DragBox.hiliteColor;
        }


        public void paint(Graphics g) {
            g.setFont(new Font("SansSerif", 0, 11));
            g.drawString("background color:", 30, 35);
//      g.setColor(Color.gray);
            g.drawString("objects color:", 30, 65);
            g.drawString("text/lines color:", 30, 95);
            g.setColor(Color.black);
            g.drawString("highlighting color:", 30, 125);
            g.setColor(c[0]);
            g.fillRect(170, 20, 30, 20);
            g.setColor(c[1]);
            g.fillRect(170, 50, 30, 20);
            g.setColor(c[2]);
            g.fillRect(170, 80, 30, 20);
            g.setColor(c[3]);
            g.fillRect(170, 110, 30, 20);
            g.setColor(Color.black);
            g.drawRect(170, 20, 30, 20);
            g.drawRect(170, 50, 30, 20);
            g.drawRect(170, 80, 30, 20);
            g.drawRect(170, 110, 30, 20);
        }
    }


    public void windowClosing(WindowEvent e) {
        setVisible(false);
    }


    public void windowClosed(WindowEvent e) {
    }


    public void windowOpened(WindowEvent e) {
    }


    public void windowIconified(WindowEvent e) {
    }


    public void windowDeiconified(WindowEvent e) {
    }


    public void windowActivated(WindowEvent e) {
    }


    public void windowDeactivated(WindowEvent e) {
    }


    public void itemStateChanged(ItemEvent e) {
        String s = cs.getSelectedItem();
        int i = 0;
        while (ColorManager.defaultColSchemes[i] != null) {
            if (ColorManager.defaultColSchemes[i].equals(s)) {
                Color cl = Utils.hrgb2color(ColorManager.defaultColSchemes[++i]);
                if (cl != null) pc.c[0] = cl;
                cl = Utils.hrgb2color(ColorManager.defaultColSchemes[++i]);
                if (cl != null) pc.c[1] = cl;
                cl = Utils.hrgb2color(ColorManager.defaultColSchemes[++i]);
                if (cl != null) pc.c[2] = cl;
                cl = Utils.hrgb2color(ColorManager.defaultColSchemes[++i]);
                if (cl != null) pc.c[3] = cl;
                pc.repaint();
                return;
            }
            i += 5;
        }
    }


    public void mouseClicked(MouseEvent ev) {
        int x = ev.getX(), y = ev.getY();
        if (x > 170 && x < 200 && y > 20 && y < 130) {
            int a = (y - 15) / 30;
            Color cl = null;
            cl = JColorChooser.showDialog(this, "Choose color", pc.c[a]);
            if (cl != null) {
                cs.select("Custom ...");
                pc.c[a] = cl;
                pc.repaint();
            }
        }
    }


    public void mousePressed(MouseEvent ev) {
    }


    public void mouseReleased(MouseEvent e) {
    }


    public void mouseDragged(MouseEvent e) {
    }


    public void mouseMoved(MouseEvent ev) {
    }


    public void mouseEntered(MouseEvent e) {
    }


    public void mouseExited(MouseEvent e) {
    }


    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Close")) {
            setVisible(false);
        }
        if (cmd.equals("Apply") || cmd.equals("Save")) {
            ColorManager.backgroundColor = pc.c[0];
            ColorManager.objectColor = pc.c[1];
            ColorManager.lineColor = pc.c[2];
            DragBox.hiliteColor = pc.c[3];
            frame.getController().redrawAll();
        }
        if (cmd.equals("Save")) {
            Preferences prefs = Preferences.userNodeForPackage(this.getClass());
            prefs.put("color.background", Utils.color2hrgb(ColorManager.backgroundColor));
            prefs.put("color.objects", Utils.color2hrgb(ColorManager.objectColor));
            prefs.put("color.line", Utils.color2hrgb(ColorManager.lineColor));
            prefs.put("color.select", Utils.color2hrgb(DragBox.hiliteColor));
            setVisible(false);
        }
    }
}

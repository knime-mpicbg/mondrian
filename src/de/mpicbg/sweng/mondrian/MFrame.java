package de.mpicbg.sweng.mondrian;
//
// A closeable Frame for Mondrian.
//

import de.mpicbg.sweng.mondrian.core.DragBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Timer;
import java.util.TimerTask;


public class MFrame extends JFrame implements WindowListener {

    public MonFrame j;
    private JMenuItem m;
    private String selString = "";
    private int counter = 0;
    private boolean same = false, added = false;

    private Timer resizePlotTimer = new Timer();
    private TimerTask resizePlotTask;
    private boolean firstTime = true;

    //  static Color backgroundColor = new Color(223, 184, 96);
    public static Color backgroundColor = new Color(255, 255, 179);
    public static Color objectColor = Color.lightGray;
    public static Color lineColor = Color.black;


    public MFrame(MonFrame j) {
        if (((System.getProperty("os.name")).toLowerCase()).indexOf("mac") > -1)
            this.setJMenuBar(j.menubar);
        this.j = j;
        this.setBackground(backgroundColor);
        addWindowListener(this);
    }


    public final void initComponents(final DragBox DB) {
        resizePlotTask = new ResizePlotTask(DB);
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public final void componentResized(final java.awt.event.ComponentEvent evt) {
                lblPlotComponentResized(DB, evt);
            }
        });
    }


    public void windowClosing(WindowEvent e) {
        close();
    }


    public boolean getAlphaHi() {
        return j.alphaHi;
    }


    public boolean hasR() {
        return j.hasR;
    }


    public void close() {
        System.out.println("Window Closed!!");

        j.windows.remove(m);
        if (j.windows.getItemCount() < 3)
            j.ca.setEnabled(false);
        if (!selString.equals(""))
            j.updateSelection();
        this.setVisible(false);
        this.dispose();
    }


    public void maintainMenu(int step) {
        selString += " [" + step + "]";
        m.setText(getTitle() + selString);
    }


    public void maintainMenu(boolean preserve) {
        if (!preserve)
            selString = "";
        m.setText(getTitle() + selString);
    }


    public void show() {

        m = new JMenuItem(getTitle());
        j.ca.setEnabled(true);

        for (int i = 2; i < j.windows.getItemCount(); i++)
            if (((j.windows.getItem(i)).getText()).substring(0, 2).equals((m.getText()).substring(0, 2)))
                same = true;
            else if (same) {
                j.windows.insert(m, i);
                added = true;
                same = false;
            }

        if (!added) {
            j.windows.add(m);
            added = true;
        }

        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toFront();
            }
        });

        super.show();
    }


    public void windowClosed(WindowEvent e) {
    }


    public void windowIconified(WindowEvent e) {
    }


    public void windowOpened(WindowEvent e) {
    }


    public void windowDeiconified(WindowEvent e) {
    }


    public void windowActivated(WindowEvent e) {
        if (((System.getProperty("os.name")).toLowerCase()).indexOf("mac") > -1)
            this.setJMenuBar(j.menubar);                 // Add it to the frame.
    }


    public void windowDeactivated(WindowEvent e) {
    }


    private void lblPlotComponentResized(DragBox DB, final java.awt.event.ComponentEvent evt) {
        if (resizePlotTask != null && !firstTime) {
//      System.out.println("+++++++++++ Canceled "+resizePlotTask);
            resizePlotTask.cancel();
            System.out.println(" Cancled timer!: " + (--counter));
        }
        if (!firstTime) {
            DB.resizeReady = false;
            resizePlotTask = new ResizePlotTask(DB);
            System.out.println("+++++++++++ Created " + (++counter));
            resizePlotTimer.schedule(resizePlotTask, 200);
        }
        firstTime = false;
    }
}

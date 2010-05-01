package de.mpicbg.sweng.mondrian;
//
// A closeable Frame for Mondrian.
//

import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
import de.mpicbg.sweng.mondrian.ui.ResizePlotTask;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;


public class MDialog extends JDialog {

    public MonFrame parentFrame;
    private JMenuItem m;
    private String selString = "";
    private int counter = 0;
    private boolean same = false, added = false;

    private Timer resizePlotTimer = new Timer();
    private TimerTask resizePlotTask;
    private boolean firstTime = true;
    private Mondrian mondrian;

    //  static Color backgroundColor = new Color(223, 184, 96);


    public MDialog(MonFrame parentFrame, Mondrian mondrian) {
        this.mondrian = mondrian;
        this.parentFrame = parentFrame;

        this.getContentPane().setBackground(ColorManager.backgroundColor);
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
        return parentFrame.alphaHi;
    }


    public void close() {
        System.out.println("Window Closed!!");

        parentFrame.windowMenu.remove(m);
        if (parentFrame.windowMenu.getItemCount() < 3)
            parentFrame.closeAllMenuItem.setEnabled(false);
        if (!selString.equals(""))
            mondrian.updateSelection();
        this.setVisible(false);
        this.dispose();
    }


    public void maintainMenu(int step) {
        selString += " [" + step + "]";
        m.setText(getTitle() + selString);
    }


    public void show() {

        m = new JMenuItem(getTitle());
        parentFrame.closeAllMenuItem.setEnabled(true);

        for (int i = 2; i < parentFrame.windowMenu.getItemCount(); i++) {
            String entryName = (parentFrame.windowMenu.getItem(i)).getText();
            if (entryName.length() < 3) {
                continue;
            }

            if (entryName.substring(0, 2).equals((m.getText()).substring(0, 2))) {
                same = true;
            } else if (same) {
                parentFrame.windowMenu.insert(m, i);
                added = true;
                same = false;
            }
        }

        if (!added) {
            parentFrame.windowMenu.add(m);
            added = true;
        }

        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toFront();
            }
        });

        super.show();
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
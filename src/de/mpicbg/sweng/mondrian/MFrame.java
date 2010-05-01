package de.mpicbg.sweng.mondrian;
//
// A closeable Frame for Mondrian.
//

import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
import de.mpicbg.sweng.mondrian.ui.ResizePlotTask;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;


public class MFrame extends JFrame {

    public AppFrame parentFrame;
    private JMenuItem m;
    private String selString = "";
    private int counter = 0;
    private boolean same = false, added = false;

    private Timer resizePlotTimer = new Timer();
    private TimerTask resizePlotTask;
    private boolean firstTime = true;
    private Mondrian mondrian;

    //  static Color backgroundColor = new Color(223, 184, 96);


    public MFrame(final AppFrame parentFrame, final Mondrian mondrian) {
        refreshMenuBar(parentFrame);

        this.mondrian = mondrian;
        this.parentFrame = parentFrame;

        this.getContentPane().setBackground(ColorManager.backgroundColor);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                close();
            }


            @Override
            public void windowActivated(WindowEvent windowEvent) {
                parentFrame.getController().setCurrent(mondrian);

                refreshMenuBar(MFrame.this.parentFrame);
            }

        });

    }


    private void refreshMenuBar(AppFrame parentFrame) {
        if (Utils.isMacOS() && Utils.isDeployed())
            this.setJMenuBar(parentFrame.menubar);
    }


    public final void initComponents(final DragBox DB) {
        resizePlotTask = new ResizePlotTask(DB);
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            public final void componentResized(final java.awt.event.ComponentEvent evt) {
                lblPlotComponentResized(DB, evt);
            }
        });
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

            //originial
//            if (entryName.substring(0, 2).equals((m.getText()).substring(0, 2))) {
            if (entryName.substring(0, 2).equals((m.getText()).substring(0, 1))) {
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
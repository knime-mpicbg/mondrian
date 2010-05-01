package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.io.ProgressIndicator;

import javax.swing.*;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ProgIndicatorImpl extends JDialog implements ProgressIndicator {

    JProgressBar progBar = new JProgressBar();


    public ProgIndicatorImpl() {
        setSize(300, 300);

        setLayout(new BorderLayout());

        progBar.setMinimum(0);
        progBar.setMaximum(100);
        add(progBar, BorderLayout.CENTER);

        setModal(true);
        setVisible(true);
    }


    public void setProgress(double progress) {
    }


    public JProgressBar getProgBar() {
        return null;
    }


    public void setProgText(String msg) {
    }
}

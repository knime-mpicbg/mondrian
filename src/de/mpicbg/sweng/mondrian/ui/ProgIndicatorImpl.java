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
        setSize(200, 30);

        setLayout(new BorderLayout());

        progBar.setMinimum(0);
        progBar.setMaximum(100);
        add(progBar, BorderLayout.CENTER);

        setLocationRelativeTo(null);

        setVisible(true);
    }


    public void setProgress(double progress) {
    }


    public JProgressBar getProgBar() {
        return progBar;
    }


    public void setProgText(String msg) {
        setTitle(msg);
//        progBar.setString(msg);
//        progBar.setStringPainted(true);
    }
}

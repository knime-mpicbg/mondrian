package de.mpicbg.sweng.mondrian.io;

import javax.swing.*;


public interface ProgressIndicator {

    void setProgress(double progress);

    JProgressBar getProgBar();

    void setProgText(String msg);
}

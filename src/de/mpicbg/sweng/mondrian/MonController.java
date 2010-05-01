package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.io.AsciiFileLoader;
import de.mpicbg.sweng.mondrian.ui.ProgIndicatorImpl;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MonController {

    MonFrame monFrame;


    List<Mondrian> mondrians = new ArrayList<Mondrian>();


    public MonController(MonFrame monFrame) {
        this.monFrame = monFrame;
    }


    public MonFrame getMonFrame() {
        return monFrame;
    }


    public Mondrian getCurrent() {
        return null;
    }


    public DataSet getCurrentDataSet() {
        return getCurrent().getDataSet();
        // before dataSets.elementAt(monFrame.dataSetCounter)
    }


    public int getNumInstances() {
        return mondrians.size();
    }


    public void removeInstance(Mondrian mondrian) {
        mondrians.remove(mondrian);
    }


    public void addAndActiviate(Mondrian mondrian) {
        mondrians.add(mondrian);
        monFrame.maintainOptionMenu();
    }


    public void loadDataSet(File file, String dsName) {

        ProgIndicatorImpl progIndicator = new ProgIndicatorImpl();
        DataSet dataSet = new AsciiFileLoader(progIndicator, getMonFrame()).loadAsciiFile(file);

        progIndicator.dispose();

        if (dataSet != null) {
            if (dsName != null) {
                dataSet.setName = dsName;
            }

            Mondrian mondrian = new Mondrian(dataSet, this);

            addAndActiviate(mondrian);

        }
    }


    public int countInstances() {
        return mondrians.size();
    }


    public void closeAll() {
        String message = "Are you sure that you would like to close all current data-sets";

        int answer = JOptionPane.showConfirmDialog(monFrame, message);
        if (answer == JOptionPane.YES_OPTION) {
            closeAll();
        }
    }


    /**
     * Close an instance.  If this is the last open window, just quit.
     */
    void close(Mondrian mondrian) {

        String message = "Close dataset \"" + getCurrentDataSet().setName + "\" and\n all corresponding plots?";

        int answer = JOptionPane.showConfirmDialog(monFrame, message);
        if (answer == JOptionPane.YES_OPTION) {
            mondrian.close();
        }
    }


    public void redrawAll() {
        for (Mondrian mondrian : mondrians) {
            for (DragBox dragBox : mondrian.getPlots()) {
                dragBox.frame.repaint();
            }
        }
    }
}

package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.io.AsciiFileLoader;
import de.mpicbg.sweng.mondrian.ui.ProgIndicatorImpl;
import de.mpicbg.sweng.mondrian.util.Utils;

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

    AppFrame appFrame;

    private Mondrian current;

    List<Mondrian> mondrians = new ArrayList<Mondrian>();


    public MonController(AppFrame appFrame) {
        this.appFrame = appFrame;
    }


    public AppFrame getMonFrame() {
        return appFrame;
    }


    public Mondrian getCurrent() {
        return current;
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
        setCurrent(mondrian);

        if (Utils.isMacOS() && Utils.isDeployed()) {
            appFrame.setVisible(false);
        }
    }


    public void setCurrent(Mondrian mondrian) {
        current = mondrian;
        appFrame.updateMenusToSelection();
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
            mondrian.getDialog().setVisible(true);
        }
    }


    public int countInstances() {
        return mondrians.size();
    }


    public void closeAll() {
        if (countInstances() > 0) {
            String message = "Are you sure that you would like to close all current data-sets";

            int answer = JOptionPane.showConfirmDialog(appFrame, message);
            if (answer == JOptionPane.YES_OPTION) {
                for (Mondrian mondrian : mondrians) {
                    close(mondrian, false);
                }
            }
        }
    }


    /**
     * Close an instance.  If this is the last open window, just quit.
     */
    public void close(Mondrian mondrian, boolean askForConfirm) {

        String message = "Close dataset \"" + getCurrentDataSet().setName + "\" and\n all corresponding plots?";

        int answer = askForConfirm ? JOptionPane.showConfirmDialog(mondrian.getDialog(), message) : JOptionPane.YES_OPTION;
        if (answer == JOptionPane.YES_OPTION) {
            mondrian.close();
        }

        mondrians.remove(mondrian);

        if (getNumInstances() == 0) {
//            System.exit(0);
            if (Utils.isMacOS() && Utils.isDeployed()) {
                appFrame.setVisible(true);
            }
        } else {
            setCurrent(mondrians.get(0));
        }
    }


    public void redrawAll() {
        for (Mondrian mondrian : mondrians) {
            for (DragBox dragBox : mondrian.getPlots()) {
                dragBox.frame.repaint();
            }
        }
    }


    public void clearColors() {
        if (countInstances() > 0) {
            getCurrentDataSet().colorsOff();
            getCurrent().dataChanged(-1);
        }
    }


    public void toggleSelection() {
        if (countInstances() > 0) {
            getCurrentDataSet().toggleSelection();
            getCurrent().updateSelection();
        }
    }


    public void selectAll() {
        if (countInstances() > -1) {
            getCurrentDataSet().selectAll();
            getCurrent().updateSelection();
        }
    }


    public void fireVarSelectionChanged() {
        appFrame.updateMenusToSelection();
    }
}

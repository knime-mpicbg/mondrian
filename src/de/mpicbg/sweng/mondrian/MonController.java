package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.io.AsciiFileLoader;
import de.mpicbg.sweng.mondrian.ui.ProgIndicatorImpl;
import de.mpicbg.sweng.mondrian.util.StatUtil;

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

        monFrame.closeDataSetMenuItem.setEnabled(true);
        monFrame.saveMenuItem.setEnabled(true);
        monFrame.maintainOptionMenu();

        // todo make the new mondrian the current one
    }


    public void loadDataSet(File file, String dataFrameName) {

        ProgIndicatorImpl progIndicator = new ProgIndicatorImpl();
        DataSet dataSet = new AsciiFileLoader(progIndicator, getMonFrame()).loadAsciiFile(file);
        progIndicator.dispose();

        if (dataSet != null) {
            Mondrian mondrian = new Mondrian(dataSet, this);

            addAndActiviate(mondrian);

        }
    }


    /**
     * adds a dataset and makes is current.
     *
     * @param data     dataset to use
     * @param monFrame
     */
    public void initWithData(DataSet data, MonFrame monFrame) {
        Mondrian mondrian = new Mondrian(data, this);
        addAndActiviate(mondrian);

        monFrame.dataSetCounter = dataSets.size() - 1;
        monFrame.setVarList();
        monFrame.setTitle("Mondrian(" + getCurrentDataSet().setName + ")");               //
        monFrame.me.setText(monFrame.getTitle());
        monFrame.closeDataSetMenuItem.setEnabled(true);
        monFrame.saveMenuItem.setEnabled(true);

        int nom = getCurrentDataSet().countSelection();
        int denom = getCurrentDataSet().n;
        String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100 * nom / denom, 2) + "%)";
        progText.setText(Display);
        progBar.setValue(nom);

        monFrame.maintainOptionMenu();
    }
}

package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.PlotFactory;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MosaicPlotFactory implements PlotFactory {

    public String getPlotName() {
        return "New Mosaic";
    }


    public String getShortDescription() {
        return "A mosaic plot";
    }


    public JPanel createPlotPanel(DataSet dataSet, int[] selectedVarIndices) {
        return null;
    }


    public boolean isCompliant(int numVariables, int numCategoricalVariables) {
        return false;
    }
}

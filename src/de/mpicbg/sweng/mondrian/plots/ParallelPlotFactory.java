package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ParallelPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Parallel Plot";
    }


    public String getShortDescription() {
        return null;
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MDialog plotDialog, DataSet dataSet, JList varNames) {
        int k = (varNames.getSelectedIndices()).length;
        int[] passTmpBuffer = new int[k];
        int count = 0;

        for (int i = 0; i < k; i++) {
            if (dataSet.getNumMissings(mondrian.getSelector().selectBuffer[k - i - 1]) < dataSet.n)  // make sure not all data is missing
                passTmpBuffer[count++] = mondrian.getSelector().selectBuffer[k - i - 1];
        }
        int[] passBuffer = new int[count];
        System.arraycopy(passTmpBuffer, 0, passBuffer, 0, count);

        return new ParallelPlot(plotDialog, dataSet, passBuffer, getMode(), varNames);
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables > 1;
    }


    public String getMode() {
        return "Poly";
    }
}

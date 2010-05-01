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
public class ScatterplotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Scatterplot";
    }


    public String getShortDescription() {
        return "TODO"; //todo
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MDialog plotDialog, DataSet dataSet, JList varNames) {
        int[] passBuffer = new int[2];
        passBuffer[0] = mondrian.getSelector().selectBuffer[1];
        passBuffer[1] = mondrian.getSelector().selectBuffer[0];

        return new Scatter2DPlot(plotDialog, 400, 400, dataSet, passBuffer, varNames, false);
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == 2;
    }
}

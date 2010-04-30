package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.Table;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class HistogramFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Histogram";
    }


    public String getShortDescription() {
        return null;
    }


    public PlotPanel createPlotPanel(MonFrame monFrame, MDialog plotDialog, DataSet dataSet, JList varNames) {

        int[] indices = varNames.getSelectedIndices();
        int weight = 0;

        return createHistogram(plotDialog, dataSet, indices, weight);
    }


    protected PlotPanel createHistogram(MDialog plotFrame, DataSet dataSet, int[] indices, int weight) {
        PlotPanel plotPanel = new PlotPanel();
        plotPanel.setLayout(new GridLayout(1, indices.length));


        for (int i = 0; i < indices.length; i++) {

            int dummy = indices[i];
            double start = dataSet.getMin(dummy);
            double width = (dataSet.getMax(dummy) - dataSet.getMin(dummy)) / 8.9;

            Table discrete = dataSet.discretize(dataSet.setName, dummy, start, width, weight);

            Histogram histogram = new Histogram(plotFrame, 250, 310, discrete, start, width, weight);
            plotPanel.add(histogram);
        }

        return plotPanel;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numCategoricalVariables == 0;
    }
}

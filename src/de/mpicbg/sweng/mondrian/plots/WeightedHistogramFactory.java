package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class WeightedHistogramFactory extends HistogramFactory {

    @Override
    public String getPlotName() {
        return "Weighted Histogram";
    }


    @Override
    public PlotPanel createPlotPanel(MonFrame monFrame, MDialog plotDialog, DataSet dataSet, JList varNames) {

        int[] vars = monFrame.getWeightVariable(varNames.getSelectedIndices(), dataSet);

        if (vars.length > 1) {
            int[] passed = new int[vars.length - 1];
            System.arraycopy(vars, 0, passed, 0, vars.length - 1);
            int weight = vars[vars.length - 1];

            //      System.out.println(passed[0]+", "+weight);

            return super.createHistogram(plotDialog, dataSet, passed, weight);

        } else {
            return super.createHistogram(plotDialog, dataSet, vars, vars[0]);
        }
    }
}

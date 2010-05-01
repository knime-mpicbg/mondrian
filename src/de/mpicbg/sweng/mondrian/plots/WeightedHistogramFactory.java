package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;
import de.mpicbg.sweng.mondrian.util.WeightCaclulator;

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
    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {

        int[] vars = WeightCaclulator.getWeightVariable(varNames.getSelectedIndices(), dataSet, mondrian.calcNumCategoricalVars(), mondrian.determineWeightIndex(), null, varNames);

        if (vars.length > 1) {
            int[] passed = new int[vars.length - 1];
            System.arraycopy(vars, 0, passed, 0, vars.length - 1);
            int weight = vars[vars.length - 1];

            //      System.out.println(passed[0]+", "+weight);

            return super.createHistogram(plotFrame, mondrian, dataSet, passed, weight);

        } else {
            return super.createHistogram(plotFrame, mondrian, dataSet, vars, vars[0]);
        }
    }
}

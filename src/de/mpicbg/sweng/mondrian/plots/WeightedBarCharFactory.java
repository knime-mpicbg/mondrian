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
public class WeightedBarCharFactory extends BarchartFactory {

    public String getPlotName() {
        return "Weighted Barchart";
    }


    public String getShortDescription() {
        return null; // todo
    }


    @Override
    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {


        int[] vars = WeightCaclulator.getWeightVariable(varNames.getSelectedIndices(), dataSet, mondrian.calcNumCategoricalVars(), mondrian.determineWeightIndex(), null, varNames);

        if (vars.length > 1) {
            int[] passed = new int[vars.length - 1];
            System.arraycopy(vars, 0, passed, 0, vars.length - 1);
            int weight = vars[vars.length - 1];

            return super.createBarChart(mondrian, plotFrame, dataSet, passed, weight);

        } else {
            return super.createBarChart(mondrian, plotFrame, dataSet, vars, vars[0]);
        }
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == 1 && numVariables == numCategoricalVariables;
    }
}

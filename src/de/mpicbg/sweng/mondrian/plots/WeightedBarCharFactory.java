package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.Table;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;
import de.mpicbg.sweng.mondrian.util.WeightCaclulator;

import javax.swing.*;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
// todo make this to become a subclass of barchartfactory
public class WeightedBarCharFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Weighted Barchart";
    }


    public String getShortDescription() {
        return null; // todo
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {
        int[] indices = varNames.getSelectedIndices();


        PlotPanel barChartsContainer = new PlotPanel();
        plotFrame.setLayout(new GridLayout(1, indices.length));

        int[] vars = WeightCaclulator.getWeightVariable(varNames.getSelectedIndices(), dataSet, mondrian.calcNumCategoricalVars(), mondrian.determineWeightIndex(), null, varNames);
        int[] passed = new int[vars.length - 1];
        System.arraycopy(vars, 0, passed, 0, vars.length - 1);

        int weight = vars[vars.length - 1];

        for (int i = 0; i < passed.length; i++) {

            int[] dummy = {0};
            dummy[0] = passed[i];

            Table breakdown = dataSet.breakDown(dataSet.setName, dummy, weight);

            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
            int tmpHeight = Math.min(totHeight - 20, 60 + breakdown.levels[0] * 30);

            Barchart weightedBarchart = new Barchart(plotFrame, 300, tmpHeight, breakdown);

            barChartsContainer.setSize(300, tmpHeight);
            barChartsContainer.add(weightedBarchart);
        }

        plotFrame.setLocation(100, 100);

        return barChartsContainer;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == 1 && numVariables == numCategoricalVariables;
    }
}

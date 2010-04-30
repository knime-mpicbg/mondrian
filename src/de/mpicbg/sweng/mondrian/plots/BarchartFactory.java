package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
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
public class BarchartFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Barchart";
    }


    public String getShortDescription() {
        return null; // todo
    }


    public PlotPanel createPlotPanel(MonFrame monFrame, MFrame plotFrame, DataSet dataSet, JList varNames) {
        int[] indices = varNames.getSelectedIndices();

        PlotPanel barChartsContainer = new PlotPanel();
        plotFrame.setLayout(new GridLayout(1, indices.length));


        int weight = -1;

        for (int i = 0; i < indices.length; i++) {

            int[] dummy = {0};
            dummy[0] = indices[i];

            Table breakdown = dataSet.breakDown(dataSet.setName, dummy, weight);

            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
            int tmpHeight = Math.min(totHeight - 30, 60 + breakdown.levels[0] * 30);

            Barchart barchart = new Barchart(plotFrame, 300, tmpHeight, breakdown);

            barChartsContainer.add(barchart);
        }

        return barChartsContainer;
    }


    public boolean isCompliant(int numVariables, int numCategoricalVariables) {
        return numVariables == numCategoricalVariables;
    }
}

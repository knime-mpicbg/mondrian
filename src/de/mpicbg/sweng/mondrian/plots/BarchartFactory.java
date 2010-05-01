package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
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


    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {
        int[] indices = varNames.getSelectedIndices();

        PlotPanel barChartsContainer = new PlotPanel();

        int numPlots = indices.length;
        barChartsContainer.setLayout(new GridLayout(1, numPlots));

        int weight = -1;

        Table[] breakdowns = new Table[numPlots];
        int maxLevels = -1;
        for (int i = 0; i < numPlots; i++) {

            int[] dummy = {0};
            dummy[0] = indices[i];

            Table breakdown = dataSet.breakDown(dataSet.setName, dummy, weight);

            maxLevels = Math.max(maxLevels, breakdown.levels[0]);

            breakdowns[i] = breakdown;
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = Math.min(screenSize.height - 30, 60 + maxLevels * 30);
        int frameWidth = Math.min(screenSize.width - 50, 300 * numPlots);
        int plotWidth = frameWidth / numPlots;

        plotFrame.setSize(frameWidth - plotFrame.getInsets().left - plotFrame.getInsets().right, height);

        for (int i = 0; i < numPlots; i++) {
            Barchart barchart = new Barchart(plotFrame, plotWidth, height, breakdowns[i]);

            barChartsContainer.add(barchart);

            if (barChartsContainer.getName() != null)
                barChartsContainer.setName(barChartsContainer.getName() + " | " + barchart.getName());
            else
                barChartsContainer.setName(barchart.getName());
        }

        return barChartsContainer;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == numCategoricalVariables;
    }
}

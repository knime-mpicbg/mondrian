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
        int weight = -1;


        return createBarChart(mondrian, plotFrame, dataSet, indices, weight);
    }


    public PlotPanel createBarChart(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, int[] indices, int weight) {

        int lastY = 333;
        int col = 0;

        for (int i = 0; i < indices.length; i++) {
            MFrame mFrame = new MFrame(plotFrame.parentFrame, mondrian);

            int[] dummy = {0};
            dummy[0] = indices[i];

            Table breakdown = dataSet.breakDown(dataSet.setName, dummy, weight);

            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
            int tmpHeight = Math.min(totHeight - 30, 60 + breakdown.levels[0] * 30);

            mFrame.setSize(300, tmpHeight);
            final Barchart barchart = new Barchart(mFrame, 300, tmpHeight, breakdown);

            if (lastY + mFrame.getHeight() > (Toolkit.getDefaultToolkit().getScreenSize()).height) {
                col += 1;
                lastY = 0;
            }
            if (300 * col > (Toolkit.getDefaultToolkit().getScreenSize()).width - 50) {
                col = 0;
                lastY = 353;
            }
            mFrame.setLocation(300 * col, lastY);

            if (lastY == 0)
                lastY += mFrame.getY();
            lastY += mFrame.getHeight();


            makeVisible(mondrian, mFrame, barchart);
        }


        return null;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == numCategoricalVariables;
    }
}

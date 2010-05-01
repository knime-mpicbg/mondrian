package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.ColorManager;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class SplomFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Scatterplot Matrix";
    }


    public String getShortDescription() {
        return "A scatter plot matrix";
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MDialog plotDialog, DataSet dataSet, JList varNames) {
        int numVars = (varNames.getSelectedIndices()).length;

        PlotPanel splomPanel = new PlotPanel();
        splomPanel.setLayout(new GridLayout(numVars - 1, numVars - 1));
        splomPanel.setMinimumSize(new Dimension(200 * numVars, 200 * numVars));

        int dims = Math.min(200 * numVars, (Toolkit.getDefaultToolkit().getScreenSize()).height);
        plotDialog.setSize(dims - 20, dims);

        for (int i = 0; i < (numVars - 1); i++)
            for (int j = 1; j < numVars; j++) {
                if (i >= j) {
                    JPanel Filler = new JPanel();
                    Filler.setBackground(ColorManager.backgroundColor);
                    splomPanel.add(Filler);
                    //          (Filler.getGraphics()).drawString("text",10,10);
                } else {
                    int[] tmpVars = new int[2];
                    //          tmpVars[0] = varNames.getSelectedIndices()[j];
                    //          tmpVars[1] = varNames.getSelectedIndices()[i];
                    tmpVars[0] = mondrian.getSelector().selectBuffer[numVars - j - 1];
                    tmpVars[1] = mondrian.getSelector().selectBuffer[numVars - i - 1];
                    //
                    Scatter2DPlot scat = new Scatter2DPlot(plotDialog, 200, 200, dataSet, tmpVars, varNames, true);
                    splomPanel.add(scat);
                }
            }

        return splomPanel;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables > 1;
    }
}

package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class SpomFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Scatterplot Matrix";
    }


    public String getShortDescription() {
        return "A scatter plot matrix";
    }


    public JPanel createPlotPanel(MonFrame monFrame, MFrame plotFrame, DataSet dataSet, int[] selectedVarIndices) {
        int p = (selectedVarIndices).length;

        JPanel splomPanel = new JPanel();
        splomPanel.setLayout(new GridLayout(p - 1, p - 1));
        splomPanel.setMinimumSize(new Dimension(200 * p, 200 * p));

        for (int i = 0; i < (p - 1); i++)
            for (int j = 1; j < p; j++) {
                if (i >= j) {
                    JPanel Filler = new JPanel();
                    Filler.setBackground(MFrame.backgroundColor);
                    splomPanel.add(Filler);
                    //          (Filler.getGraphics()).drawString("text",10,10);
                } else {
                    int[] tmpVars = new int[2];
                    //          tmpVars[0] = varNames.getSelectedIndices()[j];
                    //          tmpVars[1] = varNames.getSelectedIndices()[i];
                    tmpVars[0] = monFrame.selectBuffer[p - j - 1];
                    tmpVars[1] = monFrame.selectBuffer[p - i - 1];
                    //
                    Scatter2DPlot scat = new Scatter2DPlot(plotFrame, 200, 200, dataSet, tmpVars, monFrame.varNames, true);
                    scat.addSelectionListener(monFrame);
                    scat.addDataListener(monFrame);
                    splomPanel.add(scat);

                    monFrame.plots.addElement(scat);
                }
            }

        return splomPanel;
    }


    public boolean isCompliant(int numVariables, int numCategoricalVariables) {
        return numVariables > 0;
    }
}

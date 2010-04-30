package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MissPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Missing Value Plot";
    }


    public String getShortDescription() {
        return null;
    }


    public PlotPanel createPlotPanel(MonFrame monFrame, MDialog plotDialog, DataSet dataSet, JList varNames) {
        int k = 0;
        for (int i = 0; i < (varNames.getSelectedIndices()).length; i++)
            if (dataSet.n > dataSet.getN((varNames.getSelectedIndices())[i]))
                k++;
        int[] passVars = new int[k];
        int kk = 0;
        for (int i = 0; i < (varNames.getSelectedIndices()).length; i++) {
            if (dataSet.n > dataSet.getN(monFrame.selectBuffer[i]))
                passVars[k - 1 - kk++] = monFrame.selectBuffer[i]; //(varNames.getSelectedIndices())[i];
        }

        if (k > 0) {
//            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
//            int tmpHeight = 35 * (1 + k) + 15;
//            if (tmpHeight > totHeight)
//                if (20 * (1 + k) < totHeight)
//                    tmpHeight = totHeight;
//                else
//                    tmpHeight = 20 * (1 + k);


            return new MissPlot(plotDialog, dataSet, passVars);


        } else
            JOptionPane.showMessageDialog(plotDialog, "Non of the selected variables\ninclude any missing values");

        return null;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        // todo addtionally we should check that
//        if (!dataSet.hasMissings)
//            missingValuePlotMenuItem.setEnabled(false);

        return true;
    }
}

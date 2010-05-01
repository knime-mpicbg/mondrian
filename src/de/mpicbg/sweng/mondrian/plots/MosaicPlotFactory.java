package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.Table;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MosaicPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Mosaic Plot";
    }


    public String getShortDescription() {
        return "A mosaic plot";
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {

        int k = (varNames.getSelectedIndices()).length;
        int[] passBuffer = new int[k];
        for (int i = 0; i < k; i++)
            passBuffer[i] = mondrian.getSelector().selectBuffer[k - i - 1];

        Table breakdown = dataSet.breakDown(dataSet.setName, passBuffer, -1);
        for (int i = 0; i < (varNames.getSelectedIndices()).length - 1; i++) {
            breakdown.addInteraction(new int[]{i}, false);
        }
        breakdown.addInteraction(new int[]{(varNames.getSelectedIndices()).length - 1}, true);


        plotFrame.setSize(400, 400);
        plotFrame.setLocation(Utils.genRandomLoacation(plotFrame));
        //    mondrian.getContentPane().add(plotw);                      // Add it

        //todo renable this
//        mondrian.addWindowListener(new WindowAdapter() {
//            public void windowActivated(WindowEvent e) {
//                plotw.processWindowEvent(e);
//            }
//        });

        //todo renenable this
//        if (modelNavigator == null)
//            modelNavigator = new ModelNavigator();
//        plotw.addModelListener(modelNavigator);
//        modelNavigatorButton.setEnabled(true);

        return new MosaicPlot(plotFrame, 400, 400, breakdown);
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables >= 2 && numVariables == numCategoricalVariables;
    }
}

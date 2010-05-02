package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ParallelPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Parallel Plot";
    }


    public String getDescription() {
        return null;
    }


    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F7, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {

        int totWidth = (Toolkit.getDefaultToolkit().getScreenSize()).width;
        int tmpWidth = 50 * (1 + (varNames.getSelectedIndices()).length);
        if (tmpWidth > totWidth)
            if (20 * (1 + (varNames.getSelectedIndices()).length) < totWidth)
                tmpWidth = totWidth;
            else
                tmpWidth = 20 * (1 + (varNames.getSelectedIndices()).length);

        plotFrame.setSize(tmpWidth, 400);
        plotFrame.setLocation(Utils.genRandomLoacation(plotFrame));

        int k = (varNames.getSelectedIndices()).length;
        int[] passTmpBuffer = new int[k];
        int count = 0;

        for (int i = 0; i < k; i++) {
            if (dataSet.getNumMissings(mondrian.getSelector().selectBuffer[k - i - 1]) < dataSet.n)  // make sure not all data is missing
                passTmpBuffer[count++] = mondrian.getSelector().selectBuffer[k - i - 1];
        }
        int[] passBuffer = new int[count];
        System.arraycopy(passTmpBuffer, 0, passBuffer, 0, count);

        return new ParallelPlot(plotFrame, dataSet, passBuffer, getMode(), varNames);
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables > 1;
    }


    public String getMode() {
        return "Poly";
    }
}

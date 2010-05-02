package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ScatterplotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Scatterplot";
    }


    public String getDescription() {
        return "TODO"; //todo
    }


    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F3, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {
        int[] passBuffer = new int[2];
        passBuffer[0] = mondrian.getSelector().selectBuffer[1];
        passBuffer[1] = mondrian.getSelector().selectBuffer[0];

        plotFrame.setSize(400, 400);
        plotFrame.setLocation(300, 300);
        return new Scatter2DPlot(plotFrame, 400, 400, dataSet, passBuffer, varNames, false);
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == 2;
    }
}

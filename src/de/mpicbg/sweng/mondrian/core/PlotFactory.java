package de.mpicbg.sweng.mondrian.core;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public interface PlotFactory {

    String getPlotName();

    String getShortDescription();

    PlotPanel createPlotPanel(MonFrame monFrame, MFrame plotFrame, DataSet dataSet, JList varNames);

    /**
     * Is called whenever the variable-selection changes in order to determine the enabled-state of the different plots
     * in the menu.
     */
    boolean isCompliant(int numVariables, int numCategoricalVariables);
}

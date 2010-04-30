package de.mpicbg.sweng.mondrian.core;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public interface PlotFactory {

    String getPlotName();

    String getShortDescription();

    JPanel createPlotPanel(DataSet dataSet, int[] selectedVarIndices);

    /**
     * Is called whenever the variable-selection changes in order to determine the enabled-state of the different plots
     * in the menu.
     */
    boolean isCompliant(int numVariables, int numCategoricalVariables);
}

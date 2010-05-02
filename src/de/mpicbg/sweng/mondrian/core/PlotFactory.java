package de.mpicbg.sweng.mondrian.core;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public interface PlotFactory {

    String getPlotName();

    String getDescription();

    PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames);

    /**
     * Is called whenever the variable-selection changes in order to determine the enabled-state of the different plots
     * in the menu.
     */
    boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables);

    /**
     * Unles a factory creates several plot windows (like histogram or barchart) this method does NOT neeed to be
     * invoked by the factory class.
     */
    void makeVisible(Mondrian mondrian, MFrame plotFrame, PlotPanel plotPanel);


    /**
     * Returns either <code>null</code> or an accelerator for the plot. As there is no validation about the uniqueness
     * of it, PlotFactory-implementation should select their accelerator carefully.
     */
    KeyStroke getAccelerator();
}

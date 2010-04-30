package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MosaicPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "New Mosaic";
    }


    public String getShortDescription() {
        return "A mosaic plot";
    }


    public JPanel createPlotPanel(MonFrame monFrame, MFrame plotFrame, DataSet dataSet, int[] selectedVarIndices) {
        return null;
    }


    public boolean isCompliant(int numVariables, int numCategoricalVariables) {
        return false;
    }
}

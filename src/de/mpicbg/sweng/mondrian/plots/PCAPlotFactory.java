package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.util.r.RService;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class PCAPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "New PCA"; // todo remove the (debug) "new"
    }


    public String getShortDescription() {
        return "A pca plot";
    }


    public JPanel createPlotPanel() {
        return null;
    }


    public boolean isCompliant(int numVariables, int numCategoricalVariables) {
        return numVariables - numCategoricalVariables > 1 && RService.hasR();
    }
}

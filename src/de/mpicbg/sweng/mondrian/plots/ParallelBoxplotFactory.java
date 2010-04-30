package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.core.DataSet;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ParallelBoxplotFactory extends ParallelPlotFactory {

    @Override
    public String getPlotName() {
        return "Parallel Boxplot";
    }


    @Override
    public String getShortDescription() {
        return null;
    }


    @Override
    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {

        switch (numVariables) {
            case 1:
                return !(numCategoricalVariables == numVariables);
            case 2:
                return !(numCategoricalVariables == 1);
            default:
                return true;
        }
    }


    @Override
    public String getMode() {
        return "Box";
    }
}

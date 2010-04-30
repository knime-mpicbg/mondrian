package de.mpicbg.sweng.mondrian.plots;

/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class BoxplotByXYFactory extends ParallelBoxplotFactory {

    public String getPlotName() {
        return "Boxplot y by x";
    }


    public String getShortDescription() {
        return null;
    }


    public boolean isCompliant(int numVariables, int numCategoricalVariables) {
        return numVariables == 2 && numCategoricalVariables == 1;
    }
}

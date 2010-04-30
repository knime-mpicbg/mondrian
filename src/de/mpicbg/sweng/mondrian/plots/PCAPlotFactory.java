package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;
import de.mpicbg.sweng.mondrian.util.r.RService;
import org.rosuda.REngine.Rserve.RConnection;

import javax.swing.*;


/**
 * Does not actually create a new panel but cacluates a pca of the data and adds the coefficients as variables
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


    public PlotPanel createPlotPanel(MonFrame monFrame, MDialog plotDialog, DataSet dataSet, JList varNames) {
        int[] selectedIndices = varNames.getSelectedIndices();
        try {
            RConnection c = new RConnection();
            String call = " ~ x1 ";
            for (int i = 0; i < selectedIndices.length; i++) {
                c.assign("x", dataSet.getRawNumbers(selectedIndices[i]));
                if (dataSet.n > dataSet.getN(selectedIndices[i])) {                      // Check for missings in this variable
                    boolean[] missy = dataSet.getMissings(selectedIndices[i]);
                    int[] flag = new int[dataSet.n];
                    for (int j = 0; j < dataSet.n; j++)
                        if (missy[j])
                            flag[j] = 1;
                        else
                            flag[j] = 0;
                    c.assign("xM", flag);
                    c.voidEval("is.na(x)[xM==1] <- T");
                }
                if (i == 0)
                    c.voidEval("tempData <- x");
                else {
                    c.voidEval("tempData <- cbind(tempData, x)");
                    call += " + x" + (i + 1) + "";
                }
            }
            c.voidEval("tempData <- data.frame(tempData)");

            for (int i = 0; i < selectedIndices.length; i++)
                c.voidEval("names(tempData)[" + (i + 1) + "] <- \"x" + (i + 1) + "\"");

            String opt = "TRUE";
            int answer = JOptionPane.showConfirmDialog(null, "Calculate PCA for correlation matrix", "Standardize Data?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION)
                opt = "FALSE";

            c.voidEval("pca <- predict(princomp(" + call + " , data = tempData, cor = " + opt + ", na.action = na.exclude))");
            for (int i = 0; i < selectedIndices.length; i++) {
                double[] x = c.eval("pca[," + (i + 1) + "]").asDoubles();
                boolean missy[] = new boolean[dataSet.n];
                for (int j = 0; j < x.length; j++) {
                    if (Double.isNaN(x[j])) {
                        missy[j] = true;
                        x[j] = Double.MAX_VALUE;
                    } else
                        missy[j] = false;
                }
                dataSet.addVariable("pca " + (i + 1) + "", false, false, x, missy);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Calculation of PCA failed", e);
        }

        return null;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables - numCategoricalVariables > 1 && RService.hasR();
    }
}

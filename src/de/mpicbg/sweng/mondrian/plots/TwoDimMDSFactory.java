package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;
import de.mpicbg.sweng.mondrian.util.r.RService;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class TwoDimMDSFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "2-dim MDS";
    }


    public String getShortDescription() {
        return null; // todo
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MDialog plotDialog, DataSet dataSet, JList varNames) {
        int[] varsT = varNames.getSelectedIndices();

        try {
            RConnection c = new RConnection();
            c.voidEval("library(MASS, pos=1)");
            for (int i = 0; i < varsT.length; i++) {
                c.assign("x", dataSet.getRawNumbers(varsT[i]));
                if (dataSet.n > dataSet.getN(varsT[i])) {                      // Check for missings in this variable
                    boolean[] missy = dataSet.getMissings(varsT[i]);
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
                else
                    c.voidEval("tempData <- cbind(tempData, x)");
            }
            c.voidEval("tempD <- dist(scale(tempData))");
            c.voidEval("is.na(tempD)[tempD==0] <- T");
            c.voidEval("startConf <- cmdscale(dist(scale(tempData)), k=2)");
            c.voidEval("sMds <- sammon(tempD, y=startConf, k=2, trace=F)");
            double[] x1 = c.eval("sMds$points[,1]").asDoubles();
            double[] x2 = c.eval("sMds$points[,2]").asDoubles();

            dataSet.addVariable("mds1", false, false, x1, new boolean[dataSet.n]);
            dataSet.addVariable("mds2", false, false, x2, new boolean[dataSet.n]);

            Scatter2DPlot scatterPlot = new Scatter2DPlot(plotDialog, 400, 400, dataSet, new int[]{dataSet.k - 2, dataSet.k - 1}, varNames, false);
            scatterPlot.setName(getPlotName());
        } catch (RserveException rse) {
            System.out.println("Rserve exception: " + rse.getMessage());
        }
        catch (REXPMismatchException mme) {
            System.out.println("Mismatch exception : " + mme.getMessage());
        }
        catch (REngineException ren) {
            System.out.println("REngine exception : " + ren.getMessage());
        }

        return null;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables > 2 && numVariables - numCategoricalVariables > 2 && RService.hasR();
    }
}

package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.Table;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class HistogramFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Histogram";
    }


    public String getDescription() {
        return null;
    }


    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F2, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }


    public PlotPanel createPlotPanel(Mondrian mondrian, MFrame plotFrame, DataSet dataSet, JList varNames) {

        int[] indices = varNames.getSelectedIndices();
        int weight = -1;

        return createHistogram(plotFrame, mondrian, dataSet, indices, weight);
    }


    protected PlotPanel createHistogram(MFrame plotFrame, Mondrian mondrian, DataSet dataSet, int[] indices, int weight) {
        int lastX = 310, oldX = 0;
        int row = 0;
        int menuOffset = 0, xOff = 0;

        for (int i = 0; i < indices.length; i++) {
            final MFrame mFrame = new MFrame(plotFrame.parentFrame, mondrian);

            int dummy;
            dummy = indices[i];
            double start = dataSet.getMin(dummy);
            double width = (dataSet.getMax(dummy) - dataSet.getMin(dummy)) / 8.9;
            Table discrete = dataSet.discretize(dataSet.setName, dummy, start, width, weight);

            mFrame.setSize(310, 250);
            Histogram histogram = new Histogram(mFrame, 310, 250, discrete, start, width, weight);


            if (lastX + mFrame.getWidth() > (Toolkit.getDefaultToolkit().getScreenSize()).width + 50) {       // new Row
                row += 1;
                lastX = oldX % 310;
            }

            if (250 * row > (Toolkit.getDefaultToolkit().getScreenSize()).height - 125) {                                    // new Page
                row = 0;
                lastX = 310 + xOff;
                xOff += menuOffset;
            }

            mFrame.setLocation(lastX, xOff + 250 * row);
            lastX += mFrame.getWidth();
            oldX = lastX;

            if (i == 0) {
                menuOffset = mFrame.getY();
                xOff = menuOffset;
            }

            makeVisible(mondrian, mFrame, histogram);
        }

        return null;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numCategoricalVariables == 0;
    }
}

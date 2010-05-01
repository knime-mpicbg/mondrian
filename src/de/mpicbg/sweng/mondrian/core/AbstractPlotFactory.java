package de.mpicbg.sweng.mondrian.core;

import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public abstract class AbstractPlotFactory implements PlotFactory {

    public void makeVisible(Mondrian mondrian, MFrame plotFrame, PlotPanel plotPanel) {
        for (PlotPanel plot : plotPanel.getPlots()) {
            mondrian.addPlot((DragBox) plot);
        }

        plotFrame.getContentPane().add(plotPanel);

        plotFrame.setTitle(plotPanel.getName() != null ? plotPanel.getName() : getPlotName());
        plotFrame.setVisible(true);
    }
}

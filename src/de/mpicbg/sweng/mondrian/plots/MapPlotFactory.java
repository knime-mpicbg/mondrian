package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.AbstractPlotFactory;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.MapCache;
import de.mpicbg.sweng.mondrian.ui.PlotPanel;

import javax.swing.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MapPlotFactory extends AbstractPlotFactory {

    public String getPlotName() {
        return "Map";
    }


    public String getShortDescription() {
        return null;
    }


    public PlotPanel createPlotPanel(MonFrame monFrame, MDialog plotDialog, DataSet dataSet, JList varNames) {
        MapPlot mapPlot = new MapPlot(plotDialog, 400, 400, dataSet, monFrame.polys, varNames);

        if (mapPlot.ratio > 1)
            mapPlot.setSize((int) (350 * mapPlot.ratio), 350 + 56);
        else
            mapPlot.setSize(350, (int) (350 / mapPlot.ratio) + 56);

        return mapPlot;
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return MapCache.getInstance().getPolys(dataSet).size() > 0;
    }
}

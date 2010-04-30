package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.core.DataListener;
import de.mpicbg.sweng.mondrian.core.SelectionListener;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A base class of all plot-panels in Mondrian
 *
 * @author Holger Brandl
 */
public class PlotPanel extends JPanel {

    protected SelectionListener slistener;
    protected DataListener dlistener;


    public List<PlotPanel> getPlots() {
        if (getComponentCount() == 0) {
            return Arrays.asList(this);

        } else {
            List<PlotPanel> plots = new ArrayList<PlotPanel>();
            for (int i = 0; i < getComponentCount(); i++) {
                Component component = getComponent(i);

                // we need to check because in case of SPLOM only upper triangular is PlotPanel
                if (component instanceof PlotPanel)
                    plots.add((PlotPanel) component);
            }

            return plots;
        }
    }


    public void addSelectionListener(SelectionListener l) {
        slistener = l;
    }


    public void addDataListener(DataListener l) {
        dlistener = l;
    }
}

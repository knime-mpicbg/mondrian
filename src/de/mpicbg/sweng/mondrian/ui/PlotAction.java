package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MDialog;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.PlotFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class PlotAction extends AbstractAction {

    private PlotFactory plotFactory;
    private MonFrame monFrame;


    public PlotAction(PlotFactory plotFactory, MonFrame monFrame) {
        this.plotFactory = plotFactory;
        this.monFrame = monFrame;

        // actions are enabled if the user changes the variable selection
        setEnabled(false);

        // configure the action
        putValue(NAME, plotFactory.getPlotName()); //$NON-NLS-1$
//        putValue(SMALL_ICON, new ScaleableIcon("/resources/icons/category_add.png"));
        putValue(SHORT_DESCRIPTION, plotFactory.getShortDescription());
//        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
//        putValue(MNEMONIC_KEY, 2);
    }


    public void actionPerformed(ActionEvent e) {
        Mondrian mondrian = monFrame.getController().getCurrent();
        mondrian.getSelector().checkHistoryBuffer();

        MDialog plotFrame = new MDialog(monFrame, mondrian);
        Font SF = new Font("SansSerif", Font.PLAIN, 11);
        plotFrame.setFont(SF);

        PlotPanel plotPanel = plotFactory.createPlotPanel(mondrian, plotFrame, mondrian.getDataSet(), mondrian.getSelector().getVarNames());


        if (plotPanel != null) {
            for (PlotPanel plot : plotPanel.getPlots()) {
                mondrian.addPlot((DragBox) plot);
            }
            plotFrame.getContentPane().add(plotPanel);

            plotFrame.setTitle(plotPanel.getName() != null ? plotPanel.getName() : plotFactory.getPlotName());
            plotFrame.setVisible(true);
        }
    }


    /**
     * Invoked to match the menu-options to the current variable selection.
     */
    public void configureForVarSelection(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        setEnabled(plotFactory.isCompliant(dataSet, numVariables, numCategoricalVariables));
    }
}

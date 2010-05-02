package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.AppFrame;
import de.mpicbg.sweng.mondrian.MFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.PlotFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class PlotAction extends AbstractAction {

    private PlotFactory plotFactory;
    private AppFrame appFrame;


    public PlotAction(PlotFactory plotFactory, AppFrame appFrame) {
        this.plotFactory = plotFactory;
        this.appFrame = appFrame;

        // actions are enabled if the user changes the variable selection
        setEnabled(false);

        // configure the action
        putValue(NAME, plotFactory.getPlotName()); //$NON-NLS-1$
//        putValue(SMALL_ICON, new ScaleableIcon("/resources/icons/category_add.png"));
        putValue(SHORT_DESCRIPTION, plotFactory.getDescription());
//        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
//        putValue(MNEMONIC_KEY, 2);
    }


    public void actionPerformed(ActionEvent e) {
        Mondrian mondrian = appFrame.getController().getCurrent();
        mondrian.getSelector().checkHistoryBuffer();

        MFrame plotFrame = new MFrame(appFrame, mondrian);

        PlotPanel plotPanel = plotFactory.createPlotPanel(mondrian, plotFrame, mondrian.getDataSet(), mondrian.getSelector().getVarNames());

        if (plotPanel != null) {
            plotFactory.makeVisible(mondrian, plotFrame, plotPanel);
        }
    }


    /**
     * Invoked to match the menu-options to the current variable selection.
     */
    public void configureForVarSelection(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        setEnabled(plotFactory.isCompliant(dataSet, numVariables, numCategoricalVariables));
    }
}

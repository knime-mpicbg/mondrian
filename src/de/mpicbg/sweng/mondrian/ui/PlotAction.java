package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonFrame;
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
        plotFactory.createPlotPanel();

    }


    /**
     * Invoked to match the menu-options to the current variable selection.
     */
    public void configureForVarSelection(int numVariables, int numCategoricalVariables) {
        setEnabled(plotFactory.isCompliant(numVariables, numCategoricalVariables));
    }
}

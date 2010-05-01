package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class DeriveVariableAction extends AbstractAction {

    private MonController controller;

    private boolean isColor;
    private int deriveVarColorCounter = 1, deriveVarSelectionCounter = 1;


    public DeriveVariableAction(String name, MonController controller, boolean color) {
        super(name);

        this.controller = controller;
        isColor = color;

        // actions are enabled if the user changes the variable selection
        setEnabled(false);

        // configure the action
//        putValue(SMALL_ICON, new ScaleableIcon("/resources/icons/category_add.png"));
//        putValue(SHORT_DESCRIPTION, plotFactory.getShortDescription());
//        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
//        putValue(MNEMONIC_KEY, 2);
    }


    public void actionPerformed(ActionEvent e) {
        deriveVariable(isColor);
    }


    public void deriveVariable(boolean color) {
        String name;
        DataSet data = controller.getCurrentDataSet();

        if (color) {
            name = "Colors " + deriveVarColorCounter++;
        } else {
            name = "Selection " + deriveVarSelectionCounter++;
        }

        name = JOptionPane.showInputDialog(controller.getMonFrame(), "Please name the new variable:", name);

        double[] dData;
        if (color) {
            dData = new double[data.n];
            for (int i = 0; i < data.n; i++)
                dData[i] = (double) data.colorArray[i];
        } else {
            dData = data.getSelection();
        }
        data.addVariable(name, false, true, dData, new boolean[data.n]);

        //todo update the UI
    }
}
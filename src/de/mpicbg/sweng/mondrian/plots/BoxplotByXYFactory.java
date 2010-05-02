package de.mpicbg.sweng.mondrian.plots;

import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class BoxplotByXYFactory extends ParallelBoxplotFactory {

    public String getPlotName() {
        return "Boxplot y by x";
    }


    public String getDescription() {
        return null;
    }


    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F5, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }


    public boolean isCompliant(DataSet dataSet, int numVariables, int numCategoricalVariables) {
        return numVariables == 2 && numCategoricalVariables == 1;
    }
}

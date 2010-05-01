package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.io.DataFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class LoadRDataFrameAction extends AbstractAction {

    MonController monController;

    private Component parent;


    public LoadRDataFrameAction(MonController monController) {
        super("Open R dataframe");

        this.monController = monController;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    public void actionPerformed(ActionEvent actionEvent) {
        new DataFrameConverter(monController.getMonFrame()).loadDataFrame();
        //todo continue
    }
}

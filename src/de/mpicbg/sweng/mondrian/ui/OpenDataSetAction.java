package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class OpenDataSetAction extends AbstractAction {

    MonController monController;


    public OpenDataSetAction(MonController monController) {
        super("Load Dataset");

        this.monController = monController;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    public void actionPerformed(ActionEvent actionEvent) {
        monController.loadDataSet(null, null); // a 'null'-file will cause the acsiiloader to open a dialog
    }
}

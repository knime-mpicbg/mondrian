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
public class CreateDBDataSetAction extends AbstractAction {

    MonController monController;


    public CreateDBDataSetAction(MonController monController) {
        super("Open Database");

        this.monController = monController;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    public void actionPerformed(ActionEvent actionEvent) {
        // implement me
    }
}
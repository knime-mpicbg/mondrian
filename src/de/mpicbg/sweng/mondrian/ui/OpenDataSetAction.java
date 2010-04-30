package de.mpicbg.sweng.mondrian.ui;

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

    public OpenDataSetAction() {
        super("Open");

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    }


    public void actionPerformed(ActionEvent actionEvent) {
    }
}

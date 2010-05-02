package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.Mondrian;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class CloseDataSetAction extends AbstractAction {

    private MonController controller;


    public CloseDataSetAction(MonController controller) {
        super("Close Dataset");
        this.controller = controller;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        setEnabled(false);
    }


    public void actionPerformed(ActionEvent actionEvent) {
        close(controller.getCurrent());
    }


    private void close(Mondrian mondrian) {
        controller.close(controller.getCurrent(), true);
    }
}

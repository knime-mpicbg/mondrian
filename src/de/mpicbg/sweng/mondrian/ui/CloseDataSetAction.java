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


        // close immediately if there are no plots open
        if (mondrian.getPlots().isEmpty()) {
            mondrian.close();
            controller.removeInstance(mondrian);
            if (controller.getNumInstances() == 0) {
                System.exit(0);
            }

            return;
        }

        String message = "Close dataset \"" + controller.getCurrentDataSet().setName + "\" and\n all corresponding plots?";

        int answer = JOptionPane.showConfirmDialog(controller.getMonFrame(), message);


        if (answer == JOptionPane.YES_OPTION) {
            mondrian.close();
        }

    }
}

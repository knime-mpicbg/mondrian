package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.util.ModelNavigator;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class StartModelNavAction extends AbstractAction {

    private MonController controller;

    private ModelNavigator modelNavigator;


    public StartModelNavAction(String name, MonController controller) {
        super(name);

        this.controller = controller;

        // actions are enabled if the user changes the variable selection
        setEnabled(false);

        // configure the action
//        putValue(SMALL_ICON, new ScaleableIcon("/resources/icons/category_add.png"));
//        putValue(SHORT_DESCRIPTION, plotFactory.getShortDescription());
//        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        putValue(MNEMONIC_KEY, KeyEvent.VK_J);
    }


    public void actionPerformed(ActionEvent e) {
        if (modelNavigator == null) {
            modelNavigator = new ModelNavigator();
        } else {
            modelNavigator.show();
        }
    }
}
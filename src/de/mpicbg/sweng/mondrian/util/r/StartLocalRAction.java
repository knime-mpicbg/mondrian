package de.mpicbg.sweng.mondrian.util.r;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class StartLocalRAction extends AbstractAction {


    public StartLocalRAction(String name) {
        super(name);

        // actions are enabled if the user changes the variable selection
        setEnabled(true);
        putValue(MNEMONIC_KEY, KeyEvent.VK_R);
    }


    public void actionPerformed(ActionEvent e) {
        System.out.println("Starting RServe ... ");
        RService.checkLocalRserve();
    }
}
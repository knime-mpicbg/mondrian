package de.mpicbg.sweng.mondrian.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class GoToWebsiteAction extends AbstractAction {


    public GoToWebsiteAction(String name) {
        super(name);

        // actions are enabled if the user changes the variable selection
        setEnabled(true);

        // configure the action
//        putValue(SMALL_ICON, new ScaleableIcon("/resources/icons/category_add.png"));
//        putValue(SHORT_DESCRIPTION, plotFactory.getShortDescription());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//        putValue(MNEMONIC_KEY, 2);
    }


    public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(new URL("http://www.rosuda.org/Mondrian").toURI());
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e11) {
            e11.printStackTrace();
        }
    }
}
package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.AppFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class PreferencesAction extends AbstractAction {

    private AppFrame appFrame;


    public PreferencesAction(String name, AppFrame appFrame) {
        super(name);

        this.appFrame = appFrame;

        // actions are enabled if the user changes the variable selection
        setEnabled(true);

        // configure the action
//        putValue(SMALL_ICON, new ScaleableIcon("/resources/icons/category_add.png"));
//        putValue(SHORT_DESCRIPTION, plotFactory.getShortDescription());
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        putValue(MNEMONIC_KEY, KeyEvent.VK_K);
    }


    public void actionPerformed(ActionEvent e) {
        PreferencesFrame.showPrefsDialog(appFrame);
    }
}
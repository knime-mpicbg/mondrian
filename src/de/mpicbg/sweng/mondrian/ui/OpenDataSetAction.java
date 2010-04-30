package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.io.AsciiFileLoader;
import de.mpicbg.sweng.mondrian.util.StatUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;


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
        loadDataSet(null, null); // a 'null'-file will cause the acsiiloader to open a dialog
    }


    public void loadDataSet(File file, String title) {

        //    System.out.println(".......... IN loadDataSet("+thisDataSet+") IN .........");


        Mondrian mondrian = new Mondrian();


        if (new AsciiFileLoader(mondrian, monController.getMonFrame()).loadAsciiFile(file)) {
            setVarList();
            if (title.equals(""))
                this.setTitle("Mondrian(" + controller.getCurrentDataSet().setName + ")");               //
            else
                this.setTitle("Mondrian(" + title + ")");
            me.setText(this.getTitle());
            closeDataSetMenuItem.setEnabled(true);
            saveMenuItem.setEnabled(true);

            int nom = controller.getCurrentDataSet().countSelection();
            int denom = controller.getCurrentDataSet().n;
            String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100 * nom / denom, 2) + "%)";
            progText.setText(Display);
            progBar.setValue(nom);

            maintainOptionMenu();
        }
        new MonFrame(monFrames, MonController.dataSets, true, isDB, file);
    }
}

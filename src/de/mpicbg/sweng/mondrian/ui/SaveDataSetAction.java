package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class SaveDataSetAction extends AbstractAction {

    private boolean saveSelection;
    private MonController monController;


    public SaveDataSetAction(String name, boolean saveSelection, MonController monController) {
        super(name);
        this.saveSelection = saveSelection;
        this.monController = monController;

        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        setEnabled(false);
    }


    public void actionPerformed(ActionEvent actionEvent) {
        save(saveSelection);
    }


    public void save(boolean selection) {
        monController.getCurrent().getSelector().checkHistoryBuffer();

        FileDialog f;
        if (selection)
            f = new FileDialog(monController.getMonFrame(), "Save Selection", FileDialog.SAVE);
        else
            f = new FileDialog(monController.getMonFrame(), "Save Data", FileDialog.SAVE);
        f.setVisible(true);

        if (f.getFile() != null) {
            saveDataSet(f.getDirectory() + f.getFile(), selection);
        }
    }


    public boolean saveDataSet(String file, boolean selection) {
        try {
            Mondrian mondrian = monController.getCurrent();
            DataSet dataSet = mondrian.getDataSet();
            JList varNames = mondrian.getSelector().getVarNames();

            int k = dataSet.k;
            int n = dataSet.n;

            FileWriter fw = new FileWriter(file);

            double[][] dataCopy = new double[k][n];
            boolean[][] missing = new boolean[k][n];
            double[] selected = dataSet.getSelection();

            for (int j = 0; j < k; j++) {
                missing[j] = dataSet.getMissings(j);
                if (dataSet.categorical(j) && !dataSet.alpha(j))
                    dataCopy[j] = dataSet.getRawNumbers(j);
                else
                    dataCopy[j] = dataSet.getNumbers(j);
            }

            String line = "";
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "US"));
            DecimalFormat df = (DecimalFormat) nf;
            df.applyPattern("#.#################");

            boolean first = true;
            for (int j = 0; j < k; j++)
                if ((varNames.getSelectedIndices().length == 0) || varNames.isSelectedIndex(j)) {
                    line += (first ? "" : "\t") + dataSet.getName(j);
                    first = false;
                }
            fw.write(line + "\r");

            for (int i = 0; i < n; i++) {
                if (!selection || (selection && selected[i] > 0)) {
                    line = "";
                    first = true;
                    for (int j = 0; j < k; j++)
                        if ((varNames.getSelectedIndices().length == 0) || varNames.isSelectedIndex(j)) {
                            if (missing[j][i])
                                line += (first ? "" : "\t") + "NA";
                            else if (dataSet.categorical(j))
                                line += (first ? "" : "\t") + dataSet.getLevelName(j, dataCopy[j][i]);
                            else
                                line += (first ? "" : "\t") + df.format(dataCopy[j][i]);
                            first = false;
                        }
                    fw.write(line + (i == (n - 1) ? "" : "\r"));
                }
            }

            fw.close();

        } catch (Exception ex) {
            System.out.println("Error writing to file: " + ex);
            return false;
        }
        return true;
    }

}
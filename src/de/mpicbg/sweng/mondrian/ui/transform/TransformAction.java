package de.mpicbg.sweng.mondrian.ui.transform;

import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import java.awt.event.ActionEvent;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class TransformAction extends AbstractAction {

    private int trafoMode;
    private MonFrame monFrame;
    private int numArguments;


    public TransformAction(String title, int trafoMode, MonFrame monFrame, int numArguments) {
        super(title);


        this.trafoMode = trafoMode;
        this.monFrame = monFrame;
        this.numArguments = numArguments;
    }


    public void actionPerformed(ActionEvent actionEvent) {
        transform(trafoMode);
    }


    public boolean isCompliant(int numNumericVars) {
        return numArguments == numNumericVars;
    }


    public void transform(int mode) {
        monFrame.checkHistoryBuffer();

        System.out.println("Transform: " + mode);
        String name = "";
        DataSet data = monFrame.getController().getCurrentDataSet();

        double[] tData = new double[data.n];
        boolean[] tMiss = new boolean[data.n];
        int[] selectBuffer = monFrame.selectBuffer;
        String name1 = data.getName(selectBuffer[1]);
        String name2 = data.getName(selectBuffer[0]);
        switch (mode) {
            case 1:
                name = name1 + " + " + name2;
                break;
            case 2:
                name = name1 + " - " + name2;
                break;
            case 3:
                name = name1 + " * " + name2;
                break;
            case 4:
                name = name1 + " / " + name2;
                break;
            case 5:
                name = "-" + name2;
                break;
            case 6:
                name = "1/" + name2;
                break;
            case 7:
                name = "log(" + name2 + ")";
                break;
            case 8:
                name = "exp(" + name2 + ")";
                break;
        }
        double[] var1 = data.getRawNumbers(selectBuffer[1]);
        double[] var2 = data.getRawNumbers(selectBuffer[0]);
        boolean[] miss1 = data.getMissings(selectBuffer[1]);
        boolean[] miss2 = data.getMissings(selectBuffer[0]);
        for (int i = 0; i < data.n; i++) {
            if (miss2[i] || ((mode < 5) && (miss1[i] || miss2[i])))
                tMiss[i] = true;
            else
                tMiss[i] = false;
            switch (mode) {
                case 1:
                    tData[i] = var1[i] + var2[i];
                    break;
                case 2:
                    tData[i] = var1[i] - var2[i];
                    break;
                case 3:
                    tData[i] = var1[i] * var2[i];
                    break;
                case 4:
                    if (var2[i] != 0)
                        tData[i] = var1[i] / var2[i];
                    else
                        tMiss[i] = true;
                    break;
                case 5:
                    tData[i] = -var2[i];
                    break;
                case 6:
                    if (var2[i] != 0)
                        tData[i] = 1 / var2[i];
                    else
                        tMiss[i] = true;
                    break;
                case 7:
                    if (var2[i] > 0)
                        tData[i] = Math.log(var2[i]);
                    else
                        tMiss[i] = true;
                    break;
                case 8:
                    tData[i] = Math.exp(var2[i]);
                    break;
            }
        }
        for (int i = 0; i < data.n; i++)
            if (tMiss[i])
                tData[i] = Double.MAX_VALUE;
        boolean what;
        if (mode < 5)
            what = data.categorical(selectBuffer[0]) && data.categorical(selectBuffer[1]);
        else
            what = data.categorical(selectBuffer[0]);
        data.addVariable(name, false, what, tData, tMiss);
        monFrame.varNames = null;
        monFrame.setVarList();
    }


    public static JMenu createTrafoMenu(MonFrame monFrame) {
        JMenu transformMenu = new JMenu("Transform");

        transformMenu.setEnabled(false);
        transformMenu.add(new JMenuItem(new TransformAction("x + y", 1, monFrame, 2)));
        transformMenu.add(new JMenuItem(new TransformAction("x - y", 2, monFrame, 2)));
        transformMenu.add(new JMenuItem(new TransformAction("x * y", 3, monFrame, 2)));
        transformMenu.add(new JMenuItem(new TransformAction("x / y", 4, monFrame, 2)));
        transformMenu.addSeparator();
        transformMenu.add(new JMenuItem(new TransformAction("- x", 5, monFrame, 1)));
        transformMenu.add(new JMenuItem(new TransformAction("1/x", 6, monFrame, 1)));
        transformMenu.add(new JMenuItem(new TransformAction("log(x)", 7, monFrame, 1)));
        transformMenu.add(new JMenuItem(new TransformAction("exp(x)", 8, monFrame, 1)));

        return transformMenu;
    }
}

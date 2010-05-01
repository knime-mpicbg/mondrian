package de.mpicbg.sweng.mondrian.util;

import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class WeightCaclulator {

    public static int[] getWeightVariable(int[] vars, DataSet data, int numCategorical, int weightIndex, JFrame parent, JList jList) {

        if (numCategorical == (vars).length - 1) {
            int[] returner = new int[vars.length];
            System.arraycopy(vars, 0, returner, 0, returner.length);

            for (int i = 0; i < returner.length - 1; i++) {
                if (vars[i] == weightIndex) {
                    System.arraycopy(vars, i + 1, returner, i, returner.length - 1 - i);

                    returner[returner.length - 1] = weightIndex;
                    i = returner.length;

                } else
                    returner[i] = vars[i];
            }
            for (int i = 0; i < returner.length; i++) {
                System.out.println("ind old = " + vars[i] + " ind new = " + returner[i]);
            }

            return returner;

        } else {
            final JDialog countDialog = new JDialog(parent, " Choose Weight Variable", true);
            Choice getCount = new Choice();

            for (int j = 0; j < vars.length; j++) {
                if (data.getName(vars[j]).length() > 1 && data.getName(vars[j]).substring(0, 1).equals("/"))
                    getCount.addItem(data.getName(vars[j]).substring(2));
                else
                    getCount.addItem(data.getName(vars[j]));
            }
            for (int j = 0; j < getCount.getItemCount(); j++)
                if (getCount.getItem(j).toLowerCase().equals("count") ||
                        getCount.getItem(j).toLowerCase().equals("counts") ||
                        getCount.getItem(j).toLowerCase().equals("n") ||
                        getCount.getItem(j).toLowerCase().equals("weight") ||
                        getCount.getItem(j).toLowerCase().equals("observed") ||
                        getCount.getItem(j).toLowerCase().equals("number"))
                    getCount.select(j);
            Panel p1 = new Panel();
            p1.add(getCount);
            countDialog.add(p1, "Center");
            Button OK = new Button("OK");
            Panel p2 = new Panel();
            p2.add(OK);
            countDialog.add(p2, "South");
            OK.addActionListener(new ActionListener() {     //


                public void actionPerformed(ActionEvent e) {
                    countDialog.dispose();
                }
            });
            countDialog.pack();
            if (countDialog.getWidth() < 240)
                countDialog.setSize(240, countDialog.getHeight());
            countDialog.setResizable(false);
            countDialog.setModal(true);
            countDialog.setBounds(300, 300, 500, 400);
//            countDialog.setBounds(this.getBounds().x + this.getBounds().width / 2 - countDialog.getBounds().width / 2,
//                    this.getBounds().y + this.getBounds().height / 2 - countDialog.getBounds().height / 2,
//                    countDialog.getBounds().width,
//                    countDialog.getBounds().height);
            countDialog.show();

            String[] selecteds = new String[(jList.getSelectedValues()).length];
            for (int i = 0; i < (jList.getSelectedValues()).length; i++) {
                selecteds[i] = (String) (jList.getSelectedValues())[i];
            }

            int[] returner = new int[vars.length];
            for (int i = 0; i < vars.length; i++) {
                if ((selecteds[i].trim()).equals(getCount.getSelectedItem())) {
                    returner[vars.length - 1] = vars[i];
                    System.arraycopy(vars, i + 1, returner, i, vars.length - 1 - i);
                    i = vars.length;

                } else
                    returner[i] = vars[i];
            }
            return returner;
        }
    }
}

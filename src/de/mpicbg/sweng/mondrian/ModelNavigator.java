package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.ModelListener;
import de.mpicbg.sweng.mondrian.core.SelectionEvent;
import de.mpicbg.sweng.mondrian.plots.Mosaic;
import de.mpicbg.sweng.mondrian.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DecimalFormat;


public class ModelNavigator extends JFrame implements ActionListener, ModelListener {

    DecimalFormat dfP = new DecimalFormat("0.000");
    private Label modelP;
    private Label currentP;
    private Label interP;

    DecimalFormat dfG2 = new DecimalFormat("###0.0");
    private Label modelG2;
    private Label currentG2;
    private Label interG2;

    DecimalFormat dfX2 = new DecimalFormat("###0.0");
    private Label modelX2;
    private Label currentX2;
    private Label interX2;

    DecimalFormat dfDf = new DecimalFormat("###0");
    private Label modelDf;
    private Label currentDf;
    private Label interDf;

    private Label[] inter = new Label[5];

    private List interList;
    private Table myT;
    private Table t;
    private Table oldT;
    private Mosaic mosaic;

    private int oldMax;

    private String[] oldNames;


    public ModelNavigator() {
        this.setTitle("ModelNavigator");
        this.setVisible(false);

        GridBagLayout gbl = new GridBagLayout();
        this.getContentPane().setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 10;
        gbc.weighty = 10;

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;

        Font SF = new Font("SansSerif", Font.BOLD, 14);
        this.setFont(SF);

        //mosaic = NULL;

        Util.add(this, new Label("Drop Int.:", Label.RIGHT), gbc, 1, 0, 1, 1);
        Util.add(this, new Label("Model:", Label.RIGHT), gbc, 2, 0, 1, 1);
        Util.add(this, new Label("Add Int.:", Label.RIGHT), gbc, 3, 0, 1, 1);
        Util.add(this, new Label("G2=", Label.RIGHT), gbc, 0, 1, 1, 1);
        Util.add(this, new Label("chi2=", Label.RIGHT), gbc, 0, 2, 1, 1);
        Util.add(this, new Label("df=", Label.RIGHT), gbc, 0, 3, 1, 1);
        Util.add(this, new Label("p=", Label.RIGHT), gbc, 0, 4, 1, 1);

        modelP = new Label(" -.----", Label.RIGHT);
        Util.add(this, modelP, gbc, 2, 4, 1, 1);
        currentP = new Label(" -.----", Label.RIGHT);
        Util.add(this, currentP, gbc, 3, 4, 1, 1);
        interP = new Label(" -.----", Label.RIGHT);
        Util.add(this, interP, gbc, 1, 4, 1, 1);

        modelG2 = new Label("----.-", Label.RIGHT);
        Util.add(this, modelG2, gbc, 2, 1, 1, 1);
        currentG2 = new Label("----.-", Label.RIGHT);
        Util.add(this, currentG2, gbc, 3, 1, 1, 1);
        interG2 = new Label("----.-", Label.RIGHT);
        Util.add(this, interG2, gbc, 1, 1, 1, 1);

        modelX2 = new Label("----.-", Label.RIGHT);
        Util.add(this, modelX2, gbc, 2, 2, 1, 1);
        currentX2 = new Label("----.-", Label.RIGHT);
        Util.add(this, currentX2, gbc, 3, 2, 1, 1);
        interX2 = new Label("----.-", Label.RIGHT);
        Util.add(this, interX2, gbc, 1, 2, 1, 1);

        modelDf = new Label("----", Label.RIGHT);
        Util.add(this, modelDf, gbc, 2, 3, 1, 1);
        currentDf = new Label("----", Label.RIGHT);
        Util.add(this, currentDf, gbc, 3, 3, 1, 1);
        interDf = new Label("----", Label.RIGHT);
        Util.add(this, interDf, gbc, 1, 3, 1, 1);

        Util.add(this, new Label("... in Plot:"), gbc, 3, 5, 1, 1);

        for (int i = 0; i < 4; i++) {
            inter[i] = new Label("", Label.RIGHT);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            Util.add(this, inter[i], gbc, 2, 6 + i, 2, 1);
        }

        interList = new List(5);
        interList.add("Dummy              ");

        interList.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                whatIfDeleted();
            }
        });

//    gbc.fill = GridBagConstraints.WEST;
        Panel P = new Panel();
        P.add(interList);
        Util.add(this, interList, gbc, 0, 5, 2, 5);

        gbc.fill = GridBagConstraints.NONE;
        Button Remove = new Button("Remove");
        Remove.setActionCommand("REMOVE");
        Remove.addActionListener(this);

        Button Reset = new Button("Reset");
        Reset.setActionCommand("RESET");
        Reset.addActionListener(this);

        Button Add = new Button("Add");
        Add.setActionCommand("ADD");
        Add.addActionListener(this);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        Util.add(this, Remove, gbc, 1, 10, 1, 1);
        Util.add(this, Reset, gbc, 2, 10, 1, 1);
        Util.add(this, Add, gbc, 3, 10, 1, 1);

        this.pack();
    }


    public Insets getInsets() {
        return new Insets(super.getInsets().top + 5, 10, 10, 10);
    }


    public void updateModel(Table t, String[] names, int maxLevel) {

        if (oldT != t || names != oldNames || oldMax != maxLevel) {
            this.t = t;
            myT = (Table) t.clone();

            modelP.setText(dfP.format(Stat.round(t.p, 3)));
            modelG2.setText(dfG2.format(Stat.round(t.G2, 1)));
            modelX2.setText(dfX2.format(Stat.round(t.X2, 1)));
            modelDf.setText(dfDf.format(t.df));

            interList.delItems(0, interList.getItemCount() - 1);
            for (int i = (t.Interactions).size() - 1; i >= 0; i--) {
                String inter = "";
                int k = t.Interactions.memberAt(i).length;
                if (k > 1)
                    for (int j = 0; j < k - 1; j++) {
                        String name = t.names[t.Interactions.memberAt(i)[j]];
                        inter += name.substring(0, Math.min(5, name.length())) + "*";
                    }
                String name = t.names[t.Interactions.memberAt(i)[k - 1]];
                inter += name.substring(0, Math.min(5, name.length()));
                interList.addItem(inter);
            }

            int[] interact = new int[maxLevel];
            for (int i = 0; i < maxLevel; i++)
                interact[i] = i;
            super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            myT.addInteraction(interact, true);
            super.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            currentP.setText(dfP.format(Stat.round(myT.p, 3)));
            currentG2.setText(dfG2.format(Stat.round(myT.G2, 1)));
            currentX2.setText(dfX2.format(Stat.round(myT.X2, 1)));
            currentDf.setText(dfDf.format(myT.df));

            for (int j = 0; j < Math.min(maxLevel, 4); j++) {
                String name = myT.names[j];
                inter[j].setText(name);
            }
            for (int j = maxLevel; j < 4; j++) {
                inter[j].setText("");
            }

            myT = (Table) t.clone();

            oldT = t;
            oldNames = t.names;
            oldMax = maxLevel;

            if (mosaic != null) {
                SelectionEvent se = new SelectionEvent(mosaic);
                EventQueue evtq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                evtq.postEvent(se);
            }
        }
    }


    void whatIfDeleted() {

        int id = interList.getSelectedIndex();
        int k = (myT.Interactions).size();
        int toDelete = k - id - 1;
        int[] save = myT.Interactions.memberAt(toDelete);
        if (save.length > 1) {
            super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            myT.deleteInteraction(save);
            super.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            interP.setText("" + Stat.round(myT.p, 3));
            interG2.setText("" + Stat.round(myT.G2, 1));
            interX2.setText("" + Stat.round(myT.X2, 1));
            interDf.setText("" + myT.df);
        } else {
            interP.setText("-.----");
            interG2.setText("----.-");
            interX2.setText("----.-");
            interDf.setText("----");
        }
        myT = (Table) t.clone();
    }


    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("ADD")) {
            int[] interact = new int[oldMax];
            for (int i = 0; i < oldMax; i++)
                interact[i] = i;
            super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            t.addInteraction(interact, true);
            super.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            modelP.setText(dfP.format(Stat.round(t.p, 3)));
            modelG2.setText(dfG2.format(Stat.round(t.G2, 1)));
            modelX2.setText(dfX2.format(Stat.round(t.X2, 1)));
            modelDf.setText(dfDf.format(t.df));

            interList.delItems(0, interList.countItems() - 1);
            for (int i = (t.Interactions).size() - 1; i >= 0; i--) {
                String inter = "";
                int k = t.Interactions.memberAt(i).length;
                if (k > 1)
                    for (int j = 0; j < k - 1; j++) {
                        String name = t.names[t.Interactions.memberAt(i)[j]];
                        inter += name.substring(0, Math.min(5, name.length())) + "*";
                    }
                String name = t.names[t.Interactions.memberAt(i)[k - 1]];
                inter += name.substring(0, Math.min(5, name.length()));
                interList.addItem(inter);
            }
        } else if (command.equals("REMOVE")) {

            int id = interList.getSelectedIndex();
            if (id > -1) {
                int kk = (t.Interactions).size();
                int toDelete = kk - id - 1;
                int[] save = t.Interactions.memberAt(toDelete);
                super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                t.deleteInteraction(save);
                super.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                modelP.setText(dfP.format(Stat.round(t.p, 3)));
                modelG2.setText(dfG2.format(Stat.round(t.G2, 1)));
                modelX2.setText(dfX2.format(Stat.round(t.X2, 1)));
                modelDf.setText(dfDf.format(t.df));

                interP.setText("-.----");
                interG2.setText("----.-");
                interX2.setText("----.-");
                interDf.setText("----");

                interList.delItems(0, interList.getItemCount() - 1);
                for (int i = (t.Interactions).size() - 1; i >= 0; i--) {
                    String inter = "";
                    int k = t.Interactions.memberAt(i).length;
                    if (k > 1)
                        for (int j = 0; j < k - 1; j++) {
                            String name = t.names[t.Interactions.memberAt(i)[j]];
                            inter += name.substring(0, Math.min(5, name.length())) + "*";
                        }
                    String name = t.names[t.Interactions.memberAt(i)[k - 1]];
                    inter += name.substring(0, Math.min(5, name.length()));
                    interList.addItem(inter);
                }
            }
        }
    }
}




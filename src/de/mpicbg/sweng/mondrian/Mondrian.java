package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.Selection;
import de.mpicbg.sweng.mondrian.core.SelectionListener;
import de.mpicbg.sweng.mondrian.io.ProgressIndicator;
import de.mpicbg.sweng.mondrian.io.db.Query;
import de.mpicbg.sweng.mondrian.ui.VariableSelector;
import de.mpicbg.sweng.mondrian.util.StatUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Vector;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Mondrian implements SelectionListener, ProgressIndicator {

    public Vector<DragBox> plots = new Vector<DragBox>();

    DataSet dataSet;

    MondrianDialog dialog;


    public Mondrian() {

    }


    private Component con;


    public void addPlot(DragBox plot) {
        plots.add(plot);

        plot.addDataListener(this);
        plot.addSelectionListener(this);

    }


    public DataSet getDataSet() {
        return dataSet;
    }


    public void updateSelection() {
        // Remove Selections from list, which are no longer active
        //
        boolean selectAll = false;
        boolean unSelect = false;
        boolean toggleSelection = false;

        for (int i = 0; i < plots.size(); i++) {
            if (plots.elementAt(i).selectAll) {    // This window has caused the select all event
                plots.elementAt(i).selectAll = false;
                selectAll = true;
            }
            if (plots.elementAt(i).unSelect) {    // This window has caused the un select event
                plots.elementAt(i).unSelect = false;
                unSelect = true;
            }
            if (plots.elementAt(i).toggleSelection) {    // This window has caused the toggle selection event
                plots.elementAt(i).toggleSelection = false;
                toggleSelection = true;
            }
            if (plots.elementAt(i).deleteAll) {    // This window has caused the deletion event
                plots.elementAt(i).deleteAll = false;
                deleteSelection();
                return;
            }
            if (plots.elementAt(i).switchSel) {    // This window has caused the switch event
                plots.elementAt(i).switchSel = false;
                selSeqCheckItem.setSelected(!selSeqCheckItem.isSelected());                // perform the tick mark change manually ...
                switchSelection();
                return;
            }
            if (plots.elementAt(i).switchAlpha) {    // This window has caused the switch alpha event
                plots.elementAt(i).switchAlpha = false;
                alphaOnHighlightCheckMenuItem.setSelected(!alphaOnHighlightCheckMenuItem.isSelected());
                switchAlpha();
                plots.elementAt(i).updateSelection();
                return;
            }
        }

        if (!(unSelect || selectAll || toggleSelection)) {

            for (int i = selList.size() - 1; i >= 0; i--) {
                if ((selList.elementAt(i).status == Selection.KILLED) ||
                        !selList.elementAt(i).d.frame.isVisible()) {
                    selList.removeElementAt(i);
                }
            }

            selList.trimToSize();

            Selection oneClick = null;

            // Get the latest selection and add it, if its a new selection
            //
            for (int i = 0; i < plots.size(); i++)
                if (plots.elementAt(i).frame.isVisible()) {  // Plotwindow still exists
                    if (plots.elementAt(i).selectFlag) {       // This window has caused the selection event
                        plots.elementAt(i).selectFlag = false;    // We need to get the last selection from this plot
                        Selection S = (Selection) (((DragBox) plots.elementAt(i)).Selections.lastElement());
                        if (selList.indexOf(S) == -1) { // Not in the list yet => new Selection to add !
                            if (!(S.r.width < 3 || S.r.height < 3) && selseq) {
                                System.out.println("Selection Sequence  !!");
                                S.step = selList.size() + 1;
                                selList.addElement(S);
                            } else {
                                oneClick = S;
                                System.out.println("Click Selection  !!");
                                oneClick.status = Selection.KILLED;
                                plots.elementAt(i).Selections.removeElementAt(plots.elementAt(i).Selections.size() - 1);
                            }
                        }
                    }
                } else
                    plots.removeElementAt(i--);

            if (selList.size() > 1) {
                selList.firstElement().mode = Selection.MODE_STANDARD;
            }
            // Do the update over all selections
            //
            if (oneClick != null) {
                //  This is a oneClick selection -> make it visible for Java 1.4 ...
                oneClick.r.width += 1;
                oneClick.r.height += 1;
                (oneClick.d).maintainSelection(oneClick);
            } else {
                maintainWindowMenu(false);

                for (int i = 0; i < selList.size(); i++) {
                    Selection S = selList.elementAt(i);
                    S.step = i + 1;
                    S.total = selList.size();
                    (S.d).maintainSelection(S);
                    (S.d).frame.maintainMenu(S.step);
                }
            }
            sqlConditions = new Query();                // Replace ???
            if (controller.getCurrentDataSet().isDB)
                for (int i = 0; i < selList.size(); i++) {
                    Selection S = selList.elementAt(i);
                    if (S.mode == Selection.MODE_STANDARD)
                        sqlConditions.clearConditions();
                    String condStr = S.condition.getConditions();
                    if (!condStr.equals(""))
                        sqlConditions.addCondition(Selection.getSQLModeString(S.mode), "(" + condStr + ")");
                }
            controller.getCurrentDataSet().sqlConditions = sqlConditions;

            //      System.out.println("Main Update: "+sqlConditions.makeQuery());

        } else {
            if (toggleSelection) {
                System.out.println(" TOGGLE SELECTION ... ");
                controller.getCurrentDataSet().toggleSelection();
            } else if (unSelect) {
                System.out.println(" UNSELECT ... ");
                controller.getCurrentDataSet().clearSelection();
            } else {
                System.out.println(" SELECT ALL ... ");
                controller.getCurrentDataSet().selectAll();
            }
            if (controller.getCurrentDataSet().isDB)
                sqlConditions.clearConditions();
        }

        // Finally get the plots updated
        //
        for (int i = 0; i < plots.size(); i++) {
            //     progText.setText("Query: "+i);
            progBar.setValue(1);
            plots.elementAt(i).updateSelection();
        }

        controller.getCurrentDataSet().selChanged = true;
        int nom = controller.getCurrentDataSet().countSelection();
        int denom = controller.getCurrentDataSet().n;
        String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100F * nom / denom, 2) + "%)";
        progText.setText(Display);
        progBar.setValue(nom);

        maintainOptionMenu();

        if (nom > 0)
            saveSelectionMenuItem.setEnabled(true);
        else
            saveSelectionMenuItem.setEnabled(false);
    }


    public void dataChanged(int id) {

        //System.out.println("MonFrame got the event !!!!"+id);

        maintainOptionMenu();

        System.out.println("Key Event in MonFrame");

        // First check whether a color has been set individually
        for (int i = 0; i < plots.size(); i++) {
            int col = plots.elementAt(i).colorSet;
            if (col > -1) {
                plots.elementAt(i).colorSet = -1;
                DataSet data = controller.getCurrentDataSet();
                id = -1;
                if (col < 999) {
                    System.out.println("Setting Colors !!!!");
                    int retCol = data.addColor(col);
                    double selections[] = data.getSelection();
                    for (int j = 0; j < data.n; j++)
                        if (selections[j] != 0)
                            data.setColor(j, retCol);
                } else
                    data.colorsOff();
            }
        }
        // Then ordinary update loop
        for (int i = 0; i < plots.size(); i++)
            if (plots.elementAt(i).frame.isVisible())  // Plotwindow still exists
                if (plots.elementAt(i).dataFlag)         // This window was already updated
                    plots.elementAt(i).dataFlag = false;
                else
                    plots.elementAt(i).dataChanged(id);
            else
                plots.removeElementAt(i);
    }


    public void deleteSelection() {
        if (selList.size() > 0) {
            for (int i = 0; i < plots.size(); i++)
                (plots.elementAt(i).Selections).removeAllElements();
            for (int i = 0; i < selList.size(); i++)
                selList.elementAt(i).status = Selection.KILLED;
            maintainWindowMenu(false);
            updateSelection();
        }
    }


    public void setProgress(double progress) {
        progBar.setValue((int) (100 * progress));
    }


    public List<DragBox> getPlots() {
        return plots;
    }


    public void close() {
        if (varSelector.isDisplayable())
            varSelector.dispose();


        for (int i = plots.size() - 1; i >= 0; i--) {
            plots.elementAt(i).frame.close();
        }
    }


    public VariableSelector getSelector() {
        return varSelector;
    }


    public JProgressBar getProgBar() {
        return dialog.getProgBar();
    }


    public Component getParent() {
        return con;
    }


    public void setProgText(String msg) {
        dialog.getProgText().setText(msg);
    }
}

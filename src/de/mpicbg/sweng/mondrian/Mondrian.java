package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.*;
import de.mpicbg.sweng.mondrian.io.ProgressIndicator;
import de.mpicbg.sweng.mondrian.ui.VariableSelector;
import de.mpicbg.sweng.mondrian.util.StatUtil;

import javax.swing.*;
import java.util.List;
import java.util.Vector;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class Mondrian implements SelectionListener, ProgressIndicator, DataListener {

    MonController controller;

    public Vector<DragBox> plots = new Vector<DragBox>();

    DataSet dataSet;
    public Vector<Selection> selList = new Vector<Selection>();

    MondrianDialog dialog;


    public Mondrian(DataSet dataSet, MonController controller) {
        this.dataSet = dataSet;
        this.controller = controller;

        // create the dialog

        dialog = new MondrianDialog(controller.getMonFrame(), this);
    }


    public void addPlot(DragBox plot) {
        plots.add(plot);

        plot.addDataListener(this);
        plot.addSelectionListener(this);

    }


    public MonController getController() {
        return controller;
    }


    public MondrianDialog getDialog() {
        return dialog;
    }


    public DataSet getDataSet() {
        return dataSet;
    }


    public int calcNumCategoricalVars() {

        int numCategorical = 0;
        JList list = getSelector().getVarNames();
        for (int i = 0; i < list.getSelectedIndices().length; i++) {
            if (controller.getCurrentDataSet().categorical(list.getSelectedIndices()[i]))
                numCategorical++;
        }

        return numCategorical;
    }


    public int determineWeightIndex() {
        int weightIndex = 0;
        DataSet dataSet = controller.getCurrentDataSet();

        JList varNames = getSelector().getVarNames();
        for (int i = 0; i < varNames.getSelectedIndices().length; i++) {
            if (!dataSet.categorical(varNames.getSelectedIndices()[i]))
                weightIndex = varNames.getSelectedIndices()[i];
        }

        return weightIndex;
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
                JCheckBoxMenuItem selSeqCheckItem = controller.getMonFrame().selSeqCheckItem;
                selSeqCheckItem.setSelected(!selSeqCheckItem.isSelected());                // perform the tick mark change manually ...
                controller.monFrame.switchSelection();
                return;
            }
            if (plots.elementAt(i).switchAlpha) {    // This window has caused the switch alpha event
                plots.elementAt(i).switchAlpha = false;

                JCheckBoxMenuItem alphaOnHighlightCheckMenuItem = controller.getMonFrame().alphaOnHighlightCheckMenuItem;

                alphaOnHighlightCheckMenuItem.setSelected(!alphaOnHighlightCheckMenuItem.isSelected());
                controller.getMonFrame().switchAlpha();
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
                            if (!(S.r.width < 3 || S.r.height < 3) && controller.monFrame.selseq) {
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
                for (int i = 0; i < selList.size(); i++) {
                    Selection S = selList.elementAt(i);
                    S.step = i + 1;
                    S.total = selList.size();
                    (S.d).maintainSelection(S);
                    (S.d).frame.maintainMenu(S.step);
                }
            }

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
        }

        // Finally get the plots updated
        //
        for (int i = 0; i < plots.size(); i++) {
            //     progText.setText("Query: "+i);
            getProgBar().setValue(1);
            plots.elementAt(i).updateSelection();
        }

        controller.getCurrentDataSet().selChanged = true;
        int selectionCount = controller.getCurrentDataSet().countSelection();
        int denom = controller.getCurrentDataSet().n;
        String msg = selectionCount + "/" + denom + " (" + StatUtil.roundToString(100F * selectionCount / denom, 2) + "%)";
        setProgText(msg);
        getProgBar().setValue(selectionCount);

        controller.getMonFrame().maintainOptionMenu();

        controller.getMonFrame().saveSelectionAction.setEnabled(selectionCount > 0);
    }


    public void dataChanged(int id) {

        //System.out.println("MonFrame got the event !!!!"+id);

        controller.getMonFrame().maintainOptionMenu();

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
            for (int i = 0; i < plots.size(); i++) {
                (plots.elementAt(i).Selections).removeAllElements();
            }

            for (int i = 0; i < selList.size(); i++) {
                selList.elementAt(i).status = Selection.KILLED;
            }

            updateSelection();
        }
    }


    public void setProgress(double progress) {
        getProgBar().setValue((int) (100 * progress));
    }


    public List<DragBox> getPlots() {
        return plots;
    }


    public void close() {
        if (dialog.isDisplayable())
            dialog.dispose();


        for (int i = plots.size() - 1; i >= 0; i--) {
            plots.elementAt(i).frame.close();
        }
    }


    public VariableSelector getSelector() {
        return dialog.getVarSelector();
    }


    public JProgressBar getProgBar() {
        return dialog.getProgBar();
    }


    public void setProgText(String msg) {
        dialog.getProgText().setText(msg);
    }
}

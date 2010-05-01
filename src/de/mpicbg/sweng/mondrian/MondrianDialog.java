package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.ui.VariableSelector;
import de.mpicbg.sweng.mondrian.util.StatUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MondrianDialog extends MDialog {


    private JProgressBar progBar;
    private JLabel progText;

    private VariableSelector varSelector;
    private Mondrian mondrian;


    public MondrianDialog(MonFrame parentFrame, Mondrian mondrian) {
        super(parentFrame, mondrian);
        this.mondrian = mondrian;

        JPanel content = new JPanel(new BorderLayout());
        getContentPane().add(content);

        varSelector = new VariableSelector(mondrian.getDataSet());
        content.add(varSelector, BorderLayout.CENTER);

        // Add the status/progress bar
        JPanel progPanel = new JPanel();
        progPanel.add("North", progText);
        progBar = new JProgressBar();
        progBar.setMinimum(0);
        progBar.setMaximum(1);
        progBar.setValue(0);
        progBar.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                paintAll(MondrianDialog.this.getGraphics());
            }
        });
        progPanel.add(progBar, BorderLayout.CENTER);

        progText = new JLabel();
        progPanel.add(progText, BorderLayout.WEST);

        content.add(progPanel, BorderLayout.CENTER);

        updateSelectionInfo();
    }


    public void setTitle() {
        setTitle("Mondrian(" + mondrian.getDataSet().setName + ")");               //
    }


    public void updateSelectionInfo() {
        int nom = mondrian.getDataSet().countSelection();
        int denom = mondrian.getDataSet().n;
        String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100 * nom / denom, 2) + "%)";
        progText.setText(Display);
        progBar.setValue(nom);
    }


    public JLabel getProgText() {
        return progText;
    }


    public JProgressBar getProgBar() {
        return progBar;
    }


    public VariableSelector getVarSelector() {
        return varSelector;
    }
}

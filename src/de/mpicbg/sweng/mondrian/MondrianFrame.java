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
public class MondrianFrame extends MFrame {


    private JProgressBar progBar;
    private JLabel progText;

    private VariableSelector varSelector;
    private Mondrian mondrian;


    public MondrianFrame(AppFrame parentFrame, Mondrian mondrian) {
        super(parentFrame, mondrian);
        this.mondrian = mondrian;

        JPanel content = new JPanel(new BorderLayout());

        varSelector = new VariableSelector(mondrian);
        content.add(varSelector, BorderLayout.CENTER);

        // Add the status/progress bar
        JPanel progPanel = new JPanel();
        progBar = new JProgressBar();
        progBar.setMinimum(0);
        progBar.setMaximum(1);
        progBar.setValue(0);
        progBar.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                paintAll(MondrianFrame.this.getGraphics());
            }
        });
        progPanel.add(progBar, BorderLayout.EAST);

        progText = new JLabel();
        progPanel.add(progText, BorderLayout.WEST);

        content.add(progPanel, BorderLayout.SOUTH);

        getContentPane().add(content);
        setTitle(mondrian.getDataSet().setName);
        setSize(300, 400);

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

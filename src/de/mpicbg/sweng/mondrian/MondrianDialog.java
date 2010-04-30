package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.ui.VariableSelector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MondrianDialog extends MDialog {


    private JProgressBar progBar;
    private JLabel progText;

    private VariableSelector varSelector;


    public MondrianDialog(MonFrame parentFrame, Mondrian mondrian) {
        super(parentFrame, mondrian);


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
        progPanel.add("South", progBar);

        add("South", progPanel);
    }


    public JLabel getProgText() {
        return progText;
    }


    public JProgressBar getProgBar() {
        return progBar;
    }
}

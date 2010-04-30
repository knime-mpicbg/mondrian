package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.DataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MonController {

    MonFrame monFrame;


    List<Mondrian> mondrians = new ArrayList<Mondrian>();

    public static Vector<DataSet> dataSets;


    public MonController(MonFrame monFrame) {
        this.monFrame = monFrame;
    }


    public MonFrame getMonFrame() {
        return monFrame;
    }


    public Mondrian getCurrent() {
        return null;
    }


    public DataSet getCurrentDataSet() {
        return getCurrent().getDataSet();
        // before dataSets.elementAt(monFrame.dataSetCounter)
    }


    public int getNumInstances() {
        return mondrians.size();
    }


    public void removeInstance(Mondrian mondrian) {
        mondrians.remove(mondrian);
    }
}

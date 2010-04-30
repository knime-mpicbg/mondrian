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
        return dataSets.elementAt(monFrame.dataSetCounter);
    }
}

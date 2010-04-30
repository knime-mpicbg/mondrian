package de.mpicbg.sweng.mondrian.core;

import de.mpicbg.sweng.mondrian.plots.basic.MyPoly;

import java.util.HashMap;
import java.util.Vector;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class MapCache {

    HashMap<DataSet, Vector<MyPoly>> cache = new HashMap<DataSet, Vector<MyPoly>>();

    private static MapCache singelton;


    public static MapCache getInstance() {
        if (singelton == null) {
            singelton = new MapCache();
        }

        return singelton;

    }


    private MapCache() {
    }


    public Vector<MyPoly> getPolys(DataSet dataSet) {
        if (!cache.containsKey(dataSet)) {
            cache.put(dataSet, new Vector<MyPoly>());
        }

        return cache.get(dataSet);
    }
}

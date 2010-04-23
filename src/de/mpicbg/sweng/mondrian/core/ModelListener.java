package de.mpicbg.sweng.mondrian.core;

import de.mpicbg.sweng.mondrian.Table;


public interface ModelListener {

    public void updateModel(Table t, String[] n, int l);
}

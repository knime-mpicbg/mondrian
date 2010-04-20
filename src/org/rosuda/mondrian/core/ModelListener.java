package org.rosuda.mondrian.core;

import org.rosuda.mondrian.Table;


public interface ModelListener {

    public void updateModel(Table t, String[] n, int l);
}

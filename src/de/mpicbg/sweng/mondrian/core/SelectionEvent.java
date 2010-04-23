package de.mpicbg.sweng.mondrian.core;

import java.awt.*;         // New event model.


public class SelectionEvent extends AWTEvent {

    public SelectionEvent(DragBox s) {

        super(s, SELECTION_EVENT);

    }


    private static final int SELECTION_EVENT = AWTEvent.RESERVED_ID_MAX + 1;

}


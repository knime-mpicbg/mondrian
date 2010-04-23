package de.mpicbg.sweng.mondrian.core;

import java.awt.*;         // New event model.


public class DataEvent extends AWTEvent {

    public DataEvent(DragBox b) {
        super(b, DATA_EVENT);
    }


    private static final int DATA_EVENT = AWTEvent.RESERVED_ID_MAX + 3;
}

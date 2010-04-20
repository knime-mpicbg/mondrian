package org.rosuda.mondrian;

import org.rosuda.mondrian.DragBox;

import java.awt.*;         // New event model.


class DataEvent extends AWTEvent {

    public DataEvent(DragBox b) {
        super(b, DATA_EVENT);
    }


    public static final int DATA_EVENT = AWTEvent.RESERVED_ID_MAX + 3;
}

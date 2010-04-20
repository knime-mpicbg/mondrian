package org.rosuda.mondrian;

import java.awt.*;         // New event model.


class SelectionEvent extends AWTEvent {

    public SelectionEvent(DragBox s) {

        super(s, SELECTION_EVENT);

    }


    public static final int SELECTION_EVENT = AWTEvent.RESERVED_ID_MAX + 1;

}


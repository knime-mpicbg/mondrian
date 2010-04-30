package de.mpicbg.sweng.mondrian.util;

import de.mpicbg.sweng.mondrian.plots.MosaicPlot;

import java.awt.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ModelEvent extends AWTEvent {

    public ModelEvent(MosaicPlot m) {
        super(m, MODEL_EVENT);
    }


    public static final int MODEL_EVENT = AWTEvent.RESERVED_ID_MAX + 2;
}

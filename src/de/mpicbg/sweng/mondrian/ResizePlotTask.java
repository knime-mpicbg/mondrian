package de.mpicbg.sweng.mondrian;

import de.mpicbg.sweng.mondrian.core.DragBox;

import java.util.TimerTask;


final class ResizePlotTask extends TimerTask {

    private DragBox DB;


    /**
     * Creates a new instance of ResizePlotTask
     */
    ResizePlotTask(final DragBox DB) {
        this.DB = DB;
    }


    public void run() {
        if (DB != null && !DB.painting) {
            DB.resizeReady = true;
            DB.paint(DB.getGraphics());
        }

    }

}
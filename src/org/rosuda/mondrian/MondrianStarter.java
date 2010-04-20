package org.rosuda.mondrian;

import com.apple.mrj.*;

import java.io.File;
import java.util.Vector;


public class MondrianStarter implements MRJOpenDocumentHandler {

    protected Vector dataSets = new Vector(5, 5);
    public Vector Mondrians = new Vector(5, 5);


    /**
     * A very simple main() method for our program.
     */
    public static void main(String[] args) {
        new MondrianStarter(args);
    }


    public MondrianStarter(String[] args) {

        Join First = new Join(Mondrians, dataSets, false, false, null);

//    System.out.println(" Join Created / Register Handler ...");

        MRJApplicationUtils.registerOpenDocumentHandler(this);

        if (args.length == 1) {
            File iFile = new File(args[0]);
            if (iFile.canRead()) First.loadDataSet(false, iFile, "");
        }

        try {
            // put it to sleep "forever" ...
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
        }
    }


    public void handleOpenFile(File inFile) {
        Join theJoin = ((Join) Mondrians.lastElement());
//    while( !theJoin.mondrianRunning ) {System.out.println(" wait for Mondrian to initialize ...");}   // Wait until Mondrian initialized
//    System.out.println(".......... CALL loadDataSet("+inFile+") FROM handleOpenFile .........");
        theJoin.loadDataSet(false, inFile, "");
    }

}

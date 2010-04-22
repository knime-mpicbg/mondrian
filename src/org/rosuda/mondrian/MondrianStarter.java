package org.rosuda.mondrian;

import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;

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

        MonFrame first = new MonFrame(Mondrians, dataSets, false, false, null);

//    System.out.println(" MonFrame Created / Register Handler ...");

        MRJApplicationUtils.registerOpenDocumentHandler(this);

        if (args.length == 1) {
            File iFile = new File(args[0]);
            if (iFile.canRead()) first.loadDataSet(false, iFile, "");
        }

        try {
            // put it to sleep "forever" ...
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }


    public void handleOpenFile(File inFile) {
        MonFrame theMonFrame = ((MonFrame) Mondrians.lastElement());
//    while( !theMonFrame.mondrianRunning ) {System.out.println(" wait for Mondrian to initialize ...");}   // Wait until Mondrian initialized
//    System.out.println(".......... CALL loadDataSet("+inFile+") FROM handleOpenFile .........");
        theMonFrame.loadDataSet(false, inFile, "");
    }

}

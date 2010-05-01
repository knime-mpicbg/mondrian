package de.mpicbg.sweng.mondrian;

import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;
import de.mpicbg.sweng.mondrian.io.DataFrameConverter;

import java.io.File;
import java.util.Vector;


public class MondrianStarter implements MRJOpenDocumentHandler {


    /**
     * A very simple main() method for our program.
     */
    public static void main(String[] args) {
        new MondrianStarter(args.length > 0 ? args[0] : null);
    }


    public MondrianStarter(String dataFileName) {

        MonFrame monFrame = new MonFrame();


        MRJApplicationUtils.registerOpenDocumentHandler(this);

        if (dataFileName != null) {
            File dataFile = new File(dataFileName);

            if (dataFile.canRead()) {
                if (dataFile.getName().endsWith(".RData")) {
                    new DataFrameConverter(monFrame).loadDataFrame(dataFile);
                } else {
                    monFrame.getController().loadDataSet(dataFile, dataFile.getName());
                }
            }
        }

        try {
            // put it to sleep "forever" ...
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }


    public void handleOpenFile(File inFile) {
        // this can not work!!

        MonFrame theMonFrame = ((MonFrame) new Vector(5, 5).lastElement());
//    while( !theMonFrame.mondrianRunning ) {System.out.println(" wait for Mondrian to initialize ...");}   // Wait until Mondrian initialized
//    System.out.println(".......... CALL loadDataSet("+inFile+") FROM handleOpenFile .........");
        theMonFrame.getController().loadDataSet(inFile, "");
    }
}

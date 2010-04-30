package de.mpicbg.sweng.mondrian;

import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.io.DataFrameConverter;
import de.mpicbg.sweng.mondrian.plots.*;

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

        System.err.println("file");
        MonFrame monFrame = new MonFrame(new Vector<MonFrame>(), new Vector<DataSet>(), false, false, null);


        monFrame.regiserPlotFactory(new BarchartFactory());
        monFrame.regiserPlotFactory(new WeightedBarCharFactory());
        monFrame.regiserPlotFactory(new HistogramFactory());
        monFrame.regiserPlotFactory(new WeightedHistogramFactory());
        monFrame.regiserPlotFactory(new PCAPlotFactory());
        monFrame.regiserPlotFactory(new MissPlotFactory());
        monFrame.regiserPlotFactory(new MosaicPlotFactory());
        monFrame.regiserPlotFactory(new WeightedMosaicPlotFactory());
        monFrame.regiserPlotFactory(new ScatterplotFactory());
        monFrame.regiserPlotFactory(new SplomFactory());
        monFrame.regiserPlotFactory(new TwoDimMDSFactory());

        monFrame.regiserPlotFactory(new BoxplotByXYFactory());
        monFrame.regiserPlotFactory(new ParallelBoxplotFactory());
        monFrame.regiserPlotFactory(new ParallelPlotFactory());


//    System.out.println(" MonFrame Created / Register Handler ...");

        MRJApplicationUtils.registerOpenDocumentHandler(this);

        if (dataFileName != null) {
            File dataFile = new File(dataFileName);

            if (dataFile.canRead()) {
                if (dataFile.getName().endsWith(".RData")) {
                    new DataFrameConverter(monFrame).loadDataFrame(dataFile);
                } else {
                    monFrame.loadDataSet(false, dataFile, "");
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
        MonFrame theMonFrame = ((MonFrame) new Vector(5, 5).lastElement());
//    while( !theMonFrame.mondrianRunning ) {System.out.println(" wait for Mondrian to initialize ...");}   // Wait until Mondrian initialized
//    System.out.println(".......... CALL loadDataSet("+inFile+") FROM handleOpenFile .........");
        theMonFrame.loadDataSet(false, inFile, "");
    }

}

package de.mpicbg.sweng.mondrian;

import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.io.DataFrameConverter;
import de.mpicbg.sweng.mondrian.plots.*;

import javax.swing.*;
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

        MonFrame monFrame = new MonFrame(new Vector<MonFrame>(), new Vector<DataSet>(), false, false, null);

        monFrame.registerPlotFactory(new MissPlotFactory());
        monFrame.plotMenu.add(new JSeparator());

        monFrame.registerPlotFactory(new BarchartFactory());
        monFrame.registerPlotFactory(new WeightedBarCharFactory());
        monFrame.plotMenu.add(new JSeparator());

        monFrame.registerPlotFactory(new HistogramFactory());
        monFrame.registerPlotFactory(new WeightedHistogramFactory());
        monFrame.plotMenu.add(new JSeparator());

        monFrame.registerPlotFactory(new ScatterplotFactory());
        monFrame.registerPlotFactory(new SplomFactory());
        monFrame.plotMenu.add(new JSeparator());


        monFrame.registerPlotFactory(new MosaicPlotFactory());
        monFrame.registerPlotFactory(new WeightedMosaicPlotFactory());
        monFrame.plotMenu.add(new JSeparator());

        monFrame.registerPlotFactory(new BoxplotByXYFactory());
        monFrame.registerPlotFactory(new ParallelBoxplotFactory());
        monFrame.registerPlotFactory(new ParallelPlotFactory());
        monFrame.plotMenu.add(new JSeparator());

        monFrame.registerPlotFactory(new TwoDimMDSFactory());
        monFrame.registerPlotFactory(new MapPlotFactory());
        monFrame.registerPlotFactory(new PCAPlotFactory());


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

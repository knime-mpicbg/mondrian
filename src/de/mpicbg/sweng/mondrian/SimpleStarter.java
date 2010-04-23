package de.mpicbg.sweng.mondrian;

import java.io.File;
import java.util.Vector;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class SimpleStarter {

    public static void main(String[] args) {
        MonFrame first = new MonFrame(new Vector(5, 5), new Vector(5, 5), false, false, null);

//    System.out.println(" MonFrame Created / Register Handler ...");

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

}

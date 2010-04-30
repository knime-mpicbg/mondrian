package de.mpicbg.sweng.mondrian.io;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.plots.basic.MyPoly;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class AsciiFileLoader {

    private String justFile = "";


    private MonFrame monFrame;


    public AsciiFileLoader(MonFrame monFrame) {
        this.monFrame = monFrame;
    }


    public boolean loadAsciiFile(File file) {

        DataSet data;
        String filename = "";
        String path = "";

        if (file == null) {
            FileDialog f = new FileDialog(monFrame, "Load Data", FileDialog.LOAD);
            //      JFileChooser f = new JFileChooser(this, "Load Data", FileDialog.LOAD);
            f.setFile("");
            f.show();
            //System.out.println("->"+f.getDirectory()+"<-.->" + f.getFile());
            if (f.getFile() != null) {
                justFile = f.getFile();
                path = f.getDirectory();
                filename = f.getDirectory() + justFile;
            } else
                filename = "";
        } else {
            filename = file.getAbsolutePath();
            justFile = file.getName();
            path = file.getParent() + File.separator;
        }

        if (filename.equals("")) {
            return false;
        }
        String line = "";

        monFrame.getProgBar().setMinimum(0);
        monFrame.getProgBar().setMaximum(100);
        data = new DataSet(justFile);
        MonController.dataSets.addElement(data);
        monFrame.getProgText().setText("Loading ...");

        String mapFile = data.turboRead(filename, monFrame);
        if (mapFile == null)
            JOptionPane.showMessageDialog(monFrame, "No mapfile found although an index column\nwas specified via '/P'.");
        else if (mapFile.indexOf("ERROR") == 0) {
            JOptionPane.showMessageDialog(monFrame, mapFile.substring(mapFile.indexOf(":") + 2), "Open File Error", JOptionPane.ERROR_MESSAGE);
            monFrame.getProgText().setText("");
            monFrame.setProgress(0.0);
            return false;
        }

        monFrame.getProgText().setText("");
        monFrame.getProgBar().setValue(0);
        monFrame.getProgBar().setMaximum(data.n);

        monFrame.selectBuffer = new int[data.k + 15];

        if (mapFile != null)
            if (!mapFile.equals(""))
                if ((new File(path + mapFile).exists())) {                          // more lines detected -> read the polygon
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(path + mapFile));
                        br.mark(1000000);
                        monFrame.getProgText().setText("Polygons ...");

                        double xMin = 10e10;
                        double xMax = -10e10;
                        double yMin = 10e10;
                        double yMax = -10e10;

                        String tLine = br.readLine();
                        try {
                            StringTokenizer head = new StringTokenizer(tLine, "\t");

                            try {
//                                int Id = Integer.valueOf(head.nextToken());
//                                String name = head.nextToken();

                                int npoints = Integer.valueOf(head.nextToken());
                                double[] x = new double[npoints];
                                double[] y = new double[npoints];

                                for (int i = 0; i < npoints; i++) {
                                    tLine = br.readLine();
                                    StringTokenizer coord = new StringTokenizer(tLine);
                                    x[i] = Float.valueOf(coord.nextToken());
                                    xMin = Math.min(xMin, x[i]);
                                    xMax = Math.max(xMax, x[i]);
                                    y[i] = Float.valueOf(coord.nextToken());
                                    yMin = Math.min(yMin, y[i]);
                                    yMax = Math.max(yMax, y[i]);
                                }
                            }
                            catch (NoSuchElementException e) {
                                System.out.println("Poly Read Error: " + line);
                            }
                        }
                        catch (IOException e) {
                            System.out.println("Error: " + e);
                            System.exit(1);
                        }

                        br.reset();
                        int count = 0;
                        while (line != null) {
                            MyPoly p = new MyPoly();
                            p.read(br, xMin, 100000 / Math.min(xMax - xMin, yMax - yMin), yMin, 100000 / Math.min(xMax - xMin, yMax - yMin));
                            if (count++ % Math.max(data.n / 20, 1) == 0)
                                monFrame.getProgBar().setValue(Math.min(count, data.n));
                            monFrame.polys.addElement(p);
                            line = br.readLine();                          // Read seperator (single blank line)
                        }
                    }
                    catch (IOException e) {
                        System.out.println("Error: " + e);
                        System.exit(1);
                    }
                } else {
                    JOptionPane.showMessageDialog(monFrame, "Can't open mapfile: " + mapFile + "\nPlease check file name and location\n(the datafile will still be loaded)");
                }

        return true;
    }

    // old reader
//    private void readWithOldReader() {
//          boolean[] alpha;
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(filename));
//            data = new DataSet(justFile);
//            dataSets.addElement(data);
//            progText.setText("Peeking ...");
//            alpha = data.sniff(br);
//            progBar.setMaximum(data.n);
//            br = new BufferedReader(new FileReader(filename));
//            progText.setText("Loading ...");
//            data.read(br, alpha, progBar);
//
//            br.mark(1000000);
//            line = br.readLine();
//
//            while (line != null && (line.trim()).equals("")) {       // skip empty lines
//                br.mark(1000000);
//                line = br.readLine();
//            }
//
//            if (line != null) {                          // more lines detected -> read the polygon
//
//                progText.setText("Polygons ...");
//
//                //====================== Check Scaling of the Polygon ===============================//
//                String tLine;
//
//                double xMin = 10e10;
//                double xMax = -10e10;
//                double yMin = 10e10;
//                double yMax = -10e10;
//
//                try {
//                    tLine = line;
//
//                    StringTokenizer head = new StringTokenizer(tLine, "\t");
//
//                    try {
//                        int Id = Integer.valueOf(head.nextToken()).intValue();
//                        String name = head.nextToken();
//                        int npoints = Integer.valueOf(head.nextToken()).intValue();
//                        double[] x = new double[npoints];
//                        double[] y = new double[npoints];
//
//                        for (int i = 0; i < npoints; i++) {
//                            tLine = br.readLine();
//                            StringTokenizer coord = new StringTokenizer(tLine);
//                            x[i] = Float.valueOf(coord.nextToken()).floatValue();
//                            xMin = Math.min(xMin, x[i]);
//                            xMax = Math.max(xMax, x[i]);
//                            y[i] = Float.valueOf(coord.nextToken()).floatValue();
//                            yMin = Math.min(yMin, y[i]);
//                            yMax = Math.max(yMax, y[i]);
//                        }
//                        //                  System.out.println("Read: "+npoints+" Points - xMin: "+xMin+"xMax: "+xMax+"yMin: "+yMin+"yMax: "+yMax);
//                    }
//                    catch (NoSuchElementException e) {
//                        System.out.println("Poly Read Error: " + line);
//                    }
//                }
//                catch (IOException e) {
//                    System.out.println("Error: " + e);
//                    System.exit(1);
//                }
//                //==================================================================//
//
//                br.reset();
//                int count = 0;
//                while (line != null) {
//                    MyPoly p = new MyPoly();
//                    p.read(br, xMin, 100000 / Math.min(xMax - xMin, yMax - yMin), yMin, 100000 / Math.min(xMax - xMin, yMax - yMin));
//                    if (count++ % Math.max(data.n / 20, 1) == 0)
//                        progBar.setValue(Math.min(count, data.n));
//                    //MyPoly newP = p.thinHard();
//                    polys.addElement(p);
//                    line = br.readLine();                          // Read seperator (single blank line)
//                }
//            }
//        }
//
//        catch (IOException e) {
//            System.out.println("Error: " + e);
//            System.exit(1);
//        }
//        progText.setText("");
//        progBar.setValue(0);
//    }

}

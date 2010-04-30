package de.mpicbg.sweng.mondrian;


import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJOpenDocumentHandler;
import com.apple.mrj.MRJQuitHandler;
import de.mpicbg.sweng.mondrian.core.*;
import de.mpicbg.sweng.mondrian.io.AsciiFileLoader;
import de.mpicbg.sweng.mondrian.io.DataFrameConverter;
import de.mpicbg.sweng.mondrian.io.ProgressIndicator;
import de.mpicbg.sweng.mondrian.io.db.DBDatasetLoader;
import de.mpicbg.sweng.mondrian.io.db.Query;
import de.mpicbg.sweng.mondrian.plots.*;
import de.mpicbg.sweng.mondrian.plots.basic.MyPoly;
import de.mpicbg.sweng.mondrian.ui.AttributeCellRenderer;
import de.mpicbg.sweng.mondrian.ui.PlotAction;
import de.mpicbg.sweng.mondrian.ui.PreferencesFrame;
import de.mpicbg.sweng.mondrian.util.StatUtil;
import de.mpicbg.sweng.mondrian.util.Util;
import de.mpicbg.sweng.mondrian.util.r.RService;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;
import java.util.prefs.Preferences;


public class MonFrame extends JFrame implements ProgressIndicator, SelectionListener, DataListener, MRJQuitHandler, MRJOpenDocumentHandler {

    /**
     * Remember # of open windows so we can quit when last one is closed
     */
    protected static int num_windows = 0;
    public static Vector<DataSet> dataSets;
    protected static Vector<MonFrame> mondrians;
    private Vector<DragBox> plots = new Vector<DragBox>(10, 0);

    private java.util.List<PlotFactory> plotFacRegistry = new ArrayList<PlotFactory>();

    public Vector<Selection> selList = new Vector<Selection>(10, 0);
    public Query sqlConditions;
    public boolean selseq = false;
    public boolean alphaHi = false;
    public Vector<MyPoly> polys = new Vector<MyPoly>(256, 256);
    public JList varNames = null;
    private int numCategorical = 0;
    private int weightIndex = 0;
    private JScrollPane scrollPane;
    private JProgressBar progBar;
    private JLabel progText;
    public JMenuBar menubar;
    public JMenu windows, help, dv, sam, trans;


    // plot menu items // todo these should all go into the factory
    private JMenuItem mosaicPlotMenuItem;
    private JMenuItem weightedMosaicPlotMenuItem;
    private JMenuItem closeDataSetMenuItem;
    private JMenuItem splotMenuItem;
    private JMenuItem mapPlotMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem saveSelectionMenuItem;
    private JMenuItem missingValuePlotMenuItem;
    private JMenuItem modelNavigatorButton;
    private JMenuItem barchartMenuItem;
    private JMenuItem weightedBarchartMenuItem;
    private JMenuItem parCoordinatesMenuItem;
    private JMenuItem parallelBoxplotMenuItem;
    private JMenuItem boxplotByXYMenuItem;
    private JMenuItem scatterplotMenuItem;
    private JMenuItem histogramMenuItem;
    private JMenuItem weightedHistogramMenuItem;
    private JMenuItem twoDimMDSMenuItem;
    private JMenuItem pcaMenuItem;

    public JMenuItem closeAllMenuItem, colorsMenuItem, selectionMenuItem, me, transPlus, transMinus, transTimes, transDiv, transNeg, transInv, transLog, transExp;
    private JCheckBoxMenuItem selSeqCheckItem;
    private JCheckBoxMenuItem alphaOnHighlightCheckMenuItem;
    private JCheckBoxMenuItem orSelectionCheckMenuItem;
    private JCheckBoxMenuItem andSelectionCheckMenuItem;

    private ModelNavigator modelNavigator;
    public int dataSetCounter = -1;
    private int dCol = 1, dSel = 1;
    private int graphicsPerf;
    static String user;
    public boolean mondrianRunning = false;
    public int[] selectBuffer;
    private String searchText = "";
    private long startT = 0;
    private Vector<Integer> setIndices = new Vector<Integer>(10, 0);
    public JMenu plotMenu;


    public MonFrame(Vector<MonFrame> mondrians, Vector<DataSet> dataSets, boolean load, boolean loadDB, File loadFile) {

        mondrians.addElement(this);

        MRJApplicationUtils.registerOpenDocumentHandler(this);

        //    System.out.println("........... Creating new Instance of MonFrame .........");


        Toolkit.getDefaultToolkit().setDynamicLayout(false);

        MRJApplicationUtils.registerQuitHandler(this);

        PreferencesFrame.setScheme(2);

        // Read Preferences
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());

        if (!prefs.get("color.background", "").equals("")) {
            MFrame.backgroundColor = Util.hrgb2color(prefs.get("color.background", ""));
            MFrame.objectColor = Util.hrgb2color(prefs.get("color.objects", ""));
            MFrame.lineColor = Util.hrgb2color(prefs.get("color.line", ""));
            DragBox.hiliteColor = Util.hrgb2color(prefs.get("color.select", ""));
        }

        // Start Rserve
        System.out.println("Starting RServe ... ");
        RService.init();

        user = System.getProperty("user.name");
        System.out.println(user + " on " + System.getProperty("os.name"));

//    Properties sysProps = System.getProperties();
//    sysProps.list(System.out);

        Font SF = new Font("SansSerif", Font.BOLD, 12);
        this.setFont(SF);
        MonFrame.dataSets = dataSets;
        MonFrame.mondrians = mondrians;
        this.setTitle("Mondrian");               // Create the window.
        num_windows++;                           // Count it.

        menubar = new JMenuBar();         // Create a menubar.

        // Create menu items, with menu shortcuts, and add to the menu.
        JMenu file = menubar.add(new JMenu("File"));
        //   JMenu file = new JMenu("File");            // Create a File menu.
        JMenuItem o;
        file.add(o = new JMenuItem("Open"));
        o.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenuItem openRDataFrameMenuItem;
        file.add(openRDataFrameMenuItem = new JMenuItem("Open R dataframe"));
        openRDataFrameMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenuItem openDataBaseMenuItem;
        file.add(openDataBaseMenuItem = new JMenuItem("Open Database"));
        openDataBaseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        if (user.indexOf("theus") > -1 || true) {
            openDataBaseMenuItem.setEnabled(true);
        } else {
            openDataBaseMenuItem.setEnabled(false);
        }

        file.add(saveMenuItem = new JMenuItem("Save"));
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveMenuItem.setEnabled(false);

        file.add(saveSelectionMenuItem = new JMenuItem("Save Selection"));

        saveSelectionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveSelectionMenuItem.setEnabled(false);

        file.add(closeDataSetMenuItem = new JMenuItem("Close Dataset"));
        closeDataSetMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        closeDataSetMenuItem.setEnabled(false);

        //    file.add(p = new JMenuItem("Print Window",new JMenuShortcut(KeyEvent.VK_P)));
        JMenuItem q = new JMenuItem("Quit");
        if (((System.getProperty("os.name")).toLowerCase()).indexOf("mac") == -1) {
            file.addSeparator();                     // Put a separator in the menu
            file.add(q);
            q.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            q.addActionListener(new ActionListener() {     // Quit the program.


                public void actionPerformed(ActionEvent e) {
                    try {                                                                                // Shut down RServe if running ...
                        RConnection c = new RConnection();
                        c.shutdown();
                    } catch (RserveException ignored) {
                    }
                    System.exit(0);
                }
            });
        }
        menubar.add(file);                         // Add to menubar.
        //
        plotMenu = new JMenu("Plot");
        plotMenu.add(missingValuePlotMenuItem = new JMenuItem("Missing Value Plot"));
        missingValuePlotMenuItem.setEnabled(false);
        plotMenu.addSeparator();
        plotMenu.add(barchartMenuItem = new JMenuItem("Barchart"));
        barchartMenuItem.setEnabled(false);
        plotMenu.add(weightedBarchartMenuItem = new JMenuItem("Weighted Barchart"));
        weightedBarchartMenuItem.setEnabled(false);
        plotMenu.addSeparator();
        plotMenu.add(histogramMenuItem = new JMenuItem("Histogram"));
        histogramMenuItem.setEnabled(false);
        plotMenu.add(weightedHistogramMenuItem = new JMenuItem("Weighted Histogram"));
        weightedHistogramMenuItem.setEnabled(false);
        plotMenu.addSeparator();
        plotMenu.add(scatterplotMenuItem = new JMenuItem("Scatterplot"));
        scatterplotMenuItem.setEnabled(false);
        plotMenu.addSeparator();
        plotMenu.add(mosaicPlotMenuItem = new JMenuItem("Mosaic Plot"));
        mosaicPlotMenuItem.setEnabled(false);
        plotMenu.add(weightedMosaicPlotMenuItem = new JMenuItem("Weighted Mosaic Plot"));
        weightedMosaicPlotMenuItem.setEnabled(false);
        plotMenu.addSeparator();
        plotMenu.add(parCoordinatesMenuItem = new JMenuItem("Parallel Coordinates"));
        parCoordinatesMenuItem.setEnabled(false);
        plotMenu.add(parallelBoxplotMenuItem = new JMenuItem("Parallel Boxplot"));
        parallelBoxplotMenuItem.setEnabled(false);
        plotMenu.add(boxplotByXYMenuItem = new JMenuItem("Boxplot y by x"));
        boxplotByXYMenuItem.setEnabled(false);
        plotMenu.addSeparator();
        if (true || user.indexOf("theus") > -1) {
            plotMenu.add(splotMenuItem = new JMenuItem("SPLOM"));
            splotMenuItem.setEnabled(false);
            plotMenu.addSeparator();                     // Put a separator in the menu
        }
        plotMenu.add(mapPlotMenuItem = new JMenuItem("Map"));
        mapPlotMenuItem.setEnabled(false);
        menubar.add(plotMenu);                         // Add to menubar.
        //
        JMenu calc = new JMenu("Calc");            // Create a Calc menu.
        calc.add(trans = new JMenu("transform"));
        trans.setEnabled(false);
        trans.add(transPlus = new JMenuItem("x + y"));
        trans.add(transMinus = new JMenuItem("x - y"));
        trans.add(transTimes = new JMenuItem("x * y"));
        trans.add(transDiv = new JMenuItem("x / y"));
        trans.addSeparator();                     // Put a separator in the menu
        trans.add(transNeg = new JMenuItem("- x"));
        trans.add(transInv = new JMenuItem("1/x"));
        trans.add(transLog = new JMenuItem("log(x)"));
        trans.add(transExp = new JMenuItem("exp(x)"));

        calc.add(twoDimMDSMenuItem = new JMenuItem("2-dim MDS"));
        twoDimMDSMenuItem.setEnabled(false);
        //
        calc.add(pcaMenuItem = new JMenuItem("PCA"));
        pcaMenuItem.setEnabled(false);
        //
        menubar.add(calc);                         // Add to menubar.

        JMenu options = new JMenu("Options");      // Create an Option menu.
        JMenuItem sa;
        options.add(sa = new JMenuItem("Select All"));
        sa.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenuItem ts;
        options.add(ts = new JMenuItem("Toggle Selection"));
        ts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        options.add(sam = new JMenu("<SHIFT><ALT> is"));
        sam.add(orSelectionCheckMenuItem = new JCheckBoxMenuItem("OR Selection"));
        sam.add(andSelectionCheckMenuItem = new JCheckBoxMenuItem("AND Selection"));
        andSelectionCheckMenuItem.setSelected(true);

        options.addSeparator();                     // Put a separator in the menu
        JMenuItem cc;
        options.add(cc = new JMenuItem("Clear all Colors"));
        cc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        options.addSeparator();                     // Put a separator in the menu
        options.add(selSeqCheckItem = new JCheckBoxMenuItem("Selection Sequences", selseq));
        selSeqCheckItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenuItem cs;
        options.add(cs = new JMenuItem("Clear Sequences"));
        cs.setAccelerator(KeyStroke.getKeyStroke(Event.BACK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        options.addSeparator();                     // Put a separator in the menu
        options.add(alphaOnHighlightCheckMenuItem = new JCheckBoxMenuItem("Alpha on Highlight", alphaHi));
        alphaOnHighlightCheckMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        options.addSeparator();                     // Put a separator in the menu
        JMenuItem vm;
        options.add(vm = new JMenuItem("Switch Variable Mode"));
        vm.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        options.add(dv = new JMenu("Derive Variable from"));
        dv.add(selectionMenuItem = new JMenuItem("Selection"));
        selectionMenuItem.setEnabled(false);
        dv.add(colorsMenuItem = new JMenuItem("Colors"));
        colorsMenuItem.setEnabled(false);

        options.addSeparator();                     // Put a separator in the menu
        options.add(modelNavigatorButton = new JMenuItem("Model Navigator", KeyEvent.VK_J));
        modelNavigatorButton.setEnabled(false);

        options.addSeparator();                     // Put a separator in the menu
        JMenuItem pr;
        options.add(pr = new JMenuItem("Preferences ...", KeyEvent.VK_K));
        pr.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        menubar.add(options);                      // Add to menubar.

        windows = menubar.add(new JMenu("Window"));

        windows.add(closeAllMenuItem = new JMenuItem("Close All"));
        closeAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        closeAllMenuItem.setEnabled(false);

        windows.addSeparator();

        windows.add(me = new JMenuItem(this.getTitle()));

        help = menubar.add(new JMenu("Help"));

        JMenuItem rc;
        help.add(rc = new JMenuItem("Reference Card"));
        rc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        rc.setEnabled(true);

        JCheckBoxMenuItem ih;
        help.add(ih = new JCheckBoxMenuItem("Interactive Help"));
        ih.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        ih.setEnabled(false);

        JMenuItem oh;
        help.add(oh = new JMenuItem("Online Help"));
        oh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        oh.setEnabled(true);

        this.setJMenuBar(menubar);                 // Add it to the frame.

        Icon MondrianIcon = new ImageIcon(Util.readGif("/Logo.gif"));

        JLabel MondrianLabel = new JLabel(MondrianIcon);
        scrollPane = new JScrollPane(MondrianLabel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add("Center", scrollPane);

        // Add the status/progress bar
        JPanel progPanel = new JPanel();
        progText = new JLabel("   Welcome !    ");
        progPanel.add("North", progText);
        progBar = new JProgressBar();
        progBar.setMinimum(0);
        progBar.setMaximum(1);
        progBar.setValue(0);
        progBar.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                showIt();
            }
        });
        progPanel.add("South", progBar);

        getContentPane().add("South", progPanel);

        // Create and register action listener objects for the menu items.
        missingValuePlotMenuItem.addActionListener(new ActionListener() {     // Open a new missing value plot window


            public void actionPerformed(ActionEvent e) {
                missPlot();
            }
        });
        mosaicPlotMenuItem.addActionListener(new ActionListener() {     // Open a new mosaic plot window


            public void actionPerformed(ActionEvent e) {

                mosaicPlot();
            }
        });
        weightedMosaicPlotMenuItem.addActionListener(new ActionListener() {     // Open a new weighted mosaic plot window


            public void actionPerformed(ActionEvent e) {
                weightedMosaicPlot();
            }
        });
        barchartMenuItem.addActionListener(new ActionListener() {     // Open a new mosaic plot window


            public void actionPerformed(ActionEvent e) {
                barChart();
            }
        });
        weightedBarchartMenuItem.addActionListener(new ActionListener() {     // Open a new mosaic plot window


            public void actionPerformed(ActionEvent e) {
                weightedbarChart();
            }
        });
        histogramMenuItem.addActionListener(new ActionListener() {     // Open a histogram window


            public void actionPerformed(ActionEvent e) {
                histogram();
            }
        });
        weightedHistogramMenuItem.addActionListener(new ActionListener() {     // Open a weighted histogram window


            public void actionPerformed(ActionEvent e) {
                weightedHistogram();
            }
        });
        parCoordinatesMenuItem.addActionListener(new ActionListener() {     // Open a parallel coordinate plot window


            public void actionPerformed(ActionEvent e) {
                pc("Poly");
            }
        });
        parallelBoxplotMenuItem.addActionListener(new ActionListener() {     // Open a parallel boxplot plot window


            public void actionPerformed(ActionEvent e) {
                pc("Box");
            }
        });
        boxplotByXYMenuItem.addActionListener(new ActionListener() {     // Open a boxplot plot y by x window


            public void actionPerformed(ActionEvent e) {
                pc("Box");
            }
        });
        scatterplotMenuItem.addActionListener(new ActionListener() {     // Open a scatterplot window


            public void actionPerformed(ActionEvent e) {
                scatterplot2D();
            }
        });
        if (true || user.indexOf("theus") > -1)
            splotMenuItem.addActionListener(new ActionListener() {     // Open a new test window


                public void actionPerformed(ActionEvent e) {
                    SPLOM();
                }
            });
        o.addActionListener(new ActionListener() {     // Load a dataset


            public void actionPerformed(ActionEvent e) {
                //        System.out.println(".......... CALL loadDataSet() FROM Open .........");
                loadDataSet(false, null, "");
            }
        });
        openRDataFrameMenuItem.addActionListener(new ActionListener() {     // Load a dataset


            public void actionPerformed(ActionEvent e) {
                //        System.out.println(".......... CALL loadDataFrame() FROM Open .........");
                new DataFrameConverter(MonFrame.this).loadDataFrame();
            }
        });
        saveMenuItem.addActionListener(new ActionListener() {     // Save the current dataset


            public void actionPerformed(ActionEvent e) {
                Save(false);
            }
        });
        saveSelectionMenuItem.addActionListener(new ActionListener() {     // Save the current selection


            public void actionPerformed(ActionEvent e) {
                Save(true);
            }
        });
        openDataBaseMenuItem.addActionListener(new ActionListener() {     // Load a database


            public void actionPerformed(ActionEvent e) {
                loadDataSet(true, null, "");
            }
        });
        mapPlotMenuItem.addActionListener(new ActionListener() {     // Open a new window to draw an interactive maps


            public void actionPerformed(ActionEvent e) {
                mapPlot();
            }
        });
        twoDimMDSMenuItem.addActionListener(new ActionListener() {     // Open a new window for a 2-dim MDS


            public void actionPerformed(ActionEvent e) {
                mds();
            }
        });
        pcaMenuItem.addActionListener(new ActionListener() {     // calculate PCA


            public void actionPerformed(ActionEvent e) {
                pca();
            }
        });
        transPlus.addActionListener(new ActionListener() {     // x + y


            public void actionPerformed(ActionEvent e) {
                transform(1);
            }
        });
        transMinus.addActionListener(new ActionListener() {     // x - y


            public void actionPerformed(ActionEvent e) {
                transform(2);
            }
        });
        transTimes.addActionListener(new ActionListener() {     // x * y


            public void actionPerformed(ActionEvent e) {
                transform(3);
            }
        });
        transDiv.addActionListener(new ActionListener() {     // x / y


            public void actionPerformed(ActionEvent e) {
                transform(4);
            }
        });
        transNeg.addActionListener(new ActionListener() {     // - x


            public void actionPerformed(ActionEvent e) {
                transform(5);
            }
        });
        transInv.addActionListener(new ActionListener() {     // 1/x


            public void actionPerformed(ActionEvent e) {
                transform(6);
            }
        });
        transLog.addActionListener(new ActionListener() {     // log(x)


            public void actionPerformed(ActionEvent e) {
                transform(7);
            }
        });
        transExp.addActionListener(new ActionListener() {     // exp(x)


            public void actionPerformed(ActionEvent e) {
                transform(8);
            }
        });
        selSeqCheckItem.addActionListener(new ActionListener() {     // Change the selection mode


            public void actionPerformed(ActionEvent e) {
                switchSelection();
            }
        });
        selectionMenuItem.addActionListener(new ActionListener() {     // Derive variable from selection (false) or color (true)


            public void actionPerformed(ActionEvent e) {
                deriveVariable(false);
            }
        });
        colorsMenuItem.addActionListener(new ActionListener() {     // Derive variable from selection (false) or color (true)


            public void actionPerformed(ActionEvent e) {
                deriveVariable(true);
            }
        });
        orSelectionCheckMenuItem.addActionListener(new ActionListener() {     // Set extended selection mode AND (false) or OR (true)


            public void actionPerformed(ActionEvent e) {
                setExtSelMode(true);
            }
        });
        andSelectionCheckMenuItem.addActionListener(new ActionListener() {     // Set extended selection mode AND (false) or OR (true)


            public void actionPerformed(ActionEvent e) {
                setExtSelMode(false);
            }
        });
        sa.addActionListener(new ActionListener() {     // Select All via Menu


            public void actionPerformed(ActionEvent e) {
                selectAll();
            }
        });
        ts.addActionListener(new ActionListener() {     // Toggle Selection via Menu


            public void actionPerformed(ActionEvent e) {
                toggleSelection();
            }
        });
        cc.addActionListener(new ActionListener() {     // Clear all Colors


            public void actionPerformed(ActionEvent e) {
                clearColors();
            }
        });
        alphaOnHighlightCheckMenuItem.addActionListener(new ActionListener() {     // Change the alpha mode for highlighted cases


            public void actionPerformed(ActionEvent e) {
                switchAlpha();
            }
        });
        modelNavigatorButton.addActionListener(new ActionListener() {     // Open a new window for the model navigator


            public void actionPerformed(ActionEvent e) {
                showModeNavigator();
            }
        });
        pr.addActionListener(new ActionListener() {     // Open the Preference Box


            public void actionPerformed(ActionEvent e) {
                preferenceFrame();
            }
        });
        cs.addActionListener(new ActionListener() {     // Delete the current selection sequence


            public void actionPerformed(ActionEvent e) {
                deleteSelection();
            }
        });
        vm.addActionListener(new ActionListener() {     // Delete the current selection sequence


            public void actionPerformed(ActionEvent e) {
                switchVariableMode();
            }
        });
        closeAllMenuItem.addActionListener(new ActionListener() {     // Close all Windows


            public void actionPerformed(ActionEvent e) {
                closeAll();
            }
        });
        closeDataSetMenuItem.addActionListener(new ActionListener() {     // Close this window.


            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        me.addActionListener(new ActionListener() {   // Show main window


            public void actionPerformed(ActionEvent e) {
                toFront();
            }
        });
        rc.addActionListener(new ActionListener() {     // Show reference card window.


            public void actionPerformed(ActionEvent e) {
                Util.showRefCard(MonFrame.this);
            }
        });
        oh.addActionListener(new ActionListener() {     // Show Mondrian Webpage.


            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URL("http://www.rosuda.org/Mondrian").toURI());
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (URISyntaxException e11) {
                    e11.printStackTrace();
                }

            }
        });

        // Another event listener, this one to handle window close requests.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        this.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                topWindow();
            }
        });

        // Set the window size and pop it up.
        this.setResizable(false);
        this.setSize(295, 320);
        this.show();

        if (dataSets.isEmpty())
            graphicsPerf = setGraphicsPerformance();
        else if (dataSets.firstElement().graphicsPerf != 0)
            graphicsPerf = dataSets.firstElement().graphicsPerf;
        else
            graphicsPerf = 25000;

        Graphics g = this.getGraphics();
        g.setFont(new Font("SansSerif", 0, 11));
        g.drawString("v1.1", 260, 285);

        mondrianRunning = true;

        if (!RService.hasR()) {
            //      JOptionPane.showMessageDialog(this, "Connection to R failed:\nSome functions might be missing!\n\nPlease check installation of R and  Rserve\nor try starting Rserve manually ...","Rserve Error",JOptionPane.WARNING_MESSAGE);
            g.setColor(Color.white);
            g.fillRect(9, 275, 220, 14);
            g.setColor(Color.gray);
            g.drawString("Connection to R failed: Please check Rserve", 9, 285);
        }

        if (load)
            if (loadDB)
                loadDataSet(true, null, "");
            else {
                //        System.out.println(".......... CALL loadDataSet() FROM MonFrame .........");
                loadDataSet(false, loadFile, "");
            }
    }


    public JProgressBar getProgBar() {
        return progBar;
    }


    /**
     * this constructor is useful to create a Mondrian instance with a pre-loaded dataset. It will initialize Mondrians
     * and dataSets if necessary, otherwise the dataset will be added to the existing static list.
     *
     * @param data dataset to open
     */
    public MonFrame(DataSet data) {
        this((mondrians == null) ? new Vector<MonFrame>(5, 5) : mondrians, (dataSets == null) ? new Vector<DataSet>(5, 5) : dataSets, false, false, null);

        initWithData(data);
    }


    /**
     * adds a dataset and makes is current.
     *
     * @param data dataset to use
     */
    public void initWithData(DataSet data) {
        dataSets.addElement(data);
        dataSetCounter = dataSets.size() - 1;
        selectBuffer = new int[data.k + 15];
        setVarList();
        this.setTitle("Mondrian(" + dataSets.elementAt(dataSetCounter).setName + ")");               //
        me.setText(this.getTitle());
        closeDataSetMenuItem.setEnabled(true);
        saveMenuItem.setEnabled(true);

        int nom = dataSets.elementAt(dataSetCounter).countSelection();
        int denom = dataSets.elementAt(dataSetCounter).n;
        String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100 * nom / denom, 2) + "%)";
        progText.setText(Display);
        progBar.setValue(nom);

        maintainOptionMenu();
    }


    public JLabel getProgText() {
        return progText;
    }


    public void handleQuit() {
        System.exit(0);
    }


    void showIt() {
        paintAll(this.getGraphics());
    }


    int setGraphicsPerformance() {

        int graphicsPerf = 0;
        Image testI = createImage(200, 200);        //
        Graphics2D gI = (Graphics2D) testI.getGraphics();
        gI.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((float) 0.05)));
        long start = new java.util.Date().getTime();
        while (new java.util.Date().getTime() - start < 1000) {
            graphicsPerf++;
            gI.fillOval(10, 10, 3, 3);
        }
        System.out.println("Graphics Performance: " + graphicsPerf);

        return graphicsPerf;
    }


    /**
     * Close a window.  If this is the last open window, just quit.
     */
    void close() {
        // Modal dialog with OK button

        if (dataSetCounter == -1) {
            this.dispose();
            if (--num_windows == 0)
                System.exit(0);
            return;
        }

        String message = "Close dataset \"" + dataSets.elementAt(dataSetCounter).setName + "\" and\n all corresponding plots?";

        int answer = JOptionPane.showConfirmDialog(this, message);
        if (answer == JOptionPane.YES_OPTION) {
            num_windows--;
            for (int i = plots.size() - 1; i >= 0; i--)
                plots.elementAt(i).frame.close();
            dataSets.setElementAt(new DataSet("nullinger"), dataSetCounter);
            this.dispose();
            if (num_windows == 0) {
                new MonFrame(mondrians, dataSets, false, false, null);
                //        System.out.println(" -----------------------> disposing MonFrame !!!!!!!!!!!!!!!!");
                this.dispose();
            }
        }
    }


    public void closeAll() {
        for (int i = plots.size() - 1; i >= 0; i--) {
            plots.elementAt(i).frame.close();
            plots.removeElementAt(i);
        }
    }


    public void transform(int mode) {
        checkHistoryBuffer();

        System.out.println("Transform: " + mode);
        String name = "";
        DataSet data = dataSets.elementAt(dataSetCounter);

        double[] tData = new double[data.n];
        boolean[] tMiss = new boolean[data.n];
        String name1 = data.getName(selectBuffer[1]);
        String name2 = data.getName(selectBuffer[0]);
        switch (mode) {
            case 1:
                name = name1 + " + " + name2;
                break;
            case 2:
                name = name1 + " - " + name2;
                break;
            case 3:
                name = name1 + " * " + name2;
                break;
            case 4:
                name = name1 + " / " + name2;
                break;
            case 5:
                name = "-" + name2;
                break;
            case 6:
                name = "1/" + name2;
                break;
            case 7:
                name = "log(" + name2 + ")";
                break;
            case 8:
                name = "exp(" + name2 + ")";
                break;
        }
        double[] var1 = data.getRawNumbers(selectBuffer[1]);
        double[] var2 = data.getRawNumbers(selectBuffer[0]);
        boolean[] miss1 = data.getMissings(selectBuffer[1]);
        boolean[] miss2 = data.getMissings(selectBuffer[0]);
        for (int i = 0; i < data.n; i++) {
            if (miss2[i] || ((mode < 5) && (miss1[i] || miss2[i])))
                tMiss[i] = true;
            else
                tMiss[i] = false;
            switch (mode) {
                case 1:
                    tData[i] = var1[i] + var2[i];
                    break;
                case 2:
                    tData[i] = var1[i] - var2[i];
                    break;
                case 3:
                    tData[i] = var1[i] * var2[i];
                    break;
                case 4:
                    if (var2[i] != 0)
                        tData[i] = var1[i] / var2[i];
                    else
                        tMiss[i] = true;
                    break;
                case 5:
                    tData[i] = -var2[i];
                    break;
                case 6:
                    if (var2[i] != 0)
                        tData[i] = 1 / var2[i];
                    else
                        tMiss[i] = true;
                    break;
                case 7:
                    if (var2[i] > 0)
                        tData[i] = Math.log(var2[i]);
                    else
                        tMiss[i] = true;
                    break;
                case 8:
                    tData[i] = Math.exp(var2[i]);
                    break;
            }
        }
        for (int i = 0; i < data.n; i++)
            if (tMiss[i])
                tData[i] = Double.MAX_VALUE;
        boolean what;
        if (mode < 5)
            what = data.categorical(selectBuffer[0]) && data.categorical(selectBuffer[1]);
        else
            what = data.categorical(selectBuffer[0]);
        data.addVariable(name, false, what, tData, tMiss);
        varNames = null;
        setVarList();
    }


    public void switchSelection() {
        if (dataSetCounter > -1 && dataSets.elementAt(dataSetCounter).isDB)
            selseq = true;
        else {
            selseq = selSeqCheckItem.isSelected();
            //System.out.println("Selection Sequences : "+selseq);
            if (!selseq)
                deleteSelection();
        }
    }


    public void switchAlpha() {
        alphaHi = alphaOnHighlightCheckMenuItem.isSelected();
        updateSelection();
    }


    public void selectAll() {
        if (dataSetCounter > -1) {
            dataSets.elementAt(dataSetCounter).selectAll();
            updateSelection();
        }
    }


    public void toggleSelection() {
        if (dataSetCounter > -1) {
            dataSets.elementAt(dataSetCounter).toggleSelection();
            updateSelection();
        }
    }


    public void clearColors() {
        if (dataSetCounter > -1) {
            dataSets.elementAt(dataSetCounter).colorsOff();
            dataChanged(-1);
        }
    }


    public void setExtSelMode(boolean mode) {
        DragBox.extSelMode = mode;
        orSelectionCheckMenuItem.setSelected(mode);
        andSelectionCheckMenuItem.setSelected(!mode);
    }


    public void deriveVariable(boolean color) {

        String name;
        DataSet data = dataSets.elementAt(dataSetCounter);
        if (color)
            name = "Colors " + dCol++;
        else
            name = "Selection " + dSel++;
        name = JOptionPane.showInputDialog(this, "Please name the new variable:", name);

        double[] dData;
        if (color) {
            dData = new double[data.n];
            for (int i = 0; i < data.n; i++)
                dData[i] = (double) data.colorArray[i];
        } else {
            dData = data.getSelection();
        }
        data.addVariable(name, false, true, dData, new boolean[data.n]);
        varNames = null;
        setVarList();
    }


    public void deleteSelection() {
        if (selList.size() > 0) {
            for (int i = 0; i < plots.size(); i++)
                (plots.elementAt(i).Selections).removeAllElements();
            for (int i = 0; i < selList.size(); i++)
                selList.elementAt(i).status = Selection.KILLED;
            maintainWindowMenu(false);
            updateSelection();
        }
    }


    public void updateSelection() {
        // Remove Selections from list, which are no longer active
        //
        boolean selectAll = false;
        boolean unSelect = false;
        boolean toggleSelection = false;

        for (int i = 0; i < plots.size(); i++) {
            if (plots.elementAt(i).selectAll) {    // This window has caused the select all event
                plots.elementAt(i).selectAll = false;
                selectAll = true;
            }
            if (plots.elementAt(i).unSelect) {    // This window has caused the un select event
                plots.elementAt(i).unSelect = false;
                unSelect = true;
            }
            if (plots.elementAt(i).toggleSelection) {    // This window has caused the toggle selection event
                plots.elementAt(i).toggleSelection = false;
                toggleSelection = true;
            }
            if (plots.elementAt(i).deleteAll) {    // This window has caused the deletion event
                plots.elementAt(i).deleteAll = false;
                deleteSelection();
                return;
            }
            if (plots.elementAt(i).switchSel) {    // This window has caused the switch event
                plots.elementAt(i).switchSel = false;
                selSeqCheckItem.setSelected(!selSeqCheckItem.isSelected());                // perform the tick mark change manually ...
                switchSelection();
                return;
            }
            if (plots.elementAt(i).switchAlpha) {    // This window has caused the switch alpha event
                plots.elementAt(i).switchAlpha = false;
                alphaOnHighlightCheckMenuItem.setSelected(!alphaOnHighlightCheckMenuItem.isSelected());
                switchAlpha();
                plots.elementAt(i).updateSelection();
                return;
            }
        }

        if (!(unSelect || selectAll || toggleSelection)) {

            for (int i = selList.size() - 1; i >= 0; i--) {
                if ((selList.elementAt(i).status == Selection.KILLED) ||
                        !selList.elementAt(i).d.frame.isVisible()) {
                    selList.removeElementAt(i);
                }
            }

            selList.trimToSize();

            Selection oneClick = null;

            // Get the latest selection and add it, if its a new selection
            //
            for (int i = 0; i < plots.size(); i++)
                if (plots.elementAt(i).frame.isVisible()) {  // Plotwindow still exists
                    if (plots.elementAt(i).selectFlag) {       // This window has caused the selection event
                        plots.elementAt(i).selectFlag = false;    // We need to get the last selection from this plot
                        Selection S = (Selection) (((DragBox) plots.elementAt(i)).Selections.lastElement());
                        if (selList.indexOf(S) == -1) { // Not in the list yet => new Selection to add !
                            if (!(S.r.width < 3 || S.r.height < 3) && selseq) {
                                System.out.println("Selection Sequence  !!");
                                S.step = selList.size() + 1;
                                selList.addElement(S);
                            } else {
                                oneClick = S;
                                System.out.println("Click Selection  !!");
                                oneClick.status = Selection.KILLED;
                                plots.elementAt(i).Selections.removeElementAt(plots.elementAt(i).Selections.size() - 1);
                            }
                        }
                    }
                } else
                    plots.removeElementAt(i--);

            if (selList.size() > 1) {
                selList.firstElement().mode = Selection.MODE_STANDARD;
            }
            // Do the update over all selections
            //
            if (oneClick != null) {
                //  This is a oneClick selection -> make it visible for Java 1.4 ...
                oneClick.r.width += 1;
                oneClick.r.height += 1;
                (oneClick.d).maintainSelection(oneClick);
            } else {
                maintainWindowMenu(false);

                for (int i = 0; i < selList.size(); i++) {
                    Selection S = selList.elementAt(i);
                    S.step = i + 1;
                    S.total = selList.size();
                    (S.d).maintainSelection(S);
                    (S.d).frame.maintainMenu(S.step);
                }
            }
            sqlConditions = new Query();                // Replace ???
            if (dataSets.elementAt(dataSetCounter).isDB)
                for (int i = 0; i < selList.size(); i++) {
                    Selection S = selList.elementAt(i);
                    if (S.mode == Selection.MODE_STANDARD)
                        sqlConditions.clearConditions();
                    String condStr = S.condition.getConditions();
                    if (!condStr.equals(""))
                        sqlConditions.addCondition(Selection.getSQLModeString(S.mode), "(" + condStr + ")");
                }
            dataSets.elementAt(dataSetCounter).sqlConditions = sqlConditions;

            //      System.out.println("Main Update: "+sqlConditions.makeQuery());

        } else {
            if (toggleSelection) {
                System.out.println(" TOGGLE SELECTION ... ");
                dataSets.elementAt(dataSetCounter).toggleSelection();
            } else if (unSelect) {
                System.out.println(" UNSELECT ... ");
                dataSets.elementAt(dataSetCounter).clearSelection();
            } else {
                System.out.println(" SELECT ALL ... ");
                dataSets.elementAt(dataSetCounter).selectAll();
            }
            if (dataSets.elementAt(dataSetCounter).isDB)
                sqlConditions.clearConditions();
        }

        // Finally get the plots updated
        //
        for (int i = 0; i < plots.size(); i++) {
            //     progText.setText("Query: "+i);
            progBar.setValue(1);
            plots.elementAt(i).updateSelection();
        }

        dataSets.elementAt(dataSetCounter).selChanged = true;
        int nom = dataSets.elementAt(dataSetCounter).countSelection();
        int denom = dataSets.elementAt(dataSetCounter).n;
        String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100F * nom / denom, 2) + "%)";
        progText.setText(Display);
        progBar.setValue(nom);

        maintainOptionMenu();

        if (nom > 0)
            saveSelectionMenuItem.setEnabled(true);
        else
            saveSelectionMenuItem.setEnabled(false);
    }


    public void dataChanged(int id) {

        //System.out.println("MonFrame got the event !!!!"+id);

        maintainOptionMenu();

        System.out.println("Key Event in MonFrame");

        // First check whether a color has been set individually
        for (int i = 0; i < plots.size(); i++) {
            int col = plots.elementAt(i).colorSet;
            if (col > -1) {
                plots.elementAt(i).colorSet = -1;
                DataSet data = dataSets.elementAt(dataSetCounter);
                id = -1;
                if (col < 999) {
                    System.out.println("Setting Colors !!!!");
                    int retCol = data.addColor(col);
                    double selections[] = data.getSelection();
                    for (int j = 0; j < data.n; j++)
                        if (selections[j] != 0)
                            data.setColor(j, retCol);
                } else
                    data.colorsOff();
            }
        }
        // Then ordinary update loop
        for (int i = 0; i < plots.size(); i++)
            if (plots.elementAt(i).frame.isVisible())  // Plotwindow still exists
                if (plots.elementAt(i).dataFlag)         // This window was already updated
                    plots.elementAt(i).dataFlag = false;
                else
                    plots.elementAt(i).dataChanged(id);
            else
                plots.removeElementAt(i);
    }


    public void Save(boolean selection) {
        checkHistoryBuffer();

        FileDialog f;
        if (selection)
            f = new FileDialog(this, "Save Selection", FileDialog.SAVE);
        else
            f = new FileDialog(this, "Save Data", FileDialog.SAVE);
        f.show();
        if (f.getFile() != null)
            saveDataSet(f.getDirectory() + f.getFile(), selection);
    }


    public boolean saveDataSet(String file, boolean selection) {
        try {
            int k = dataSets.elementAt(dataSetCounter).k;
            int n = dataSets.elementAt(dataSetCounter).n;

            FileWriter fw = new FileWriter(file);

            double[][] dataCopy = new double[k][n];
            boolean[][] missing = new boolean[k][n];
            DataSet data = dataSets.elementAt(dataSetCounter);
            double[] selected = data.getSelection();
            for (int j = 0; j < k; j++) {
                missing[j] = data.getMissings(j);
                if (data.categorical(j) && !data.alpha(j))
                    dataCopy[j] = data.getRawNumbers(j);
                else
                    dataCopy[j] = data.getNumbers(j);
            }

            String line = "";
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "US"));
            DecimalFormat df = (DecimalFormat) nf;
            df.applyPattern("#.#################");

            boolean first = true;
            for (int j = 0; j < k; j++)
                if ((varNames.getSelectedIndices().length == 0) || varNames.isSelectedIndex(j)) {
                    line += (first ? "" : "\t") + data.getName(j);
                    first = false;
                }
            fw.write(line + "\r");

            for (int i = 0; i < n; i++) {
                if (!selection || (selection && selected[i] > 0)) {
                    line = "";
                    first = true;
                    for (int j = 0; j < k; j++)
                        if ((varNames.getSelectedIndices().length == 0) || varNames.isSelectedIndex(j)) {
                            if (missing[j][i])
                                line += (first ? "" : "\t") + "NA";
                            else if (data.categorical(j))
                                line += (first ? "" : "\t") + data.getLevelName(j, dataCopy[j][i]);
                            else
                                line += (first ? "" : "\t") + df.format(dataCopy[j][i]);
                            first = false;
                        }
                    fw.write(line + (i == (n - 1) ? "" : "\r"));
                }
            }
            fw.close();

        } catch (Exception ex) {
            System.out.println("Error writing to file: " + ex);
            return false;
        }
        return true;
    }


    public void loadDataSet(boolean isDB, File file, String title) {

        //    System.out.println(".......... IN loadDataSet("+thisDataSet+") IN .........");

        if (isDB) {
            new DBDatasetLoader(this).loadDataBase();
        } else if (dataSetCounter == -1) {
            if (new AsciiFileLoader(this).loadAsciiFile(file)) {
                setVarList();
                if (title.equals(""))
                    this.setTitle("Mondrian(" + dataSets.elementAt(dataSetCounter).setName + ")");               //
                else
                    this.setTitle("Mondrian(" + title + ")");
                me.setText(this.getTitle());
                closeDataSetMenuItem.setEnabled(true);
                saveMenuItem.setEnabled(true);

                int nom = dataSets.elementAt(dataSetCounter).countSelection();
                int denom = dataSets.elementAt(dataSetCounter).n;
                String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100 * nom / denom, 2) + "%)";
                progText.setText(Display);
                progBar.setValue(nom);

                maintainOptionMenu();
            }
        } else {
            new MonFrame(mondrians, dataSets, true, isDB, file);
        }
        if (dataSetCounter != -1)
            dataSets.elementAt(dataSetCounter).graphicsPerf = graphicsPerf;
    }


    public void setVarList() {
        if (varNames != null) {
            paint(this.getGraphics());
            return;
        }
        if (dataSetCounter == -1)
            dataSetCounter = dataSets.size() - 1;
        final DataSet data = dataSets.elementAt(dataSetCounter);
        String listNames[] = new String[data.k];
        for (int j = 0; j < data.k; j++) {
            listNames[j] = " " + data.getName(j);
            //      System.out.println("Adding:"+listNames[j]);
        }

        varNames = new JList(listNames);
        scrollPane.setViewportView(varNames);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setWheelScrollingEnabled(true);

        varNames.setBackground(new Color(222, 222, 222));

        varNames.requestFocus();

        varNames.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = varNames.locationToIndex(e.getPoint());
                    if (!data.alpha(index)) {
                        if (data.categorical(index))
                            data.catToNum(index);
                        else
                            data.numToCat(index);
                        setVarList();
                        maintainPlotMenu();
                    }
                } else {
                    int index = varNames.locationToIndex(e.getPoint());
                    System.out.println("Shift " + e.isShiftDown());
                    System.out.print("Item Selected: " + index);
                    int diff = 0;
                    if (e.isShiftDown()) {
                        diff = selectBuffer[0] - index;
                        diff -= (diff < 0 ? -1 : 1);
                        System.out.println(" diff " + diff);
                    }
                    for (int j = Math.abs(diff); j >= 0; j--) {
                        if (varNames.isSelectedIndex(index) && index != selectBuffer[0]) {
                            for (int i = data.k - 1; i > 0; i--)
                                selectBuffer[i] = selectBuffer[i - 1];
                            selectBuffer[0] = index + (j * (diff < 0 ? -1 : 1));
                        }
                        if (!varNames.isSelectedIndex(index)) {              // Deselection, remove elements from Buffer
                            for (int i = 0; i < data.k; i++)
                                if (selectBuffer[i] == index) {
                                    System.arraycopy(selectBuffer, i + 1, selectBuffer, i, data.k - 1 - i);
                                }
                        }
                        System.out.println(" History: " + selectBuffer[0] + " " + selectBuffer[1] + " " + selectBuffer[2] + " " + selectBuffer[3] + " " + selectBuffer[4]);
                    }
                    maintainPlotMenu();
                }
            }
        });

        varNames.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                maintainPlotMenu();
            }
        });

        varNames.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {

                if (e.getModifiers() != Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                        && (Character.isSpaceChar(e.getKeyChar()) || Character.isJavaLetterOrDigit(e.getKeyChar()) || (e.getKeyChar() == KeyEvent.VK_PERIOD) || (e.getKeyChar() == KeyEvent.VK_MINUS))) {
                    setIndices.removeAllElements();
                    if (searchText.equals(""))
                        startT = new Date().getTime();
                    if (new Date().getTime() < startT + 1000) {
                        searchText += e.getKeyChar();
                    } else {
                        searchText = "" + e.getKeyChar();
                    }
                    startT = new Date().getTime();
                    //         System.out.println("Search Text: "+searchText+" Position: "+(scrollPane.getVerticalScrollBar()).getValue());
                    if (!searchText.equals(""))
                        for (int i = 0; i < data.k; i++) {
                            String tmp = data.getName(i);
                            if ((tmp.toUpperCase()).startsWith((searchText.toUpperCase())))
                                setIndices.addElement(i);
                        }
                    if (setIndices.size() > 0) {
                        int[] setArray = new int[setIndices.size()];
                        for (int i = 0; i < setIndices.size(); i++)
                            setArray[i] = setIndices.elementAt(i);
                        varNames.setSelectedIndices(setArray);
                        varNames.ensureIndexIsVisible(setArray[setIndices.size() - 1]);
                        varNames.ensureIndexIsVisible(setArray[0]);
                    }
                }
            }
        });

        varNames.setCellRenderer(new AttributeCellRenderer(this));

        RepaintManager currentManager = RepaintManager.currentManager(varNames);
        currentManager.setDoubleBufferingEnabled(true);

        if (polys.size() > 0)
            mapPlotMenuItem.setEnabled(true);

        this.setResizable(true);

        this.show();
    }


    public void setProgress(double progress) {
        progBar.setValue((int) (100 * progress));
    }

    /*  public void handleReOpenApplication(ApplicationEvent event) {}

public void handleQuit(ApplicationEvent event) {}

public void handlePrintFile(ApplicationEvent event) {} */


    public int[] getWeightVariable(int[] vars, DataSet data) {

        if (numCategorical == (vars).length - 1) {
            int[] returner = new int[vars.length];
            System.arraycopy(vars, 0, returner, 0, returner.length);

            for (int i = 0; i < returner.length - 1; i++) {
                if (vars[i] == weightIndex) {
                    System.arraycopy(vars, i + 1, returner, i, returner.length - 1 - i);

                    returner[returner.length - 1] = weightIndex;
                    i = returner.length;

                } else
                    returner[i] = vars[i];
            }
            for (int i = 0; i < returner.length; i++) {
                System.out.println("ind old = " + vars[i] + " ind new = " + returner[i]);
            }

            return returner;

        } else {
            final Dialog countDialog = new Dialog(this, " Choose Weight Variable", true);
            Choice getCount = new Choice();

            for (int j = 0; j < vars.length; j++) {
                if (data.getName(vars[j]).length() > 1 && data.getName(vars[j]).substring(0, 1).equals("/"))
                    getCount.addItem(data.getName(vars[j]).substring(2));
                else
                    getCount.addItem(data.getName(vars[j]));
            }
            for (int j = 0; j < getCount.getItemCount(); j++)
                if (getCount.getItem(j).toLowerCase().equals("count") ||
                        getCount.getItem(j).toLowerCase().equals("counts") ||
                        getCount.getItem(j).toLowerCase().equals("n") ||
                        getCount.getItem(j).toLowerCase().equals("weight") ||
                        getCount.getItem(j).toLowerCase().equals("observed") ||
                        getCount.getItem(j).toLowerCase().equals("number"))
                    getCount.select(j);
            Panel p1 = new Panel();
            p1.add(getCount);
            countDialog.add(p1, "Center");
            Button OK = new Button("OK");
            Panel p2 = new Panel();
            p2.add(OK);
            countDialog.add(p2, "South");
            OK.addActionListener(new ActionListener() {     //


                public void actionPerformed(ActionEvent e) {
                    countDialog.dispose();
                }
            });
            countDialog.pack();
            if (countDialog.getWidth() < 240)
                countDialog.setSize(240, countDialog.getHeight());
            countDialog.setResizable(false);
            countDialog.setModal(true);
            countDialog.setBounds(this.getBounds().x + this.getBounds().width / 2 - countDialog.getBounds().width / 2,
                    this.getBounds().y + this.getBounds().height / 2 - countDialog.getBounds().height / 2,
                    countDialog.getBounds().width,
                    countDialog.getBounds().height);
            countDialog.show();

            String[] selecteds = new String[(varNames.getSelectedValues()).length];
            for (int i = 0; i < (varNames.getSelectedValues()).length; i++) {
                selecteds[i] = (String) (varNames.getSelectedValues())[i];
            }

            int[] returner = new int[vars.length];
            for (int i = 0; i < vars.length; i++) {
                if ((selecteds[i].trim()).equals(getCount.getSelectedItem())) {
                    returner[vars.length - 1] = vars[i];
                    System.arraycopy(vars, i + 1, returner, i, vars.length - 1 - i);
                    i = vars.length;

                } else
                    returner[i] = vars[i];
            }
            return returner;
        }
    }


    public void showModeNavigator() {
        if (modelNavigator == null)
            modelNavigator = new ModelNavigator();
        else
            modelNavigator.show();
    }


    public void preferenceFrame() {
        PreferencesFrame.showPrefsDialog(this);
    }


    public void SPLOM() {
        checkHistoryBuffer();

        int p = (varNames.getSelectedIndices()).length;
        final MFrame scatterMf = new MFrame(this);
        int dims = Math.min(200 * p, (Toolkit.getDefaultToolkit().getScreenSize()).height);
        scatterMf.setSize(dims - 20, dims);
        scatterMf.getContentPane().setLayout(new GridLayout(p - 1, p - 1));

        for (int i = 0; i < (p - 1); i++)
            for (int j = 1; j < p; j++) {
                if (i >= j) {
                    JPanel Filler = new JPanel();
                    Filler.setBackground(MFrame.backgroundColor);
                    scatterMf.getContentPane().add(Filler);
                    //          (Filler.getGraphics()).drawString("text",10,10);
                } else {
                    int[] tmpVars = new int[2];
                    //          tmpVars[0] = varNames.getSelectedIndices()[j];
                    //          tmpVars[1] = varNames.getSelectedIndices()[i];
                    tmpVars[0] = selectBuffer[p - j - 1];
                    tmpVars[1] = selectBuffer[p - i - 1];
                    //
                    Scatter2DPlot scat = new Scatter2DPlot(scatterMf, 200, 200, dataSets.elementAt(dataSetCounter), tmpVars, varNames, true);
                    scat.addSelectionListener(this);
                    scat.addDataListener(this);
                    plots.addElement(scat);
                }
            }
        scatterMf.setLocation(300, 0);
        scatterMf.setTitle("Scatterplot Matrix");
        scatterMf.show();
    }


    public void pc(String mode) {
        checkHistoryBuffer();

        final MFrame pC = new MFrame(this);

        int totWidth = (Toolkit.getDefaultToolkit().getScreenSize()).width;
        int tmpWidth = 50 * (1 + (varNames.getSelectedIndices()).length);
        if (tmpWidth > totWidth)
            if (20 * (1 + (varNames.getSelectedIndices()).length) < totWidth)
                tmpWidth = totWidth;
            else
                tmpWidth = 20 * (1 + (varNames.getSelectedIndices()).length);

        pC.setSize(tmpWidth, 400);
        pC.setLocation(300, 0);

        int k = (varNames.getSelectedIndices()).length;
        int[] passTmpBuffer = new int[k];
        int count = 0;
        for (int i = 0; i < k; i++) {
            if (dataSets.elementAt(dataSetCounter).getNumMissings(selectBuffer[k - i - 1]) < dataSets.elementAt(dataSetCounter).n)  // make sure not all data is missing
                passTmpBuffer[count++] = selectBuffer[k - i - 1];
        }
        int[] passBuffer = new int[count];
        System.arraycopy(passTmpBuffer, 0, passBuffer, 0, count);

        ParallelCoordinates plotw = new ParallelCoordinates(pC, dataSets.elementAt(dataSetCounter), passBuffer, mode, varNames);
        plotw.addSelectionListener(this);
        plotw.addDataListener(this);
        plots.addElement(plotw);
        pC.getContentPane().add(plotw);
        pC.show();
    }


    public void missPlot() {
        checkHistoryBuffer();

        final MFrame mV = new MFrame(this);
        int k = 0;
        for (int i = 0; i < (varNames.getSelectedIndices()).length; i++)
            if (dataSets.elementAt(dataSetCounter).n > dataSets.elementAt(dataSetCounter).getN((varNames.getSelectedIndices())[i]))
                k++;
        int[] passVars = new int[k];
        int kk = 0;
        for (int i = 0; i < (varNames.getSelectedIndices()).length; i++)
            if (dataSets.elementAt(dataSetCounter).n > dataSets.elementAt(dataSetCounter).getN(selectBuffer[i]))
                passVars[k - 1 - kk++] = selectBuffer[i]; //(varNames.getSelectedIndices())[i];

        if (k > 0) {
            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
            int tmpHeight = 35 * (1 + k) + 15;
            if (tmpHeight > totHeight)
                if (20 * (1 + k) < totHeight)
                    tmpHeight = totHeight;
                else
                    tmpHeight = 20 * (1 + k);

            mV.setSize(300, Math.min(tmpHeight, (Toolkit.getDefaultToolkit().getScreenSize()).height - 30));
            mV.setLocation(150, 150);

            final MissPlot plotw = new MissPlot(mV, dataSets.elementAt(dataSetCounter), passVars);
            plotw.setScrollX();
            plotw.addSelectionListener(this);
            plotw.addDataListener(this);
            plots.addElement(plotw);
            mV.show();
        } else
            JOptionPane.showMessageDialog(this, "Non of the selected variables\ninclude any missing values");
    }


    public void weightedMosaicPlot() {
        checkHistoryBuffer();

        final MFrame mondrian = new MFrame(this);
        mondrian.setSize(400, 400);

        DataSet data = dataSets.elementAt(dataSetCounter);

        int k = (varNames.getSelectedIndices()).length;
        int[] passBuffer = new int[k];
        for (int i = 0; i < k; i++)
            passBuffer[i] = selectBuffer[k - i - 1];

        //    int[] vars = getWeightVariable(varNames.getSelectedIndices(), data);
        int[] vars = getWeightVariable(passBuffer, data);
        int[] passed = new int[vars.length - 1];
        System.arraycopy(vars, 0, passed, 0, vars.length - 1);
        int weight = vars[vars.length - 1];
        Table breakdown = data.breakDown(data.setName, passed, weight);
        for (int i = 0; i < passed.length - 1; i++)
            breakdown.addInteraction(new int[]{i}, false);
        breakdown.addInteraction(new int[]{passed.length - 1}, true);
        final MosaicPlot plotw = new MosaicPlot(mondrian, 400, 400, breakdown);
        plotw.addSelectionListener(this);
        plotw.addDataListener(this);
        plots.addElement(plotw);
        //    mondrian.getContentPane().add(plotw);                      // Add it
        mondrian.setLocation(300, 0);
        mondrian.show();

        if (modelNavigator == null)
            modelNavigator = new ModelNavigator();
        plotw.addModelListener(modelNavigator);
        modelNavigatorButton.setEnabled(true);
    }


    public void mosaicPlot() {
        checkHistoryBuffer();

        final MFrame mondrian = new MFrame(this);
        mondrian.setSize(400, 400);

        DataSet tempData = dataSets.elementAt(dataSetCounter);

        int k = (varNames.getSelectedIndices()).length;
        int[] passBuffer = new int[k];
        for (int i = 0; i < k; i++)
            passBuffer[i] = selectBuffer[k - i - 1];

        Table breakdown = tempData.breakDown(tempData.setName, passBuffer, -1);
        for (int i = 0; i < (varNames.getSelectedIndices()).length - 1; i++) {
            breakdown.addInteraction(new int[]{i}, false);
        }
        breakdown.addInteraction(new int[]{(varNames.getSelectedIndices()).length - 1}, true);

        final MosaicPlot plotw = new MosaicPlot(mondrian, 400, 400, breakdown);
        plotw.addSelectionListener(this);
        plotw.addDataListener(this);
        plots.addElement(plotw);
        //    mondrian.getContentPane().add(plotw);                      // Add it
        mondrian.setLocation(300, 0);
        mondrian.show();

        mondrian.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                plotw.processWindowEvent(e);
            }
        });

        if (modelNavigator == null)
            modelNavigator = new ModelNavigator();
        plotw.addModelListener(modelNavigator);
        modelNavigatorButton.setEnabled(true);
    }


    public void barChart() {

        DataSet tempData = dataSets.elementAt(dataSetCounter);

        int[] indices = varNames.getSelectedIndices();
        int lastY = 333;
        int col = 0;
        for (int i = 0; i < indices.length; i++) {
            final MFrame bars = new MFrame(this);

            int[] dummy = {0};
            dummy[0] = indices[i];

            Table breakdown = tempData.breakDown(tempData.setName, dummy, -1);

            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
            int tmpHeight = Math.min(totHeight - 30, 60 + breakdown.levels[0] * 30);

            bars.setSize(300, tmpHeight);
            final Barchart plotw = new Barchart(bars, 300, tmpHeight, breakdown);

            plotw.addSelectionListener(this);
            plotw.addDataListener(this);
            plots.addElement(plotw);
            if (lastY + bars.getHeight() > (Toolkit.getDefaultToolkit().getScreenSize()).height) {
                col += 1;
                lastY = 0;
            }
            if (300 * col > (Toolkit.getDefaultToolkit().getScreenSize()).width - 50) {
                col = 0;
                lastY = 353;
            }
            bars.setLocation(300 * col, lastY);

            bars.show();
            if (lastY == 0)
                lastY += bars.getY();
            lastY += bars.getHeight();
        }
    }


    public void weightedbarChart() {

        DataSet tempData = dataSets.elementAt(dataSetCounter);

        int[] vars = getWeightVariable(varNames.getSelectedIndices(), tempData);
        int[] passed = new int[vars.length - 1];
        System.arraycopy(vars, 0, passed, 0, vars.length - 1);
        int weight = vars[vars.length - 1];
        int lastY = 333;
        int col = 0;

        for (int i = 0; i < passed.length; i++) {
            final MFrame bars = new MFrame(this);

            int[] dummy = {0};
            dummy[0] = passed[i];
            Table breakdown = tempData.breakDown(tempData.setName, dummy, weight);

            int totHeight = (Toolkit.getDefaultToolkit().getScreenSize()).height;
            int tmpHeight = Math.min(totHeight - 20, 60 + breakdown.levels[0] * 30);

            bars.setSize(300, tmpHeight);
            final Barchart plotw = new Barchart(bars, 300, tmpHeight, breakdown);

            plotw.addSelectionListener(this);
            plotw.addDataListener(this);
            plots.addElement(plotw);
            if (lastY + bars.getHeight() > (Toolkit.getDefaultToolkit().getScreenSize()).height) {
                col += 1;
                lastY = 0;
            }
            if (300 * col > (Toolkit.getDefaultToolkit().getScreenSize()).width - 50) {
                col = 0;
                lastY = 333;
            }
            bars.setLocation(300 * col, lastY);

            bars.show();
            if (lastY == 0)
                lastY += bars.getY();
            lastY += bars.getHeight();
        }
    }


    public void weightedHistogram() {

        DataSet tempData = dataSets.elementAt(dataSetCounter);

        int[] vars = getWeightVariable(varNames.getSelectedIndices(), tempData);
        if (vars.length > 1) {
            int[] passed = new int[vars.length - 1];
            System.arraycopy(vars, 0, passed, 0, vars.length - 1);
            int weight = vars[vars.length - 1];

            //      System.out.println(passed[0]+", "+weight);

            histoCore(tempData, passed, weight);
        } else
            histoCore(tempData, vars, vars[0]);
    }


    public void histogram() {

        DataSet tempData = dataSets.elementAt(dataSetCounter);
        int[] indices = varNames.getSelectedIndices();

        histoCore(tempData, indices, -1);
    }


    public void histoCore(DataSet tempData, int[] indices, int weight) {
        int lastX = 310, oldX = 0;
        int row = 0;
        int menuOffset = 0, xOff = 0;

        for (int i = 0; i < indices.length; i++) {
            final MFrame hists = new MFrame(this);

            int dummy;
            dummy = indices[i];
            double start = tempData.getMin(dummy);
            double width = (tempData.getMax(dummy) - tempData.getMin(dummy)) / 8.9;
            Table discrete = tempData.discretize(tempData.setName, dummy, start, width, weight);

            hists.setSize(310, 250);
            final Histogram plotw = new Histogram(hists, 250, 310, discrete, start, width, weight);

            plotw.addSelectionListener(this);
            plotw.addDataListener(this);
            plots.addElement(plotw);
            if (lastX + hists.getWidth() > (Toolkit.getDefaultToolkit().getScreenSize()).width + 50) {       // new Row
                row += 1;
                lastX = oldX % 310;
            }
            if (250 * row > (Toolkit.getDefaultToolkit().getScreenSize()).height - 125) {                                    // new Page
                row = 0;
                lastX = 310 + xOff;
                xOff += menuOffset;
            }
            hists.setLocation(lastX, xOff + 250 * row);
            lastX += hists.getWidth();
            oldX = lastX;

            hists.show();
            if (i == 0) {
                menuOffset = hists.getY();
                xOff = menuOffset;
            }
        }
    }


    public void mapPlot() {
        final MFrame mapf = new MFrame(this);
        mapf.setSize(400, 400);
        mapf.setTitle("Map");

        MapPlot mapPlot = new MapPlot(mapf, 400, 400, dataSets.elementAt(dataSetCounter), polys, varNames);
        mapPlot.addSelectionListener(this);
        mapPlot.addDataListener(this);
        plots.addElement(mapPlot);

        if (mapPlot.ratio > 1)
            mapf.setSize((int) (350 * mapPlot.ratio), 350 + 56);
        else
            mapf.setSize(350, (int) (350 / mapPlot.ratio) + 56);
        mapf.setLocation(0, 333);

        mapf.show();
    }


    public void scatterplot2D() {
        checkHistoryBuffer();

        final MFrame scatterf = new MFrame(this);
        scatterf.setSize(400, 400);

        int[] passBuffer = new int[2];
        passBuffer[0] = selectBuffer[1];
        passBuffer[1] = selectBuffer[0];
        Scatter2DPlot scat = new Scatter2DPlot(scatterf, 400, 400, dataSets.elementAt(dataSetCounter), passBuffer, varNames, false);
        scat.addSelectionListener(this);
        scat.addDataListener(this);
        plots.addElement(scat);
        scatterf.setLocation(300, 333);
        scatterf.show();
    }


    public void mds() {

        int[] varsT = varNames.getSelectedIndices();
        DataSet dataT = dataSets.elementAt(dataSetCounter);
        try {
            RConnection c = new RConnection();
            c.voidEval("library(MASS, pos=1)");
            for (int i = 0; i < varsT.length; i++) {
                c.assign("x", dataT.getRawNumbers(varsT[i]));
                if (dataT.n > dataT.getN(varsT[i])) {                      // Check for missings in this variable
                    boolean[] missy = dataT.getMissings(varsT[i]);
                    int[] flag = new int[dataT.n];
                    for (int j = 0; j < dataT.n; j++)
                        if (missy[j])
                            flag[j] = 1;
                        else
                            flag[j] = 0;
                    c.assign("xM", flag);
                    c.voidEval("is.na(x)[xM==1] <- T");
                }
                if (i == 0)
                    c.voidEval("tempData <- x");
                else
                    c.voidEval("tempData <- cbind(tempData, x)");
            }
            c.voidEval("tempD <- dist(scale(tempData))");
            c.voidEval("is.na(tempD)[tempD==0] <- T");
            c.voidEval("startConf <- cmdscale(dist(scale(tempData)), k=2)");
            c.voidEval("sMds <- sammon(tempD, y=startConf, k=2, trace=F)");
            double[] x1 = c.eval("sMds$points[,1]").asDoubles();
            double[] x2 = c.eval("sMds$points[,2]").asDoubles();

            dataT.addVariable("mds1", false, false, x1, new boolean[dataT.n]);
            dataT.addVariable("mds2", false, false, x2, new boolean[dataT.n]);

            final MFrame scatterf = new MFrame(this);
            scatterf.setSize(400, 400);
            scatterf.setTitle("Scatterplot 2D");

            Scatter2DPlot scat = new Scatter2DPlot(scatterf, 400, 400, dataT, new int[]{dataT.k - 2, dataT.k - 1}, varNames, false);
            scat.addSelectionListener(this);
            plots.addElement(scat);
            scatterf.setLocation(300, 333);
            scatterf.show();
        } catch (RserveException rse) {
            System.out.println("Rserve exception: " + rse.getMessage());
        }
        catch (REXPMismatchException mme) {
            System.out.println("Mismatch exception : " + mme.getMessage());
        }
        catch (REngineException ren) {
            System.out.println("REngine exception : " + ren.getMessage());
        }
    }


    public void pca() {

        int[] varsT = varNames.getSelectedIndices();
        DataSet dataT = dataSets.elementAt(dataSetCounter);
        try {
            RConnection c = new RConnection();
            String call = " ~ x1 ";
            for (int i = 0; i < varsT.length; i++) {
                c.assign("x", dataT.getRawNumbers(varsT[i]));
                if (dataT.n > dataT.getN(varsT[i])) {                      // Check for missings in this variable
                    boolean[] missy = dataT.getMissings(varsT[i]);
                    int[] flag = new int[dataT.n];
                    for (int j = 0; j < dataT.n; j++)
                        if (missy[j])
                            flag[j] = 1;
                        else
                            flag[j] = 0;
                    c.assign("xM", flag);
                    c.voidEval("is.na(x)[xM==1] <- T");
                }
                if (i == 0)
                    c.voidEval("tempData <- x");
                else {
                    c.voidEval("tempData <- cbind(tempData, x)");
                    call += " + x" + (i + 1) + "";
                }
            }
            c.voidEval("tempData <- data.frame(tempData)");

            for (int i = 0; i < varsT.length; i++)
                c.voidEval("names(tempData)[" + (i + 1) + "] <- \"x" + (i + 1) + "\"");

            String opt = "TRUE";
            int answer = JOptionPane.showConfirmDialog(this, "Calculate PCA for correlation matrix", "Standardize Data?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION)
                opt = "FALSE";

            c.voidEval("pca <- predict(princomp(" + call + " , data = tempData, cor = " + opt + ", na.action = na.exclude))");
            for (int i = 0; i < varsT.length; i++) {
                double[] x = c.eval("pca[," + (i + 1) + "]").asDoubles();
                boolean missy[] = new boolean[dataT.n];
                for (int j = 0; j < x.length; j++) {
                    if (Double.isNaN(x[j])) {
                        missy[j] = true;
                        x[j] = Double.MAX_VALUE;
                    } else
                        missy[j] = false;
                }
                dataT.addVariable("pca " + (i + 1) + "", false, false, x, missy);
            }
            varNames = null;
            setVarList();
        } catch (RserveException rse) {
            JOptionPane.showMessageDialog(this, "Calculation of PCA failed");
            System.out.println(rse);
        }
        catch (REXPMismatchException rse) {
            JOptionPane.showMessageDialog(this, "Calculation of PCA failed");
            System.out.println(rse);
        }
        catch (REngineException rse) {
            JOptionPane.showMessageDialog(this, "Calculation of PCA failed");
            System.out.println(rse);
        }
    }


    public void switchVariableMode() {
        for (int i = 0; i < varNames.getSelectedIndices().length; i++) {
            int index = (varNames.getSelectedIndices())[i];
            DataSet data = dataSets.elementAt(dataSetCounter);
            if (!data.alpha(index)) {
                if (data.categorical(index))
                    data.catToNum(index);
                else
                    data.numToCat(index);
            }
        }
        setVarList();
        maintainPlotMenu();
    }


    public void getSelectedTypes() {
        numCategorical = 0;
        for (int i = 0; i < varNames.getSelectedIndices().length; i++) {
            if (dataSets.elementAt(dataSetCounter).categorical(varNames.getSelectedIndices()[i]))
                numCategorical++;
            else
                weightIndex = varNames.getSelectedIndices()[i];
        }
    }


    public void checkHistoryBuffer() {

        int k = (varNames.getSelectedIndices()).length;
        boolean error = false;
        boolean[] check = new boolean[k];
        for (int i = 0; i < k; i++)
            check[i] = false;
        /*    for( int i=0; i<k; i++ )
        System.out.print(selectBuffer[i]+", ");
        System.out.println("");
        for( int i=0; i<k; i++ )
        System.out.print(varNames.getSelectedIndices()[i]+", ");
        System.out.println("");
        */
        for (int i = 0; i < k; i++) {
            int match = selectBuffer[i];
            for (int j = 0; j < k; j++)
                if (varNames.getSelectedIndices()[j] == match)
                    if (check[j])
                        error = true;
                    else
                        check[j] = true;
        }
        for (int i = 0; i < k; i++)
            if (!check[i])
                error = true;

        if (error) {
            System.out.println(" Error in Selection History " + k);
            for (int i = 0; i < k; i++)
                selectBuffer[k - i - 1] = varNames.getSelectedIndices()[i];
        }
    }


    public void maintainPlotMenu() {
        if (dataSetCounter == -1) {
            if (dataSets.size() > 0)
                dataSetCounter = dataSets.size() - 1;
            else return; // invalid state, don't bother
        }

        // this updates the counter of the categorical variables
        getSelectedTypes();

        // update the enabled states of all registered plot-factories
        for (int i = 0; i < plotMenu.getMenuComponentCount(); i++) {
            if (!(plotMenu.getMenuComponent(i) instanceof JMenuItem)) {
                continue;
            }

            JMenuItem menuItem = (JMenuItem) plotMenu.getMenuComponent(i);
            if (menuItem.getAction() instanceof PlotAction) {
                ((PlotAction) menuItem.getAction()).configureForVarSelection((varNames.getSelectedIndices()).length, numCategorical);
            }
        }

        //    System.out.println("number categorical: "+numCategorical+", weight Index "+weightIndex);

        switch ((varNames.getSelectedIndices()).length) {
            case 0:
                mosaicPlotMenuItem.setEnabled(false);
                barchartMenuItem.setEnabled(false);
                weightedBarchartMenuItem.setEnabled(false);
                weightedMosaicPlotMenuItem.setEnabled(false);
                histogramMenuItem.setEnabled(false);
                weightedHistogramMenuItem.setEnabled(false);
                parCoordinatesMenuItem.setEnabled(false);
                parallelBoxplotMenuItem.setEnabled(false);
                //              sc.setEnabled(false);
                scatterplotMenuItem.setEnabled(false);
                twoDimMDSMenuItem.setEnabled(false);
                pcaMenuItem.setEnabled(false);
                missingValuePlotMenuItem.setEnabled(false);
                splotMenuItem.setEnabled(false);
                break;
            case 1:
                if (numCategorical == (varNames.getSelectedIndices()).length) {
                    barchartMenuItem.setEnabled(true);
//n.setEnabled(true);
                    histogramMenuItem.setEnabled(false);
                    weightedHistogramMenuItem.setEnabled(false);
                    parallelBoxplotMenuItem.setEnabled(false);
                } else {
                    barchartMenuItem.setEnabled(false);
                    histogramMenuItem.setEnabled(true);
                    weightedHistogramMenuItem.setEnabled(true);
                    parallelBoxplotMenuItem.setEnabled(true);
                }
                missingValuePlotMenuItem.setEnabled(true);
                mosaicPlotMenuItem.setEnabled(false);
                weightedBarchartMenuItem.setEnabled(false);
                weightedMosaicPlotMenuItem.setEnabled(false);
                parCoordinatesMenuItem.setEnabled(false);
                boxplotByXYMenuItem.setEnabled(false);
                //              sc.setEnabled(false);
                scatterplotMenuItem.setEnabled(false);
                twoDimMDSMenuItem.setEnabled(false);
                pcaMenuItem.setEnabled(false);
                splotMenuItem.setEnabled(false);
                break;
            case 2:
                parCoordinatesMenuItem.setEnabled(true);
                scatterplotMenuItem.setEnabled(true);
                splotMenuItem.setEnabled(true);
                missingValuePlotMenuItem.setEnabled(true);
                twoDimMDSMenuItem.setEnabled(false);
                parallelBoxplotMenuItem.setEnabled(true);
                boxplotByXYMenuItem.setEnabled(false);
                if (numCategorical == (varNames.getSelectedIndices()).length) {
                    barchartMenuItem.setEnabled(true);
                    mosaicPlotMenuItem.setEnabled(true);
                } else {
                    barchartMenuItem.setEnabled(false);
                    mosaicPlotMenuItem.setEnabled(false);
                }
                if (numCategorical == 1) {
                    weightedBarchartMenuItem.setEnabled(true);
                    weightedMosaicPlotMenuItem.setEnabled(true);
                    parallelBoxplotMenuItem.setEnabled(false);
                    boxplotByXYMenuItem.setEnabled(true);
                } else {
                    weightedBarchartMenuItem.setEnabled(false);
                    weightedMosaicPlotMenuItem.setEnabled(false);
                }
                if (numCategorical == 0) {
                    histogramMenuItem.setEnabled(true);
                    weightedHistogramMenuItem.setEnabled(true);
                    pcaMenuItem.setEnabled(true);
                } else {
                    histogramMenuItem.setEnabled(false);
                    weightedHistogramMenuItem.setEnabled(false);
                }
                break;
            default:
                if (numCategorical == (varNames.getSelectedIndices()).length) {
                    barchartMenuItem.setEnabled(true);
                    mosaicPlotMenuItem.setEnabled(true);
                } else {
                    barchartMenuItem.setEnabled(false);
                    mosaicPlotMenuItem.setEnabled(false);
                }
                if (numCategorical == (varNames.getSelectedIndices()).length - 1) {
                    weightedBarchartMenuItem.setEnabled(true);
                    weightedMosaicPlotMenuItem.setEnabled(true);
                } else {
                    weightedBarchartMenuItem.setEnabled(false);
                    weightedMosaicPlotMenuItem.setEnabled(false);
                }
                if (numCategorical == 0) {
                    histogramMenuItem.setEnabled(true);
                    weightedHistogramMenuItem.setEnabled(true);
                } else {
                    histogramMenuItem.setEnabled(false);
                    weightedHistogramMenuItem.setEnabled(false);
                }
                if ((varNames.getSelectedIndices()).length - numCategorical > 1 && RService.hasR())
                    pcaMenuItem.setEnabled(true);
                if ((varNames.getSelectedIndices()).length - numCategorical > 2 && RService.hasR())
                    twoDimMDSMenuItem.setEnabled(true);
                parCoordinatesMenuItem.setEnabled(true);
                parallelBoxplotMenuItem.setEnabled(true);
                missingValuePlotMenuItem.setEnabled(true);
                splotMenuItem.setEnabled(true);
                scatterplotMenuItem.setEnabled(false);
                //        sc.setEnabled(false);
        }
        if (!dataSets.elementAt(dataSetCounter).hasMissings)
            missingValuePlotMenuItem.setEnabled(false);

        // Now handle transform Menue
        int alphs = 0;
        DataSet data = dataSets.elementAt(dataSetCounter);
        for (int i = 0; i < varNames.getSelectedIndices().length; i++)
            if (data.alpha(varNames.getSelectedIndices()[i]))
                alphs++;
        if (alphs == 0 && (varNames.getSelectedIndices().length == 2 || varNames.getSelectedIndices().length == 1)) {
            trans.setEnabled(true);
            if (varNames.getSelectedIndices().length == 2) {
                transPlus.setText(data.getName(selectBuffer[1]) + " + " + data.getName(selectBuffer[0]));
                transMinus.setText(data.getName(selectBuffer[1]) + " - " + data.getName(selectBuffer[0]));
                transTimes.setText(data.getName(selectBuffer[1]) + " * " + data.getName(selectBuffer[0]));
                transDiv.setText(data.getName(selectBuffer[1]) + " / " + data.getName(selectBuffer[0]));
                transNeg.setText("-x");
                transInv.setText("1/x");
                transLog.setText("log(x)");
                transExp.setText("exp(x)");
                transPlus.setEnabled(true);
                transMinus.setEnabled(true);
                transTimes.setEnabled(true);
                transDiv.setEnabled(true);
                transNeg.setEnabled(false);
                transInv.setEnabled(false);
                transLog.setEnabled(false);
                transExp.setEnabled(false);
            } else {
                if (data.getName(selectBuffer[0]).indexOf("-") > 0 || data.getName(selectBuffer[0]).indexOf("+") > 0)
                    transNeg.setText("-(" + data.getName(selectBuffer[0]) + ")");
                else
                    transNeg.setText("-" + data.getName(selectBuffer[0]));
                if (data.getName(selectBuffer[0]).indexOf("-") > 0 || data.getName(selectBuffer[0]).indexOf("+") > 0)
                    transInv.setText("1/(" + data.getName(selectBuffer[0]) + ")");
                else
                    transInv.setText("1/" + data.getName(selectBuffer[0]));
                transLog.setText("log(" + data.getName(selectBuffer[0]) + ")");
                transExp.setText("exp(" + data.getName(selectBuffer[0]) + ")");
                transPlus.setText("x + y");
                transMinus.setText("x - y");
                transTimes.setText("x * y");
                transDiv.setText("x / y");
                transPlus.setEnabled(false);
                transMinus.setEnabled(false);
                transTimes.setEnabled(false);
                transDiv.setEnabled(false);
                transNeg.setEnabled(true);
                transInv.setEnabled(true);
                transLog.setEnabled(true);
                transExp.setEnabled(true);
            }
        } else
            trans.setEnabled(false);
    }


    public void maintainOptionMenu() {
        DataSet data = dataSets.elementAt(dataSetCounter);

        // Selection
        if (data.countSelection() == 0)
            selectionMenuItem.setEnabled(false);
        else
            selectionMenuItem.setEnabled(true);
        // Colors
        if (data.colorBrush)
            colorsMenuItem.setEnabled(true);
        else
            colorsMenuItem.setEnabled(false);

        boolean mode = DragBox.extSelMode;
        orSelectionCheckMenuItem.setSelected(mode);
        andSelectionCheckMenuItem.setSelected(!mode);
    }


    public void maintainWindowMenu(boolean preserve) {
        for (int i = 0; i < plots.size(); i++)
            plots.elementAt(i).frame.maintainMenu(preserve);
    }


    public void topWindow() {
        if (((System.getProperty("os.name")).toLowerCase()).indexOf("mac") > -1)
            this.setJMenuBar(menubar);                 // Add it to the frame.
    }


    public void handleOpenFile(File inFile) {
        //  handleOpenFile does not get an Event if a file is dropped on a non-running Mondrian.app, so we need to get it here, but only in this singular situation!
        //
        //    System.out.println(".......... CALL loadDataSet("+inFile+") FROM handleOpenFile IN MonFrame .........");
        if (mondrianRunning)
            return;
        while (!mondrianRunning)
            System.out.println(" wait for Mondrian to initialize ...");   // Wait until Mondrian initialized
        if (!dataSets.isEmpty())
            return;
        loadDataSet(false, inFile, "");
    }


    public void setDataSet(DataSet data) {
        dataSets.addElement(data);
        setVarList();
        selseq = true;
        selSeqCheckItem.setSelected(true);
        selSeqCheckItem.setEnabled(false);
    }


    public void regiserPlotFactory(PlotFactory plotFactory) {
        plotFacRegistry.add(plotFactory);

        // add the new factory to the main-menu
        plotMenu.add(new JMenuItem(new PlotAction(plotFactory, this)));
    }

}
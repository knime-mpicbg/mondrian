package de.mpicbg.sweng.mondrian;


import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJQuitHandler;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.PlotFactory;
import de.mpicbg.sweng.mondrian.plots.*;
import de.mpicbg.sweng.mondrian.ui.*;
import de.mpicbg.sweng.mondrian.ui.transform.TransformAction;
import de.mpicbg.sweng.mondrian.util.Utils;
import de.mpicbg.sweng.mondrian.util.r.RService;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;


public class MonFrame extends JFrame implements MRJQuitHandler {

    MonController controller;

    /**
     * Remember # of open windows so we can quit when last one is closed
     */
    protected static int num_windows = 0;

    public boolean selseq = false;
    public boolean alphaHi = false;


    public JMenuBar menubar;
    public JMenu plotMenu, windowMenu, helpMenu, deriveVarMenu, transformMenu;

    public JMenuItem modelNavigatorButton;
    public JMenuItem closeDataSetMenuItem;


    public JMenuItem closeAllMenuItem, colorsMenuItem, selectionMenuItem, me, transPlus, transMinus, transTimes, transDiv, transNeg, transInv, transLog, transExp;
    public JCheckBoxMenuItem selSeqCheckItem;
    public JCheckBoxMenuItem alphaOnHighlightCheckMenuItem;
    private JCheckBoxMenuItem orSelectionCheckMenuItem;
    private JCheckBoxMenuItem andSelectionCheckMenuItem;

    private ModelNavigator modelNavigator;

    private int dCol = 1, dSel = 1;

    public SaveDataSetAction saveAction;
    public SaveDataSetAction saveSelectionAction;


    public MonFrame() {

        controller = new MonController(this);

        Toolkit.getDefaultToolkit().setDynamicLayout(false);
        MRJApplicationUtils.registerQuitHandler(this);

        // Start Rserve
        RService.init();

        Font SF = new Font("SansSerif", Font.BOLD, 12);
        this.setFont(SF);
        this.setTitle("Mondrian");               // Create the window.
        num_windows++;                           // Count it.

        menubar = new JMenuBar();         // Create a menubar.

        // Create menu items, with menu shortcuts, and add to the menu.
        JMenu file = menubar.add(new JMenu("File"));
        file.add(new JMenuItem(new OpenDataSetAction(controller)));
        file.add(new JMenuItem(new LoadRDataFrameAction(controller)));
        file.add(new JMenuItem(new CreateDBDataSetAction(controller)));

        saveAction = new SaveDataSetAction("Save", false, controller);
        file.add(new JMenuItem(saveAction));

        saveSelectionAction = new SaveDataSetAction("Save Selection", true, controller);
        file.add(new JMenuItem(saveSelectionAction));

        file.add(closeDataSetMenuItem = new JMenuItem(new CloseDataSetAction(controller)));

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

        plotMenu = new JMenu("Plot");
        menubar.add(plotMenu);                         // Add to menubar.

        transformMenu = TransformAction.createTrafoMenu(this);

        menubar.add(transformMenu);

        JMenu options = new JMenu("Options");      // Create an Option menu.
        JMenuItem sa;
        options.add(sa = new JMenuItem("Select All"));
        sa.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenuItem ts;
        options.add(ts = new JMenuItem("Toggle Selection"));
        ts.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        JMenu sam = new JMenu("<SHIFT><ALT> is");
        options.add(sam);
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

        options.add(deriveVarMenu = new JMenu("Derive Variable from"));
        deriveVarMenu.add(selectionMenuItem = new JMenuItem("Selection"));
        selectionMenuItem.setEnabled(false);
        deriveVarMenu.add(colorsMenuItem = new JMenuItem("Colors"));
        colorsMenuItem.setEnabled(false);

        options.addSeparator();                     // Put a separator in the menu
        options.add(modelNavigatorButton = new JMenuItem("Model Navigator", KeyEvent.VK_J));
        modelNavigatorButton.setEnabled(false);

        options.addSeparator();                     // Put a separator in the menu
        JMenuItem pr;
        options.add(pr = new JMenuItem("Preferences ...", KeyEvent.VK_K));
        pr.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        menubar.add(options);                      // Add to menubar.

        windowMenu = menubar.add(new JMenu("Window"));

        windowMenu.add(closeAllMenuItem = new JMenuItem("Close All"));
        closeAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        closeAllMenuItem.setEnabled(false);

        windowMenu.addSeparator();

        windowMenu.add(me = new JMenuItem(this.getTitle()));

        helpMenu = menubar.add(new JMenu("Help"));

        JMenuItem rc;
        helpMenu.add(rc = new JMenuItem("Reference Card"));
        rc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        rc.setEnabled(true);

        JCheckBoxMenuItem ih;
        helpMenu.add(ih = new JCheckBoxMenuItem("Interactive Help"));
        ih.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        ih.setEnabled(false);

        JMenuItem oh;
        helpMenu.add(oh = new JMenuItem("Online Help"));
        oh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, Event.SHIFT_MASK | Event.ALT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        oh.setEnabled(true);

        this.setJMenuBar(menubar);                 // Add it to the frame.

        Icon MondrianIcon = new ImageIcon(Utils.readGif("/Logo.gif"));

        JLabel MondrianLabel = new JLabel(MondrianIcon);

        JScrollPane scrollPane = new JScrollPane(MondrianLabel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add("Center", scrollPane);


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
                // todo reenable this
//                deleteSelection();
            }
        });
        vm.addActionListener(new ActionListener() {     // Delete the current selection sequence


            public void actionPerformed(ActionEvent e) {
                controller.getCurrent().getSelector().switchVariableMode();
            }
        });
        closeAllMenuItem.addActionListener(new ActionListener() {     // Close all Windows


            public void actionPerformed(ActionEvent e) {
                controller.closeAll();
            }
        });
        closeDataSetMenuItem.addActionListener(new ActionListener() {     // Close this window.


            public void actionPerformed(ActionEvent e) {
                controller.close(controller.getCurrent());
            }
        });
        me.addActionListener(new ActionListener() {   // Show main window


            public void actionPerformed(ActionEvent e) {
                toFront();
            }
        });
        rc.addActionListener(new ActionListener() {     // Show reference card window.


            public void actionPerformed(ActionEvent e) {
                Utils.showRefCard();
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
                controller.closeAll();
            }
        });

        this.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
//                topWindow();
            }
        });

        // Set the window size and pop it up.
        this.setResizable(false);
        this.setSize(295, 320);
        this.show();

        Graphics g = this.getGraphics();
        g.setFont(new Font("SansSerif", 0, 11));
        g.drawString("v1.1", 260, 285);

        registerCommonPlots();
    }


    private void registerCommonPlots() {
        registerPlotFactory(new MissPlotFactory());
        plotMenu.add(new JSeparator());

        registerPlotFactory(new BarchartFactory());
        registerPlotFactory(new WeightedBarCharFactory());
        plotMenu.add(new JSeparator());

        registerPlotFactory(new HistogramFactory());
        registerPlotFactory(new WeightedHistogramFactory());
        plotMenu.add(new JSeparator());

        registerPlotFactory(new ScatterplotFactory());
        registerPlotFactory(new SplomFactory());
        plotMenu.add(new JSeparator());


        registerPlotFactory(new MosaicPlotFactory());
        registerPlotFactory(new WeightedMosaicPlotFactory());
        plotMenu.add(new JSeparator());

        registerPlotFactory(new BoxplotByXYFactory());
        registerPlotFactory(new ParallelBoxplotFactory());
        registerPlotFactory(new ParallelPlotFactory());
        plotMenu.add(new JSeparator());

        registerPlotFactory(new TwoDimMDSFactory());
        registerPlotFactory(new MapPlotFactory());
        registerPlotFactory(new PCAPlotFactory());

    }


    public void handleQuit() {
        System.exit(0);
    }


    public void switchSelection() {
        if (controller.countInstances() > -1 && controller.getCurrentDataSet().isDB)
            selseq = true;
        else {
            selseq = selSeqCheckItem.isSelected();
            //System.out.println("Selection Sequences : "+selseq);
            if (!selseq)
                getController().getCurrent().deleteSelection();
        }
    }


    public void switchAlpha() {
        alphaHi = alphaOnHighlightCheckMenuItem.isSelected();
        controller.getCurrent().updateSelection();
    }


    public void selectAll() {
        if (controller.countInstances() > -1) {
            controller.getCurrentDataSet().selectAll();
            controller.getCurrent().updateSelection();
        }
    }


    public void toggleSelection() {
        if (controller.countInstances() > 0) {
            controller.getCurrentDataSet().toggleSelection();
            controller.getCurrent().updateSelection();
        }
    }


    public void clearColors() {
        if (controller.countInstances() > 0) {
            controller.getCurrentDataSet().colorsOff();
            controller.getCurrent().dataChanged(-1);
        }
    }


    public void setExtSelMode(boolean mode) {
        DragBox.extSelMode = mode;
        orSelectionCheckMenuItem.setSelected(mode);
        andSelectionCheckMenuItem.setSelected(!mode);
    }


    public void deriveVariable(boolean color) {

        String name;
        DataSet data = controller.getCurrentDataSet();
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
    }


    public void showModeNavigator() {
        if (modelNavigator == null) {
            modelNavigator = new ModelNavigator();
        } else {
            modelNavigator.show();
        }
    }


    public void preferenceFrame() {
        PreferencesFrame.showPrefsDialog(this);
    }


    public void maintainPlotMenu() {

        int numCategorical = controller.getCurrent().calcNumCategoricalVars();

        // update the enabled states of all registered plot-factories
        for (int i = 0; i < plotMenu.getMenuComponentCount(); i++) {
            if (!(plotMenu.getMenuComponent(i) instanceof JMenuItem)) {
                continue;
            }

            JMenuItem menuItem = (JMenuItem) plotMenu.getMenuComponent(i);
            if (menuItem.getAction() instanceof PlotAction) {
                PlotAction plotAction = (PlotAction) menuItem.getAction();

                JList varNames = controller.getCurrent().getSelector().getVarNames();
                if (varNames.getSelectedIndices().length == 0) {
                    plotAction.setEnabled(false);
                } else {
                    plotAction.configureForVarSelection(controller.getCurrentDataSet(), (varNames.getSelectedIndices()).length, numCategorical);
                }
            }
        }


        // Now handle transform menu
        updateTrafoMenuToVarSelection();
    }


    private void updateTrafoMenuToVarSelection() {
        int alphs = 0;

        JList varNames = controller.getCurrent().getSelector().getVarNames();
        DataSet data = controller.getCurrentDataSet();

        for (int i = 0; i < varNames.getSelectedIndices().length; i++) {
            if (data.alpha(varNames.getSelectedIndices()[i]))
                alphs++;
        }

        if (alphs == 0 && (varNames.getSelectedIndices().length == 2 || varNames.getSelectedIndices().length == 1)) {
            transformMenu.setEnabled(true);

            for (int i = 0; i < transformMenu.getComponentCount(); i++) {
                Component menuComponent = transformMenu.getMenuComponent(i);
                if (!(menuComponent instanceof JMenuItem)) {
                    continue;
                }

                TransformAction action = (TransformAction) ((JMenuItem) menuComponent).getAction();
                action.setEnabled(action.isCompliant(varNames.getSelectedIndices().length));
            }

        } else {
            transformMenu.setEnabled(false);
        }
    }


    public void maintainOptionMenu() {
        DataSet data = controller.getCurrentDataSet();

        closeDataSetMenuItem.setEnabled(true);
        saveAction.setEnabled(true);


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


    public void registerPlotFactory(PlotFactory plotFactory) {

        // add the new factory to the main-menu
        plotMenu.add(new JMenuItem(new PlotAction(plotFactory, this)));
    }


    public MonController getController() {
        return controller;
    }
}
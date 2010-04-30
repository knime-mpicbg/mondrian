package de.mpicbg.sweng.mondrian;


import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJQuitHandler;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.core.PlotFactory;
import de.mpicbg.sweng.mondrian.core.Selection;
import de.mpicbg.sweng.mondrian.io.db.Query;
import de.mpicbg.sweng.mondrian.plots.basic.MyPoly;
import de.mpicbg.sweng.mondrian.ui.*;
import de.mpicbg.sweng.mondrian.ui.transform.TransformAction;
import de.mpicbg.sweng.mondrian.util.StatUtil;
import de.mpicbg.sweng.mondrian.util.Utils;
import de.mpicbg.sweng.mondrian.util.r.RService;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;


public class MonFrame extends JFrame implements MRJQuitHandler {

    MonController controller;

    /**
     * Remember # of open windows so we can quit when last one is closed
     */
    protected static int num_windows = 0;

    protected static Vector<MonFrame> monFrames;

    private java.util.List<PlotFactory> plotFacRegistry = new ArrayList<PlotFactory>();

    public Vector<Selection> selList = new Vector<Selection>();
    public Query sqlConditions;
    public boolean selseq = false;
    public boolean alphaHi = false;
    public Vector<MyPoly> polys = new Vector<MyPoly>(256, 256);
    private int numCategorical = 0;
    private int weightIndex = 0;
    private JProgressBar progBar;

    public JMenuBar menubar;
    public JMenu windowMenu, helpMenu, deriveVarMenu, transformMenu;

    private JMenuItem saveMenuItem;
    private JMenuItem saveSelectionMenuItem;

    private JMenuItem modelNavigatorButton;
    private JMenuItem closeDataSetMenuItem;


    public JMenuItem closeAllMenuItem, colorsMenuItem, selectionMenuItem, me, transPlus, transMinus, transTimes, transDiv, transNeg, transInv, transLog, transExp;
    private JCheckBoxMenuItem selSeqCheckItem;
    private JCheckBoxMenuItem alphaOnHighlightCheckMenuItem;
    private JCheckBoxMenuItem orSelectionCheckMenuItem;
    private JCheckBoxMenuItem andSelectionCheckMenuItem;

    private ModelNavigator modelNavigator;
    public int dataSetCounter = -1;
    private int dCol = 1, dSel = 1;
    public boolean mondrianRunning = false;
    public JMenu plotMenu;


    public MonFrame(Vector<MonFrame> monFrames, Vector<DataSet> dataSets, boolean load, boolean loadDB, File loadFile) {

        monFrames.addElement(this);

        Toolkit.getDefaultToolkit().setDynamicLayout(false);
        MRJApplicationUtils.registerQuitHandler(this);

        // Start Rserve
        RService.init();

        Font SF = new Font("SansSerif", Font.BOLD, 12);
        this.setFont(SF);
        MonController.dataSets = dataSets;
        MonFrame.monFrames = monFrames;
        this.setTitle("Mondrian");               // Create the window.
        num_windows++;                           // Count it.

        menubar = new JMenuBar();         // Create a menubar.

        // Create menu items, with menu shortcuts, and add to the menu.
        JMenu file = menubar.add(new JMenu("File"));
        file.add(new JMenuItem(new OpenDataSetAction(controller)));


        file.add(new JMenuItem(new LoadRDataFrameAction(controller)));

        file.add(new JMenuItem(new CreateDBDataSetAction(controller)));


        file.add(saveMenuItem = new JMenuItem(new SaveDataSetAction("Save", false, controller)));
        file.add(saveSelectionMenuItem = new JMenuItem(new SaveDataSetAction("Save Selection", true, controller)));

        saveSelectionMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        saveSelectionMenuItem.setEnabled(false);

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
                close();
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

        mondrianRunning = true;

        if (!RService.hasR()) {
            //      JOptionPane.showMessageDialog(this, "Connection to R failed:\nSome functions might be missing!\n\nPlease check installation of R and  Rserve\nor try starting Rserve manually ...","Rserve Error",JOptionPane.WARNING_MESSAGE);
            g.setColor(Color.white);
            g.fillRect(9, 275, 220, 14);
            g.setColor(Color.gray);
            g.drawString("Connection to R failed: Please check Rserve", 9, 285);
        }


        new OpenDataSetAction(controller).actionPerformed();
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
        this((monFrames == null) ? new Vector<MonFrame>(5, 5) : monFrames, (MonController.dataSets == null) ? new Vector<DataSet>(5, 5) : MonController.dataSets, false, false, null);

        initWithData(data);
    }


    /**
     * adds a dataset and makes is current.
     *
     * @param data dataset to use
     */
    public void initWithData(DataSet data) {
        MonController.dataSets.addElement(data);
        dataSetCounter = MonController.dataSets.size() - 1;
        setVarList();
        this.setTitle("Mondrian(" + controller.getCurrentDataSet().setName + ")");               //
        me.setText(this.getTitle());
        closeDataSetMenuItem.setEnabled(true);
        saveMenuItem.setEnabled(true);

        int nom = controller.getCurrentDataSet().countSelection();
        int denom = controller.getCurrentDataSet().n;
        String Display = nom + "/" + denom + " (" + StatUtil.roundToString(100 * nom / denom, 2) + "%)";
        progText.setText(Display);
        progBar.setValue(nom);

        maintainOptionMenu();
    }


    public void handleQuit() {
        System.exit(0);
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

        Mondrian current = controller.getCurrent();
        String message = "Close dataset \"" + controller.getCurrentDataSet().setName + "\" and\n all corresponding plots?";

        int answer = JOptionPane.showConfirmDialog(this, message);
        if (answer == JOptionPane.YES_OPTION) {
            num_windows--;
            for (int i = plots.size() - 1; i >= 0; i--)
                plots.elementAt(i).frame.close();
            MonController.dataSets.setElementAt(new DataSet("nullinger"), dataSetCounter);
            this.dispose();
            if (num_windows == 0) {
                new MonFrame(monFrames, MonController.dataSets, false, false, null);
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


    public void switchSelection() {
        if (dataSetCounter > -1 && controller.getCurrentDataSet().isDB)
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
            controller.getCurrentDataSet().selectAll();
            updateSelection();
        }
    }


    public void toggleSelection() {
        if (dataSetCounter > -1) {
            controller.getCurrentDataSet().toggleSelection();
            updateSelection();
        }
    }


    public void clearColors() {
        if (dataSetCounter > -1) {
            controller.getCurrentDataSet().colorsOff();
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
        varNames = null;
        setVarList();
    }


    public void setVarList() {
        if (varNames != null) {
            paint(this.getGraphics());
            return;
        }
        if (dataSetCounter == -1)
            dataSetCounter = MonController.dataSets.size() - 1;


        this.show();
    }


    public static int[] getWeightVariable(int[] vars, DataSet data) {

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


    public void switchVariableMode() {
        for (int i = 0; i < varNames.getSelectedIndices().length; i++) {
            int index = (varNames.getSelectedIndices())[i];
            DataSet data = controller.getCurrentDataSet();
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
            if (controller.getCurrentDataSet().categorical(varNames.getSelectedIndices()[i]))
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
            if (MonController.dataSets.size() > 0)
                dataSetCounter = MonController.dataSets.size() - 1;
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
                PlotAction plotAction = (PlotAction) menuItem.getAction();

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


    public void setDataSet(DataSet data) {
        MonController.dataSets.addElement(data);
        setVarList();
        selseq = true;
        selSeqCheckItem.setSelected(true);
        selSeqCheckItem.setEnabled(false);
    }


    public void registerPlotFactory(PlotFactory plotFactory) {
        plotFacRegistry.add(plotFactory);

        // add the new factory to the main-menu
        plotMenu.add(new JMenuItem(new PlotAction(plotFactory, this)));
    }


    public MonController getController() {
        return controller;
    }
}
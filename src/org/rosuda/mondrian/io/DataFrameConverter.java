package org.rosuda.mondrian.io;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.rosuda.mondrian.MonFrame;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class DataFrameConverter {

    private JFrame dataFrames;

    MonFrame monFrame;


    public DataFrameConverter(MonFrame monFrame) {
        this.monFrame = monFrame;
    }


    public void loadDataFrame() {
        System.out.println("Load data.frame()");

        JFileChooser f = new JFileChooser("~/.");
        f.setFileHidingEnabled(false);
        f.setSelectedFile(new File("~/.RData"));
        f.showDialog(null, "Open .RData File");

        final File rDataFile = f.getSelectedFile();

        System.out.println("->" + rDataFile.getAbsolutePath() + "<-");

        boolean isWindows = isWindows();

        if (rDataFile.exists())
            try {
                RConnection rC = createRConnection();
                if (isWindows)
                    rC.voidEval("load(\"" + (rDataFile.getAbsolutePath()).replaceAll("\\\\", "\\\\\\\\") + "\")");
                else
                    rC.voidEval("load(\"" + rDataFile.getAbsolutePath() + "\")");
                String[] ls = rC.eval("sort(ls())").asStrings();

                dataFrames = new JFrame("Choose a data.frame");
                dataFrames.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                JPanel dfPanel = new JPanel();
                GridBagLayout dfLayout = new GridBagLayout();
                GridBagConstraints dfCLayout = new GridBagConstraints();
                dfPanel.setLayout(dfLayout);
                DefaultListModel model = new DefaultListModel();
                final JList list = new JList(model);
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                list.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_W ||
                                (e.getKeyCode() == KeyEvent.VK_ESCAPE)) dataFrames.dispose();
                    }
                });

                JScrollPane pane = new JScrollPane(list);
                final JButton chooseButton = new JButton("Load");
                chooseButton.setDefaultCapable(true);
                //original code
//                getRootPane().setDefaultButton(chooseButton);
                dataFrames.getRootPane().setDefaultButton(chooseButton);

                chooseButton.setEnabled(false);

                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dataFrames.dispose();
                    }
                });

                chooseButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dataFrames.dispose();
                        importRData(rDataFile, (String) list.getSelectedValue());
                    }
                });

                for (int i = 0; i < ls.length; i++) {
                    String Rclass = rC.eval("class(" + ls[i] + ")").asString();
                    if (Rclass.equals("data.frame")) {
                        System.out.println(ls[i] + "  " + Rclass);
                        model.addElement(ls[i]);
                    }
                }

                list.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            dataFrames.dispose();
                            importRData(rDataFile, (String) list.getSelectedValue());
                        }
                    }
                });

                list.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        if (list.getSelectedIndex() > -1)
                            chooseButton.setEnabled(true);
                        else
                            chooseButton.setEnabled(false);
                    }
                });


                dfCLayout.gridx = 0;
                dfCLayout.gridy = 0;
                dfCLayout.gridwidth = 2;
                dfCLayout.gridheight = 1;
                dfCLayout.fill = GridBagConstraints.BOTH;
                dfCLayout.weightx = 1;
                dfCLayout.weighty = 50;
                dfCLayout.anchor = GridBagConstraints.NORTH;
                dfLayout.setConstraints(pane, dfCLayout);

                dfPanel.add(pane);

                dfCLayout.gridx = 0;
                dfCLayout.gridy = 1;
                dfCLayout.gridwidth = 1;
                dfCLayout.gridheight = 1;
                dfCLayout.fill = GridBagConstraints.BOTH;
                dfCLayout.weightx = 1;
                dfCLayout.weighty = 1;
                dfCLayout.anchor = GridBagConstraints.SOUTH;
                dfLayout.setConstraints(cancelButton, dfCLayout);

                dfPanel.add(cancelButton);

                dfCLayout.gridx = 1;
                dfCLayout.gridy = 1;
                dfCLayout.gridwidth = 1;
                dfCLayout.gridheight = 1;
                dfCLayout.fill = GridBagConstraints.BOTH;
                dfCLayout.weightx = 1;
                dfCLayout.weighty = 1;
                dfCLayout.anchor = GridBagConstraints.SOUTH;
                dfLayout.setConstraints(chooseButton, dfCLayout);

                dfPanel.add(chooseButton);

                dataFrames.setContentPane(dfPanel);
                dataFrames.setSize(260, 300);
                dataFrames.setResizable(false);

                dataFrames.setLocation((int) ((Toolkit.getDefaultToolkit().getScreenSize()).getWidth() / 2) - 130,
                        (int) ((Toolkit.getDefaultToolkit().getScreenSize()).getHeight() / 2) - 150);

                dataFrames.setVisible(true);

            } catch (RserveException rse) {
                System.out.println("Rserve exception: " + rse.getMessage());
            }
            catch (REXPMismatchException mme) {
                System.out.println("Mismatch exception : " + mme.getMessage());
            }
    }


    private RConnection createRConnection() {
        try {
            return new RConnection();
        } catch (RserveException e) {
            throw new RuntimeException(e);
        }
    }


    public void importRData(File Rdata, String rDataSet) {
        RConnection rC = createRConnection();
        try {
            if (isWindows())
                rC.voidEval("write.table(" + rDataSet + ", \"" + (Rdata.getParentFile() + File.separator).replaceAll("\\\\", "\\\\\\\\") + ".MondrianTmpImport.txt\", quote=FALSE, sep=\"\\t\", row.names = FALSE)");
            else
                rC.voidEval("write.table(" + rDataSet + ", \"" + Rdata.getParentFile() + File.separator + ".MondrianTmpImport.txt\", quote=FALSE, sep=\"\\t\", row.names = FALSE)");

            rC.close();
        } catch (RserveException rse) {
            System.out.println("Rserve exception: " + rse.getMessage());
        }

        File tmpR = new File(Rdata.getParentFile() + File.separator + ".MondrianTmpImport.txt");
        monFrame.loadDataSet(false, tmpR, rDataSet);
        tmpR.delete();
    }


    private static boolean isWindows() {
        String osname = System.getProperty("os.name");
        if (osname != null && osname.length() >= 7 && osname.substring(0, 7).equals("Windows")) {
            return true;
        }

        return
                false;
    }
}

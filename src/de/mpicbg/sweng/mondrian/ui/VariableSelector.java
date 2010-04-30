package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.MonController;
import de.mpicbg.sweng.mondrian.MonFrame;
import de.mpicbg.sweng.mondrian.core.DataSet;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.Vector;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class VariableSelector extends JPanel {

    public JList varNames = null;
    private JScrollPane scrollPane;
    public int[] selectBuffer;


    MonFrame monFrame;
    MonController monController;

    private String searchText = "";
    private long startT = 0;
    private Vector<Integer> setIndices = new Vector<Integer>(10, 0);


    public VariableSelector(DataSet datSet) {
        selectBuffer = new int[datSet.k + 15];

        rebuild(datSet);
    }


    public JList getVarNames() {
        return varNames;
    }


    private void rebuild(final DataSet datSet) {
        final DataSet data = monFrame.getController().getCurrentDataSet();
        String listNames[] = new String[data.k];
        for (int j = 0; j < data.k; j++) {
            listNames[j] = " " + data.getName(j);
            //      System.out.println("Adding:"+listNames[j]);
        }

        varNames = new JList(listNames);

        scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
                        rebuild(datSet);
                        monFrame.maintainPlotMenu();
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
                    monFrame.maintainPlotMenu();
                }
            }
        });

        varNames.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                monFrame.maintainPlotMenu();
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

        varNames.setCellRenderer(new AttributeCellRenderer(datSet));

        RepaintManager currentManager = RepaintManager.currentManager(varNames);
        currentManager.setDoubleBufferingEnabled(true);
    }
}

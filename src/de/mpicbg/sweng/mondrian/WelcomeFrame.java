package de.mpicbg.sweng.mondrian;


import com.apple.mrj.MRJAboutHandler;
import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJQuitHandler;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;


public class WelcomeFrame extends Frame
        implements ActionListener,
        MRJAboutHandler,
        MRJQuitHandler {

    private static final String message = "Hello World!";
    private Font font = new Font("serif", Font.ITALIC + Font.BOLD, 36);

    private AboutBox aboutBox;

    // Declarations for menus
    private static final MenuBar mainMenuBar = new MenuBar();

    private static final Menu fileMenu = new Menu("File");
    private MenuItem miNew;
    private MenuItem miOpen;
    private MenuItem miClose;
    private MenuItem miSave;
    private MenuItem miSaveAs;

    private static final Menu editMenu = new Menu("Edit");
    private MenuItem miUndo;
    private MenuItem miCut;
    private MenuItem miCopy;
    private MenuItem miPaste;
    private MenuItem miClear;
    private MenuItem miSelectAll;


    void addFileMenuItems() {
        miNew = new MenuItem("New");
        miNew.setShortcut(new MenuShortcut(KeyEvent.VK_N, false));
        fileMenu.add(miNew).setEnabled(true);
        miNew.addActionListener(this);

        miOpen = new MenuItem("Open...");
        miOpen.setShortcut(new MenuShortcut(KeyEvent.VK_O, false));
        fileMenu.add(miOpen).setEnabled(true);
        miOpen.addActionListener(this);

        miClose = new MenuItem("Close");
        miClose.setShortcut(new MenuShortcut(KeyEvent.VK_W, false));
        fileMenu.add(miClose).setEnabled(true);
        miClose.addActionListener(this);

        miSave = new MenuItem("Save");
        miSave.setShortcut(new MenuShortcut(KeyEvent.VK_S, false));
        fileMenu.add(miSave).setEnabled(true);
        miSave.addActionListener(this);

        miSaveAs = new MenuItem("Save As...");
        miSaveAs.setShortcut(new MenuShortcut(KeyEvent.VK_S, true));
        fileMenu.add(miSaveAs).setEnabled(true);
        miSaveAs.addActionListener(this);

        mainMenuBar.add(fileMenu);
    }


    void addEditMenuItems() {
        miUndo = new MenuItem("Undo");
        miUndo.setShortcut(new MenuShortcut(KeyEvent.VK_Z, false));
        editMenu.add(miUndo).setEnabled(true);
        miUndo.addActionListener(this);
        editMenu.addSeparator();

        miCut = new MenuItem("Cut");
        miCut.setShortcut(new MenuShortcut(KeyEvent.VK_X, false));
        editMenu.add(miCut).setEnabled(true);
        miCut.addActionListener(this);

        miCopy = new MenuItem("Copy");
        miCopy.setShortcut(new MenuShortcut(KeyEvent.VK_C, false));
        editMenu.add(miCopy).setEnabled(true);
        miCopy.addActionListener(this);

        miPaste = new MenuItem("Paste");
        miPaste.setShortcut(new MenuShortcut(KeyEvent.VK_V, false));
        editMenu.add(miPaste).setEnabled(true);
        miPaste.addActionListener(this);

        miClear = new MenuItem("Clear");
        editMenu.add(miClear).setEnabled(true);
        miClear.addActionListener(this);
        editMenu.addSeparator();

        miSelectAll = new MenuItem("Select All");
        miSelectAll.setShortcut(new MenuShortcut(KeyEvent.VK_A, false));
        editMenu.add(miSelectAll).setEnabled(true);
        miSelectAll.addActionListener(this);

        mainMenuBar.add(editMenu);
    }


    void addMenus() {
        addFileMenuItems();
        addEditMenuItems();
        setMenuBar(mainMenuBar);
    }


    private WelcomeFrame() {
        super("Mondrian");
        setLayout(null);
        addMenus();

        aboutBox = new AboutBox();
        Toolkit.getDefaultToolkit();
        MRJApplicationUtils.registerAboutHandler(this);
        MRJApplicationUtils.registerQuitHandler(this);

        setVisible(true);

    }


    public void paint(Graphics g) {
        g.setColor(Color.blue);
        g.setFont(font);
        g.drawString(message, 40, 80);
    }


    public void handleAbout() {
        aboutBox.setResizable(false);
        aboutBox.setVisible(true);
        aboutBox.show();
    }


    public void handleQuit() {
        System.exit(0);
    }


    // ActionListener interface (for menus)
    public void actionPerformed(ActionEvent newEvent) {
        if (newEvent.getActionCommand().equals(miNew.getActionCommand())) doNew();
        else if (newEvent.getActionCommand().equals(miOpen.getActionCommand())) doOpen();
        else if (newEvent.getActionCommand().equals(miClose.getActionCommand())) doClose();
        else if (newEvent.getActionCommand().equals(miSave.getActionCommand())) doSave();
        else if (newEvent.getActionCommand().equals(miSaveAs.getActionCommand())) doSaveAs();
        else if (newEvent.getActionCommand().equals(miUndo.getActionCommand())) doUndo();
        else if (newEvent.getActionCommand().equals(miCut.getActionCommand())) doCut();
        else if (newEvent.getActionCommand().equals(miCopy.getActionCommand())) doCopy();
        else if (newEvent.getActionCommand().equals(miPaste.getActionCommand())) doPaste();
        else if (newEvent.getActionCommand().equals(miClear.getActionCommand())) doClear();
        else if (newEvent.getActionCommand().equals(miSelectAll.getActionCommand())) doSelectAll();
    }


    void doNew() {
    }


    void doOpen() {
    }


    void doClose() {
    }


    void doSave() {
    }


    void doSaveAs() {
    }


    void doUndo() {
    }


    void doCut() {
    }


    void doCopy() {
    }


    void doPaste() {
    }


    void doClear() {
    }


    void doSelectAll() {
    }


    public static void main(String args[]) {
        new WelcomeFrame();
    }
}

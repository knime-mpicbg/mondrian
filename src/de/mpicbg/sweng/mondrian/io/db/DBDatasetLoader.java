package de.mpicbg.sweng.mondrian.io.db;

import de.mpicbg.sweng.mondrian.AppFrame;
import de.mpicbg.sweng.mondrian.Mondrian;
import de.mpicbg.sweng.mondrian.core.DataSet;
import de.mpicbg.sweng.mondrian.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class DBDatasetLoader {

    AppFrame appFrame;
    Driver d;
    Connection con;
    public JDialog dbLoadDialog;


    public DBDatasetLoader(AppFrame appFrame) {
        this.appFrame = appFrame;
    }


    public void loadDataBase() {
        if (dbLoadDialog == null) {
            dbLoadDialog = new JDialog();
            dbLoadDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    dbLoadDialog.dispose();
                }
            });

            dbLoadDialog.setTitle("DB Connection");
            GridBagLayout gbl = new GridBagLayout();
            dbLoadDialog.getContentPane().setLayout(gbl);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 20;
            gbc.weighty = 100;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;

            Utils.add(dbLoadDialog, new JLabel(" Driver: "), gbc, 0, 0, 1, 1);
            Utils.add(dbLoadDialog, new JLabel(" URL: "), gbc, 0, 1, 1, 1);
            Utils.add(dbLoadDialog, new JLabel(" User: "), gbc, 0, 2, 1, 1);
            Utils.add(dbLoadDialog, new JLabel(" Pwd: "), gbc, 2, 2, 1, 1);
            Utils.add(dbLoadDialog, new JLabel(" DB: "), gbc, 0, 3, 1, 1);
            Utils.add(dbLoadDialog, new JLabel(" Table: "), gbc, 0, 4, 1, 1);

            final JTextField DriverName = new JTextField("org.gjt.mm.mysql.Driver", 35);
            final JTextField URL = new JTextField("jdbc:mysql://137.250.124.51:3306/datasets", 35);
            final JTextField Username = new JTextField("theusm", 16);
            final JPasswordField Passwd = new JPasswordField("", 16);
            final Choice DBList = new Choice();
            DBList.addItem("Not Connected");
            DBList.setEnabled(false);
            final Choice tableList = new Choice();
            tableList.addItem("Choose DB");
            tableList.setEnabled(false);
            final JButton Select = new JButton("Select");
            Select.setEnabled(false);
            final JButton Cancel = new JButton("Cancel");
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            Utils.add(dbLoadDialog, DriverName, gbc, 1, 0, 3, 1);
            Utils.add(dbLoadDialog, URL, gbc, 1, 1, 3, 1);
            Utils.add(dbLoadDialog, Username, gbc, 1, 2, 1, 1);
            Utils.add(dbLoadDialog, Passwd, gbc, 3, 2, 1, 1);
            Utils.add(dbLoadDialog, DBList, gbc, 1, 3, 3, 1);
            Utils.add(dbLoadDialog, tableList, gbc, 1, 4, 3, 1);
            gbc.fill = GridBagConstraints.NONE;
            Utils.add(dbLoadDialog, Select, gbc, 1, 5, 1, 1);
            Utils.add(dbLoadDialog, Cancel, gbc, 3, 5, 1, 1);

            final JButton Load = new JButton("Load");
            dbLoadDialog.getRootPane().setDefaultButton(Load);
            final JButton Connect = new JButton("Connect");
            Connect.setEnabled(false);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.CENTER;
            Utils.add(dbLoadDialog, Load, gbc, 4, 0, 1, 1);
            Utils.add(dbLoadDialog, Connect, gbc, 4, 2, 1, 1);

            dbLoadDialog.pack();
            dbLoadDialog.show();
            Load.addActionListener(new ActionListener() {     //


                public void actionPerformed(ActionEvent e) {
                    if (LoadDriver(DriverName.getText())) {
                        Connect.setEnabled(true);
                        dbLoadDialog.getRootPane().setDefaultButton(Connect);
                    }
                }
            });

            Cancel.addActionListener(new ActionListener() {     //


                public void actionPerformed(ActionEvent e) {
                    dbLoadDialog.dispose();
                }
            });

            Connect.addActionListener(new ActionListener() {     //


                public void actionPerformed(ActionEvent e) {
                    if (DBConnect(URL.getText(), Username.getText(), Passwd.getText())) {
                        try {
                            // Create statement
                            Statement stmt = con.createStatement();

                            // Execute query
                            String query = "show databases";

                            // Obtain the result set
                            ResultSet rs = stmt.executeQuery(query);

                            DBList.removeAll();
                            while (rs.next()) {
                                DBList.addItem(rs.getString(1));
                            }
                            DBList.setEnabled(true);

                            rs.close();

                            // Close statement
                            stmt.close();
                        } catch (Exception ex) {
                            System.out.println("Driver Exception: " + ex);
                        }
                    }
                }
            });

            DBList.addItemListener(new ItemListener() {     //


                public void itemStateChanged(ItemEvent e) {
                    try {
                        // Create statement
                        Statement stmt = con.createStatement();

                        // Execute query
                        String query = "show tables from " + DBList.getSelectedItem();

                        // Obtain the result set
                        ResultSet rs = stmt.executeQuery(query);

                        tableList.removeAll();
                        while (rs.next()) {
                            tableList.addItem(rs.getString(1));
                        }
                        tableList.setEnabled(true);

                        rs.close();

                        // Close statement
                        stmt.close();
                        con.close();                                // disconnect from DB and connect to selected DB
                        String url = URL.getText();
                        DBConnect(url.substring(0, url.lastIndexOf("/") + 1) + DBList.getSelectedItem(), Username.getText(), Passwd.getText());
                    } catch (Exception ex) {
                        System.out.println("Can't get tables out of DB: " + ex);
                    }
                }
            });

            tableList.addItemListener(new ItemListener() {     //


                public void itemStateChanged(ItemEvent e) {
                    Select.setEnabled(true);
                    try {
                        // Create statement
                        Statement stmt = con.createStatement();

                        // Execute query
                        String query = "show fields from " + tableList.getSelectedItem() + " from " + DBList.getSelectedItem();

                        // Obtain the result set
                        ResultSet rs = stmt.executeQuery(query);

                        while (rs.next()) {
                            System.out.println(rs.getString(1) + " - " + rs.getString(2));
                        }

                        rs.close();

                        // Close statement
                        stmt.close();

                        dbLoadDialog.getRootPane().setDefaultButton(Select);
                    } catch (Exception ex) {
                        System.out.println("Can't retreive columns of table >" + tableList.getSelectedItem() + "<: " + ex);
                    }
                }
            });

            Select.addActionListener(new ActionListener() {     //


                public void actionPerformed(ActionEvent e) {
                    DataSet data = new DataSet(con, DBList.getSelectedItem(), tableList.getSelectedItem());
                    dbLoadDialog.dispose();

                    appFrame.getController().addAndActiviate(new Mondrian(data, appFrame.getController()));
                }
            });
        }
    }


    public boolean DBConnect(String URL, String Username, String Passwd) {
        try {
            // Connect to the database at that URL.
            //	  URL="jdbc:mysql://137.250.124.51:3306/datasets";
            //      System.out.println("Database trying to connect ...: "+URL+"?user="+Username+"&password="+Passwd);
            con = DriverManager.getConnection(URL, Username, Passwd);
            //      con = DriverManager.getConnection(URL+"?user="+Username+"&password="+Passwd);
            System.out.println("Database Connected");
            return true;
        } catch (Exception ex) {
            System.out.println("Connection Exception: " + ex);
            return false;
        }
    }


    public boolean LoadDriver(String Driver) {
        try {
            d = (Driver) Class.forName(Driver).newInstance();
            System.out.println("Driver Registered");
            return true;
        } catch (Exception ex) {
            System.out.println("Driver Exception: " + ex);
            return false;
        }
    }


}

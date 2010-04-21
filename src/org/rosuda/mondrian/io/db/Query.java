package org.rosuda.mondrian.io.db;

import java.util.Vector;


public class Query {

    public Vector Item = new Vector(10, 10);
    public Vector Table = new Vector(10, 10);
    public Vector conditionString = new Vector(10, 10);
    public Vector conditionMode = new Vector(10, 10);
    public Vector Group = new Vector(10, 10);
    public Vector Order = new Vector(10, 10);


    public Query() {

    }


    public void addItem(String item) {
        Item.addElement(item);
    }


    public void addTable(String table) {
        Table.addElement(table);
    }


    public void addCondition(String mode, String condition) {
        if (!condition.equals("")) {
            conditionString.addElement(condition);
            conditionMode.addElement(mode);
        }
    }


    public void addGroup(String group) {
        if (!group.equals(""))
            Group.addElement(group);
    }


    public void addOrder(String order) {
        if (!order.equals(""))
            Order.addElement(order);
    }


    public void clearConditions() {
        conditionString.removeAllElements();
        conditionMode.removeAllElements();
    }


    public String getItems() {
        String query = "";
        if (Item.size() > 0) {
            for (int i = 0; i < Item.size() - 1; i++)
                query += Item.elementAt(i) + ", ";
            query += Item.elementAt(Item.size() - 1);
        }
        return query;
    }


    public String getTables() {
        String query = "";
        if (Table.size() > 0) {
            for (int i = 0; i < Table.size() - 1; i++)
                query += Table.elementAt(i) + ", ";
            query += Table.elementAt(Table.size() - 1);
        }
        return query;
    }


    public String getConditions() {
        String query = "";
        if (conditionString.size() > 0) {
            query += conditionString.elementAt(0);
            for (int i = 1; i < conditionString.size(); i++)
                query += " " + conditionMode.elementAt(i) + " " + conditionString.elementAt(i);
        }
        return query;
    }


    public String getGroups() {
        String query = "";
        if (Group.size() > 0) {
            for (int i = 0; i < Group.size() - 1; i++)
                query += Group.elementAt(i) + ", ";
            query += Group.elementAt(Group.size() - 1);
        }
        return query;
    }


    public String getOrder() {
        String query = "";
        if (Order.size() > 0) {
            for (int i = 0; i < Order.size() - 1; i++)
                query += Order.elementAt(i) + ", ";
            query += Order.elementAt(Order.size() - 1);
        }
        return query;
    }


    public String makeQuery() {

        String query = "SELECT " + this.getItems() + ", COUNT(*) FROM " + this.getTables();

        if (conditionString.size() > 0)
            query += " WHERE " + getConditions();

        if (Group.size() > 0)
            query += " GROUP BY " + getGroups();

        if (Order.size() > 0)
            query += " ORDER BY " + getOrder() + " ";

        return query;
    }


    public void print() {
        System.out.println(makeQuery());
    }
}

package de.mpicbg.sweng.mondrian.core;

import de.mpicbg.sweng.mondrian.io.db.Query;

import java.awt.*;               //


public class Selection {

    public static final int MODE_STANDARD = 0;
    public static final int MODE_AND = 1;
    public static final int MODE_OR = 2;
    public static final int MODE_XOR = 3;
    public static final int MODE_NOT = 4;

    private static final int VALID = 0;
    public static final int KILLED = 1;

    public Rectangle r;
    public Object o;
    public int step;
    public int total;
    public int mode;
    public int status = VALID;
    public DragBox d;
    public String modeString;
    public Query condition = new Query();        // Query to store the part of the WHERE clause generated


    public Selection(Rectangle r, Object o, int step, int mode, DragBox d) {

        this.r = r;
        this.o = o;
        this.step = step;
        this.total = step;
        this.mode = mode;
        this.d = d;
        this.status = VALID;
    }


    public static String getModeString(int mode) {

        switch (mode) {
            case MODE_STANDARD:
                return "Replace";
            case MODE_AND:
                return "And";
            case MODE_OR:
                return "Or";
            case MODE_XOR:
                return "XOr";
            case MODE_NOT:
                return "Not";
        }
        return null;
    }


    public void setMode(int mode) {
        this.mode = mode;
    }


    public static String getSQLModeString(int mode) {

        switch (mode) {
            case MODE_STANDARD:
                return "Replace";
            case MODE_AND:
                return "AND";
            case MODE_OR:
                return "OR";
            case MODE_XOR:
                return "XOr";
            case MODE_NOT:
                return "AND NOT";
        }
        return null;
    }
}

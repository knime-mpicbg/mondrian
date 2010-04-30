package de.mpicbg.sweng.mondrian.ui;

import de.mpicbg.sweng.mondrian.core.DragBox;
import de.mpicbg.sweng.mondrian.util.Utils;

import java.awt.*;
import java.util.prefs.Preferences;


/**
 * Document me!
 *
 * @author Holger Brandl
 */
public class ColorManager {

    //  static Color backgroundColor = new Color(223, 184, 96);
    public static Color backgroundColor = new Color(255, 255, 179);
    public static Color objectColor = Color.lightGray;
    public static Color lineColor = Color.black;


    static {
        setScheme(0);

        initialize();
    }


    public static String[] defaultColSchemes = {
            "Classic", "#ffff99", "#c0c0c0", "#000000", "#ff0000",
            "Fancy", "#ffff99", "#c0c0c0", "#000000", "#00ff00",
            "Terra di Siena", "#dfb860", "#c0c0c0", "#000000", "#b46087",
            "DataDesk", "#000000", "#000000", "#ffffff", "#ff0000",
            "Daltonian", "#009999", "#c0c0c0", "#000000", "#ff7400",
            null
    };


    public static void initialize() {
        Preferences prefs = Utils.getPrefs();

        if (!prefs.get("color.background", "").equals("")) {
            ColorManager.backgroundColor = Utils.hrgb2color(prefs.get("color.background", ""));
            ColorManager.objectColor = Utils.hrgb2color(prefs.get("color.objects", ""));
            ColorManager.lineColor = Utils.hrgb2color(prefs.get("color.line", ""));
            DragBox.hiliteColor = Utils.hrgb2color(prefs.get("color.select", ""));
        }
    }


    public static void setScheme(int dragan) {
        int i = dragan * 5;
        Color cl = Utils.hrgb2color(defaultColSchemes[++i]);
        if (cl != null)
            backgroundColor = cl;
        cl = Utils.hrgb2color(defaultColSchemes[++i]);
        if (cl != null)
            objectColor = cl;
        cl = Utils.hrgb2color(defaultColSchemes[++i]);
        if (cl != null)
            lineColor = cl;
        cl = Utils.hrgb2color(defaultColSchemes[++i]);
        if (cl != null)
            DragBox.hiliteColor = cl;
    }
}

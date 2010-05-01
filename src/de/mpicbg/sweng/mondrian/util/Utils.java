package de.mpicbg.sweng.mondrian.util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;


public class Utils {

    private static final HashMap LABELToURLTemplate = new HashMap();


    public static void add(JFrame f, Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;

        f.getContentPane().add(c, gbc);
    }


    public static void add(JDialog f, Component c, GridBagConstraints gbc, int x, int y, int w, int h) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;

        f.getContentPane().add(c, gbc);
    }

/*  public static Image readGif(String name) {

  Image image=null;
  try {
    File sourceimage = new File(name);
    image = ImageIO.read(sourceimage);

  } catch (IOException e) {
    System.out.println("Read GIF Exception: "+e);
  }
  return image;
}
*/


    public static byte[] readGif(String name) {

        byte[] arrayLogo;
        try {
            InputStream inputLogo = Utils.class.getResourceAsStream(name);

            arrayLogo = streamToBytes(inputLogo);
            inputLogo.close();

        } catch (IOException e) {
            System.out.println("Logo Exception: " + e);
            arrayLogo = new byte[1];
        }
        return arrayLogo;
    }


    public static byte[] streamToBytes(InputStream strm) throws IOException {
        byte[] tmpBuf = new byte[2048];
        byte[] buf = new byte[0];

        int len;
        while ((len = strm.read(tmpBuf)) > -1) {
            byte[] newBuf = new byte[buf.length + len];
            System.arraycopy(buf, 0, newBuf, 0, buf.length);
            System.arraycopy(tmpBuf, 0, newBuf, buf.length, len);
            buf = newBuf;
        }

        return buf;
    }


    public static String toPhoneNumber(double d) {

        DecimalFormat phoneN = new DecimalFormat("0000000000");
        String tmp = phoneN.format((long) d);
        return (tmp.substring(0, 3) + "-" + tmp.substring(3, 6) + "-" + tmp.substring(6, 10));
    }


    public static double atod(String a) {
        return Double.valueOf(a).doubleValue();
    }


    /**
     * Reallocates an array with a new size, and copies the contents of the old array to the new array.
     *
     * @param oldArray the old array, to be reallocated.
     * @param newSize  the new array size.
     * @return A new array with the same contents.
     */
    public static Object resizeArray(Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0)
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
        return newArray;
    }


    public static Image makeColorTransparent(Image im, final Color color) {
        ImageFilter filter = new RGBImageFilter() {
            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;


            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }


    public static String color2hrgb(Color c) {
        int i = (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
        String s = Integer.toHexString(i);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return "#" + s;
    }


    public static Color hrgb2color(String s) {
        if (s != null && s.length() > 0 && s.charAt(0) == '#') {
            int c = Utils.parseHexInt(s.substring(1));
            return new Color((c >> 16) & 255, (c >> 8) & 255, c & 255);
        }
        return null;
    }


    public static int parseHexInt(String s) {
        int i = 0;
        try {
            i = Integer.parseInt(s, 16);
        } catch (Exception ignored) {
        }
        return i;
    }


    public static int[] roundProportions(double[] votes, double total, int pie) {

        int[] rounds = new int[votes.length];

        int start = -1;
        int stop = votes.length;
        while (votes[++start] == 0) {
        }
        while (votes[--stop] == 0) {
        }
//    System.out.println("Start: "+start+" Stop: "+stop);
        int k = 1;
        double eps = 0;
        int sum = 0;
        int converge = 24;
        while (sum != pie && k < 64) {
            k++;
            sum = 0;
            for (int i = start; i <= stop; i++) {
                if (k >= converge)
                    eps = Math.random() - 0.5;
                if (votes[i] < 0.0000000001)
                    rounds[i] = 0;
                else
                    rounds[i] = (int) Math.round((double) (votes[i]) / total * pie + eps);
                sum += rounds[i];
            }
            //System.out.println("k: "+k+" eps: "+eps+" sum: "+sum+" pie: "+pie);
            if (sum > pie)
                eps -= 1 / Math.pow(2, k);
            else if (sum < pie)
                eps += 1 / Math.pow(2, k);
        }
        if (sum != pie)
            System.out.println(" Rounding Failed !!!");

        return rounds;
    }


    public static boolean isNumber(String s) {
        try {
            Double dummy = Double.valueOf(s);
        }
        catch (NumberFormatException e) {
            return false;
        }
        return true;
    }


    /**
     * Register a new mapping for tooltip images.
     *
     * @param label    label of field which contains image id data
     * @param template template for url of image any occurence of "$var" will be replaced byt the field value.
     */
    public static void registerHTMLTemplate(String label, String template) {
        LABELToURLTemplate.put(label, template);
    }


    /**
     * Convert the value to html for the tooltip.
     * <p/>
     * If there is a template in the LABELToURLTemplate HashMap the template will be used. If not the value will be used
     * straight.
     *
     * @param label name of the field for which the value is to be transformed.
     */
    public static String getHTMLValue(String label, String val) {
        String urlTemplate = (String) LABELToURLTemplate.get(label);
        if (urlTemplate != null)
            val = urlTemplate.replaceAll("\\$val", val);
        return val;
    }


    public static String info2Html(String infoText) {

        String infoTxt = "";
        String sep = ": </TD><TD  align='left'> <font size=4 face='courier'>";
        String para = "</TD><TR height=5><TD align=right><font size=4 face='courier'> ";

        StringTokenizer info = new StringTokenizer(infoText, "\n");

        String nextT;
        while (info.hasMoreTokens()) {
            nextT = info.nextToken();
            StringTokenizer line = new StringTokenizer(nextT, "\t");

            if (nextT.indexOf("\t") > -1)
                infoTxt = infoTxt + para + line.nextToken() + sep + line.nextToken() + "</TR>";
            else
                infoTxt = infoTxt + "<TR height=5><TD align=center colspan=2><font size=4 face='courier'>" + nextT + "</TR>";
        }
        //System.out.println("<HTML><TABLE border='0' cellpadding='0' cellspacing='0'>"+infoTxt+" </TABLE></html>");
        return "<HTML><TABLE border='0' cellpadding='0' cellspacing='0'>" + infoTxt + " </TABLE></html>";
    }


    public static void showRefCard() {
        final JFrame refCardf = new JFrame();

        Icon RefIcon = new ImageIcon(readGif("/ReferenceCard.gif"));

        JLabel RefLabel = new JLabel(RefIcon);
        JScrollPane refScrollPane = new JScrollPane(RefLabel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        refCardf.getContentPane().add("Center", refScrollPane);
        refCardf.setTitle("Mondrian - Reference Card");
        refCardf.setResizable(false);
        refCardf.pack();
        refCardf.setSize(refCardf.getWidth(), Math.min(refCardf.getHeight(), (Toolkit.getDefaultToolkit().getScreenSize()).height - 34));
        refCardf.setLocation((Toolkit.getDefaultToolkit().getScreenSize()).width - refCardf.getWidth(), 0);
        refCardf.show();

        refCardf.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                refCardf.dispose();
            }
        });
        refCardf.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getModifiers() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode() == KeyEvent.VK_W)
                    refCardf.dispose();
            }
        });
    }


    public static Preferences getPrefs() {
        return Preferences.userNodeForPackage(Utils.class);
    }


    public static boolean isMacOS() {
        return ((System.getProperty("os.name")).toLowerCase()).contains("mac");
    }


    public static boolean isDeployed() {
        return System.getProperty("apple.laf.useScreenMenuBar") != null;
    }
}

 

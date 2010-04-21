package org.rosuda.mondrian;


import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;
import java.io.*;
import java.util.*;


class StreamHogOld extends Thread {

    InputStream is;


    StreamHogOld(InputStream is) {
        this.is = is;
        start();
    }


    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.println("Rserve>" + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


public class Srs {

    public static boolean launchRserve(String cmd) {
        return launchRserve(cmd, "--vanilla --slave", "--no-save --slave");
    }


    public static boolean launchRserve(String cmd, String rargs, String rsrvargs) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                    "/bin/sh", "-c",
                    "echo 'library(Rserve);Rserve(args=\"" + rsrvargs + "\")'|" + cmd + " " + rargs});

            System.out.println("waiting for Rserve to start ... (" + p + ")");
            // we need to fetch the output - some platforms will die if you don't ...
            StreamHogOld errorHog = new StreamHogOld(p.getErrorStream());
            StreamHogOld outputHog = new StreamHogOld(p.getInputStream());
            p.waitFor();
            System.out.println("call terminated, let us try to connect ...");
        } catch (Exception x) {
            System.out.println("failed to start Rserve process with " + x.getMessage());
            return false;
        }
        try {
            RConnection c = new RConnection();
            System.out.println("Rserve is running.");
            c.close();
            return true;
        } catch (Exception e2) {
            System.out.println("Try failed with: " + e2.getMessage());
        }
        return false;
    }


    public static boolean checkLocalRserve() {
        try {
            RConnection c = new RConnection();
            System.out.println("Rserve is running.");
            c.close();
            return true;
        } catch (Exception e) {
            System.out.println("First connect try failed with: " + e.getMessage());
        }
        return (launchRserve("R") ||
                ((new File("/usr/local/lib/R/bin/R")).exists() && launchRserve("/usr/local/lib/R/bin/R")) ||
                ((new File("/usr/lib/R/bin/R")).exists() && launchRserve("/usr/lib/R/bin/R")) ||
                ((new File("/usr/local/bin/R")).exists() && launchRserve("/usr/local/bin/R")) ||
                ((new File("/sw/bin/R")).exists() && launchRserve("/sw/bin/R")) ||
                ((new File("/Library/Frameworks/R.framework/Resources/bin/R")).exists() && launchRserve("/Library/Frameworks/R.framework/Resources/bin/R")));
    }


    public static void main(String[] args) {
        System.out.println("result=" + checkLocalRserve());
        try {
            RConnection c = new RConnection();
            c.shutdown();
        } catch (Exception ignored) {
        }
    }
}

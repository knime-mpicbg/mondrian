package org.rosuda.mondrian;//package org.rosuda.Mondrian;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Funktionsweise: readFile() liest Datei in den Speicher als Buffer dieser wird dann nach Vorarbeiten (= Formatanalyse,
 * Fehleranalyse, etc.) in einzelne items zerlegt. In item[][] werden 2 Arten von Information gespeichert: a) wenn
 * Spalte numerisch ist, der dazugehörige numerische Wert als double b) wenn Spalte alphanumerisch ist, die Verweise auf
 * word[][][] In word[][][] werden die einzelnen Wörter der alphanumerischen Spalten gespeichert und das pro Wort genau
 * einmal. In s_word[][][] (nicht implementiert als Objektattribut) die dazugehörige sortierte Liste. In wordCount[][]
 * wird die absolute Häufigkeit eines Elements gespeichert. Dabei gibt der erste Index die Spalte und der zweite Index
 * den Ort des Elements an, welches sich befinden kann in: a) word[][][], wenn Spalte alphanumerisch ist b)
 * discretValue[][], wenn Spalte numerisch ist. isDiscret[] gibt an, ob eine Spalte diskret ist oder nicht.
 * Alphanumerische Spalte sind automatisch diskret. Bei numerischen Spalten sind diejenigen Spalten diskret, die weniger
 * als discretLimit verschiedene Elemente haben, d.h. ab discretLimit wird die Spalte bereits als nichtdiskret
 * angesehen. wordStackSize[] gibt den Limit für existierende Wörter in word[][][] bzw. in discretValue[][] an, je
 * nachdem ob Spalte numerisch diskret oder alphanumerisch ist. In head[][] wird die Kopfzeile gespeichert. Achtung:
 * Kopf MUSS existieren. numericalColumn[] gibt an, ob eine Spalte numerisch ist oder nicht. NA[][] gibt an, ob ein
 * Element ein Missing ist. softFehler und maximal 1 hardFehler werden in error[] gespeichert. errorposition merkt sich
 * die Position des harten Fehlers im Buffer. Dieser kann dann mit Hilfe von findRegion() eine Umgebung des Fehlers
 * ausgeben. isPolygonAvailable gibt an, ob ein Polygon im Datensatz existiert. TimeStamps[] gibt die Zeitabstände an,
 * mit denen ein ActionEvent für eine ProgressBar ausgelöst wird.
 * <p/>
 * getItem() gibt ein Element der DataMatrix als Objekt aus. Dieses muss danach konvertiert werden in a) char[], wenn es
 * sich um ein Wort handelt b) double[] und dann Zugriff auf das erste Element, wenn es eine Zahl ist. checkIt()
 * analysiert, ob die Wörter des Datensatzes auch richtig gespeichert wurden. -> wird später noch auf Zahlen
 * fortgesetzt.
 */


public class BufferTokenizer {

    /**
     * some constants for easy reading
     */
    final byte TAB = (byte) '\t';
    final byte SPACE = (byte) ' ';
    final byte NEWLINE = (byte) '\n';
    final byte RETURN = (byte) '\r';
    final byte DOT = (byte) '.';
    final byte MINUS = (byte) '-';
    final byte QUOTE = (byte) '"';
    final byte KOMMA = (byte) ',';

    private ProgressIndicator prId;

    /**
     * #columns and #lines
     */
    int columns, lines;

    int discretLimit = 0; // ab discretLimit inkl. bereits Behandlung als nicht-diskret
    String format;

    /**
     * headline (j,k) = (column, letter)
     */
    byte[][] head;

    /**
     * items (j,i) = (column, line) if column is numerical: numerical value saved else: saved reference to item in
     * word[][][]
     */
    double[][] item;

    /**
     * words (j,i,k) = (column, line, letter) if column is not numerical: words saved else: null equal words are NOT saved
     * twice NOTE: first word, which is null in a column, indicates the end of a column -> see wordStackSize[]
     */
    byte[][][] word;

/** sorted words: like words, but sorted
 sorted list is not a class attribute for speed reasons */
    // char[][][] s_word;

    /**
     * numericalColumn (j) = (column) if true: column is numerical else: column is not numerical
     */
    boolean[] numericalColumn;

    boolean[] isPhoneNum;  //MTh

    /**
     * isDiscret (j) = (column) if true: column is discret else: column is continuous NOTE: not numerical columns are
     * always discret
     */
    boolean[] isDiscret;

    /**
     * NA (j,i) = (column, line) saves missings (missings = NA or NaN) if true: element (i,j) is a missing one else: not a
     * missing
     */
    boolean[][] NA;

    /**
     * NACount counts amount of NA's in a numerical column
     */
    int[] NACount;

    /**
     * wordCount (j,i) = (column, line) referenced to word[][][] or item[][] counts #appearence of a word or numerical
     * discret values in a column NOTE: first element, which is 0 in a column, indicates the end of a column in wordCount
     * -> see wordStackSize[]
     */
    int[][] wordCount;

    /**
     * wordStackSize (j) = (column) limit position for existing words in word[][][] for a column
     */
    int[] wordStackSize;

    /**
     * used for handling discret values in a numerical column
     */
    double[][] discretValue;

// 	i think i do not need it any more
    double[] doubleCover = new double[1];

    /**
     * default word sepearator in a line
     */
    byte SEPERATOR = TAB;

// 	for a routine
    boolean wordNotFound = false;

    /**
     * newline-seperator
     */
    String newLineBreaker = "";

    /**
     * name of filename
     */
    String file;

    /**
     * buffer into which the file is loaded
     */
    ByteBuffer buffer;

    /**
     * error: String of softerrors, if harderror occurs System exits, harderror is saved in Sring[] error hardReadError: if
     * true: hard reading error occured errorposition: position of hard reading error
     */
    String[] error = null;
    boolean hardReadError = false;
    int errorposition;

    int positionSecondLine = 0;

    /**
     * timeStamps: in which time distances new ActionEvent is processed timeStampCounter: reference to
     * timeStamps[]-elements
     */
    int[] timeStamps = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 1000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 10000, 100000, 100000, 100000, 100000, 100000, 100000};
    long timestart = 0;
    long timestop = 0;
    int timeStampCounter = 0;


    /**
     * isPolygonAvailable: indicates availability of a polygon polygonName: name of polygon in DataMatrix
     */
    boolean isPolygonAvailable = false;
    String polygonName = null;
    int polygonID = -1;


    /**
     * potence: for precisioned calculating of doubles from chars warning: 10e23 := 9.999999999999999E22 (only this
     * number)
     */
    double[] potence = {1.0e0, 1.0e1, 1.0e2, 1.0e3, 1.0e4, 1.0e5, 1.0e6, 1.0e7, 1.0e8, 1.0e9,
            1.0e10, 1.0e11, 1.0e12, 1.0e13, 1.0e14, 1.0e15, 1.0e16, 1.0e17, 1.0e18, 1.0e19,
            1.0e20, 1.0e21, 1.0e22, 1.0e23, 1.0e24, 1.0e25, 1.0e26, 1.0e27, 1.0e28, 1.0e29,
            1.0e30, 1.0e31, 1.0e32, 1.0e33, 1.0e34, 1.0e35, 1.0e36, 1.0e37, 1.0e38, 1.0e39,
            1.0e40, 1.0e41, 1.0e42, 1.0e43, 1.0e44, 1.0e45, 1.0e46, 1.0e47, 1.0e48, 1.0e49,
            1.0e50, 1.0e51, 1.0e52, 1.0e53, 1.0e54, 1.0e55, 1.0e56, 1.0e57, 1.0e58, 1.0e59,
            1.0e60, 1.0e61, 1.0e62, 1.0e63, 1.0e64, 1.0e65, 1.0e66, 1.0e67, 1.0e68, 1.0e69,
            1.0e70, 1.0e71, 1.0e72, 1.0e73, 1.0e74, 1.0e75, 1.0e76, 1.0e77, 1.0e78, 1.0e79,
            1.0e80, 1.0e81, 1.0e82, 1.0e83, 1.0e84, 1.0e85, 1.0e86, 1.0e87, 1.0e88, 1.0e89,
            1.0e90, 1.0e91, 1.0e92, 1.0e93, 1.0e94, 1.0e95, 1.0e96, 1.0e97, 1.0e98, 1.0e99,
            1.0e100, 1.0e101, 1.0e102, 1.0e103, 1.0e104, 1.0e105, 1.0e106, 1.0e107, 1.0e108, 1.0e109,
            1.0e110, 1.0e111, 1.0e112, 1.0e113, 1.0e114, 1.0e115, 1.0e116, 1.0e117, 1.0e118, 1.0e119,
            1.0e120, 1.0e121, 1.0e122, 1.0e123, 1.0e124, 1.0e125, 1.0e126, 1.0e127, 1.0e128, 1.0e129,
            1.0e130, 1.0e131, 1.0e132, 1.0e133, 1.0e134, 1.0e135, 1.0e136, 1.0e137, 1.0e138, 1.0e139,
            1.0e140, 1.0e141, 1.0e142, 1.0e143, 1.0e144, 1.0e145, 1.0e146, 1.0e147, 1.0e148, 1.0e149,
            1.0e150, 1.0e151, 1.0e152, 1.0e153, 1.0e154, 1.0e155, 1.0e156, 1.0e157, 1.0e158, 1.0e159,
            1.0e160, 1.0e161, 1.0e162, 1.0e163, 1.0e164, 1.0e165, 1.0e166, 1.0e167, 1.0e168, 1.0e169,
            1.0e170, 1.0e171, 1.0e172, 1.0e173, 1.0e174, 1.0e175, 1.0e176, 1.0e177, 1.0e178, 1.0e179,
            1.0e180, 1.0e181, 1.0e182, 1.0e183, 1.0e184, 1.0e185, 1.0e186, 1.0e187, 1.0e188, 1.0e189,
            1.0e190, 1.0e191, 1.0e192, 1.0e193, 1.0e194, 1.0e195, 1.0e196, 1.0e197, 1.0e198, 1.0e199,
            1.0e200, 1.0e201, 1.0e202, 1.0e203, 1.0e204, 1.0e205, 1.0e206, 1.0e207, 1.0e208, 1.0e209,
            1.0e210, 1.0e211, 1.0e212, 1.0e213, 1.0e214, 1.0e215, 1.0e216, 1.0e217, 1.0e218, 1.0e219,
            1.0e220, 1.0e221, 1.0e222, 1.0e223, 1.0e224, 1.0e225, 1.0e226, 1.0e227, 1.0e228, 1.0e229,
            1.0e230, 1.0e231, 1.0e232, 1.0e233, 1.0e234, 1.0e235, 1.0e236, 1.0e237, 1.0e238, 1.0e239,
            1.0e240, 1.0e241, 1.0e242, 1.0e243, 1.0e244, 1.0e245, 1.0e246, 1.0e247, 1.0e248, 1.0e249,
            1.0e250, 1.0e251, 1.0e252, 1.0e253, 1.0e254, 1.0e255, 1.0e256, 1.0e257, 1.0e258, 1.0e259,
            1.0e260, 1.0e261, 1.0e262, 1.0e263, 1.0e264, 1.0e265, 1.0e266, 1.0e267, 1.0e268, 1.0e269,
            1.0e270, 1.0e271, 1.0e272, 1.0e273, 1.0e274, 1.0e275, 1.0e276, 1.0e277, 1.0e278, 1.0e279,
            1.0e280, 1.0e281, 1.0e282, 1.0e283, 1.0e284, 1.0e285, 1.0e286, 1.0e287, 1.0e288, 1.0e289,
            1.0e290, 1.0e291, 1.0e292, 1.0e293, 1.0e294, 1.0e295, 1.0e296, 1.0e297, 1.0e298, 1.0e299,
            1.0e300, 1.0e301, 1.0e302, 1.0e303, 1.0e304, 1.0e305, 1.0e306, 1.0e307, 1.0e308};

    double[] negpotence = {1.0e-0, 1.0e-1, 1.0e-2, 1.0e-3, 1.0e-4, 1.0e-5, 1.0e-6, 1.0e-7, 1.0e-8, 1.0e-9,
            1.0e-10, 1.0e-11, 1.0e-12, 1.0e-13, 1.0e-14, 1.0e-15, 1.0e-16, 1.0e-17, 1.0e-18, 1.0e-19,
            1.0e-20, 1.0e-21, 1.0e-22, 1.0e-23, 1.0e-24, 1.0e-25, 1.0e-26, 1.0e-27, 1.0e-28, 1.0e-29,
            1.0e-30, 1.0e-31, 1.0e-32, 1.0e-33, 1.0e-34, 1.0e-35, 1.0e-36, 1.0e-37, 1.0e-38, 1.0e-39,
            1.0e-40, 1.0e-41, 1.0e-42, 1.0e-43, 1.0e-44, 1.0e-45, 1.0e-46, 1.0e-47, 1.0e-48, 1.0e-49,
            1.0e-50, 1.0e-51, 1.0e-52, 1.0e-53, 1.0e-54, 1.0e-55, 1.0e-56, 1.0e-57, 1.0e-58, 1.0e-59,
            1.0e-60, 1.0e-61, 1.0e-62, 1.0e-63, 1.0e-64, 1.0e-65, 1.0e-66, 1.0e-67, 1.0e-68, 1.0e-69,
            1.0e-70, 1.0e-71, 1.0e-72, 1.0e-73, 1.0e-74, 1.0e-75, 1.0e-76, 1.0e-77, 1.0e-78, 1.0e-79,
            1.0e-80, 1.0e-81, 1.0e-82, 1.0e-83, 1.0e-84, 1.0e-85, 1.0e-86, 1.0e-87, 1.0e-88, 1.0e-89,
            1.0e-90, 1.0e-91, 1.0e-92, 1.0e-93, 1.0e-94, 1.0e-95, 1.0e-96, 1.0e-97, 1.0e-98, 1.0e-99,
            1.0e-100, 1.0e-101, 1.0e-102, 1.0e-103, 1.0e-104, 1.0e-105, 1.0e-106, 1.0e-107, 1.0e-108, 1.0e-109,
            1.0e-110, 1.0e-111, 1.0e-112, 1.0e-113, 1.0e-114, 1.0e-115, 1.0e-116, 1.0e-117, 1.0e-118, 1.0e-119,
            1.0e-120, 1.0e-121, 1.0e-122, 1.0e-123, 1.0e-124, 1.0e-125, 1.0e-126, 1.0e-127, 1.0e-128, 1.0e-129,
            1.0e-130, 1.0e-131, 1.0e-132, 1.0e-133, 1.0e-134, 1.0e-135, 1.0e-136, 1.0e-137, 1.0e-138, 1.0e-139,
            1.0e-140, 1.0e-141, 1.0e-142, 1.0e-143, 1.0e-144, 1.0e-145, 1.0e-146, 1.0e-147, 1.0e-148, 1.0e-149,
            1.0e-150, 1.0e-151, 1.0e-152, 1.0e-153, 1.0e-154, 1.0e-155, 1.0e-156, 1.0e-157, 1.0e-158, 1.0e-159,
            1.0e-160, 1.0e-161, 1.0e-162, 1.0e-163, 1.0e-164, 1.0e-165, 1.0e-166, 1.0e-167, 1.0e-168, 1.0e-169,
            1.0e-170, 1.0e-171, 1.0e-172, 1.0e-173, 1.0e-174, 1.0e-175, 1.0e-176, 1.0e-177, 1.0e-178, 1.0e-179,
            1.0e-180, 1.0e-181, 1.0e-182, 1.0e-183, 1.0e-184, 1.0e-185, 1.0e-186, 1.0e-187, 1.0e-188, 1.0e-189,
            1.0e-190, 1.0e-191, 1.0e-192, 1.0e-193, 1.0e-194, 1.0e-195, 1.0e-196, 1.0e-197, 1.0e-198, 1.0e-199,
            1.0e-200, 1.0e-201, 1.0e-202, 1.0e-203, 1.0e-204, 1.0e-205, 1.0e-206, 1.0e-207, 1.0e-208, 1.0e-209,
            1.0e-210, 1.0e-211, 1.0e-212, 1.0e-213, 1.0e-214, 1.0e-215, 1.0e-216, 1.0e-217, 1.0e-218, 1.0e-219,
            1.0e-220, 1.0e-221, 1.0e-222, 1.0e-223, 1.0e-224, 1.0e-225, 1.0e-226, 1.0e-227, 1.0e-228, 1.0e-229,
            1.0e-230, 1.0e-231, 1.0e-232, 1.0e-233, 1.0e-234, 1.0e-235, 1.0e-236, 1.0e-237, 1.0e-238, 1.0e-239,
            1.0e-240, 1.0e-241, 1.0e-242, 1.0e-243, 1.0e-244, 1.0e-245, 1.0e-246, 1.0e-247, 1.0e-248, 1.0e-249,
            1.0e-250, 1.0e-251, 1.0e-252, 1.0e-253, 1.0e-254, 1.0e-255, 1.0e-256, 1.0e-257, 1.0e-258, 1.0e-259,
            1.0e-260, 1.0e-261, 1.0e-262, 1.0e-263, 1.0e-264, 1.0e-265, 1.0e-266, 1.0e-267, 1.0e-268, 1.0e-269,
            1.0e-270, 1.0e-271, 1.0e-272, 1.0e-273, 1.0e-274, 1.0e-275, 1.0e-276, 1.0e-277, 1.0e-278, 1.0e-279,
            1.0e-280, 1.0e-281, 1.0e-282, 1.0e-283, 1.0e-284, 1.0e-285, 1.0e-286, 1.0e-287, 1.0e-288, 1.0e-289,
            1.0e-290, 1.0e-291, 1.0e-292, 1.0e-293, 1.0e-294, 1.0e-295, 1.0e-296, 1.0e-297, 1.0e-298, 1.0e-299,
            1.0e-300, 1.0e-301, 1.0e-302, 1.0e-303, 1.0e-304, 1.0e-305, 1.0e-306, 1.0e-307, 1.0e-308, 1.0e-309,
            1.0e-310, 1.0e-311, 1.0e-312, 1.0e-313, 1.0e-314, 1.0e-315, 1.0e-316, 1.0e-317, 1.0e-318, 1.0e-319,
            1.0e-320, 1.0e-321, 1.0e-322, 1.0e-323};


    /**
     * sets limit for discret handling of numerical columns
     *
     * @param i
     */
    public void setDiscretLimit(int n) {
        this.discretLimit = (n > 800) ? (15 * Math.max(1, (int) (Math.log(n) / Math.log(10)) - 1)) : ((int) (1.5 * Math.sqrt(n)));
        //	discretLimit = n;
        System.out.println(" Set Limit to: " + discretLimit);
    }


    /**
     * method to get converted items as Objects
     *
     * @param j column-position
     * @param i line-position
     * @return Object in column j, line i
     */
    public Object getItem(int j, int i) {

        if (!numericalColumn[j]) {
            // word
            return byteToCharArray(pointToWord(j, i));
        } else {
            // double
            doubleCover[0] = item[j][i];
            return doubleCover;
        }

    }


    /**
     * just a little pointer to word in saved list
     *
     * @param j column-position
     * @param i line-position
     * @return byte[] found in position (j,i) in data-matrix
     */
    public byte[] pointToWord(int j, int i) {

        return word[j][(int) item[j][i]];

    }


    /**
     * BufferTokenizer:
     *
     * @param discretLimit   discretLimit setting. for good speed use small discretLimit
     * @param acceptedErrors make sure acceptedErrors should be >=1
     */

    public BufferTokenizer(int discretLimit, int acceptedErrors, String file, ProgressIndicator pi) throws UnacceptableFormatException, ScanException, OutOfMemoryError {
        long start, stop;
        long startfull, stopfull;
        error = new String[acceptedErrors];
        prId = pi;

        timestart = System.currentTimeMillis();

        startfull = System.currentTimeMillis();

        start = System.currentTimeMillis();
        try {
            buffer = readFile(file);
        } catch (IOException e) {
            handleException(e);
            System.err.println(e);
            System.exit(0);
        }
        stop = System.currentTimeMillis();
        System.out.println("Harddrive to RAM: " + (stop - start));
        prId.setProgress(.05);

        newLineBreaker = getNewLineBreaker(buffer);

        format = analyzeFormat(buffer);
        if (format == "TAB-Format") SEPERATOR = TAB;
        else if (format == "SPACE-Format") SEPERATOR = SPACE;
        else if (format == "KOMMA-Format") SEPERATOR = KOMMA;
        else if (format == "KOMMA-QUOTE-Format") SEPERATOR = KOMMA;
        else if (format == "UNKNOWN-Format") {
            System.out.println(format);
            throw new UnacceptableFormatException();
        }

        columns = amountColumns(buffer, format);

        // polygontest
        isPolygonAvailable = isPolygonAvailableInHead(buffer, format);
        if (isPolygonAvailable) {
            polygonName = getPolygonName(buffer);
            // some errors during polygon seeking routine might happen before real error analysis
            // so here they are handled
            if (hardReadError) {
                System.out.println(error[0]);
                throw new ScanException(error[0]);
            }
        }
        setDiscretLimit(discretLimit);

        System.out.println("Format: " + format);

        if (format == "TAB-Format") {
            // Format testen
            start = System.currentTimeMillis();
            error = testUNQUOTEDFormat(buffer, SEPERATOR, acceptedErrors);
            stop = System.currentTimeMillis();
            System.out.println("testTABFormat: " + (stop - start));
            for (int x = 0; x < error.length && error[x] != null; x++) {
                System.out.println("" + (x + 1) + ". " + error[x]);
            }
            if (hardReadError) {
                System.out.println(findRegion(buffer, errorposition));
//              throw new ScanException(findRegion(buffer,errorposition).toString());
                String printError = "";
                for (int i = 0; i < error.length; i++)
                    if (error[i] != null)
                        printError = error[i];
                throw new ScanException(printError + "!   ");//        \nContext is \n ..."+findRegion(buffer,errorposition).toString()+"... ");
            }

            buffer.rewind();

            // Anzahl der Zeilen lesen und Spaltenstruktur erkennen
            start = System.currentTimeMillis();
            numericalColumn = new boolean[columns];
            positionSecondLine = getPositionSecondLine(buffer);
            lines = amountLines(buffer, format);

            setDiscretLimit(lines);

            if (buffer.get(buffer.limit() - 1) == NEWLINE) {
                if (buffer.get(buffer.limit() - 2) == RETURN) {
                    buffer.limit(buffer.limit() - 2);
                    lines--;
                } else {
                    buffer.limit(buffer.limit() - 1);
                    lines--;
                }
            } else if (buffer.get(buffer.limit() - 1) == RETURN) {
                buffer.limit(buffer.limit() - 1);
                lines--;
            }
            stop = System.currentTimeMillis();
            System.out.println("Zeilen lesen: " + (stop - start));

            // Daten initialisieren
            start = System.currentTimeMillis();
            isDiscret = new boolean[columns];
            for (int i = 0; i < isDiscret.length; i++) isDiscret[i] = true;
            isPhoneNum = new boolean[columns];  // MTh
            for (int i = 0; i < isPhoneNum.length; i++) isPhoneNum[i] = false;
            item = new double[columns][lines];
            NA = new boolean[columns][lines];
            word = new byte[columns][this.discretLimit][];
            wordCount = new int[columns][lines];
            NACount = new int[columns];
            discretValue = new double[columns][this.discretLimit];
            wordStackSize = new int[columns];
            stop = System.currentTimeMillis();
            System.out.println("Initialisierungen: " + (stop - start));

            buffer.rewind();

            // Daten einlesen und abspeichern
            head = readHead(buffer, format);

            start = System.currentTimeMillis();
            buffer.rewind();
            tokenizeUNQUOTEDBuffer(buffer, SEPERATOR);
            stop = System.currentTimeMillis();
            System.out.println("tokenize TABBuffer: " + (stop - start));


        } else if (format == "SPACE-Format") {
            // Format testen und Anzahl der Zeilen lesen
            start = System.currentTimeMillis();
            positionSecondLine = getPositionSecondLine(buffer);
            numericalColumn = new boolean[columns];
            error = testQUOTEDFormat(buffer, SEPERATOR, acceptedErrors);
            stop = System.currentTimeMillis();
            System.out.println("testSPACEFormat: " + (stop - start));
            for (int x = 0; x < error.length && error[x] != null; x++) {
                System.out.println("" + (x + 1) + ". " + error[x]);
            }
            if (hardReadError) {
                System.out.println(findRegion(buffer, errorposition));
                throw new ScanException(findRegion(buffer, errorposition).toString());
            }

            if (buffer.get(buffer.limit() - 1) == NEWLINE) {
                if (buffer.get(buffer.limit() - 2) == RETURN) {
                    buffer.limit(buffer.limit() - 2);
                    lines--;
                } else {
                    buffer.limit(buffer.limit() - 1);
                    lines--;
                }
            } else if (buffer.get(buffer.limit() - 1) == RETURN) {
                buffer.limit(buffer.limit() - 1);
                lines--;
            }

            buffer.rewind();

            // Daten initialisieren
            start = System.currentTimeMillis();
            isDiscret = new boolean[columns];
            for (int i = 0; i < isDiscret.length; i++) isDiscret[i] = true;
            isPhoneNum = new boolean[columns];  // MTh
            for (int i = 0; i < isPhoneNum.length; i++) isPhoneNum[i] = false;
            item = new double[columns][lines];
            NA = new boolean[columns][lines];
            word = new byte[columns][discretLimit][];
            wordCount = new int[columns][lines];
            NACount = new int[columns];
            discretValue = new double[columns][discretLimit];
            wordStackSize = new int[columns];
            stop = System.currentTimeMillis();
            System.out.println("Initialisierungen: " + (stop - start));

            buffer.rewind();

            // Daten einlesen und abspeichern
            head = readHead(buffer, format);

            start = System.currentTimeMillis();
            buffer.rewind();
            tokenizeQUOTEDBuffer(buffer, SEPERATOR);
            stop = System.currentTimeMillis();
            System.out.println("tokenize SPACEBuffer: " + (stop - start));

        } else if (format == "KOMMA-Format") {
            // Format testen
            start = System.currentTimeMillis();
            error = testUNQUOTEDFormat(buffer, SEPERATOR, acceptedErrors);
            stop = System.currentTimeMillis();
            System.out.println("testKOMMAFormat: " + (stop - start));
            for (int x = 0; x < error.length && error[x] != null; x++) {
                System.out.println("" + (x + 1) + ". " + error[x]);
            }
            if (hardReadError) {
                System.out.println(findRegion(buffer, errorposition));
                throw new ScanException(findRegion(buffer, errorposition).toString());
            }

            buffer.rewind();

            // Anzahl der Zeilen lesen und Spaltenstruktur erkennen
            start = System.currentTimeMillis();
            numericalColumn = new boolean[columns];
            positionSecondLine = getPositionSecondLine(buffer);
            lines = amountLines(buffer, format);
            if (buffer.get(buffer.limit() - 1) == NEWLINE) {
                if (buffer.get(buffer.limit() - 2) == RETURN) {
                    buffer.limit(buffer.limit() - 2);
                    lines--;
                } else {
                    buffer.limit(buffer.limit() - 1);
                    lines--;
                }
            } else if (buffer.get(buffer.limit() - 1) == RETURN) {
                buffer.limit(buffer.limit() - 1);
                lines--;
            }
            stop = System.currentTimeMillis();
            System.out.println("Zeilen lesen: " + (stop - start));

            // Daten initialisieren
            start = System.currentTimeMillis();
            isDiscret = new boolean[columns];
            for (int i = 0; i < isDiscret.length; i++) isDiscret[i] = true;
            isPhoneNum = new boolean[columns];  // MTh
            for (int i = 0; i < isPhoneNum.length; i++) isPhoneNum[i] = false;
            item = new double[columns][lines];
            NA = new boolean[columns][lines];
            word = new byte[columns][discretLimit][];
            wordCount = new int[columns][lines];
            NACount = new int[columns];
            discretValue = new double[columns][discretLimit];
            wordStackSize = new int[columns];
            stop = System.currentTimeMillis();
            System.out.println("Initialisierungen: " + (stop - start));

            buffer.rewind();

            // Daten einlesen und abspeichern
            head = readHead(buffer, format);

            start = System.currentTimeMillis();
            buffer.rewind();
            tokenizeUNQUOTEDBuffer(buffer, SEPERATOR);
            stop = System.currentTimeMillis();
            System.out.println("tokenize KOMMABuffer: " + (stop - start));

        } else if (format == "KOMMA-QUOTE-Format") {
            // Format testen und Anzahl der Zeilen lesen
            start = System.currentTimeMillis();
            positionSecondLine = getPositionSecondLine(buffer);
            numericalColumn = new boolean[columns];
            error = testQUOTEDFormat(buffer, SEPERATOR, acceptedErrors);
            stop = System.currentTimeMillis();
            System.out.println("testKOMMAQUOTEFormat: " + (stop - start));
            for (int x = 0; x < error.length && error[x] != null; x++) {
                System.out.println("" + (x + 1) + ". " + error[x]);
            }
            if (hardReadError) {
                System.out.println(findRegion(buffer, errorposition));
                throw new ScanException(findRegion(buffer, errorposition).toString());
            }

            if (buffer.get(buffer.limit() - 1) == NEWLINE) {
                if (buffer.get(buffer.limit() - 2) == RETURN) {
                    buffer.limit(buffer.limit() - 2);
                    lines--;
                } else {
                    buffer.limit(buffer.limit() - 1);
                    lines--;
                }
            } else if (buffer.get(buffer.limit() - 1) == RETURN) {
                buffer.limit(buffer.limit() - 1);
                lines--;
            }

            buffer.rewind();

            // Daten initialisieren
            start = System.currentTimeMillis();
            isDiscret = new boolean[columns];
            for (int i = 0; i < isDiscret.length; i++) isDiscret[i] = true;
            isPhoneNum = new boolean[columns];  // MTh
            for (int i = 0; i < isPhoneNum.length; i++) isPhoneNum[i] = false;
            item = new double[columns][lines];
            NA = new boolean[columns][lines];
            word = new byte[columns][discretLimit][];
            wordCount = new int[columns][lines];
            NACount = new int[columns];
            discretValue = new double[columns][discretLimit];
            wordStackSize = new int[columns];
            stop = System.currentTimeMillis();
            System.out.println("Initialisierungen: " + (stop - start));

            buffer.rewind();

            // Daten einlesen und abspeichern
            head = readHead(buffer, format);

            start = System.currentTimeMillis();
            buffer.rewind();
            tokenizeQUOTEDBuffer(buffer, SEPERATOR);
            stop = System.currentTimeMillis();
            System.out.println("tokenize KOMMAQUOTEBuffer: " + (stop - start));


        } else {
            System.err.println(format);
            throw new UnacceptableFormatException();
        }

        stopfull = System.currentTimeMillis();
        System.out.println("Gesamtzeit: " + (stopfull - startfull));

        prId.setProgress(.98);

        System.out.print("Total memory used: ");
        System.out.println((Runtime.getRuntime()).totalMemory());


/**        head output **/
/*
		System.out.print("	");
		for(int j=0; j<head.length; j++) {
			for(int i=0; i<head[j].length; i++) {
				System.out.print((char)head[j][i]);
			}
			System.out.print("	");
		}
		System.out.println();
*/

/**     items output **/
/*
		for(int i=0; i<lines; i++) {
			System.out.print(i); System.out.print("	");
			for(int j=0; j<columns; j++) {
				// System.out.print(item[j][i]); System.out.print("	");
				// System.out.print(NA[j][i]);
				if(numericalColumn[j]) {
					double item = ((double[])getItem(j,i))[0];
					if(item==Double.MAX_VALUE) System.out.print("NA");
					else System.out.print(item);
				}
				else System.out.print((char[])getItem(j,i));
				System.out.print("	");
			}
			System.out.println();
		}
*/


/**        word correctness test **/
/*		
         start = System.currentTimeMillis();
        if(checkIt(SEPERATOR)) {
            System.out.println("words saved correctly");
        } else {
            System.out.println("words NOT saved correctly");
        }
        stop = System.currentTimeMillis();
        System.out.println("Time for wordcorrectness-test: " + (stop-start));
*/

/**     isDiscret[j] output **/
/*		for(int j=0; j<columns; j++) {
            System.out.print(isDiscret[j]); System.out.print("	");
        }
        System.out.println();
*/

        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < columns; j++) {
                if (NA[j][i]) NACount[j]++;
            }
        }


    }


    /**
     * file to ByteBuffer converter
     *
     * @param file filename
     * @return converted ByteBuffer
     * @throws IOException if errors occured during reading
     */
    public ByteBuffer readFile(String file) throws IOException {
        FileChannel fc = (new FileInputStream(file)).getChannel();
        ByteBuffer buffer = ByteBuffer.allocate((int) fc.size());
        fc.read(buffer);
        fc.close();
        return buffer;
    }


    /**
     * checks byte, if it is a real char problems on other platforms: so maybe false information if ASCII(byte)>128 real
     * chars (here): 65..90, 97..122, 192..255
     *
     * @param b byte
     * @return true, if b is a real char, else false
     */
    private boolean isChar(byte b) {

        if ((b >= 65 && b <= 90) || (b >= 97 && b <= 122) || (b >= 192 && b <= 255))
            return true;
        else
            return false;

    }


    /**
     * checks byte to be a real number real number = 48..57
     *
     * @param b byte
     * @return true, if b is a real number, else false
     */
    private boolean isNumber(byte b) {

        if (b >= 48 && b <= 57)
            return true;
        else
            return false;

    }


    /**
     * analyzes text-format in ByteBuffer "SPACE-Format" = ... ..., where ... == number | "word" "KOMMA-QUOTE-Format" =
     * ...,... where ... == number | "word" "TAB-Format" = ...	..., where ... == number | word "KOMMA-Format" = ...,... ,
     * where ... == number | word
     *
     * @param buffer the associated ByteBuffer to read
     * @return "UNKNOWN-Format" if format not found out, "SPACE-Format", "KOMMA-QUOTE-Format" if, "TAB-Format",
     *         "KOMMA-Format"
     */
    public String analyzeFormat(ByteBuffer buffer) {

        buffer.rewind();
        byte b;
        String format = "UNKNOWN-Format"; // default
        int amountTAB = 0;
        int amountSPACE = 0;
        int amountKOMMA = 0;

        // untersuche Kopfzeile: WICHTIG: diese muss existieren!!!

        if (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == QUOTE) {
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == QUOTE) {
                        if (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == SPACE || b == RETURN || b == NEWLINE) {
                                format = "SPACE-Format";
                                break;
                            } else if (b == KOMMA) {
                                format = "KOMMA-QUOTE-Format";
                                break;
                            } else {
                                format = "UNKNOWN-Format";
                                break;
                            }
                        } else {
                            format = "SPACE-Format";
                            break;
                        }
                    } else if (b == TAB) {
                        format = "TAB-Format";
                        break;
                    }
                }
            } else {
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == TAB) {
                        format = "TAB-Format";
                        break;
                    } else if (b == KOMMA) {
                        format = "KOMMA-Format";
                        break;
                    } else if (b == RETURN || b == NEWLINE) {
                        format = "TAB-Format"; // für den Fall, dass doch keine Kopfzeile existiert oder nur 1 Spalte
                        break;
                    }
                }
            }
        }

        // untersuche die ersten zwei Zeilen auf das ausgewählte Format:

        buffer.rewind();
        for (int i = 0; i < 2; i++) {
            while (buffer.hasRemaining()) {
                b = buffer.get();
                if (b == TAB) {
                    amountTAB++;
                } else if (b == SPACE) {
                    if (buffer.hasRemaining()) {
                        if (buffer.get(buffer.position() - 2) == QUOTE && buffer.get(buffer.position()) == QUOTE) {
                            amountSPACE++;
                        }
                    }
                } else if (b == KOMMA) {
                    if (format == "KOMMA-QUOTE-Format") {
                        if (buffer.hasRemaining()) {
                            if (buffer.get(buffer.position() - 2) == QUOTE && buffer.get(buffer.position()) == QUOTE) {
                                amountKOMMA++;
                            }
                        }
                    } else {
                        amountKOMMA++;
                    }
                } else if (b == RETURN) {
                    if (buffer.hasRemaining()) {
                        buffer.mark();
                        if (buffer.get() == NEWLINE) {
                            break;
                        } else {
                            buffer.reset();
                            break;
                        }
                    }
                } else if (b == NEWLINE) {
                    break;
                }
            }
        }

        if (format == "TAB-Format") {
            if (amountTAB >= 2 * amountSPACE && amountTAB >= 2 * amountKOMMA) {

            } else {
                format = "UNKNOWN-Format";
            }
        } else if (format == "SPACE-Format") {
            if (amountSPACE >= 2 * amountTAB && amountSPACE >= 2 * amountKOMMA) {

            } else {
                format = "UNKNOWN-Format";
            }
        } else if (format == "KOMMA-Format" || format == "KOMMA-QUOTE-Format") {
            if (amountKOMMA >= 2 * amountTAB && amountKOMMA >= 2 * amountSPACE) {

            } else {
                format = "UNKNOWN-Format";
            }
        }


        return format;

        // returnable formats are:
        // UNKNOWN-Format (default), TAB-Format, SPACE-Format, KOMMA-Format, KOMMA-QUOTE-Format
    }


    /**
     * reads out headline in ByteBuffer
     *
     * @param buffer associated ByteBuffer
     * @param format associated formation of text in ByteBuffer
     * @return headline ([word][letter])
     */
    public byte[][] readHead(ByteBuffer buffer, String format) {

        buffer.rewind();
        byte b;
        int k = 0;
        int j = 0;
        byte[][] head = new byte[columns][];

        if (format == "TAB-Format" || format == "KOMMA-Format") {
            while (buffer.hasRemaining()) {

                k = 0;
                buffer.mark();
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        buffer.reset();
                        break;
                    } else if (b == RETURN || b == NEWLINE) {
                        buffer.reset();
                        break;
                    } else
                        k++;
                }
                if (!buffer.hasRemaining())
                    buffer.reset();
                head[j] = new byte[k];


                for (int l = 0; l < k; l++) {
                    b = buffer.get();
                    if (b < 0) System.out.println(b + " <-----");
//                    head[j][l] = (byte)( b>-1?b:256+b ); 		
                    head[j][l] = b;
                }

                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        j++;
                    } else if (b == RETURN || b == NEWLINE) {
                        buffer.rewind();
                        break;
                    }
                }
            }
        } else if (format == "SPACE-Format" || format == "KOMMA-QUOTE-Format") {


            while (buffer.hasRemaining()) {

                k = 0;
                buffer.position(buffer.position() + 1);

                buffer.mark();
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == QUOTE) {
                        buffer.reset();
                        break;
                    } else k++;
                }

                if (!buffer.hasRemaining())
                    buffer.reset();
                head[j] = new byte[k];


                for (int l = 0; l < k; l++) {
                    b = buffer.get();

                    head[j][l] = b;

                }

                buffer.position(buffer.position() + 1);
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        j++;
                    } else if (b == RETURN || b == NEWLINE) {
                        buffer.rewind();
                        break;
                    }
                }
            }
        }


        // Head-Analyzer
        byte[] temp;
        for (j = 0; j < head.length; j++) {
            if (head[j].length >= 2) {
                if (head[j][0] == '/') {
                    if (head[j][1] == 'C') {
                        isDiscret[j] = false;
                        temp = new byte[head[j].length - 2];
                        for (k = 0; k < head[j].length - 2; k++) {
                            temp[k] = head[j][k + 2];
                        }
                        head[j] = temp;
                    } else if (head[j][1] == 'D') {
                        isDiscret[j] = true;
                        temp = new byte[head[j].length - 2];
                        for (k = 0; k < head[j].length - 2; k++) {
                            temp[k] = head[j][k + 2];
                        }
                        head[j] = temp;
                    } else if (head[j][1] == 'T') {  //MTh
                        isDiscret[j] = true;
                        isPhoneNum[j] = true;
                        temp = new byte[head[j].length - 2];
                        for (k = 0; k < head[j].length - 2; k++) {
                            temp[k] = head[j][k + 2];
                        }
                        head[j] = temp;
                    } else if (head[j][1] == 'P') {
                        isDiscret[j] = true;
                        temp = new byte[head[j].length - 2];
                        for (k = 0; k < head[j].length - 2; k++) {
                            temp[k] = head[j][k + 2];
                        }
                        head[j] = temp;
                        polygonID = j;
                    } else if (head[j][1] == 'U') {
                        isDiscret[j] = true;
                        numericalColumn[j] = false;
                        temp = new byte[head[j].length - 2];
                        for (k = 0; k < head[j].length - 2; k++) {
                            temp[k] = head[j][k + 2];
                        }
                        String label;
                        try {
                            label = new String(temp, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new Error(e);
                        }
                        Matcher m = Pattern.compile("(.*)<([^>]+)>(.*)").matcher(label);
                        if (m.matches()) {
                            temp = m.group(2).getBytes();
                            String template = "<image src='"
                                    + m.group(1) + "$val" + m.group(3) + "'>";
                            Util.registerHTMLTemplate(m.group(2), template);
                        } else {
                            System.err.println("Unknown Url for column: " + label);
                        }

                        head[j] = temp;
                    }
                }
            }
        }
        return head;
    }


    /**
     * method to get position of second line in ByteBuffer
     *
     * @param buffer associated ByteBuffer
     * @return position of second line in data-matrix
     */
    public int getPositionSecondLine(ByteBuffer buffer) {

        buffer.rewind();
        byte b;
        int positionSecondLine = 0;
        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == RETURN) {
                buffer.mark();
                if (buffer.get() == NEWLINE) {
                    positionSecondLine = buffer.position();
                    break;
                } else {
                    buffer.reset();
                    positionSecondLine = buffer.position();
                    break;
                }
            } else if (b == NEWLINE) {
                positionSecondLine = buffer.position();
                break;
            }
        }
        buffer.rewind();
        return positionSecondLine;
    }


    /**
     * @param buffer the associated ByteBuffer to read
     * @param format the format to handle with
     * @return amount of columns
     */
    int amountColumns(ByteBuffer buffer, String format) throws ScanException {
        buffer.rewind();
        int j = 0;
        int k = 0;
        byte b;

        if (format == "TAB-Format" || format == "KOMMA-Format") {
            while (buffer.hasRemaining() == true) {
                b = buffer.get();
                if (b == SEPERATOR)
                    j++;
                if (b == NEWLINE || b == RETURN)
                    break;
            }
        } else { // SPACE-Format oder KOMMA-QUOTE-Format
            buffer.mark();
            while (buffer.hasRemaining()) {
                b = buffer.get();
                if (b == QUOTE) {
                    k++;
                } else if (b == RETURN || b == NEWLINE) {
                    break;
                }
            }
            if (k % 2 != 0) {
                System.out.println("ERROR: Uneven amount of quotes in headLine");
                throw new ScanException(new String("ERROR: Uneven amount of quotes in headLine"));
            }
            buffer.reset();
            while (buffer.hasRemaining()) {
                b = buffer.get();
                if (b == QUOTE) {
                    while (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == QUOTE) {
                            if (buffer.hasRemaining()) {
                                b = buffer.get();
                                if (b == SEPERATOR) {
                                    j++;
                                    break;
                                } else if (b == RETURN || b == NEWLINE)
                                    return j + 1;
                            }
                        }
                    }
                } else if (b == SEPERATOR) {
                    j++;
                } else if (b == RETURN || b == NEWLINE) {
                    return j + 1;
                } else {
                    while (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == SEPERATOR) {
                            j++;
                            break;
                        } else if (b == RETURN || b == NEWLINE) {
                            return j + 1;
                        }
                    }
                }
            }
        }
        return j + 1;
    }


    /**
     * @param buffer the associated ByteBuffer to read
     * @param format the format to handle with
     * @return amount of lines
     */
    int amountLines(ByteBuffer buffer, String format) throws ScanException, UnacceptableFormatException {
        buffer.rewind();
        int i = 0; // counts lines
        int j = 0; // counts columns
        // columns has to be initaliazed before
        numericalColumn = new boolean[columns];
        for (int k = 0; k < columns; k++) {
            numericalColumn[k] = true;
        }
        boolean dotAvailable = false;
        boolean minusAvailable = false;
        boolean expAvailable = false;
        boolean minusexpAvailable = false;
        byte b;
        byte SEPERATOR;

        buffer.rewind();
        buffer.position(positionSecondLine);

        if (format == "TAB-Format" || format == "KOMMA-Format") {
            if (format == "TAB-Format") SEPERATOR = TAB;
            else SEPERATOR = KOMMA;

            while (buffer.hasRemaining()) {

                if (numericalColumn[j]) {
                    dotAvailable = false;
                    minusAvailable = false;
                    expAvailable = false;
                    while (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == MINUS) {
                            buffer.position(buffer.position() - 2);
                            b = buffer.get();
                            if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                minusAvailable = true;
                                buffer.position(buffer.position() + 1);
                                continue;
                            } else if (b == 'e' || b == 'E') {
                                minusexpAvailable = true;
                                buffer.position(buffer.position() + 1);
                                continue;
                            } else {
                                buffer.position(buffer.position() + 1);
                                numericalColumn[j] = false;
                                // do not need the following lines, hopefully
//								while (buffer.hasRemaining()) {
//									b = buffer.get();
//									if (b == SEPERATOR) {
//										j++;
//										break;
//									} else if (b == RETURN) {
//										buffer.mark();
//										i++;
//										j = 0;
//										if (buffer.hasRemaining()) {
//											if (buffer.get() == NEWLINE)
//												break;
//											else {
//												buffer.reset();
//												break;
//											}
//										}
//									} else if (b == NEWLINE) {
//										i++;
//										j = 0;
//										break;
//									}
//								}
                                break;
                            }
                        } else if (isNumber(b)) {
                            continue;
                        } else if (b == DOT) {
                            minusAvailable = true;
                            if (expAvailable) {
                                numericalColumn[j] = false;
                                while (buffer.hasRemaining()) {
                                    b = buffer.get();
                                    if (b == SEPERATOR) {
                                        j++;
                                        break;
                                    } else if (b == RETURN) {
                                        buffer.mark();
                                        i++;
                                        j = 0;
                                        if (buffer.hasRemaining()) {
                                            if (buffer.get() == NEWLINE)
                                                break;
                                            else {
                                                buffer.reset();
                                                break;
                                            }
                                        }
                                    } else if (b == NEWLINE) {
                                        i++;
                                        j = 0;
                                        break;
                                    }
                                }
                                break;

                            }
                            if (dotAvailable) {
                                numericalColumn[j] = false;
                                while (buffer.hasRemaining()) {
                                    b = buffer.get();
                                    if (b == SEPERATOR) {
                                        j++;
                                        break;
                                    } else if (b == RETURN) {
                                        buffer.mark();
                                        i++;
                                        j = 0;
                                        if (buffer.hasRemaining()) {
                                            if (buffer.get() == NEWLINE)
                                                break;
                                            else {
                                                buffer.reset();
                                                break;
                                            }
                                        }
                                    } else if (b == NEWLINE) {
                                        i++;
                                        j = 0;
                                        break;
                                    }
                                }
                                break;
                            } else {
                                dotAvailable = true;
                                if (buffer.hasRemaining()) {
                                    buffer.mark();
                                    b = buffer.get();
                                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                        buffer.reset();
                                        numericalColumn[j] = false;
                                        break;
                                    } else {
                                        buffer.reset();
                                        continue;
                                    }
                                }
                            }
                        } else if (b == SEPERATOR) {
                            j++;
                            break;
                        } else if (b == RETURN) {
                            buffer.mark();
                            i++;
                            j = 0;
                            if (buffer.hasRemaining()) {
                                if (buffer.get() == NEWLINE)
                                    break;
                                else {
                                    buffer.reset();
                                    break;
                                }
                            }
                        } else if (b == NEWLINE) {
                            i++;
                            j = 0;
                            break;
                        } else {
                            if (b == (byte) 'N') {
                                if (buffer.position() > 1) {
                                    b = buffer.get(buffer.position() - 2);
                                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                    } else {
                                        numericalColumn[j] = false;
                                        break;
                                    }
                                }
                                if (buffer.hasRemaining()) {
                                    b = buffer.get();
                                    if (b == (byte) 'A') {
                                        if (buffer.hasRemaining()) {
                                            b = buffer.get();
                                            if (b == SEPERATOR) {
                                                j++;
                                                break;
                                            } else if (b == RETURN) {
                                                buffer.mark();
                                                i++;
                                                j = 0;
                                                if (buffer.hasRemaining()) {
                                                    if (buffer.get() == NEWLINE)
                                                        break;
                                                    else {
                                                        buffer.reset();
                                                        break;
                                                    }
                                                }
                                            } else if (b == NEWLINE) {
                                                i++;
                                                j = 0;
                                                break;
                                            } else {
                                                numericalColumn[j] = false;
                                                break;
                                            }

                                        } else
                                            break;
                                    } else if (b == (byte) 'a') {
                                        if (buffer.hasRemaining()) {
                                            b = buffer.get();
                                            if (b == (byte) 'N') {
                                                if (buffer.hasRemaining()) {
                                                    b = buffer.get();
                                                    if (b == SEPERATOR) {
                                                        j++;
                                                        break;
                                                    } else if (b == RETURN) {
                                                        buffer.mark();
                                                        i++;
                                                        j = 0;
                                                        if (buffer.hasRemaining()) {
                                                            if (buffer.get() == NEWLINE)
                                                                break;
                                                            else {
                                                                buffer.reset();
                                                                break;
                                                            }
                                                        }
                                                    } else if (b == NEWLINE) {
                                                        i++;
                                                        j = 0;
                                                        break;
                                                    } else {
                                                        numericalColumn[j] = false;
                                                        break;
                                                    }

                                                } else
                                                    break;
                                            }
                                        }
                                    } else {
                                        numericalColumn[j] = false;
                                        buffer.position(buffer.position() - 2);
                                        while (buffer.hasRemaining()) {
                                            b = buffer.get();
                                            if (b == SEPERATOR) {
                                                j++;
                                                break;
                                            } else if (b == RETURN) {
                                                buffer.mark();
                                                i++;
                                                j = 0;
                                                if (buffer.hasRemaining()) {
                                                    if (buffer.get() == NEWLINE) break;
                                                    else {
                                                        buffer.reset();
                                                        break;
                                                    }

                                                }
                                            } else if (b == NEWLINE) {
                                                i++;
                                                j = 0;
                                                break;
                                            }
                                        }

                                        break;
                                    }
                                } else {
                                    numericalColumn[j] = false;
                                    break;
                                }
                            } else if (b == (byte) 'e' || b == (byte) 'E') {
                                if (buffer.hasRemaining())
                                    if (isNumber(buffer.get(buffer.position())) || buffer.get(buffer.position()) == MINUS) {
                                        if (!expAvailable) {
                                            if (buffer.hasRemaining()) {
                                                b = buffer.get();
                                                if (!isNumber(b) && b != (byte) MINUS) {
                                                    numericalColumn[j] = false;
                                                    break;
                                                } else {
                                                    expAvailable = true;
                                                    continue;
                                                }
                                            } else {
                                                numericalColumn[j] = false;
                                                break;
                                            }
                                        } else {
                                            numericalColumn[j] = false;
                                            break;
                                        }
                                    } else {
                                        numericalColumn[j] = false;
                                        break;
                                    }
                            } else {
                                numericalColumn[j] = false;
                                break;
                            }
                        }

                    } // end while

                } else {
                    while (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == SEPERATOR) {
                            j++;
                            break;
                        } else if (b == RETURN) {
                            buffer.mark();
                            i++;
                            j = 0;
                            if (buffer.hasRemaining()) {
                                if (buffer.get() == NEWLINE)
                                    break;
                                else {
                                    buffer.reset();
                                    break;
                                }
                            }
                        } else if (b == NEWLINE) {
                            i++;
                            j = 0;
                            break;
                        }
                    }
                }
                if (j < columns) {
                } else {
                    System.out.println("Too long line in (i,j) = (" + (i + 2)
                            + "," + (j + 1) + ")");
                    throw new ScanException("Too long line in (i,j) = (" + (i + 2)
                            + "," + (j + 1) + ")");
                }

            } // end big while

        } else if (format == "UNKNOWN-Format") {

            System.out.println(format);
            throw new UnacceptableFormatException();
        }

        return i + 1;
    }


    /**
     * method to go to next line
     *
     * @param buffer associated ByteBuffer
     * @return this ByteBuffer with pointer on first byte in second line
     */
    private ByteBuffer gotoNextLine(ByteBuffer buffer) {
        byte b;
        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == RETURN) {
                buffer.mark();
                if (buffer.hasRemaining()) {
                    if (buffer.get() == NEWLINE) {
                        break;
                    } else {
                        buffer.reset();
                        break;
                    }
                } else {
                    break;
                }
            } else if (b == NEWLINE) {
                break;
            }
        }
        return buffer;
    }


    /**
     * tests unquoted ByteBuffer for errors
     *
     * @param buffer          associated ByteBuffer
     * @param SEPERATOR       associated seperator of words
     * @param maxamountErrors maximal errors amount. stop if more errors occur
     * @return String[] of errors (which errors occur)
     */
    public String[] testUNQUOTEDFormat(ByteBuffer buffer, byte SEPERATOR, int maxamountErrors) {

        buffer.rewind();
        byte b;
        String[] error = new String[maxamountErrors];
        int k = 0;
        int i = -1, j = 0;
        boolean doubleSEPERATOR = false;

        while (buffer.hasRemaining()) {

            b = buffer.get();
            j = 0;
            i++;
            doubleSEPERATOR = false;
            if (b == SEPERATOR) {
                // SEPERATOR at BOL
                if (k < error.length) {
                    if (i == 0) {
                        error[k++] = new String("hardError: SEPERATOR at BOL in headLine");
                        errorposition = buffer.position();
                        return error;
                    } else {
                        error[k++] = new String("softError: SEPERATOR at BOL in line " + (i + 1));
                        doubleSEPERATOR = true;
                    }
                } else
                    return error;
                // go to next line
//				buffer = gotoNextLine(buffer);
//				continue;
            } else if (b == RETURN) {
                // RETURN at BOL
                if (k < error.length) {
                    error[k++] = new String("hardError: RETURN at BOL in line " + (i + 1));
                    errorposition = buffer.position();
                    hardReadError = true;
                    return error;
                } else
                    return error;
            } else if (b == NEWLINE) {
                // NEWLINE at BOL
                if (k < error.length) {
                    error[k++] = new String("hardError: NEWLINE at BOL in line " + (i + 1));
                    errorposition = buffer.position();
                    hardReadError = true;
                    return error;
                } else
                    return error;
            }
            while (buffer.hasRemaining()) {
                b = buffer.get();
                if (b == SEPERATOR) {
                    j++;
                    // change: 17.08.2005
                    if (j >= columns) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: Too many entries in line "
                                    + (i + 1));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;

                        } else
                            return error;
                    }
                    buffer.mark();
                    if (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == SEPERATOR) {
                            // doubleSEPERATOR
                            if (k < error.length) {
                                if (i == 0) {
                                    error[k++] = new String("hardError: doubleSEPERATOR in headLine");
                                    errorposition = buffer.position();
                                    return error;
                                } else error[k++] = new String("softError: doubleSEPERATOR in line "
                                        + (i + 1));
                            } else
                                return error;
                            // go to next line
                            buffer = gotoNextLine(buffer);
                            break;
                        } else if (b == RETURN) {
                            // SEPERATOR at EOL
                            if (k < error.length) {
                                if (i == 0) {
                                    error[k++] = new String("hardError: SEPERATOR at EOL in headLine");
                                    errorposition = buffer.position();
                                    return error;
                                } else error[k++] = new String("softError: SEPERATOR at EOL in line "
                                        + (i + 1));
                            } else
                                return error;
                            // go to next line
                            buffer = gotoNextLine(buffer);
                            break;
                        } else if (b == NEWLINE) {
                            // SEPERATOR at EOL
                            if (k < error.length) {
                                if (i == 0) {
                                    error[k++] = new String("hardError: SEPERATOR at EOL in headLine");
                                    errorposition = buffer.position();
                                    return error;
                                } else error[k++] = new String("softError: SEPERATOR at EOL in line "
                                        + (i + 1));
                            } else
                                return error;
                            // go to next line
                            buffer = gotoNextLine(buffer);
                            break;
                        } else
                            buffer.reset();
                    } else {
                        // SEPERATOR at EOF
                        if (k < error.length) {
                            error[k++] = new String("softError: SEPERATOR at EOF");
                        } else
                            return error;
                    }
                } else if (b == RETURN) {
                    // Missing entries
                    if (j < columns - 1) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: Missing entries in line "
                                    + (i + 1));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    } else if (j >= columns) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: Too many entries in line "
                                    + (i + 1));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;

                        } else
                            return error;
                    }
                    buffer.mark();
                    if (buffer.hasRemaining()) {
                        if (buffer.get() == NEWLINE) {
                            break;
                        } else {
                            buffer.reset();
                            break;
                        }
                    }
                } else if (b == NEWLINE) {
                    // Missing entries
                    if (j < columns - 1) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: Missing entries in line "
                                    + (i + 1));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    } else if (j >= columns) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: Too many entries in line "
                                    + (i + 1));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    }
                    break;
                }
            }
        }
        b = buffer.get(buffer.position() - 1);

        if (doubleSEPERATOR) j++;
        if (j < columns - 1) {
            if (k < error.length) {
                error[k++] = new String("hardError: Missing entries in last line");
                errorposition = buffer.position();
                hardReadError = true;
                return error;
            } else
                return error;

        } else if (j >= columns) {
            if (k < error.length) {
                error[k++] = new String("hardError: Too many entries in last line");
                errorposition = buffer.position();
                hardReadError = true;
                return error;
            } else
                return error;
        }
        if (b == SEPERATOR && j < columns) {
            if (k < error.length) {
                error[k++] = new String("hardError: SEPERATOR at EOF / Missing values in last line");
                errorposition = buffer.position();
                hardReadError = true;
                return error;
            } else
                return error;
        } else if (b == NEWLINE) {
            if (k < error.length) {
                error[k++] = new String("softError: NEWLINE at EOF");
            } else
                return error;
        } else if (b == RETURN) {
            if (k < error.length) {
                error[k++] = new String("softError: RETURN at EOF");
            } else
                return error;

        }

        return error;
    }


    /**
     * tests quoted ByteBuffer for errors and gets amount of lines in quoted format
     *
     * @param buffer          associated ByteBuffer
     * @param SEPERATOR       associated seperator of words
     * @param maxamountErrors maximal errors amount. stop if more errors occur
     * @return String[] of errors (which errors occur)
     */
    public String[] testQUOTEDFormat(ByteBuffer buffer, byte SEPERATOR, int maxamountErrors) {

        for (int i = 0; i < columns; i++) {
            System.out.println(i + " " + numericalColumn[i]);
        }
        buffer.rewind();
        byte b;
        String[] error = new String[maxamountErrors];
        int k = 0;
        int i = 0, j = 0;
        boolean breaking = false;
        boolean dotAvailable = false;
        boolean expAvailable = false;
        boolean minusAvailable = false;
        boolean minusexpAvailable = false;

        // Suche Fehler in Kopfzeile

        while (buffer.hasRemaining()) {

            b = buffer.get();
            if (b == QUOTE) {
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == QUOTE) {
                        if (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == SEPERATOR) {
                                j++;
                                break;
                            } else if (b == RETURN || b == NEWLINE) {
                                breaking = true;
                                break;
                            } else {
                                if (k < error.length) {
                                    error[k++] = new String("hardError: error in headLine in j = " + (j + 1));
                                    errorposition = buffer.position();
                                    hardReadError = true;
                                    return error;
                                }
                                return error;

                            }
                        } else {
                            // nur eine Spalte im Head
                        }
                    }
                }
            } else if (b == SEPERATOR) {
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b != QUOTE) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: error in headLine in j = " + (j + 1));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        }
                        return error;
                    }
                } else {
                    // SEPERATOR at EOL
                }
            } else {
                if (k < error.length) {
                    error[k++] = new String("hardError: error in headLine in j = " + (j + 1) + " (word not quoted)");
                    errorposition = buffer.position();
                    hardReadError = true;
                    return error;
                }
                return error;
            }
            if (breaking) {
                break;
            }
        }

        j = 0;
        breaking = false;
        // lies erste Zeile ein, um Wort und Zahl zu erkennen. NA = Zahl
        buffer.rewind();
        buffer.position(positionSecondLine);

        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (isNumber(b) || b == MINUS || b == DOT) {
                numericalColumn[j] = true;
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        buffer.position(buffer.position() - 1);
                        break;
                    }
                }
            } else if (b == 'N') {
                numericalColumn[j] = true;
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        buffer.position(buffer.position() - 1);
                        break;
                    }
                }
            } else if (b == SEPERATOR) {
                j++;
                if (j >= columns) {
                    if (k < error.length) {
                        error[k++] = new String("hardError: Too many entries in line "
                                + (i + 1));
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
            } else if (b == RETURN || b == NEWLINE) {
                break;
            } else if (b == (byte) 'e' || b == (byte) 'E') {
                numericalColumn[j] = true;
                // EXPONENT, allowing no number before e/E. reading as 1E...
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        buffer.position(buffer.position() - 1);
                        break;
                    }
                }
            } else {
                numericalColumn[j] = false;
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        buffer.position(buffer.position() - 1);
                        break;
                    }
                }
            }
        }

        // finde Strukturfehler
        buffer.position(positionSecondLine);
        j = 0;
        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == QUOTE) {
                if (numericalColumn[j]) {
                    // Fehler
                    if (k < error.length) {
                        error[k++] = new String("hardError: QUOTE in numerical column (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == QUOTE) {
                        if (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                // alles in Ordnung
                                buffer.position(buffer.position() - 1);
                                break;
                            } else {
                                // Fehler: QUOTE im Wort
                                if (k < error.length) {
                                    error[k++] = new String("hardError: QUOTE in word (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                    errorposition = buffer.position();
                                    hardReadError = true;
                                    return error;
                                } else
                                    return error;
                            }
                        } else {
                            // alles in Ordnung
                        }
                    } else {
                        continue;
                    }
                }
            } else if (isNumber(b) || b == MINUS || b == DOT || b == (byte) 'e' || b == (byte) 'E') {
                if (!numericalColumn[j]) {
                    // Fehler
                    if (k < error.length) {
                        error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
                if (b == 'e' || b == 'E') expAvailable = true;
                else expAvailable = false;
                minusexpAvailable = false;
                if (b == MINUS) minusAvailable = true;
                else minusAvailable = false;
                if (b == DOT) dotAvailable = true;
                else dotAvailable = false;

                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (isNumber(b)) {
                        // alles in Ordnung
                    } else if (b == MINUS) {
                        buffer.position(buffer.position() - 2);
                        b = buffer.get();
                        if (b == SEPERATOR || b == NEWLINE || b == RETURN) {
                            minusAvailable = true;
                            buffer.position(buffer.position() + 1);
                        } else if (b == (byte) 'e' || b == (byte) 'E') {
                            // if no number before e/E -> problem was recognized earlier
                            minusexpAvailable = true;
                            buffer.position(buffer.position() + 1);
                        } else {
                            if (k < error.length) {
                                error[k++] = new String("hardError: not a number (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                errorposition = buffer.position();
                                hardReadError = true;
                                return error;
                            } else
                                return error;
                        }
                    } else if (b == DOT) {
                        if (buffer.hasRemaining()) {
                            buffer.mark();
                            b = buffer.get();
                            if (b == SEPERATOR || b == NEWLINE || b == RETURN) {
                                if (k < error.length) {
                                    error[k++] = new String("hardError: not a number (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                    errorposition = buffer.position();
                                    hardReadError = true;
                                    return error;
                                } else
                                    return error;
                            } else {
                                buffer.reset();
                            }
                        }
                        if (expAvailable) {
                            // Fehler: dot in exponent
                            if (k < error.length) {
                                error[k++] = new String("hardError:  dot in exponent (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                errorposition = buffer.position();
                                hardReadError = true;
                                return error;
                            } else
                                return error;
                        }
                        if (dotAvailable) {
                            // Fehler: >2 dots
                            if (k < error.length) {
                                error[k++] = new String("hardError: >=2 dots in number (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                errorposition = buffer.position();
                                hardReadError = true;
                                return error;
                            } else
                                return error;
                        } else {
                            dotAvailable = true;
                            // alles in Ordnung
                        }
                    } else if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        if (buffer.get(buffer.position() - 2) == 'e' || buffer.get(buffer.position() - 2) == 'E') // das exp-'e'/'E' steht ohne Exponenten
                            if (k < error.length) {
                                error[k++] = new String("hardError: no exponent found in (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                errorposition = buffer.position();
                                hardReadError = true;
                                return error;
                            } else
                                return error;

                        // alles in Ordnung
                        buffer.position(buffer.position() - 1);
                        break;
                    } else if (b == (byte) 'e' || b == (byte) 'E') {
                        if (!expAvailable) expAvailable = true;
                        else {
                            if (k < error.length) {
                                error[k++] = new String("hardError: not a number (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                errorposition = buffer.position();
                                hardReadError = true;
                                return error;
                            } else
                                return error;
                        }
                    } else {
                        // Fehler: in Zahl
                        if (k < error.length) {
                            error[k++] = new String("hardError: not a number (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    }
                }
            } else if (b == SEPERATOR) {
                j++;
                // change: 17.08.2005
                if (j >= columns) {
                    if (k < error.length) {
                        error[k++] = new String("hardError: Too many entries in line "
                                + (i + 1));
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;

                    } else
                        return error;
                }
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        buffer.position(buffer.position() - 1);
                        // doubleSEPERATOR
                    } else if (b == RETURN || b == NEWLINE) {
                        // SEPERATOR at EOL
                    } else {
                        buffer.position(buffer.position() - 1);
                    }
                } else {
                    // Fehler
                }
            } else if (b == RETURN) {
                if (j > columns - 1) {
                    if (k < error.length) {
                        error[k++] = new String("hardError: Too many entries in line " + (i + 2));
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                } else if (j < columns - 1) {
                    if (k < error.length) {
                        error[k++] = new String("hardError: Missing entries in line " + (i + 2));
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
                i++;
                j = 0;
                if (buffer.hasRemaining()) {
                    buffer.mark();
                    if (buffer.get() == NEWLINE) {
                    } else {
                        buffer.reset();
                    }
                }
                // leere Zeilen
                if (buffer.hasRemaining()) {
                    buffer.mark();
                    b = buffer.get();
                    if (b == RETURN || b == NEWLINE) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: empty line i = " + (i + 2));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    } else {
                        buffer.reset();
                    }
                }
            } else if (b == NEWLINE) {
                if (j > columns - 1) {
                    if (k < error.length) {
                        error[k++] = new String("hardError: Too many entries in line " + (i + 2));
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                } else if (j < columns - 1) {
                    if (k < error.length) {
                        error[k++] = new String("hardError: Missing entries in line " + (i + 2));
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
                i++;
                j = 0;
                if (buffer.hasRemaining()) {
                    buffer.mark();
                    b = buffer.get();
                    if (b == RETURN || b == NEWLINE) {
                        if (k < error.length) {
                            error[k++] = new String("hardError: empty line i = " + (i + 2));
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    } else {
                        buffer.reset();
                    }
                }

            } else if (b == 'N') {
                if (!numericalColumn[j]) {
                    // Fehler
                    if (k < error.length) {
                        error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == 'A') {
                        if (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                // alles in Orndung
                                buffer.position(buffer.position() - 1);
                            } else {
                                // Fehler
                                if (k < error.length) {
                                    error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                    errorposition = buffer.position();
                                    hardReadError = true;
                                    return error;
                                } else
                                    return error;
                            }
                        } else {
                            // alles in Ordnung
                        }
                    } else if (b == 'a') {
                        if (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == 'N') {
                                b = buffer.get();
                                if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                    // alles in Ordnung
                                    buffer.position(buffer.position() - 1);
                                } else {
                                    // Fehler
                                    if (k < error.length) {
                                        error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                        errorposition = buffer.position();
                                        hardReadError = true;
                                        return error;
                                    } else
                                        return error;
                                }
                            } else {
                                // Fehler
                                if (k < error.length) {
                                    error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                    errorposition = buffer.position();
                                    hardReadError = true;
                                    return error;
                                } else
                                    return error;
                            }
                        } else {
                            // Fehler
                            if (k < error.length) {
                                error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                                errorposition = buffer.position();
                                hardReadError = true;
                                return error;
                            } else
                                return error;
                        }
                    } else {
                        // Fehler
                        if (k < error.length) {
                            error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                            errorposition = buffer.position();
                            hardReadError = true;
                            return error;
                        } else
                            return error;
                    }
                } else {
                    // Fehler
                    if (k < error.length) {
                        error[k++] = new String("hardError: word not quoted (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                        errorposition = buffer.position();
                        hardReadError = true;
                        return error;
                    } else
                        return error;
                }
            } else {
                // Fehler
                if (k < error.length) {
                    error[k++] = new String("hardError: error in (i,j) = (" + (i + 1) + "," + (j + 1) + ")");
                    errorposition = buffer.position();
                    hardReadError = true;
                    return error;
                } else
                    return error;
            }
        }

        lines = i + 1;
        return error;
    }


    /**
     * tokenizes errorfree ByteBuffer into tokens (unquoted format) saved in item
     *
     * @param buffer    associated ByteBuffer
     * @param SEPERATOR associated seperator of words
     */
    public void tokenizeUNQUOTEDBuffer(ByteBuffer buffer, byte SEPERATOR) {
        byte b;
        int i, j, k; // i = Zeilenindex, j = Spaltenindex, k = zählt Wortlänge
        int[] discretLimit_c = new int[columns]; // specified discretLimit for column
        for (int l = 0; l < wordStackSize.length; l++) {
            wordStackSize[l] = 0;
            discretLimit_c[l] = discretLimit;
        }
        boolean wordFound = false;
        boolean valueFound = false;
        byte[] temp = null;
        double tempNumber;
        int[] discretLimitCounter = new int[columns];
        int findWordIndex = 0;
        int findValueIndex = 0;
        byte[][] previousWord = new byte[columns][];
        int[] previousWordPosition = new int[columns];
        byte[][][] s_word = new byte[columns][discretLimit][];
        int[][] pointer = new int[columns][discretLimit];

        buffer.position(positionSecondLine);
        i = 0;
        j = 0;
        k = 0;

        while (buffer.hasRemaining()) {
            k = 0;

            if (numericalColumn[j] == false) {

                if (buffer.hasRemaining()) {
                    buffer.mark();
                    b = buffer.get();

                    // doubleSEPERATOR-Behandlung als NA
                    if (b == SEPERATOR) {
                        temp = new byte[2];
                        temp[0] = 'N';
                        temp[1] = 'A';
                        NA[j][i] = true;
                        if (wordStackSize[j] != 0) {
                            if (eqCharArray(previousWord[j], temp)) {
                                findWordIndex = previousWordPosition[j];
                                wordNotFound = false;
                            } else {
                                findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                previousWord[j] = temp;
                                previousWordPosition[j] = findWordIndex;
                            }
                            findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                            if (wordNotFound) {
                                wordNotFound = false;
                                if (wordStackSize[j] >= discretLimit_c[j]) {
                                    byte[][] tempArray = word[j];
                                    byte[][] s_tempArray = s_word[j];
                                    int[] tempPointer = pointer[j];
                                    discretLimit_c[j] *= 2;
                                    word[j] = new byte[2 * wordStackSize[j]][];
                                    s_word[j] = new byte[2 * wordStackSize[j]][];
                                    pointer[j] = new int[2 * wordStackSize[j]];
                                    System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                                    System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                                    System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                                }
                                int z = 0;
                                System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                s_word[j][findWordIndex] = temp;
                                pointer[j][findWordIndex] = wordStackSize[j];
                                word[j][wordStackSize[j]] = temp;
                                item[j][i] = wordStackSize[j];
                                wordStackSize[j]++;
                                wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                            } else {
                                item[j][i] = pointer[j][findWordIndex];
                                wordCount[j][pointer[j][findWordIndex]]++;
                                wordNotFound = true;
                            }
                        } else {
                            previousWord[j] = temp;
                            previousWordPosition[j] = 0;
                            s_word[j][0] = temp;
                            pointer[j][0] = 0;
                            word[j][0] = temp;
                            item[j][i] = 0;
                            wordCount[j][0]++;
                            wordStackSize[j]++;
                        }
                        j++;
                        continue;


                    } else if (b == RETURN) { // hier kommt er nie rein
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            temp = new byte[2];
                            temp[0] = 'N';
                            temp[1] = 'A';
                            NA[j][i] = true;
                            if (wordStackSize[j] != 0) {
                                if (eqCharArray(previousWord[j], temp)) {
                                    findWordIndex = previousWordPosition[j];
                                    wordNotFound = false;
                                } else {
                                    findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                    previousWord[j] = temp;
                                    previousWordPosition[j] = findWordIndex;
                                }
                                findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                if (wordNotFound) {
                                    wordNotFound = false;
                                    if (wordStackSize[j] >= discretLimit_c[j]) {
                                        byte[][] tempArray = word[j];
                                        byte[][] s_tempArray = s_word[j];
                                        int[] tempPointer = pointer[j];
                                        discretLimit_c[j] *= 2;
                                        word[j] = new byte[2 * wordStackSize[j]][];
                                        s_word[j] = new byte[2 * wordStackSize[j]][];
                                        pointer[j] = new int[2 * wordStackSize[j]];
                                        System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                                        System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                                        System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                                    }
                                    int z = 0;
                                    System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    s_word[j][findWordIndex] = temp;
                                    pointer[j][findWordIndex] = wordStackSize[j];
                                    word[j][wordStackSize[j]] = temp;
                                    item[j][i] = wordStackSize[j];
                                    wordStackSize[j]++;
                                    wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                                } else {
                                    item[j][i] = pointer[j][findWordIndex];
                                    wordCount[j][pointer[j][findWordIndex]]++;
                                    wordNotFound = true;
                                }
                            } else {
                                previousWord[j] = temp;
                                previousWordPosition[j] = 0;
                                s_word[j][0] = temp;
                                pointer[j][0] = 0;
                                word[j][0] = temp;
                                item[j][i] = 0;
                                wordCount[j][0]++;
                                wordStackSize[j]++;
                            }
                        }
                        i++;
                        j = 0;
                        if (buffer.hasRemaining()) {
                            if (buffer.get(buffer.position()) == NEWLINE) {
                                buffer.position(buffer.position() + 1);
                            }

                        }
                        continue;
                    } else if (b == NEWLINE) { // hier auch nicht
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            temp = new byte[2];
                            temp[0] = 'N';
                            temp[1] = 'A';
                            NA[j][i] = true;
                            if (wordStackSize[j] != 0) {
                                if (eqCharArray(previousWord[j], temp)) {
                                    findWordIndex = previousWordPosition[j];
                                    wordNotFound = false;
                                } else {
                                    findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                    previousWord[j] = temp;
                                    previousWordPosition[j] = findWordIndex;
                                }
                                findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                if (wordNotFound) {
                                    wordNotFound = false;
                                    if (wordStackSize[j] >= discretLimit_c[j]) {
                                        byte[][] tempArray = word[j];
                                        byte[][] s_tempArray = s_word[j];
                                        int[] tempPointer = pointer[j];
                                        discretLimit_c[j] *= 2;
                                        word[j] = new byte[2 * wordStackSize[j]][];
                                        s_word[j] = new byte[2 * wordStackSize[j]][];
                                        pointer[j] = new int[2 * wordStackSize[j]];
                                        System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                                        System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                                        System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                                    }
                                    int z = 0;
                                    System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    s_word[j][findWordIndex] = temp;
                                    pointer[j][findWordIndex] = wordStackSize[j];
                                    word[j][wordStackSize[j]] = temp;
                                    item[j][i] = wordStackSize[j];
                                    wordStackSize[j]++;
                                    wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                                } else {
                                    item[j][i] = pointer[j][findWordIndex];
                                    wordCount[j][pointer[j][findWordIndex]]++;
                                    wordNotFound = true;
                                }
                            } else {
                                previousWord[j] = temp;
                                previousWordPosition[j] = 0;
                                s_word[j][0] = temp;
                                pointer[j][0] = 0;
                                word[j][0] = temp;
                                item[j][i] = 0;
                                wordCount[j][0]++;
                                wordStackSize[j]++;
                            }
                        }
                        i++;
                        j = 0;
                        continue;
                    } else {
                        buffer.reset();
                    }
                }

                // lies Wortlänge ein
                buffer.mark();
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        buffer.reset();
                        break;
                    } else
                        k++;
                }
                if (!buffer.hasRemaining())
                    buffer.reset();
                temp = new byte[k]; // geht schnell
                for (int l = 0; l < k; l++) {
                    b = buffer.get();
                    temp[l] = b;
                }
                temp = cutSpacesInItem(temp);

                // System.out.println("temp " + new String(temp));

                // modified

                if (wordStackSize[j] != 0) {
                    //System.out.println("word.length = " + word[j].length);
                    if (eqCharArray(previousWord[j], temp)) {
                        // System.out.println("previous " + new String(previousWord));
                        // System.out.println("temp2 " + new String(temp));
                        findWordIndex = previousWordPosition[j];
                        // System.out.println("findWordIndex2 " + findWordIndex);
                        wordNotFound = false;
                    } else {
                        findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                        previousWord[j] = temp;
                        previousWordPosition[j] = findWordIndex;
                    }


                    //System.out.println("wordStackSize " + wordStackSize[j]);
                    // System.out.println("findWordIndex " + findWordIndex);
                    if (wordNotFound) {
                        wordNotFound = false;
                        // wordFound = true;

                        if (wordStackSize[j] >= discretLimit_c[j]) {
                            byte[][] tempArray = word[j];
                            byte[][] s_tempArray = s_word[j];
                            int[] tempPointer = pointer[j];
                            discretLimit_c[j] *= 2;
                            word[j] = new byte[2 * wordStackSize[j]][];
                            s_word[j] = new byte[2 * wordStackSize[j]][];
                            pointer[j] = new int[2 * wordStackSize[j]];
                            System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                            System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                            System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                        }
                        int z = 0;
                        /*System.out.println("/// start before");
                              while(z < item[j].length) {
                                  System.out.print(z); System.out.print("	");
                                  // System.out.print(word[j][z]); System.out.print("	");
                                  System.out.print(item[j][z]); System.out.print("	");
                                  System.out.println(wordCount[j][z]);
                                  z++;
                              }
                              System.out.println("/// end before");*/
                        System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                        System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                        /* for(int m=0; m<i; m++) {
                                  if(item[j][m] >= findWordIndex) {
                                      item[j][m]++;
                                  }
                              } */
                        s_word[j][findWordIndex] = temp;
                        pointer[j][findWordIndex] = wordStackSize[j];
                        word[j][wordStackSize[j]] = temp;
                        item[j][i] = wordStackSize[j];
                        wordStackSize[j]++;
                        wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                        /*System.out.println("/// start after");
                              z = 0;
                              while(z < item[j].length) {
                                  System.out.print(z); System.out.print("	");
                                  // System.out.print(word[j][z]); System.out.print("	");
                                  System.out.print(item[j][z]); System.out.print("	");
                                  System.out.println(wordCount[j][z]);
                                  z++;
                              }
                              System.out.println("/// end after");*/

                    } else {
                        //System.out.println("bbb " + i);
                        item[j][i] = pointer[j][findWordIndex];
                        wordCount[j][pointer[j][findWordIndex]]++;
                        wordNotFound = true;
                    }
                } else {
                    previousWord[j] = temp;
                    previousWordPosition[j] = 0;
                    s_word[j][0] = temp;
                    pointer[j][0] = 0;
                    word[j][0] = temp;
                    item[j][i] = 0;
                    wordCount[j][0]++;
                    wordStackSize[j]++;
                }

                // end modified


                if (temp.length == 2 && temp[0] == 'N' && temp[1] == 'A') NA[j][i] = true;
                else if (temp.length == 3 && temp[0] == 'N' && temp[1] == 'a' && temp[2] == 'N') NA[j][i] = true;

                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        j++;
                    } else if (b == RETURN) {
                        progressing(i); // execute progressing() every new line
                        buffer.mark();
                        if (buffer.hasRemaining()) {
                            if (buffer.get() == NEWLINE) {
                                i++;
                                j = 0;
                            } else {
                                buffer.reset();
                                i++;
                                j = 0;
                            }
                        }
                    } else if (b == NEWLINE) {
                        progressing(i); // execute progressing() every new line
                        i++;
                        j = 0;
                    }
                }

                // numericalColumn[j] = true
            } else {
                if (buffer.hasRemaining()) {
                    buffer.mark();
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        NA[j][i] = true;
                        item[j][i] = Double.MAX_VALUE;
//						if(!NA[j][i])NACount[j]++;
//						j++;
                        buffer.reset();
//						continue;
                    } else if (b == RETURN) {
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            NA[j][i] = true;
                            item[j][i] = Double.MAX_VALUE;
//							if(!NA[j][i])NACount[j]++;
                        }
                        buffer.position(buffer.position() - 1);


                    } else if (b == NEWLINE) {
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            NA[j][i] = true;
                            item[j][i] = Double.MAX_VALUE;
//							if(!NA[j][i])NACount[j]++;
                        }
                        buffer.position(buffer.position() - 1);

                    } else {
                        buffer.reset();
                    }
                }
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == (byte) 'N') {
                        item[j][i] = Double.MAX_VALUE;
//						if(!NA[j][i])NACount[j]++;
                        if (buffer.get() == 'A') {
                            NA[j][i] = true;
//							break;
                        } // NA
                        else if (buffer.get() == 'N') {
                            NA[j][i] = true;
//							break;
                        } // NaN
                    } else if (b == MINUS) {
                        while (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == DOT) {
                                while (buffer.hasRemaining()) {
                                    b = buffer.get();
                                    if (b == SEPERATOR || b == NEWLINE || b == RETURN) {
//										item[j][i] = item[j][i] * Math.pow(10,-k);
                                        item[j][i] = item[j][i] / potence[k];
                                        break;
                                    } else if (!buffer.hasRemaining()) {
                                        item[j][i] = (item[j][i] * 10 + (b - 48)) / potence[k + 1];
                                        break;
                                    } else if (b == (byte) 'e' || b == (byte) 'E') {
                                        item[j][i] = item[j][i] / potence[k];
                                        // attention: returns infinity in not-acceptable exponents
                                        int exp = getExponent(buffer);
                                        if (exp >= 0) {
                                            if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                                            else item[j][i] = item[j][i] * 10 * 1e308;
                                        } else {
                                            if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                                            else item[j][i] = item[j][i] / 10 * 1e-323;
                                        }
                                        if (buffer.hasRemaining()) buffer.position(buffer.position() + 1);
                                        break;
                                    } else {
//										item[j][i] += (b - 48) * Math.pow(10, -(++k));
                                        item[j][i] = item[j][i] * 10 + (b - 48);
                                        k++;
                                    }
                                }
                                break;
                            } else if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                break;
                            } else if (b == (byte) 'e' || b == (byte) 'E') {
                                // attention: returns infinity in not-acceptable exponents
                                int exp = getExponent(buffer);
                                if (exp >= 0) {
                                    if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                                    else item[j][i] = item[j][i] * 10 * 1e308;
                                } else {
                                    if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                                    else item[j][i] = item[j][i] / 10 * 1e-323;
                                }
                                if (buffer.hasRemaining()) buffer.position(buffer.position() + 1);
                                break;
                            } else
                                item[j][i] = item[j][i] * 10 + (b - 48);
                        }
                        item[j][i] = -item[j][i];
                        break;
                    } else if (b == DOT) {
                        while (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == SEPERATOR || b == NEWLINE || b == RETURN) {
//								item[j][i] = item[j][i] * Math.pow(10,-k);
                                item[j][i] = item[j][i] / potence[k];
                                break;
                            } else if (!buffer.hasRemaining()) {
                                item[j][i] = (item[j][i] * 10 + (b - 48)) / potence[k + 1];
                                break;
                            } else if (b == (byte) 'e' || b == (byte) 'E') {
                                item[j][i] = item[j][i] / potence[k];
                                // attention: returns infinity in not-acceptable exponents
                                int exp = getExponent(buffer);
                                if (exp >= 0) {
                                    if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                                    else item[j][i] = item[j][i] * 10 * 1e308;
                                } else {
                                    if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                                    else item[j][i] = item[j][i] / 10 * 1e-323;
                                }
                                // TODO: is the following a good solution?
                                // here's the problem with positioning.
//								System.out.println(item[j][i]);
                                if (buffer.hasRemaining()) buffer.position(buffer.position() + 1);
                                break;
                            } else {
//                              item[j][i] += (b - 48) * Math.pow(10, -(++k));
                                item[j][i] = item[j][i] * 10 + (b - 48);
//								System.out.println(item[j][i]);
                                k++;
                            }
                        }
                        // XXX: TESTE OB DIESES BREAK HIER NICHT DOCH STEHEN MUSS
                        break;
                    } else if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        break;
                    } else if (b == (byte) 'e' || b == (byte) 'E') {
                        // attention: returns infinity in not-acceptable exponents
                        int exp = getExponent(buffer);
                        if (exp >= 0) {
                            if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                            else item[j][i] = item[j][i] * 10 * 1e308;
                        } else {
                            if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                            else item[j][i] = item[j][i] / 10 * 1e-323;
                        }
                    } else {
                        item[j][i] = item[j][i] * 10 + (b - 48);
//						System.out.println(item[j][i]);
                    }
                } // end while
//				System.out.println("item["+j+"]["+i+"]: " + item[j][i]);
                // modified
                if (isDiscret[j]) {
                    for (int m = 0; m < wordStackSize[j]; m++) {
                        if (discretValue[j][m] == item[j][i]) {
                            valueFound = true;
                            findValueIndex = m;
                            break;
                        }
                    }
                    if (!valueFound) {
                        wordCount[j][wordStackSize[j]] = 1; // new item inserted
                        discretValue[j][wordStackSize[j]] = item[j][i];
                        wordStackSize[j]++;
                        if (wordStackSize[j] >= discretLimit_c[j]) {
                            isDiscret[j] = false;
                        }
                    } else {
                        wordCount[j][findValueIndex]++;
                        valueFound = false;
                    }
                }

                // end modified
                buffer.position(buffer.position() - 1);
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR)
                        j++;
                    else if (b == RETURN) {
                        progressing(i); // execute progressing() every new line
                        buffer.mark();
                        if (buffer.hasRemaining()) {
                            if (buffer.get() == NEWLINE) {
                                i++;
                                j = 0;
                            } else {
                                buffer.reset();
                                i++;
                                j = 0;
                            }
                        }
                    } else if (b == NEWLINE) {
                        progressing(i); // execute progressing() every new line
                        i++;
                        j = 0;
                    }
                }
            }

        }
    }


    /**
     * tokenizes errorfree ByteBuffer into tokens (quoted format) saved in item
     *
     * @param buffer    associated ByteBuffer
     * @param SEPERATOR associated seperator of words
     */

    public void tokenizeQUOTEDBuffer(ByteBuffer buffer, byte SEPERATOR) {

        byte b;
        int i, j, k; // i = Zeilenindex, j = Spaltenindex, k = zählt Wortlänge
        int[] discretLimit_c = new int[columns];// letzter Eintrag in der Zeile
        for (int l = 0; l < wordStackSize.length; l++) {
            wordStackSize[l] = 0;
            discretLimit_c[l] = discretLimit;
        }
        boolean wordFound = false;
        boolean valueFound = false;
        byte[] temp = null;
        double tempNumber;
        int[] discretLimitCounter = new int[columns];
        int findWordIndex = 0;
        int findValueIndex = 0;
        byte[][] previousWord = new byte[columns][];
        int[] previousWordPosition = new int[columns];
        byte[][][] s_word = new byte[columns][discretLimit][];
        int[][] pointer = new int[columns][discretLimit];


        buffer.position(positionSecondLine);
        i = 0;
        j = 0;
        k = 0;

        while (buffer.hasRemaining()) {
            k = 0;

            if (numericalColumn[j] == false) {

                if (buffer.hasRemaining()) {
                    buffer.mark();
                    b = buffer.get();

                    // doubleSEPERATOR-Behandlung als NA
                    if (b == SEPERATOR) {
                        temp = new byte[2];
                        temp[0] = 'N';
                        temp[1] = 'A';
                        NA[j][i] = true;
                        if (wordStackSize[j] != 0) {
                            if (eqCharArray(previousWord[j], temp)) {
                                findWordIndex = previousWordPosition[j];
                                wordNotFound = false;
                            } else {
                                findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                previousWord[j] = temp;
                                previousWordPosition[j] = findWordIndex;
                            }
                            findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                            if (wordNotFound) {
                                wordNotFound = false;
                                if (wordStackSize[j] >= discretLimit_c[j]) {
                                    byte[][] tempArray = word[j];
                                    byte[][] s_tempArray = s_word[j];
                                    int[] tempPointer = pointer[j];
                                    discretLimit_c[j] *= 2;
                                    word[j] = new byte[2 * wordStackSize[j]][];
                                    s_word[j] = new byte[2 * wordStackSize[j]][];
                                    pointer[j] = new int[2 * wordStackSize[j]];
                                    System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                                    System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                                    System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                                }
                                int z = 0;
                                System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                s_word[j][findWordIndex] = temp;
                                pointer[j][findWordIndex] = wordStackSize[j];
                                word[j][wordStackSize[j]] = temp;
                                item[j][i] = wordStackSize[j];
                                wordStackSize[j]++;
                                wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                            } else {
                                item[j][i] = pointer[j][findWordIndex];
                                wordCount[j][pointer[j][findWordIndex]]++;
                                wordNotFound = true;
                            }
                        } else {
                            previousWord[j] = temp;
                            previousWordPosition[j] = 0;
                            s_word[j][0] = temp;
                            pointer[j][0] = 0;
                            word[j][0] = temp;
                            item[j][i] = 0;
                            wordCount[j][0]++;
                            wordStackSize[j]++;
                        }
                        j++;
                        continue;


                    } else if (b == RETURN) { // hier kommt er nie rein
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            temp = new byte[2];
                            temp[0] = 'N';
                            temp[1] = 'A';
                            NA[j][i] = true;
                            if (wordStackSize[j] != 0) {
                                if (eqCharArray(previousWord[j], temp)) {
                                    findWordIndex = previousWordPosition[j];
                                    wordNotFound = false;
                                } else {
                                    findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                    previousWord[j] = temp;
                                    previousWordPosition[j] = findWordIndex;
                                }
                                findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                if (wordNotFound) {
                                    wordNotFound = false;
                                    if (wordStackSize[j] >= discretLimit_c[j]) {
                                        byte[][] tempArray = word[j];
                                        byte[][] s_tempArray = s_word[j];
                                        int[] tempPointer = pointer[j];
                                        discretLimit_c[j] *= 2;
                                        word[j] = new byte[2 * wordStackSize[j]][];
                                        s_word[j] = new byte[2 * wordStackSize[j]][];
                                        pointer[j] = new int[2 * wordStackSize[j]];
                                        System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                                        System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                                        System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                                    }
                                    int z = 0;
                                    System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    s_word[j][findWordIndex] = temp;
                                    pointer[j][findWordIndex] = wordStackSize[j];
                                    word[j][wordStackSize[j]] = temp;
                                    item[j][i] = wordStackSize[j];
                                    wordStackSize[j]++;
                                    wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                                } else {
                                    item[j][i] = pointer[j][findWordIndex];
                                    wordCount[j][pointer[j][findWordIndex]]++;
                                    wordNotFound = true;
                                }
                            } else {
                                previousWord[j] = temp;
                                previousWordPosition[j] = 0;
                                s_word[j][0] = temp;
                                pointer[j][0] = 0;
                                word[j][0] = temp;
                                item[j][i] = 0;
                                wordCount[j][0]++;
                                wordStackSize[j]++;
                            }
                        }
                        i++;
                        j = 0;
                        if (buffer.hasRemaining()) {
                            if (buffer.get(buffer.position()) == NEWLINE) {
                                buffer.position(buffer.position() + 1);
                            }

                        }
                        continue;
                    } else if (b == NEWLINE) { // hier auch nicht
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            temp = new byte[2];
                            temp[0] = 'N';
                            temp[1] = 'A';
                            NA[j][i] = true;
                            if (wordStackSize[j] != 0) {
                                if (eqCharArray(previousWord[j], temp)) {
                                    findWordIndex = previousWordPosition[j];
                                    wordNotFound = false;
                                } else {
                                    findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                    previousWord[j] = temp;
                                    previousWordPosition[j] = findWordIndex;
                                }
                                findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                                if (wordNotFound) {
                                    wordNotFound = false;
                                    if (wordStackSize[j] >= discretLimit_c[j]) {
                                        byte[][] tempArray = word[j];
                                        byte[][] s_tempArray = s_word[j];
                                        int[] tempPointer = pointer[j];
                                        discretLimit_c[j] *= 2;
                                        word[j] = new byte[2 * wordStackSize[j]][];
                                        s_word[j] = new byte[2 * wordStackSize[j]][];
                                        pointer[j] = new int[2 * wordStackSize[j]];
                                        System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                                        System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                                        System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                                    }
                                    int z = 0;
                                    System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                                    s_word[j][findWordIndex] = temp;
                                    pointer[j][findWordIndex] = wordStackSize[j];
                                    word[j][wordStackSize[j]] = temp;
                                    item[j][i] = wordStackSize[j];
                                    wordStackSize[j]++;
                                    wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                                } else {
                                    item[j][i] = pointer[j][findWordIndex];
                                    wordCount[j][pointer[j][findWordIndex]]++;
                                    wordNotFound = true;
                                }
                            } else {
                                previousWord[j] = temp;
                                previousWordPosition[j] = 0;
                                s_word[j][0] = temp;
                                pointer[j][0] = 0;
                                word[j][0] = temp;
                                item[j][i] = 0;
                                wordCount[j][0]++;
                                wordStackSize[j]++;
                            }
                        }
                        i++;
                        j = 0;
                        continue;
                    } else {
                        buffer.reset();
                    }
                }

                // lies Wortlänge ein
                if (numericalColumn[j] == false) {
                    buffer.position(buffer.position() + 1);
                    buffer.mark();
                    while (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == QUOTE) {
                            buffer.reset();
                            break;
                        } else k++;
                    }
                }
                if (!buffer.hasRemaining())
                    buffer.reset();
                temp = new byte[k]; // geht schnell
                for (int l = 0; l < k; l++) {
                    b = buffer.get();

                    temp[l] = b;
                }
                temp = cutSpacesInItem(temp);

                // modified

                if (wordStackSize[j] != 0) {
                    //System.out.println("word.length = " + word[j].length);
                    if (eqCharArray(previousWord[j], temp)) {
                        // System.out.println("previous " + new String(previousWord));
                        // System.out.println("temp2 " + new String(temp));
                        findWordIndex = previousWordPosition[j];
                        // System.out.println("findWordIndex2 " + findWordIndex);
                        wordNotFound = false;
                    } else {
                        findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                        previousWord[j] = temp;
                        previousWordPosition[j] = findWordIndex;
                    }

                    findWordIndex = findWord(s_word[j], wordStackSize[j], temp);
                    //System.out.println("wordStackSize " + wordStackSize[j]);
                    // System.out.println("findWordIndex " + findWordIndex);
                    if (wordNotFound) {
                        wordNotFound = false;
                        // wordFound = true;

                        if (wordStackSize[j] >= discretLimit_c[j]) {
                            byte[][] tempArray = word[j];
                            byte[][] s_tempArray = s_word[j];
                            int[] tempPointer = pointer[j];
                            discretLimit_c[j] *= 2;
                            word[j] = new byte[2 * wordStackSize[j]][];
                            s_word[j] = new byte[2 * wordStackSize[j]][];
                            pointer[j] = new int[2 * wordStackSize[j]];
                            System.arraycopy(tempArray, 0, word[j], 0, wordStackSize[j]);
                            System.arraycopy(s_tempArray, 0, s_word[j], 0, wordStackSize[j]);
                            System.arraycopy(tempPointer, 0, pointer[j], 0, wordStackSize[j]);
                        }
                        int z = 0;
                        /*System.out.println("/// start before");
                              while(z < item[j].length) {
                                  System.out.print(z); System.out.print("	");
                                  // System.out.print(word[j][z]); System.out.print("	");
                                  System.out.print(item[j][z]); System.out.print("	");
                                  System.out.println(wordCount[j][z]);
                                  z++;
                              }
                              System.out.println("/// end before");*/
                        System.arraycopy(s_word[j], findWordIndex, s_word[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                        System.arraycopy(pointer[j], findWordIndex, pointer[j], findWordIndex + 1, wordStackSize[j] - findWordIndex);
                        /* for(int m=0; m<i; m++) {
                                  if(item[j][m] >= findWordIndex) {
                                      item[j][m]++;
                                  }
                              } */
                        s_word[j][findWordIndex] = temp;
                        pointer[j][findWordIndex] = wordStackSize[j];
                        word[j][wordStackSize[j]] = temp;
                        item[j][i] = wordStackSize[j];
                        wordStackSize[j]++;
                        wordCount[j][pointer[j][findWordIndex]] = 1; // new word saved
                        /*System.out.println("/// start after");
                              z = 0;
                              while(z < item[j].length) {
                                  System.out.print(z); System.out.print("	");
                                  // System.out.print(word[j][z]); System.out.print("	");
                                  System.out.print(item[j][z]); System.out.print("	");
                                  System.out.println(wordCount[j][z]);
                                  z++;
                              }
                              System.out.println("/// end after");*/

                    } else {
                        //System.out.println("bbb " + i);
                        item[j][i] = pointer[j][findWordIndex];
                        wordCount[j][pointer[j][findWordIndex]]++;
                        wordNotFound = true;
                    }
                } else {
                    previousWord[j] = temp;
                    previousWordPosition[j] = 0;
                    s_word[j][0] = temp;
                    pointer[j][0] = 0;
                    word[j][0] = temp;
                    item[j][i] = 0;
                    wordCount[j][0]++;
                    wordStackSize[j]++;
                }

                // end modified

                if (temp.length == 2 && temp[0] == 'N' && temp[1] == 'A') NA[j][i] = true;
                else if (temp.length == 3 && temp[0] == 'N' && temp[1] == 'a' && temp[2] == 'N') NA[j][i] = true;

                if (!numericalColumn[j]) {
                    buffer.position(buffer.position() + 1); // QUOTE wird eingelesen
                }
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        j++;
                    } else if (b == RETURN) {
                        progressing(i); // execute progressing() every new line
                        buffer.mark();
                        if (buffer.hasRemaining()) {
                            if (buffer.get() == NEWLINE) {
                                i++;
                                j = 0;
                            } else {
                                buffer.reset();
                                i++;
                                j = 0;
                            }
                        }
                    } else if (b == NEWLINE) {
                        progressing(i); // execute progressing() every new line
                        i++;
                        j = 0;
                    }
                }

                // numericalColumn[j] = true
            } else {

                if (buffer.hasRemaining()) {
                    buffer.mark();
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        NA[j][i] = true;
                        item[j][i] = Double.MAX_VALUE;
//						if(!NA[j][i])NACount[j]++;
                        j++;
                        continue;
                    } else if (b == RETURN) {
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            NA[j][i] = true;
                            item[j][i] = Double.MAX_VALUE;
//							if(!NA[j][i])NACount[j]++;
                        }
                        buffer.position(buffer.position() - 1);


                    } else if (b == NEWLINE) {
                        if (buffer.get(buffer.position() - 2) == SEPERATOR) {
                            NA[j][i] = true;
                            item[j][i] = Double.MAX_VALUE;
//							if(!NA[j][i])NACount[j]++;
                        }
                        buffer.position(buffer.position() - 1);

                    } else {
                        buffer.reset();
                    }
                }
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == (byte) 'N') {
                        item[j][i] = Double.MAX_VALUE;
//						if(!NA[j][i])NACount[j]++;
                        if (buffer.get() == 'A') {
                            NA[j][i] = true;
                            break;
                        } // NA
                        else if (buffer.get() == 'N') {
                            NA[j][i] = true;
                            break;
                        } // NaN
                    } else if (b == MINUS) {
                        while (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == DOT) {
                                while (buffer.hasRemaining()) {
                                    b = buffer.get();
                                    if (b == SEPERATOR || b == NEWLINE || b == RETURN) {
//										item[j][i] = item[j][i] * Math.pow(10,-k);
                                        item[j][i] = item[j][i] / potence[k];
                                        break;
                                    } else if (!buffer.hasRemaining()) {
                                        item[j][i] = (item[j][i] * 10 + (b - 48)) / potence[k + 1];
                                        break;
                                    } else if (b == (byte) 'e' || b == (byte) 'E') {
                                        item[j][i] = item[j][i] / potence[k];
                                        // attention: returns infinity in not-acceptable exponents
                                        int exp = getExponent(buffer);
                                        if (exp >= 0) {
                                            if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                                            else item[j][i] = item[j][i] * 10 * 1e308;
                                        } else {
                                            if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                                            else item[j][i] = item[j][i] / 10 * 1e-323;
                                        }
                                        break;
                                    } else {
//										item[j][i] += (b - 48) * Math.pow(10, -(++k));
                                        item[j][i] = item[j][i] * 10 + (b - 48);
                                        k++;
                                    }
                                }
                                break;
                            } else if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                                break;
                            } else if (b == (byte) 'e' || b == (byte) 'E') {
                                // attention: returns infinity in not-acceptable exponents
                                int exp = getExponent(buffer);
                                if (exp >= 0) {
                                    if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                                    else item[j][i] = item[j][i] * 10 * 1e308;
                                } else {
                                    if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                                    else item[j][i] = item[j][i] / 10 * 1e-323;
                                }
                            } else
                                item[j][i] = item[j][i] * 10 + (b - 48);
                        }
                        item[j][i] = -item[j][i];
                        break;
                    } else if (b == DOT) {
                        while (buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == SEPERATOR || b == NEWLINE || b == RETURN) {
//								item[j][i] = item[j][i] * Math.pow(10,-k);
                                item[j][i] = item[j][i] / potence[k];
                                break;
                            } else if (!buffer.hasRemaining()) {
                                item[j][i] = (item[j][i] * 10 + (b - 48)) / potence[k + 1];
                                break;
                            } else if (b == (byte) 'e' || b == (byte) 'E') {
                                item[j][i] = item[j][i] / potence[k];
                                // attention: returns infinity in not-acceptable exponents
                                int exp = getExponent(buffer);
                                if (exp >= 0) {
                                    if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                                    else item[j][i] = item[j][i] * 10 * 1e308;
                                } else {
                                    if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                                    else item[j][i] = item[j][i] / 10 * 1e-323;
                                }
                                break;

                            } else {
//                              item[j][i] += (b - 48) * Math.pow(10, -(++k));
                                item[j][i] = item[j][i] * 10 + (b - 48);
                                k++;
                            }
                        }
                        break;
                    } else if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        break;
                    } else if (b == (byte) 'e' || b == (byte) 'E') {
                        // attention: returns infinity in not-acceptable exponents
                        int exp = getExponent(buffer);
                        if (exp >= 0) {
                            if (exp <= 308) item[j][i] = item[j][i] * potence[exp];
                            else item[j][i] = item[j][i] * 10 * 1e308;
                        } else {
                            if (exp >= -323) item[j][i] = item[j][i] * negpotence[-exp];
                            else item[j][i] = item[j][i] / 10 * 1e-323;
                        }
                    } else {
                        item[j][i] = item[j][i] * 10 + (b - 48);
                    }
                } // end while

                // modified
                if (isDiscret[j]) {
                    for (int m = 0; m < wordStackSize[j]; m++) {
                        if (discretValue[j][m] == item[j][i]) {
                            valueFound = true;
                            findValueIndex = m;
                            break;
                        }
                    }
                    if (!valueFound) {
                        wordCount[j][wordStackSize[j]] = 1; // new item inserted
                        discretValue[j][wordStackSize[j]] = item[j][i];
                        wordStackSize[j]++;
                        if (wordStackSize[j] >= discretLimit_c[j]) {
                            isDiscret[j] = false;
                        }
                    } else {
                        wordCount[j][findValueIndex]++;
                        valueFound = false;
                    }
                }

                // end modified


                buffer.position(buffer.position() - 1);
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR) {
                        j++;
                        break;
                    } else if (b == RETURN) {
                        progressing(i); // execute progressing() every new line
                        buffer.mark();
                        if (buffer.hasRemaining()) {
                            if (buffer.get() == NEWLINE) {
                                i++;
                                j = 0;
                            } else {
                                buffer.reset();
                                i++;
                                j = 0;
                            }
                        }
                        break;
                    } else if (b == NEWLINE) {
                        progressing(i); // execute progressing() every new line
                        i++;
                        j = 0;
                        break;
                    }
                }
            }

        }
    }


    /**
     * checks byte-arrays for equality
     *
     * @param char1 first byte-array
     * @param char2 second byte-array
     * @return true, if char1==char2, else false
     */
    private boolean eqCharArray(byte[] char1, byte[] char2) {

        int i = char1.length;
        int j = char2.length;

        if (i != j)
            return false;

        for (int k = 0; k < i; k++) {
            if (char1[k] == char2[k]) {
            } else
                return false;
        }

        return true;

    }


    /**
     * @param charArray byte-array to be converted
     * @param isNA      tag for helping convertion
     * @return converted double
     */
    public double charArraytoDouble(byte[] charArray, boolean isNA) {
        double number = 0;
        boolean dotAvailable = false;
        int l = 0;
        int j = 0;

        if (isNA) return Double.MAX_VALUE;

        if (charArray[0] != '-') {
            for (int k = 0; k < charArray.length; k++) {
                if (charArray[k] == '.') {
                    dotAvailable = true;
                    l = k;
                    break;
                } else {
                    number = number * 10 + (charArray[k] - 48);
                }
            }
            if (dotAvailable) {
                for (int k = l + 1; k < charArray.length; k++) {
                    j++;
                    number = number + ((charArray[k] - 48) * Math.pow(10, -j));

                }
            }

        } else {
            for (int k = 1; k < charArray.length; k++) {
                if (charArray[k] == '.') {
                    dotAvailable = true;
                    l = k;
                    break;
                } else {
                    number = number * 10 + (charArray[k] - 48);
                }

            }
            if (dotAvailable) {
                for (int k = l + 1; k < charArray.length; k++) {
                    j++;
                    number = number + ((charArray[k] - 48) * Math.pow(10, -j));
                }
            }
            number = -number;
        }
        return number;
    }


    /**
     * method for finding neighbourhood of error-places in buffer
     *
     * @param buffer   associated ByteBuffer
     * @param position position to get neighbourhood of
     * @return neighbourhood of position
     */
    public StringBuffer findRegion(ByteBuffer buffer, int position) {

        byte b;
        StringBuffer region = new StringBuffer();
        buffer.position(position);
        int k = 0;
        int i = 0;

        while (buffer.position() > 0) {
            buffer.position(buffer.position() - 1);
            b = buffer.get(buffer.position());
            if (b == RETURN || b == NEWLINE) {
                break;
            } else {
                if (k >= 15) break;
                k++;
            }
        }
        buffer.position(buffer.position() + 1);
        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == RETURN || b == NEWLINE) {
                break;
            } else {
                if (i == 15 + k) break;
                else i++;
//				region.append((char)b);
                region.append(b);
            }

        }
        return region;

    }


    /**
     * compares byte-arrays
     *
     * @param char1 first byte-array
     * @param char2 second byte-array
     * @return -1 if char1<char2, 0 if char1==char2, 1 if char1>char2
     */
    // compare char arrays by lexicographical order
    // NullPointerException not implemented cause of speed performance
    // return -1 if char1 < char2, 0 if char1 = char2, +1 if char1 > char2
    private int compareCharArrays(byte[] char1, byte[] char2) {

        if (char1.length < char2.length) {
            for (int i = 0; i < char1.length; i++) {
                if (char1[i] < char2[i]) {
                    return -1;
                } else if (char1[i] > char2[i]) {
                    return 1;
                } else {
                    continue;
                }
            }
            return -1;
        } else if (char1.length > char2.length) {
            for (int i = 0; i < char2.length; i++) {
                if (char1[i] < char2[i]) {
                    return -1;
                } else if (char1[i] > char2[i]) {
                    return 1;
                } else {
                    continue;
                }
            }
            return 1;
        } else {
            for (int i = 0; i < char1.length; i++) {
                if (char1[i] < char2[i]) {
                    return -1;
                } else if (char1[i] > char2[i]) {
                    return 1;
                } else {
                    continue;
                }
            }
            return 0;
        }
    }


    private void progressing(int i) {

        timestop = System.currentTimeMillis();

        if (timestop - timestart >= 250) {
            timeStampCounter++;
            timestart = System.currentTimeMillis();
            System.out.println("old: " + (double) i / (double) lines);
            System.out.println("old: " + (double) i);
            System.out.println("old: " + (double) lines);


            if (prId != null && lines > 0)
                prId.setProgress((double) i / (double) lines * 0.85 + .1);


        }
    }


    /**
     * method for finding words in list
     *
     * @param list          list to go through
     * @param listStackSize limit of list (not capacity!, capacity==list.size)
     * @param word          word to be found in list
     * @return position of found word in list
     */
    // list has to be sorted
    // returns position if word found or added to list and
    // sets wordNotFound = true or false
    private int findWord(byte[][] list, int listStackSize, byte[] word) {

        int lb = 0;
        int rb = listStackSize - 1;
        int pivot = (rb - lb) / 2;
        int compareNumber;

        while (true) {
            // no word found
            if (rb - lb <= 0) {
                wordNotFound = true;

                /*System.out.println("leftBound = " + lb);
                    System.out.println("rightBound = " + rb);
                    System.out.println("pivot = " + pivot);*/

                compareNumber = compareCharArrays(word, list[pivot]);
                //System.out.println("" + new String(word) + "	" + new String(list[pivot]) + "	" + compareNumber);

                if (compareNumber == -1) {
                    wordNotFound = true;
                    return lb;
                } else if (compareNumber == 1) {
                    wordNotFound = true;
                    return lb + 1;
                } else {
                    wordNotFound = false;
                    return lb;
                }
            }

            compareNumber = compareCharArrays(word, list[pivot]);
            if (compareNumber == -1) {
                rb = pivot - 1;
                pivot = lb + (rb - lb) / 2;
                continue;
            } else if (compareNumber == 1) {
                lb = pivot + 1;
                pivot = lb + (rb - lb) / 2;
                continue;
            } else {
                wordNotFound = false;
                return pivot;
            }
        }
    }


    /**
     * Easy-checking of words (doesn't check doubles).
     *
     * @param SEPERATOR associated seperator in file
     * @return true if words were converted correct, else false
     */
    // very easy check-text method, doesn't check doubles (not yet)
    private boolean checkIt(byte SEPERATOR) {

        int countWord = 0, countNumber = 0;
        boolean dotAvailable = false;
        StringBuffer text = new StringBuffer();
        String text1 = new String();
        String text2 = new String();
        byte b;
        int i = 0, j = 0;

        buffer.rewind();
        buffer.position(getPositionSecondLine(buffer));
        while (buffer.hasRemaining()) {

            b = buffer.get();

            if (b == SEPERATOR) {
                text1 = new String(text);
                text = new StringBuffer();
                if (!numericalColumn[j]) {
                    text2 = new String((char[]) getItem(j, i));
                    countWord++;
                } else {
                    text2 = text1;
                    countNumber++;
                }
                if (!text1.equals(text2)) {
                    System.out.println("text1 does not equal text2");
                    return false;
                } else {
                    text1 = "";
                    text2 = "";
                }
                j++;
                dotAvailable = false;
                continue;
            } else if (b == RETURN) {
                buffer.mark();
                if (buffer.get() == NEWLINE) {
                } else {
                    buffer.reset();
                }
                text1 = new String(text);
                text = new StringBuffer();
                if (!numericalColumn[j]) {
                    text2 = new String((char[]) getItem(j, i));
                    countWord++;
                } else {
                    text2 = text1;
                    countNumber++;
                }
                if (!text1.equals(text2)) {
                    System.out.println("text1 does not equal text2, i = " + i + " j = " + j);
                    return false;
                } else {
                    text1 = "";
                    text2 = "";
                }
                i++;
                j = 0;
                dotAvailable = false;
                continue;
            } else if (b == NEWLINE) {
                text1 = new String(text);
                text = new StringBuffer();
                if (!numericalColumn[j]) {
                    text2 = new String((char[]) getItem(j, i));
                    countWord++;
                } else {
                    text2 = text1;
                    countNumber++;
                }
                if (!text1.equals(text2)) {
                    System.out.println("text1 does not equal text2");
                    return false;
                } else {
                    text1 = "";
                    text2 = "";
                }
                i++;
                j = 0;
                dotAvailable = false;
                continue;
            } else {
                if (b == DOT) {
                    dotAvailable = true;
                }
                text.append((char) b);
            }
        }
        return true;
    }


    private byte[] doubleToCharArray(double d, boolean dotAvailable) {

        return null;
    }


    /**
     * checks if a polygon-token ("/P") is available in headline additionally remarks hard reading error, if more than 1
     * polygon is available
     *
     * @param buffer associated ByteBuffer
     * @param format format of text
     * @return true if check was successfull (that means: exactly 1 token in head)
     */
    public boolean isPolygonAvailableInHead(ByteBuffer buffer, String format) {

        byte b;
        boolean polyavailable = false;
        byte SEPERATOR = TAB; // default SEPERATOR
        buffer.rewind();

        if (format == "TAB-Format") {
            SEPERATOR = TAB;
        } else if (format == "KOMMA-Format") {
            SEPERATOR = KOMMA;
        } else if (format == "SPACE-Format") {
            SEPERATOR = SPACE;
        } else if (format == "KOMMA-QUOTE-Format") {
            SEPERATOR = KOMMA;
        }

        if (format == "TAB-Format" || format == "KOMMA-Format") {
            b = buffer.get();
            if (b == (byte) '/') {
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == (byte) 'P') {
                        polyavailable = true;
                    } else {
                        buffer.rewind();
                    }
                }
            } else {
                buffer.rewind();
            }

            while (buffer.hasRemaining()) {
                b = buffer.get();

                if (b == SEPERATOR) {
                    if (buffer.hasRemaining()) {
                        b = buffer.get();
                        if (b == (byte) '/') {
                            if (buffer.hasRemaining()) {
                                b = buffer.get();
                                if (b == 'P') {
                                    if (polyavailable) {
                                        error[0] = "hardError: more than 1 polygon available";
                                        errorposition = buffer.position();
                                        hardReadError = true;
                                    } else {
                                        polyavailable = true;
                                    }
                                }

                            }
                        }
                    }
                } else if (b == RETURN || b == NEWLINE) {
                    buffer.rewind();
                    return polyavailable;
                }
            }

            return true;
        } else {
            // format == "SPACE-Format" or format == "KOMMA-QUOTE-Format"
            boolean QUOTEAvailable = false;
            b = buffer.get();
            if (b == (byte) '"' && buffer.hasRemaining()) {
                b = buffer.get();
                if (b == (byte) '/' && buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == (byte) 'P') {
                        polyavailable = true;
                    } else {
                        buffer.rewind();
                    }
                } else {
                    buffer.rewind();
                }
            }
            while (buffer.hasRemaining()) {
                b = buffer.get();

                if (b == SEPERATOR && buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == (byte) '"' && !QUOTEAvailable && buffer.hasRemaining()) {
                        b = buffer.get();
                        QUOTEAvailable = true;
                        if (b == (byte) '/' && buffer.hasRemaining()) {
                            b = buffer.get();
                            if (b == 'P') {
                                if (polyavailable) {
                                    error[0] = "hardReadError: more than 1 polygon available";
                                    errorposition = buffer.position();
                                    hardReadError = true;
                                } else {
                                    polyavailable = true;
                                }
                            }

                        }
                    } else continue;
                } else if (b == RETURN || b == NEWLINE) {
                    buffer.rewind();
                    return polyavailable;
                }
            }

            return true;
        }
    }

// getPolygonName: returns polygonname if available, else null


    /**
     * method to get polygon-name in file polygon-names can be found at the end of file
     *
     * @param buffer associated ByteBuffer
     * @return polygon-name if available, else null
     */
    private String getPolygonName(ByteBuffer buffer) {

        byte b;
        int startposition = buffer.limit() - 1;
        int limitposition = buffer.limit();
        StringBuffer strbuf = new StringBuffer();

        b = buffer.get(buffer.limit() - 1);
        if (b == RETURN || b == NEWLINE) {
            return null;
        }

        buffer.position(buffer.limit() - 1);

        while (buffer.position() > 0) {
            b = buffer.get(buffer.position());
            if (b == RETURN || b == NEWLINE) {
                startposition = buffer.position() + 1;
                break;
            } else {
                buffer.position(buffer.position() - 1);
            }
        }

        buffer.position(startposition);

        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == SEPERATOR) {
                // ERROR
                error[0] = "hardError: SEPERATOR in polygon name, maybe more than 1 polygon name available";
                hardReadError = true;
                errorposition = buffer.position();
                return null;
            } else strbuf.append((char) b);
        }

        buffer.position(startposition - 1);

        // test existance of empty lines before polygonName
        // sets buffer.limit() to last element
        while (buffer.position() > 0) {
            b = buffer.get(buffer.position());
            if (b == RETURN || b == NEWLINE) {
                buffer.position(buffer.position() - 1);
            } else {
                limitposition = buffer.position() + 1;
                break;
            }
        }

        buffer.position(limitposition);
        b = buffer.get();
        if (b == RETURN) {
            b = buffer.get();
            if (b == RETURN) {
                // alles ok.
            } else if (b == NEWLINE) {
                b = buffer.get();
                if (b == RETURN || b == NEWLINE) {
                    // alles ok.
                } else {
                    // ERROR
                    error[0] = "hardError: no empty line before PolygonName in file";
                    hardReadError = true;
                    errorposition = buffer.position();
                }
            } else {
                // ERROR
                error[0] = "hardError: no empty line before PolygonName in file";
                hardReadError = true;
                errorposition = buffer.position();
            }
        } else if (b == NEWLINE) {
            b = buffer.get();
            if (b == RETURN) {
                // alles ok.
            } else if (b == NEWLINE) {
                // alles ok.
            } else {
                // ERROR
                error[0] = "hardError: no empty line before PolygonName in file";
                hardReadError = true;
                errorposition = buffer.position();
            }
        } else {
            // ERROR
            error[0] = "hardError: no empty line before PolygonName in file";
            hardReadError = true;
            errorposition = buffer.position();
        }


        buffer.limit(limitposition);

        if (error[0] != null) {
            return null;
        } else {
            return new String(strbuf);
        }
    }


    /**
     * Convertion must be extended to more platforms
     *
     * @param b byte-array to be converted
     * @return converted char-array
     */
    // needs supportability on every machine, need general code converter
    public char[] byteToCharArray(byte[] b) {
        char[] c = new char[b.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = (char) b[i];
        }
        return c;
    }


    /**
     * method to get linebreaker
     *
     * @param buffer associated ByteBuffer
     * @return "RN" for /r/n, "R" for /r and "N" for /n
     */
    public String getNewLineBreaker(ByteBuffer buffer) {
        byte b;
        String str = "";
        buffer.rewind();
        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == RETURN) {
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == NEWLINE) {
                        str = "RN";
                        break;
                    } else {
                        str = "R";
                        break;
                    }
                } else {
                    str = "R";
                    break;
                }
            } else if (b == NEWLINE) {
                str = "N";
                break;
            }
        }
        return str;
    }

    // double reaches exponent == -323 .. +308 in Java / R
    // we have to look at this, Java doesn't accept other
    // we do not allow "E" following by no number


    public boolean isExponent(ByteBuffer buffer) {

        byte b;
        // gerade wurde ein "e" oder ein "E" eingelesen
        buffer.mark();
        while (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == MINUS) {
                if (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (!isNumber(b)) break; // "E-" following by no number
                } else break;  // "E-" at EOF
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (isNumber(b))
                        if (!buffer.hasRemaining()) {
                            buffer.reset();
                            return true;
                        } else continue;
                    else if (b == RETURN || b == NEWLINE || b == SEPERATOR) {
                        buffer.reset(); // next byte is a number
                        return true;
                    } else break;
                }
                break;
            } else {
                if (!isNumber(b)) break; // "E-" following by no number
                else buffer.position(buffer.position() - 1);
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (isNumber(b)) {
                        if (!buffer.hasRemaining()) {
                            buffer.reset();
                            return true;
                        } else continue;
                    } else if (b == RETURN || b == NEWLINE || b == SEPERATOR) {
                        buffer.reset(); // next byte is a number
                        return true;
                    } else break;
                }
                break;
            }
        }

        buffer.reset();
        return false;
    }


    // get sure, that isExponent() was run before
    // returns exponent and sets position of buffer to seperator/newline/return


    public int getExponent(ByteBuffer buffer) {

        int exponent = 0; // -323 .. 308
        byte b = 0;
        // gerade wurde ein "e" oder ein "E" eingelesen
        buffer.mark();
        if (buffer.hasRemaining()) {
            b = buffer.get();
            if (b == MINUS) {
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        exponent = -exponent;
                        buffer.position(buffer.position() - 1);
                        break;
                    } else {
                        exponent = exponent * 10 + (b - 48);
                        if (!buffer.hasRemaining()) {
                            exponent = -exponent;
                            break;
                        }
                    }
                }
            } else {
                buffer.position(buffer.position() - 1);
                while (buffer.hasRemaining()) {
                    b = buffer.get();
                    if (b == SEPERATOR || b == RETURN || b == NEWLINE) {
                        buffer.position(buffer.position() - 1);
                        break;
                    } else {
                        exponent = exponent * 10 + (b - 48);
//						if(!buffer.hasRemaining()) {
//							break;
//						}
                    }
                }
            }

        }
        return exponent;
    }


    public void handleException(final Exception e) {
        final javax.swing.JFrame frame = new javax.swing.JFrame();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                stringWriter.write("\nSystem will be exited");
                JOptionPane.showMessageDialog(frame, stringWriter.toString(), "Exception occurred", JOptionPane.ERROR_MESSAGE);
                // ändere das
                System.exit(0);
            }
        });
    }


    public void handleError(final Error e) {
        final javax.swing.JFrame frame = new javax.swing.JFrame();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                stringWriter.write("\nSystem will be exited");
                JOptionPane.showMessageDialog(frame, stringWriter.toString(), "Error occurred", JOptionPane.ERROR_MESSAGE);
                // ändere das
                System.exit(0);
            }
        });
    }


    public byte[] cutSpacesInItem(final byte[] temp) {
        if (temp[0] != SPACE && temp[temp.length - 1] != SPACE) return temp;

        int beginSpaceCount = 0, endSpaceCount = 0;

        // begin space handling
        for (int i = 0; i < temp.length; i++) {
            if (temp[i] == SPACE) beginSpaceCount++;
            else break;
        }
        for (int i = 0; i < temp.length; i++) {
            if (temp[temp.length - i - 1] == SPACE) endSpaceCount++;
            else break;
        }

        byte[] ret = new byte[temp.length - beginSpaceCount - endSpaceCount];
        System.arraycopy(temp, beginSpaceCount, ret, 0, ret.length);
        return ret;

    }

/*	public static void main(String args[]) {
        try {
//		new BufferTokenizer(10,10,"C:\\work files\\Families '95(3).txt");
        new BufferTokenizer(10,10,"D:\\Test2.txt");
        } catch (Exception e) {
            System.err.println(e);
        }
    } */


}
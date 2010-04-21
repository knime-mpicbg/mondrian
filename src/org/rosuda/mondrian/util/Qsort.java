package org.rosuda.mondrian.util;//
// Quick sort - from K+R. The optional flag argument is handed back
// to compare(), so the original qsort() call can easily control the
// comparison.
//


public class Qsort

{

    private static int[] index;
    static String Stmp;
    static double Dtmp;
    static int tmpI;

    ///////////////////////////////////////////////////////////////////////////


    public static int[]
    qsort(String a[]) {

        qsort(a, 0, a.length - 1);
        return index;
    }

    ///////////////////////////////////////////////////////////////////////////


    public static int[]
    qsort(String a[], int low, int high) {

        index = new int[high + 1];
        for (int i = 0; i < high + 1; i++)
            index[i] = i;
        String[] b = a.clone();
        QuickSort(b, low, high);
        return index;
    }

    ///////////////////////////////////////////////////////////////////////////


    private static void
    QuickSort(String v[], int left, int right) {

        int i;
        int last;

        if (left < right) {
            Sswap(v, left, (left + right) / 2);
            last = left;
            for (i = left + 1; i <= right; i++) {
                if (v[i].compareTo(v[left]) < 0)
                    Sswap(v, ++last, i);
            }    // End for //
            Sswap(v, left, last);
            QuickSort(v, left, last - 1);
            QuickSort(v, last + 1, right);
        }   // End if //
    }

    ///////////////////////////////////////////////////////////////////////////


    private static void
    Sswap(String v[], int i, int j) {

        Stmp = v[i];
        v[i] = v[j];
        v[j] = Stmp;

        tmpI = index[i];
        index[i] = index[j];
        index[j] = tmpI;
    }

    ///////////////////////////////////////////////////////////////////////////

    // The same show for doubles

    ///////////////////////////////////////////////////////////////////////////


    public static int[]
    qsort(double a[]) {

        qsort(a, 0, a.length - 1);
        return index;
    }

    ///////////////////////////////////////////////////////////////////////////


    public static int[]
    qsort(double a[], int low, int high) {

        index = new int[high + 1];
        for (int i = 0; i < high + 1; i++)
            index[i] = i;
        double[] b = a.clone();
        QuickSort(b, low, high);
        return index;
    }

    ///////////////////////////////////////////////////////////////////////////


    private static void
    QuickSort(double v[], int left, int right) {

        int i;
        int last;

        if (left < right) {
            int piv = (left + right) / 2;
            Dswap(v, left, piv);//(left+right)/2);
            last = left;
            for (i = left + 1; i <= right; i++) {
                if (v[i] < v[left])
                    Dswap(v, ++last, i);
            }    // End for //
            Dswap(v, left, last);
            if (left != last) {                        // at least one permutation .. business as usual
                QuickSort(v, left, last - 1);
            } else {                                // NO permutation pivot is smalest element
                while ((last < v.length - 1) && (v[++last] == v[left])) {
                }    // skip all elements which are as small as the pivot
                last--;
            }
            QuickSort(v, last + 1, right);
        }   // End if //
    }

    ///////////////////////////////////////////////////////////////////////////


    private static void
    Dswap(double v[], int i, int j) {

        Dtmp = v[i];
        v[i] = v[j];
        v[j] = Dtmp;

        tmpI = index[i];
        index[i] = index[j];
        index[j] = tmpI;
    }

    ///////////////////////////////////////////////////////////////////////////
}


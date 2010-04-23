package de.mpicbg.sweng.mondrian.core;

import java.util.BitSet;
import java.util.Vector;               // for Vector


public class InteractionSet implements Cloneable {

    private Vector Set = new Vector(256, 0);
    private Vector Strip;
    private int k;
    private int[] insertAt;
    private final int IN = 1;
    private final int OUT = 0;


    public InteractionSet(int k) {
        this.k = k;
        insertAt = new int[k];
    }


    public Object clone() {
        InteractionSet clone = new InteractionSet(this.k);
        clone.Set = (Vector) Set.clone();
        clone.Strip = (Vector) Strip.clone();
        clone.k = this.k;
        System.arraycopy(this.insertAt, 0, clone.insertAt, 0, this.insertAt.length);

        return clone;
    }


    public boolean isMember(int[] k) {

        int l = Set.size();
        BitSet test = new BitSet(k.length);

        for (int i = 0; i < k.length; i++)
            test.set(k[i]);

        for (int i = 0; i < l; i++) {
            if (test.equals(Set.elementAt(i)))
                return true;
        }
        return false;
    }


    boolean isMember(BitSet test) {

        for (int i = 0; i < Set.size(); i++) {
            if (test.equals(Set.elementAt(i)))
                return true;
        }
        return false;
    }


    public void newMember(int[] inters) {

        if (!isMember(inters)) {
            BitSet newI = new BitSet(inters.length);

            for (int i = 0; i < inters.length; i++)
                newI.set(inters[i]);

            int pos = nBits(newI) - 1;
            if (insertAt[pos] < Set.size())
                Set.insertElementAt(newI, insertAt[pos]);
            else
                Set.addElement(newI);

            for (int i = pos; i < k; i++)
                insertAt[i]++;

            maintainHirarchie(newI, IN);
            Strip = (Vector) Set.clone();
            strip();
        }
    }


    void newMember(BitSet incl) {

        BitSet newI = (BitSet) incl.clone();

        int pos = nBits(newI) - 1;
        if (insertAt[pos] < Set.size())
            Set.insertElementAt(newI, insertAt[pos]);
        else
            Set.addElement(newI);
        for (int i = pos; i < k; i++)
            insertAt[i]++;
    }


    void setMember(int[] k, int l) {

        BitSet newI = new BitSet(k.length);

        for (int i = 0; i < k.length; i++)
            newI.set(k[i]);

        Set.setElementAt(newI, l);
    }


    public void deleteMember(int[] inter) {

        BitSet test = new BitSet(inter.length);

        for (int i = 0; i < inter.length; i++)
            test.set(inter[i]);

        for (int i = 0; i < Set.size(); i++)
            if (test.equals(Set.elementAt(i))) {
                Set.removeElementAt(i);
                for (int j = 0; j < k; j++)
                    if (insertAt[j] > i)
                        insertAt[j]--;
            }
        maintainHirarchie(test, OUT);
        Strip = (Vector) Set.clone();
        strip();
    }


    public void deleteMember(BitSet rem) {

        for (int i = 0; i < Set.size(); i++)
            if (rem.equals(Set.elementAt(i))) {
                Set.removeElementAt(i);
                for (int j = 0; j < k; j++)
                    if (insertAt[j] > i)
                        insertAt[j]--;
            }
    }


    public int[] memberAt(int k) {

        BitSet out = (BitSet) (Set.elementAt(k));
        int[] Inter = new int[nBits(out)];
        int count = 0;
        for (int i = 0; i < out.size(); i++)
            if (out.get(i))
                Inter[count++] = i;
        return Inter;
    }


    public int[] SmemberAt(int k) {

        BitSet out = (BitSet) (Strip.elementAt(k));
        int[] Inter = new int[nBits(out)];
        int count = 0;
        for (int i = 0; i < out.size(); i++)
            if (out.get(i))
                Inter[count++] = i;
        return Inter;
    }


    public int size() {
        return Set.size();
    }


    public int Ssize() {
        return Strip.size();
    }


    public void permute(int[] perm) {

        for (int i = 0; i < this.size(); i++) {
            int[] interPerm = this.memberAt(i);
            for (int j = 0; j < interPerm.length; j++) {
                int k = 0;
                while (perm[k] != interPerm[j])
                    k++;
                interPerm[j] = k;
            }
            this.setMember(interPerm, i);
        }
    }


    void maintainHirarchie(BitSet InOut, int dir) {

        //System.out.println("Maintain: "+InOut.toString());
        BitSet Inter = (BitSet) InOut.clone();
        if (dir == IN) {
            if (nBits(Inter) > 2) {
                for (int i = 0; i < Inter.size(); i++)
                    if (Inter.get(i)) {
                        Inter.clear(i);
                        if (!isMember(Inter)) {
                            newMember(Inter);
                            maintainHirarchie(Inter, IN);
                        }
                        Inter.set(i);
                    }
            }
        } else {
            for (int i = 0; i < Set.size(); i++) {
                BitSet Inter1 = (BitSet) Inter.clone();
                Inter1.and(((BitSet) Set.elementAt(i)));
                if (Inter.equals(Inter1))
                    deleteMember((BitSet) Set.elementAt(i));
            }
        }
    }


    void strip() {

        int i = Set.size();
        while (i > 0) {
            i--;
            BitSet Inter = ((BitSet) Strip.elementAt(i));
            int j = i;
            while (j > 0) {
                j--;
                BitSet inStrip = (BitSet) Strip.elementAt(j);
                if (!Inter.equals(inStrip)) {
                    BitSet Inter1 = (BitSet) Inter.clone();
                    Inter1.and(inStrip);
                    if (inStrip.equals(Inter1)) {
                        Strip.removeElementAt(j);
                        i--;
                    }
                }
            }
        }
    }


    int nBits(BitSet b) {

        int count = 0;
        for (int i = 0; i < b.size(); i++)
            if (b.get(i))
                count++;
        return count;
    }
}

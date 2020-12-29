package com.beyondeye.kjsonpatch.lcs;

import java.util.ArrayList;
import java.util.List;

/**
 * code extracted from Apache Commons Collections 4.1
 * Created by daely on 7/22/2016.
 */
public class ListUtils {
    //-----------------------------------------------------------------------
    /**
     * Returns the longest common subsequence (LCS) of two sequences (lists).
     *
     * @param <E>  the element type
     * @param a  the first list
     * @param b  the second list
     * @return the longest common subsequence
     * @throws NullPointerException if either list is {@code null}
     * @since 4.0
     */
    public static <E> List<E> longestCommonSubsequence(final List<E> a, final List<E> b) {
        return longestCommonSubsequence( a, b, DefaultEquator.defaultEquator() );
    }
    /**
     * Returns the longest common subsequence (LCS) of two sequences (lists).
     *
     * @param <E>  the element type
     * @param a  the first list
     * @param b  the second list
     * @param equator  the equator used to test object equality
     * @return the longest common subsequence
     * @throws NullPointerException if either list or the equator is {@code null}
     * @since 4.0
     */
    public static <E> List<E> longestCommonSubsequence(final List<E> a, final List<E> b,
                                                       final Equator<? super E> equator) {
        if (a == null || b == null) {
            throw new NullPointerException("List must not be null");
        }
        if (equator == null) {
            throw new NullPointerException("Equator must not be null");
        }

        final SequencesComparator<E> comparator = new SequencesComparator<E>(a, b, equator);
        final EditScript<E> script = comparator.getScript();
        final LcsVisitor<E> visitor = new LcsVisitor<E>();
        script.visit(visitor);
        return visitor.getSubSequence();
    }
    /**
     * A helper class used to construct the longest common subsequence.
     */
    private static final class LcsVisitor<E> implements CommandVisitor<E> {
        private ArrayList<E> sequence;

        public LcsVisitor() {
            sequence = new ArrayList<E>();
        }

        public void visitInsertCommand(final E object) {}

        public void visitDeleteCommand(final E object) {}

        public void visitKeepCommand(final E object) {
            sequence.add(object);
        }

        public List<E> getSubSequence() {
            return sequence;
        }
    }
}

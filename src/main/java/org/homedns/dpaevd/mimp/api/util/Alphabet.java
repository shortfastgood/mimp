/*
 * @(#) Alphabet.java 1.0
 *
 * Copyright (c) 2013 by DPAEVD All Rights Reserved.
 *
 * Please refer to the file "copyright.html" for important copyright and licensing information.
 */
package org.homedns.dpaevd.mimp.api.util;

import java.util.Arrays;

/**
 * @author Robert Sedgewick
 * @author Kevin Wayne
 * @author Daniele Denti <a href="mailto:daniele.denti@bluewin.ch">daniele.denti@bluewin.ch</a>
 * @version 2013.1
 */

public class Alphabet {

    /** The binary alphabet 01 */
    public static final Alphabet BINARY = new Alphabet("01");

    /** The octal alphabet 01234567 */
    public static final Alphabet OCTAL = new Alphabet("01234567");

    /** The decimal alphabet 0123456789 */
    public static final Alphabet DECIMAL = new Alphabet("0123456789");

    /** The hexadecimal alphabet 0123456789ABCDEFabcdef */
    public static final Alphabet HEXADECIMAL = new Alphabet("0123456789ABCDEFabcdef");

    /** The decimal alphabet supporting scientific notations */
    public static final Alphabet SCI = Alphabet.merge(DECIMAL.toString(), "'.Ee+-");

    /** The decimal alphabet supporting scientific notations */
    public static final Alphabet NUMBER = Alphabet.merge(HEXADECIMAL.toString(), SCI.toString());

    /** The DNA alphabet ACTG */
    public static final Alphabet DNA = new Alphabet("ACTG");

    /** The lower case ASCII alphabet abcdefghijklmnopqrstuvwxyz */
    public static final Alphabet LOWERCASE = new Alphabet("abcdefghijklmnopqrstuvwxyz");

    /** The lower case LATIN-1 alphabet àáââäæçèéêëìíîïðñòóôõöøùúûüýþ */
    public static final Alphabet EXTENDED_LOWERCASE = new Alphabet("àáâåäæçèéêëìíîïðñòóôõöøùúûüýþ");

    /** The upper case ASCII alphabet ABCDEFGHIJKLMNOPQRSTUVWXYZ */
    public static final Alphabet UPPERCASE = new Alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    /** The upper case LATIN-1 alphabet ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ */
    public static final Alphabet EXTENDED_UPPERCASE = new Alphabet("ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ");

    /** The protein alphabet ACDEFGHIKLMNPQRSTVWY */
    public static final Alphabet PROTEIN = new Alphabet("ACDEFGHIKLMNPQRSTVWY");

    /** The BASE64 alphabet ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/ */
    public static final Alphabet BASE64 = new Alphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");

    /** The ASCII alphabet */
    public static final Alphabet ASCII = new Alphabet(128);

    /** The ASCII printable alphabet */
    public static final Alphabet ASCII_PRINTABLE = Alphabet.merge(" !\"#$%&'()*+,-./", DECIMAL.toString(), ":;<=>?@",
            UPPERCASE.toString(), "[\\]_", LOWERCASE.toString(), "{|}");

    /** The LATIN-1 alphabet */
    public static final Alphabet EXTENDED_ASCII = new Alphabet(256);

    /** The LATIN-1 printable alphabet */
    public static final Alphabet EXTENDED_ASCII_PRINTABLE = Alphabet.merge(ASCII_PRINTABLE.toString(),
            "¡¢£¤¥¦©ª«¬®¯°±²³µ·¹º»¼½¾¿", EXTENDED_UPPERCASE.toString(), "×ß÷ÿ");

    /** The UNICODE alphabet */
    public static final Alphabet UNICODE16 = new Alphabet(65536);

    /** The white chars */
    public static final Alphabet WHITE_CHARS = new Alphabet(" \b\f\n\r\t");

    /**
     * Merges different character sequences to a single alphabet.
     *
     * @param alphabets The alphabets to be merged.
     * @return The alphabet resulting from the merge operation.
     * @throws IllegalArgumentException if a duplicate there is ad duplicate character.
     */
    public static Alphabet merge(final String... alphabets) {
        final StringBuilder buf = new StringBuilder();
        for (final String alphabet : alphabets) {
            for(int i=0; i < alphabet.length(); i++) {
                if (buf.toString().indexOf(alphabet.charAt(i)) < 0) {
                    buf.append(alphabet.charAt(i));
                }
            }
        }
        return new Alphabet(buf.toString());
    }

    private final char[] alphabet; // the characters in the alphabet

    private final int[] inverse; // indices

    private final int R; // the radix of the alphabet

    /**
     * Create a new Alphabet of UNICODE chars 0 to 255 (extended ASCII)
     */
    public Alphabet() {
        this(256);
    }

    /**
     * Creates a new Alphabet of UNICODE chars 0 to R-1.
     *
     * @param R the radix of the alphabet
     */
    public Alphabet(final int R) {
        alphabet = new char[R];
        inverse = new int[R];
        this.R = R;

        // can't use char since R can be as big as 65,536
        for (int i = 0; i < R; i++) {
            alphabet[i] = (char) i;
        }
        for (int i = 0; i < R; i++) {
            inverse[i] = i;
        }
    }

    /**
     * Creates a new Alphabet from chars in string.
     *
     * @param alpha The alphabet string.
     * @throws IllegalArgumentException if a duplicate there is ad duplicate character.
     */
    public Alphabet(final String alpha) {

        R = alpha.length();

        // check that alphabet contains no duplicate chars
        final boolean[] unicode = new boolean[Character.MAX_VALUE];
        for (int i = 0; i < R; i++) {
            final char c = alpha.charAt(i);
            if (unicode[c]) {
                throw new IllegalArgumentException(String.format("Duplicate alphabet character '%c' in string [%s]", c, alpha));
            }
            else {
                unicode[c] = true;
            }
        }

        alphabet = alpha.toCharArray();
        inverse = new int[Character.MAX_VALUE];
        Arrays.fill(inverse, -1);

        // can't use char since R can be as big as 65,536
        for (int c = 0; c < R; c++) {
            inverse[alphabet[c]] = c;
        }
    }

    /**
     * Is character c in the alphabet?
     *
     * @param c the character to check.
     * @return true if is part of the alphabet, otherwise false.
     */
    public boolean contains(final char c) {
        return c < inverse.length && inverse[c] != -1;
    }

    /**
     * Returns the radix.
     *
     * @return the radix.
     */
    public int R() {
        return R;
    }

    /**
     * @return the clone of the internal array.
     * @since 3.0
     */
    public char[] toArray() {
        final char[] clone = new char[alphabet.length];
        System.arraycopy(alphabet, 0, clone, 0, alphabet.length);
        return clone;
    }

    /**
     * Converts an index between 0 and R-1 into a char over this alphabet
     *
     * @param index the index to convert to char.
     * @return the char corresponding to the index.
     * @throws IndexOutOfBoundsException if the index is &lt;0 or &gt; R-1
     */
    public char toChar(final int index) {
        if (index < 0 || index >= R) {
            throw new IndexOutOfBoundsException("Alphabet index out of bounds");
        }
        return alphabet[index];
    }

    /**
     * Convert base-R integer into a String over this alphabet.
     *
     * @param indices The indices.
     * @return the corresponding string.
     */
    public String toChars(final int[] indices) {
        final StringBuilder s = new StringBuilder(indices.length);
        for (final int indice : indices) {
            s.append(toChar(indice));
        }
        return s.toString();
    }

    /**
     * Converts c to index between 0 and R-1.
     *
     * @param c the character to convert.
     * @return the corresponding index.
     */
    public int toIndex(final char c) {
        if (!contains(c)) {
            throw new IllegalArgumentException(String.format("Character %c not in alphabet", c));
        }
        return inverse[c];
    }

    /**
     * Convert String s over this alphabet into a base-R integer.
     *
     * @param s The string to convert.
     * @return the indices.
     */
    public int[] toIndices(final String s) {
        final char[] source = s.toCharArray();
        final int[] target = new int[s.length()];
        for (int i = 0; i < source.length; i++) {
            target[i] = toIndex(source[i]);
        }
        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new String(alphabet);
    }
}

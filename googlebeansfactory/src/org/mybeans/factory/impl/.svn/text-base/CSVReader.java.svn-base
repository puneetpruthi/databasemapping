/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.io.*;

/**
 * A wrapper class around BufferedReader to handle a character stream
 * that consists of comma separated values (CSVs)
 *
 * @author Jeffrey Eppinger (jle@cs.cmu.com)
 */

public class CSVReader {

    private BufferedReader br;

    /**
     * Constructor takes a java.io.Reader, such as a java.io.FileReader
     * @param in the data source
     */
    public CSVReader(Reader in) {
        br = new BufferedReader(in);
    }

    /**
     * Alternate constructor takes a java.io.Reader and buffer sz, such as a java.io.FileReader
     * @param in the data source
     * @param sz the buffer size to use
     */
    public CSVReader(Reader in, int sz) {
        br = new BufferedReader(in,sz);
    }

    /**
     * This is the only new method.  Like <tt>readLine()</tt> from
     * the BufferedReader (which it uses internally), <tt>readCSVLine()</tt>
     * reads one line and returns the comma separated values as in an array
     * of strings.  At the end of the file, <tt>readCSVLine()</tt> returns
     * <code>null</code> (just as <tt>readLine()</tt> does).
     *
     * <p>Spaces at the beginning or end of the values are not removed.
     *
     * <p>Quotes (") may be used to enclose commas in a value.  Unmatched
     *    quotes are closed by an end-of-line.
     *    Quotes are removed from the beginning and end of values
     *    only if the first and last characters of a value are quotes.
     *
     * @return an array of strings containing the values that
     *    are separated by commas on the line read.
     *    An empty line returns an array of length 1 containing an empty string.
     *    At end of file <code>null</code> is returned.
     */
    public String[] readCSVLine() throws IOException {

        // Get a line by calling the superclass's readLine method
        String line = br.readLine();

        // If we're at the end of the file, readLine returns null
        // so we return null.
        if (line == null) return null;

        // Count up the number of unquoted commas
        int commaCount = 0;
        boolean inQuote = false;
        for (int i=0; i<line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote && c == ',') {
                commaCount = commaCount + 1;
            }
        }

        // Allocate an array of the necessary size to return the strings
        String[] values = new String[commaCount+1];


        // In a loop, set beginIndex and endIndex to the start and end
        // positions of each argment and then use the substring method
        // to create strings for each of the comma separate values

        // Start beginIndex at the beginning of the String, position 0
        int beginIndex = 0;

        for (int i=0; i<commaCount; i++) {

            // set endIndex to the position of the (next) unquoted comma
            inQuote = false;
            int endIndex = beginIndex;
            char c = line.charAt(endIndex);
            while (c != ',' || inQuote) {
                if (c == '"') {
                    inQuote = !inQuote;
                }
                endIndex = endIndex+1;
                c = line.charAt(endIndex);
            }


            // if the argument begins and ends with quotes, remove them

            if (line.charAt(beginIndex) == '"' && line.charAt(endIndex-1) == '"') {

                // If we made it here, we have quotes around our string.
                // Add/substract one from the start/end of the args
                // to substring to get the value.  (See else comment
                // below for details on how this works.)

                values[i] = line.substring(beginIndex+1,endIndex-1);

            } else {

                // If we made it here, we don't have quotes around
                // our string.  Take the substring of this line
                // from the beginIndex to the endIndex.  The substring
                // method called on a String will return the portion
                // of the String starting with the beginIndex and up
                // to but not including the endIndex.

                values[i] = line.substring(beginIndex,endIndex);
            }

            // Set beginIndex to the position character after the
            // comma.  (Remember, endIndex was set to the position
            // of the comma.)
            beginIndex = endIndex + 1;
        }

        // handle the value that's after the last comma
        if (beginIndex == line.length()) {
            values[commaCount] = "";
        } else if (line.charAt(beginIndex) == '"' && line.charAt(line.length()-1) == '"') {
            values[commaCount] = line.substring(beginIndex+1,line.length()-1);
        } else {
            values[commaCount] = line.substring(beginIndex,line.length());
        }

        return values;
    }

    /**
     * Method forwarded to BufferedReader.
     */
    public void close() throws IOException       { br.close(); }

    /**
     * Method forwarded to BufferedReader.
     */
    public void mark(int readAheadLimit) throws IOException
                                                 { br.mark(readAheadLimit); }

    /**
     * Method forwarded to BufferedReader.
     */
    public boolean markSupported()               { return br.markSupported(); }

    /**
     * Method forwarded to BufferedReader.
     */
    public int read() throws IOException         { return br.read(); }

    /**
     * Method forwarded to BufferedReader.
     */
    public int read(char[] cbuf,int off,int len) throws IOException
                                                 { return br.read(cbuf,off,len); }

    /**
     * Method forwarded to BufferedReader.
     */
    public int read(char[] cbuf) throws IOException
                                                 { return br.read(cbuf); }
    /**
     * Method forwarded to BufferedReader.
     */
    public String readLine() throws IOException  { return br.readLine(); }

    /**
     * Method forwarded to BufferedReader.
     */
    public boolean ready() throws IOException    { return br.ready(); }

    /**
     * Method forwarded to BufferedReader.
     */
    public void reset() throws IOException       { br.reset(); }

    /**
     * Method forwarded to BufferedReader.
     */
    public long skip(long n) throws IOException  { return br.skip(n); }

}

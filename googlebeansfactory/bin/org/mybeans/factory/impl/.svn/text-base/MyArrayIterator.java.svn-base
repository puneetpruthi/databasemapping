/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class MyArrayIterator<T> implements Iterator<T> {
    private T[] array;
    private int pos = 0;

    public MyArrayIterator(T[] array) { this.array = array; }
    public boolean hasNext() { return pos < array.length; }

    public T next() {
        if (!hasNext()) throw new NoSuchElementException(String.valueOf(pos));
        return array[pos++];
    }

    public void remove() { throw new UnsupportedOperationException(); }
}

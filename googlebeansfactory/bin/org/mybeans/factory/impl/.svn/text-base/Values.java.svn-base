/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.lang.reflect.Array;

import org.mybeans.nonmodifiable.NMDate;
import org.mybeans.nonmodifiable.NMSQLDate;
import org.mybeans.nonmodifiable.NMTime;


public class Values {
    protected Values() {
        // Contains only static methods.
    }

    protected static Object cloneBeanValue(Object value) {
        // Note: does not clone referenced bean values

        if (value == null) return null;

        if (!value.getClass().isArray()) return cloneIfDate(value);

        // We have a non-null array
        if (value instanceof byte[])    return ((byte[])value).clone();
        if (value instanceof boolean[]) return ((boolean[])value).clone();
        if (value instanceof double[])  return ((double[])value).clone();
        if (value instanceof float[])   return ((float[])value).clone();
        if (value instanceof int[])     return ((int[])value).clone();
        if (value instanceof long[])    return ((long[])value).clone();

        Object[] array = (Object[]) value;
        Object[] clone = (Object[]) Array.newInstance(array.getClass().getComponentType(),array.length);
        for (int i=0; i<clone.length; i++) {
            clone[i] = cloneIfDate(array[i]);
        }
        return clone;
    }

    protected static java.util.Date cloneDate(java.util.Date date) {
        // copy dates, remember java.sql.Date & java.sql.Time are subclasses of java.util.Date
        long time = date.getTime();
        if (date instanceof java.sql.Date) return new java.sql.Date(time);
        if (date instanceof java.sql.Time) return new java.sql.Time(time);
        return new java.util.Date(time);
    }

    public static Object cloneIfDate(Object obj) {
        if (obj instanceof java.util.Date) {
            if (obj instanceof NMDate) return obj;
            if (obj instanceof NMTime) return obj;
            if (obj instanceof NMSQLDate) return obj;
            return cloneDate((java.util.Date) obj);
        }

        return obj;
    }

    protected static boolean equalByteArrays(byte[] a1, byte[] a2) {
        if (a1 == null && a2 == null) return true;
        if (a1 == null) return false;
        if (a2 == null) return false;

        if (a1.length != a2.length) return false;
        for (int i=0; i<a1.length; i++) {
            if (a1[i] != a2[i]) return false;
        }
        return true;
    }

}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;


public class DBValues extends Values {
    private DBValues() {
        // Cannot instantiate...static helper methods only.
    }

    private static StringBuffer appendDBValue(StringBuffer b, Object dbValue) {
        if (dbValue instanceof Object[]) {
            Object[] array = (Object[]) dbValue;
            return appendDBValues(b,array);
        }
        return b.append(dbValue);
    }


    public static StringBuffer appendDBValues(StringBuffer b, Object[] dbValues) {
        b.append('[');
        for (int i=0; i<dbValues.length; i++) {
            if (i > 0) b.append(',');
            appendDBValue(b,dbValues[i]);
        }
        b.append(']');
        return b;
    }

    public static int compareNonArrayNonNullDBValues(Property[] props, Object[] dbValues1, Object[] dbValues2) {
        for (int i=0; i<props.length; i++) {
            int c = compareNonArrayNonNullDBValues(props[i],dbValues1[i],dbValues2[i]);
            if (c != 0) return c;
        }

        return 0;
    }

    public static int compareNonArrayNonNullDBValues(Property prop, Object dbValue1, Object dbValue2) {
        if (dbValue1 == null) throw new AssertionError();
        if (dbValue2 == null) throw new AssertionError();

        if (prop instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
            Property[] refPriKeyProps = refProp.getRefBeanPrimaryKeyProperties();
            Object[] subDBValues1 = (Object[]) dbValue1;
            Object[] subDBValues2 = (Object[]) dbValue2;
            return compareNonArrayNonNullDBValues(refPriKeyProps,subDBValues1,subDBValues2);
        }

        return myCompare(dbValue1,dbValue2);
    }

    protected static boolean equalDBValues(Property[] props, Object[] dbValues1, Object[] dbValues2) {
        for (int i=0; i<props.length; i++) {
            Property prop = props[i];
            if (prop.isArray()) {
                Object[] a1 = (Object[]) dbValues1[i];
                Object[] a2 = (Object[]) dbValues2[i];
                if (!equalArrayDBValues(prop,a1,a2)) return false;
            } else {
                if (!equalNonArrayDBValues(prop,dbValues1[i],dbValues2[i])) return false;
            }
        }

        return true;
    }

    protected static boolean equalArrayDBValues(Property prop, Object[] dbValueArray1, Object[] dbValueArray2) {
        if (dbValueArray1 == null && dbValueArray2 == null) return true;
        if (dbValueArray1 == null) return false;
        if (dbValueArray2 == null) return false;
        if (dbValueArray1.length != dbValueArray2.length) return false;
        for (int i=0; i<dbValueArray1.length; i++) {
            if (!equalNonArrayDBValues(prop,dbValueArray1[i],dbValueArray2[i])) return false;
        }
        return true;
    }


    private static boolean equalNonArrayDBValues(Property[] props, Object[] dbValues1, Object[] dbValues2) {
        for (int i=0; i<props.length; i++) {
            if (!equalNonArrayDBValues(props[i],dbValues1[i],dbValues2[i])) return false;
        }

        return true;
    }

    protected static boolean equalNonArrayDBValues(Property prop, Object dbValue1, Object dbValue2) {
        if (dbValue1 == null && dbValue2 == null) return true;
        if (dbValue1 == null) return false;
        if (dbValue2 == null) return false;

        // Now we know we have two non-null values
        if (prop instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
            Property[] refPriKeyProps = refProp.getRefBeanPrimaryKeyProperties();
            Object[] subDBValues1 = (Object[]) dbValue1;
            Object[] subDBValues2 = (Object[]) dbValue2;
            return equalNonArrayDBValues(refPriKeyProps,subDBValues1,subDBValues2);
        }

        if (prop.getType() == byte[].class) {
            return equalByteArrays((byte[]) dbValue1, (byte[]) dbValue2);
        }

        return dbValue1.equals(dbValue2);
    }

    public static Object[] extractPrimaryKeyDBValues(int primaryKeyPropsLen, Object[] dbValues) {
        Object[] priKeyDBValues = new Object[primaryKeyPropsLen];
        for (int i=0; i<primaryKeyPropsLen; i++) {
            priKeyDBValues[i] = dbValues[i];
        }
        return priKeyDBValues;
    }
    
    public static byte[] getBytes(Object dbValue) {
    	if (dbValue == null)            return Encode.getNullBytes();
    	if (dbValue instanceof Boolean) return Encode.getBooleanBytes((Boolean) dbValue);
    	if (dbValue instanceof Double)  return Encode.getDoubleBytes( (Double)  dbValue);
    	if (dbValue instanceof Float)   return Encode.getFloatBytes(  (Float)   dbValue);
    	if (dbValue instanceof Integer) return Encode.getIntBytes((Integer) dbValue);
    	if (dbValue instanceof Long)    return Encode.getLongBytes(   (Long)    dbValue);
    	
    	if (dbValue instanceof java.util.Date) return Encode.getDateBytes((java.util.Date) dbValue);
    	if (dbValue instanceof String)         return Encode.getStringBytes((String) dbValue);
    	if (dbValue instanceof byte[])         return Encode.getBytesBytes((byte[]) dbValue);

    	if (dbValue instanceof Object[]) {
    		try {
	    		Object[] dbValueArray = (Object[]) dbValue;
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        	baos.write(Encode.getRawIntBytes(dbValueArray.length));
	    		for (int i=0; i<dbValueArray.length; i++) {
	    			baos.write(getBytes(dbValueArray[i]));
	    		}
	    		return baos.toByteArray();
    		} catch (IOException e) {
    			// Can't happen
    			e.printStackTrace();
    			throw new AssertionError("IOException thrown from ByteArrayOutputStream.write(byte[]): "+e.getMessage());
    		}
    	}

    	throw new AssertionError("Unknown object type: "+dbValue.getClass().getName());
    }

    private static Object[] makeDBArrayValues(Property property, Object obj) {
        if (obj == null) return null;

        // No arrays of byte[]

        int len = Array.getLength(obj);
        Object[] answer = new Object[len];
        for (int i=0; i<len; i++) {
            Object beanValue = Array.get(obj,i);
            // Do not call makeDBValue here because we will loop infinitely (because property.isArray() is true)
            // Note: arrays or arrays are not allowed
            if (beanValue == null) {
                answer[i] = null;
            } else if (property instanceof ReferencedBeanProperty) {
                ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
                answer[i] = makeDBRefPriKeyValues(refProp, beanValue);
            } else if (property instanceof EnumProperty) {
            	EnumProperty enumProp = (EnumProperty) property;
            	answer[i] = enumProp.makeDBValue(beanValue);
            } else {
                answer[i] = cloneIfDate(beanValue);
            }
        }
        return answer;
    }

    private static Object[] makeDBRefPriKeyValues(ReferencedBeanProperty refProp, Object refBean) {
    	AbstractFactory<?> refFactory = refProp.getFactory();
        Property[] refPriKeyProps = refFactory.primaryKeyProperties;
        Object[] dbValues = new Object[refPriKeyProps.length];
        for (int i=0; i<refPriKeyProps.length; i++) {
            Object keyValue = refFactory.getBeanValue(refBean,refPriKeyProps[i]);
            dbValues[i] = makeDBValue(refPriKeyProps[i],keyValue);
        }
        return dbValues;
    }

    public static Object makeDBValue(Property property, Object beanValue) {
        if (beanValue == null) return null;

        if (property.isArray()) {
            return makeDBArrayValues(property,beanValue);
        }

        if (property instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
            return makeDBRefPriKeyValues(refProp, beanValue);
        }
        
        if (property instanceof EnumProperty) {
        	EnumProperty enumProp = (EnumProperty) property;
        	return enumProp.makeDBValue(beanValue);
        }

        if (property.getType() == byte[].class) {
            return ((byte[])beanValue).clone();
        }

        return cloneIfDate(beanValue);
    }

    public static Object[] makeDBValues(Property[] properties, Object[] beanValues) {
        // This method will (call methods that) clone the values that are modifiable

        Object[] answer = new Object[properties.length];
        for (int i=0; i<properties.length; i++) {
            answer[i] = makeDBValue(properties[i],beanValues[i]);
        }

        return answer;
    }

    @SuppressWarnings("unchecked")
    private static int myCompare(Object aObj, Object bObj) {
        try {
            Comparable a = (Comparable) aObj;
            Comparable b = (Comparable) bObj;
            return a.compareTo(b);
        } catch (ClassCastException e) {
            throw new AssertionError(aObj.getClass()+" & "+bObj.getClass());
        }
    }

    public static String toString(Object dbValueOrValues) {
        StringBuffer b = new StringBuffer();
        return appendDBValue(b,dbValueOrValues).toString();
    }
}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.lang.reflect.Array;

import org.mybeans.factory.ReferencedBeanException;
import org.mybeans.factory.RollbackException;


public class BeanValues extends Values {
    private BeanValues() {
        // Cannot instantiate...static helper methods only.
    }

    public static boolean equalBeanValues(Object v1, Object v2, Property property) {
        if (!property.isArray()) return equalNonArrayBeanValues(v1,v2,property);

        if (v1 == null && v2 == null) return true;
        if (v1 == null) return false;
        if (v2 == null) return false;

        // We have two (non-null) arrays
        if (Array.getLength(v1) != Array.getLength(v2)) return false;

        for (int i=0, n=Array.getLength(v1); i<n; i++) {
            if (!equalNonArrayBeanValues(Array.get(v1,i),Array.get(v2,i),property)) return false;
        }

        return true;
    }

    private static boolean equalNonArrayBeanValues(Object v1, Object v2, Property property) {
        if (v1 == null && v2 == null) return true;
        if (v1 == null) return false;
        if (v2 == null) return false;

        // We have two non-null (non-array property) values

        if (property instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
            AbstractFactory<?> refFactory = refProp.getFactory();
            for (Property p : refFactory.primaryKeyProperties) {
                Object k1 = refFactory.getBeanValue(v1,p);
                Object k2 = refFactory.getBeanValue(v2,p);
                if (!equalNonArrayBeanValues(k1,k2,p)) return false;
            }
            return true;
        }

        // Note: byte[] is not an array property
        if (property.getType() == byte[].class) {
            return equalByteArrays((byte[]) v1, (byte[]) v2);
        }

        return v1.equals(v2);
    }

    private static Object makeBeanArray(Property property, Object[] dbArray) throws RollbackException {
        if (dbArray == null) return null;

        if (property.getType() == boolean[].class) {
            boolean[] answer = new boolean[dbArray.length];
            for (int i=0; i<answer.length; i++) answer[i] = (Boolean) dbArray[i];
            return answer;
        }

        if (property.getType() == double[].class) {
            double[] answer = new double[dbArray.length];
            for (int i=0; i<answer.length; i++) answer[i] = (Double) dbArray[i];
            return answer;
        }

        if (property.getType() == float[].class) {
            float[] answer = new float[dbArray.length];
            for (int i=0; i<answer.length; i++) answer[i] = (Float) dbArray[i];
            return answer;
        }

        if (property.getType() == int[].class) {
            int[] answer = new int[dbArray.length];
            for (int i=0; i<answer.length; i++) answer[i] = (Integer) dbArray[i];
            return answer;
        }

        if (property.getType() == long[].class) {
            long[] answer = new long[dbArray.length];
            for (int i=0; i<answer.length; i++) answer[i] = (Long) dbArray[i];
            return answer;
        }

        // Do not call makeBeanValue here because we will loop infinitely (because property.isArray() is true)
        // Note: arrays or arrays are not allowed, so no arrays of byte[]

        Object[] beanArray = (Object[]) Array.newInstance(property.getBaseType(),dbArray.length);
        for (int i=0; i<dbArray.length; i++) {
            if (property instanceof ReferencedBeanProperty) {
                ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
                Object[] refPriKeyDBValues = (Object[]) dbArray[i];
                beanArray[i] = makeBeanRefBeanValue(refProp,refPriKeyDBValues);
            } else if (property instanceof EnumProperty){
            	EnumProperty enumProp = (EnumProperty) property;
            	beanArray[i] = enumProp.makeBeanValue((String) dbArray[i]);
            } else {
                beanArray[i] = cloneIfDate(dbArray[i]);
            }
        }
        return beanArray;
    }

    private static Object makeBeanRefBeanValue(ReferencedBeanProperty refProp, Object[] refPriKeyDBValues) throws RollbackException {
        if (refPriKeyDBValues == null) return null;

        AbstractFactory<?> refFactory = refProp.getFactory();
        Property[] refPriKeyProps = refFactory.primaryKeyProperties;

        Object[] refPriKeyBeanValues = new Object[refPriKeyDBValues.length];
        for (int i=0; i<refPriKeyBeanValues.length; i++) {
            refPriKeyBeanValues[i] = makeBeanValue(refPriKeyProps[i],refPriKeyDBValues[i]);
        }

        Object refBean = refFactory.lookup(refPriKeyBeanValues);
        if (refBean != null) return refBean;
        
        throw new ReferencedBeanException("Referenced bean does not exist: "+refFactory.beanClass.getSimpleName()+"("+toString(refPriKeyBeanValues)+")",
        		null,null,refFactory.beanClass,refPriKeyBeanValues);
    }

    private static Object makeBeanValue(Property property, Object dbValue) throws RollbackException {
        if (dbValue == null) return null;

        if (property.isArray()) {
            Object[] dbArray = (Object[]) dbValue;
            return makeBeanArray(property,dbArray);
        }

        if (property instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) property;
            Object[] refPriKeyDBValues = (Object[]) dbValue;
            return makeBeanRefBeanValue(refProp,refPriKeyDBValues);
        }
        
        if (property instanceof EnumProperty) {
        	EnumProperty enumProp = (EnumProperty) property;
        	return enumProp.makeBeanValue((String) dbValue);
        }

        if (property.getType() == byte[].class) return ((byte[])dbValue).clone();

        return cloneIfDate(dbValue);
    }

    public static Object[] makeBeanValues(Property[] props, Object[] dbValues) throws RollbackException {
        Object[] beanValues = new Object[dbValues.length];
        for (int i=0; i<props.length; i++) {
            // System.out.println("BeanValues.makeBeanValues: "+props[i]+","+DBValues.toString(dbValues[i]));
            beanValues[i] = makeBeanValue(props[i],dbValues[i]);
        }
        return beanValues;
    }
    
    public static String toString(Object[] beanValues) {
        StringBuffer b = new StringBuffer();
        for (Object beanValue : beanValues) {
        	if (b.length() > 0) b.append(',');
        	b.append(beanValue);
        }
        return b.toString();
    }
}

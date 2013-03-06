/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

public class PrimaryKey<B> implements Comparable<PrimaryKey<B>> {
    private PrimaryKeyInfo<B> primaryKeyInfo;
    private Object[]       dbValues;

    public PrimaryKey(PrimaryKeyInfo<B> primaryKeyInfo, Object[] primaryKeyDBValues) {
        this.primaryKeyInfo = primaryKeyInfo;
        this.dbValues       = primaryKeyDBValues;
    }

    public int compareTo(PrimaryKey<B> other) {
        Property[] priKeyProps = primaryKeyInfo.getProperties();
        return DBValues.compareNonArrayNonNullDBValues(priKeyProps,dbValues,other.dbValues);
    }

    public boolean equals(Object obj) {
        if (obj instanceof PrimaryKey) {
            PrimaryKey<?> other = (PrimaryKey<?>) obj;
            if (primaryKeyInfo != other.primaryKeyInfo) return false;
            Property[] priKeyProps = primaryKeyInfo.getProperties();
            return DBValues.equalDBValues(priKeyProps,dbValues,other.dbValues);
        }
        return false;
    }

    public Object[] getDBValues() { return dbValues; }

    public int hashCode() {
        Property[] priKeyProps = primaryKeyInfo.getProperties();
        return hashCode(priKeyProps,dbValues);
    }

    private int hashCode(Property[] priKeyProps, Object[] myDBValues) {
        int answer = 0;
        for (int i=0; i<priKeyProps.length; i++) {
            Property prop = priKeyProps[i];
            if (prop instanceof ReferencedBeanProperty) {
                ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
                Property[] refPriKeyProps = refProp.getRefBeanPrimaryKeyProperties();
                Object[]   mySubDBVals    = (Object[]) myDBValues[i];
                answer ^= hashCode(refPriKeyProps,mySubDBVals);
            } else {
                answer ^= myDBValues[i].hashCode();
            }
        }
        return answer;
    }

    public boolean keyEquals(Object[] otherPrimaryKeyDBValues) {
        Property[] priKeyProps = primaryKeyInfo.getProperties();
        return DBValues.equalDBValues(priKeyProps,dbValues,otherPrimaryKeyDBValues);
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("PrimaryKey(");
        DBValues.appendDBValues(b,dbValues);
        b.append(')');
        return b.toString();
    }
}

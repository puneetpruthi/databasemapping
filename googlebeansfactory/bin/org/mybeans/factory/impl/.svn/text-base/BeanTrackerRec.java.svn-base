/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

public class BeanTrackerRec<B> {
    private PrimaryKey<B> key;
    private B             bean;
    private Object[]      dbValues;

    public BeanTrackerRec(PrimaryKey<B> key, B bean, Object[] dbValues) {
        this.key      = key;
        this.bean     = bean;
        this.dbValues = dbValues;
    }

    public PrimaryKey<B> getKey()      { return key;      }
    public B             getBean()     { return bean;     }
    public Object[]      getDBValues() { return dbValues; }

    public void setDBValues(Object[] newDBValues) {
    	dbValues = newDBValues;
    }
}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.RollbackException;
import org.mybeans.nonmodifiable.NMDate;
import org.mybeans.nonmodifiable.NMSQLDate;
import org.mybeans.nonmodifiable.NMTime;

public abstract class AbstractFactory<B> implements BeanFactory<B> {

    // Instance variables

    // Initialized by constructor
    protected Class<B> beanClass;
    protected AbstractFactory<?>[] referencedFactories;

    // Initialized by initProperties();
    protected Property[] properties            = null;
    protected Property[] primaryKeyProperties  = null;
    protected PrimaryKeyInfo<B> primaryKeyInfo = null;

    // Constructor

    protected AbstractFactory(Class<B> beanClass, String[] primaryKeyNames, AbstractFactory<?>[] referencedFactories) {
        this.beanClass = beanClass;
        this.referencedFactories = referencedFactories;

        primaryKeyInfo = PrimaryKeyInfo.getInstance(beanClass,primaryKeyNames,referencedFactories);
        primaryKeyProperties = primaryKeyInfo.getProperties();
        properties = Property.deriveProperties(beanClass,primaryKeyInfo,referencedFactories);
    }

    // Public methods
    
    public String computeDigest(B bean) throws RollbackException {
    	try {
    		if (bean == null) throw new NullPointerException("The \"bean\" argument is null");

    		MessageDigest md = MessageDigest.getInstance("SHA1");
	
			for (Property property : properties) {
                Object beanValue = getBeanValue(bean,property);
                Object dbValue = DBValues.makeDBValue(property,beanValue);
                byte[] bytes = DBValues.getBytes(dbValue);
                md.update(bytes);
			}

			byte[] digestBytes = md.digest();

			// Format the digest as a String
			StringBuffer digestSB = new StringBuffer();
			for (int i=0; i<digestBytes.length; i++) {
			  int lowNibble = digestBytes[i] & 0x0f;
			  int highNibble = (digestBytes[i]>>4) & 0x0f;
			  digestSB.append(Integer.toHexString(highNibble));
			  digestSB.append(Integer.toHexString(lowNibble));
			}
			String digestStr = digestSB.toString();
	
			return digestStr;
    	} catch (Exception e) {
    		TranImpl.rollbackAndThrow(e);
    		throw new AssertionError("rollbackAndThrow() returned");
    	}
	}

	public void copyInto(B from, B to) throws RollbackException {
		try {
			if (from == null) throw new NullPointerException("The \"from\" argument is null");
			if (to   == null) throw new NullPointerException("The \"to\" argument is null");
			if (!beanClass.isInstance(from)) throw new IllegalArgumentException("The \"from\" arg is not of type "+beanClass.getName()+" (it's "+from.getClass().getName()+")");
			if (!beanClass.isInstance(to  )) throw new IllegalArgumentException("The \"to\" arg is not of type "+beanClass.getName()+" (it's "+to.getClass().getName()+")");
	
			// we don't copy the primary key, so start with columns[1]
			for (Property property : properties) {
	            if (!property.isPrimaryKeyProperty()) {
	                Object fromValue = getBeanValue(from,property);
	                Object toValue   = Values.cloneBeanValue(fromValue);
	                setBeanValue(to,property,toValue);
	            }
			}
		} catch (Exception e) {
			TranImpl.rollbackAndThrow(e);
		}
	}

    public boolean equals(B bean1, B bean2) throws RollbackException {
    	try {
			if (bean1 == null) throw new NullPointerException("The \"bean1\" argument is null");
			if (bean2 == null) throw new NullPointerException("The \"bean2\" argument is null");
			if (!beanClass.isInstance(bean1)) throw new IllegalArgumentException("The \"bean1\" arg is not of type "+beanClass.getName()+" (it's "+bean1.getClass().getName()+")");
			if (!beanClass.isInstance(bean2)) throw new IllegalArgumentException("The \"bean2\" arg is not of type "+beanClass.getName()+" (it's "+bean2.getClass().getName()+")");
	
			for (Property property : properties) {
				Object v1 = getBeanValue(bean1,property);
				Object v2 = getBeanValue(bean2,property);
				if (!BeanValues.equalBeanValues(v1,v2,property)) return false;
			}
			return true;
    	} catch (Exception e) {
    		TranImpl.rollbackAndThrow(e);
    		throw new AssertionError("rollbackAndThrow() returned");
    	}
	}
    
    public Object[] getPrimaryKeyValues(B bean) throws RollbackException {
    	try {
			if (bean == null) throw new NullPointerException("The \"bean\" argument is null");
			if (!beanClass.isInstance(bean)) throw new IllegalArgumentException("The \"bean1\" arg is not of type "+beanClass.getName()+" (it's "+bean.getClass().getName()+")");
	
			Object[] values = new Object[primaryKeyProperties.length];
			for (int i=0; i<primaryKeyProperties.length; i++) {
				Property prop = primaryKeyProperties[i];
				values[i] = getBeanValue(bean,prop);
			}
			return values;
    	} catch (Exception e) {
    		TranImpl.rollbackAndThrow(e);
    		throw new AssertionError("rollbackAndThrow() returned");
    	}
    }
    
    // Non-public methods

    protected Object getBeanValue(Object bean, Property property) {
        Method getter = property.getGetter();
        try {
            return getter.invoke(bean);
        } catch (IllegalAccessException e) {
            throw new BeanFactoryException("IllegalAccessException when getting "+
                    property+" from bean="+bean,e);
        } catch (InvocationTargetException e) {
            throw new BeanFactoryException("InvocationTargetException when getting "+
                    property+" from bean="+bean,e);
        }
    }

    protected Object getMaxMinValueOfTrackedBeans(
    		Map<PrimaryKey<B>,BeanTrackerRec<B>> changedBeans,
    		Property prop,
    		MatchOp op) {
    	Object matchValue = null;
        for (BeanTrackerRec<B> rec : changedBeans.values()) {
        	Object value = getBeanValue(rec.getBean(),prop);
        	matchValue = matchMaxMin(prop,op,matchValue,value);
        }
        return matchValue;
    }

    protected B makeBean(Object[] dbValues) throws RollbackException {
        Object beanValues[] = BeanValues.makeBeanValues(properties,dbValues);
        Object[] priKeyBeanValues = new Object[primaryKeyInfo.getProperties().length];
        for (int i=0; i<priKeyBeanValues.length; i++) {
            priKeyBeanValues[i] = beanValues[i];
        }

        B bean = primaryKeyInfo.makeNewBean(priKeyBeanValues);

        for (int i=priKeyBeanValues.length; i<beanValues.length; i++) {
			Property prop = properties[i];
			Object beanValue = beanValues[i];
			setBeanValue(bean,prop,beanValue);
		}

		return bean;
	}

    protected Object[] makeDBValues(B bean) {
        Object[] dbValues = new Object[properties.length];
        for (int i=0; i<properties.length; i++) {
            Object beanValue = getBeanValue(bean,properties[i]);
            dbValues[i] = DBValues.makeDBValue(properties[i],beanValue);
        }
        return dbValues;
    }

    protected static Object makeNonModIfNonMod(Object value, Property prop) {
        if (value == null) return null;

        if (value instanceof Date) {
            Date d = (Date) value;
            if (prop.getBaseType() == NMDate.class) {
                return new NMDate(d.getTime());
            } else if (prop.getBaseType() == NMSQLDate.class) {
                return new NMSQLDate(d.getTime());
            } else if (prop.getBaseType() == NMTime.class) {
                return new NMTime(d.getTime());
            } else if (prop.getBaseType() == java.util.Date.class) {
                return new java.util.Date(d.getTime());
            }
        }

        if (value instanceof Integer && prop.getBaseType() == boolean.class) {
            int x = (Integer) value;
            return new Boolean(x!=0);
        }

        return value;
    }

    protected Object matchMaxMin(Property prop, MatchOp op, Object value1, Object value2) {
    	if (value1 == null) return value2;
    	if (value2 == null) return value1;

    	// We have two non-null values.  Must compare them.

    	if (op == MatchOp.MAX) {
        	if (DBValues.compareNonArrayNonNullDBValues(prop,value1,value2) > 0) {
        		return value1;
        	}
        	return value2;
    	}

    	if (op == MatchOp.MIN) {
        	if (DBValues.compareNonArrayNonNullDBValues(prop,value1,value2) < 0) {
        		return value1;
        	}
        	return value2;
    	}

    	throw new AssertionError("Invalid op: "+op);
    }

    @SuppressWarnings("unchecked")
	protected B[] newArray(int size) {
		Object array = java.lang.reflect.Array.newInstance(beanClass,size);
		return (B[]) array;
	}

    protected B newBean(Object[] priKeyDBValues) throws RollbackException {
        Object priKeyBeanValues[] = BeanValues.makeBeanValues(primaryKeyInfo.getProperties(),priKeyDBValues);
        B bean = primaryKeyInfo.makeNewBean(priKeyBeanValues);
        return bean;
    }


    protected void setBeanValue(B bean, Property property, Object value) {
		try {
			Method setter = property.getSetter();
			// System.out.println("bean="+bean.getClass().getSimpleName()+", prop="+property+", value="+value);
			setter.invoke(bean,value);
		} catch (IllegalAccessException e) {
			throw new BeanFactoryException("IllegalAccessException when setting "+
                    property+" to value="+value+" for bean="+bean,e);
		} catch (InvocationTargetException e) {
            throw new BeanFactoryException("InvocationTargetException when setting "+
                    property+" to value="+value+" for bean="+bean,e);
		}
	}

	protected B[] toArray(List<B> list) {
		B[] array = newArray(list.size());
		list.toArray(array);
		return array;
	}

    protected void validatePrimaryKeyValues(Object[] keyValues) throws RollbackException {
        // Note this method validates properties and types of the values, but does not clone them
        // Problems found cause IllegalArgumentException or NullPointerException to be thrown
        // All exceptions (including IllegalArgumentException and NullPointerException) are caught and
        // chained in RollbackException to ensure any active transaction for this thread is rolled back.

        try {
            if (keyValues == null) throw new NullPointerException("keyValues");

            Property[] p = primaryKeyInfo.getProperties();
            if (p.length != keyValues.length) {
                throw new IllegalArgumentException("Wrong number of key values: "+keyValues.length+" (should be "+p.length+")");
            }

            for (int i=0; i<p.length; i++) {
                if (keyValues[i] == null) {
                    throw new NullPointerException("Primary key value cannot be null: property="+p[i].getName());
                }

                if (!p[i].isInstance(keyValues[i])) {
                    throw new IllegalArgumentException("Key value for property "+p[i].getName()+" is not instance of "+p[i].getType()+".  Rather it is "+keyValues[i].getClass());
                }
            }
        } catch (Exception e) {
            TranImpl.rollbackAndThrow(e);
        }
    }
}

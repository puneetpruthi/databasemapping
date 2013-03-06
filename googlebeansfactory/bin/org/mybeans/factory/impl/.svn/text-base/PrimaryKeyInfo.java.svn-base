/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mybeans.factory.BeanFactoryException;


public class PrimaryKeyInfo<B> {
    public static <B> PrimaryKeyInfo<B> getInstance(Class<B> beanClass, String[] primaryKeyNames, AbstractFactory<?>[] referencedFactories) {
        String[] keyNames = primaryKeyNames.clone();
        Property[] priKeyProps = Property.derivePrimaryKeyProperties(beanClass,keyNames,referencedFactories);
        return new PrimaryKeyInfo<B>(beanClass,keyNames,priKeyProps);
    }

    private String[]       keyNames;
    private Property[]     properties;
    private Class<B>       beanClass;
	private Constructor<B> constructor;
	private Method[]       setters;

    private PrimaryKeyInfo(Class<B> beanClass, String[] primaryKeyNames, Property[] primaryKeyProperties) {
        keyNames = primaryKeyNames;
        properties = primaryKeyProperties;
		this.beanClass = beanClass;
		setupConstructor();
	}

    public String[] getKeyNames() {
        return keyNames;
    }

    public Object[] getPrimaryKeyDBValues(Object[] dbValues) {
        int len = properties.length;
        return DBValues.extractPrimaryKeyDBValues(len,dbValues);
    }

    public Property[] getProperties() {
        return properties;
    }

	public B makeNewBean(Object[] primaryKeyValues) {
		try {
			if (setters == null) return constructor.newInstance(primaryKeyValues);

			B answer = constructor.newInstance();
            for (int i=0; i<setters.length; i++) {
                setters[i].invoke(answer,primaryKeyValues[i]);
            }
			return answer;
		} catch (IllegalAccessException e) {
			throw new AssertionError(e);
		} catch (InstantiationException e) {
			throw new AssertionError(e);
		} catch (InvocationTargetException e) {
			throw new AssertionError(e);
		}
	}

	private void setupConstructor() {
        Class<?>[] types = new Class[properties.length];
        for (int i=0; i<properties.length; i++) {
            types[i] = properties[i].getType();
        }

		try {
			constructor = beanClass.getConstructor(types);
			setters = null;
			return;
		} catch (NoSuchMethodException e) {
			// Continue on to try finding a null constructor and the needed setters
		}

		try {
			constructor = beanClass.getConstructor();
        } catch (NoSuchMethodException e) {
        	StringBuffer error = new StringBuffer();
        	error.append("No usable public constructor for bean=");
        	error.append(beanClass);
        	error.append(" (need a ");
        	error.append(beanClass.getSimpleName());
        	error.append("() constructor or a ");
        	error.append(beanClass.getSimpleName());
        	error.append('(');
        	for (int i=0; i<types.length; i++) {
        		if (i>0) error.append(',');
        		error.append(types[i].getSimpleName());
        	}
        	error.append(") constructor to match the current database table)");
            throw new BeanFactoryException(error.toString());
        }

        setters = new Method[properties.length];
        for (int i=0; i<properties.length; i++) {
            String setterName = "set" +
            properties[i].getName().substring(0,1).toUpperCase() +
            properties[i].getName().substring(1);
            try{
                setters[i] = beanClass.getMethod(setterName,types[i]);
            } catch (NoSuchMethodException e) {
                throw new BeanFactoryException("Could not find a usable public constructor / setter combination for bean="+beanClass+" (no "+setterName+"() method)");
            }
        }
	}

	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("PrimaryKey(");
        for (int i=0; i<properties.length; i++) {
            if (i>0) b.append(',');
            b.append(properties[i].getName());
            b.append('(');
            b.append(properties[i].getType().getName());
            b.append(')');
        }
		b.append(')');
		return b.toString();
	}
}

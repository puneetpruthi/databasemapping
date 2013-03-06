/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

/**
 * The exception thrown by a <tt>BeanFactory</tt> indicating that a value loaded from a
 * database cannot be mapped to one of the valid Enumeration Constant Values for an
 * enumeration variable in a bean.
 * <p>
 * If this a subclass of <tt>RollbackException</tt>.
 * If a user initiated transaction was active when a <tt>BeanFactory</tt> method throws
 * <tt>RollbackException</tt> the transaction (by convention) is rolled back before throwing
 * <tt>RollbackException</tt>.
 * <p>
 * Methods are provided in this exception to obtain information about the existing bean and the
 * missing other bean to which the existing bean refers.
 */
public class EnumValueException extends RollbackException {
    private Class<?> beanClass;   // The bean class that contains the enumeration property
	private String   propertyName;
    private Class<?> propertyClass;
    private String   dbValue;

	public EnumValueException(
            String   message,
            Class<?> beanClass,
            String   propertyName,
            Class<?> propertyClass,
            String   dbValue) {
		super(message);
        this.beanClass  = beanClass;
        this.propertyName = propertyName;
        this.propertyClass  = propertyClass;
        this.dbValue = dbValue;
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public String getDbValue() {
		return dbValue;
	}

	public Class<?> getPropertyClass() {
		return propertyClass;
	}

	public String getPropertyName() {
		return propertyName;
	}
}

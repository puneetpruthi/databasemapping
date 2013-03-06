/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

/**
 * The exception thrown when a <tt>BeanTable</tt> encounters a problem, such as
 * not being able to access the database, not being able to find getter and setter
 * methods for a primary key, finding inconsistencies between an existing
 * database schema and the bean properties, etc.
 */
public class BeanFactoryException extends RuntimeException {
    public BeanFactoryException(String s) {
		super(s);
	}
	public BeanFactoryException(Throwable cause) {
		super(cause);
	}
    public BeanFactoryException(String message,Throwable cause) {
        super(message,cause);
    }
}

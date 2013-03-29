/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.forms;

/**
 * The exception thrown when a <tt>FormBeanFactory</tt> encounters a problem, such as
 * not being able to instantiate a bean or not being able to set one of the bean's
 * properties.
 */
public class FormBeanError extends RuntimeException {
    public FormBeanError(String s) {
		super(s);
	}
	public FormBeanError(Throwable cause) {
		super(cause);
	}
    public FormBeanError(String message,Throwable cause) {
        super(message,cause);
    }
}

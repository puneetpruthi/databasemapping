/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

/**
 * The exception thrown when a <tt>BeanFactory</tt> encounters a problem, such as
 * not being able to connect to the database, deadlock, when invalid parameters
 * are passed, etc.
 * <p>
 * If a user initiated transaction was active when a <tt>BeanFactory</tt> method throws
 * <tt>RollbackException</tt> the transaction (by convention) is rolled back before throwing
 * <tt>RollbackException</tt>.
 */
public class RollbackException extends Exception {
	public RollbackException(String message) {
		super(message);
	}

	public RollbackException(Exception e) {
		super(e);
	}

    public RollbackException(String message, Exception e) {
        super(message,e);
    }
}

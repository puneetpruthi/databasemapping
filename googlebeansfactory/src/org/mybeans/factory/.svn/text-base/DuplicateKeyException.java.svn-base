/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

/**
 * The exception thrown by a <tt>BeanFactory</tt> (typically from the <tt>create()</tt> method)
 * to signal an attempt to create a new bean in a table that has the same primary key as an
 * existing bean.  (The attempt has failed.)
 * <p>
 * If this a subclass of <tt>RollbackException</tt>.
 * If a user initiated transaction was active when a <tt>BeanFactory</tt> method throws
 * <tt>RollbackException</tt> the transaction (by convention) is rolled back before throwing
 * <tt>RollbackException</tt>.
 */
public class DuplicateKeyException extends RollbackException {
	public DuplicateKeyException(String message) {
		super(message);
	}
}

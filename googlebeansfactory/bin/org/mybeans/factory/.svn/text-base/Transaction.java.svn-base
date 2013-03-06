/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

import org.mybeans.factory.impl.TranImpl;

/**
 * This class is used to begin and end <code>BeanFactory</code> transactions.
 *
 * Transactions are associated with threads.
 * When <code>Transaction.begin()</code> is called a new transaction
 * is started for the current thread.
 * To successfully complete a transaction and save its work, call
 * <code>Transaction.commit()</code>.  To undo a transaction's work
 * call <code>Transaction.rollback()</code>.
 * <p>
 * All <code>BeanFactory</code> methods that perform their work on behalf of
 * a transaction should throw <code>RollbackException</code> if there is a
 * problem performing the work.
 * By convention, the current thread's transaction is rolled back before throwing
 * <code>RollbackException</code>.  The reason for rolling back the transaction is
 * described in the message.
 * Should the transaction have been rolled back due to some underlying exception,
 * <code>RollbackException</code> will be thrown as a chained exception with the
 * underlying exception as its cause.
 * <p>
 * Because transactions can hold locks in the underlying databases use by
 * <code>BeanFactory</code>s, it is recommended that user interactions not
 * occur during a transaction.  For similar reasons, a method that begins
 * a transaction should ensure that all avenues of departure from the method
 * either commit or roll back the transaction
 * A generic way to handle this would be:
 * <p><blockquote><pre>
 *     try {
 *         Transaction.begin();
 *         // Application logic, including calls to BeanFactories
 *         Transaction.commit();
 *     } catch (...) {
 *         ...
 *     } finally {
 *         if (Transaction.isActive()) Transaction.rollback();
 *         ...
 *     }
 * </pre></blockquote><p>
 *
 */
public class Transaction {
	private Transaction() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Begins a new transaction for this thread.
	 * @throws RollbackException if there is some reason the transaction could not be started.
	 * One reason is if you are already in a transaction.
	 */
	public static void begin() throws RollbackException {
		TranImpl.begin();
	}

	/**
	 * Commits the work performed by this thread's currently running transaction.
	 * @throws RollbackException if there is some reason the transaction could not be committed.
	 */
	public static void commit() throws RollbackException {
        TranImpl.commit();
	}

	/**
	 * Tests whether a transaction is currently running for this thread.
	 * @return true if this thread is in a transaction.
	 */
	public static boolean isActive() {
        return TranImpl.isActive();
	}

	/**
	 * Causes the work performed by the current thread's currently running transaction to be undone.
	 * @throws AssertionError if not in a transaction.
	 */
	public static void rollback() {
        TranImpl.rollback();
	}
}

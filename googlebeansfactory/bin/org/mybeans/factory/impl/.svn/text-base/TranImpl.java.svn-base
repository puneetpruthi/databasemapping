/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;


import java.util.ArrayList;

import org.mybeans.factory.RollbackException;


public class TranImpl {
    private static ThreadLocal<TranImpl> myTran = new ThreadLocal<TranImpl>();

    public static void begin() throws RollbackException {
        TranImpl t = myTran.get();
        if (t != null) rollbackAndThrow("Cannot begin twice without commit or rollback (i.e., you were already in a transaction)!");
        myTran.set(new TranImpl());
    }

    public static void commit() throws RollbackException {
        TranImpl t = myTran.get();
        if (t == null) rollbackAndThrow("Not in a transaction");
        t.executeCommit();
    }

    public static boolean isActive() {
        return myTran.get() != null;
    }

    public static void rollback() {
        TranImpl t = myTran.get();
        if (t == null) throw new AssertionError("Not in a transaction");
        t.executeRollback();
    }

    static void rollbackAndThrow(String message) throws RollbackException {
        rollbackAndThrow(new RollbackException(message));
    }

    static void rollbackAndThrow(Exception e) throws RollbackException {
        TranImpl t = myTran.get();
        if (t != null) t.executeRollback();
        if (e instanceof RollbackException) throw (RollbackException) e;
        throw new RollbackException(e);
    }

	static void join(OutcomeListener listener) throws RollbackException {
		TranImpl t = myTran.get();
		if (t == null) throw new RollbackException("Must be in a transaction.");
		t.involvedFactories.add(listener);
	}

    private ArrayList<OutcomeListener> involvedFactories = new ArrayList<OutcomeListener>();

	private TranImpl() {
        /* Private constructor forces use of static factory (TranImpl.begin()) */
    }

	private void executeCommit() throws RollbackException {
		for (OutcomeListener listener : involvedFactories) {
			listener.prepare();
		}

		for (OutcomeListener listener : involvedFactories) {
			listener.commit();
		}

		myTran.set(null);
	}

	private void executeRollback() {
		for (OutcomeListener listener : involvedFactories) {
			listener.rollback();
		}
		myTran.set(null);
	}
}

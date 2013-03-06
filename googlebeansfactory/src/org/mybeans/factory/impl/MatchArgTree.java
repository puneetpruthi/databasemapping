/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.util.Iterator;

import org.mybeans.factory.MatchArg;
import org.mybeans.factory.RollbackException;

public abstract class MatchArgTree {

	/*
     * Note this method validates properties and types of the values, but does not clone them
     * Problems found cause IllegalArgumentException or NullPointerException to be thrown.
     * All exceptions (including IllegalArgumentException and NullPointerException) are caught and
     * chained in RollbackException to ensure any active transaction for this thread is rolled back.
     *
     * Checks for the following:
     *     No array properties (but byte[] is not checked as it's allowed for EQUALS)
     *
     *     For binary operators:
     *         * no null match values for primary key properties
     *         * match value must be of same type as property
     *         * no null values for non-nullable fields
     *
     */

	public static MatchArgTree buildTree(Property[] allBeanProperties, MatchArg constraint) throws RollbackException {
        try {
            if (constraint == null) throw new NullPointerException("constraint cannot be null)");

        	if (constraint instanceof UnaryMatchArg) {
        		UnaryMatchArg arg = (UnaryMatchArg) constraint;
        		return new MatchArgLeafNode(allBeanProperties,arg);
        	}
        	
        	if (constraint instanceof BinaryMatchArg) {
        		BinaryMatchArg arg = (BinaryMatchArg) constraint;
        		return new MatchArgLeafNode(allBeanProperties,arg);
        	}
        	
    		LogicMatchArg arg = (LogicMatchArg) constraint;
    		return new MatchArgInternalNode(allBeanProperties,arg);
        } catch (Exception e) {
        	if (e instanceof RollbackException) throw (RollbackException) e;
            TranImpl.rollbackAndThrow(e);
            throw new AssertionError("executeRollback returned");
        }
	}
	
	protected MatchOp  op = null;  // We'll always have an op
    
    public MatchArgTree(MatchOp op) {
    	this.op = op;
    }

    public MatchOp getOp() { return op; }

    public abstract boolean containsNonPrimaryKeyProps();
    public abstract boolean containsMaxOrMin();
    
    public abstract Property[] getProperties();
    public abstract Object[]   getValues();
    
    public abstract Iterator<MatchArgLeafNode> leafIterator();

    public abstract boolean satisfied(Object[] dbValues) throws RollbackException;
}

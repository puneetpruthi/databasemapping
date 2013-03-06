/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

import org.mybeans.factory.impl.BinaryMatchArg;
import org.mybeans.factory.impl.LogicMatchArg;
import org.mybeans.factory.impl.MatchOp;
import org.mybeans.factory.impl.UnaryMatchArg;

/**
 * A class to specify contraints when matching beans.  Use with the <tt>BeanFactory.match()</tt> method.
 * <p>
 * For example:
 * <p><blockquote><pre>
 *     User[] array = userFactory.match(MatchArg.equals("password","testing"));
 * </pre></blockquote>
 * would return all users with password equal to testing.
 */
public abstract class MatchArg {
	protected MatchArg() {}
	
	protected abstract MatchOp getOp();

    public static MatchArg and(MatchArg...constraints) {
    	return new LogicMatchArg(MatchOp.AND,constraints);
    }

    public static MatchArg contains(String keyName, String s) {
    	return new BinaryMatchArg(keyName,MatchOp.CONTAINS,s);
    }

    public static MatchArg containsIgnoreCase(String keyName, String s) {
    	return new BinaryMatchArg(keyName,MatchOp.CONTAINS_IGNORE_CASE,s);
    }

    public static MatchArg endsWith(String keyName, String ending) {
    	return new BinaryMatchArg(keyName,MatchOp.ENDS_WITH,ending);
    }

    public static MatchArg endsWithIgnoreCase(String keyName, String ending) {
    	return new BinaryMatchArg(keyName,MatchOp.ENDS_WITH_IGNORE_CASE,ending);
    }

    public static MatchArg equals(String keyName, Object matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.EQUALS,matchValue);
    }

    public static MatchArg equalsIgnoreCase(String keyName, String matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.EQUALS_IGNORE_CASE,matchValue);
    }

    public static MatchArg greaterThan(String keyName, Object matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.GREATER,matchValue);
    }

    public static MatchArg greaterThanOrEqualTo(String keyName, Object matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.GREATER_OR_EQUALS,matchValue);
    }

    public static MatchArg lessThan(String keyName, Object matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.LESS,matchValue);
    }

    public static MatchArg lessThanOrEqualTo(String keyName, Object matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.LESS_OR_EQUALS,matchValue);
    }

    public static MatchArg max(String keyName) {
    	return new UnaryMatchArg(keyName,MatchOp.MAX);
    }

    public static MatchArg min(String keyName) {
    	return new UnaryMatchArg(keyName,MatchOp.MIN);
    }
    
    public static MatchArg notEquals(String keyName, Object matchValue) {
    	return new BinaryMatchArg(keyName,MatchOp.NOT_EQUALS,matchValue);
    }

    public static MatchArg or(MatchArg...constraints) {
    	return new LogicMatchArg(MatchOp.OR,constraints);
    }

    public static MatchArg startsWith(String keyName, String beginning) {
    	return new BinaryMatchArg(keyName,MatchOp.STARTS_WITH,beginning);
    }

    public static MatchArg startsWithIgnoreCase(String keyName, String beginning) {
    	return new BinaryMatchArg(keyName,MatchOp.STARTS_WITH_IGNORE_CASE,beginning);
    }
}

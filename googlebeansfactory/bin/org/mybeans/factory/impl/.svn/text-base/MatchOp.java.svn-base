/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

public enum MatchOp {
    // Valid for comparing any types, except arrays (byte[] is okay)
        EQUALS,
        NOT_EQUALS,

    // Valid for comparing numbers, Dates, or Strings
        GREATER,
        GREATER_OR_EQUALS,
        LESS,
        LESS_OR_EQUALS,

    // Valid for matching String properties, only
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        EQUALS_IGNORE_CASE,
        CONTAINS_IGNORE_CASE,
        STARTS_WITH_IGNORE_CASE,
        ENDS_WITH_IGNORE_CASE,

    // Valid for matching the max/min values of numbers, Dates or Strings
        MAX,
        MIN,
        
    // Logical ops valid only for combining other ops
        OR,
        AND;


    public String toString() {
        switch (this) {
        	case AND:                     return getClass().getSimpleName()+".AND" ;
	        case CONTAINS:                return getClass().getSimpleName()+".CONTAINS";
	        case CONTAINS_IGNORE_CASE:    return getClass().getSimpleName()+".CONTAINS_IGNORE_CASE";
	        case ENDS_WITH:				  return getClass().getSimpleName()+".ENDS_WITH";
	        case ENDS_WITH_IGNORE_CASE:   return getClass().getSimpleName()+".ENDS_WITH_IGNORE_CASE";
	        case EQUALS:                  return getClass().getSimpleName()+".EQUALS";
	        case EQUALS_IGNORE_CASE:      return getClass().getSimpleName()+".EQUALS_IGNORE_CASE";
	        case GREATER:                 return getClass().getSimpleName()+".GREATER";
	        case GREATER_OR_EQUALS:       return getClass().getSimpleName()+".GREATER_OR_EQUALS";
	        case LESS:                    return getClass().getSimpleName()+".LESS";
	        case LESS_OR_EQUALS:          return getClass().getSimpleName()+".LESS_OR_EQUALS";
	        case MAX:                     return getClass().getSimpleName()+".MAX";
	        case MIN:                     return getClass().getSimpleName()+".MIN";
        	case OR:                      return getClass().getSimpleName()+".OR" ;
	        case STARTS_WITH:             return getClass().getSimpleName()+".STARTS_WITH";
	        case STARTS_WITH_IGNORE_CASE: return getClass().getSimpleName()+".STARTS_WITH_IGNORE_CASE";
	        default:                      throw new AssertionError(this);
        }
    }
}

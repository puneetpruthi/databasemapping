/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.mybeans.factory.RollbackException;


public class MatchArgLeafNode extends MatchArgTree {
	private Property property;
	private Object   matchValue    = null;
	
	
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
    public MatchArgLeafNode(Property[] allBeanProperties, UnaryMatchArg arg) {
    	super(arg.getOp());
    	
        // propertyForName throws IllegalArgumentException if the property name is not valid
        property = Property.propertyForName(allBeanProperties,arg.getKeyName());

        if (property.isArray()) throw new IllegalArgumentException("Array properties cannot be match contraints: "+property);
        
		// Valid for matching the max/min values of numbers, Dates or Strings
		
		if (!isNumber() && !isDate() && !isString()) {
    		throw new IllegalArgumentException(op+" cannot be applied to this property type: "+property);
		}
		
		// Note: no matchingTypeCheck as no value for unary ops
    }
    
    public MatchArgLeafNode(Property[] allBeanProperties, BinaryMatchArg arg) {
    	super(arg.getOp());

    	// propertyForName throws IllegalArgumentException if the property name is not valid
        property = Property.propertyForName(allBeanProperties,arg.getKeyName());

        matchValue = arg.getKeyValue();

        if (property.isArray()) throw new IllegalArgumentException("Array properties cannot be match contraints: "+property);

		matchingTypeCheck();
    
        switch (op) {
        	case EQUALS:
        	    // Valid for comparing any types, except arrays (byte[] is okay)
        		break;
        	case GREATER:
        	case GREATER_OR_EQUALS:
        	case LESS:
        	case LESS_OR_EQUALS:
                // Valid for comparing numbers, Dates, or Strings
        		if (!isNumber() && !isDate() && !isString()) {
            		throw new IllegalArgumentException(op+" cannot be applied to this property type: "+property);
        		}
        		break;
        	case CONTAINS:
        	case STARTS_WITH:
        	case ENDS_WITH:
        	case EQUALS_IGNORE_CASE:
        	case CONTAINS_IGNORE_CASE:
        	case STARTS_WITH_IGNORE_CASE:
        	case ENDS_WITH_IGNORE_CASE:
                // Valid for matching String properties, only
        		if (!isString()) {
            		throw new IllegalArgumentException(op+" cannot be applied to this property type: "+property);
        		}
        		break;
        	default:
        		throw new AssertionError("Unknown op: "+op);
        }
    }
    
    public void fixConstraint(MatchOp newOp, Object newValue) {
    	op = newOp;
    	matchValue = newValue;
    }
    
	public Property   getProperty()   { return property; }
	public Property[] getProperties() { return new Property[] { property }; }
	public Object     getValue()      { return matchValue;    }
	public Object[]   getValues()     { return new Object[]   { matchValue    }; }
	
    public Iterator<MatchArgLeafNode> leafIterator() {
    	return new MyLeafIterator(this);
    }
 
    public boolean containsMaxOrMin() {
    	if (op == MatchOp.MAX) return true;
    	if (op == MatchOp.MIN) return true;
    	return false;
    }

    public boolean containsNonPrimaryKeyProps() {
    	return !property.isPrimaryKeyProperty();
    }

    private boolean isDate() {
    	Class<?> c = property.getBaseType();
    	if (c == java.util.Date.class) return true;
    	if (c == java.sql.Date.class)  return true;
    	if (c == java.sql.Time.class)  return true;
    	if (c == org.mybeans.nonmodifiable.NMDate.class) return true;
    	if (c == org.mybeans.nonmodifiable.NMTime.class) return true;
    	if (c == org.mybeans.nonmodifiable.NMSQLDate.class) return true;
    	return false;
    }

    private boolean isNumber() {
    	Class<?> c = property.getBaseType();
    	if (c == float.class)  return true;
    	if (c == int.class)    return true;
    	if (c == double.class) return true;
    	if (c == long.class)   return true;
    	return false;
    }

    private boolean isString() {
    	return property.getBaseType() == String.class;
    }

    private void matchingTypeCheck() {
        if (property.isPrimaryKeyProperty() && matchValue == null) throw new IllegalArgumentException("Primary key constraint value cannot be null: property="+property.getName());
        if (matchValue != null && !property.isInstance(matchValue)) throw new IllegalArgumentException("Constraint value for property "+property.getName()+" is not instance of "+property.getType()+".  Rather it is "+matchValue.getClass());
        if (matchValue == null && !property.isNullable()) throw new IllegalArgumentException("Constraint value for property "+property.getName()+" cannot be null");
    }

    public boolean satisfied(Object[] dbValues) throws RollbackException {
        Object dbValue  = dbValues[property.getPropertyNum()];
        
        // System.out.println("MatchArgLeafNode.satisfied: prop="+property+", op="+op+", matchValue="+matchValue+", dbValue="+dbValue);

        if (property.isArray()) TranImpl.rollbackAndThrow("Assertion Error: check for array should have already occurred: "+property);
        
        if (op == null) return false;

        switch (op) {
            case EQUALS:
                Object keyDBValue = DBValues.makeDBValue(property,matchValue);
                return DBValues.equalNonArrayDBValues(property,dbValue,keyDBValue);
            case GREATER:
            case GREATER_OR_EQUALS:
            case LESS:
            case LESS_OR_EQUALS:
                if (matchValue == null) TranImpl.rollbackAndThrow(op+" cannot have a matchValue==NULL");
                if (property instanceof ReferencedBeanProperty || property.getBaseType() == byte[].class ||
                        property.getBaseType() == boolean.class) TranImpl.rollbackAndThrow(op+" cannot be applied to this property type: "+property);
                if (dbValue == null) return false;
                switch (op) {
                	case GREATER:
                		return DBValues.compareNonArrayNonNullDBValues(property,dbValue,matchValue) > 0;
                	case GREATER_OR_EQUALS:
                		return DBValues.compareNonArrayNonNullDBValues(property,dbValue,matchValue) >= 0;
                	case LESS:
                		return DBValues.compareNonArrayNonNullDBValues(property,dbValue,matchValue) < 0;
                	case LESS_OR_EQUALS:
                		return DBValues.compareNonArrayNonNullDBValues(property,dbValue,matchValue) <= 0;
                	default:
                		throw new AssertionError(op);
                }
            case CONTAINS:
            case STARTS_WITH:
            case ENDS_WITH:
            case EQUALS_IGNORE_CASE:
            case CONTAINS_IGNORE_CASE:
            case STARTS_WITH_IGNORE_CASE:
            case ENDS_WITH_IGNORE_CASE:
                if (property.getBaseType() != String.class) throw new RollbackException(op+" is only valid on Strings");
                if (matchValue == null) throw new RollbackException(op+" cannot have a matchValue==NULL");
                if (dbValue == null) return false;
                String a = (String) dbValue;
                String b = (String) matchValue;
                switch (op) {
                    case CONTAINS:                return a.contains(b);
                    case STARTS_WITH:             return a.startsWith(b);
                    case ENDS_WITH:               return a.endsWith(b);
                    case EQUALS_IGNORE_CASE:      return a.equalsIgnoreCase(b);
                    case CONTAINS_IGNORE_CASE:    return a.toLowerCase().contains(b.toLowerCase());
                    case STARTS_WITH_IGNORE_CASE: return a.toLowerCase().startsWith(b.toLowerCase());
                    case ENDS_WITH_IGNORE_CASE:   return a.toLowerCase().endsWith(b.toLowerCase());
                    default:                      throw new AssertionError(op);
                }
            default:
                TranImpl.rollbackAndThrow("Unknown op: "+op);
                throw new AssertionError("executeRollback returned");
        }
    }
    
    private static class MyLeafIterator implements Iterator<MatchArgLeafNode> {
    	private MatchArgLeafNode node;
    	
    	public MyLeafIterator(MatchArgLeafNode node) {
    		this.node = node;
    	}
    	
    	public boolean hasNext() {
    		return node != null;
    	}
    	
    	public MatchArgLeafNode next() {
    		if (node == null) throw new NoSuchElementException();
    		MatchArgLeafNode answer = node;
    		node = null;
    		return answer;
    	}
    	
    	public void remove() {
    		throw new UnsupportedOperationException();
    	}
    }
}

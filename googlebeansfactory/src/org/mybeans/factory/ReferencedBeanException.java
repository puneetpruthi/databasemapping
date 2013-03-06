/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

/**
 * The exception thrown by a <tt>BeanFactory</tt> indicating that it cannot instantiate
 * a bean because it refers to another bean (using that other bean's primary key) that does
 * not exist in the other bean's table.
 * <p>
 * If this a subclass of <tt>RollbackException</tt>.
 * If a user initiated transaction was active when a <tt>BeanFactory</tt> method throws
 * <tt>RollbackException</tt> the transaction (by convention) is rolled back before throwing
 * <tt>RollbackException</tt>.
 * <p>
 * Methods are provided in this exception to obtain information about the existing bean and the
 * missing other bean to which the existing bean refers.
 */
public class ReferencedBeanException extends RollbackException {
    private Class<?> referringBeanClass;
    private Class<?> referencedBeanClass;
    private Object[] referringBeanPrimaryKeyValues;
    private Object[] referencedBeanPrimaryKeyValues;

	public ReferencedBeanException(
            String   message,
            Class<?> referringBeanClass,
            Object[] referringBeanPrimaryKeyValues,
            Class<?> referencedBeanClass,
            Object[] refererencedBeanPrimaryKeyValues) {
		super(message);
        this.referringBeanClass  = referringBeanClass;
        this.referencedBeanClass = referencedBeanClass;
        this.referringBeanPrimaryKeyValues  = referringBeanPrimaryKeyValues;
        this.referencedBeanPrimaryKeyValues = refererencedBeanPrimaryKeyValues;
	}

    public Class<?> getReferencedBeanClass() { return referencedBeanClass; }
    public Class<?> getReferringBeanClass()  { return referringBeanClass;  }

    public Object[] getReferencedBeanPrimaryKeyValues() { return referencedBeanPrimaryKeyValues; }
    public Object[] getReferringBeanPrimaryKeyValues()  { return referringBeanPrimaryKeyValues; }
}

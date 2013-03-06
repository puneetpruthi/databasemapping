/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

public class OtherReferencedBeanProperty extends ReferencedBeanProperty {
	private AbstractFactory<?> refFactory;

	public OtherReferencedBeanProperty(String name, Class<?> baseType, Class<?> type, Class<?> beanClass, boolean isPrimaryKeyProperty, AbstractFactory<?> refFactory) {
		super(name,baseType,type,isPrimaryKeyProperty,beanClass,refFactory.primaryKeyProperties);
		this.refFactory = refFactory;
	}

    public AbstractFactory<?> getFactory() { return refFactory; }
}

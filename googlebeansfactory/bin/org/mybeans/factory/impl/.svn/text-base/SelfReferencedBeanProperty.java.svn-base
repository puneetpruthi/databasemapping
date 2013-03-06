/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

public class SelfReferencedBeanProperty extends ReferencedBeanProperty {
	private AbstractFactory<?> refFactory = null;

	public SelfReferencedBeanProperty(String name, Class<?> baseType, Class<?> type, Class<?> beanClass, Property[] primaryKeyProps) {
		super(name,baseType,type,false,beanClass,primaryKeyProps);
	}

    public AbstractFactory<?> getFactory() {
    	if (refFactory == null) throw new AssertionError("factory wasn't set yet");
    	return refFactory;
    }
    
    public void setFactory(AbstractFactory<?> f) { refFactory = f; }
}

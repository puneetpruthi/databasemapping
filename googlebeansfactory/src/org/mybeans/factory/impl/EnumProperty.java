/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.util.HashMap;
import java.util.Map;

import org.mybeans.factory.EnumValueException;

public class EnumProperty extends Property {
	private Class<?> beanClass;
	private Map<String,Object> enumConstantsMap = new HashMap<String,Object>();


	public EnumProperty(String name, Class<?> baseType, Class<?> type, Class<?> beanClass) {
		super(name,baseType,type,false,beanClass);
		
		this.beanClass = beanClass;
		
		for (Object enumConstant : baseType.getEnumConstants()) {
			enumConstantsMap.put(makeDBValue(enumConstant),enumConstant);
		}
	}
	
	public String makeDBValue(Object beanValue) {
		return beanValue.toString();
	}
	
	public Object makeBeanValue(String dbValue) throws EnumValueException {
		Object beanValue = enumConstantsMap.get(dbValue);
		
		if (beanValue == null) {
			throw new EnumValueException(
							"Cannot map dbValue to enum constant: dbValue="+dbValue+", prop="+getName()+", type="+getType(),
							beanClass,getName(),getType(),dbValue);
		}
		
		return beanValue;
	}
}

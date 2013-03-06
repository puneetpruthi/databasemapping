/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.util.ArrayList;
import java.util.Arrays;

import org.mybeans.factory.BeanFactoryException;

public abstract class ReferencedBeanProperty extends Property {
	private Property[] refBeanPrimaryBeanProperties;
	
    private String[]   columnNames;
    private Class<?>[] columnTypes;
    private int[]      columnStrLens;

	public ReferencedBeanProperty(String     name,
								  Class<?>   baseType,
								  Class<?>   type,
								  boolean    isPrimaryKeyProperty,
								  Class<?>   beanClass,
								  Property[] refBeanPrimaryKeyProperties) {
		super(name,baseType,type,isPrimaryKeyProperty,beanClass);

		if (refBeanPrimaryKeyProperties.length == 0) {
			throw new BeanFactoryException("Cannot have a referenced bean with no primary key properties: property="+name+", type="+baseType);
		}

		this.refBeanPrimaryBeanProperties = refBeanPrimaryKeyProperties;
		
        columnNames   = computeColumnNames();
        columnTypes   = computeColumnTypes();
        columnStrLens = computeColumnMaxStrLens();
	}
    
	public int[]      getColumnMaxStrLens() { return columnStrLens; }
    public String[]   getColumnNames()      { return columnNames;   }
    public Class<?>[] getColumnTypes()      { return columnTypes;   }

    public abstract AbstractFactory<?> getFactory();
    
    public Property[] getRefBeanPrimaryKeyProperties() { return refBeanPrimaryBeanProperties; }
	
	private int[] computeColumnMaxStrLens() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (Property prop : refBeanPrimaryBeanProperties) {
        	for (int colMaxStrLen : prop.getColumnMaxStrLens()) {
        		list.add(colMaxStrLen);
        	}
        }

        int[] answer = new int[list.size()];
        for (int i=0; i<answer.length; i++) {
        	answer[i] = list.get(i);
        }
        
        return answer;
	}

    private String[] computeColumnNames() {
        ArrayList<String> suffixList = new ArrayList<String>();
        for (Property prop : refBeanPrimaryBeanProperties) {
            if (prop instanceof ReferencedBeanProperty) {
                ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
                suffixList.addAll(Arrays.asList(refProp.getColumnNames()));
            } else {
                suffixList.add(prop.getName());
            }
        }

        if (suffixList.size() == 1) {
            return new String[] {getName()};
        }

        ArrayList<String> nameList = new ArrayList<String>();
        for (String suffix : suffixList) {
            nameList.add(getName() + META_SEPARATOR + suffix);
        }
        String[] answer = new String[nameList.size()];
        nameList.toArray(answer);
        return answer;
    }

    private Class<?>[] computeColumnTypes() {
        ArrayList<Class<?>> answer = new ArrayList<Class<?>>();
        for (Property prop : refBeanPrimaryBeanProperties) {
            if (prop instanceof ReferencedBeanProperty) {
                ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
                answer.addAll(Arrays.asList(refProp.getColumnTypes()));
            } else {
                answer.add(prop.getType());
            }
        }

        return answer.toArray(new Class[answer.size()]);
    }

	public String toString() {
		StringBuffer b = new StringBuffer();
        b.append(this.getClass().getSimpleName());
        b.append("(");
        if (isPrimaryKeyProperty()) b.append("PriKey,");
        b.append("name=");
		b.append(getName());
		b.append(", type=");
		b.append(getBaseType().getName());
        if (isArray()) b.append("[]");
		b.append(", referenced using (");
		boolean firstProp = true;
		for (Property p : getRefBeanPrimaryKeyProperties()) {
			if (firstProp) {
				firstProp = false;
			} else {
				b.append(',');
			}
			b.append(p);
		}
		b.append(')');
		return b.toString();
	}
}

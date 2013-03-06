/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.mybeans.factory.BeanFactoryException;



public class Property implements Comparable<Property> {
    public static final String META_SEPARATOR = "__";  // two underscore characters

    private static final Integer INTEGER_ZERO = new Integer(0);
    private static final Long    LONG_ZERO    = new Long(0);
    private static final Float   FLOAT_ZERO   = new Float(0);
    private static final Double  DOUBLE_ZERO  = new Double(0);

    private static Class<?> baseClassForName(String s) {
        if (s.equals("java.lang.String"))  return String.class;

        if (s.equals("boolean"))        return boolean.class;
        if (s.equals("byte[]"))         return byte[].class;
        if (s.equals("int"))            return int.class;
        if (s.equals("long"))           return long.class;
        if (s.equals("double"))         return double.class;
        if (s.equals("float"))          return float.class;

        if (s.equals("java.sql.Date"))  return java.sql.Date.class;
        if (s.equals("java.util.Date")) return java.util.Date.class;
        if (s.equals("java.sql.Time"))  return java.sql.Time.class;

        if (s.equals(org.mybeans.nonmodifiable.NMDate.class.getName())) return org.mybeans.nonmodifiable.NMDate.class;
        if (s.equals(org.mybeans.nonmodifiable.NMTime.class.getName())) return org.mybeans.nonmodifiable.NMTime.class;
        if (s.equals(org.mybeans.nonmodifiable.NMSQLDate.class.getName())) return org.mybeans.nonmodifiable.NMSQLDate.class;

        return null;
    }

    private static void checkForDuplicateProperties(Property[] property) {
        for (int i=0; i<property.length; i++) {
            if (getPropertyNum(property,property[i].getName()) != i) {
                throw new BeanFactoryException("Duplicate property names: "+
                        property[getPropertyNum(property,property[i].getName())]+
                        " and "+property[i]);
            }
        }
    }

    public static Property[] derivePrimaryKeyProperties(Class<?>          beanClass,
	                                                    String[]          primaryKeyNames,
                                                        AbstractFactory<?>[] referencedFactories)
    {
        Property[] props = new Property[primaryKeyNames.length];
        for (int i=0; i<primaryKeyNames.length; i++) {
            String capName = primaryKeyNames[i].substring(0,1).toUpperCase() + primaryKeyNames[i].substring(1);
            try {
                Method getter = beanClass.getMethod("get"+capName);
                Class<?>  propType = getter.getReturnType();
                if (propType == void.class) throw new BeanFactoryException("Invalid return type (void) for primary key's getter: "+getter);
                if (propType.isArray() && propType != byte[].class) throw new BeanFactoryException("Primary key property cannot be an array: property="+primaryKeyNames[i]+", type="+propType.getCanonicalName());
                props[i] = getInstance(primaryKeyNames[i],propType,beanClass,null,referencedFactories);
                props[i].setPropertyNum(i);
            } catch (NoSuchMethodException e) {
                throw new BeanFactoryException("Could not find getter method for primary key: get"+capName+"()");
            }
        }

        return props;
    }

    public static <T> Property[] deriveProperties(Class<T> beanClass,
	                                              PrimaryKeyInfo<T> primaryKey,
	                                              AbstractFactory<?>[] referencedFactories)
    {
        Property[] primaryKeyProps = primaryKey.getProperties();

        ArrayList<Property> list = new ArrayList<Property>();
        list.addAll(Arrays.asList(primaryKeyProps));

        Method[] methods = beanClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            Class<?>  propType = method.getReturnType();
            Property newProp = null;
            if (methodName.length() > 2 && methodName.startsWith("is") && propType == boolean.class) {
                String propName = methodName.substring(2,3).toLowerCase() + methodName.substring(3);
                newProp = deriveProperty(propName,propType,beanClass,primaryKeyProps,referencedFactories);
            } else if (methodName.length() > 3 && methodName.startsWith("get")) {
                String propName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
                newProp = deriveProperty(propName,propType,beanClass,primaryKeyProps,referencedFactories);
            } else {
            	newProp = null;
            }
            if (newProp != null) list.add(newProp);
        }

        Property[] answer = new Property[list.size()];
        list.toArray(answer);
        Arrays.sort(answer);
        for (int i=0; i<answer.length; i++) answer[i].setPropertyNum(i);
        checkForDuplicateProperties(answer);
        return answer;
    }
    
    private static Property deriveProperty(
    		String propName,
    		Class<?> propType,
    		Class<?> beanClass,
    		Property[] primaryKeyProps,
    		AbstractFactory<?>[] referencedFactories)
    {
        if (propType == void.class) {
        	// Props cannot have null type
        	return null;
        }
        
        if (getPropertyNum(primaryKeyProps,propName) >= 0) {
        	// Primary key properties have already been derived
        	return null;
        }

        String setterName = "set"+propName.substring(0,1).toUpperCase()+propName.substring(1);
        try {
            beanClass.getMethod(setterName,propType);
            return getInstance(propName,propType,beanClass,primaryKeyProps,referencedFactories);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    // Pass primaryKeyProperties only when getting non-primary key properties
    // This can be done because we already know the primary key properties when we're looking for
    // the non-primary key properties.
    private static Property getInstance(String     propertyName,
                                        Class<?>   type,
                                        Class<?>   beanClass,
                                        Property[] primaryKeyProps,
                                        AbstractFactory<?>[] referencedFactories)
    {
    	boolean isPrimaryKeyProperty = (primaryKeyProps == null);
    	
        if (type == byte[].class) {
            return new Property(propertyName,type,type,isPrimaryKeyProperty,beanClass);
        }

        Class<?> baseType = type;
        if (type.isArray()) {
            if (isPrimaryKeyProperty) throw new BeanFactoryException("Primary key property cannot be an array: property="+propertyName+", type="+type.getCanonicalName());
            if (type.getName().charAt(1)=='[') throw new BeanFactoryException("Cannot map multi-dimensional arrays: property="+propertyName+", type="+type.getCanonicalName());
            baseType = type.getComponentType();
        }

        if (baseClassForName(baseType.getName()) != null) {
            return new Property(propertyName,baseType,type,isPrimaryKeyProperty,beanClass);
        }
        
        if (baseType.isEnum()) {
            if (isPrimaryKeyProperty) throw new BeanFactoryException("Primary key property cannot be an enum: property="+propertyName+", type="+type.getCanonicalName());
        	return new EnumProperty(propertyName,baseType,type,beanClass);
        }
        
        if (baseType == beanClass) {
        	if (isPrimaryKeyProperty) throw new BeanFactoryException("Primary key property cannot contain can properties that reference themselves: property="+propertyName+", type="+type.getCanonicalName());
        	return new SelfReferencedBeanProperty(propertyName,baseType,type,beanClass,primaryKeyProps);
        }

        for (AbstractFactory<?> f : referencedFactories) {
            if (f.beanClass == baseType) {
                return new OtherReferencedBeanProperty(propertyName,baseType,type,beanClass,isPrimaryKeyProperty,f);
            }
        }

        throw new BeanFactoryException("Cannot map this class type: "+type.getCanonicalName()+" (property name: "+propertyName+").  Was a referencedFactory provided for this type?");
    }

    private static int getPropertyNum(Property[] properties, String name) {
        for (int i=0; i<properties.length; i++) {
            if (properties[i].getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public static Property[] propertiesForNames(Property[] allProps, String[] propertyNames) {
        Property[] answer = new Property[propertyNames.length];
        for (int i=0; i<propertyNames.length; i++) {
            answer[i] = propertyForName(allProps,propertyNames[i]);
        }
        return answer;
    }

    public static Property propertyForName(Property[] allProps, String propertyName) {
        int num = getPropertyNum(allProps,propertyName);
        if (num != -1) return allProps[num];
        throw new IllegalArgumentException("No such property: "+propertyName);
    }

	private String     name;
	private boolean    array;
	private Class<?>   baseType;
	private Method     getter;
	private Method     setter;
    private Class<?>   type;
    private boolean    primaryKeyProperty;
    private int[]      columnMaxStrLens; // Set in constructor.  ReferencedBeanProperties override accessor
    private String[]   columnNames;   // Initialized in constructor.  ReferencedBeanProperties override accessor
    private Class<?>[] columnTypes;   // Initialized in constructor.  ReferencedBeanProperties override accessor
    private int        propertyNum = -1;   // Set by calling setPropertyNum()

	protected Property(String   name,
                       Class<?> baseType,
                       Class<?> type,
                       boolean  isPrimaryKeyProperty,
                       Class<?> beanClass) {
		this.name      = name;
		this.baseType  = baseType;
        this.type      = type;
        this.primaryKeyProperty = isPrimaryKeyProperty;

        array = (baseType != type);

		String capName = name.substring(0,1).toUpperCase() + name.substring(1);

		try {
			getter = beanClass.getMethod("get"+capName);
		} catch (NoSuchMethodException e) {
			if (type != boolean.class) {
				throw new BeanFactoryException(beanClass.getName()+" doesn't match table: no get"+capName+"() method");
			}
			try {
				getter = beanClass.getMethod("is"+capName);
			} catch (NoSuchMethodException e2) {
				throw new BeanFactoryException(beanClass.getName()+" doesn't match table: no get"+capName+"() or is"+capName+"() method");
			}
		}

		if (getter.getReturnType() != type) {
			throw new BeanFactoryException(beanClass.getName()+" doesn't match table: get"+capName+"() returns "+getter.getReturnType().getCanonicalName()+" (not "+type.getCanonicalName()+" which is the table's type)");
		}

        if (isPrimaryKeyProperty) {
            setter = null;
        } else {
    		try {
    			setter = beanClass.getMethod("set"+capName,type);
    		} catch (NoSuchMethodException e) {
    			throw new BeanFactoryException(beanClass.getName()+" doesn't match table: no set"+capName+"("+type.getCanonicalName()+") method");
    		}
        }

        columnMaxStrLens = new int[] { deriveMaxStringLength(name,baseType,beanClass) };
        columnNames = new String[] { name     };
        columnTypes = new Class[]  { baseType };
	}

	public int compareTo(Property other) {
		boolean thisPrimary  = (this.primaryKeyProperty);
		boolean otherPrimary = (other.primaryKeyProperty);

        if (thisPrimary && otherPrimary) {
            return getPropertyNum() - other.getPropertyNum();
        }

        if (thisPrimary)  return -1;
		if (otherPrimary) return 1;

		if (!array && other.array) return -1;
		if (array && !other.array) return 1;

		int c = name.compareTo(other.name);
		if (c != 0) return c;

		return baseType.getName().compareTo(other.baseType.getName());
	}

    
    /**
     * Method for checking whether there is a specified max string length or not.
     * If yes, derive the length from beanClass. If not, set the length to 255.
     * This applies to Strings properties, but also enum properties which are stored as Strings
     * @param propName
     * @param baseType
     * @param beanClass
     * @return
     */
    private int deriveMaxStringLength(String    propName,
		    						  Class<?>  baseType,
		    						  Class <?> beanClass)
    {
    	if (baseType != String.class && !baseType.isEnum()) return 0;

    	String propNamePlusSuffix = propName + "MAXLENGTH";
    	
    	Field [] allFields = beanClass.getDeclaredFields();
		for (Field f : allFields) {
			String fieldName = f.getName();
			try {
				if (f.getType() == int.class && fieldName.endsWith("_MAX_LENGTH")) {
					String nameNoUnderScores = fieldName.replaceAll("_", "");
					if (nameNoUnderScores.equalsIgnoreCase(propNamePlusSuffix)) {
						return f.getInt(beanClass);
					}
				}
			} catch (IllegalAccessException e) {
				throw new BeanFactoryException("Illegal Access Exception when accessing "+fieldName);
			}
		}
		
		return 255;   //if no MAX_LENGTH specified, default size is 255
    }
    
    public boolean equals(Object obj) {
		if (obj instanceof Property) {
			Property other = (Property) obj;
			return compareTo(other) == 0;
		}
		return false;
	}

    public Class<?>   getBaseType()         { return baseType;      }

    public int[]      getColumnMaxStrLens() { return columnMaxStrLens; }
    public String[]   getColumnNames()      { return columnNames;   }
    public Class<?>[] getColumnTypes()      { return columnTypes;   }

    public Object getDefaultValue() {
        if (baseType == int.class) return INTEGER_ZERO;
        if (baseType == long.class) return LONG_ZERO;
        if (baseType == boolean.class) return Boolean.FALSE;
        if (baseType == float.class) return FLOAT_ZERO;
        if (baseType == double.class) return DOUBLE_ZERO;
        return null;

    }

	public Method   getGetter()      { return getter;    }
	public String   getName()        { return name;      }

    public int getPropertyNum() {
        if (propertyNum < 0) throw new AssertionError("getColumnNum() called before setColumnNum(): "+this);
        return propertyNum;
    }

    public Method   getSetter()      { return setter;    }
    public Class<?> getType()        { return type;      }

	public int hashCode() {
		return name.hashCode();
	}

	public boolean isArray()      { return array; }

	public boolean isInstance(Object value) {
		if (type == boolean.class) return value instanceof Boolean;
		if (type == double.class)  return value instanceof Double;
		if (type == float.class)   return value instanceof Float;
		if (type == int.class)     return value instanceof Integer;
		if (type == long.class)    return value instanceof Long;
		return type.isInstance(value);
	}

    public boolean isNullable() {
        if (baseType == boolean.class) return false;
        if (baseType == double.class)  return false;
        if (baseType == float.class)   return false;
        if (baseType == int.class)     return false;
        if (baseType == long.class)    return false;
        return true;
    }

    public boolean isPrimaryKeyProperty() { return primaryKeyProperty; }

    public void setPropertyNum(int position) {
        if (position < 0) throw new IllegalArgumentException("position < 0: "+position);
        propertyNum = position;
    }

	public String toString() {
		StringBuffer b = new StringBuffer();
        b.append(this.getClass().getSimpleName());
        b.append("(");
        if (primaryKeyProperty) b.append("PriKey,");
        b.append("name=");
		b.append(name);
		b.append(",type=");
		b.append(baseType.getName());
		if (array) b.append("[]");
		b.append(')');
		return b.toString();
	}
}
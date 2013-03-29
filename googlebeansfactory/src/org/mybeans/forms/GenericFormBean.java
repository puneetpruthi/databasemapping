package org.mybeans.forms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mybeans.factory.BeanFactoryException;

public abstract class GenericFormBean {
	private String[] fieldNames;
	private String   button      = "";
	
	public String   getAction()  { return null; }
	public String   getBanner()  { return null; }
	public String[] getButtons() { return new String[] { "Submit" }; }
	public String   getFocus()   { return null; }
	public String   getTitle()   { return null; }
	public int      getWidth()   { return 40;   }
	
	public GenericFormRow[] getRows() {
		GenericFormRow[] rows = new GenericFormRow[fieldNames.length];
		
		for (int i=0; i<rows.length; i++) {
			String fieldName = fieldNames[i];
			MyFormRow row = new MyFormRow();
			row.setComment(getComment(fieldName));
			row.setDisplayName(getDisplayName(fieldName));
			row.setName(fieldName);
			row.setValue(getValue(fieldName));
			row.setDisabled(isDisabled(fieldName));
			row.setHidden(isHidden(fieldName));
			row.setPassword(isPassword(fieldName));
			rows[i] = row;
		}
		
		return rows;
	}

	public boolean isEmpty() {
		if (getValue("button").length() > 0) return false;
		
		for (String fieldName : fieldNames) {
			String value = getValue(fieldName);
			if (value != null && value.length() > 0) return false;
		}

		return true;
	}
	
	protected String  getComment(String formFieldName) { return null; }
	protected boolean isDisabled(String formFieldName) { return false; }
	protected boolean isHidden(String formFieldName)   { return false; }
	protected boolean isPassword(String formFieldName) { return false; }
	
	protected String  getDisplayName(String formFieldName) {
		StringBuffer b = new StringBuffer();
		b.append(Character.toUpperCase(formFieldName.charAt(0)));
		for (int i=1; i<formFieldName.length(); i++) {
			char c = formFieldName.charAt(i);
			if (Character.isUpperCase(c)) b.append(' ');
			b.append(c);
		}
		b.append(':');
		return b.toString();
	}
	
	public GenericFormBean(String[] formFieldNamesInOrderOfDisplay) {
		fieldNames = formFieldNamesInOrderOfDisplay.clone();
		validateFieldNames();
	}
	
	public String getButton() { return button; }
	
	private String getValue(String fieldName) {
		String capName = fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
		try {
			Method getter = getClass().getMethod("get"+capName);
			if (getter.getReturnType() != String.class) throw new BeanFactoryException("The get"+capName+"() method in "+getClass().getSimpleName()+" doesn't have a return type of String");
            return (String) getter.invoke(this);
		} catch (NoSuchMethodException e) {
			throw new BeanFactoryException(getClass().getSimpleName()+" does not have a get method for field named "+fieldName+" (which needs to be called get"+capName+"())",e);
        } catch (IllegalAccessException e) {
            throw new BeanFactoryException("IllegalAccessException when calling get"+capName+"() method in "+getClass().getSimpleName(),e);
        } catch (InvocationTargetException e) {
            throw new BeanFactoryException("InvocationTargetException when calling get"+capName+"() method in "+getClass().getSimpleName(),e);
        }
	}

	public void setButton(String s) { button = s; }

	private void validateFieldNames() {
		for (String fieldName : fieldNames) {
			getValue(fieldName);
		}
	}
	
	private static class MyFormRow implements GenericFormRow {
		private String name;
		private String displayName;
		private String value;
		private String comment;
		private boolean hidden;
		private boolean disabled;
		private boolean password;
		
		public boolean isPassword() {
			return password;
		}
		public void setPassword(boolean password) {
			this.password = password;
		}
		public String getComment() {
			return comment;
		}
		public void setComment(String comment) {
			this.comment = comment;
		}
		public String getDisplayName() {
			return displayName;
		}
		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		public boolean isDisabled() {
			return disabled;
		}
		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}
		public boolean isHidden() {
			return hidden;
		}
		public void setHidden(boolean hidden) {
			this.hidden = hidden;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
}

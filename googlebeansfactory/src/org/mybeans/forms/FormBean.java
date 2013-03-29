package org.mybeans.forms;

import java.util.List;

public interface FormBean {
	public List<String> getValidationErrors();
	public boolean isEmpty();
}

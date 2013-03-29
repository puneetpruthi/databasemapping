package org.mybeans.forms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

/**
 * This is a factory class to create and populate JavaBeans with the parameter values from
 * an HTTP request.
 * <p>
 * The bean must provide setter methods with <tt>void</tt> return type
 * and have a single <tt>String</tt> or <tt>String[]</tt> argument.
 * Also, the bean must have a no-args constructor.
 * <p> 
 * See the <tt>create()</tt> method below for a more detailed description of how the bean will be populated.
 * @param <B> the for bean to be populated.
 */
public class FormBeanFactory<B> {
	/**
	 * Returns an instance of a <tt>FormBeanFactory</tt> for the given bean class.  Optionally,
	 * accepts a list of characters (passed in a string) that the factory will convert into HTML character entities.
	 * For example, if passed &quot;&lt;&gt;&quot; this factory would always
	 * convert any &lt; and &gt; characters in request parameters into &amp;lt; and &amp;gt; characters.
	 * @param <T> The type of the beans populated and returned by this factory.
	 * @param beanClass The class of beans to be populated and returned by this factory.
	 * @param charsToConvert A string containing the character this factory will  convert into HMTL character entities.
	 * @return a factory that can be used to populate form beans using request parameter data.
	 * @throws IllegalArgumentException if the <tt>beanClass</tt> does not have the proper constructor or setters.
	 */
	public static <T> FormBeanFactory<T> getInstance(
						Class<T> beanClass,
						String charsToConvert)
	{
		return new FormBeanFactory<T>(beanClass,charsToConvert);
	}
	
	private Pattern badCharPat = null;;
	private List<Method> setterList = new ArrayList<Method>();
	private List<Method> arraySetterList = new ArrayList<Method>();
	private Constructor<B> constructor;
	
	private FormBeanFactory(Class<B> beanClass, String charsToConvert) {
		try {
			constructor = beanClass.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(beanClass.getSimpleName()+" does not have a no-args constructor.");
		}
		
		for (Method m : beanClass.getMethods()) {
			Class<?>[] params = m.getParameterTypes();
			if (m.getName().startsWith("set") &&
					params.length == 1 && params[0] == String.class &&
					m.getReturnType() == void.class) {
				setterList.add(m);
			} else if (m.getName().startsWith("set") &&
					params.length == 1 && params[0] == String[].class &&
					m.getReturnType() == void.class) {
				arraySetterList.add(m);
			}
		}
		
		if (setterList.size() == 0 && arraySetterList.size() == 0) {
			throw new IllegalArgumentException(beanClass.getSimpleName()+" does not have any setters of the correct signature.");
		}
		
		if (charsToConvert == null || charsToConvert.length() == 0) {
			badCharPat = null;
			return;
		}
		
		StringBuffer b = new StringBuffer("[");
		for (int i=0; i<charsToConvert.length(); i++) {
			b.append("\\u");
			appendHexCode(b,charsToConvert.charAt(i));
		}
		b.append(']');
		badCharPat = Pattern.compile(b.toString());
//		System.out.println("Pattern="+b);
	}
	
	/**
	 * Creates a form bean B and sets its properties with the parameters of the
	 * same name found in the given request.
	 * <p>
	 * If the setter for a bean property takes a <tt>String</tt> argument, then
	 * <tt>request.getParameter()</tt> is used to obtain the value from the request.
	 * If the setter takes a <tt>String[]</tt> argument, then
	 * <tt>request.getParameterValues()</tt> is used to obtain all the values 
	 * of this parameter from the request.  (You would use this later style of
	 * setter if you needed to received multiple values from an HTML Checkbox
	 * form field.
	 * @param request an HTTP request from which parameter values should be obtained.
	 * @return an instance of B populated with the request parameters  as described above.
	 * @throws FormBeanError if setting a bean property throws an exception.
	 */
	public B create(HttpServletRequest request) {
		B answer;
		
		try {
			answer = constructor.newInstance();
		} catch (Exception e) {
			throw new FormBeanError("Exception when instantiating bean",e);
		}
	
		for (Method m : setterList) {
			String name = m.getName().substring(3,4).toLowerCase()+m.getName().substring(4);
			String value = fixChars(request.getParameter(name));
			if (value != null) {
				try {
					m.invoke(answer,fixChars(value));
				} catch (Exception e) {
					throw new FormBeanError("Exception when calling "+m.getName()+" with value="+value,e);
				}
			}
		}

		for (Method m : arraySetterList) {
			String name = m.getName().substring(3,4).toLowerCase()+m.getName().substring(4);
			String[] values = request.getParameterValues(name);
			if (values != null) {
				String[] clone = new String[values.length];
				for (int i=0; i<values.length; i++) clone[i] = fixChars(values[i]);
				try {
					m.invoke(answer,(Object)clone);
				} catch (Exception e) {
					throw new FormBeanError("Exception when calling "+m.getName()+" with values.length="+values.length,e);
				}
			}
		}

		return answer;
	}
	
	private void appendHexCode(StringBuffer b, char c) {
		appendHexDigit(b, ((c>>12) & 0xf) );
		appendHexDigit(b, ((c>>8)  & 0xf) );
		appendHexDigit(b, ((c>>4)  & 0xf) );
		appendHexDigit(b, (  c     & 0xf) );
	}
	
	private void appendHexDigit(StringBuffer b, int x) {
		if (x < 0 | x > 15) throw new AssertionError(String.valueOf(x));
		
		if (x < 10) {
			b.append((char)('0'+x));
			return;
		}
		
		b.append((char)('a'+x-10));
	}
	
	private String fixChars(String s) {
		if (badCharPat == null) return s;
		if (s == null || s.length() == 0) return s;
		
        Matcher m = badCharPat.matcher(s);
        StringBuffer b = null;
        while (m.find()) {
            if (b == null) b = new StringBuffer();
            switch (s.charAt(m.start())) {
                case '<':  m.appendReplacement(b,"&lt;");
                           break;
                case '>':  m.appendReplacement(b,"&gt;");
                           break;
                case '&':  m.appendReplacement(b,"&amp;");
                		   break;
                case '"':  m.appendReplacement(b,"&quot;");
                           break;
                default:   m.appendReplacement(b,"&#"+((int)s.charAt(m.start()))+';');
            }
        }
        
        if (b == null) return s;
        m.appendTail(b);
        return b.toString();
    }
}

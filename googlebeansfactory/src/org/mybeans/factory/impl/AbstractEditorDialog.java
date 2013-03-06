/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.lang.reflect.Array;
import java.sql.Time;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.mybeans.nonmodifiable.NMDate;
import org.mybeans.nonmodifiable.NMSQLDate;
import org.mybeans.nonmodifiable.NMTime;


public abstract class AbstractEditorDialog extends JDialog {
	private static final String dateFormatStr               = "yyyy-MM-dd";
	private static final String dateTimeFormatWithSecStr    = "yyyy-MM-dd HH:mm:ss";
	private static final String dateTimeFormatWithoutSecStr = "yyyy-MM-dd HH:mm";
	private static final String timeFormatWithSecStr        = "HH:mm:ss";
	private static final String timeFormatWithoutSecStr     = "HH:mm";

	private static final Font fixedFont = new Font("Courier",Font.PLAIN,12);
	private static final Font titleFont = new Font("Serif",Font.BOLD,18);

	protected GridBagConstraints cTitles, cButton, cButton2, cLabel, cLabelRight, cLabelRight2, cField, cField2, cFieldWide, cMessage;

	private JTextField messageField = null;  // setup by addMessageField()
	private Color originalMessageFieldColor = null;

	public AbstractEditorDialog() {
		super((JFrame)null,true);
		setupConstraints();
	}

	public AbstractEditorDialog(JDialog parent) {
		super(parent,true);
		setupConstraints();
	}

	protected JButton addButton(JPanel pane, String title, GridBagLayout layout, GridBagConstraints constraints, ActionListener listener) {
		JButton b = new JButton(title);
		if (listener != null) b.addActionListener(listener);
		if (layout != null && constraints != null) layout.setConstraints(b,constraints);
		pane.add(b);
		return b;
	}

	protected JComponent addComponent(JPanel pane, JComponent component, GridBagLayout layout, GridBagConstraints constraints) {
		layout.setConstraints(component,constraints);
		pane.add(component);
		return component;
	}

	protected JTextField addDisplayField(JPanel pane, int width, GridBagLayout layout, GridBagConstraints constraints) {
		JTextField f = addField(pane,width,layout,constraints,null);
		f.setEditable(false);
		return f;
	}

	protected JTextField addDisplayField(JPanel pane, GridBagLayout layout, GridBagConstraints constraints) {
		JTextField f = addField(pane,layout,constraints,null);
		f.setEditable(false);
		return f;
	}

	protected JTextField addField(JPanel pane, GridBagLayout layout, GridBagConstraints constraints, ActionListener listener) {
		JTextField f = new JTextField();
		if (listener != null) f.addActionListener(listener);
		if (layout != null && constraints != null) layout.setConstraints(f,constraints);
		pane.add(f);
		return f;
	}

	protected JTextField addField(JPanel pane, int width, GridBagLayout layout, GridBagConstraints constraints, ActionListener listener) {
		JTextField f = new JTextField(width);
		if (listener != null) f.addActionListener(listener);
		if (layout != null && constraints != null) layout.setConstraints(f,constraints);
		pane.add(f);
		return f;
	}

	protected JLabel addLabel(JPanel pane, String text, GridBagLayout layout, GridBagConstraints constraints) {
		JLabel label = new JLabel(text);
		if (layout != null && constraints != null) layout.setConstraints(label,constraints);
		pane.add(label);
		return label;
	}

	protected JTextField addMessageField(JPanel pane, GridBagLayout layout, GridBagConstraints constraints) {
		JTextField f = addDisplayField(pane,layout,constraints);
		f.setEditable(false);
		messageField = f;
		originalMessageFieldColor = f.getBackground();
		return f;
	}

	protected JTextField addMessageField(JPanel pane, int width, GridBagLayout layout, GridBagConstraints constraints) {
		JTextField f = addDisplayField(pane,width,layout,constraints);
		f.setEditable(false);
		messageField = f;
		originalMessageFieldColor = f.getBackground();
		return f;
	}

	protected JTextArea addScrollableTextArea(JPanel pane, int rows, int cols, GridBagLayout layout, GridBagConstraints constraints) {
		JTextArea textArea = new JTextArea(rows,cols);
		textArea.setFont(fixedFont);
		JScrollPane scroller = new JScrollPane(textArea);
		if (layout != null && constraints != null) layout.setConstraints(scroller,constraints);
		pane.add(scroller);
		return textArea;
	}

	protected JLabel addTitle(JPanel pane, String title, GridBagLayout layout, GridBagConstraints constraints) {
		JLabel label = new JLabel(title);
		label.setFont(titleFont);
		if (layout != null && constraints != null) layout.setConstraints(label,constraints);
		pane.add(label);
		return label;
	}

	protected void clearMessage() {
		messageField.setText("");
		messageField.setBackground(originalMessageFieldColor);
	}

	protected void errorMessage(Exception e) {
		errorMessage(e.getMessage());
	}

	protected void errorMessage(String s) {
		messageField.setText(s);
		messageField.setCaretPosition(0);
		messageField.setBackground(Color.PINK);
	}

	protected String formatHint(Property prop) {
		Class<?> type = prop.getBaseType();
		if (type == java.util.Date.class || type == NMDate.class) return dateTimeFormatWithSecStr;
		if (type == java.sql.Date.class || type == NMSQLDate.class) return dateFormatStr;
		if (type == Time.class || type == NMTime.class) return timeFormatWithSecStr;
		return type.getSimpleName();
	}

	private String getFormatString(String s, Class<?> type) {
		if (type == java.util.Date.class || type == NMDate.class) {
			if (s.length() == dateTimeFormatWithoutSecStr.length()) return dateTimeFormatWithoutSecStr;
			return dateTimeFormatWithSecStr;
		}

		if (type == java.sql.Date.class || type == NMSQLDate.class) {
			return dateFormatStr;
		}

		if (type == Time.class || type == NMTime.class) {
			if (s.length() == timeFormatWithoutSecStr.length()) return timeFormatWithoutSecStr;
			return timeFormatWithSecStr;
		}

		return null;
	}

	private SimpleDateFormat getFormatter(java.util.Date d) {
		if (d instanceof java.sql.Date) return new SimpleDateFormat(dateFormatStr);
		if (d instanceof Time) return new SimpleDateFormat(timeFormatWithSecStr);
		return new SimpleDateFormat(dateTimeFormatWithSecStr);
	}

	protected void message(String s) {
		messageField.setText(s);
		messageField.setCaretPosition(0);
		messageField.setBackground(Color.GREEN);
	}

	protected Object parseValue(String s, Property prop) throws ParseException {
		if (s.equalsIgnoreCase("null")) return null;

		Class<?> type = prop.getBaseType();
		if (type == String.class) {
			if (s.length() > 1 && s.charAt(0) == '"' && s.charAt(s.length()-1) == '"') {
				return s.substring(1,s.length()-1);
			}
			return s;
		}

		if (type == boolean.class) {
			if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
			if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
			throw new ParseException(prop.getName()+": Invalid boolean (use \"true\" or \"false\")",0);
		}

		if (type == float.class) {
			try {
				return new Float(s);
			} catch (NumberFormatException e) {
				throw new ParseException(prop.getName()+": Invalid float",0);
			}
		}

		if (type == double.class) {
			try {
				return new Double(s);
			} catch (NumberFormatException e) {
				throw new ParseException(prop.getName()+": Invalid double",0);
			}
		}

		if (type == int.class) {
			try {
				return new Integer(removeCommas(s));
			} catch (NumberFormatException e) {
				throw new ParseException(prop.getName()+": Invalid int",0);
			}
		}

		if (type == long.class) {
			try {
				return new Long(removeCommas(s));
			} catch (NumberFormatException e) {
				throw new ParseException(prop.getName()+": Invalid long",0);
			}
		}

		String formatStr = getFormatString(s,type);
		if (formatStr == null) throw new ParseException(prop.getName()+": Unknown type: "+type.getName(),0);

		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(formatStr);
			java.util.Date d = dateFormat.parse(s);
			long time = d.getTime();
			if (type == java.sql.Date.class) return new java.sql.Date(time);
			if (type == Time.class)          return new Time(time);
			if (type == NMDate.class)        return new NMDate(time);
			if (type == NMSQLDate.class)     return new NMSQLDate(time);
			if (type == NMTime.class)        return new NMTime(time);
			return d;
		} catch (ParseException e) {
			throw new ParseException(prop.getName()+": Invalid Date & Time (use "+formatStr+")",0);
		}
	}

	private String removeCommas(String s) {
		if (s.indexOf(',') == -1) return s;

		StringBuffer b = new StringBuffer();
		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c != ',') b.append(c);
		}

		return b.toString();
	}

	protected GridBagConstraints pos(GridBagConstraints c, int row, int col) {
		GridBagConstraints answer = (GridBagConstraints) c.clone();
		answer.gridy = row;
		answer.gridx = col;
		return answer;
	}

	private void setupConstraints() {
		cTitles = new GridBagConstraints();
		cTitles.gridwidth = GridBagConstraints.REMAINDER;
		cTitles.ipady = 15;
		cTitles.anchor = GridBagConstraints.CENTER;

		cLabel = new GridBagConstraints();
		cLabel.anchor = GridBagConstraints.WEST;
		cLabel.ipadx = 5;
		cLabel.gridwidth = 1;

		cLabelRight = (GridBagConstraints) cLabel.clone();
		cLabelRight.anchor = GridBagConstraints.EAST;

		cLabelRight2 = (GridBagConstraints) cLabelRight.clone();
		cLabelRight2.gridwidth = 2;

		cButton = new GridBagConstraints();
		cButton.ipadx = 5;
		cButton.anchor = GridBagConstraints.EAST;
		cButton.gridwidth = 1;

		cButton2 = (GridBagConstraints) cButton.clone();
		cButton2.ipadx = 20;
		cButton2.gridwidth = 2;

		cField = new GridBagConstraints();
		cField.fill = GridBagConstraints.BOTH;
		cField.ipadx = 120;
		cField.anchor = GridBagConstraints.CENTER;
		cField.gridwidth = 1;

		cField2 = (GridBagConstraints) cField.clone();
		cField2.gridwidth = 2;

		cFieldWide = (GridBagConstraints) cField.clone();
		cFieldWide.ipadx = 220;

		cMessage = new GridBagConstraints();
		cMessage.fill = GridBagConstraints.BOTH;
		cMessage.ipadx = 0;
		cMessage.anchor = GridBagConstraints.CENTER;
		cMessage.gridwidth = GridBagConstraints.REMAINDER;
	}

	protected String stringValue(Object obj) {
		if (obj == null) return "null";

		if (obj instanceof String) return "\""+obj+"\"";

		if (obj instanceof Boolean) return String.valueOf(obj);

		if (obj instanceof Double || obj instanceof Float) {
            DecimalFormat df = new DecimalFormat("#,###.000");
            return df.format(obj);
        }

		if (obj instanceof Integer || obj instanceof Long) {
			DecimalFormat df = new DecimalFormat("#,###");
			return df.format(obj);
		}

		if (obj instanceof java.util.Date) {
			java.util.Date d = (java.util.Date) obj;
			SimpleDateFormat dateFormat = getFormatter(d);
			return dateFormat.format(d);
		}

        if (!obj.getClass().isArray()) return obj.toString();

        if (Array.getLength(obj) == 0) {
            StringBuffer b = new StringBuffer();
            b.append("zero ");
            String typeName = obj.getClass().getSimpleName();
            b.append(typeName.substring(0,typeName.length()-2));
            b.append('s');
            return b.toString();
        }

        if (Array.getLength(obj) < 5) {
            StringBuffer b = new StringBuffer();
            for (int i=0, n=Array.getLength(obj); i<n; i++) {
                if (i>0) b.append(',');
                b.append(stringValue(Array.get(obj,i)));
            }
            return b.toString();
        }

        StringBuffer b = new StringBuffer();
        b.append(Array.getLength(obj));
        b.append(' ');
        b.append(obj.getClass().getComponentType().getSimpleName());
        if (Array.getLength(obj) != 1) b.append('s');
        b.append(": ");
        int show = 2;
        if (Array.getLength(obj) < show) show = Array.getLength(obj);
        for (int i=0; i<show; i++) {
            if (i > 0) b.append(", ");
            b.append(stringValue(Array.get(obj,i)));
        }
        if (Array.getLength(obj) > show) b.append(", ...");
        return b.toString();
    }
}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;


public class ShowArray extends AbstractEditorDialog implements ActionListener {
	private JTextArea    textArea;
	private JButton      saveButton, cancelButton, setToNullButton;

	private boolean editable;
	private boolean saved = false;

	private ArrayList<Object> editedArray;
	private Property property;

	public ShowArray(JDialog parent, Property property, Object arrayValue, boolean editable) {
		super(parent);
		this.property = property;

		String title = (editable ? "Edit " : "Show ") + property.getName();
		setTitle(title);
		setBounds(200,200,400,380);

		JPanel pane = new JPanel();

		addTitle(pane,title,null,null);
		pane.add(new JLabel("Enter the array values, one per line.  Then click \"Use Above Values\"."));
		pane.add(new JLabel("To set the entire array to null, click \"Set to Null\" below."));

		textArea = addScrollableTextArea(pane,12,50,null,null);
		textArea.setEditable(editable);

		if (arrayValue != null) {
			StringBuffer b = new StringBuffer();
            for (int i=0, n=Array.getLength(arrayValue); i<n; i++) {
				b.append(stringValue(Array.get(arrayValue,i)));
				b.append('\n');
			}
			textArea.setText(b.toString());
		}

		if (editable) {
			saveButton = addButton(pane,"Use Above Values",null,null,this);
			setToNullButton = addButton(pane,"Set to Null",null,null,this);
			cancelButton = addButton(pane,"Cancel",null,null,this);
			addMessageField(pane,36,null,null);
		}

		getContentPane().add(pane);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();

		if (editable) clearMessage();

		if (src == cancelButton) {
			setVisible(false);
		}

		if (src == setToNullButton) {
			editedArray = null;
			saved = true;
			setVisible(false);
		}

		if (src == saveButton) {
			try {
				editedArray = new ArrayList<Object>();
				BufferedReader r = new BufferedReader(new StringReader(textArea.getText()));
                for (String s=r.readLine(); s!=null; s=r.readLine()) {
					editedArray.add(parseValue(s,property));
				}
				saved = true;
				setVisible(false);
			} catch (ParseException e) {
				errorMessage(e);
			} catch (IOException e) {
				errorMessage(e);
			}
		}
	}

	public Object getEditedArray() {
		if (editedArray == null) return null;

        int n = editedArray.size();
        Object array = Array.newInstance(property.getBaseType(),n);
        for (int i=0; i<n; i++) {
            Array.set(array,i,editedArray.get(i));
        }
 System.out.println("getEditedArray: "+array);
		return array;
	}

	public boolean wasSaved() { return saved; }
}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;


import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;



public class Editor<B> extends AbstractEditorDialog implements ActionListener, WindowListener {
	private static DecimalFormat df = new DecimalFormat("#,###");

	private JButton listButton, searchButton, createButton, deleteButton, exitButton;
	private JTextField numField;
    private JTextField[] searchKeyFields;

	private AbstractFactory<B> factory;

	public Editor(AbstractFactory<B> factory, boolean closeOnExit) {
		super();
		this.factory = factory;

		String title = factory.beanClass.getSimpleName()+" Editor";
		setTitle(title);
		setBounds(50,50,450,236);
		if (closeOnExit) {
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			addWindowListener(this);
		}

		GridBagLayout layout = new GridBagLayout();
		JPanel pane = new JPanel(layout);

        int row = 0;
		addTitle(pane,title,layout,pos(cTitles,row,0));
        row++;

		String sidePadding = "         ";
		addLabel(pane,sidePadding,layout,pos(cLabelRight,row,0));
		addLabel(pane,"Instances: ",layout,pos(cLabelRight,row,1));
		numField = addDisplayField(pane,layout,pos(cField,row,2));
		listButton = addButton(pane,"List All",layout,pos(cButton,row,3),this);
		addLabel(pane,sidePadding,layout,pos(cLabelRight,row,4));
		row++;

		addTitle(pane,"",layout,pos(cTitles,row,0));
        row++;

		Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
        searchKeyFields = new JTextField[priKeyProps.length];
        for (int i=0; i<priKeyProps.length; i++) {
            addLabel(pane,priKeyProps[i].getName()+": ",layout,pos(cLabelRight,row,1));
            searchKeyFields[i] = addField(pane,layout,pos(cField2,row,2),this);
            addLabel(pane,"  "+formatHint(priKeyProps[i]),layout,pos(cLabel,row,4));
            row++;
        }

		JPanel line = new JPanel();
		searchButton = addButton(line,"Lookup",null,null,this);
		createButton = addButton(line,"Create",null,null,this);
		deleteButton = addButton(line,"Delete",null,null,this);
        exitButton   = addButton(line,"Exit",  null,null,this);
		addComponent(pane,line,layout,pos(cMessage,row,0));
        row++;

		addTitle(pane,"",layout,pos(cTitles,row++,0));
		addMessageField(pane,layout,pos(cMessage,row++,0));

		updateSize();

		getContentPane().add(pane);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		clearMessage();

		if (src == listButton) {
			new ShowList<B>(this,factory);
		} else if (src == createButton) {
			createAction();
		} else if (src == deleteButton) {
			deleteAction();
		} else if (src == searchButton) {
			searchAction();
        } else if (src == exitButton) {
            System.exit(0);
		} else {
            for (int i=0; i>searchKeyFields.length; i++) {
                if (src == searchKeyFields[i]) {
                    searchAction();
                    break;
                }
            }
        }

		updateSize();
	}

	private void createAction() {
		boolean primaryKeyIsAutoIncrement = false;
        Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
		if (priKeyProps.length == 1 && priKeyProps[0].getType() == int.class) primaryKeyIsAutoIncrement = true;
        if (priKeyProps.length == 1 && priKeyProps[0].getType() == long.class) primaryKeyIsAutoIncrement = true;

		try {
            String[] keyStrings = new String[searchKeyFields.length];
            Property zeroLenKeyProp = null;
            for (int i=0; i<keyStrings.length; i++) {
                keyStrings[i] = searchKeyFields[i].getText();
                if (keyStrings[i].length() == 0 && zeroLenKeyProp == null) {
                    zeroLenKeyProp = priKeyProps[i];
                }
            }

			B bean;
            Object[] key = new Object[priKeyProps.length];
			if (primaryKeyIsAutoIncrement && zeroLenKeyProp != null) {
				Transaction.begin();
				bean = factory.create();
				Transaction.rollback();
                for (int i=0; i<key.length; i++) {
                    key[i] = factory.getBeanValue(bean,priKeyProps[i]);
                }
			} else if (zeroLenKeyProp != null) {
				errorMessage("Primary key property ("+zeroLenKeyProp.getName()+") must be provided");
				return;
			} else {
                for (int i=0; i<key.length; i++) {
                    key[i] = parseValue(keyStrings[i],priKeyProps[i]);
                }

				Transaction.begin();
				bean = factory.create(key);
				Transaction.rollback();
			}

			ShowBean<B> editor = new ShowBean<B>(this,factory,bean,true,true);
			if (editor.wasBeanStored()) {
				message("Created "+factory.beanClass.getSimpleName()+" with primary key "+stringValue(key));
			}
		} catch (ParseException e) {
			errorMessage(e.getMessage());
		} catch (RollbackException e) {
			e.printStackTrace();
			errorMessage(e.getMessage());
		}
	}

	private void deleteAction() {
        Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
        String[] keyStrings = new String[searchKeyFields.length];
        Property zeroLenKeyProp = null;
        for (int i=0; i<keyStrings.length; i++) {
            keyStrings[i] = searchKeyFields[i].getText();
            if (keyStrings[i].length() == 0) zeroLenKeyProp = priKeyProps[i];
        }

        if (zeroLenKeyProp != null) {
            errorMessage("Primary key property ("+zeroLenKeyProp.getName()+") must be provided");
            return;
        }

		try {
            Object[] key = new Object[priKeyProps.length];
            for (int i=0; i<key.length; i++) {
                key[i] = parseValue(keyStrings[i],priKeyProps[i]);
            }

			factory.delete(key);
			message("Deleted "+factory.beanClass.getSimpleName()+" with primary key "+stringValue(keyStrings));
		} catch (ParseException e) {
			errorMessage(e);
		} catch (RollbackException e) {
			e.printStackTrace();
			errorMessage(e);
		}
	}

	private void searchAction() {
        Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
        String[] keyStrings = new String[searchKeyFields.length];
        Property zeroLenKeyProp = null;
        for (int i=0; i<keyStrings.length; i++) {
            keyStrings[i] = searchKeyFields[i].getText();
            if (keyStrings[i].length() == 0) zeroLenKeyProp = priKeyProps[i];
        }

        if (zeroLenKeyProp != null) {
            errorMessage("Primary key property ("+zeroLenKeyProp.getName()+") must be provided");
            return;
        }

		try {
            Object[] key = new Object[priKeyProps.length];
            for (int i=0; i<key.length; i++) {
                key[i] = parseValue(keyStrings[i],priKeyProps[i]);
            }

			B bean = factory.lookup(key);
			if (bean == null) {
				errorMessage("There is no instance with primary key = \""+stringValue(key)+"\"");
				return;
			}

			ShowBean<B> editor = new ShowBean<B>(this,factory,bean,true,false);
			if (editor.wasBeanStored()) {
				message("Saved "+factory.beanClass.getSimpleName()+" with primary key "+stringValue(key));
			}
		} catch (ParseException e) {
			errorMessage(e.getMessage());
		} catch (RollbackException e) {
			e.printStackTrace();
			errorMessage(e.getMessage());
		}
	}

	private void updateSize() {
		try {
			int numBeans = factory.getBeanCount();
			numField.setText(df.format(numBeans));
		} catch (RollbackException e) {
			numField.setText("");
			errorMessage(e);
		}
	}

	public void windowActivated(WindowEvent evt)   { /* Do nothing */ }
	public void windowClosed(WindowEvent evt)      { System.exit(0);  }
	public void windowClosing(WindowEvent evt)     { /* Do nothing */ }
	public void windowDeactivated(WindowEvent evt) { /* Do nothing */ }
	public void windowDeiconified(WindowEvent evt) { /* Do nothing */ }
	public void windowIconified(WindowEvent evt)   { /* Do nothing */ }
	public void windowOpened(WindowEvent evt)      { /* Do nothing */ }
}

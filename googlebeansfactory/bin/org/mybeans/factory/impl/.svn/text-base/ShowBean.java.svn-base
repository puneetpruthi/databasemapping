/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;


public class ShowBean<B> extends AbstractEditorDialog implements ActionListener {
	private JButton      cancelButton;
	private JButton[]    editButtons;
	private JTextField[] fields;
	private JTextField   messageField;
	private JButton      saveButton;

	private AbstractFactory<B> factory;
	private B            oldBean, savedBean;
	private Object[]     arrayValues;
	private boolean      create;

	private boolean beanWasStored = false;

	public ShowBean(AbstractFactory<B> factory, B bean, boolean editable, boolean create) {
		super();
		commonInit(factory,bean,editable,create);
	}

	public ShowBean(JDialog parent, AbstractFactory<B> factory, B bean, boolean editable, boolean create) {
		super(parent);
		commonInit(factory,bean,editable,create);
	}

	public void commonInit(AbstractFactory<B> factory, B bean, boolean editable, boolean create) {
		this.factory  = factory;
//		this.editable = editable;   // Not implemented, yet
		this.create   = create;

		loadArrayValues(bean);

		String title = (editable ? "Edit " : "Show ") + factory.beanClass.getSimpleName();
		setTitle(title);
		setBounds(160,160,520,200+factory.properties.length*20);

		GridBagLayout layout = new GridBagLayout();
		JPanel pane = new JPanel(layout);

		int row = 0;
		addTitle(pane,title,layout,pos(cTitles,row,0));
		row++;

		addTitle(pane,"",layout,pos(cTitles,row,0));
		row++;

		fields = new JTextField[factory.properties.length];
		if (editable) editButtons = new JButton[factory.properties.length];

		for (int i=0; i<factory.properties.length; i++) {
			addLabel(pane,factory.properties[i].getName(),layout,pos(cLabelRight,row,1));
			fields[i] = addField(pane,layout,pos(cFieldWide,row,2),null);
			if (!editable || factory.properties[i].isPrimaryKeyProperty()) {
				fields[i].setEditable(false);
                addLabel(pane,"  (primary key)",layout,pos(cLabel,row,3));
			} else if (!factory.properties[i].isArray()){
				addLabel(pane,"  "+formatHint(factory.properties[i]),layout,pos(cLabel,row,3));
			} else {
				fields[i].setEditable(false);
				editButtons[i] = addButton(pane,"Edit Array",layout,pos(cLabel,row,3),this);
			}
			row++;
		}

		if (editable) {
			fields[0].setEditable(false);
			addTitle(pane,"",layout,pos(cTitles,row,0));
			row++;

			JPanel line = new JPanel();
			saveButton = addButton(line,create?"Create":"Save",null,null,this);
			cancelButton = addButton(line,"Cancel",null,null,this);
			addComponent(pane,line,layout,pos(cMessage,row,0));
			row++;

			addTitle(pane,"",layout,pos(cTitles,row,0));
			row++;

			messageField = addMessageField(pane,layout,pos(cMessage,row,0));
			row++;
		}

        try {
            oldBean = copyBean(bean);
            populateFields();
        } catch (RollbackException e) {
            messageField.setText(e.getMessage());
            e.printStackTrace();
        }


		getContentPane().add(pane);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();

		if (src == cancelButton) {
			setVisible(false);
		}

		for (int i=0; i<factory.properties.length; i++) {
			if (src == editButtons[i]) editArray(i);
		}

		if (src == saveButton) saveBean();
	}

	private B copyBean(B fromBean) throws RollbackException {
        Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
		Object[] key = new Object[priKeyProps.length];
        for (int i=0; i<key.length; i++) {
            key[i] = factory.getBeanValue(fromBean,priKeyProps[i]);
        }
		B toBean = factory.newBean(key);

		for (int i=priKeyProps.length; i<factory.properties.length; i++) {
			Object fromValue = factory.getBeanValue(fromBean,factory.properties[i]);
			Object toValue = Values.cloneBeanValue(fromValue);
			factory.setBeanValue(toBean,factory.properties[i],toValue);
		}

		return toBean;
	}

	private void editArray(int i) {
		ShowArray editor = new ShowArray(this,factory.properties[i],arrayValues[i],true);
		if (editor.wasSaved()) {
			Object editedArray = editor.getEditedArray();
			arrayValues[i] = editedArray;
			fields[i].setText(stringValue(editedArray));
			fields[i].setCaretPosition(0);
		}
	}

	public B getSavedBean() { return savedBean; }

	private void loadArrayValues(B bean) {
		arrayValues = new Object[factory.properties.length];
		for (int i=0; i<factory.properties.length; i++) {
			if (factory.properties[i].isArray()) {
				Object value = factory.getBeanValue(bean,factory.properties[i]);
                arrayValues[i] = Values.cloneBeanValue(value);
			}
		}
	}

	private B makeNewBean() throws ParseException, RollbackException {
        Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
        Object[] key = new Object[priKeyProps.length];
        for (int i=0; i<key.length; i++) {
            key[i] = factory.getBeanValue(oldBean,priKeyProps[i]);
        }
        B answer = factory.newBean(key);

        for (int i=priKeyProps.length; i<fields.length; i++) {
			Object value;
			if (factory.properties[i].isArray()) {
				value = arrayValues[i];
			} else {
				value = parseValue(fields[i].getText(),factory.properties[i]);
			}
			factory.setBeanValue(answer,factory.properties[i],value);
		}
		return answer;
	}

	private void populateFields() {
		for (int i=0; i<factory.properties.length; i++) {
			if (factory.properties[i].isArray()) {
				fields[i].setText(stringValue(arrayValues[i]));
			} else {
				fields[i].setText(stringValue(factory.getBeanValue(oldBean,factory.properties[i])));
			}
			fields[i].moveCaretPosition(0);
		}
	}

	private void saveBean() {
		try {
			B newBean = makeNewBean();
            Property[] priKeyProps = factory.primaryKeyInfo.getProperties();
            Object[] key = new Object[priKeyProps.length];
            for (int i=0; i<priKeyProps.length; i++) {
                key[i] = factory.getBeanValue(newBean,priKeyProps[i]);
            }

			Transaction.begin();
			B dbBean = factory.lookup(key);

			try {
				if (create && dbBean == null) {
					dbBean = factory.create(key);
				} else if (create && dbBean != null) {
					Transaction.rollback();
					throw new RollbackException("Someone else created the bean while you were editing.  You are now editing the current bean.");
				} else if (dbBean == null) {
					dbBean = factory.create(key);
					Transaction.rollback();
					throw new RollbackException("Someone else deleted the bean while you were editing.  You can now re-create the bean.");
				} else if (!factory.equals(oldBean,dbBean)) {
					Transaction.rollback();
					throw new RollbackException("Someone else made changes while you were editing.  You are now editing the current bean.");
				}
			} catch (RollbackException e) {
				oldBean = dbBean;
				loadArrayValues(oldBean);
				populateFields();
				throw e;
			}

			factory.copyInto(newBean,dbBean);
			Transaction.commit();
			beanWasStored = true;
			savedBean = dbBean;
			setVisible(false);
		} catch (ParseException e) {
			messageField.setText(e.getMessage());
		} catch (RollbackException e) {
			messageField.setText(e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean wasBeanStored() { return beanWasStored; }
}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.mybeans.factory.RollbackException;



public class ShowList<B> extends AbstractEditorDialog implements ActionListener, MouseListener {
	private final static int charWidth = 7;
	private final static int charHeight = 17;

	private JTextArea  textArea;
	private JButton editButton, deleteButton, refreshButton;

	private AbstractFactory<B> factory;
	private B[] list;
	private int[] lineStartPos;
    private Property[] priKeyProps;


	public ShowList(JDialog parent, AbstractFactory<B> factory) {
		super(parent);
		this.factory = factory;

        priKeyProps = factory.primaryKeyInfo.getProperties();

		String title = factory.beanClass.getSimpleName()+" List";
		setTitle(title);
		setBounds(130,130,400,340);

		JPanel pane = new JPanel();
		addTitle(pane,"Primary Key Values",null,null);
		textArea = addScrollableTextArea(pane,12,50,null,null);
		textArea.setEditable(false);
		textArea.addMouseListener(this);

		editButton = addButton(pane,"Edit",null,null,this);
		deleteButton = addButton(pane,"Delete",null,null,this);
		refreshButton = addButton(pane,"Refresh",null,null,this);
		addMessageField(pane,32,null,null);

		refreshList();

		getContentPane().add(pane);
		setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		clearMessage();

		if (src == refreshButton) {
			refreshList();
            return;
		}

        if (textArea.getSelectionStart() == textArea.getSelectionEnd()) {
            errorMessage("Select a primary key from the list above");
            return;
        }

        int pos = textArea.getSelectionStart();
        int row = Arrays.binarySearch(lineStartPos,pos);

        if (row == -1) {
            errorMessage("Could not find selection (internal error)");
            return;
        }

        if (src == editButton) list[row] = editBean(list[row]);
        if (src == deleteButton) deleteBean(list[row]);
	}

	private void deleteBean(B bean) {
		try {
            factory.delete(getPrimaryKeyValues(bean));
			refreshList();
			message("Deleted "+factory.beanClass.getSimpleName()+" with key "+stringValue(getPrimaryKeyValues(bean)));
		} catch (RollbackException e) {
			errorMessage(e);
		}
	}

	private B editBean(B bean) {
        ShowBean<B> editor = new ShowBean<B>(this,factory,bean,true,false);

        if (!editor.wasBeanStored()) return bean;

        message("Updated "+factory.beanClass.getSimpleName()+" with key "+stringValue(getPrimaryKeyValues(bean)));
        return editor.getSavedBean();
	}

    private Object[] getPrimaryKeyValues(B bean) {
        Object[] priKeyVals = new Object[priKeyProps.length];
        for (int i=0; i<priKeyVals.length; i++) {
            priKeyVals[i] = factory.getBeanValue(bean,priKeyProps[i]);
        }
        return priKeyVals;
    }

	public void mouseClicked(MouseEvent evt)  {
		int row = evt.getY()/charHeight;
		int col = evt.getX()/charWidth;
		if (row < lineStartPos.length-1 && col < lineStartPos[row+1]-lineStartPos[row]-1) {
			textArea.select(lineStartPos[row],lineStartPos[row+1]-1);
		}
	}

	public void mouseEntered(MouseEvent evt)  { /* Do nothing */ }
	public void mouseExited(MouseEvent evt)   { /* Do nothing */ }
	public void mousePressed(MouseEvent evt)  { /* Do nothing */ }
	public void mouseReleased(MouseEvent evt) { /* Do nothing */ }

	public void refreshList() {
		try {
			list = factory.match();
			lineStartPos = new int[list.length+1];
			StringBuffer b = new StringBuffer();
			for (int i=0; i<list.length; i++) {
				lineStartPos[i] = b.length();
                b.append(stringValue(getPrimaryKeyValues(list[i])));
				b.append('\n');
			}

			lineStartPos[lineStartPos.length-1] = b.length();
			textArea.setText(b.toString());
			textArea.setCaretPosition(0);
			message("Loaded "+list.length+" instances");
		} catch (RollbackException e) {
			errorMessage(e);
		}
	}
}

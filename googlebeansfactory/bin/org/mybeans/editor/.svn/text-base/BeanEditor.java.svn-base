/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.editor;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.impl.AbstractFactory;
import org.mybeans.factory.impl.Editor;
import org.mybeans.factory.impl.ShowBean;


public class BeanEditor<B> {
    public static <B> BeanEditor<B> getInstance(BeanFactory<B> factory) {
        return new BeanEditor<B>(factory);
    }

	private AbstractFactory<B> factory;
	private boolean closeOnExit = false;

    private BeanEditor(BeanFactory<B> factory) {
    	this.factory = (AbstractFactory<B>) factory;
    }

    public boolean create(B bean) {
    	try {
    		MyShowBean<B> msb = new MyShowBean<B>(factory,bean,true,true);
    		SwingUtilities.invokeAndWait(msb);
    		return msb.answer;
    	} catch (InterruptedException e) {
    		throw new AssertionError(e);
    	} catch (InvocationTargetException e) {
    		throw new AssertionError(e);
    	}
    }

    public void display(B bean) {
    	try {
    		SwingUtilities.invokeAndWait(new MyShowBean<B>(factory,bean,false,false));
    	} catch (InterruptedException e) {
    		throw new AssertionError(e);
    	} catch (InvocationTargetException e) {
    		throw new AssertionError(e);
    	}
    }

    public boolean edit(B bean) {
    	try {
    		MyShowBean<B> msb = new MyShowBean<B>(factory,bean,true,false);
    		SwingUtilities.invokeAndWait(msb);
    		return msb.answer;
    	} catch (InterruptedException e) {
    		throw new AssertionError(e);
    	} catch (InvocationTargetException e) {
    		throw new AssertionError(e);
    	}
    }

    public void setCloseOnExit(boolean closeOnExit) {
    	this.closeOnExit = closeOnExit;
    }

    public void start() {
    	try {
    		SwingUtilities.invokeAndWait(new MyEditor<B>(factory,closeOnExit));
    	} catch (InterruptedException e) {
    		throw new AssertionError(e);
    	} catch (InvocationTargetException e) {
    		throw new AssertionError(e);
    	}
    }

    private static class MyShowBean<X> implements Runnable {
    	private AbstractFactory<X> factory;
    	private X bean;
    	private boolean editable;
    	private boolean create;

    	public boolean answer = false;

    	public MyShowBean(AbstractFactory<X> f, X b, boolean edit, boolean create) {
    		factory = f;
    		bean = b;
    		editable = edit;
    		this.create = create;
    	}

    	public void run() {
    		ShowBean<X> sb = new ShowBean<X>(factory,bean,editable,create);
    		answer = sb.wasBeanStored();
    	}
    }

    private static class MyEditor<X> implements Runnable {
    	private AbstractFactory<X> factory;
    	private boolean closeOnExit;

    	public MyEditor(AbstractFactory<X> f, boolean closeOnExit) {
    		factory = f;
    		this.closeOnExit = closeOnExit;
    	}

    	public void run() {
    		new Editor<X>(factory,closeOnExit);
    	}
    }
}

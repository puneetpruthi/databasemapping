/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.BeanTable;


public class CSVTable<B> extends BeanTable<B> {
    // For debugging
    private Writer debug = null;

    // Initialized by constructor
    private Class<B> beanClass;
    private File     csvFile;
    private int      maxBackups;
    private AbstractFactory<?>[] referencedFactories;

    private CSVFactory<B> factory = null;

    // Constructors

	public 	CSVTable(
            Class<B> beanClass,
            File     csvFile,
            int      maxBackups,
			AbstractFactory<?>[] referencedFactories) {
		this.beanClass  = beanClass;
        this.csvFile    = csvFile;
        this.maxBackups = maxBackups;
        this.referencedFactories = referencedFactories;
    }

    // Public instance methods (that implement super class abstract methods)
    public void create(String... primaryKeyNames) {
        PrimaryKeyInfo<B> primaryKeyInfo = PrimaryKeyInfo.getInstance(beanClass,primaryKeyNames,referencedFactories);
        Property[] properties = Property.deriveProperties(beanClass,primaryKeyInfo,referencedFactories);

        if (csvFile.exists()) {
            throw new BeanFactoryException("Cannot create file.  It already exists.  File="+csvFile);
        }

        try {
            FileWriter fw = new FileWriter(csvFile);
            fw.write(CSVFactory.generateHeader(properties));
            fw.close();
        } catch (IOException e) {
            throw new BeanFactoryException(e);
        }
    }

    public void createSecondaryIndex(String...secondaryKeyPropertyNames) {
        // Does nothing in this implementation
    }

    public void delete() {
        if (!csvFile.delete()) throw new BeanFactoryException("Cannot delete file.  File="+csvFile);
    }

    public Class<B> getBeanClass() { return beanClass; }
    
    public synchronized BeanFactory<B> getFactory() {
        if (factory != null) return factory;
        String[] priKeyNames = getPrimaryKeyNames();
        factory = new CSVFactory<B>(beanClass,csvFile,maxBackups,priKeyNames,referencedFactories);
        factory.setDebugOutput(debug);

        for (Property p : factory.properties) {
        	if (p instanceof SelfReferencedBeanProperty) {
        		SelfReferencedBeanProperty selfRefProp = (SelfReferencedBeanProperty) p;
        		selfRefProp.setFactory(factory);
        	}
        }

        return factory;
    }

    public boolean exists() {
        return csvFile.exists();
    }
    public synchronized void setDebugOutput(Writer writer) {
        debug = writer;
        if (factory != null) factory.setDebugOutput(debug);
    }

    public void setIdleConnectionCleanup(boolean enable) {
        // Does nothing in this implementation
    }


    // Private instance methods

    private String[] getPrimaryKeyNames() {
        try {
            FileReader fr = new FileReader(csvFile);
            CSVReader  cr = new CSVReader(fr);

            String[] columnNames = cr.readCSVLine();
            if (columnNames == null || columnNames.length == 0) {
                throw new BeanFactoryException("First line of file must contain column names: file="+csvFile);
            }

            ArrayList<String> answer = new ArrayList<String>();
            for (int i=0; i<columnNames.length; i++) {
                String name = columnNames[i];
                if (name.length() < 1) throw new BeanFactoryException("Column name has zero length: column="+(i+1)+", file="+csvFile);
                boolean isPrimaryKeyProp = (name.charAt(0)=='*');
                if (isPrimaryKeyProp) {
                    name = name.substring(1);
                    if (name.length() < 1) throw new BeanFactoryException("Primary key column name has zero length: column="+(i+1)+", file="+csvFile);
                    if (!name.contains(Property.META_SEPARATOR)) {
                        if (!answer.contains(name)) answer.add(name);
                    } else {
                        String prefix = name.substring(0,name.indexOf(Property.META_SEPARATOR));
                        if (!answer.contains(prefix)) answer.add(prefix);
                    }
                }
            }

            cr.close();
            fr.close();

            return answer.toArray(new String[answer.size()]);
        } catch (Exception e) {
            throw new BeanFactoryException(e);
        }
    }
}

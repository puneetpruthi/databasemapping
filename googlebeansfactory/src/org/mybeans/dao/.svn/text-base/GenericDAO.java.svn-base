package org.mybeans.dao;

import java.io.File;
import java.lang.reflect.Method;

import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.BeanTable;
import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;

public class GenericDAO<B> {
	public static void useJDBC(String jdbcDriverName, String jdbcURL) {
		BeanTable.useJDBC(jdbcDriverName,jdbcURL);
	}

	public static void useJDBC(String jdbcDriverName, String jdbcURL, String user, String password) {
		BeanTable.useJDBC(jdbcDriverName,jdbcURL,user,password);
	}
	
	public static void useCSVFiles(File directory) {
		BeanTable.useCSVFiles(directory);
	}
	
	public static void useCSVFiles(File directory, int backupsToKeep) {
		BeanTable.useCSVFiles(directory,backupsToKeep);
	}

	private BeanTable<B>   table;
	private BeanFactory<B> factory;
	private String[]       primaryKeyPropertyNames;
	private boolean        autoIncrementOnCreate = false;
	
	public GenericDAO(BeanTable<B> table, String...primaryKeyPropertyNames) {
		this.table = table;
        
        if (!table.exists()) table.create(primaryKeyPropertyNames);

	    factory = table.getFactory();
	    this.primaryKeyPropertyNames = primaryKeyPropertyNames;
	}

	public GenericDAO(Class<B> beanClass, String tableName, String...primaryKeyPropertyNames) {
		this(beanClass,tableName,primaryKeyPropertyNames,new GenericDAO<?>[0]);
	}

	public GenericDAO(Class<B> beanClass, String tableName, String[] primaryKeyPropertyNames, GenericDAO<?>[] referencedBeanDAOs) {
		BeanFactory<?>[] refFactories = new BeanFactory<?>[referencedBeanDAOs.length];
		for (int i=0; i<refFactories.length; i++) {
			refFactories[i] = referencedBeanDAOs[i].getFactory();
		}

		table = BeanTable.getInstance(beanClass,tableName,refFactories);
        
        if (!table.exists()) table.create(primaryKeyPropertyNames);

	    factory = table.getFactory();
	    this.primaryKeyPropertyNames = primaryKeyPropertyNames.clone();
	}
	
	public void setIdleConnectionCleanup(boolean enable) {
		table.setIdleConnectionCleanup(enable);
	}
	
	public void setUseAutoIncrementOnCreate(boolean enable) {
		if (primaryKeyPropertyNames.length != 1) {
			throw new UnsupportedOperationException("Auto-increment only works if there are one primary key column");
		}
		
		Class<B> beanClass = table.getBeanClass();
		String getterName = "get" + Character.toUpperCase(primaryKeyPropertyNames[0].charAt(0)) + primaryKeyPropertyNames[0].substring(1);
		
		try {
			Method getter = beanClass.getMethod(getterName, new Class<?>[0]);
			Class<?> returnType = getter.getReturnType();
			
			if (returnType == int.class) {
				autoIncrementOnCreate = enable;
				return;
			}
			
			if (returnType == long.class) {
				autoIncrementOnCreate = enable;
				return;
			}
			
			throw new UnsupportedOperationException("Auto-increment only works if the primary key column type is int or long.");
		} catch (NoSuchMethodException e) {
			throw new BeanFactoryException("Could not access getter method: "+getterName+"()");
		}
	}

	public B create(B bean) throws DAOException {
		try {
			Transaction.begin();
			
			B dbBean;
			if (autoIncrementOnCreate) {
				dbBean = factory.create();
			} else {
				Object[] pkValues = factory.getPrimaryKeyValues(bean);
				dbBean = factory.create(pkValues);
			}
			
			factory.copyInto(bean, dbBean);
			Transaction.commit();
			return dbBean;
			
		} catch (RollbackException e) {
			throw new DAOException(e);
		} finally {
			if (Transaction.isActive()) Transaction.rollback();
		}
	}
	
	public void createOrUpdate(B bean) throws DAOException {
		try {
			Transaction.begin();

			Object[] pkValues = factory.getPrimaryKeyValues(bean);
			B dbBean = factory.lookup(pkValues);

			if (dbBean == null) dbBean = factory.create(pkValues);
			
			factory.copyInto(bean,dbBean);
			Transaction.commit();
		} catch (RollbackException e) {
			throw new DAOException(e);
		} finally {
			if (Transaction.isActive()) Transaction.rollback();
		}
	}
	
	public void delete(Object...primaryKeyValues) throws DAOException {
		try {
			factory.delete(primaryKeyValues);
		} catch (RollbackException e) {
			throw new DAOException(e);
		}
	}
	
	
	public B[] getAll() throws DAOException {
		try {
			return factory.match();
		} catch (RollbackException e) {
			throw new DAOException(e);
		}
	}

	public int getCount() throws DAOException {
		try {
			return factory.getBeanCount();
		} catch (RollbackException e) {
			throw new DAOException(e);
		}
	}
	
	protected BeanFactory<B> getFactory() { return factory; }
	protected BeanTable<B>   getTable()   { return table;   }
	
	public B lookup(Object...primaryKeyValues) throws DAOException {
		try {
			return factory.lookup(primaryKeyValues);
		} catch (RollbackException e) {
			throw new DAOException(e);
		}
	}
	
	public void update(B bean) throws DAOException {
		try {
			Transaction.begin();
			Object[] pkValues = factory.getPrimaryKeyValues(bean);
			B dbBean = factory.lookup(pkValues);
			factory.copyInto(bean, dbBean);
			Transaction.commit();
		} catch (RollbackException e) {
			throw new DAOException(e);
		} finally {
			if (Transaction.isActive()) Transaction.rollback();
		}
	}
}

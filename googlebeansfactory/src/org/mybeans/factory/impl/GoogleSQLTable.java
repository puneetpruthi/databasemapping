/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.BeanTable;
import org.mybeans.nonmodifiable.NMDate;
import org.mybeans.nonmodifiable.NMSQLDate;
import org.mybeans.nonmodifiable.NMTime;


public class GoogleSQLTable<B> extends BeanTable<B> {
    protected static final int MAX_STRING_LEN = 255;
	protected static final String ARRAY_POS_COLUMN_NAME = Property.META_SEPARATOR+"pos"+Property.META_SEPARATOR;

    // For debugging
    private Writer printSQL = null;

    // Initialized by constructor
    private Class<B>       beanClass;
    private String         tableName;
    private ConnectionPool connectionPool;
    private AbstractFactory<?>[] referencedFactories;

    private GoogleSQLFactory<B> factory = null;

    // Constructors

	public GoogleSQLTable(
            Class<B> beanClass,
            String tableName,
            String jdbcDriver,
			String jdbcURL,
            String user,
            String password,
			AbstractFactory<?>[] referencedFactories)
	{
		// Check for null values and throw here (it's less confusing for the caller)
		if (beanClass  == null) throw new NullPointerException("beanClass");
		if (tableName  == null) throw new NullPointerException("tableName");
		if (jdbcDriver == null) throw new NullPointerException("jdbcDriver");
		if (jdbcURL    == null) throw new NullPointerException("jdbcURL");
		// User and password can be null
		
		this.beanClass = beanClass;
        this.tableName = tableName.toLowerCase();
        this.referencedFactories = referencedFactories;
                
		try {
			connectionPool = ConnectionPool.getInstance(jdbcDriver,jdbcURL,user,password);
		} catch (ConnectionException e) {
			throw new BeanFactoryException(e);
		}
    }

    // Public instance methods (that implement super class abstract methods)

    public synchronized void create(String... primaryKeyNames) {
        PrimaryKeyInfo<B> primaryKeyInfo = PrimaryKeyInfo.getInstance(beanClass,primaryKeyNames,referencedFactories);
        Property[] primaryKeyProperties = primaryKeyInfo.getProperties();
        Property[] properties = Property.deriveProperties(beanClass,primaryKeyInfo,referencedFactories);

        String primaryKeyColumnNamesCommaSeparated = appendColumnNamesCommaSeparated(new StringBuffer(),primaryKeyInfo.getProperties()).toString();

        // Make main table
        StringBuffer b = new StringBuffer();
        b.append("create table ");
        b.append(tableName);
        b.append(" (");
        for (int i=0; i<properties.length; i++) {
            if (i > 0) b.append(", ");
            Property prop = properties[i];
            if (prop.isPrimaryKeyProperty() && prop.getType() == int.class && primaryKeyInfo.getProperties().length == 1) {
                b.append(prop.getName());
                b.append(" INT NOT NULL AUTO_INCREMENT");
            } else if (prop.isPrimaryKeyProperty() && prop.getType() == long.class && primaryKeyInfo.getProperties().length == 1) {
                b.append(prop.getName());
                b.append(" BIGINT NOT NULL AUTO_INCREMENT");
            } else if (prop.isArray()) {
                b.append(prop.getName());
                b.append(" BOOLEAN NOT NULL DEFAULT 0");
            } else {
                String[]   columnNames = prop.getColumnNames();
                Class<?>[] columnTypes = prop.getColumnTypes();
                int[]      columnStrLens = prop.getColumnMaxStrLens();
                for (int j=0; j<columnNames.length; j++) {
                    if (j > 0) b.append(", ");
                    b.append(columnNames[j]);
                    b.append(' ');
                    b.append(javaToSql(columnTypes[j],prop,columnStrLens[j]));
                }
            }
        }

        if (primaryKeyProperties.length > 0) {
	        b.append(", PRIMARY KEY(");
	        b.append(primaryKeyColumnNamesCommaSeparated);
	        b.append(')');
        }
        
        b.append(')');
        
        Connection con;
        try {
            con = connectionPool.getConnection();
        } catch (ConnectionException e) {
            throw new BeanFactoryException(e);
        }

        try {
            Statement stmt = con.createStatement();
            if (printSQL != null) printDebug("createTable: "+b);
            stmt.executeUpdate(b.toString());
            stmt.close();
        } catch (SQLException e) {
            try { con.close(); } catch (SQLException e2) { /* ignore */ }
            throw new BeanFactoryException("Error creating table \""+tableName+"\": "+e.getMessage());
        }

        // If there are any array columns, make auxiliary tables to hold their data
        for (Property prop : properties) {
            if (prop.isArray()) {
                try {
                    Statement stmt = con.createStatement();
                    StringBuffer sql = new StringBuffer();
                    sql.append("CREATE TABLE ").append(tableName).append('_');
                    sql.append(prop.getName().toLowerCase()).append(" (");
                    for (int i=0; i<primaryKeyProperties.length; i++) {
                        if (i > 0) sql.append(", ");
                        String[]   priKeyColumnNames = primaryKeyProperties[i].getColumnNames();
                        Class<?>[] priKeyColumnTypes = primaryKeyProperties[i].getColumnTypes();
                        int[]      priKeyColStrLens  = primaryKeyProperties[i].getColumnMaxStrLens();
                        for (int j=0; j<priKeyColumnNames.length; j++) {
                            if (j > 0) sql.append(", ");
                            sql.append(priKeyColumnNames[j]);
                            sql.append(' ');
                            sql.append(javaToSql(priKeyColumnTypes[j],primaryKeyProperties[i],priKeyColStrLens[j]));
                        }
                    }
                    sql.append(", ");
                    sql.append(ARRAY_POS_COLUMN_NAME);
                    sql.append(" INTEGER NOT NULL");
                    String[]   columnNames = prop.getColumnNames();
                    Class<?>[] columnTypes = prop.getColumnTypes();
                    int[]      columnStrLens = prop.getColumnMaxStrLens();
                    for (int j=0; j<columnNames.length; j++) {
                        sql.append(", ");
                        sql.append(columnNames[j]);
                        sql.append(' ');
                        sql.append(javaToSql(columnTypes[j],prop,columnStrLens[j]));
                    }
                    sql.append(", PRIMARY KEY(");
                    sql.append(primaryKeyColumnNamesCommaSeparated);
                    sql.append(',');
                    sql.append(ARRAY_POS_COLUMN_NAME);
                    sql.append("))");
                    if (printSQL != null) printDebug("createTable: "+sql);
                    stmt.executeUpdate(sql.toString());
                    stmt.close();
                } catch (SQLException e) {
                    try { con.close(); } catch (SQLException e2) { /* ignore */ }
                    throw new BeanFactoryException("Error creating auxiliary table \""+tableName+'_'+prop.getName()+"\": "+e.getMessage());
                }
            }
        }
        connectionPool.releaseConnection(con);
    }

    public synchronized void createSecondaryIndex(String...secondaryKeyNames) {
        throw new UnsupportedOperationException();
    }

    public synchronized void delete() {
        // Drop main table and any other tables with the main table prefix

        // First get a list of the tables to drop
        ArrayList<String> tablesToDrop = new ArrayList<String>();

        Connection con;
        try {
            con = connectionPool.getConnection();
        } catch (ConnectionException e) {
            throw new BeanFactoryException(e);
        }

        try {
            Statement stmt = con.createStatement();
            if (printSQL != null) printDebug("deleteTable: SHOW TABLES");
            ResultSet rs = stmt.executeQuery("SHOW TABLES");

            while (rs.next()) {
                String s = rs.getString(1);
                if (File.separatorChar == '\\') {
                    // It's windows...case insensitive matching
                    String lower = s.toLowerCase();
                    if (lower.equalsIgnoreCase(tableName)) tablesToDrop.add(s);
                    if (lower.startsWith(tableName.toLowerCase()+"_")) tablesToDrop.add(s);
                } else {
                    // It's Unix...case counts
                    if (s.equals(tableName)) tablesToDrop.add(s);
                    if (s.startsWith(tableName+"_")) tablesToDrop.add(s);
                }
            }

            rs.close();
            stmt.close();

            for (String name : tablesToDrop) {
                stmt = con.createStatement();
                String sql = "DROP TABLE "+name;
                if (printSQL != null) printDebug("deleteTable: "+sql);
                stmt.executeUpdate(sql);
                stmt.close();
            }

            connectionPool.releaseConnection(con);
        } catch (SQLException e) {
            try { con.close(); } catch (SQLException e2) {  }
            throw new BeanFactoryException(e);
        }
    }

    public Class<B> getBeanClass() { return beanClass; }

    public synchronized BeanFactory<B> getFactory() {
        if (factory != null) return factory;
        String[] priKeyNames = getPrimaryKeyNamesFromTable();
        factory = new GoogleSQLFactory<B>(beanClass,tableName,priKeyNames,connectionPool,printSQL,referencedFactories);
        factory.setDebugOutput(printSQL);

        for (Property p : factory.properties) {
        	if (p instanceof SelfReferencedBeanProperty) {
        		SelfReferencedBeanProperty selfRefProp = (SelfReferencedBeanProperty) p;
        		selfRefProp.setFactory(factory);
        	}
        }

        return factory;

    }

    public synchronized void setDebugOutput(Writer writer) {
        printSQL = writer;
        if (factory != null) factory.setDebugOutput(printSQL);
    }

    public synchronized void setIdleConnectionCleanup(boolean enable) {
        connectionPool.setIdleConnectionCleanup(enable);
    }

    public boolean exists() {
        // Returns true if main table exists.
        // If main table exists, but aux tables are missing, then the world is inconsistent
        // ...you'll get an error during creation allowing admin to examine existing tables before dropping them.
        // Likewise if main table doesn't exist, but aux tables do.

        Connection con;
        try {
            con = connectionPool.getConnection();
        } catch (ConnectionException e) {
            throw new BeanFactoryException(e);
        }

        try {
            Statement stmt = con.createStatement();
            if (printSQL != null) printDebug("tableExists("+tableName+"): SHOW TABLES");
            ResultSet rs = stmt.executeQuery("SHOW TABLES");

            boolean answer = false;
            while (rs.next() && !answer) {
                String s = rs.getString(1);
                if (tableName.equalsIgnoreCase(s)) answer = true;
            }

            stmt.close();
            connectionPool.releaseConnection(con);

            return answer;
        } catch (SQLException e) {
            try { con.close(); } catch (SQLException e2) {  }
            throw new BeanFactoryException(e);
        }
    }

    // Private instance methods

    private StringBuffer appendColumnNamesCommaSeparated(StringBuffer sql, Property[] props) {
        for (int i=0; i<props.length; i++) {
            if (i > 0) sql.append(", ");
            Property prop = props[i];
            if (prop.isArray()) {
                sql.append(prop.getName());
            } else {
                String[] columnNames = props[i].getColumnNames();
                for (int j=0; j<columnNames.length; j++) {
                    if (j > 0) sql.append(", ");
                    sql.append(columnNames[j]);
                }
            }
        }
        return sql;
    }

    private String[] getPrimaryKeyNamesFromTable() {
        Connection con;
        try {
            con = connectionPool.getConnection();
        } catch (ConnectionException e) {
            throw new BeanFactoryException(e);
        }

        try {
            Statement stmt = con.createStatement();
            String sql = "DESCRIBE "+tableName;
            if (printSQL != null) printDebug("getPrimaryKeyNamesFromTable: "+sql);
            ResultSet rs = stmt.executeQuery(sql);

            ArrayList<String> list = new ArrayList<String>();
            while (rs.next()) {
                String name = rs.getString(1);
                // String sqlType = rs.getString(2);
                // boolean nonNull = !rs.getBoolean(3);
                boolean primaryKey = rs.getString(4).equalsIgnoreCase("PRI");
                if (primaryKey) {
                    if (!name.contains(Property.META_SEPARATOR)) {
                        list.add(name);
                    } else {
                        String prefix = name.substring(0,name.indexOf(Property.META_SEPARATOR));
                        if (!list.contains(prefix)) list.add(prefix);
                    }
                }
            }

            stmt.close();
            connectionPool.releaseConnection(con);

// Don't check anymore now that tables with only one row and no primary key are allowed.
//          if (list.size() == 0) throw new BeanFactoryException("Could not find any primary key in table: "+tableName);

            return list.toArray(new String[list.size()]);
        } catch (SQLException e) {
            try { con.close(); } catch (SQLException e2) {  }
            throw new BeanFactoryException(e);
        }
    }

    private String javaToSql(Class<?> javaType, Property prop, int maxStringLength) {
        StringBuffer sql = new StringBuffer();

        // Types that in Java default to NULL
        
        if (javaType.isEnum())                sql.append("VARCHAR(").append(maxStringLength).append(")");

        if (javaType == String.class)         sql.append("VARCHAR(").append(maxStringLength).append(")");

        if (javaType == java.sql.Date.class)  sql.append("DATE");
        if (javaType == java.util.Date.class) sql.append("DATETIME");
        if (javaType == java.sql.Time.class)  sql.append("TIME");

        if (javaType == NMSQLDate.class)      sql.append("DATE");
        if (javaType == NMDate.class)         sql.append("DATETIME");
        if (javaType == NMTime.class)         sql.append("TIME");

        if (javaType == byte[].class)         sql.append("LONGBLOB");

        if (sql.length() > 0) {
            if (prop.isPrimaryKeyProperty()) sql.append(" NOT NULL");
            return sql.toString();
        }

        // Types that in Java default to NOT NULL DEFAULT 0

        if (javaType == boolean.class)        sql.append("TINYINT");
        if (javaType == double.class)         sql.append("DOUBLE");
        if (javaType == float.class)          sql.append("FLOAT");
        if (javaType == int.class)            sql.append("INT");
        if (javaType == long.class)           sql.append("BIGINT");

        if (sql.length() > 0) {
            if (prop.isPrimaryKeyProperty()) {
                sql.append(" NOT NULL");
            } else if (prop instanceof ReferencedBeanProperty) {
                // sql.append(" DEFAULT NULL"); <= this is the default
            } else {
                sql.append(" NOT NULL DEFAULT 0");
            }
            return sql.toString();
        }

        throw new BeanFactoryException("Cannot map Java type: "+javaType.getCanonicalName());
    }
    
	private void printDebug(String s) {
		String line = Thread.currentThread().getName()+": "+getClass().getSimpleName()+": "+s+"\n";
		try {
			printSQL.write(line);
			printSQL.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
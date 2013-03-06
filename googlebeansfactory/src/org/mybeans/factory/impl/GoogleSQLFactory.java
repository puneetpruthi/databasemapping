/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory.impl;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.DuplicateKeyException;
import org.mybeans.factory.MatchArg;
import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;
import org.mybeans.nonmodifiable.NMDate;
import org.mybeans.nonmodifiable.NMSQLDate;
import org.mybeans.nonmodifiable.NMTime;

public class GoogleSQLFactory<B> extends AbstractFactory<B> implements OutcomeListener {
	private static final String ARRAY_POS_COLUMN_NAME = Property.META_SEPARATOR+"pos"+Property.META_SEPARATOR;
	private static final Object[] ZERO_LEN_ARRAY = new Object[0];
	private static ThreadLocal<Connection> threadConnection = new ThreadLocal<Connection>();
	private static ThreadLocal<ArrayList<GoogleSQLFactory<?>>> threadInvolvedGoogleSQLFactories =  new ThreadLocal<ArrayList<GoogleSQLFactory<?>>>();

    // For debugging
    public Writer printSQL;

    // Initialized by constructor
    private ConnectionPool connectionPool;
    private String  tableName;
    private boolean containsArrayFields;
    private String  primaryKeyColumnNamesAndQuestions;
    private String  primaryKeyColumnNamesCommasQuestions;
    private String  primaryKeyColumnNamesCommaSeparated;
    private String  primaryKeyQuestionsCommaSeparated;

	private ThreadLocal<Map<PrimaryKey<B>,BeanTrackerRec<B>>> threadTrackedBeans = new ThreadLocal<Map<PrimaryKey<B>,BeanTrackerRec<B>>>();

    // Constructors

	public GoogleSQLFactory(
            Class<B>         beanClass,
            String           tableName,
            String[]         primaryKeyNames,
            ConnectionPool   connectionPool,
            Writer           printSQL,
			AbstractFactory<?>[] referencedFactories) {
		super(beanClass,primaryKeyNames,referencedFactories);
		this.tableName = tableName.toLowerCase();
        this.connectionPool = connectionPool;
        this.printSQL = printSQL;
        
        primaryKeyColumnNamesAndQuestions    = appendColumnNamesSeparatorsQuestions(new StringBuffer(),primaryKeyProperties," AND ").toString();
        primaryKeyColumnNamesCommasQuestions = appendColumnNamesSeparatorsQuestions(new StringBuffer(),primaryKeyProperties,",").toString();
        primaryKeyColumnNamesCommaSeparated  = appendColumnNamesCommaSeparated(new StringBuffer(),primaryKeyProperties).toString();
        primaryKeyQuestionsCommaSeparated    = appendColumnQuestionsCommaSeparated(new StringBuffer(),primaryKeyProperties).toString();

        containsArrayFields = false;
        for (Property p : properties) {
            if (p.isArray()) containsArrayFields = true;
        }

        validateTable();
    }

    // Public instance methods

    public B create(Object... primaryKeyValues) throws RollbackException {
        if (!TranImpl.isActive()) {
            // Need to run this in a transaction so that we can (generate the key
            // if auto increment and then) instantiate the bean and then
            // copy the non-default values out to the DB at commit time.
            Transaction.begin();
            B answer = create(primaryKeyValues);
            Transaction.commit();
            return answer;
        }

        boolean autoIncrement;
        if (primaryKeyValues.length == 0 && primaryKeyProperties.length == 1 &&
                (primaryKeyProperties[0].getType() == int.class || primaryKeyProperties[0].getType() == long.class)) {
            autoIncrement = true;
        } else {
            autoIncrement = false;
            validatePrimaryKeyValues(primaryKeyValues); // throws RollbackException in case of problems
        }

        Connection con = join();                        // throws RollbackException in case of problems
        
        try {
        	if (primaryKeyProperties.length == 0 && getBeanCount() > 0) {
        		myRollbackAndThrow(con, new DuplicateKeyException("This is a one row table (because there are no primary keys) and the row already exists"));
        	}
        	
            String sql = "INSERT INTO " + tableName + " (" + primaryKeyColumnNamesCommaSeparated +
            	") values (" + primaryKeyQuestionsCommaSeparated + ")";
            if (printSQL != null) printDebug("create: "+sql);
            PreparedStatement pstmt = con.prepareStatement(sql);
            Object[] priKeyDBValues;
            if (autoIncrement) {
                priKeyDBValues = null;
                pstmt.setObject(1,null);  // Set to null for auto_increment
            } else {
                priKeyDBValues = DBValues.makeDBValues(primaryKeyInfo.getProperties(),primaryKeyValues);
                pstmtSetDBValues(pstmt,1,primaryKeyInfo.getProperties(),priKeyDBValues);
            }
            pstmt.executeUpdate();
            pstmt.close();

            if (autoIncrement) {
                Statement stmt = con.createStatement();
                if (printSQL != null) printDebug("create: SELECT LAST_INSERT_ID()");
                ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
                rs.next();
                Object id = rs.getObject("LAST_INSERT_ID()");
                if (printSQL != null) printDebug("create: ...LAST_INSERT_ID()="+id);
                stmt.close();

                if (primaryKeyInfo.getProperties()[0].getType() == long.class) {
                    priKeyDBValues = new Object[] { id };
                } else {
                    long x = (Long) id;
                    int i = (int) x;
                    priKeyDBValues = new Object[] { i };
                }
            }

            // Always runs in a transaction, so track the bean, don't close the connection
            B bean = newBean(priKeyDBValues);
            PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,priKeyDBValues);
            threadTrackedBeans.get().put(key,new BeanTrackerRec<B>(key,bean,null));
            return bean;
        } catch (SQLException e) {
            RollbackException re;
            if (e.getMessage().startsWith("Duplicate")) {
                re = new DuplicateKeyException(e.getMessage());
            } else {
                re = new RollbackException(e);
            }
            myRollbackAndThrow(con,re);
            throw new AssertionError("executeRollback returned");
        } catch (RollbackException e) {
        	throw e;
        } catch (Exception e) {
            myRollbackAndThrow(con,e);
            throw new AssertionError("executeRollback returned");
        }
    }

    public void delete(Object... primaryKeyValues) throws RollbackException {
        validatePrimaryKeyValues(primaryKeyValues);   // throws RollbackException if problems
        Connection con = join();                      // throws RollbackException if problems

        try {
            if (containsArrayFields && !TranImpl.isActive()) con.setAutoCommit(false);

            String whereClause = " WHERE " + primaryKeyColumnNamesAndQuestions;
            String sql = "DELETE FROM "+tableName+whereClause;
            if (printSQL != null) printDebug("delete: "+sql);
            PreparedStatement pstmt = con.prepareStatement(sql);

            Object[] priKeyDBValues = DBValues.makeDBValues(primaryKeyInfo.getProperties(),primaryKeyValues);
            pstmtSetDBValues(pstmt,1,primaryKeyInfo.getProperties(),priKeyDBValues);
            int num = pstmt.executeUpdate();
            pstmt.close();

            if (num != 1) {
                StringBuffer b = new StringBuffer();
                for (int i=0; i<primaryKeyValues.length; i++) {
                    if (i > 0) b.append(",");
                    b.append(primaryKeyValues[i]);
                }
                if (num == 0) myRollbackAndThrow(con,new RollbackException("No row with primary key = \""+b+"\"."));
                myRollbackAndThrow(con,new RollbackException("AssertionError: There are "+num+" rows with primary key = \""+b+"\"."));
            }

            // Delete any array values stored in auxiliary tables
            for (Property prop : properties) {
                if (prop.isArray()) {
                    sql = "DELETE FROM "+tableName+'_'+prop.getName().toLowerCase()+whereClause;
                    if (printSQL != null) printDebug("delete: "+sql);
                    pstmt = con.prepareStatement(sql);
                    pstmtSetDBValues(pstmt,1,primaryKeyInfo.getProperties(),priKeyDBValues);
                    pstmt.executeUpdate();
                    pstmt.close();
                }
            }

            if (TranImpl.isActive()) {
                // In this implementation deleted beans are "untracked" as they are deleted in the DB in this method
                PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,priKeyDBValues);
                threadTrackedBeans.get().remove(key);
            } else {
                if (containsArrayFields) {
                    con.commit();
                    con.setAutoCommit(true);
                }
	            if (printSQL != null) printDebug("delete: releasing connection: "+con);
                connectionPool.releaseConnection(con);
            }
        } catch (SQLException e) {
            myRollbackAndThrow(con,e);
        } catch (RollbackException e) {
        	throw e;
        } catch (Exception e) {
        	myRollbackAndThrow(con,e);
        }
    }

    public B lookup(Object... primaryKeyValues) throws RollbackException {
        validatePrimaryKeyValues(primaryKeyValues);   // throws RollbackException in case of problems

        try {
	        MatchArg[] matchArgs = new MatchArg[primaryKeyProperties.length];
	        for (int i=0; i<primaryKeyProperties.length; i++) {
	            matchArgs[i] = MatchArg.equals(primaryKeyProperties[i].getName(),primaryKeyValues[i]);
	        }
	
	        B[] list = match(matchArgs);
	        if (list.length == 0) return null;
	        if (list.length == 1) return list[0];
	
	        StringBuffer b = new StringBuffer();
	        for (int i=0; i<primaryKeyValues.length; i++) {
	            if (i>0) b.append(',');
	            b.append(primaryKeyValues[i]);
	        }
	        TranImpl.rollbackAndThrow("AssertionError: "+list.length+" records with same primary key: "+b);
	        throw new AssertionError("rollbackAndThrow returned");
        } catch (RollbackException e) {
        	throw e;
        } catch (Exception e) {
	        TranImpl.rollbackAndThrow(e);
	        throw new AssertionError("rollbackAndThrow returned");
        }
    }

    public B[] match(MatchArg...constraints) throws RollbackException {
        MatchArgTree sepMatchArgs = MatchArgTree.buildTree(properties,MatchArg.and(constraints)); // throws RollbackException in case of problems
        if (!TranImpl.isActive() && sepMatchArgs.containsMaxOrMin()) {
        	// If we have a max or min, we must do match in a transaction so we can
        	// first fetch max and min values and then match the binary constraints
            Transaction.begin();
            B[] answer = sqlMatch(sepMatchArgs);  // throws RollbackException in case of problems
            Transaction.commit();
            return answer;
        }

        return sqlMatch(sepMatchArgs);           // throws RollbackException in case of problems
    }

    public int getBeanCount() throws RollbackException {
        Connection con = join();

        try {
            Statement stmt = con.createStatement();
            String sql = "SELECT COUNT(*) FROM "+tableName;
            if (printSQL != null) printDebug("getBeanCount: "+sql);
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            int answer = rs.getInt(1);
            stmt.close();

            if (TranImpl.isActive()) return answer;
            
            if (printSQL != null) printDebug("getBeanCount: releasing connection: "+con);
            connectionPool.releaseConnection(con);
            return answer;
        } catch (Exception e) {
            myRollbackAndThrow(con,e);
            throw new AssertionError("executeRollback returned");
        }
    }

    protected void setDebugOutput(Writer writer) {
        printSQL = writer;
    }

    // Private instance methods

    private StringBuffer appendNonArrayColumnNamesAndQuestions(StringBuffer sql, Property prop, String separator) {
        String[] columnNames = prop.getColumnNames();
        for (int i=0; i<columnNames.length; i++) {
            if (i > 0) sql.append(separator);
            sql.append(columnNames[i]);
            sql.append("=?");
        }

        return sql;
    }

    private StringBuffer appendColumnNamesAndQuestions(StringBuffer sql, Property prop, String separator) {
        if (prop.isArray()) {
            sql.append(prop.getName());
            sql.append("=?");
            return sql;
        }

        return appendNonArrayColumnNamesAndQuestions(sql,prop,separator);
    }

    private StringBuffer appendColumnNamesSeparatorsQuestions(StringBuffer sql, Property[] props, String separator) {
        for (int i=0; i<props.length; i++) {
            if (i > 0) sql.append(separator);
            appendColumnNamesAndQuestions(sql,props[i],separator);
        }
        return sql;
    }

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

    private StringBuffer appendColumnQuestionsCommaSeparated(StringBuffer sql, Property[] props) {
        for (int i=0; i<props.length; i++) {
            if (i > 0) sql.append(',');
            Property prop = props[i];
            if (prop.isArray()) {
                sql.append('?');
            } else {
                String[] columnNames = props[i].getColumnNames();
                for (int j=0; j<columnNames.length; j++) {
                    if (j > 0) sql.append(',');
                    sql.append('?');
                }
            }
        }
        return sql;
    }

    private static class Column {
        String  name;
        String  sqlType;
        boolean isNonNull;
        boolean isPrimaryKey;
        int     position;
    }

    private static class ColumnList {
        ArrayList<Column> list = new ArrayList<Column>();

        ColumnList(ResultSet rs) throws SQLException {
            int pos = 0;
            while (rs.next()) {
                Column c = new Column();
                c.name = rs.getString(1);
                c.sqlType = rs.getString(2);
                c.isNonNull = !rs.getBoolean(3);
                c.isPrimaryKey = rs.getString(4).equalsIgnoreCase("PRI");
                pos++;
                c.position = pos;
                list.add(c);
            }
        }

        Iterator<Column> iterator() { return list.iterator(); }
    }

    private void validateTable() {
        Connection con;
        try {
            con = connectionPool.getConnection();
        } catch (ConnectionException e) {
            throw new BeanFactoryException(e);
        }

        try {
            // So many possible errors, catch BeanFactoryExceptions below, close con and then re-throw
            Statement stmt = con.createStatement();
            String sql = "DESCRIBE "+tableName;
            ResultSet rs = stmt.executeQuery(sql);
            ColumnList columnList = new ColumnList(rs);
            rs.close();
            stmt.close();
            Iterator<Column> columnIter = columnList.iterator();

            for (Property prop : properties) {
                if (prop.isArray()) {
                    if (!columnIter.hasNext()) throw new BeanFactoryException("Table="+tableName+" is missing column: "+prop.getName()+" that backs "+prop);
                    Column column = columnIter.next();
                    Class<?> dbType = sqlToJava(column.sqlType);
                    if (dbType != boolean.class) throw new BeanFactoryException("Table="+tableName+", column="+column.name+": incorrect database type for array support: "+column.sqlType+" (should be BOOLEAN NOT NULL)");
                    if (!column.isNonNull) throw new BeanFactoryException("Table="+tableName+", column="+column.name+": incorrect database type for array support: must be non-null");
                    if (column.isPrimaryKey) throw new BeanFactoryException("Table="+tableName+", column="+column.name+": incorrect database type for array support: cannot be part of the primary key");
                } else {
                    String[]   columnNames = prop.getColumnNames();
                    Class<?>[] columnTypes = prop.getColumnTypes();
                    for (int i=0; i<columnNames.length; i++) {
                        if (!columnIter.hasNext()) throw new BeanFactoryException("Table="+tableName+" is missing column: "+columnNames[i]+" that backs "+prop);
                        Column column = columnIter.next();
                        if (!column.name.equals(columnNames[i])) {
                            throw new BeanFactoryException("Column #"+column.position+" should have name "+columnNames[i]+" (but is instead "+column.name+")");
                        }


                        if (prop.isPrimaryKeyProperty() && !column.isPrimaryKey) {
                            throw new BeanFactoryException("Table "+tableName+" does not indicate column \""+column.name+"\" as part of the primary key (and it should)");
                        }

                        if (column.isPrimaryKey && !(prop.isPrimaryKeyProperty())) {
                            throw new BeanFactoryException("Table "+tableName+" does indicates column \""+column.name+"\" as part of the primary key (and it should not)");
                        }

                        Class<?> dbType = sqlToJava(column.sqlType);
                        if (dbType == null) throw new BeanFactoryException("Table="+tableName+", "+column.name+": do not know how to map this database type: "+column.sqlType);
                        if (!typeMatch(dbType,columnTypes[i])) throw new BeanFactoryException("Table="+tableName+", column="+column.name+": bean & DB types do not match: beanType="+columnTypes[i]+", DBType="+dbType);

                        if (column.isPrimaryKey) {
                            if(!column.isNonNull) throw new BeanFactoryException("Table="+tableName+", "+column.name+": database column allows nulls for this type (and should not because it's part of the primary key)");
                        } else if (prop instanceof ReferencedBeanProperty) {
                            if (column.isNonNull) throw new BeanFactoryException("Table="+tableName+", "+column.name+": database column does not allow nulls for this type (and should because it's part of a non-primary key bean reference)");
                        } else if (prop.getDefaultValue() == null && column.isNonNull) {
                            throw new BeanFactoryException("Table="+tableName+", "+column.name+": database column does not allow nulls for this type (and should because of it's type: "+dbType+")");
                        }
                    }
                }
            }

            if (columnIter.hasNext()) {
                Column column = columnIter.next();
                throw new BeanFactoryException("Column ("+column.name+") without corresponding bean property");
            }

            for (Property prop : properties) {
                if (prop.isArray()) {
                    stmt = con.createStatement();
                    String sql2 = "DESCRIBE "+tableName+'_'+prop.getName().toLowerCase();
//                  printDebug("validateTable: "+sql2);
                    rs = stmt.executeQuery(sql2);

                    for (Property priKeyProp : primaryKeyInfo.getProperties()) {
                        String[]   colNames = priKeyProp.getColumnNames();
                        Class<?>[] colTypes = priKeyProp.getColumnTypes();
                        for (int i=0; i<colNames.length; i++) {
                            checkProperty(rs,colNames[i],colTypes[i],true,true,prop.getName());
                        }
                    }
                    checkProperty(rs,ARRAY_POS_COLUMN_NAME,int.class,true,true,prop.getName());

                    String[]   colNames = prop.getColumnNames();
                    Class<?>[] colTypes = prop.getColumnTypes();
                    for (int i=0; i<colNames.length; i++) {
                        checkProperty(rs,colNames[i],colTypes[i],!prop.isNullable(),false,prop.getName());
                    }

                    if (rs.next()) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+prop.getName()+" has extra column named "+rs.getString(1));

                    rs.close();
                    stmt.close();
                }
            }

            if (printSQL != null) printDebug("validateTable: releasing connection: "+con);
            connectionPool.releaseConnection(con);
        } catch (SQLException e) {
            try { con.close(); } catch (SQLException e2) {  }
            throw new BeanFactoryException(e);
        } catch (BeanFactoryException e) {
            try { con.close(); } catch (SQLException e2) {  }
            throw e;
        }
    }

    private String computeSql(MatchArgTree argTree) {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT * FROM ");
        sql.append(tableName);
        
        String whereTest = computeWhereTest(argTree);
        if (whereTest.length() > 0) {
        	sql.append(" WHERE ");
        	sql.append(whereTest);
        }

        if (TranImpl.isActive()) sql.append(" FOR UPDATE");

        return sql.toString();
    }

    private String computeWhereTest(MatchArgTree argTree) {
        MatchOp op = argTree.getOp();

        if (argTree instanceof MatchArgInternalNode) {
    		MatchArgInternalNode internalNode = (MatchArgInternalNode) argTree;
    		List<MatchArgTree> subNodes = internalNode.getSubNodes();
    		StringBuffer sql = new StringBuffer();
    		for (MatchArgTree subNode : subNodes) {
    			if (sql.length() > 0) {
    				if (op == MatchOp.AND) sql.append(" AND ");
    				if (op == MatchOp.OR)  sql.append(" OR ");
    			}
    			sql.append('(');
    			sql.append(computeWhereTest(subNode));
    			sql.append(')');
    		}
    		return sql.toString();
    	}
	
        if (op == null) return "NULL is not ?";  // op is null when a max or min constraint match any rows
        
		MatchArgLeafNode leaf = (MatchArgLeafNode) argTree;
        String keyName = leaf.getProperty().getName();
        switch (op) {
            case EQUALS:
                return keyName+" <=> ?";
            case GREATER:
                return keyName+" > ?";
            case GREATER_OR_EQUALS:
                return keyName+" >= ?";
            case LESS:
                return keyName+" < ?";
            case LESS_OR_EQUALS:
                return keyName+" <= ?";
            case CONTAINS:
            case STARTS_WITH:
            case ENDS_WITH:
                return keyName+" LIKE BINARY ?";
            case EQUALS_IGNORE_CASE:
            case CONTAINS_IGNORE_CASE:
            case STARTS_WITH_IGNORE_CASE:
            case ENDS_WITH_IGNORE_CASE:
                return keyName+" LIKE ?";
            case MAX:
            case MIN:
                throw new AssertionError(op+" in constraints should have be converted to EQUALS at this point");
            default:
                throw new AssertionError("Unknown op: "+op);
        }
    }

    private void fillArrayDBValues(Connection con, Object[] dbValues) throws SQLException {
        for (int i=0; i<properties.length; i++) {
            if (properties[i].isArray()) {
                boolean containsArrayData = (Boolean) dbValues[i];
                if (!containsArrayData) {
                    dbValues[i] = null;
                } else {
                    String sql = "SELECT * FROM "+tableName+'_'+properties[i].getName().toLowerCase()+
                        " WHERE "+primaryKeyColumnNamesAndQuestions+" ORDER BY "+
                        primaryKeyColumnNamesCommaSeparated+','+ARRAY_POS_COLUMN_NAME;
                    if (printSQL != null) printDebug("fillArrayDBValues: "+sql);
                    PreparedStatement pstmt = con.prepareStatement(sql);
                    Property[] priKeyDBProps = primaryKeyInfo.getProperties();
                    Object[] priKeyDBVals = primaryKeyInfo.getPrimaryKeyDBValues(dbValues);
                    pstmtSetDBValues(pstmt,1,priKeyDBProps,priKeyDBVals);
                    ResultSet rs = pstmt.executeQuery();

                    ArrayList<Object> arrayDBValues = new ArrayList<Object>();
                    while (rs.next()) {
                        Iterator<String> colNameIter = new MyArrayIterator<String>(properties[i].getColumnNames());
                        arrayDBValues.add(loadNonArrayDBValue(properties[i],rs,colNameIter));
                    }

                    dbValues[i] = arrayDBValues.toArray();
                    pstmt.close();
                }
            }
        }
    }

    private void fixDBValuesForPartialStringMatch(MatchArgTree argTree) {
    	Iterator<MatchArgLeafNode> iter = argTree.leafIterator();
    	while (iter.hasNext()) {
    		MatchArgLeafNode arg = iter.next();
    		
    		Object value = arg.getValue();
    		if (value instanceof String) {
    			String strValue = (String) value;
	    		MatchOp op = arg.getOp();
	            switch (op) {
	                case CONTAINS:
	                case CONTAINS_IGNORE_CASE:
	    	        	arg.fixConstraint(op,'%'+strValue+'%');
	                    break;
	                case STARTS_WITH:
	                case STARTS_WITH_IGNORE_CASE:
	    	        	arg.fixConstraint(op,strValue+'%');
	                    break;
	                case ENDS_WITH:
	                case ENDS_WITH_IGNORE_CASE:
	    	        	arg.fixConstraint(op,'%'+strValue);
	                    break;
	                default:
	                    // Do nothing
	            }
    		}
        }
    }

    private void fixMaxMin(MatchArgTree argTree) throws RollbackException {
    	// Max and min matches must be run in a transaction
    	if (!TranImpl.isActive()) throw new AssertionError("Caller should have started a transaction");
    	
    	Iterator<MatchArgLeafNode> iter = argTree.leafIterator();
    	while (iter.hasNext()) {
    		MatchArgLeafNode arg = iter.next();
			MatchOp op = arg.getOp();
			
    		if (op == MatchOp.MAX || op == MatchOp.MIN) {
    			Property prop = arg.getProperty();
    			StringBuffer sql = new StringBuffer();
    			sql.append("select ");
    			if (op == MatchOp.MAX) {
    				sql.append("max(");
    			} else {
    				sql.append("min(");
    			}
    			if (prop.getType() == String.class) sql.append("binary ");
    			sql.append(prop.getName());
    			sql.append(") from ");
    			sql.append(tableName);
    			sql.append(" for update");

    	        Connection con = join();

    	        try {
    	            if (printSQL != null) printDebug("fixMaxMin: "+sql);
    	            Statement stmt = con.createStatement();
    	            ResultSet rs = stmt.executeQuery(sql.toString());
    	            // If now rows in the table, then NULL is returned for max or min operator
    	            if (!rs.next()) throw new AssertionError("No row returned.");
    	            Object matchValue;
	    			if (op == MatchOp.MAX) {
	    				matchValue = rs.getObject("max("+prop.getName()+")");
	    			} else {
	    				matchValue = rs.getObject("min("+prop.getName()+")");
	    			}
    	            stmt.close();


        	        if (matchValue == null) {
        	        	// If there is no match from some max or min op, we set the constraint's op
        	        	// to null.  This causes this constraint to evaluate to false.
        	        	arg.fixConstraint(null,null);
        	        } else {
        	        	arg.fixConstraint(MatchOp.EQUALS,matchValue);
        	        }
    	        } catch (SQLException e) {
    	            myRollbackAndThrow(con,e);
    	            throw new AssertionError("executeRollback returned");
    	        }
    		}
    	}
    }

    private Object loadDBValue(Property prop, ResultSet rs) throws SQLException {
        // If prop.isArray(), then this is the boolean converted in fillArrayDBValues()
        if (prop.isArray()) return rs.getObject(prop.getName());

        Iterator<String> colNameIter = new MyArrayIterator<String>(prop.getColumnNames());
        return loadNonArrayDBValue(prop,rs,colNameIter);
    }

    private Object[] loadDBValues(ResultSet rs) throws SQLException {
        Object[] dbValues = new Object[properties.length];
        for (int i=0; i<properties.length; i++) {
            Property prop = properties[i];
            dbValues[i] = loadDBValue(prop,rs);
        }
        return dbValues;
    }

    private Object loadNonArrayDBValue(Property prop, ResultSet rs, Iterator<String> columnNameIter) throws SQLException {
        // This method is used to load one value from the ResultSet
        // By non-array we mean the prop may be an array, but we aren't checking for that here
        // So if prop.isArray() is true, we ignore it because we are
        if (prop instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
            Property[] priKeyProps = refProp.getRefBeanPrimaryKeyProperties();
            Object[] dbValues = new Object[priKeyProps.length];
            for (int i=0; i<priKeyProps.length; i++) {
            	dbValues[i] = loadNonArrayDBValue(priKeyProps[i],rs,columnNameIter);
            }
            if (dbValues[0] == null) return null;
            return dbValues;
        }

        String name = columnNameIter.next();
        Object dbValue = rs.getObject(name);
        return makeNonModIfNonMod(dbValue,prop);
    }
    
    private void myRollbackAndThrow(Connection con, Exception e) throws RollbackException {
        if (TranImpl.isActive()) TranImpl.rollbackAndThrow(e);

        try { if (con.getAutoCommit()) con.rollback(); } catch (SQLException e2) { /* ignore */ }
        try { con.close(); } catch (SQLException e2) { /* ignore */ }
        TranImpl.rollbackAndThrow(e);
    }

    private int pstmtSetArrayPos(PreparedStatement pstmt, int pos, int arrayPos) throws SQLException {
        if (printSQL != null) printDebug("        pstmtSetArrayPos: pos="+pos+", arrayPos="+arrayPos);
        pstmt.setInt(pos,arrayPos);
        return 1;
    }

    private int pstmtSetNonArrayDBValue(PreparedStatement pstmt, int pos, Property prop, Object dbValue) throws SQLException {
        if (prop instanceof ReferencedBeanProperty) {
            ReferencedBeanProperty refProp = (ReferencedBeanProperty) prop;
            Property[] refPriKeyProps = refProp.getRefBeanPrimaryKeyProperties();
            Object[] refKeyDBVals = (Object[]) dbValue;
            return pstmtSetDBValues(pstmt,pos,refPriKeyProps,refKeyDBVals);
        }

        if (printSQL != null) printDebug("        pstmtSetNonArrayDBValue: prop="+prop+", pos="+pos+", dbValue="+dbValue);
        pstmt.setObject(pos,dbValue);
        return 1;
    }

    private int pstmtSetDBValue(PreparedStatement pstmt, int pos, Property prop, Object dbValue) throws SQLException {
        if (prop.isArray()) {
            Object[] a = (Object[]) dbValue;
            boolean containsArray = (a!=null);
            if (printSQL != null) printDebug("        pstmtSetDBValue: pos="+pos+", dbValue="+containsArray);
            pstmt.setObject(pos,containsArray);
            return 1;
        }

        return pstmtSetNonArrayDBValue(pstmt,pos,prop,dbValue);
    }

    private int pstmtSetDBValues(PreparedStatement pstmt, int startPos, Property[] props, Object[] dbValues) throws SQLException {
        int pos = startPos;
        for (int i=0; i<props.length; i++) {
            Property prop = props[i];
            Object dbValue = (dbValues==null ? null : dbValues[i]);
            pos += pstmtSetDBValue(pstmt,pos,prop,dbValue);
        }

        return pos-startPos;
    }

    private B[] sqlMatch(MatchArgTree argTree) throws RollbackException {
    	try {
	        if (TranImpl.isActive() && argTree.containsNonPrimaryKeyProps()) {
	        	// If we are in a transaction and we're matching on non-primary key properties,
	        	// we must flush any changed beans (without committing the transaction)
	        	// so that these changes can be searched.
	
	        	// (Primary key properties cannot be changed, so if these are the only properties
	        	// being searched there's no need to flush.
	
	        	flushChangedBeans();
	        }
	        
	    	if (argTree.containsMaxOrMin()) {
		    	fixMaxMin(argTree);
	    	}
	
	        String sql = computeSql(argTree);
	        fixDBValuesForPartialStringMatch(argTree);
	        Object[] keyDBValues = DBValues.makeDBValues(argTree.getProperties(),argTree.getValues());
	        return sqlMatch(sql, keyDBValues,argTree);
    	} catch (RollbackException e) {
    		throw e;
    	} catch (Exception e) {
    		TranImpl.rollbackAndThrow(e);
    		throw new AssertionError("rollbackAndThrow() returned");
    	}
    }

    private B[] sqlMatch(String sql, Object[] keyDBValues, MatchArgTree argTree) throws RollbackException {
        Connection con = join();
        Map<PrimaryKey<B>,BeanTrackerRec<B>> beanTracker = threadTrackedBeans.get();

        try {
            if (containsArrayFields && !TranImpl.isActive()) con.setAutoCommit(false);

            if (printSQL != null) printDebug("sqlMatch: "+sql);
            PreparedStatement pstmt = con.prepareStatement(sql.toString());
            pstmtSetDBValues(pstmt,1,argTree.getProperties(),keyDBValues);
            ResultSet rs = pstmt.executeQuery();

            List<B>  answerBeans = new ArrayList<B>();
            ArrayList<Object[]> newDBValuesList = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] dbValues = loadDBValues(rs);
                PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,primaryKeyInfo.getPrimaryKeyDBValues(dbValues));
                if (beanTracker != null && beanTracker.containsKey(key)) {
                	answerBeans.add(beanTracker.get(key).getBean());
                } else {
                    newDBValuesList.add(dbValues);
                }
            }
            pstmt.close();

            if (containsArrayFields) {
                for (Object[] dbValues : newDBValuesList) {
                    fillArrayDBValues(con,dbValues);
                }
            }

            if (TranImpl.isActive()) {
                for (Object[] dbValues : newDBValuesList) {
                    if (printSQL != null) printDebug("sqlMatch: making bean: "+DBValues.toString(dbValues));
                    B bean = makeBean(dbValues);
                    PrimaryKey<B> key = new PrimaryKey<B>(primaryKeyInfo,primaryKeyInfo.getPrimaryKeyDBValues(dbValues));
                    beanTracker.put(key,new BeanTrackerRec<B>(key,bean,dbValues));
                    answerBeans.add(bean);
                }
                return toArray(answerBeans);
            }

            if (containsArrayFields) {
                con.commit();
                con.setAutoCommit(true);
            }
            if (printSQL != null) printDebug("sqlMatch: releasing connection: "+con);
            connectionPool.releaseConnection(con);

            for (Object[] dbValues : newDBValuesList) {
                B bean = makeBean(dbValues);
                answerBeans.add(bean);
            }
            return toArray(answerBeans);
        } catch (SQLException e) {
            myRollbackAndThrow(con,e);
            throw new AssertionError("myRollbackAndThrow returned");
        }
    }

    private Class<?> sqlToJava(String sqlType) {
        String uc = sqlType.toUpperCase();
        // boolean isNonNull = sqlType.endsWith(" NOT NULL");

        if (uc.startsWith("VARCHAR(")) return String.class;

        if (uc.startsWith("TINYINT(")) return boolean.class;
        if (uc.startsWith("INT("))     return int.class;
        if (uc.startsWith("BIGINT("))  return long.class;
        if (uc.startsWith("DOUBLE"))   return double.class;
        if (uc.startsWith("FLOAT"))    return float.class;

        if (uc.equals("DATE"))         return java.sql.Date.class;
        if (uc.equals("DATETIME"))     return java.util.Date.class;
        if (uc.equals("TIME"))         return java.sql.Time.class;
        if (uc.equals("TIMESTAMP"))    return java.util.Date.class;

        if (uc.equals("LONGBLOB"))     return byte[].class;

        throw new BeanFactoryException("Cannot map SQL type: "+sqlType);
    }

	// Transaction management methods

	private Connection join() throws RollbackException {
		if (!TranImpl.isActive()) {
	        try {
	        	Connection c = connectionPool.getConnection();
	        	if (printSQL != null) printDebug("join: no transaction, connection="+c);
	            return c;
	        } catch (ConnectionException e) {
	            throw new RollbackException(e);
	        }
		}

		if (threadTrackedBeans.get() != null) {
        	if (printSQL != null) printDebug("join: factory already in a transaction, connection="+threadConnection.get());
			return threadConnection.get();
		}

		threadTrackedBeans.set(new HashMap<PrimaryKey<B>,BeanTrackerRec<B>>());

		List<GoogleSQLFactory<?>> ourOtherInvolvedFactories = threadInvolvedGoogleSQLFactories.get();
		if (ourOtherInvolvedFactories != null) {
			ourOtherInvolvedFactories.add(this);
        	if (printSQL != null) printDebug("join: factory joining existing transaction, connection="+threadConnection.get());
			return threadConnection.get();
		}

		Connection con = null;
		try {
            con = connectionPool.getConnection();
            if (printSQL != null) printDebug("join: BEGIN_TRANSACTION, connection="+con);
			con.setAutoCommit(false);
			threadConnection.set(con);

			threadInvolvedGoogleSQLFactories.set(new ArrayList<GoogleSQLFactory<?>>());
			TranImpl.join(this);
			return con;
        } catch (ConnectionException e) {
            throw new RollbackException(e);
        } catch (SQLException e) {
			try { con.close(); } catch (SQLException e2) { /* Ignore */ }
            throw new RollbackException(e);
        }
	}

	private void cleanUpThreadVariables() {
		// Clean up my instance's thread variables
		threadTrackedBeans.set(null);

		// Clean up other involved factories instance thread variables
		for (GoogleSQLFactory<?> otherFactory : threadInvolvedGoogleSQLFactories.get()) {
			otherFactory.threadTrackedBeans.set(null);
		}

		// Clean up static thread variables (shared by all GoogleSQLFactories)
		threadInvolvedGoogleSQLFactories.set(null);

		Connection con = threadConnection.get();
		threadConnection.set(null);
		try {
			con.setAutoCommit(true);
			if (printSQL != null) printDebug("cleanUpThreadVariables: releasing connection: "+con);
			connectionPool.releaseConnection(con);
		} catch (SQLException e) {
			try { con.close(); } catch (SQLException e2) { /* Ignore */ }
		}
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

	public void prepare() throws RollbackException {

		for (GoogleSQLFactory<?> otherFactory : threadInvolvedGoogleSQLFactories.get()) {
			otherFactory.doPrepare();
		}
		doPrepare();

		Connection con = threadConnection.get();
		try {
            if (printSQL != null) printDebug("prepare: COMMIT_TRANSACTION");
			con.commit();
			cleanUpThreadVariables();
		} catch (SQLException e) {
			TranImpl.rollbackAndThrow(e);
		}
	}

	public void rollback() {
		Connection con = threadConnection.get();
		try {
            if (printSQL != null) printDebug("prepare: ROLLBACK_TRANSACTION");
			con.rollback();
			cleanUpThreadVariables();
		} catch (SQLException e) {
			throw new AssertionError(e);
		}
	}

	public void commit() {
		// Nothing to do
	}

	public void doPrepare() throws RollbackException {
		flushChangedBeans();
	}

	private void flushChangedBeans() throws RollbackException {
		Map<PrimaryKey<B>,BeanTrackerRec<B>> trackedBeans = threadTrackedBeans.get();
		if (trackedBeans == null) return;

		for (BeanTrackerRec<B> r : trackedBeans.values()) {
			B bean = r.getBean();
            Object[] oldDBValues = r.getDBValues();
			Object[] newDBValues = makeDBValues(bean);
			if (!r.getKey().keyEquals(primaryKeyInfo.getPrimaryKeyDBValues(newDBValues))) {
				TranImpl.rollbackAndThrow("Bean "+bean+" has changed primary key value: key should be \""+
                        DBValues.toString(r.getKey().getDBValues())+
                        "\" but instead is \""+
                        DBValues.toString(primaryKeyInfo.getPrimaryKeyDBValues(newDBValues))+"\"");
			}

			flushBeanIfChanged(oldDBValues,newDBValues);
			r.setDBValues(newDBValues);
		}
	}

	private void flushBeanIfChanged(Object[] oldDBValues, Object[] newDBValues) throws RollbackException {
		boolean[] changedScalars = computeChangedScalars(oldDBValues,newDBValues);
        boolean[] changedArrays  = computeChangedArrays(oldDBValues,newDBValues);

        if (changedScalars != null) {
            flushScalarChanges(changedScalars,newDBValues);
        }

        if (changedArrays != null) {
            Object[] priKeyDBValues = primaryKeyInfo.getPrimaryKeyDBValues(newDBValues);
            for (int i=1; i<properties.length; i++) {
                if (changedArrays[i]) {
                    Object[] oldArray = null;
                    if (oldDBValues != null) oldArray = (Object[]) oldDBValues[i];
                    Object[] newArray = (Object[]) newDBValues[i];
                    flushArrayChanges(priKeyDBValues,properties[i],oldArray,newArray);
                }
            }
        }
    }

    private boolean[] setChangedProperty(boolean[] changed, int i) {
        boolean[] answer;
        if (changed != null) {
            answer = changed;
        } else {
            answer = new boolean[properties.length];
        }

        answer[i] = true;
        return answer;
    }

    private Property[] changedProperties(boolean[] changed) {
        ArrayList<Property> answer = new ArrayList<Property>();
        for (int i=0; i<properties.length; i++) {
            if (changed[i]) answer.add(properties[i]);
        }
        return answer.toArray(new Property[answer.size()]);
    }
    
    private void checkMaxStringLength(Property p, Object newDBValue, Object[] priKeyDBVals) throws RollbackException {
    	// Just call the other check method with zero for the array position.
    	// That method will only look at (and print out) the array position
    	// if the property (p) is an array property.
    	
    	checkMaxStringLength(p, 0, newDBValue, priKeyDBVals);
    }

    private void checkMaxStringLength(Property p, int arrayPos, Object newDBValue, Object[] priKeyDBVals) throws RollbackException {
    	if (p.getBaseType() != String.class) return;
    	
        //Class cast exception occurred :(String) newDBValue;
    	
    	String newStringValue = String.valueOf(newDBValue);
    	if (newStringValue == null) return;
    	
    	int maxStrLen = p.getColumnMaxStrLens()[0];
    	if (newStringValue.length() <= maxStrLen) return;
    	
    	// Generate error
    	String message = null;
  		if(p.isArray()) {
  			message = "String length too long in array of String: table="+tableName+", primary-key="+DBValues.toString(priKeyDBVals)+" property="+p.getName()+", array-position="+arrayPos+", maxLen="+maxStrLen+", length="+newStringValue.length()+", value=\""+newStringValue+"\"";
  		} else {
  			message = "String length too long: table="+tableName+", primary-key="+DBValues.toString(priKeyDBVals)+" property="+p.getName()+", maxLen="+maxStrLen+", length="+newStringValue.length()+", value=\""+newStringValue+"\"";
  		}
    	
    	TranImpl.rollbackAndThrow(message);
    }
    
    private boolean[] computeChangedScalars(Object[] oldDBValues, Object[] newDBValues) {
        if (oldDBValues == null) {
            // Bean was just created.  Only the primary key values were written, so we need to flush
            // the non-primary-key properties that contain non-default values (non-null or non-zero or non-false)
            boolean[] changed = null;
            for (int i=primaryKeyInfo.getProperties().length; i<properties.length; i++) {
                Property prop = properties[i];
                if (prop.isArray() && newDBValues[i] != null) {
                    changed = setChangedProperty(changed,i);
                } else if (!DBValues.equalNonArrayDBValues(prop,prop.getDefaultValue(),newDBValues[i])) {
                    changed = setChangedProperty(changed,i);
                }
            }
            return changed;
        }

        // We have old values to compare against
        boolean[] changed = null;
		for (int i=primaryKeyInfo.getProperties().length; i<properties.length; i++) {
            Property prop = properties[i];
			if (prop.isArray()) {
                Object[] oldArray = (Object[]) oldDBValues[i];
                Object[] newArray = (Object[]) newDBValues[i];
                if ((oldArray == null && newArray != null) || (oldArray != null && newArray == null)) {
                    changed = setChangedProperty(changed,i);
                }
			} else if (!DBValues.equalNonArrayDBValues(prop,oldDBValues[i],newDBValues[i])) {
                changed = setChangedProperty(changed,i);
			}
		}
        return changed;
    }

    private boolean[] computeChangedArrays(Object[] oldDBValues, Object[] newDBValues) {
        if (oldDBValues == null) {
            // newly created so flush if non-default value (non-null or non-zero or non-false)
            boolean[] changed = null;
            for (int i=primaryKeyInfo.getProperties().length; i<properties.length; i++) {
                Property prop = properties[i];
                if (prop.isArray() && newDBValues[i] != null) {
                    Object[] newArray = (Object[]) newDBValues[i];
                    if (newArray.length > 0) {
                        changed = setChangedProperty(changed,i);
                    }
                }
            }
            return changed;
        }

        // We have old values to compare against
        boolean[] changed = null;
        for (int i=primaryKeyInfo.getProperties().length; i<properties.length; i++) {
            Property prop = properties[i];
            if (prop.isArray()) {
                Object[] oldArray = (Object[]) oldDBValues[i];
                Object[] newArray = (Object[]) newDBValues[i];
                if (oldArray == null && newArray != null && newArray.length > 0) {
                    changed = setChangedProperty(changed,i);
                } else if (oldArray != null && newArray == null && oldArray.length > 0) {
                    changed = setChangedProperty(changed,i);
                } else if (oldArray != null && newArray != null && !DBValues.equalArrayDBValues(prop,oldArray,newArray)) {
                    changed = setChangedProperty(changed,i);
                }
            }
        }
        return changed;
    }

    private void flushScalarChanges(boolean[] changedScalars, Object[] newDBValues) throws RollbackException {
		StringBuffer sql = new StringBuffer();
        sql.append("UPDATE ").append(tableName).append(" SET ");
        appendColumnNamesSeparatorsQuestions(sql,changedProperties(changedScalars),",");
		if (primaryKeyProperties.length > 0) sql.append(" WHERE ").append(primaryKeyColumnNamesAndQuestions);

        Connection con = threadConnection.get();

		try {
			PreparedStatement pstmt = con.prepareStatement(sql.toString());
            if (printSQL != null) printDebug("flushScalarChanges: "+sql);
            
            Object[] priKeyDBVals = primaryKeyInfo.getPrimaryKeyDBValues(newDBValues);

            int pos = 1;
			for(int i=0; i<properties.length; i++) {
				if (changedScalars[i]) {
                    if (printSQL != null) printDebug("   "+properties[i]+" value="+DBValues.toString(newDBValues[i]));
                    checkMaxStringLength(properties[i],newDBValues[i],priKeyDBVals);
                    pos += pstmtSetDBValue(pstmt,pos,properties[i],newDBValues[i]);
				}
			}
            pstmtSetDBValues(pstmt,pos,primaryKeyInfo.getProperties(),priKeyDBVals);
			int num = pstmt.executeUpdate();
			pstmt.close();

			if (num > 1) TranImpl.rollbackAndThrow("Found "+num+" rows with same primary key: "+DBValues.toString(priKeyDBVals));
			if (num == 0) TranImpl.rollbackAndThrow("Couldn't find the row with this primary key: "+DBValues.toString(priKeyDBVals));
		} catch (SQLException e) {
			TranImpl.rollbackAndThrow(e);
		}
	}
    
	private void flushArrayChanges(Object[] primaryKeyValues, Property prop, Object[] oldArray, Object[] newArray) throws RollbackException {
		if (oldArray == null) oldArray = ZERO_LEN_ARRAY;
		if (newArray == null) newArray = ZERO_LEN_ARRAY;
		if (oldArray.length == 0 && newArray.length == 0) return;

        Connection con = threadConnection.get();
        String propertyName = prop.getName().toLowerCase();
        
		try {
			PreparedStatement pstmt = null;
			int arrayPos = 0;
			while (arrayPos < oldArray.length && arrayPos < newArray.length) {
				if (!DBValues.equalNonArrayDBValues(prop,oldArray[arrayPos],newArray[arrayPos])) {
                    // Case 1: change value
					if (pstmt == null) {
                        StringBuffer sql = new StringBuffer();
                        sql.append("UPDATE ").append(tableName).append('_').append(propertyName).append(" SET ");
                        appendNonArrayColumnNamesAndQuestions(sql,prop,",");
                        sql.append(" WHERE ").append(primaryKeyColumnNamesAndQuestions);
                        sql.append(" AND ").append(ARRAY_POS_COLUMN_NAME).append("=?");
                        if (printSQL != null) printDebug("flushArrayChanges (Case#1): "+sql);
						pstmt = con.prepareStatement(sql.toString());
					}
					checkMaxStringLength(prop,arrayPos,newArray[arrayPos],primaryKeyValues);
                    int pos = 1;
                    pos += pstmtSetNonArrayDBValue(pstmt,pos,prop,newArray[arrayPos]);
                    pos += pstmtSetDBValues(pstmt,pos,primaryKeyInfo.getProperties(),primaryKeyValues);
                    pstmtSetArrayPos(pstmt,pos,arrayPos);
					int num = pstmt.executeUpdate();
					if (num != 1) TranImpl.rollbackAndThrow("Failure when updating "+tableName+'_'+propertyName+": pos="+arrayPos+", value="+newArray[arrayPos]+": num="+num);
				}
				arrayPos++;
			}

			if (pstmt != null) {
				pstmt.close();
				pstmt = null;
			}

			while (arrayPos < oldArray.length) {
                // Case 2: Delete values
				if (pstmt == null) {
                    StringBuffer sql = new StringBuffer();
                    sql.append("DELETE FROM ").append(tableName).append('_').append(propertyName);
                    sql.append(" WHERE ").append(primaryKeyColumnNamesAndQuestions);
                    sql.append(" AND ").append(ARRAY_POS_COLUMN_NAME).append("=?");
                    if (printSQL != null) printDebug("flushArrayChanges (Case#2): "+sql);
                    pstmt = con.prepareStatement(sql.toString());
				}
                int pos = 1;
                pos += pstmtSetDBValues(pstmt,pos,primaryKeyInfo.getProperties(),primaryKeyValues);
                pstmtSetArrayPos(pstmt,pos,arrayPos);
				int num = pstmt.executeUpdate();
				if (num != 1) TranImpl.rollbackAndThrow("Failure when deleting from "+tableName+'_'+propertyName+": pos="+arrayPos+", value="+newArray[arrayPos]+": num="+num);
				arrayPos++;
			}


			if (pstmt != null) {
				pstmt.close();
				pstmt = null;
			}

			while (arrayPos < newArray.length) {
                // Case 3: Add values
				if (pstmt == null) {
                    StringBuffer sql = new StringBuffer();
                    sql.append("INSERT INTO ").append(tableName).append('_').append(propertyName).append(" SET ");
                    sql.append(primaryKeyColumnNamesCommasQuestions).append(",");
                    sql.append(ARRAY_POS_COLUMN_NAME).append("=?").append(",");
                    appendNonArrayColumnNamesAndQuestions(sql,prop,",");
                    if (printSQL != null) printDebug("flushArrayChanges (Case#3): "+sql);
                    pstmt = con.prepareStatement(sql.toString());
				}
				checkMaxStringLength(prop,arrayPos,newArray[arrayPos],primaryKeyValues);
                int pos = 1;
                pos += pstmtSetDBValues(pstmt,pos,primaryKeyInfo.getProperties(),primaryKeyValues);
                pos += pstmtSetArrayPos(pstmt,pos,arrayPos);
                pstmtSetNonArrayDBValue(pstmt,pos,prop,newArray[arrayPos]);
				int num = pstmt.executeUpdate();
				if (num != 1) TranImpl.rollbackAndThrow("Failure when inserting into "+tableName+'_'+propertyName+": pos="+arrayPos+", value="+newArray[arrayPos]+": num="+num);
				arrayPos++;
			}

			if (pstmt != null) pstmt.close();
		} catch (SQLException e) {
			TranImpl.rollbackAndThrow(e);
		}
	}

	private void checkProperty(ResultSet rs, String name, Class<?> type, boolean nonNull, boolean primaryKey, String arrayName) throws SQLException {

		if (!rs.next()) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": missing column named "+name+" (or entire table doesn't exist)");

        if (!name.equals(rs.getString(1))) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": incorrect column name: "+rs.getString(1)+" (should be "+name+")");

        String   sqlType = rs.getString(2);
		Class<?> dbType = sqlToJava(sqlType);
		if (dbType == null) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": column "+name+" has unusable type: "+sqlType);
		if (dbType != type && dbType != type.getSuperclass()) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": column "+name+" incompatible type: "+sqlType+" => "+dbType+" vs "+type);

        boolean dbNonNull = !rs.getBoolean(3);
		if (nonNull && !dbNonNull) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": column "+name+" must be not allow nulls");
		if (!nonNull && dbNonNull) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": column "+name+" must be allow nulls");

		boolean dbPriKey = rs.getString(4).equalsIgnoreCase("PRI");
		if (primaryKey && !dbPriKey) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": column "+name+" must be part of the primary key");
		if (!primaryKey && dbPriKey) throw new BeanFactoryException("Auxilary table for array: "+tableName+'_'+arrayName+": column "+name+" must not be part of the primary key");
	}

	private boolean typeMatch(Class<?> dbType, Class<?> beanType) {
		if (dbType == java.util.Date.class && beanType == NMDate.class) return true;
		if (dbType == java.sql.Date.class && beanType == NMSQLDate.class) return true;
		if (dbType == java.sql.Time.class && beanType == NMTime.class) return true;
		if (dbType == String.class && beanType.isEnum()) return true;
		return dbType == beanType;
	}
}

/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;

import org.mybeans.factory.impl.AbstractFactory;
import org.mybeans.factory.impl.CSVTable;
import org.mybeans.factory.impl.GoogleSQLTable;
import org.mybeans.factory.impl.MySQLTable;

/**
 * An abstract class to manipulate tables that store JavaBean properties.  Instantiate using one
 * of the <tt>getInstance()</tt> methods.  Then use the method calls to create and delete tables.
 * Finally, use the <tt>getFactory()</tt> method to obtain a BeanFactory for the table in order
 * to create, lookup, and delete rows in the table that are used to store the properties of a
 * JavaBean.
 *
 * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for examples and details.)
 */
public abstract class BeanTable<B> {
	
	private static final int DEFAULT_CSV_BACKUPS = 5;

	private static File   csvDir     = null;
	private static int    csvBackups = DEFAULT_CSV_BACKUPS;
	private static String jdbcDriver = null;
	private static String jdbcURL    = null;
	private static String user       = null;
	private static String password   = null;
	
	/**
     * Use this method to obtain a BeanFactory that stores files in a CSV File.
     * This method will maintain a default number of backup files in the files system (current 5).
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param csvFile the file in which data is stored.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a CSV file to storing JavaBeans of type <tt>B</tt>.
	 */
    public static <B> BeanTable<B> getCSVInstance(
            Class<B> beanClass,
            File     csvFile,
            BeanFactory<?>... referencedFactories) {
        return getCSVInstance(beanClass,csvFile,DEFAULT_CSV_BACKUPS,referencedFactories);
    }

    /**
     * Use this method to obtain a BeanFactory that stores files in a CSV File.
     * This method will maintain a default number of backup files in the files system (current 5).
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param fileName the name of the CSV file in which data is stored.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a CSV file to storing JavaBeans of type <tt>B</tt>.
	 */
    public static <B> BeanTable<B> getCSVInstance(
            Class<B> beanClass,
            String   fileName,
            BeanFactory<?>... referencedFactories) {
        return getCSVInstance(beanClass,fileName,DEFAULT_CSV_BACKUPS,referencedFactories);
    }

    /**
     * Use this method to obtain a CSV BeanFactory and specify the number of backup files.
     * This method will maintain a default number of backup files in the files system (current 5).
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param fileName the name of the CSV file in which data is stored.
     * @param backups the maximum number of backup files maintained for this table.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a CSV file to storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getCSVInstance(
            Class<B> beanClass,
            String   fileName,
            int      backups,
            BeanFactory<?>... referencedFactories) {
    	File csvFile;
    	if (fileName.toLowerCase().endsWith(".csv")) {
    		csvFile = new File(fileName);
    	} else {
    		csvFile = new File(fileName+".csv");
    	}
        return new CSVTable<B>(beanClass,csvFile,backups,castFactories(referencedFactories));
    }

    /**
     * Use this method to obtain a CSV BeanFactory and specify the number of backup files.
     * This method will maintain a default number of backup files in the files system (current 5).
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param csvFile the file in which data is stored.
     * @param backups the maximum number of backup files maintained for this table.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a CSV file to storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getCSVInstance(
            Class<B> beanClass,
            File     csvFile,
            int      backups,
            BeanFactory<?>... referencedFactories) {
        return new CSVTable<B>(beanClass,csvFile,backups,castFactories(referencedFactories));
    }

    /**
     * Use this method to obtain a BeanFactory using system properties to determine where
     * the data is stored.  For a MySQL specify the following required system properties:
     * <ul>
     * <li>org.mybeans.factory.jdbcDriver</li>
     * <li>org.mybeans.factory.jdbcURL</li>
     * </ul>
     * and the following optional properties:
     * <ul>
     * <li>org.mybeans.factory.user</li>
     * <li>org.mybeans.factory.password</li>
     * <li>org.mybeans.factory.debug</li>
     * </ul>
     * Set debug to <tt>true</tt> and the BeanFactory will print to <tt>System.out</tt> debugging
     * information, including all the SQL calls executed.
     * <p>
     * For a CSV factory specify the following required system property:
     * <ul>
     * <li>org.mybeans.factory.csvDirectory</li>
     * </ul>
     * This property specifies the file system directory (which must exist) in which data files are
     * to be stored.  A <tt>.csv</tt> extension will be added to the <tt>tableName</tt>.
     * The following optional properties may be specified for CSV factories:
     * <ul>
     * <li>org.mybeans.factory.csvBackups</li>
     * <li>org.mybeans.factory.debug</li>
     * </ul>
     * Set debug to <tt>true</tt> and the BeanFactory will print to <tt>System.out</tt> debugging
     * information.
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param tableName the name of the table in the database.
     *     For CSV files a <tt>.csv</tt> extension will appended to the table name.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a CSV file to storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getInstance(
            Class<B> beanClass,
            String   tableName,
            BeanFactory<?>...referencedFactories) {
        if (jdbcDriver == null && csvDir == null) throw new BeanFactoryException("Must specify whether to use a JDBC database or CSV files by calling either BeanTable.useJDBC() or BeanTable.useCSVFiles().");

        if (csvDir != null) {
            if (!csvDir.exists()) throw new BeanFactoryException("CSV Directory does not exist: "+csvDir);
        	File file = new File(csvDir,tableName+".csv");
            return getCSVInstance(beanClass,file,csvBackups,referencedFactories);
        }

        return getSQLInstance(beanClass,tableName,jdbcDriver,jdbcURL,user,password,referencedFactories);
    }

    /**
     * Use this method to obtain a BeanFactory when no user id or password is required to access a SQL database.
     * Currently the only SQL database supported is MySQL.
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param tableName the name of the table in the database.
     * @param jdbcDriver the name of the JDBC Driver for the SQL database.
     * @param jdbcURL the URL of the SQL database.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a SQL table storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getSQLInstance(
            Class<B> beanClass,
            String   tableName,
            String   jdbcDriver,
            String   jdbcURL,
            BeanFactory<?>...referencedFactories) {
        return new MySQLTable<B>(beanClass,tableName,jdbcDriver,jdbcURL,null,null,castFactories(referencedFactories));
    }

    /**
     * Use this method to obtain a BeanFactory when a user id and password are required to access a SQL database.
     * Currently the only SQL database supported is MySQL.
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param tableName the name of the table in the database.
     * @param jdbcDriver the name of the JDBC Driver for the SQL database.
     * @param jdbcURL the URL of the SQL database.
     * @param user the user id used to log into the database.
     * @param password the password used to log into the database.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a SQL table storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getSQLInstance(
            Class<B> beanClass,
            String   tableName,
            String   jdbcDriver,
            String   jdbcURL,
            String   user,
            String   password,
            BeanFactory<?>...referencedFactories) {
        return new MySQLTable<B>(beanClass,tableName,jdbcDriver,jdbcURL,user,password,castFactories(referencedFactories));
    }

    /**
     * Use this method to obtain a BeanFactory when no user id or password is required to access a SQL database.
     * This implementation supports Google SQL.
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param tableName the name of the table in the database.
     * @param jdbcDriver the name of the JDBC Driver for the SQL database.
     * @param jdbcURL the URL of the SQL database.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a SQL table storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getGoogleSQLInstance(
            Class<B> beanClass,
            String   tableName,
            String   jdbcDriver,
            String   jdbcURL,
            BeanFactory<?>...referencedFactories) {
        return new GoogleSQLTable<B>(beanClass,tableName,jdbcDriver,jdbcURL,null,null,castFactories(referencedFactories));
    }

    /**
     * Use this method to obtain a BeanFactory when a user id and password are required to access a SQL database.
     * This implementation supports Google SQL.
     * @param <B> a JavaBean.  (See <tt>BeanFactory</tt> for details.)
     * @param beanClass the <tt>Class</tt> class for <tt>B</tt>.
     * @param tableName the name of the table in the database.
     * @param jdbcDriver the name of the JDBC Driver for the SQL database.
     * @param jdbcURL the URL of the SQL database.
     * @param user the user id used to log into the database.
     * @param password the password used to log into the database.
     * @param referencedFactories zero or more bean factories used to store JavaBeans of other types referenced by <tt>B</tt>.
     * @return a <tt>BeanTable</tt> that manipulates a SQL table storing JavaBeans of type <tt>B</tt>.
     */
    public static <B> BeanTable<B> getGoogleSQLInstance(
            Class<B> beanClass,
            String   tableName,
            String   jdbcDriver,
            String   jdbcURL,
            String   user,
            String   password,
            BeanFactory<?>...referencedFactories) {
        return new GoogleSQLTable<B>(beanClass,tableName,jdbcDriver,jdbcURL,user,password,castFactories(referencedFactories));
    }

    
    public static void useCSVFiles(File csvDirectory) {
    	useCSVFiles(csvDirectory,DEFAULT_CSV_BACKUPS);
    }

    public static void useCSVFiles(File csvDirectory, int numBackups) {
    	BeanTable.csvDir = csvDirectory;
    	BeanTable.csvBackups = numBackups;
    }
    
    public static void useJDBC(String jdbcDriverName, String jdbcURL) {
    	useJDBC(jdbcDriverName,jdbcURL,null,null);
    }
    
    public static void useJDBC(String jdbcDriverName, String jdbcURL, String user, String password) {
    	BeanTable.jdbcDriver = jdbcDriverName;
    	BeanTable.jdbcURL    = jdbcURL;
    	BeanTable.user       = user;
    	BeanTable.password   = password;
    	
    	csvDir = null;
    }

    /*
     * Converts the array of <tt>BeanFactory<?></tt> to an array of <tt>AbstractBeanFactory</tt>.
     * @throws NullPointerException if any of the <tt>referencedBeanFactories</tt> in the array are null or if the
     * <tt>referencedBeanFactories</tt> array itself is null (use a zero length array instead).
     * @throws IllegalArgumentException if any of <tt>referencedBeanFactories</tt> in the array not instances of
     * <tt>AbstractBeanFactory</tt>.
     */
    private static AbstractFactory<?>[] castFactories(BeanFactory<?>[] referencedBeanFactories) {
        int len = 0;
        if (referencedBeanFactories != null) len = referencedBeanFactories.length;

        Object array = Array.newInstance(AbstractFactory.class,len);
        AbstractFactory<?>[] answer = (AbstractFactory<?>[]) array;

        for (int i=0; i<len; i++) {
            BeanFactory<?> f = referencedBeanFactories[i];
            if (f == null) {
                throw new NullPointerException("referencedBeanFactories["+i+"]");
            }

            if (f instanceof AbstractFactory) {
                answer[i] = (AbstractFactory<?>) f;
            } else {
                throw new IllegalArgumentException("referencedBeanFactories["+i+"]: This bean factory can only support referenced factories that are subclasses of "+AbstractFactory.class.getName());
            }
        }

        return answer;
    }

    protected BeanTable() {
    }

    /**
     * Creates this table in the database.
     * This method uses introspection to determine the properties of <tt>B</tt> and creates a
     * database table that can store instances of <tt>B</tt>.
     *
     * @param primaryKeyPropertyNames the names of the properties of <tt>B</tt> that comprise the
     * primary key.  Primary key property values cannot be <tt>null</tt> and each instance of <tt>B</tt>
     * stored in this table must have a unique primary key.
     *
     * @throws BeanFactoryException if the table cannot be created.
     * Possible reasons the table cannot be created include: the table already exists,
     * there are missing getters, setters, or constructors for the primary key properties,
     * this <tt>BeanTable</tt> cannot determine how to map a property to the database table,
     * there is an error connecting to the database.
     */
    public abstract void create(String...primaryKeyPropertyNames);

    /**
     * Creates a secondary index for this table in the database.
     * The table must already exist in the database.
     *
     * @param secondaryKeyPropertyNames the names of the properties of <tt>B</tt> that comprise the
     * secondary key.  Secondary key property values can be <tt>null</tt> and are not unique.
     * @throws UnsupportedOperationException if the operation is not supported by the
     * underlying database (or by this <tt>BeanFactory</tt> implementation).
     * @throws BeanFactoryException if the index cannot be created.
     * Possible reasons include the table does not exist,
     * there are missing getters, setters, or constructors for the secondary key properties,
     * there is an error connecting to the database.
     */
    public abstract void createSecondaryIndex(String...secondaryKeyPropertyNames);

    /**
     * Deletes this table from the database.
     * Also deletes any auxiliary tables (that contain array data).
     * This call does not validate the table (see <tt>getFactory()</tt>).
     * This call does not throw an exception if the table does not exist.
     *
     * @throws BeanFactoryException if there is an error connecting to the database.
     */
    public abstract void delete();

    /**
     * Checks to see if this table exists in the database.
     *
     * This call does not check to see if the table will successfully validate (see <tt>getFactory()</tt>).
     *
     * @return true if the table exists.
     * @throws BeanFactoryException if there is an error connecting to the database.
     */
    public abstract boolean exists();
    
    public abstract Class<B> getBeanClass();

    /**
     * This method is used to obtain a <tt>BeanFactory</tt> that can be used to store in and retrieve from this
     * table beans of type <tt>B</tt>.
     * The first time <tt>getFactory()</tt> is called, the schema for this table is validated versus
     * the properties for <tt>B</tt>.  If the table columns and bean properties do not match-up
     * <tt>BeanFactoryException</tt> is thrown.
     * On subsequent calls, the same <tt>BeanFactory</tt> reference may be return (without re-validating the table).
     * @return a <tt>BeanFactory</tt> for this table.
     * @throws BeanFactoryException if there is an error connecting to the database,
     * if this table does not exist in the database, or
     * if this table's schema does not match the properties for <tt>B</tt>.
     */
    public abstract BeanFactory<B> getFactory();

    /**
     * Enable (or disable) the printing of debug messages.
     * For tables backed by a relational database this usually includes printing the SQL commands generated.
     * By default, printing of debug messages is disabled.
     * @param writer a character writer to which debugging messages will be printed.  To disable printing of
     * debug messages, pass in <tt>null</tt>.
     */
    public abstract void setDebugOutput(Writer writer);

    /**
     * A convenience method that enable (or disable) the printing of debug messages to an <tt>OutputStream</tt>.
     * For tables backed by a relational database this usually includes printing the SQL commands generated.
     * By default, printing of debug messages is disabled.  This method will wrap the given parameter in
     * an <tt>OutputStreamWriter</tt> and then call <tt>setDebugOutput(Writer)</tt>.
     * @param out an output stream to which debugging messages will be printed.  To disable printing of
     * debug messages, pass in <tt>null</tt>.  For example, you may use <tt>System.out</tt> to write debugging output
     * to the screen.
     */
    public void setDebugOutput(OutputStream out) {
    	if (out == null) {
    		setDebugOutput((OutputStream) null);
    	} else {
    		setDebugOutput(new OutputStreamWriter(out));
    	}
    }
    
    /**
     * Enable cleanup of idle database connections.  By default, cleanup of idle database
     * connections is not enabled in some implementations because this creates a background
     * cleanup thread which prevents simple demo programs from exiting.  Long running
     * servers should enable the clean of idle database connections.
     * @param enable <tt>true</tt> to clean up idle database connections, <tt>false</tt> to disable.
     */
    public abstract void setIdleConnectionCleanup(boolean enable);
}

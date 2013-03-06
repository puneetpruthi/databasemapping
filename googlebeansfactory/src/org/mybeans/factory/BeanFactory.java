/*
 * Copyright (c) 2005-2006 Jeffrey L. Eppinger.  All Rights Reserved.
 *     Permission granted for educational use only.
 */

package org.mybeans.factory;

/**
 * This is the public interface for bean factories that use database
 * tables to store JavaBeans.
 * Many implementations are possible.  Included with this package are
 * two implementations instantiated via the <tt>BeanTable</tt> class:
 * (1) an implementation storing beans in MySQL databases, and
 * (2) an implementation storing beans in CSV files.
 * <p>
 * The JavaBean must either have a public, no-argument constructor or a
 * public constructor that takes only the value of the bean's primary key.
 * If both constructors are present, the latter is used.
 * <p>
 * JavaBeans declare in the usual way getter and setter methods for the properties
 * that they want to have backed by columns in the database table.
 * The <code>BeanFactory</code> uses the Java reflection classes to inspect the bean.
 * The <code>BeanFactory</code> will only store and retrieve properties that have
 * matching a getter/setter pair (in addition to the property that is the primary key).
 * The getter must have a signature of
 * <p><blockquote><pre>
 *     public &lt;type&gt; get&lt;Name&gt;()
 * </pre></blockquote><p>
 * and the setter must have a signature of
 * <p><blockquote><pre>
 *     public void set&lt;Name&gt;(&lt;type&gt; newValue)
 * </pre></blockquote><p>
 * The properties that comprise the primary key (for the table that backs the beans)
 * must have getter methods as described above, but not need not setter methods if the bean
 * has a constructor that takes the values of the primary key properties.  (This is the
 * preferred approach as you are not allowed to change the values of the primary key
 * properties.)
 * <p>
 * Here is an example of a simple bean representing a user and its password:
 * <p><blockquote><pre>
 * public class User {
 *     private String userName;
 *     private String password = null;
 *
 *     public User(String userName) { this.userName = userName; }
 *
 *     public String getUserName() { return userName; }
 *     public String getPassword() { return password; }

 *     public void setPassword(String s) { password = s; }
 * }
 * </pre></blockquote>
 * <p>
 * JavaBean properties can be any of the following Java types:
 * <code>boolean</code>,
 * <code>byte[]</code>,
 * <code>double</code>,
 * <code>float</code>,
 * <code>int</code>,
 * <code>long</code>,
 * <code>java.lang.String</code>,
 * <code>java.sql.Date</code>,
 * <code>java.sql.Time</code>, and
 * <code>java.util.Date</code>.
 * <p>
 * JavaBean can also be non-modifiable dates or times as defined by the
 * <code>org.mybeans.nonmodifiable</code> package.  Use of
 * these non-modifiable subclasses of dates and times reduces the amount
 * of protective copying done by the bean factories.
 * <p>
 * JavaBean properties can also be a reference to another JavaBean
 * that is backed by a <code>BeanFactory</code>.  In this case, those
 * references to those factories must be provided  to this
 * <code>BeanFactory</code> when it is instantiated.  See <tt>BeanTable.getInstance()</tt>
 * for more information.
 * When storing references to other beans, the <code>BeanFactory</code> stores
 * in its database table the foreign key for the referenced beans.
 * <p>
 * Arrays of the above types (except for arrays of <code>byte[]</code>) are also supported.
 * Multi-dimensional arrays are not allowed.  Also, arrays cannot be used as primary keys.
 * In some implementations (such a relational database impleplementations)
 * auxiliary tables are used for backing arrays.
 * <p>
 * Each table that backs a bean will be indexed by a primary key.
 * The primary key must correspond to one or more of the JavaBean's
 * properties.
 * Primary key properties can be of any of the above types, except for arrays.
 * (I.e., a primary key property can be a reference another bean, but cannot be a byte array or another array.)
 * The primary key properties are specified when creating the table.
 * See <tt>BeanTable.create()</tt> for more details.
 * <p>
 * This package provides a <tt>BeanTable</tt> class that allows
 * users to provide information specifying the location
 * of the table that stores instances of a bean along with database
 * specific information such as JDBC Driver, JDBC URL, and database login.
 * The <tt>BeanTable</tt> also provides methods to create and delete
 * tables.  For example:
 * <p><blockquote><pre>
 *     System.setProperty("org.mybeans.factory.jdbcDriver",...);
 *     System.setProperty("org.mybeans.factory.jdbcURL",...);
 *     System.setProperty("org.mybeans.factory.user",...);
 *     System.setProperty("org.mybeans.factory.password",...);
 *
 *     BeanTable<User> t = BeanTable.getInstance(User.class,"user");
 *     if (!t.exists()) t.create("userName");
 *     BeanFactory<User> userFactory = t.getFactory();
 * </pre></blockquote>
 * <p>
 * <code>BeanFactory</code> methods can be invoked from within an enclosing <code>Transaction</code>
 * which will enforce the ACID properties.  Specific ACID guarantees provided
 * are particular to the <code>BeanFactory</code> implementation.
 * <p>
 * Changes made to a bean will be written out to the database at commit time.
 * The only way to make changes to a bean's properties (after it is created) is to obtain
 * a reference from its factory (using <code>create()</code> or <code>lookup()</code>
 * calls) from within a transaction.  All references returned from within a transaction
 * are tracked.  You may then use these references to make setter calls on the bean.
 * If the primary key properties are changes, the enclosing transaction will be
 * rolled back at commit time.
 * <p>
 * For example:
 * <p><blockquote><pre>
 *     Transaction.begin();
 *     User u = userFactory.lookup(userName);
 *     u.setPassword(newPassword);
 *     Transaction.commit();
 * </pre></blockquote>
 * <p>
 * All <code>BeanFactory</code> calls may be made outside a transaction.  In all cases this is equivalent
 * to making the same call from within a transaction and then immediately committing it.  However, in many
 * implementations of <code>BeanFactory</code> this done more efficiently.
 * For example, a call outside a transaction to <code>userFactory.lookup(userName)</code> is equivalent to:
 * <p><blockquote><pre>
 *     Transaction.begin();
 *     User u = userFactory.lookup(userName);
 *     Transaction.commit();
 *     return u;
 * </pre></blockquote>
 * <p>
 * All <code>BeanFactory</code> calls throw <code>RollbackException</code> in case of failure.
 * No other exceptions are thrown from <code>BeanFactory</code> methods.  Any internally thrown
 * exceptions are caught and a <code>RollbackException</code> is thrown with the internal exception
 * as the <tt>RollbackException</tt>'s cause.
 * Any enclosing transaction is rolled back before <code>RollbackException</code> is thrown.
 * <p>
 * All transactions should be committed or rolled back before awaiting user input.
 * If transactions are left running, locks may be held in the underlying database causing new transactions
 * to timeout.  To guard against accidentally not committing or rolling back a transaction (perhaps because of
 * a programming bug or an unexpected exception in non-<tt>BeanFactory</tt> code)
 * you should always put the transaction in a <code>try</code> /
 * <code>catch</code> statement with a <code>finally</code> clause that commits or rolls back the transaction.
 * For example:
 * <p><blockquote><pre>
 *     try {
 *         Transaction.begin();
 *         ...
 *         Transaction.commit();
 *     } catch (RollbackException e) {
 *         ...
 *     } finally {
 *         if (Transaction.isActive()) Transaction.rollback();
 *     }
 * </pre></blockquote>
 * <p>
 * @author  Jeffrey Eppinger
 * @see     org.mybeans.factory.BeanTable
 * @see     org.mybeans.factory.Transaction
 */

public interface BeanFactory<B> {
	
	/**
	 * Computes a message digest of all the properties for the given bean.  The digest is returned as a String.
	 * Two beans with the same property values with have digest strings with the same value.
	 * <p>
	 * For properties that are references to other beans backed by another BeanFactory, only the primary key
	 * values for the referenced bean are used to compute the digest.
	 * <p>
	 * Digest strings returned from this method are not meant for long-term storage.  Future releases of the
	 * BeanFactory may compute the digest and/or encode the digest into a string using different algorithms.
	 * The current implementation uses the SHA1 algorithm to compute the digest and returns it as a hex string.
	 * <p>
	 * The primary use of this method is to easily compute a digest for a bean so as to detect that some other
	 * thread/user has changed the bean between this thread/user's transactions.
	 * @param bean the bean from which to extract values for the digest.
	 * @return a digest of the beans properties.
	 */
	public String computeDigest(B bean) throws RollbackException;
	
	/**
	 * Copies the values of the properties from one bean into another bean
	 * (except for the primary key).
	 *
	 * If the <code>to</code> bean into which changes are being copied is a bean being tracked
	 * by an active transaction, then properties changed by this call will be written out if the
	 * transaction commits.
	 *
	 * When copying an array, a new array is created containing the same values as the original.
	 * When copying, modifiable Dates and Times are cloned; references to non-modifiable objects
	 * are simply copied.
	 *
	 * Care should be taken with properties that reference beans backed by a referenced <code>BeanFactory</code>;
	 * in this case, only the reference to the bean is copied.  No cloning occurs.  (See section on
	 * referenced beans, above.)
     *
	 * Primary key property values are not copied
	 * (because you are not allowed to change a bean's primary key).
	 * To write an untracked bean into the database, first use the <code>create()</code>
	 * or <code>lookup()</code> methods to to obtain a tracked bean and then call this method
	 * to copy the other property values into the tracked bean.
	 *
	 * @param from the bean from which property values are copied
	 * @param to the bean into which the property values are set
	 * @throws RollbackException in case of problems, specifically if either argument is null
	 */
	public void copyInto(B from, B to) throws RollbackException;

	/**
	 * Creates a new instance of B with the given primary key.
	 *
	 * If this call is made in an enclosing transaction, the returned bean is tracked
	 * and checked at commit time.  Any changed properties of tracked beans will be
	 * written to the database at commit time.  (Primary key properties cannot be changed.)
     *
     * If the bean has a primary key that is of type <code>int</code> or <code>long</code>, this
     * method can be called with no arguments and the <code>BeanFactory</code> (or the underlying database)
     * will use the next available key value for the primary key when instantiating the new bean.
     *
	 * @param primaryKeyValues the values of the properties that comprise the primary key for
     * bean being created.
     * @return a reference to an instance of <code>B</code> with the given primary key and values
     * set by calling one of <tt>B</tt>'s constructors.  (See description above on constructors.)
	 * @throws DuplicateKeyException if a bean with the given primary key is already in the database.
	 * This is a (subclass of) RollbackException, so any enclosing transaction is rolled back.
     * @throws ReferencedBeanException if the primary key contains a referenced
     * bean that is not stored in the database.
     * This is a (subclass of) RollbackException, so any enclosing transaction is rolled back.
	 * @throws RollbackException if the transaction cannot be completed for any one of a number of reasons,
	 * including SQLExceptions, deadlocks, errors accessing the bean,
     * the <code>primaryKeyValues</code> are not of the correct types, etc.
	 */
	public B create(Object... primaryKeyValues) throws RollbackException;

    /**
     * Deletes from the database the bean with the given primary key.
     *
     * @param primaryKeyValues the values of the properties that comprise the primary key for
     * bean being deleted.
     * @throws RollbackException if there is no bean in the database with this primary key or
     *     if there is an error accessing the database, including IOException or deadlock.
     *     (To avoid getting a RollbackException deleting a bean that doesn't exist in the
     *     database, first check to see if it's already there using the
     *     <code>lookup()</code> method.)
     */
    public void delete(Object... primaryKeyValues) throws RollbackException;

	/**
	 * Tests two beans to see whether all the property values are the same.
	 *
	 * If a property is of a primitive type, two property values are the same if they are <code>==</code>.
	 * If a property type is a <code>String</code>, <code>Date</code>, or <code>Time</code>,
	 * the the property values are the same if both are <code>null</code>
	 * or both are <code>equals()</code> (using <code>java.lang.Object.equals()</code>).
	 * If a property type is a bean backed by a referenced <code>BeanFactory</code>,
	 * then the two property values are the same if both are <code>null</code> or both refer to
	 * beans that have the same primary key value.
	 * If a property type is an array, then the two property values are the same if they are both
	 * <code>null</code> or both the same length and each corresponding element is the same as described above (herein).
	 * @param bean1 one bean
	 * @param bean2 another bean
	 * @return true of each property is the same, false otherwise
	 * @throws RollbackException in case of problems, specifically if either argument is null
	 */
    public boolean equals(B bean1, B bean2) throws RollbackException;
    
    /**
     * Returns the number of beans in the database.
     *
     * When run inside a transaction, this will lock the whole database (table).
     *
     * @return the number of beans in the database.
     *
     * @throws RollbackException if there is an error accessing the database, including IOException or deadlock.
     */
    public int getBeanCount() throws RollbackException;
    
    public Object[] getPrimaryKeyValues(B bean) throws RollbackException;

    /**
     * Looks up a bean by its primary key.
     * If this call is made in an enclosing transaction, the returned bean is tracked
	 * and checked at commit time.  Any changed properties of tracked beans will be
	 * written to the database at commit time.  (Primary key properties cannot be changed.)
     *
     * Referenced beans will be instantiated using the appropriate
     * factory and the foreign key values stored in this table.
     * Referenced beans will also be tracked if this call is made in an enclosing transaction.
     *
     * @param primaryKeyValues the values of the properties that comprise the primary key for
     * bean being looked up.
     * @return a reference to an instance of <code>B</code> with the given primary key and values populated from the database.
     * If there is no such bean, then <code>null</code> is returned.
     * @throws ReferencedBeanException if the looked up bean references another bean that is not stored in the database.
     * This is a (subclass of) RollbackException, so any enclosing transaction is rolled back.
     * @throws RollbackException if there is an error accessing the database,
     * including IOException or deadlock.
     */
    public B lookup(Object... primaryKeyValues) throws RollbackException;

    /**
     * Searches the database for beans matching the given constraints.
     * Constraints are specified with <code>MatchArg</code>s which limit properties to
     * values or ranges, such as equals, less-than or greater-than a given value.
     * Operators on strings also include starts-with, ends-with, and contains.
     * Beans are instantiated as if <tt>lookup()</tt> was called.
     *
     * If no constraints are specified, all the beans in the database (table) are returned.
     *
     * @param constraints zero or more contraints, all of which must be <code>true</code> for each bean
     * returned by this call.
     * @return an array of beans that match the given constraints.  If no beans match the
     * constraints, a zero length array is returned.
     * @throws ReferencedBeanException if a bean satisfying the constraints references another bean that is not stored in the database.
     * This is a (subclass of) RollbackException, so any enclosing transaction is rolled back.
     * @throws RollbackException if there is an error accessing the database,
     * including IOException or deadlock.
     */
    public B[] match(MatchArg... constraints) throws RollbackException;
}

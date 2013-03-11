package model;

import java.io.Serializable;

import org.mybeans.dao.*;
import org.mybeans.factory.*;

import databeans.UserInfo;

public class UserInfoDAO implements Serializable{
	private BeanFactory<UserInfo> factory;
	
	public UserInfoDAO(String jdbcDriverName, String jdbcURL) throws DAOException {
		try {
			// Get a BeanTable so we can create the "user" table
	        BeanTable<UserInfo> userTable = BeanTable.getGoogleSQLInstance(
	        								UserInfo.class,
	        								"UserInfo",
	        								jdbcDriverName,
	        								jdbcURL);
	        
	        if (!userTable.exists()) 
	        	userTable.create("userID");
	        
	        // Long running web apps need to clean up idle database connections.
	        // So we can tell each BeanTable to clean them up.  (You would only notice
	        // a problem after leaving your web app running for several hours.)
	        // userTable.setIdleConnectionCleanup(true);
	
	        // Get a BeanFactory which the actions will use to read and write rows of the "user" table
	        factory = userTable.getFactory();
		} catch (BeanFactoryException e) {
			throw new DAOException(e);
		}
	}
	
	public synchronized void create(UserInfo user) throws DAOException {
			try {
	        	Transaction.begin();
	        	// Check if username exists ?
	        	UserInfo[] users = factory.match();
	        	if(users != null)
	        	{
		        	for(UserInfo eachUser : users)
		        	{
		        		if(eachUser.getName().equals(user.getName()) == true)
		        		{
		        			throw new DAOException("A User by username " + eachUser.getName() + " already exits !");
		        		}
		        	}
	        	}
				UserInfo dbUser = factory.create();
				factory.copyInto(user, dbUser);
				Transaction.commit();
			} catch (DuplicateKeyException e) {
				throw new DAOException("Duplicate Entry not allowed in Database");
			} catch (RollbackException e) {
				throw new DAOException("Database Error");
			} finally {
				if (Transaction.isActive()) Transaction.rollback();
			}
		}
		
		public synchronized UserInfo[] getUserList() throws DAOException{
			try {
				UserInfo[] users = factory.match();
				//TODO Arrays.sort(users);  // We want them sorted by last and first names (as per User.compareTo());
				return users;
			} catch (RollbackException e) {
				throw new DAOException(e);
			}
    	}
		
		protected BeanFactory<UserInfo> getFactory() throws DAOException{ 
			return factory; 
		}
		
		public synchronized UserInfo searchName(String uname) throws DAOException{
			UserInfo[] users;
			try {
				users = factory.match(MatchArg.equals("uname", uname));
			} catch (RollbackException e) {
				// TODO Auto-generated catch block
				throw new DAOException(e);
			}
			
			if(users == null){
				System.out.println("none users");
				return null;
			}
			
			if(users.length > 1 || users.length <= 0)
				return null;
			else
				return users[0];
		}
}

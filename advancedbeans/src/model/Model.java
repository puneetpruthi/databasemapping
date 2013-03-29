package model;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.mybeans.dao.DAOException;

public class Model {
	private UserInfoDAO userDAO;
	private DatedUserDAO dateduserDAO;
	
	public Model(ServletConfig config) throws ServletException {
		try {
			String jdbcDriver = config.getInitParameter("jdbcDriverName");
			String jdbcURL    = config.getInitParameter("jdbcURL");
			
			//System.out.println("Creating Table if not present");
			userDAO   = new UserInfoDAO(jdbcDriver, jdbcURL);
			dateduserDAO = new DatedUserDAO(jdbcDriver, jdbcURL);
		} catch (DAOException e) {
			throw new ServletException(e);
		}
	}
	
	public UserInfoDAO    getUserInfoDAO() 	  						{ return userDAO;   }
	public DatedUserDAO   getDatedUserDAO() 	  					{ return dateduserDAO;   }
}
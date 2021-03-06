package advancedbeans;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.rdbms.AppEngineDriver;
import com.google.cloud.sql.jdbc.Connection;
import com.google.cloud.sql.jdbc.PreparedStatement;
import com.google.cloud.sql.jdbc.ResultSet;

import databeans.UserInfo;
//import com.google.appengine.api.datastore.DatastoreService;
//import com.google.appengine.api.datastore.DatastoreServiceFactory;
//import com.google.appengine.api.datastore.Entity;
//import com.google.appengine.api.datastore.FetchOptions;
//import com.google.appengine.api.datastore.Key;
//import com.google.appengine.api.datastore.KeyFactory;
//import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class AdvancedbeansServlet extends HttpServlet {
//	private UserInfoDAO userDAO;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		Connection conn = null;
		PrintWriter outputStream = resp.getWriter();
		
		resp.setContentType("text/html");
		outputStream.println("Google SQL EXAMPLE");
		print_form(outputStream);

//		ServletConfig config = getServletConfig();
//		String jdbcDriver = config.getInitParameter("jdbcDriverName");
//		String jdbcURL    = config.getInitParameter("jdbcURL");
//		
//		//System.out.println("Creating Table if not present");
//		try {
//			userDAO   = new UserInfoDAO(jdbcDriver, jdbcURL);
//		} catch (DAOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		if(userDAO == null)
//			outputStream.println("<span style=\"color:#ff0000\">Failure in creating DAO</span><br>");	
		
		if(req.getQueryString() == null){
			outputStream.println("<span style=\"color:#ff0000\">No query string was provided</span><br>");
		}
		else{
			if (req.getParameter("gname") != null){
				outputStream.println("<span style=\"color:#ff0000\">You have asked to retrieve data for " + req.getParameter("gname") + "</span><br>");
			    try {
				    String statement;
				    PreparedStatement prepStmt;

					DriverManager.registerDriver(new AppEngineDriver());
				    conn = (Connection) DriverManager.getConnection("jdbc:google:rdbms://gsqlbeanfactory:googlebeanfactorydb/ex1");
				    
				    //Get data from the table
				    statement ="SELECT * from usertable";
				    prepStmt = conn.prepareStatement(statement);
				    ResultSet rs = prepStmt.executeQuery(statement);
				    outputStream.println("Results populated <br>");
				    while(rs.next())
				    {
				    	UserInfo thisUser = new UserInfo();
				    	thisUser.setName(rs.getString("userName"));
				    	thisUser.setEmail(rs.getString("emailID"));
				    	thisUser.setPassword(rs.getString("password"));
				    	
				    	outputStream.println(thisUser.toString() + "<br>");
				    }
				    outputStream.println("Results ended <br>");
			    } catch (SQLException e) {
					// TODO Auto-generated catch block
					outputStream.println("Exception Caught:" + e.getMessage());
				}
			}
			else if(req.getParameter("pname") != null){				
				String newName = req.getParameter("pname");
				String newEmail = req.getParameter("pemail");
				String newPass = req.getParameter("ppass");
				
				if(newName == null || newName == "" || newEmail == null || newEmail == "" || newPass == null || newPass == "")
					outputStream.println("<span style=\"color:#ff0000\">Failure in inputing</span>");
				
				UserInfo reqUser = new UserInfo();
				
				reqUser.setName(newName);
				reqUser.setPassword(newPass);
				reqUser.setEmail(newEmail);
				
				if(insertData(reqUser, outputStream) != 0)
				{
					outputStream.println("<span style=\"color:#ff0000\">Failure in inputing</span>");	
				}
				else
				{
					outputStream.println("<span style=\"color:#0000FF\">" + reqUser.getName() + " is added successfully !</span>");
				}
			}
			else
				outputStream.println("Invalid servlet call");
		}
}
	
	public int insertData(UserInfo newUser, PrintWriter outputStream)
	{
		if (newUser == null)
		{
			return -1;
		}
				
	    try {
	    	Connection conn;
		    String statement;
		    PreparedStatement prepStmt;

			DriverManager.registerDriver(new AppEngineDriver());
		    conn = (Connection) DriverManager.getConnection("jdbc:google:rdbms://gsqlbeanfactory:googlebeanfactorydb/ex1");

		    //Get data from the table
		    statement ="INSERT INTO usertable (userName, emailID, password) VALUES(?,?,?)";
		    outputStream.println("SQL Query: " + statement + "<br>");
		    prepStmt = conn.prepareStatement(statement);
		    prepStmt.setString(1, newUser.getName());
		    prepStmt.setString(2, newUser.getEmail());
		    prepStmt.setString(3, newUser.getPassword());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			outputStream.println("Exception Caught:" + e.getMessage());
			return -1;
		}
	    
//		try {
//			userDAO.create(newUser);
//		} catch (DAOException e) {
//			// TODO Auto-generated catch block
//			outputStream.println(e.getMessage());
//			return -1;
//		}
//		Key userKey = KeyFactory.createKey("simpleUser", "tempDB");
//        Entity thisUser = new Entity("Userlist", userKey);
//        thisUser.setProperty("name", newUser.getName());
//        thisUser.setProperty("age", newUser.getEmail());

//        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
//        datastore.put(thisUser);
        return 0;
	 }
	
	private void print_form(PrintWriter outputStream) {
		// TODO Auto-generated method stub
		outputStream.println("<table border=\"1\">");
		outputStream.println("<tr><td>");
		outputStream.println("<p>Insert new entry</p>");
		outputStream.println("<form action=\"/advancedbeans\" method=\"GET\">");
		outputStream.println("Enter your name: <input type=\"text\" name=\"pname\"><br>");
		outputStream.println("Enter your email: <input type=\"text\" name=\"pemail\"><br>");
		outputStream.println("Enter your password: <input type=\"text\" name=\"ppass\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Enter\">");
		outputStream.println("</form>");
		outputStream.println("</td><td>");
		outputStream.println("<p>Get All Entries</p>");
		outputStream.println("<form action=\"/advancedbeans\" method=\"GET\">");
		outputStream.println("<input hidden=\"true\" type=\"text\" name=\"gname\" value=\"xxx\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Get\">");
		outputStream.println("</form>");
		outputStream.println("</td></tr>");
		outputStream.println("</table>");
	}
	
//	public UserInfoDAO    getUserInfoDAO() 	  						{ return userDAO;   }

}
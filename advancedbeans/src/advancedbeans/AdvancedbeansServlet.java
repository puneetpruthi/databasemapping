package advancedbeans;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.http.*;

import org.mybeans.dao.DAOException;

import dao.UserInfoDAO;

//import com.google.appengine.api.datastore.DatastoreService;
//import com.google.appengine.api.datastore.DatastoreServiceFactory;
//import com.google.appengine.api.datastore.Entity;
//import com.google.appengine.api.datastore.FetchOptions;
//import com.google.appengine.api.datastore.Key;
//import com.google.appengine.api.datastore.KeyFactory;
//import com.google.appengine.api.datastore.Query;

import databeans.UserInfo;

@SuppressWarnings("serial")
public class AdvancedbeansServlet extends HttpServlet {
	private UserInfoDAO userDAO;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		PrintWriter outputStream = resp.getWriter();
		outputStream.println("Google SQL EXAMPLE");

		ServletConfig config = getServletConfig();
		String jdbcDriver = config.getInitParameter("jdbcDriverName");
		String jdbcURL    = config.getInitParameter("jdbcURL");
		
		//System.out.println("Creating Table if not present");
		try {
			userDAO   = new UserInfoDAO(jdbcDriver, jdbcURL);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(userDAO == null)
			outputStream.println("<span style=\"color:#ff0000\">Failure in creating DAO</span><br>");	
		
		if(req.getQueryString() == null){
			outputStream.println("<span style=\"color:#ff0000\">No query string was provided</span><br>");
		}
		else{
			if (req.getParameter("gname") != null){
//				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
//				Key userKey = KeyFactory.createKey("simpleUser", "tempDB");
//				Query query = new Query("Userlist", userKey);
//				java.util.List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(500));
//				if(users.isEmpty())				
//				{
//					outputStream.println("<span style=\"color:#ff0000\">Could not find name</span>");	
//				}
//				else
//				{
//					outputStream.println("<b>Data base entries:</b>");
//			        for (Entity iter : users) {
//			        	outputStream.println("<br><span style=\"color:#0000FF\">" + iter.getProperty("name") + " of age " + iter.getProperty("age")  + "</span>");
//			        }
//				}
//
//				retrieveData(req, outputStream);
				outputStream.println("You have asked to retrieve data<br>");
		}
			else if(req.getParameter("pname") != null){
				String newName = (String)req.getParameter("pname");
				String newEmail = (String)req.getParameter("pemail");
				
				if(newName == null || newName == "" || newEmail == null || newEmail == "")
					outputStream.println("<span style=\"color:#ff0000\">Failure in inputing</span>");
				
				UserInfo reqUser = new UserInfo(newName, newEmail);
				
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
		
		print_form(outputStream);
	}
	
	private void print_form(PrintWriter outputStream) {
		// TODO Auto-generated method stub
		outputStream.println("<table border=\"1\">");
		outputStream.println("<tr><td>");
		outputStream.println("<p>Insert new entry</p>");
		outputStream.println("<form action=\"/advancedbeans\" method=\"GET\">");
		outputStream.println("Enter your name: <input type=\"text\" name=\"pname\"><br>");
		outputStream.println("Enter your email: <input type=\"text\" name=\"pemail\"><br>");
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
	
	public int insertData(UserInfo newUser, PrintWriter outputStream)
	{
		if (newUser == null)
		{
			return -1;
		}
		
		try {
			userDAO.create(newUser);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			outputStream.println(e.getMessage());
			return -1;
		}
//		Key userKey = KeyFactory.createKey("simpleUser", "tempDB");
//        Entity thisUser = new Entity("Userlist", userKey);
//        thisUser.setProperty("name", newUser.getName());
//        thisUser.setProperty("age", newUser.getEmail());

//        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
//        datastore.put(thisUser);
        return 0;
	 }
	
	public UserInfoDAO    getUserInfoDAO() 	  						{ return userDAO;   }

}
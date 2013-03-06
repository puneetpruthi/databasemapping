package com.simpleExample;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.databases.PMF;
import com.databases.simpleUser;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

public class retrieveData extends HttpServlet{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		PrintWriter outputStream = resp.getWriter();
		print_form(outputStream);

		if(req.getQueryString() == null)
		{
			outputStream.println("This is a get request without query string");
		}
		else
		{
			// An actual get request with query string
			outputStream.println("<p>This is a get request with query string</p><br>");
			//outputStream.println("<p>Name entered = " + req.getParameter("gname") + "</p><br>");
			
			if(req.getParameter("gname") == null || req.getParameter("gname") == "")
				outputStream.println("<span style=\"color:#ff0000\">Please enter a Valid name</span>");
			else
			{
				DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
				Key userKey = KeyFactory.createKey("simpleUser", "tempDB");
				Query query = new Query("Userlist", userKey);
				java.util.List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(500));
				if(users.isEmpty())				
				{
					outputStream.println("<span style=\"color:#ff0000\">Could not find name</span>");	
				}
				else
				{
					outputStream.println("<b>Data base entries:</b>");
			        for (Entity iter : users) {
			        	outputStream.println("<br><span style=\"color:#0000FF\">" + iter.getProperty("name") + " of age " + iter.getProperty("age")  + "</span>");
			        }
				}
			}				
		}
	}
	
	private void print_form(PrintWriter outputStream) {
		// TODO Auto-generated method stub
		outputStream.println("<br>====================<br><b>DATABASE EXAMPLE</b><br>====================<br><br>");
		outputStream.println("<table border=\"1\">");
		outputStream.println("<tr><td>");
		outputStream.println("<p>Insert new entry</p>");
		outputStream.println("<form action=\"/insertData\" method=\"GET\">");
		outputStream.println("Enter your name: <input type=\"text\" name=\"pname\"><br>");
		outputStream.println("Enter your age : <input type=\"text\" name=\"page\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Enter\">");
		outputStream.println("</form>");
		outputStream.println("</td><td>");
		outputStream.println("<p>Get All Entries</p>");
		outputStream.println("<form action=\"/retrieveData\" method=\"GET\">");
		outputStream.println("<input hidden=\"true\" type=\"text\" name=\"gname\" value=\"xxx\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Get\">");
		outputStream.println("</form>");
		outputStream.println("</td></tr>");
		outputStream.println("</table>");
	}

	private simpleUser getData(String parameter) {
		if (parameter == null)
		{
			return null;
		}
		
	    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	    Key userKey = KeyFactory.createKey("simpleUser", "tempDB");
	    // Run an ancestor query to ensure we see the most up-to-date
	    // view of the Greetings belonging to the selected Guestbook.
	    Query query = new Query("Userlist", userKey).addSort("name", Query.SortDirection.DESCENDING);
	    java.util.List<Entity> users = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5));
	    
		// TODO Auto-generated method stub
		Key k = KeyFactory.createKey(simpleUser.class.getName(), parameter);
		 PersistenceManager pm = PMF.get().getPersistenceManager();
		    try {
		    	simpleUser retUser = pm.getObjectById(simpleUser.class, k);
		    	return retUser;
		    } finally {
		      pm.close();
		    }
	}	
}

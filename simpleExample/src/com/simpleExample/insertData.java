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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class insertData extends HttpServlet{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		PrintWriter outputStream = resp.getWriter();
		print_form(outputStream);
		outputStream.println("This is a post request<br>");
		
		if(req.getQueryString() == null)
		{
			outputStream.println("No query string<br>");
			outputStream.println(req.getContextPath());
			outputStream.println(req.getAttribute("pname"));
			outputStream.println(req.getPathInfo());
		}
		else
		{
			if(req.getParameter("pname") == null || req.getParameter("page") == null)
				outputStream.println("<span style=\"color:#ff0000\">Please enter a valid name and age</span>");
			else
			{
				String newName = (String)req.getParameter("pname");
				int newAge = Integer.parseInt(req.getParameter("page"));
				simpleUser reqUser = new simpleUser(newName, newAge);
				if(insertData(reqUser) != 0)
				{
					outputStream.println("<span style=\"color:#ff0000\">Failure in inputing</span>");	
				}
				else
				{
					outputStream.println("<span style=\"color:#0000FF\">" + reqUser.getName() + " is added successfully !</span>");
				}
			}				
		}
	}
	
	private void print_form(PrintWriter outputStream) {
		// TODO Auto-generated method stub
		outputStream.println("<br><br><b>DATABASE EXAMPLE</b><br>====================<br>");
		outputStream.println("<table border=\"1\">");
		outputStream.println("<tr><td>");
		outputStream.println("<p>Insert new entry</p>");
		outputStream.println("<form action=\"/insertData\" method=\"GET\">");
		outputStream.println("Enter your name: <input type=\"text\" name=\"pname\"><br>");
		outputStream.println("Enter your age: <input type=\"text\" name=\"page\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Enter\">");
		outputStream.println("</form>");
		outputStream.println("</td><td>");
		outputStream.println("<p>Get Age</p>");
		outputStream.println("<form action=\"/retrieveData\" method=\"GET\">");
		outputStream.println("Search name: <input hidden=\"true\" type=\"text\" name=\"gname\" value=\"xxx\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Get\">");
		outputStream.println("</form>");
		outputStream.println("</td></tr>");
		outputStream.println("</table>");
	}

	public int insertData(simpleUser newUser)
	{
		if (newUser == null)
		{
			return -1;
		}
		Key userKey = KeyFactory.createKey("simpleUser", "tempDB");
        Entity thisUser = new Entity("Userlist", userKey);
        thisUser.setProperty("name", newUser.getName());
        thisUser.setProperty("age", newUser.getAge());

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(thisUser);
        return 0;
	 }

}

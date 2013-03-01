package com.simpleExample;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.databases.PMF;
import com.databases.simpleUser;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public class DatabaseServlet extends HttpServlet{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		PrintWriter outputStream = resp.getWriter();
		
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
				simpleUser reqUser = getData(req.getParameter("gname"));
				if(reqUser == null)
				{
					outputStream.println("<span style=\"color:#ff0000\">Could not find name</span>");	
				}
				else
				{
					outputStream.println("<span style=\"color:#0000FF\">Age for " + reqUser.getName() + "is" + reqUser.getAge() + "</span>");
				}
			}				
		}
		print_form(outputStream);
	}
	
	private void print_form(PrintWriter outputStream) {
		// TODO Auto-generated method stub
		outputStream.println("<br><br><b>===== DATABASE EXAMPLE ======</b><br><br>");
		outputStream.println("<table border=\"1\">");
		outputStream.println("<tr><td>");
		outputStream.println("<p>Insert new entry</p>");
		outputStream.println("<form action=\"/databaseexample\" method=\"POST\">");
		outputStream.println("Enter your name: <input type=\"text\" name=\"pname\"><br>");
		outputStream.println("Enter your age: <input type=\"text\" name=\"page\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Enter\">");
		outputStream.println("</form>");
		outputStream.println("</td><td>");
		outputStream.println("<p>Get Age</p>");
		outputStream.println("<form action=\"/databaseexample\" method=\"GET\">");
		outputStream.println("Search name: <input type=\"text\" name=\"gname\"><br>");
		outputStream.println("<input type=\"submit\" value=\"Get\">");
		outputStream.println("</form>");
		outputStream.println("</td></tr>");
		outputStream.println("</table>");
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		PrintWriter outputStream = resp.getWriter();
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
			// An actual get request with query string
			outputStream.println("<p>This is a get request with query string" + req.getQueryString() + "</p><br>");
			outputStream.println("<p>Name inserting = " + req.getParameter("pname") + "</p><br>");
			
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
					outputStream.println("<span style=\"color:#0000FF\">" + reqUser.getName() + "is added successfully !</span>");
				}
			}				
		}
	}
	
	private simpleUser getData(String parameter) {
		if (parameter == null)
		{
			return null;
		}
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

	public int insertData(simpleUser newUser)
	{
		if (newUser == null)
		{
			return -1;
		}
		int ret = -1;
	    PersistenceManager pm = PMF.get().getPersistenceManager();
	    try {
	      pm.makePersistent(newUser);
	      ret = 0;
	    } finally {
	      pm.close();
	    }
	    return (ret);
	 }

}

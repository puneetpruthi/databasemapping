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
		print_form(outputStream);
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
}

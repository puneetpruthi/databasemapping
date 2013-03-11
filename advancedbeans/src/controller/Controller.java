package controller;

import model.*;
import controller.*;
import databeans.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@SuppressWarnings("serial")
public class Controller extends HttpServlet{
	Model model;
    public void init() throws ServletException {
    	  restoreDatabase(getServletConfig());
    }

    void restoreDatabase(ServletConfig config) throws ServletException
    {
    	model = new Model(config);
    	
        Action.add(new CreateUserAction(model));
        Action.add(new DisplayUserAction(model));
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request,response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Check if return is a json string or a nextPage ?
        // Check if tables are present
        restoreDatabase(getServletConfig());
        String nextPage = performTheAction(request);
        
        // Continue processing
        if(nextPage == null)
        {
        	return;
        }
        else if(nextPage.contains(".do") == true || 
           nextPage.contains(".jsp") == true ||
           nextPage.contains(".html") == true)
        {
        	sendToNextPage(nextPage,request,response);
        }
        else
        {
        	//JSON String
        	sendJSONString(nextPage,request,response);
        }
    }
    
    private void sendJSONString(String jsonString, HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
//    	String requestingURL = request.getRequestURI();
    	PrintWriter out;
		try {
			out = response.getWriter();
	    	out.println(jsonString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
     * Extracts the requested action and (depending on whether the user is logged in)
     * perform it (or make the user login).
     * @param request
     * @return the next page (the view)
     */
    private String performTheAction(HttpServletRequest request) {
        HttpSession session     = request.getSession(true);
        String      servletPath = request.getServletPath();
        UserInfo    user = (UserInfo) session.getAttribute("user");
        String      action = getActionName(servletPath);

        //System.out.println("servletPath="+servletPath+" requestURI="+request.getRequestURI()+"  user=" + (user != null?user.getUname():"NO USER"));
        //System.out.println( "Perform the action = " + action);

        String completeURL = request.getRequestURL().append("?").append( 
           	 request.getQueryString()).toString();
           System.out.println( "Request string = " + completeURL);
//           if (SQLTest.test(completeURL) == true )
//           {
//           	request.setAttribute("errorMsg", "Invalid Query");
//           	return "error.jsp";
//           }
//           
          Map params = (Map) request.getParameterMap();
          Iterator i = ((java.util.Map<String, String[]>) params).keySet().iterator();
          
          while ( i.hasNext() )
          {
             String key = (String) i.next();
             String value = ((String[]) ((java.util.Map<String, String[]>) params).get( key ))[ 0 ];
             if (SQLTest.test(value) == true )
             {
             	request.setAttribute("errorMsg", "Invalid Query/Parameters");
             	return "error.jsp";
             }
          
          }
        
        if (action.equals("start")) {
       // 	System.out.println("controller Aap dude hain");
        	// If he's logged in but back at the /start page, send him to manage his pics
			return Action.perform("manage.do",request);
        }

        // SEARCH FOR THE ACTION
        if(Action.isValidAction(action) == false)
        {
        	System.out.println("Invalid Action");
        	request.setAttribute("errorMsg", "This Page Does not Exists");
        	return "error.jsp";
        }
        
        if (action.equals("register.do") || action.equals("login.do")) {
        	// Allow these actions without logging in
			return Action.perform(action,request);
        }
        System.out.println("not a login");
        if (user == null) {
        	// If the user hasn't logged in, direct him to the login page
			return Action.perform("login.do",request);
        }
        System.out.println("Perform anyway");
      	// Let the logged in user run his chosen action
		return Action.perform(action,request);
    }
    
    /*
     * If nextPage is null, send back 404
     * If nextPage starts with a '/', redirect to this page.
     *    In this case, the page must be the whole servlet path including the webapp name
     * Otherwise dispatch to the page (the view)
     *    This is the common case
     * Note: If nextPage equals "image", we will dispatch to /image.  In the web.xml file, "/image"
     *    is mapped to the ImageServlet will which return the image bytes for display.
     */
    private void sendToNextPage(String nextPage, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    //	System.out.println("nextPage="+nextPage);
    	
    	if (nextPage == null) {
    		response.sendError(HttpServletResponse.SC_NOT_FOUND,request.getServletPath());
    		return;
    	}
    	
    	if (nextPage.charAt(0) == '/') {
			String host  = request.getServerName();
			String port  = ":"+String.valueOf(request.getServerPort());
			if (port.equals(":80")) port = "";
			//System.out.println("http://"+host+port+nextPage);
			response.sendRedirect("http://"+host+port+nextPage);
			return;
    	}
    	
    	RequestDispatcher d = request.getRequestDispatcher("/"+nextPage);
    	//System.out.println(d.);
   		d.forward(request,response);
    }

	/*
	 * Returns the path component after the last slash removing any "extension"
	 * if present.
	 */
    private String getActionName(String path) {
    	// We're guaranteed that the path will start with a slash
        int slash = path.lastIndexOf('/');
        return path.substring(slash+1);
    }
}
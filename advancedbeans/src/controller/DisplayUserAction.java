package controller;

import javax.servlet.http.HttpServletRequest;

import model.DatedUserDAO;
import model.Model;
import model.UserInfoDAO;

import org.mybeans.dao.DAOException;

import databeans.DatedUser;
import databeans.UserInfo;


/*
 * Processes the parameters from the form in register.jsp.
 * If successful:
 *   (1) creates a new User bean
 *   (2) sets the "user" session attribute to the new User bean
 *   (3) redirects to view the originally requested photo.
 * If there was no photo originally requested to be viewed
 * (as specified by the "redirect" hidden form value),
 * just redirect to manage.do to allow the user to add some
 * photos.
 */
public class DisplayUserAction extends Action {
//	private FormBeanFactory<DisplayUserForm> formBeanFactory = FormBeanFactory.getInstance(DisplayUserForm.class,"<>\"");

	private UserInfoDAO userDAO;
	private DatedUserDAO dateduserDAO;
	
	public DisplayUserAction(Model model) {
		userDAO = model.getUserInfoDAO();
		dateduserDAO = model.getDatedUserDAO();
	}

	@Override
	public String getName() { return "DisplayUser.do"; }

    @Override
	public String perform(HttpServletRequest request) {
    	//System.out.println("register.do called");
    	
//    	DisplayUserForm form = formBeanFactory.create(request);
//    	
//        // Set up the request attributes (the errors list and the form bean so
//        // we can just return to the jsp with the form if the request isn't correct)
        String errorMsg;
//        
//        request.setAttribute("form",form);
//
//        // If no params were passed, return with no errors so that the form will be
//        // presented (we assume for the first time).
//        if (!form.isPresent()) {
//        	//System.out.println("Form not present");
//            return "basic.jsp";
//        }
//
//        // Any validation errors?
//        errorMsg = form.getValidationErrors();
//        if (errorMsg != null) {
//        	request.setAttribute("errorMsg", errorMsg);
//        	//System.out.println("Validation failed");
//            return "basic.jsp";
//        }
        
		UserInfo [] userlist;
		DatedUser [] dateduserlist;

        try {
        	userlist = userDAO.getUserList();
        	request.setAttribute("result", userlist);
        	dateduserlist = dateduserDAO.getUserList();
        	request.setAttribute("dresult", dateduserlist);
        	return "basic.jsp";
        } catch (DAOException e) {
        	//System.out.println(e.getMessage());
        	errorMsg = e.getMessage();
        	request.setAttribute("errorMsg", errorMsg);
        	return "basic.jsp";
        }
    }
}

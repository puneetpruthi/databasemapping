<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- The HTML 4.01 Transitional DOCTYPE declaration-->
<!-- above set at the top of the file will set     -->
<!-- the browser's rendering engine into           -->
<!-- "Quirks Mode". Replacing this declaration     -->
<!-- with a "Standards Mode" doctype is supported, -->
<!-- but may lead to some differences in layout.   -->

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>Bean Factory Project</title>
  </head>

<% 
	HttpSession thisSession = request.getSession();
	String errorMsg = (String)request.getAttribute("errorMsg");
	String goodMsg = (String)request.getAttribute("goodMsg");
%>

  <body>
    <h1>Bean Factory Project for Google SQL</h1>

	<table border="1">
	<tr><td>
	<p>Insert new entry</p>
		<form action="/CreateUser.do" method="GET">
			Enter your name: <input type="text" name="pname"><br>
			Enter your email: <input type="text" name="pemail"><br>
			Enter your password: <input type="text" name="ppass"><br>
			<input type="submit" value="Enter">
		</form>
	</td><td>
		<p>Get All Entries</p>
		<form action="/DisplayUser.do" method="GET">
			<input type="submit" value="Get">
		</form>
	</td></tr>
	</table>

	        	 <c:choose>  
	       			<c:when test="${not empty result}">  
	       				<br><a>Users in database</a><br>
	              	  <c:forEach items="${result}" var="item">
						<a>Name: ${item.name} >>> Email : ${item.email} >>> Pass : ${item.password}</a><br>											
					  </c:forEach> 
        			</c:when>  
	   	  		 </c:choose>  
				<c:remove var="result"></c:remove> 

	        	 <c:choose>  
	       			<c:when test="${not empty dresult}">  
	       				<br><a>Users in database</a><br>
	              	  <c:forEach items="${dresult}" var="item">
						<a>Name: ${item.name} >>> Email : ${item.email} >>> Pass : ${item.password}</a><br>											
					  </c:forEach> 
        			</c:when>  
	   	  		 </c:choose>  
				<c:remove var="result"></c:remove> 
<%
			if(goodMsg != null)
			{
%>
					<span style="color:#0000ff"><%=goodMsg%></span><br>
<%	
				request.removeAttribute("goodMsg");
			}
%>
	
<%
			if(errorMsg != null)
			{
%>
					<span style="color:#ff0000"><%=errorMsg%></span><br>
<%	
				request.removeAttribute("errorMsg");
			}
%>

  </body>
</html>



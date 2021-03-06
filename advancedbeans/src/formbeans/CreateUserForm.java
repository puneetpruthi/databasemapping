package formbeans;

public class CreateUserForm {
	String pemail;
	String pname;
	String ppass;
	/**
	 * @return the pemail
	 */
	public String getPemail() {
		return pemail;
	}
	/**
	 * @return the pname
	 */
	public String getPname() {
		return pname;
	}
	/**
	 * @return the ppass
	 */
	public String getPpass() {
		return ppass;
	}
	public String getValidationErrors() {
		//System.out.println(fname + ":" + lname + ":" + uname + ":" + email + ":" + passwd);
		if (pemail == null || pemail.length() == 0) {
			return ("Email is required");
		}

		if (pname == null || pname.length() == 0) {
			return ("Name is required");
		}

		if (ppass == null || ppass.length() == 0) {
			return ("Password is required");
		}
		
		return null;
	}
	public boolean isPresent() {
		if (pemail != null) return true;
		if (pname  != null) return true;
		if (ppass  != null) return true;
		return false;
	}
	/**
	 * @param pemail the pemail to set
	 */
	public void setPemail(String pemail) {
		this.pemail = pemail.trim();
	}
	
	/**
	 * @param pname the pname to set
	 */
	public void setPname(String pname) {
		this.pname = pname.trim();
	}
	
	/**
	 * @param ppass the ppass to set
	 */
	public void setPpass(String ppass) {
		this.ppass = ppass.trim();
	}

}

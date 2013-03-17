package formbeans;

public class DisplayUserForm {
	String gname;
	
	/**
	 * @return the gname
	 */
	public String getGname() {
		return gname;
	}

	public String getValidationErrors() {
		if (gname == null || gname.length() == 0) {
			return ("Name is required");
		}
		return null;
	}

	public boolean isPresent() {
		if (gname != null) return true;
		return false;
	}
	
	/**
	 * @param gname the gname to set
	 */
	public void setGname(String gname) {
		this.gname = gname;
	}

}

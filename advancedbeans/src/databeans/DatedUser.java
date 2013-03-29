package databeans;

import java.util.Date;


public class DatedUser {
	private String email;

	private String name;	
	private String password;
	private java.util.Date   entrydate;

	private int	   userID = -1;

	public DatedUser() {
		super();
	}

	/**
	 * @return the entrydate
	 */
	public java.util.Date getEntrydate() {
		return entrydate;
	}
	/**
	 * @param entrydate the entrydate to set
	 */
	public void setEntrydate(java.util.Date entrydate) {
		this.entrydate = entrydate;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}
	
	//		public UserInfo(int id, String name, String email, String password) {
//		super();
//		this.userID = id;
//		this.name = name;
//		this.email = email;
//		this.password = password;
//	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	
	
	/**
	 * @return the userID
	 */
	public int getUserID() {
		return userID;
	}
	
/**
	 * @param email the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @param userID the userID to set
	 */
	public void setUserID(int userID) {
		this.userID = userID;
	}
	
	@Override
	public String toString(){
		return ("< Username: " + name + " | Email : " + email + " | Password : " + password + ">");
	}
}
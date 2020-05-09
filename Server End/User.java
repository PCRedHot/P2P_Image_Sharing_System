/**
 * @author CHOI Chong Hing (UID: 3035564940)
 * The User Class
 * It contains the username and the hashed password of a user
 * it also counts the number of fail login
 * A user is considered locked if there are three consecutive fail logins 
 */
class User {
	private String username, password;
	private int failCount;
	
	/**
	 * The User constructor
	 * @param username - The username of the User
	 * @param password - The password of the User
	 */
	User(String username, String password) {
		this.username = username;
		this.password = password;
		this.failCount = 0;
	}
	
	/**
	 * The getUsername() method
	 * @return username of the User
	 */
	String getUsername() {
		return username;
	}
	
	/**
	 * The getHashedPassword() method
	 * @return the hashed password of the User
	 */
	String getHashedPassword() {
		return password;
	}
	
	/**
	 * The isLocked() method
	 * @return if the User is locked
	 */
	boolean isLocked() {
		return (failCount >= 3);
	}
	
	/**
	 * The failed() method
	 * Increase the fail login count by 1
	 * It should be called upon an unsuccessful login
	 */
	void failed() {
		failCount++;
	}
	
	/**
	 * The successfulLogin() method
	 * Reset the fail login count to 0
	 * It should be call upon a successful login
	 */
	void successfulLogin() {
		failCount = 0;
	}
}

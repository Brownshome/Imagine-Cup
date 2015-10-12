package security;

import org.mindrot.jbcrypt.BCrypt;

import database.Database;
import database.DatabaseException;

public class Login {
	public static String getPassword(String username) throws DatabaseException {
		return Database.IMPL.getSaltedHash(username);
	}

	public static void registerUser(String username, String password) throws DatabaseException {
		String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
		Database.IMPL.regesterUser(username, hashed);
	}

	public static boolean passwordCorrect(String password, String expected) throws DatabaseException {
		if (password == null || expected == null) //would this ever happen?
			return false;
		
		return BCrypt.checkpw(password, expected);
	}
}

package database;

import java.sql.SQLException;


@SuppressWarnings("serial")
public class DatabaseException extends SQLException {

	public DatabaseException(String message) {
		super(message);
	}
	
}

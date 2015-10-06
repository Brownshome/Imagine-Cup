package database;

import java.sql.SQLException;


@SuppressWarnings("serial")
public class DatabaseException extends Exception {

	public DatabaseException(String message) {
		super(message);
	}

	public DatabaseException(SQLException sqle) {
		super("SQL Error : " + sqle.getSQLState());
	}
}

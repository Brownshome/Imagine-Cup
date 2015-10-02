package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLEmbededDatabase {
	static {
		connect("UserDataBase");
	}
	
	static Connection connection;
	
	static void connect(String databaseName) {
		String URL = "jdbc:derby:" + databaseName + ";create=true";

		try {
			connection = DriverManager.getConnection(URL);
			
		} catch (SQLException sql) {
			System.out.println("Database creation failed : " + sql.getMessage());
			System.exit(0);
		}
	}
	
	static void createTablesIfNotPresent() throws SQLException {
		ResultSet tables = connection.getMetaData().getTables(null, null, "*", null);
		if(!tables.first()) {
			//create the table
		}
	}
}

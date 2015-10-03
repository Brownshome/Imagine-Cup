package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SQLEmbededDatabase implements Database {
	Connection connection;
	
	static final String DATABASE = "UserDatabase";
	
	static String[] tableNames = {
		"UserPrefs",
		"LoginData",
		"FriendRequests",
		"Invites",
		"UserData",
		"ChatHistory",
		"ArenaHistory"
	};
	
	static String[] tableCreateCommand = {
		"CREATE TABLE LoginData "
		+ "("
		+ "Username VARCHAR(255) NOT NULL PRIMARY KEY "
		+ ")",
				
		"CREATE TABLE UserPrefs "
		+ "("
		+ "Username VARCHAR(255) NOT NULL PRIMARY KEY REFERENCES LoginData, "
		+ "NonFriendsJoin BOOLEAN DEFAULT false, "
		+ "FriendsJoin BOOLEAN DEFAULT false, "
		+ "NonFriendsInvite BOOLEAN DEFAULT false, "
		+ "FriendsInvite BOOLEAN DEFAULT false, "
		+ "ShareInfo BOOLEAN DEFAULT false "
		+ ")",
		
		"CREATE TABLE FriendRequests "
		+ "("
		+ "From VARCHAR(255) NOT NULL FOREIGN KEY REFERENCES LoginData, "
		+ "To VARCHAR(255) NOT NULL FOREIGN KEY REFERENCES LoginData, "
		+ "Message VARCHAR(255), "
		+ "Date DATE DEFAULT GETDATE(), "
		+ "Accepted BOOLEAN DEFAULT FALSE "
		+ ")",
		
		"CREATE TABLE Invites "
		+ "("
		+ "Username VARCHAR(255) NOT NULL FOREIGN KEY REFERENCES LoginData, "
		+ "Owner VARCHAR(255) NOT NULL FOREIGN KEY REFERENCES LoginData, "
		+ "Message VARCHAR(255), "
		+ "Date DATE DEFAULT GETDATE(), "
		+ "Accepted BOOLEAN DEFAULT FALSE "
		+ ")",
		
		"CREATE TABLE UserData "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "DateJoined DATE DEFAULT GETDATE(), "
		+ "LastSeen DATE DEFAULT GETDATE(), "
		+ "AvatarData BINARY(15) "
		+ ")",
		
		"CREATE TABLE ChatHistory "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "From VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Text VARCHAR(8192), "
		+ "Date DATE DEFAULT GETDATE() "
		+ ")",
		
		"CREATE TABLE ArenaHistory "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Event VARCHAR(64) CHECK Event IN ('Join', 'Leave'), "
		+ "Date DATA DEFAULT GETDATE() "
		+ ")"
	};
	
	void connect(String databaseName) {
		String URL = "jdbc:derby:" + databaseName + ";create=true";

		try {
			connection = DriverManager.getConnection(URL);
			createTablesIfNotPresent();
		} catch (SQLException sql) {
			System.out.println("Database creation failed : " + sql.getMessage());
			System.exit(0);
		}
	}
	
	void createTablesIfNotPresent() throws SQLException {
		Statement statement = connection.createStatement();
		boolean flag = false;
		
		for(int i = 0; i < tableNames.length; i++) {
			ResultSet tables = connection.getMetaData().getTables(null, null, "UserData", null);
			if(!tables.first()) {
				flag = true;
				statement.addBatch(tableCreateCommand[i]);
			}
		}
		
		if(flag)
			statement.executeBatch();
	}

	public SQLEmbededDatabase() {
		connect(DATABASE);
	}

	void createPreparedStatements() throws SQLException {
		createUser = new PreparedStatement[] {
			connection.prepareStatement("INSERT INTO LoginData (Username) VALUES ?"),
			connection.prepareStatement("INSERT INTO UserPrefs (Username) VALUES ?"),
			connection.prepareStatement("INSERT INTO UserData (Username) VALUES ?")
		};
		
		incommingFriendRequests = connection.prepareStatement("SELECT (From, Message) FROM FriendRequests WHERE To = ? AND NOT Accepted");
		outgoingFriendRequests = connection.prepareStatement("SELECT (To, Message) FROM FriendRequests WHERE From = ? AND NOT Accepted");
		friends = connection.prepareStatement("SELECT (To, From) FROM FriendRequests WHERE (To = ? OR From = ?) AND Accepted");
		isFriend = connection.prepareStatement("SELECT (To, From) FROM FriendRequests WHERE ((To = ? AND From = ?) OR (From = ? AND To = ?)) AND Accepted");
	}
	
	PreparedStatement[] createUser;
	@Override
	public void createUser(String username) throws SQLException {
		for(PreparedStatement ps : createUser) {
			ps.setString(1, username);
			ps.executeUpdate();
		}
	}

	PreparedStatement canCreateArena;
	@Override
	public boolean canCreateArena(String username) throws SQLException {
		return true;
	}

	@Override
	public int getMaxPersonCount(String username) throws SQLException {
		return Integer.MAX_VALUE;
	}

	PreparedStatement incommingFriendRequests;
	@Override
	public String[] incommingFriendRequests(String username) throws SQLException {
		incommingFriendRequests.setString(0, username);
		ResultSet result = incommingFriendRequests.executeQuery();
		
		ArrayList<String> requests = new ArrayList<>();
		while(result.next()) {
			String from = result.getString("From");
			String message = result.getString("Message");
			
			requests.add(from);
			requests.add(message);
		}
		
		return requests.toArray(new String[0]);
	}

	PreparedStatement outgoingFriendRequests;
	@Override
	public String[] outgoingFriendRequests(String username) throws SQLException {
		outgoingFriendRequests.setString(0, username);
		ResultSet result = outgoingFriendRequests.executeQuery();
		
		ArrayList<String> requests = new ArrayList<>();
		while(result.next()) {
			String to = result.getString("To");
			String message = result.getString("Message");
			
			requests.add(to);
			requests.add(message);
		}
		
		return requests.toArray(new String[0]);
	}

	PreparedStatement friends;
	@Override
	public String[] friends(String username) throws SQLException {
		friends.setString(1, username);
		friends.setString(2, username);
		ResultSet result = friends.executeQuery();
		ArrayList<String> list = new ArrayList<>();
		while(result.next()) {
			String from = result.getString("From");
			String to = result.getString("To");
			
			list.add(from.equals(username) ? to : from);
		}
		
		return list.toArray(new String[0]);
	}

	PreparedStatement isFriend;
	@Override
	public boolean isFriend(String username, String query) throws SQLException {
		isFriend.setString(1, username);
		isFriend.setString(2, query);
		isFriend.setString(3, username);
		isFriend.setString(4, query);
		return isFriend.executeQuery().first();
	}

	PreparedStatement addFriendRequest;
	@Override
	public void addFriendRequest(String from, String to, String msg) throws SQLException {
		try {
			addFriendRequest.executeUpdate();
		} catch(SQLException ex) {
			if(ex.getSQLState().equals("23503")) {
				throw new DatabaseException("That user is not valid.");
			} else {
				throw ex;
			}
		}
	}

	@Override
	public void addOutstandingFriendAccept(String accepter, String requester) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void acceptFriendRequest(String accepter, String requester) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void makeFriends(String username, String username2)
			throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeFriendRequest(String requester, String requestee)
			throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getAvatarData(String username) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean closeEmptyArenas(String username) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean acceptInvite(String arenaOwner, String username)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allowNonFriendsToJoin(String owner) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allowFriendsToJoin(String owner) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allowFriendsToInvite(String username)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allowNonFriendsToInvite(String username)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addArenaInvite(String owner, String username, String message)
			throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPreferences(String username, byte[] preferences)
			throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] getPreferences(String username) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}
}

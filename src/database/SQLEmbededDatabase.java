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

	static final String DATABASE = "database";
	static final String ERR_PRIMARY_KEY = "23505";
	static final String ERR_REFERENCE = "23503";
	static final String DATABASE_NOT_FOUND = "XJ004";
	
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
		+ "UserFrom VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "UserTo VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Message VARCHAR(255), "
		+ "DateMade DATE DEFAULT CURRENT_DATE, "
		+ "Accepted BOOLEAN DEFAULT FALSE "
		+ ")",
	
		"CREATE TABLE Invites "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Owner VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Message VARCHAR(255), "
		+ "DateMade DATE DEFAULT CURRENT_DATE, "
		+ "Accepted BOOLEAN DEFAULT FALSE "
		+ ")",
		
		"CREATE TABLE UserData "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "DateJoined DATE DEFAULT CURRENT_DATE, "
		+ "LastSeen DATE DEFAULT CURRENT_DATE, "
		+ "AvatarData VARCHAR(15) FOR BIT DATA "
		+ ")",

		"CREATE TABLE ChatHistory "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "UserFrom VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Text VARCHAR(8192), "
		+ "DateMade DATE DEFAULT CURRENT_DATE "
		+ ")",

		"CREATE TABLE ArenaHistory "
		+ "("
		+ "Username VARCHAR(255) NOT NULL REFERENCES LoginData, "
		+ "Event VARCHAR(64) CHECK (Event IN ('Join', 'Leave')), "
		+ "DateMade DATE DEFAULT CURRENT_DATE "
		+ ")"
	};

	void connect(String databaseName) {
		String URL = "jdbc:derby:" + databaseName;

		try {
			try {
				connection = DriverManager.getConnection(URL);
			} catch(SQLException sql) {
				if(!sql.getSQLState().equals(DATABASE_NOT_FOUND))
					throw sql;
					
				System.out.println("Database not found, creating...");
				connection = DriverManager.getConnection(URL + ";create=true");
				createTables();
			}
			
			createPreparedStatements();
		} catch (SQLException sql) {
			System.out.println("Database connection failed : " + sql.getMessage());
			System.exit(0);
		}
	}

	void createTables() throws SQLException {
		Statement statement = connection.createStatement();

		for(int i = 0; i < tableNames.length; i++)
			statement.addBatch(tableCreateCommand[i]);

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

		incommingFriendRequests = connection.prepareStatement("SELECT UserFrom, Message FROM FriendRequests WHERE UserTo = ? AND NOT Accepted");
		outgoingFriendRequests = connection.prepareStatement("SELECT UserTo, Message FROM FriendRequests WHERE UserFrom = ? AND NOT Accepted");
		friends = connection.prepareStatement("SELECT UserTo, UserFrom FROM FriendRequests WHERE (UserTo = ? OR UserFrom = ?) AND Accepted");
		isFriend = connection.prepareStatement("SELECT UserTo, UserFrom FROM FriendRequests WHERE ((UserTo = ? AND UserFrom = ?) OR (UserFrom = ? AND UserTo = ?)) AND Accepted");
		addFriendRequest = connection.prepareStatement("INSERT INTO FriendRequests (UserFrom, UserTo, Message) VALUES (?, ?, ?)");
		acceptFriendRequest = connection.prepareStatement("UPDATE FriendRequests SET Accepted = TRUE WHERE UserFrom = ? AND UserTo = ?");
		removeFriendRequest = connection.prepareStatement("DELETE FROM FriendRequests WHERE UserTo = ? AND UserFrom = ?");
		getAvatarData = connection.prepareStatement("SELECT AvatarData FROM UserData WHERE Username = ?");
		acceptInvite = connection.prepareStatement("UPDATE Invites SET Accepted = TRUE WHERE Username = ? AND Owner = ?");
		allowNonFriendsToJoin = connection.prepareStatement("SELECT NonFriendsJoin FROM UserPrefs WHERE Username = ?");
		allowFriendsToJoin = connection.prepareStatement("SELECT FriendsJoin FROM UserPrefs WHERE Username = ?");
		allowFriendsToInvite = connection.prepareStatement("SELECT FriendsInvite FROM UserPrefs WHERE Username = ?");
		allowNonFriendsToInvite = connection.prepareStatement("SELECT NonFriendsInvite FROM UserPrefs WHERE Username = ?");
		addArenaInvite = connection.prepareStatement("INSERT INTO Invites (Username, Owner, Message) VALUES (?, ?, ?)");
		setPreferences = connection.prepareStatement("UPDATE UserPrefs SET NonFriendsJoin = ?, FriendsJoin = ?, NonFriendsInvite = ?, FriendsInvite = ?, ShareInfo = ? WHERE Username = ?");
		getPreferences = connection.prepareStatement("SELECT NonFriendsJoin, FriendsJoin, NonFriendsInvite, FriendsInvite, ShareInfo FROM UserPrefs WHERE Username = ?");
	}

	PreparedStatement[] createUser;
	@Override
	public void createUserIfAbsent(String username) throws DatabaseException {
		try {
			for(PreparedStatement ps : createUser) {
				ps.setString(1, username);
				ps.executeUpdate();
			}
		} catch(SQLException ex) {
			if(ex.getSQLState().equals(ERR_PRIMARY_KEY))
				return; //the user already exists

			throw new DatabaseException(ex);
		}
	}

	PreparedStatement canCreateArena;
	@Override
	public boolean canCreateArena(String username) throws DatabaseException {
		return true;
	}

	@Override
	public int getMaxPersonCount(String username) throws DatabaseException {
		return Integer.MAX_VALUE;
	}

	PreparedStatement incommingFriendRequests;
	@Override
	public String[] incommingFriendRequests(String username) throws DatabaseException {
		try {
			incommingFriendRequests.setString(1, username);
			ResultSet result = incommingFriendRequests.executeQuery();

			ArrayList<String> requests = new ArrayList<>();
			while(result.next()) {
				String from = result.getString(1);
				String message = result.getString(2);

				requests.add(from);
				requests.add(message);
			}

			return requests.toArray(new String[0]);
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement outgoingFriendRequests;
	@Override
	public String[] outgoingFriendRequests(String username) throws DatabaseException {
		try {
			outgoingFriendRequests.setString(1, username);
			ResultSet result = outgoingFriendRequests.executeQuery();

			ArrayList<String> requests = new ArrayList<>();
			while(result.next()) {
				String to = result.getString(1);
				String message = result.getString(2);

				requests.add(to);
				requests.add(message);
			}

			return requests.toArray(new String[0]);
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement friends;
	@Override
	public String[] friends(String username) throws DatabaseException {
		try {
			friends.setString(1, username);
			friends.setString(2, username);
			ResultSet result = friends.executeQuery();
			ArrayList<String> list = new ArrayList<>();
			while(result.next()) {
				String from = result.getString(2);
				String to = result.getString(1);

				list.add(from.equals(username) ? to : from);
			}

			return list.toArray(new String[0]);
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement isFriend;
	@Override
	public boolean isFriend(String username, String query) throws DatabaseException {
		try {
			isFriend.setString(1, username);
			isFriend.setString(2, query);
			isFriend.setString(3, username);
			isFriend.setString(4, query);
			return isFriend.executeQuery().first();
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement addFriendRequest;
	@Override
	public void addFriendRequest(String from, String to, String msg) throws DatabaseException {
		try {
			addFriendRequest.setString(1, "from");
			addFriendRequest.setString(2, "to");
			addFriendRequest.setString(3, "msg");
			addFriendRequest.executeUpdate();
		} catch(SQLException ex) {
			if(ex.getSQLState().equals(ERR_REFERENCE))
				throw new DatabaseException("That user is not valid.");

			throw new DatabaseException(ex);
		}
	}

	PreparedStatement acceptFriendRequest;
	@Override
	public void acceptFriendRequest(String accepter, String requester) throws DatabaseException {
		try {
			acceptFriendRequest.setString(1, requester);
			acceptFriendRequest.setString(2, accepter);
			if(acceptFriendRequest.executeUpdate() == 0)
				throw new DatabaseException("Invalid username");
			
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement removeFriendRequest;
	@Override
	public void removeFriendRequest(String requester, String requestee) throws DatabaseException {
		try {
			removeFriendRequest.setString(1, requestee);
			removeFriendRequest.setString(2, requester);
			removeFriendRequest.executeUpdate();
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement getAvatarData;
	@Override
	public byte[] getAvatarData(String username) throws DatabaseException {
		try {
			getAvatarData.setString(1, username);
			ResultSet result = getAvatarData.executeQuery();
			if(result.next())
				return result.getBytes(1);
			
			throw new DatabaseException("That is not a valid username.");
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	@Override
	public boolean closeEmptyArenas(String username) throws DatabaseException {
		return false;
	}

	PreparedStatement acceptInvite;
	@Override
	public boolean acceptInvite(String arenaOwner, String username) throws DatabaseException {
		try {
			acceptInvite.setString(1, username);
			acceptInvite.setString(2, arenaOwner);
			if(acceptInvite.executeUpdate() == 0)
				return false;
			
			return true;
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement allowNonFriendsToJoin;
	@Override
	public boolean allowNonFriendsToJoin(String username) throws DatabaseException {
		try {
			allowNonFriendsToJoin.setString(1, username);
			ResultSet result = allowNonFriendsToJoin.executeQuery();
			if(!result.next())
				throw new DatabaseException(username + "is not a valid username.");
			
			return result.getBoolean(1);
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement allowFriendsToJoin;
	@Override
	public boolean allowFriendsToJoin(String username) throws DatabaseException {
		try {
			allowFriendsToJoin.setString(1, username);
			ResultSet result = allowFriendsToJoin.executeQuery();
			if(!result.next())
				throw new DatabaseException(username + "is not a valid username.");
			
			return result.getBoolean(1);
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement allowFriendsToInvite;
	@Override
	public boolean allowFriendsToInvite(String username) throws DatabaseException {
		try {
			allowFriendsToInvite.setString(1, username);
			ResultSet result = allowFriendsToInvite.executeQuery();
			if(!result.next())
				throw new DatabaseException(username + "is not a valid username.");
			
			return result.getBoolean(1);
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement allowNonFriendsToInvite;
	@Override
	public boolean allowNonFriendsToInvite(String username) throws DatabaseException {
		try {
			allowNonFriendsToInvite.setString(1, username);
			ResultSet result = allowNonFriendsToInvite.executeQuery();
			if(!result.next())
				throw new DatabaseException(username + "is not a valid username.");
			
			return result.getBoolean(1);
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement addArenaInvite;
	@Override
	public void addArenaInvite(String owner, String username, String message) throws DatabaseException {
		try {
			addArenaInvite.setString(1, username);
			addArenaInvite.setString(2, owner);
			addArenaInvite.setString(3, message);
		} catch(SQLException ex) {
			if(ex.getSQLState().equals(ERR_REFERENCE))
				throw new DatabaseException("That username is not valid");
			
			throw new DatabaseException(ex);
		}
	}
	
	PreparedStatement setPreferences;
	/** preferences [nonFriendsJoin, friendsJoin, nonFriendsInvite, friendsInvite, shareInfo]*/
	@Override
	public void setPreferences(String username, int preferences) throws DatabaseException {
		boolean nonFriendsJoin = (preferences & 1) != 0;
		boolean friendsJoin = (preferences & 2) != 0;
		boolean nonFriendsInvite = (preferences & 4) != 0;
		boolean friendsInvite = (preferences & 8) != 0;
		boolean shareInfo = (preferences & 16) != 0;
		
		try {
			setPreferences.setBoolean(1, nonFriendsJoin);
			setPreferences.setBoolean(2, friendsJoin);
			setPreferences.setBoolean(3, nonFriendsInvite);
			setPreferences.setBoolean(4, friendsInvite);
			setPreferences.setBoolean(5, shareInfo);
			
			if(setPreferences.executeUpdate() == 0)
				throw new DatabaseException(username + " is not a user");
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}

	PreparedStatement getPreferences;
	@Override
	public int getPreferences(String username) throws DatabaseException {
		try {
			getPreferences.setString(1, username);
			ResultSet result = getPreferences.executeQuery();
			if(result.next())
				throw new DatabaseException(username + " is not a valid user");
			
			int out = 0;
			int bit = 1;
			for(int i = 0; i < 5; i++) {
				if(result.getBoolean(i + 1))
					out += bit;
				
				bit <<= 1;
			}
			
			return out;
		} catch(SQLException ex) {
			throw new DatabaseException(ex);
		}
	}
}
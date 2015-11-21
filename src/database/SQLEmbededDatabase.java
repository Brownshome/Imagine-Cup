package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import util.Doublet;
import util.Tripplet;

public class SQLEmbededDatabase implements Database {
	Connection connection;

	static final String DATABASE = "sqlbackend";
	static final String ERR_PRIMARY_KEY = "23000";
	static final String ERR_REFERENCE = "23000";
	static final String USERNAME = "root";
	static final String PASSWORD = null; //password goes here;
	
	void connect(String databaseName) {
		String URL = "jdbc:mysql://localhost/" + databaseName;

		try {
			connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
			createPreparedStatements();
		} catch (SQLException sql) {
			System.out.println("Database connection failed : " + sql.getMessage());
			System.exit(0);
		}
	}

	public SQLEmbededDatabase() {
		connect(DATABASE);
	}

	void createPreparedStatements() throws SQLException {
		createUser = new PreparedStatement[] {
				connection.prepareStatement("INSERT INTO LoginData (Username, Hash) VALUES (?, ?)"),
				connection.prepareStatement("INSERT INTO UserPrefs (Username) VALUES (?)"),
				connection.prepareStatement("INSERT INTO UserData (Username) VALUES (?)")
		};
		updateLastSeen = connection.prepareStatement("UPDATE UserData SET LastSeen = CURRENT_TIMESTAMP WHERE Username = ?");
		incommingFriendRequests = connection.prepareStatement("SELECT UserFrom, Message FROM FriendRequests WHERE UserTo = ? AND NOT Accepted");
		outgoingFriendRequests = connection.prepareStatement("SELECT UserTo, Message FROM FriendRequests WHERE UserFrom = ? AND NOT Accepted");
		friends = connection.prepareStatement("SELECT UserTo, UserFrom FROM FriendRequests WHERE (UserTo = ? OR UserFrom = ?) AND Accepted");
		isFriend = connection.prepareStatement("SELECT UserTo, UserFrom FROM FriendRequests WHERE ((UserTo = ? AND UserFrom = ?) OR (UserFrom = ? AND UserTo = ?)) AND Accepted");
		addFriendRequest = connection.prepareStatement("INSERT INTO FriendRequests (UserFrom, UserTo, Message) VALUES (?, ?, ?)");
		acceptFriendRequest = connection.prepareStatement("UPDATE FriendRequests SET Accepted = TRUE, DateAccepted = CURRENT_TIMESTAMP WHERE UserFrom = ? AND UserTo = ?");
		removeFriendRequest = connection.prepareStatement("DELETE FROM FriendRequests WHERE UserTo = ? AND UserFrom = ? AND NOT Accepted");
		getAvatarData = connection.prepareStatement("SELECT AvatarData FROM UserData WHERE Username = ?");
		allowNonFriendsToJoin = connection.prepareStatement("SELECT NonFriendsJoin FROM UserPrefs WHERE Username = ?");
		allowFriendsToJoin = connection.prepareStatement("SELECT FriendsJoin FROM UserPrefs WHERE Username = ?");
		allowFriendsToInvite = connection.prepareStatement("SELECT FriendsInvite FROM UserPrefs WHERE Username = ?");
		allowNonFriendsToInvite = connection.prepareStatement("SELECT NonFriendsInvite FROM UserPrefs WHERE Username = ?");
		setPreferences = connection.prepareStatement("UPDATE UserPrefs SET NonFriendsJoin = ?, FriendsJoin = ?, NonFriendsInvite = ?, FriendsInvite = ?, ShareInfo = ? WHERE Username = ?");
		getPreferences = connection.prepareStatement("SELECT NonFriendsJoin, FriendsJoin, NonFriendsInvite, FriendsInvite, ShareInfo FROM UserPrefs WHERE Username = ?");
		addChatMessage = connection.prepareStatement("INSERT INTO ChatHistory (UserTo, UserFrom, Text) VALUES (?, ?, ?)");
		getSaltedHash = connection.prepareStatement("SELECT Hash FROM LoginData WHERE Username = ?");
		addArenaEvent = connection.prepareStatement("INSERT INTO ArenaHistory (Username, Event, Data) VALUES (?, ?, ?)");
		setAvatarData = connection.prepareStatement("UPDATE UserData SET AvatarData = ? WHERE Username = ?");
		getHistory = connection.prepareStatement(
				"SELECT * FROM ( " +
				"SELECT Event, DateMade, Data FROM ArenaHistory WHERE Username = ? " +
				"UNION " +
				"SELECT 'CHAT_FROM', DateMade, UserFrom || ': ' || Text FROM CHATHISTORY WHERE UserTo = ? " +
				"UNION " +
				"SELECT 'CHAT_TO', DateMade, UserTo || ': ' || Text FROM CHATHISTORY WHERE UserFrom = ? " +
				"UNION " +
				"SELECT 'REQUEST_TO_MADE', DateMade, UserTo || ': ' || Message FROM FRIENDREQUESTS WHERE UserFrom = ? " +
				"UNION " +
				"SELECT 'REQUEST_TO_ACCEPTED', DateAccepted, UserTo || ': ' || Message FROM FRIENDREQUESTS WHERE UserFrom = ? AND Accepted = TRUE " +
				"UNION " +
				"SELECT 'REQUEST_FROM_MADE', DateMade, UserFrom || ': ' || Message FROM FRIENDREQUESTS WHERE UserTo = ? " +
				"UNION " +
				"SELECT 'REQUEST_TO_ACCEPTED', DateAccepted, UserFrom || ': ' || Message FROM FRIENDREQUESTS WHERE UserTo = ? AND Accepted = TRUE " +
				") AS tmp ORDER BY 2 DESC LIMIT ? OFFSET ?"
		);
		setStatus = connection.prepareStatement("UPDATE UserData SET Status = ? WHERE Username = ?");
		getStatus = connection.prepareStatement("SELECT Status FROM USERDATA WHERE Username = ?");
		removeFriend = connection.prepareStatement("DELETE FROM FriendRequests WHERE ((UserTo = ? AND UserFrom = ?) OR (UserTo = ? AND UserFrom = ?)) AND Accepted");
	}

	PreparedStatement[] createUser;
	PreparedStatement updateLastSeen;
	@Override
	public void userLogin(String username) throws DatabaseException {
		try {
			updateLastSeen.setString(1, username);
			if(updateLastSeen.executeUpdate() == 0)
				throw new DatabaseException("That user does not exist.");
		} catch(SQLException ex) {
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
	public List<Doublet<String, String>> incommingFriendRequests(String username) throws DatabaseException {
		try {
			incommingFriendRequests.setString(1, username);
			ResultSet result = incommingFriendRequests.executeQuery();

			List<Doublet<String, String>> requests = new ArrayList<>();
			while(result.next()) {
				String from = result.getString(1);
				String message = result.getString(2);

				requests.add(new Doublet<>(from, message));
			}

			return requests;
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement outgoingFriendRequests;
	@Override
	public List<Doublet<String, String>> outgoingFriendRequests(String username) throws DatabaseException {
		try {
			outgoingFriendRequests.setString(1, username);
			ResultSet result = outgoingFriendRequests.executeQuery();

			List<Doublet<String, String>> requests = new ArrayList<>();
			while(result.next()) {
				String to = result.getString(1);
				String message = result.getString(2);

				requests.add(new Doublet<>(to, message));
			}

			return requests;
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement friends;
	@Override
	public List<String> friends(String username) throws DatabaseException {
		try {
			friends.setString(1, username);
			friends.setString(2, username);
			ResultSet result = friends.executeQuery();
			List<String> list = new ArrayList<>();
			while(result.next()) {
				String from = result.getString(2);
				String to = result.getString(1);

				list.add(from.equals(username) ? to : from);
			}

			return list;
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
			return isFriend.executeQuery().next();
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement addFriendRequest;
	@Override
	public void addFriendRequest(String from, String to, String msg) throws DatabaseException {
		try {
			addFriendRequest.setString(1, from);
			addFriendRequest.setString(2, to);
			addFriendRequest.setString(3, msg);
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
			
			if(removeFriendRequest.executeUpdate() == 0)
				throw new DatabaseException("Such a friend request does not exist.");
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
			setPreferences.setString(6, username);
			
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
			if(!result.next())
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

	PreparedStatement addChatMessage;
	@Override
	public void addChatMessage(String username, String to, String text) throws DatabaseException {
		try {
			addChatMessage.setString(1, to);
			addChatMessage.setString(2, username);
			addChatMessage.setString(3, text);
			addChatMessage.executeUpdate();
		} catch(SQLException ex) {
			if(ex.getSQLState().equals(ERR_REFERENCE))
				throw new DatabaseException("That user is not valid.");
			throw new DatabaseException(ex);
		}
	}

	@Override
	public void regesterUser(String username, String hash) throws DatabaseException {
		try {
			createUser[0].setString(1, username);
			createUser[0].setString(2, hash);
			createUser[0].executeUpdate();
			
			createUser[1].setString(1, username);
			createUser[1].executeUpdate();
			
			createUser[2].setString(1, username);
			createUser[2].executeUpdate();
			
		} catch(SQLException ex) {
			if(ex.getSQLState().equals(ERR_PRIMARY_KEY))
				throw new DatabaseException("That user already exists");

			throw new DatabaseException(ex);
		}
	}

	PreparedStatement getSaltedHash;
	@Override
	public String getSaltedHash(String username) throws DatabaseException {
		try {
			getSaltedHash.setString(1, username);
			ResultSet result = getSaltedHash.executeQuery();
			if(!result.next())
				throw new DatabaseException(username + " does not exist.");
			
			return result.getString(1);
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}
	
	PreparedStatement addArenaEvent;
	@Override
	public void addArenaEvent(String username, HistoryEvent event, String data) throws DatabaseException {
		try {
			addArenaEvent.setString(1, username);
			addArenaEvent.setString(2, event.name());
			addArenaEvent.setString(3, data);
			
			addArenaEvent.executeUpdate();
			
		} catch(SQLException sqle) {
			if(sqle.getSQLState().equals(ERR_REFERENCE))
				throw new DatabaseException(username + " is not a valid user.");
			
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement setAvatarData;
	@Override
	public void setAvatarData(String username, byte[] binary) throws DatabaseException {
		try {
			setAvatarData.setBytes(1, binary);
			setAvatarData.setString(2, username);
			if(setAvatarData.executeUpdate() == 0)
				throw new DatabaseException(username + " is not a valid user.");
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement getHistory;
	@Override
	public List<Tripplet<HistoryEvent, Date, String>> getHistory(String username, int from, int to) throws DatabaseException {
		if(from < 0 || to < 0 || to < from)
			throw new DatabaseException("Invalid range: " + from + " to " + to + ".");
		
		try {
			getHistory.setString(1, username);
			getHistory.setString(2, username);
			getHistory.setString(3, username);
			getHistory.setString(4, username);
			getHistory.setString(5, username);
			getHistory.setString(6, username);
			getHistory.setString(7, username);

			getHistory.setInt(8, to - from);
			getHistory.setInt(9, from);
			
			ResultSet result = getHistory.executeQuery();
			
			List<Tripplet<HistoryEvent, Date, String>> list = new ArrayList<>();
			while(result.next()) {
				HistoryEvent event = HistoryEvent.valueOf(result.getString(1));
				Timestamp date = result.getTimestamp(2);
				String detail = result.getString(3);
				
				list.add(new Tripplet<>(event, date, detail));
			}
			
			return list;
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement setStatus;
	@Override
	public void setStatus(String username, String status) throws DatabaseException {
		try {
			setStatus.setString(1, status);
			setStatus.setString(2, username);
			if(setStatus.executeUpdate() == 0)
				throw new DatabaseException(username + " is not a valid user.");
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement getStatus;
	@Override
	public String getStatus(String username) throws DatabaseException {
		try {
			getStatus.setString(1, username);
			ResultSet result = getStatus.executeQuery();
			if(!result.next())
				throw new DatabaseException(username + " is not a valid user.");
			
			return result.getString(1);
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}

	PreparedStatement removeFriend;
	@Override
	public void removeFriend(String userA, String userB) throws DatabaseException {
		try {
			removeFriend.setString(1, userA);
			removeFriend.setString(2, userB);
			removeFriend.setString(3, userB);
			removeFriend.setString(4, userA);
			if(removeFriend.executeUpdate() == 0)
				throw new DatabaseException(userA + " and " + userB + " are not friends.");
		} catch(SQLException sqle) {
			throw new DatabaseException(sqle);
		}
	}
}
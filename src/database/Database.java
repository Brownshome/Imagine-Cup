package database;

import java.sql.SQLException;

public interface Database {
	public Database IMPL = null;

	public void		createUser(String username) throws SQLException;
	
	public boolean 	canCreateArena(String username) throws SQLException;
	public int 		getMaxPersonCount(String username) throws SQLException;
	
	/** The string array holds [username0, msg0, username1, msg1, ...] */
	public String[] incommingFriendRequests(String username) throws SQLException;
	public String[] outgoingFriendRequests(String username) throws SQLException;
	public String[] friends(String username) throws SQLException;
	public boolean 	isFriend(String username, String query) throws SQLException;
	
	public void 	addFriendRequest(String from, String to, String msg) throws SQLException;
	public void 	addOutstandingFriendAccept(String accepter, String requester) throws SQLException;
	public void 	acceptFriendRequest(String accepter, String requester) throws SQLException;
	public void 	makeFriends(String username, String username2) throws SQLException;
	public void 	removeFriendRequest(String requester, String requestee) throws SQLException;
	public byte[] 	getAvatarData(String username) throws SQLException;
	public boolean 	closeEmptyArenas(String username) throws SQLException;
	
	/** returns true if there was such an invite */
	public boolean 	acceptInvite(String arenaOwner, String username) throws SQLException;
	public boolean allowNonFriendsToJoin(String owner) throws SQLException;
	public boolean allowFriendsToJoin(String owner) throws SQLException;
	public boolean allowFriendsToInvite(String username) throws SQLException;
	public boolean allowNonFriendsToInvite(String username) throws SQLException;
	public void addArenaInvite(String owner, String username, String message) throws SQLException;
	public void setPreferences(String username, byte[] preferences) throws SQLException;
	public byte[] getPreferences(String username) throws SQLException;
}

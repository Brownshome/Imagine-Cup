package database;

import java.util.Date;
import java.util.List;

import util.Doublet;
import util.Tripplet;

public interface Database {
	public Database IMPL = new SQLEmbededDatabase();

	public void	userLogin(String username) throws DatabaseException;
	public void	regesterUser(String username, String hash) throws DatabaseException;
	public String getSaltedHash(String username) throws DatabaseException;
	
	public boolean canCreateArena(String username) throws DatabaseException;
	public int getMaxPersonCount(String username) throws DatabaseException;
	
	/** The string array holds [username0, msg0, username1, msg1, ...] */
	public List<Doublet<String, String>> incommingFriendRequests(String username) throws DatabaseException;
	public List<Doublet<String, String>> outgoingFriendRequests(String username) throws DatabaseException;
	public List<String> friends(String username) throws DatabaseException;
	public boolean isFriend(String username, String query) throws DatabaseException;
	
	public void addFriendRequest(String from, String to, String msg) throws DatabaseException;
	public void acceptFriendRequest(String accepter, String requester) throws DatabaseException;
	public void removeFriendRequest(String requester, String requestee) throws DatabaseException;
	public byte[] getAvatarData(String username) throws DatabaseException;
	public boolean closeEmptyArenas(String username) throws DatabaseException;
	
	public boolean allowNonFriendsToJoin(String owner) throws DatabaseException;
	public boolean allowFriendsToJoin(String owner) throws DatabaseException;
	public boolean allowFriendsToInvite(String username) throws DatabaseException;
	public boolean allowNonFriendsToInvite(String username) throws DatabaseException;
	public void setPreferences(String username, int preferences) throws DatabaseException;
	public int getPreferences(String username) throws DatabaseException;

	public void addChatMessage(String username, String to, String text) throws DatabaseException;
	public void	addArenaEvent(String username, HistoryEvent event, String data) throws DatabaseException;
	public void setAvatarData(String username, byte[] binary) throws DatabaseException;
	public List<Tripplet<HistoryEvent, Date, String>> getHistory(String username, int from, int to) throws DatabaseException;
	public void setStatus(String username, String status) throws DatabaseException;
	public String getStatus(String username) throws DatabaseException;
	public void removeFriend(String userA, String userB) throws DatabaseException;
}

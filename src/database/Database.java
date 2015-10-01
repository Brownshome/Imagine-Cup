
package database;

public interface Database {
	public Database IMPL = null;

	public boolean 	canCreateArena(String username);
	public int 		getMaxPersonCount(String username);
	
	/** The string array holds [username0, msg0, username1, msg1, ...] */
	public String[] incommingRequests(String username);
	public String[] outgoingRequests(String username);
	public String[] friends(String username);
	public boolean 	isFriend(String username, String query);
	
	public void 	addFriendRequest(String from, String to, String msg);
	public void 	addOutstandingFriendAccept(String accepter, String requester);
	public void 	acceptFriendRequest(String accepter, String requester);
	public void 	makeFriends(String username, String username2);
	public void 	removeFriendRequest(String requester, String requestee);
	public byte[] 	getAvatarData(String username);
	public boolean 	closeEmptyArenas(String username);
	
	/** returns true if there was such an invite */
	public boolean 	acceptInvite(String arenaOwner, String username);
	public boolean allowNonFriendsToJoin(String owner);
	public boolean allowFriendsToJoin(String owner);
	public boolean allowFriendsToInvite(String username);
	public boolean allowNonFriendsToInvite(String username);
	public void addArenaInvite(String owner, String username, String message);
	public void setPreferences(String username, byte[] preferences);
	public byte[] getPreferences(String username);
}

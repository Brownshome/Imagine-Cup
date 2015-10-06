package database;

public interface Database {
	public Database IMPL = new SQLEmbededDatabase();

	public void		userLogin(String username) throws DatabaseException;
	
	public boolean 	canCreateArena(String username) throws DatabaseException;
	public int 		getMaxPersonCount(String username) throws DatabaseException;
	
	/** The string array holds [username0, msg0, username1, msg1, ...] */
	public String[] incommingFriendRequests(String username) throws DatabaseException;
	public String[] outgoingFriendRequests(String username) throws DatabaseException;
	public String[] friends(String username) throws DatabaseException;
	public boolean 	isFriend(String username, String query) throws DatabaseException;
	
	public void 	addFriendRequest(String from, String to, String msg) throws DatabaseException;
	public void 	acceptFriendRequest(String accepter, String requester) throws DatabaseException;
	public void 	removeFriendRequest(String requester, String requestee) throws DatabaseException;
	public byte[] 	getAvatarData(String username) throws DatabaseException;
	public boolean 	closeEmptyArenas(String username) throws DatabaseException;
	
	public boolean 	allowNonFriendsToJoin(String owner) throws DatabaseException;
	public boolean 	allowFriendsToJoin(String owner) throws DatabaseException;
	public boolean 	allowFriendsToInvite(String username) throws DatabaseException;
	public boolean 	allowNonFriendsToInvite(String username) throws DatabaseException;
	public void 	setPreferences(String username, int preferences) throws DatabaseException;
	public int 		getPreferences(String username) throws DatabaseException;

	public void 	addChatMessage(String username, String to, String text) throws DatabaseException;
}

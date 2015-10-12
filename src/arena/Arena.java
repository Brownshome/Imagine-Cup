package arena;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import packets.OutboundPackets;
import server.Connection;
import util.RunnableWithDatabaseException;
import database.Database;
import database.DatabaseException;
import database.HistoryEvent;

public class Arena {
	public static final Map<String, Arena> ARENAS = Collections.synchronizedMap(new HashMap<>());
	static final String DISCONNECT_STRING = "User connection has closed.";
	
	public String owner;
	public HashMap<String, String> invites = new HashMap<>();
	
	//NB: includes the owner aswell
	public Map<Connection, RunnableWithDatabaseException> members = Collections.synchronizedMap(new HashMap<>());
	
	public Arena(Connection owner) throws DatabaseException {
		if(!owner.privilageCheck())
			return;
		
		this.owner = owner.username;
		
		RunnableWithDatabaseException hook = () -> removeFromArena(DISCONNECT_STRING, owner);
		members.put(owner, hook);
		owner.addCloseHook(hook);
		
		if(ARENAS.containsKey(owner.username)) {
			OutboundPackets.SERVER_ERROR.send(owner, 3, "You can only have one arena active at a time.");
			return;
		} else {
			ARENAS.put(owner.username, this);
			Database.IMPL.addArenaEvent(owner.username, HistoryEvent.CREATE, "You created the arena.");
		}
		
		owner.arena = this;
	}
	
	public static void addToArena(String owner, Connection member) {
		if(!member.privilageCheck())
			return;

		try {
			Arena arena = ARENAS.get(owner);
			if(arena == null) {
				OutboundPackets.SERVER_ERROR.send(member, 3, "The user " + owner + " does not have an arena active.");
				return;
			}
			
			if(
				!owner.equals(member.username) &&
				!arena.acceptInvite(member.username) && 
				!(Database.IMPL.isFriend(owner, member.username) && Database.IMPL.allowFriendsToJoin(owner)) &&
				!Database.IMPL.allowNonFriendsToJoin(owner)
			) {
				OutboundPackets.SERVER_ERROR.send(member, 4, "You are not invited to this arena.");
				return;
			}

			if(arena.members.containsKey(member)) {
				OutboundPackets.SERVER_ERROR.send(member, 3, "You are already part of this arena.");
				return;
			}

			int max = Database.IMPL.getMaxPersonCount(owner);
			if(max <= arena.members.size()) {
				OutboundPackets.SERVER_ERROR.send(member, 3, "This arena is full.");
				//TODO possible infoming of the owner
				return;
			}

			byte[] avatarData = Database.IMPL.getAvatarData(member.username);

			Database.IMPL.addArenaEvent(member.username, HistoryEvent.JOIN, "You joined the arena.");
			
			for(Connection c : arena.members.keySet()) {
				OutboundPackets.ARENA_OTHER_JOINED.send(c, member.username);
				OutboundPackets.AVATAR_SEND.send(c, member.username, avatarData);
				OutboundPackets.AVATAR_SEND.send(member, c.username, Database.IMPL.getAvatarData(c.username));
				Database.IMPL.addArenaEvent(c.username, HistoryEvent.JOIN, "User " + member.username + " joined.");
			}

			RunnableWithDatabaseException hook = () -> removeFromArena(member.username, member);
			arena.members.put(member, hook);
			member.addCloseHook(hook);

			member.arena = arena;

			//TODO start audio stream

		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(member, 5, de.getMessage());
		}
	}

	public static void closeArena(String reason, Connection owner) throws DatabaseException {
		if(!owner.privilageCheck())
			return;
		
		Arena arena = ARENAS.get(owner.username);
		
		if(arena == null) {
			OutboundPackets.SERVER_ERROR.send(owner, 3, "You don't own an arena.");
			return;
		}
		
		arena.members.remove(owner);
		
		Database.IMPL.addArenaEvent(owner.username, HistoryEvent.CLOSE, "You closed your arena.");
		for(Entry<Connection, RunnableWithDatabaseException> e : arena.members.entrySet()) {
			OutboundPackets.ARENA_CLOSED.send(e.getKey(), reason);
			Database.IMPL.addArenaEvent(e.getKey().username, HistoryEvent.CLOSE, owner.username + " closed the arena.");
			e.getKey().arena = null;			
		}
		
		ARENAS.remove(owner.username);
		
		//TODO close audio stream
	}
	
	public static void removeFromArena(String reason, Connection leaver) throws DatabaseException {
		if(!leaver.privilageCheck())
			return;
		
		Arena arena = leaver.arena;
		if(arena == null) {
			OutboundPackets.SERVER_ERROR.send(leaver, 3, "You are not a member of any areana.");
			return;
		}
		
		if(reason != DISCONNECT_STRING) //to avoid concurrent modification exceptions
			leaver.removeCloseHook(arena.members.get(leaver));
		
		arena.members.remove(leaver);
		
		//TODO address case where owner is not online
		Database.IMPL.addArenaEvent(leaver.username, HistoryEvent.LEAVE, "You left the arena.");
		for(Entry<Connection, RunnableWithDatabaseException> e : arena.members.entrySet()) {
			OutboundPackets.ARENA_OTHER_LEFT.send(e.getKey(), leaver.username, reason);
			Database.IMPL.addArenaEvent(e.getKey().username, HistoryEvent.LEAVE, leaver.username + " left the arena.");
		}
		
		leaver.arena = null;
		
		try {
			if(arena.members.isEmpty() && Database.IMPL.closeEmptyArenas(leaver.username)) {
				ARENAS.remove(arena.owner);
			}
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(leaver, 5, de.getMessage());
		}
	}
	
	public void makeInvite(String other, String message) {
		invites.put(other, message);
	}
	
	public boolean acceptInvite(String username) {
		return invites.remove(username) != null;
	}
}

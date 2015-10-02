package arena;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import packets.OutboundPackets;
import server.Connection;
import database.Database;

public class Arena {
	public static final Map<String, Arena> ARENAS = Collections.synchronizedMap(new HashMap<>());
	static final String DISCONNECT_STRING = "User connection has closed.";
	
	public String owner;
	
	//NB: includes the owner aswell
	public Map<Connection, Runnable> members = Collections.synchronizedMap(new HashMap<>());
	
	public Arena(Connection owner) {
		if(!owner.privilageCheck())
			return;
		
		this.owner = owner.username;
		
		Runnable hook = () -> removeFromArena(DISCONNECT_STRING, owner);
		members.put(owner, hook);
		owner.addCloseHook(hook);
		
		if(ARENAS.containsKey(owner.username)) {
			OutboundPackets.SERVER_ERROR.send(owner, 3, "You can only have one arena active at a time.");
		} else {
			ARENAS.put(owner.username, this);
		}
		
		owner.arena = this;
	}

	public static void addToArena(String owner, Connection member) {
		if(!member.privilageCheck())
			return;
		
		if(!Database.IMPL.acceptInvite(owner, member.username) && 
		!(Database.IMPL.isFriend(owner, member.username) && Database.IMPL.allowFriendsToJoin(owner)) &&
		!Database.IMPL.allowNonFriendsToJoin(owner)) {
			OutboundPackets.SERVER_ERROR.send(member, 4, "You are not invited to this arena.");
		}
		
		Arena arena = ARENAS.get(owner);
		if(arena == null) {
			OutboundPackets.SERVER_ERROR.send(member, 3, "The user " + owner + " does not have an arena active.");
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
		
		for(Connection c : arena.members.keySet()) {
			OutboundPackets.ARENA_OTHER_JOINED.send(c, member.username);
			OutboundPackets.AVATAR_SEND.send(c, avatarData);
			OutboundPackets.AVATAR_SEND.send(member, Database.IMPL.getAvatarData(c.username));
		}
		
		Runnable hook = () -> removeFromArena(member.username, member);
		arena.members.put(member, hook);
		member.addCloseHook(hook);
		
		member.arena = arena;
		
		//TODO start audio stream
	}

	public static void closeArena(String reason, Connection owner) {
		if(!owner.privilageCheck())
			return;
		
		Arena arena = ARENAS.get(owner.username);
		
		if(arena == null) {
			OutboundPackets.SERVER_ERROR.send(owner, 3, "You don't own an arena.");
			return;
		}
		
		arena.members.remove(owner);
		
		for(Entry<Connection, Runnable> e : arena.members.entrySet()) {
			OutboundPackets.ARENA_CLOSED.send(e.getKey(), reason);
			
			e.getKey().arena = null;			
		}
		
		ARENAS.remove(owner.username);
		
		//TODO close audio stream
	}
	
	public static void removeFromArena(String reason, Connection leaver) {
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
		
		for(Entry<Connection, Runnable> e : arena.members.entrySet()) {
			OutboundPackets.ARENA_OTHER_LEFT.send(e.getKey(), leaver.username, reason);
		}
		
		leaver.arena = null;
		
		if(arena.members.isEmpty() && Database.IMPL.closeEmptyArenas(leaver.username)) {
			ARENAS.remove(arena.owner);
			//TODO logging
		}
	}
}

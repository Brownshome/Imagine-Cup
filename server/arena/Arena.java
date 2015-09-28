package arena;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import packets.OutboundPackets;
import server.Connection;

public class Arena {
	public static final Map<String, Arena> ARENAS = new HashMap<>();
	
	Connection owner;
	
	//NB: includes the owner aswell
	HashSet<Connection> members = new HashSet<>();
	
	public Arena(Connection owner) {
		this.owner = owner;
		members.add(owner);
		
		if(!owner.privilageCheck())
			return;
		
		if(ARENAS.containsKey(owner.username)) {
			OutboundPackets.SERVER_ERROR.send(owner, 3, "You can only have one arena active at a time.");
		} else {
			synchronized(ARENAS) {
				ARENAS.put(owner.username, this);
			}
		}
	}
}

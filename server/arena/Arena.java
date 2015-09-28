package arena;

import java.util.HashSet;

import server.Connection;

public class Arena {
	Connection owner;
	
	//NB: includes the owner aswell
	HashSet<Connection> members = new HashSet<>();
	
	public Arena(Connection owner) {
		this.owner = owner;
		members.add(owner);
	}
}

package packets;

import server.Connection;
import database.DatabaseException;

@FunctionalInterface
public interface PacketHandler {
	public void handle(Connection c, Object[] data) throws DatabaseException;
}

package packets;

import server.Connection;

@FunctionalInterface
public interface PacketHandler {
	public void handle(Connection c, Object[] data);
}

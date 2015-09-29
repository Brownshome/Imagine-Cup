package packets;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.FLOAT;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayInputStream;

import arena.Arena;
import server.Connection;

public enum InboundPackets {
	HISTORY_GET(null, INTEGER),
	
	FEATURES_GET(null),
	
	TEXT_SEND(null, STRING, STRING),
	
	FRIEND_REQUEST((c, o) -> c.friendRequest((String) o[0], (String) o[1]), STRING, STRING),
	FRIEND_ACCEPT((c, o) -> c.friendAccept((String) o[0]), STRING),
	FRIEND_REJECT((c, o) -> c.friendReject((String) o[0]), STRING),
	
	ARENA_CREATE((c, o) -> new Arena(c)),
	ARENA_INVITE(null, STRING, STRING),
	ARENA_LEAVE(null, STRING),
	
	PREFERENCES_SET(null, BINARY),
	
	STATUS_UPDATE(null, STRING),
	
	NEWS_FEED_ADD(null, BINARY),
	
	AVATAR_SEND(null, BINARY),
	
	ANNOTATE_TEXT(null, FLOAT, FLOAT, FLOAT, STRING),
	
	UPLOAD_FILE(null, BYTE, BYTE, STRING);
	
	private DataType[] types;
	private PacketHandler handler;
	
	InboundPackets(PacketHandler handler, DataType... types) {
		this.types = types;
		this.handler = handler;
	}
	
	public void handle(Connection connection, ByteArrayInputStream array) {
		Object[] objects = new Object[types.length];
		
		for(int i = 0; i < types.length; i++) {
			objects[i] = types[i].read(array);
		}
		
		handler.handle(connection, objects);
	}
}
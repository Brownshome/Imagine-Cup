package packets;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;
import static packets.DataType.FLOAT;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import server.Connection;

public enum InboundPackets {
	HISTORY_GET(null, INTEGER),
	
	FEATURES_GET(null),
	
	TEXT_SEND(null, STRING, STRING),
	
	FRIEND_REQUEST(null, STRING, STRING),
	FRIEND_ACCEPT(null, STRING),
	FRIEND_REJECT(null, STRING),
	
	ARENA_CREATE(null),
	ARENA_INVITE(null, STRING, STRING),
	ARENA_LEAVE(null, STRING),
	
	PREFERENCES_SET(null, BINARY),
	
	STATUS_UPDATE(null, STRING),
	
	NEWS_FEED_ADD(null, BINARY),
	
	AVATAR_SEND(null, BINARY),
	
	ANNOTATE_TEXT(null, FLOAT, FLOAT, FLOAT, STRING),
	
	UPLOAD_FILE(null, BYTE, BYTE, STRING);
	
	private DataType[] types;
	private Consumer<Object[]> handler;
	
	InboundPackets(Consumer<Object[]> handler, DataType... types) {
		this.types = types;
		this.handler = handler;
	}
	
	public void handle(Connection connection, ByteArrayInputStream array) {
		Object[] objects = new Object[types.length + 1];
		
		objects[0] = connection;
		for(int i = 0; i < types.length; i++) {
			objects[i + 1] = types[i].read(array);
		}
		
		handler.accept(objects);
	}
}
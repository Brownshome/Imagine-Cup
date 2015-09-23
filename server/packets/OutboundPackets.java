package packets;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayOutputStream;

import server.Connection;

public enum OutboundPackets {
	SERVER_ERROR(null, INTEGER, STRING),
	
	AVATAR_SEND(null, STRING, BINARY),
	
	USER_JOINED(null, STRING),
	USER_LEFT(null, STRING, STRING),
	
	ARENA_CLOSED(null, STRING),
	ARENA_INVITE(null, STRING, STRING),
	
	FRIEND_REQUEST(null, STRING, STRING),
	FRIEND_ACCEPT(null, STRING),
	FRIEND_REJECT(null, STRING),
	FRIEND_REMOVE(null, STRING),
	
	NEWS_FEED_SEND(null, INTEGER, BINARY),
	
	FILE_UPLOADED(null, BYTE, STRING),
	FILE_UPLOAD_COMPLETE(null, STRING),
	FILE_START_TRANFER(null, STRING, BYTE, BINARY),
	
	TEXT_SEND(null, BYTE, STRING),
	;
	
	private DataType[] types;
	OutboundPackets(DataType... types) {
		this.types = types;
	}
	
	public void send(Connection connection, Object... objects) {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		
		for(int i = 0; i < types.length; i++) {
			types[i].write(objects[i], array);
		}
		
		
	}
}
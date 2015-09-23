package packets;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayOutputStream;

import server.Connection;

public enum OutboundPackets {
	SERVER_ERROR(INTEGER, STRING),
	
	AVATAR_SEND(STRING, BINARY),
	
	USER_JOINED(STRING),
	USER_LEFT(STRING, STRING),
	
	ARENA_CLOSED(STRING),
	ARENA_INVITE(STRING, STRING),
	
	FRIEND_REQUEST(STRING, STRING),
	FRIEND_ACCEPT(STRING),
	FRIEND_REJECT(STRING),
	FRIEND_REMOVE(STRING),
	
	NEWS_FEED_SEND(INTEGER, BINARY),
	
	FILE_UPLOADED(BYTE, STRING),
	FILE_UPLOAD_COMPLETE(STRING),
	FILE_START_TRANFER(STRING, BYTE, BINARY),
	
	TEXT_SEND(BYTE, STRING),
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
		
		connection.send(array.toByteArray());
	}
}
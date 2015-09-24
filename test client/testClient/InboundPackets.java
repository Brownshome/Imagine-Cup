package testClient;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayInputStream;

import packets.DataType;

public enum InboundPackets {
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
	
	InboundPackets(DataType... types) {
		this.types = types;
	}
	
	public void handle(ByteArrayInputStream array) {
		Object[] objects = new Object[types.length];
		
		for(int i = 0; i < types.length; i++) {
			objects[i] = types[i].read(array);
		}
		
		Client.printPacket(name(), objects);
	}
}
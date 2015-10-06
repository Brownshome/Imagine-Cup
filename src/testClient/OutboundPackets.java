package testClient;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.FLOAT;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayOutputStream;

import packets.DataType;

public enum OutboundPackets {
	HISTORY_GET(INTEGER),

	FEATURES_GET(),

	TEXT_SEND(STRING, STRING),

	FRIEND_REQUEST(STRING, STRING),
	FRIEND_ACCEPT(STRING),
	FRIEND_REJECT(STRING),
	
	ARENA_CLOSE(STRING),
	ARENA_CREATE(),
	ARENA_INVITE(STRING, STRING),
	ARENA_LEAVE(STRING),
	ARENA_JOIN(STRING),
	
	PREFERENCES_SET(INTEGER),
	PREFERENCES_GET(),

	STATUS_UPDATE(STRING),

	NEWS_FEED_ADD(BINARY),

	AVATAR_SEND(BINARY),

	ANNOTATE_TEXT(FLOAT, FLOAT, FLOAT, STRING),
	
	FILE_UPLOAD(BYTE, BYTE, INTEGER, STRING, STRING),
	FILE_TRANSFER(STRING, BINARY),
	
	LOGIN(STRING, STRING);

	DataType[] types;
	OutboundPackets(DataType... types) {
		this.types = types;
	}

	public void send(Connection connection, Object... objects) {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		array.write(ordinal());

		for(int i = 0; i < types.length; i++) {
			types[i].write(objects[i], array);
		}

		connection.send(array.toByteArray());
	}
}

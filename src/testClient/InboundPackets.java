package testClient;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.FLOAT;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayInputStream;

import packets.DataType;

public enum InboundPackets {
	SERVER_ERROR(INTEGER, STRING),

	AVATAR_SEND(STRING, BINARY),

	TEXT_SEND(BYTE, STRING, STRING),

	ANNOTATE_TEXT(STRING, FLOAT, FLOAT, FLOAT, STRING),

	AVATAR_UPDATE_OTHERS(STRING, BINARY),

	ARENA_OTHER_JOINED(STRING),
	ARENA_OTHER_LEFT(STRING, STRING),
	ARENA_CLOSED(STRING),
	ARENA_INVITE(STRING, STRING),

	NEWS_FEED_SEND(BYTE, BINARY),

	FRIEND_SEND(STRING),
	FRIEND_REQUEST(STRING, STRING),
	FRIEND_REMOVE(STRING),

	STATUS_UPDATE(STRING, STRING),

	HISTORY_SEND(BYTE, INTEGER, INTEGER, STRING),

	LOGIN_OK(),
	
	PREFERENCES_SEND(INTEGER);

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

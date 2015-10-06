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

	ARENA_CLOSE((c, o) -> Arena.closeArena((String) o[0], c), STRING),
	ARENA_CREATE((c, o) -> new Arena(c)),
	ARENA_INVITE((c, o) -> c.inviteToArena((String) o[0], (String) o[1]), STRING, STRING),
	ARENA_LEAVE((c, o) -> Arena.removeFromArena((String) o[0], c), STRING),
	ARENA_JOIN((c, o) -> Arena.addToArena((String) o[0], c), STRING),
	
	PREFERENCES_SET((c, o) -> c.setPreferences((int) o[0]), INTEGER),
	PREFERENCES_GET((c, o) -> c.sendPreferences()),

	STATUS_UPDATE(null, STRING),

	NEWS_FEED_ADD(null, BINARY),

	AVATAR_SEND(null, BINARY),

	ANNOTATE_TEXT((c, o) -> c.annotateText((float) o[0], (float) o[1], (float) o[2], (String) o[3]), FLOAT, FLOAT, FLOAT, STRING),
	
	FILE_UPLOAD((c, o) -> c.handleFileUpload((byte) o[0], (byte) o[1], (int) o[2], (String) o[3], (String) o[4]), BYTE, BYTE, INTEGER, STRING, STRING),
	FILE_TRANFER((c, o) -> c.handleFileDataPacket((String) o[0], (byte[]) o[1]), STRING, BINARY),
	
	LOGIN((c, o) -> c.login((String) o[0], (String) o[1]), STRING, STRING);

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

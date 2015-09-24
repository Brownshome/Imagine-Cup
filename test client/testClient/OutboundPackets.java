package testClient;

import java.io.ByteArrayOutputStream;

import packets.DataType;
import static packets.DataType.*;

public enum OutboundPackets {
	TEST_PACKET(INTEGER, STRING, BYTE, BINARY);
	
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
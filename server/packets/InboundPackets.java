package packets;

import static packets.DataType.BINARY;
import static packets.DataType.BYTE;
import static packets.DataType.INTEGER;
import static packets.DataType.STRING;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.function.Consumer;

public enum InboundPackets {
	TEST_PACKET(o -> System.out.println("Test packet recieved " + Arrays.toString(o)), INTEGER, STRING, BYTE, BINARY);
	
	private DataType[] types;
	private Consumer<Object[]> handler;
	
	InboundPackets(Consumer<Object[]> handler, DataType... types) {
		this.types = types;
		this.handler = handler;
	}
	
	public void handle(ByteArrayInputStream array) {
		Object[] objects = new Object[types.length];
		
		for(int i = 0; i < types.length; i++) {
			objects[i] = types[i].read(array);
		}
		
		handler.accept(objects);
	}
}
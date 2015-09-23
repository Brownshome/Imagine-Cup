package packets;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

public enum InboundPackets {
	;
	
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
package packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import server.Server;

public enum DataType {
	STRING(DataType::toString, DataType::writeString), 
	INTEGER(DataType::toInt, DataType::writeInt), 
	BINARY(DataType::toBinary, DataType::writeBinary), 
	BYTE(DataType::toByte, (b, o) -> o.write((byte) b));

	private Function<ByteArrayInputStream, Object> read;
	private BiConsumer<Object, ByteArrayOutputStream> write;

	private DataType(Function<ByteArrayInputStream, Object> read, BiConsumer<Object, ByteArrayOutputStream> write) {
		this.read = read;
		this.write = write;
	}

	Object read(ByteArrayInputStream data) {
		return read.apply(data);
	}

	void write(Object value, ByteArrayOutputStream data) {
		write.accept(value, data);
	}

	private static void writeString(Object value, ByteArrayOutputStream out) {
		try { out.write(((String) value).getBytes(Server.CHARSET)); } catch (IOException e) {}
	}

	private static void writeInt(Object value, ByteArrayOutputStream out) {
		int i = (int) value;
		out.write(i >> 24 & 0xff);
		out.write(i >> 16 & 0xff);
		out.write(i >> 8 & 0xff);
		out.write(i & 0xff);
	}

	private static void writeBinary(Object value, ByteArrayOutputStream out) {
		try { out.write((byte[]) value); } catch (IOException e) {}
	}

	private static byte toByte(ByteArrayInputStream data) {
		if(data.available() < 1)
			throw new RuntimeException("Packet overflow");

		return (byte) data.read();
	}

	private static int toInt(ByteArrayInputStream data) {
		if(data.available() < 4)
			throw new RuntimeException("Packet overflow");

		return data.read() << 24 | data.read() << 16 | data.read() << 8 | data.read();
	}

	private static byte[] toBinary(ByteArrayInputStream data) {
		byte[] b = new byte[data.available()];
		try { data.read(b); } catch(IOException e) {} //luv you too Java checked exceptions
		return b;
	}

	private static String toString(ByteArrayInputStream data) {
		byte[] string = new byte[data.available()];
		int i = 0;

		while(i < string.length) {
			int b = data.read();
			if(b == -1)
				throw new RuntimeException("String not null terminated");

			if(b == 0)
				break;

			string[i] = (byte) b;

			i++;
		}

		return new String(string, 0, i, Server.CHARSET);
	}
}

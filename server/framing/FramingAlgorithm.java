package framing;

import packets.PacketException;

public interface FramingAlgorithm {
	byte[] encode(byte[] data);
	byte[] decode(byte[] data) throws PacketException;
}

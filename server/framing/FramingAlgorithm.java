package framing;

public interface FramingAlgorithm {
	byte[] encode(byte[] data);
	byte[] decode(byte[] data);
}

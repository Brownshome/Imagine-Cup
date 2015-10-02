package packets;

@SuppressWarnings("serial")
public class PacketException extends Exception {
	private static String makeString(int packet, String detail) {
		InboundPackets packetType = null;
		
		if(packet >= 0 && packet < InboundPackets.values().length)
			packetType = InboundPackets.values()[packet];
		
		return "Packet error processing " + (packetType == null ? "UNKNOWN" : packetType.name()) + " : " + detail;
	}
	
	public PacketException(int packet, String detail) {
		super(makeString(packet, detail));
	}
}

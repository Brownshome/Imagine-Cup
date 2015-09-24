package testClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;

public class Client {
	public static void main(String[] arg) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Enter server address: ");
		String address = in.readLine();
		if(!address.contains(":")) {
			System.out.println("Please input as ip:port.\nExiting...");
			System.exit(0);
		}

		String[] ipPort = address.split(":");

		String ip = ipPort[0];
		int port = Integer.decode(ipPort[1]);

		try ( Socket socket = new Socket(ip, port) ) {
			Connection.createNewConnection(socket);

			while(true) {
				System.out.println("Please type a packet id followed by the data to send. RAW is the packet id for raw "
						+ "data. ENCODED is the packet id for directly sending data without encoding. Type EXIT to close.");

				String[] data = in.readLine().split(" ");

				if(data[0].equals("EXIT"))
					System.exit(0);

				if(data[0].equals("RAW")) {
					Connection.connection.send(DatatypeConverter.parseHexBinary(data[1]));
				} else if(data[0].equals("ENCODED")) {
					Connection.connection.sendNonEncoded(DatatypeConverter.parseHexBinary(data[1]));
				} else {
					OutboundPackets packet = null;
					
					try {
						packet = OutboundPackets.valueOf(data[0]);
					} catch(IllegalArgumentException iae) {
						System.out.println("Invalid packet type.");
					}
					
					Object[] objects = new Object[packet.types.length];

					for(int i = 0; i < objects.length; i++) {
						switch(packet.types[i]) {
						case STRING:
							objects[i] = data[i + 1];
							break;
						case INTEGER:
							objects[i] = Integer.parseInt(data[i + 1]);
							break;
						case BYTE:
							objects[i] = Byte.parseByte(data[i + 1]);
							break;
						case BINARY:
							objects[i] = DatatypeConverter.parseHexBinary(data[i + 1]);
						}
					}

					packet.send(Connection.connection, objects);
				}
			}
		}
	}

	public static void printPacket(String name, Object[] objects) {
		System.out.print(name + ": [ ");

		for(int i = 0; i < objects.length; i++) {
			if(i != 0)
				System.out.print(", ");

			System.out.print(objects[i].toString());
		}
		
		System.out.println(" ]");
	}
}

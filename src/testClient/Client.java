package testClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

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

		try ( Socket TCPSocket = new Socket(ip, port); DatagramSocket UDPSocket = new DatagramSocket() ) {
			Connection.createNewConnection(TCPSocket, UDPSocket);

			while(true) {
				System.out.println("Please type a packet id followed by the data to send. RAW is the packet id for raw "
						+ "data. ENCODED is the packet id for directly sending data without encoding. Type EXIT to close.");

				String[] data = in.readLine().split(" ");

				if(data[0].equals("EXIT"))
					System.exit(0);

				if(data[0].equals("UDP")) {
					byte[] c = DatatypeConverter.parseHexBinary(data[1]);
					UDPSocket.send(new DatagramPacket(c, c.length, InetAddress.getByName(ip), port + 1));
				} else if(data[0].equals("RAW")) {
					Connection.connection.send(DatatypeConverter.parseHexBinary(data[1]));
				} else if(data[0].equals("ENCODED")) {
					Connection.connection.sendNonEncoded(DatatypeConverter.parseHexBinary(data[1]));
				} else {
					OutboundPackets packet = null;

					try {
						packet = OutboundPackets.valueOf(data[0]);
					} catch(IllegalArgumentException iae) {
						System.out.println("Invalid packet type, the following are valid types:");
						
						for(OutboundPackets p : OutboundPackets.values())
							System.out.println(p);
						
						continue;
					}

					Object[] objects = new Object[packet.types.length];

					try {
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
								break;
							case FLOAT:
								objects[i] = Float.parseFloat(data[i + 1]);
							}
						}
					} catch(Exception e) {
						System.out.println("The format for " + packet + " is " + Arrays.toString(packet.types));
						continue;
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

			if(objects[i] instanceof byte[])
				System.out.print(DatatypeConverter.printHexBinary((byte[]) objects[i]));
			else
				System.out.print(objects[i].toString());
		}

		System.out.println(" ]");
	}
}

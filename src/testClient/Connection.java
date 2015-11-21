package testClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import packets.PacketException;
import server.Server;
import framing.COBS;
import framing.FramingAlgorithm;

public class Connection {
	static Connection connection;
	static final FramingAlgorithm FRAMER = new COBS();

	public static void createNewConnection(Socket socket, DatagramSocket UDPSocket) {
		connection = new Connection(socket);
		new Thread(connection::listenJob, "TCP Listener").start();

		Thread UDPThread = new Thread(() -> {
			try {
				while(true) {
					byte[] d = new byte[1024];
					DatagramPacket p = new DatagramPacket(d, d.length);
					UDPSocket.receive(p);
					Client.printPacket("UDP", new Object[] {Arrays.copyOf(p.getData(), p.getLength())});
				}
			} catch(IOException e) {
				System.out.println("Could not connect to socket");
				e.printStackTrace();
			}
		}, "UDP Listener");

		UDPThread.setDaemon(true);
		UDPThread.start();
	}

	public Socket socket;

	InputStream in;
	OutputStream out;

	Queue<byte[]> sendQueue = new LinkedList<>();

	Connection(Socket socket) {
		this.socket = socket;

		try {
			in = socket.getInputStream(); 
			out = socket.getOutputStream();
		} catch(IOException ioex) {
			System.out.println("Could not connect to socket");
			ioex.printStackTrace();
		}
	}

	void listenJob() {
		int b;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		while(true) {
			try {
				b = in.read();

				if(b == -1) {
					closeConnection();
					return;
				}

				if(b == 0x00) {
					try {
						decodePacket(buffer.toByteArray());
					} catch(PacketException pe) {
						System.out.println("Error handling packet");
					}
					
					buffer = new ByteArrayOutputStream();
					continue;
				}

				buffer.write(b);
			} catch (IOException e) {
				System.out.println("Socket closed: " + e.getMessage());
				closeConnection();
				return;
			}
		}
	}

	synchronized void closeConnection() {
		try {
			in.close();
			socket.close();
			out.close();
		} catch (IOException e) {}
	}

	void decodePacket(byte[] byteArray) throws PacketException {
		ByteArrayInputStream in = new ByteArrayInputStream(FRAMER.decode(byteArray));

		int id = in.read();

		if(id >= InboundPackets.values().length) {
			System.out.println("Packet recieved UNKNOWN(" + id + ")");
			System.out.println(Arrays.toString(byteArray));
			System.out.println(new String(byteArray, Server.CHARSET));
			return;
		}

		//System.out.println("Packet recieved " + InboundPackets.values()[id].name() + "(" + id + ")");

		InboundPackets.values()[id].handle(in);
	}

	public synchronized void send(byte[] data) {
		try {
			out.write(FRAMER.encode(data));
			out.write(0x00);
		} catch(IOException e) { 
			throw new RuntimeException(e);
		}
	}

	public synchronized void sendNonEncoded(byte[] data) {
		try {
			out.write(data);
		} catch(IOException e) { 
			throw new RuntimeException(e);
		}
	}
}

package testClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import server.Server;
import framing.COBS;
import framing.FramingAlgorithm;

public class Connection {
	static Connection connection;
	static final FramingAlgorithm FRAMER = new COBS();

	public static void createNewConnection(Socket socket) {
		connection = new Connection(socket);
		new Thread(connection::listenJob).start();
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
					decodePacket(buffer.toByteArray());
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

	void decodePacket(byte[] byteArray) {
		ByteArrayInputStream in = new ByteArrayInputStream(FRAMER.decode(byteArray));
		
		int id = in.read();
		
		if(id >= InboundPackets.values().length) {
			System.out.println("Packet recieved UNKNOWN(" + id + ")");
			System.out.println(Arrays.toString(byteArray));
			System.out.println(new String(byteArray, Server.CHARSET));
			return;
		}
		
		System.out.println("Packet recieved " + InboundPackets.values()[id].name() + "(" + id + ")");
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

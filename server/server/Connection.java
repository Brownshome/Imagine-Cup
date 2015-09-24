package server;

import static packets.OutboundPackets.SERVER_ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import packets.InboundPackets;
import framing.COBS;
import framing.FramingAlgorithm;

public class Connection {
	public static final Map<String, Connection> CONNECTIONS = new HashMap<>();
	
	static final FramingAlgorithm FRAMER = new COBS();
	
	public static void createNewConnection(Socket socket) {
		System.out.println("Accepted connection from " + socket.getInetAddress());
		new Thread(new Connection(socket)::listenJob).run(); 
		//TODO thread pool, and actually make this multithreaded, for some reason the socket gets closed if the listenJob is called from another thread.
	}

	public Socket socket;
	public String username;
	
	InputStream in;
	OutputStream out;
	
	Connection(Socket socket) {
		this.socket = socket;
		
		try {
			in = socket.getInputStream(); 
			out = socket.getOutputStream();
		} catch(IOException ioex) {
			Server.error("Failed to create socket", ioex);
		}
		
		username = null;
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
					byte[] bytes = buffer.toByteArray();
					
					decodePacket(bytes);
					
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
			SERVER_ERROR.send(this, 2, "Invalid packet id: " + id);
			System.out.println("Packet recieved UNKNOWN(" + id + ")");
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
			Server.error("Error sending packet", e);
		}
	}
}

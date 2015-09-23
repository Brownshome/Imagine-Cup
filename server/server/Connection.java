package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import packets.InboundPackets;
import framing.COBS;
import framing.FramingAlgorithm;

public class Connection {
	public static final Map<String, Connection> CONNECTIONS = new HashMap<>();
	
	static final FramingAlgorithm FRAMER = new COBS();
	
	public static void createNewConnection(Socket socket) {
		new Thread(new Connection(socket)::job).start();
	}

	public Socket socket;
	public String username;
	
	InputStream in;
	OutputStream out;
	Thread thread;
	
	Queue<byte[]> sendQueue = new LinkedList<>();

	Connection(Socket socket) {
		this.socket = socket;
		thread = Thread.currentThread();
		
		try {
			in = socket.getInputStream(); 
			out = socket.getOutputStream();
		} catch(IOException ioex) {
			Server.error("Failed to read from socket", ioex);
		}
		
		username = null;
	}
	
	void job() {
		int b;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		while(true) {
			sendQueued();
			
			try {
				buffer.write(b = in.read());
				
				if(b == 0x00) {
					decodePacket(buffer.toByteArray());
					buffer = new ByteArrayOutputStream();
				}
			} catch (IOException e) {
				Server.error("Failed to read from socket", e);
			}
		}
	}
	
	void decodePacket(byte[] byteArray) {
		byteArray = FRAMER.decode(byteArray);
		
		InboundPackets.values()[byteArray[0]].handle(new ByteArrayInputStream(byteArray, 1, Integer.MAX_VALUE));
	}

	public synchronized void addToSendQueue(byte[] packet) {
		sendQueue.add(packet);
	}
	
	synchronized void sendQueued() {
		sendQueue.forEach(this::send);
	}
	
	/** This is COBS encoding, the data is read into segments with 0x00 as delimiters, these 0x00s are removed and the length of each sengment + 1
	 * is prepended onto the front. 0xFF has a special meaning, it represents a block of length of 254 */
	void send(byte[] data) {
		if(thread != Thread.currentThread()) {
			addToSendQueue(data);
			return;
		}
		
		try {
			out.write(FRAMER.encode(data));
			out.write(0x00);
		} catch(IOException e) { 
			throw new RuntimeException(e);
		}
	}
}

package server;

import static packets.OutboundPackets.SERVER_ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import packets.InboundPackets;
import packets.PacketException;
import framing.COBS;
import framing.FramingAlgorithm;

public class Connection {
	/** This contains a username - connection map, users are added to this once they successfully log in */
	public static final Map<String, Connection> CONNECTIONS = new HashMap<>();
	
	/** The packet sepperation algorithm that is used */
	static final FramingAlgorithm FRAMER = new COBS();
	
	/** The thread pool that connections are formed with */
	static final ExecutorService POOL = Executors.newCachedThreadPool(); //I know this is vunlnerable to DoS but I'll worry about that later
	
	public static void createNewConnection(Socket socket) {
		System.out.println("Accepted connection from " + socket.getInetAddress());
		POOL.submit(new Connection(socket)::listenJob);
	}

	public Socket socket;
	public String username;
	
	InputStream in;
	OutputStream out;
	ArrayList<Runnable> closeList = new ArrayList<>();
	
	Connection(Socket socket) {
		this.socket = socket;
		
		try {
			in = socket.getInputStream(); 
			out = socket.getOutputStream();
		} catch(IOException ioex) {
			closeConnection();
			System.out.println("Failed to create IO stream : " + ioex.getMessage());
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
					
					try {
						decodePacket(bytes);
					} catch(PacketException pde) {
						System.out.println(pde.getMessage());
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
	
	synchronized void addCloseHook(Runnable r) {
		closeList.add(r);
	}
	
	synchronized void closeConnection() {
		try {
			in.close();
			socket.close();
			out.close();
		} catch (IOException e) {}
		
		closeList.forEach(Runnable::run);
	}
	
	void decodePacket(byte[] byteArray) throws PacketException {
		ByteArrayInputStream in = new ByteArrayInputStream(FRAMER.decode(byteArray));
		
		int id = in.read();
		if(id >= InboundPackets.values().length || id < 0) {
			SERVER_ERROR.send(this, 2, "Invalid packet id: " + id);
			throw new PacketException(id, "Invalid packet ID");
		}
		
		System.out.println("Packet recieved " + InboundPackets.values()[id].name() + "(" + id + ")");
		InboundPackets.values()[id].handle(this, in);
	}

	public synchronized void send(byte[] data) {
		try {
			out.write(FRAMER.encode(data));
			out.write(0x00);
		} catch(IOException e) { 
			System.out.println("Error sending packet : " + e.getMessage());
		}
	}
}

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

import database.Database;
import packets.InboundPackets;
import packets.OutboundPackets;
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

		if(username != null)
			CONNECTIONS.remove(username);
		
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

	public boolean privilageCheck() {
		if(username != null)
			return true;
		
		OutboundPackets.SERVER_ERROR.send(this, 4, "You must be logged in to do this.");
		return false;
	}
	
	public void friendRequest(String username, String msg) {
		Connection other = CONNECTIONS.get(username);

		if(!privilageCheck())
			return;

		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "You cannot send a friend request to yourself, weirdo.");
			return;
		}

		if(other != null)
			OutboundPackets.FRIEND_REQUEST.send(other, this.username, msg);

		Database.IMPL.addFriendRequest(this.username, username, msg);
	}

	public void friendAccept(String username) {
		Connection other = CONNECTIONS.get(username);

		if(!privilageCheck())
			return;
		
		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "There is no friend request from you to yourself, you wish.");
			return;
		}

		if(other != null) {
			OutboundPackets.FRIEND_ACCEPT.send(other, this.username);
		} else {
			Database.IMPL.addOutstandingFriendAccept(this.username, username);
		}
		
		Database.IMPL.makeFriends(this.username, username);
		Database.IMPL.removeFriendRequest(username, this.username);
	}

	public void friendReject(String name) {
		Connection other = CONNECTIONS.get(name);
		
		if(!privilageCheck())
			return;
		
		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "Why would you want to reject a friend request from yourself.");
			return;
		}
		
		if(other != null) {
			OutboundPackets.FRIEND_REJECT.send(this, username);
		}
		
		Database.IMPL.removeFriendRequest(name, username);
	}
}

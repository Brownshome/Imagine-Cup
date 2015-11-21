
package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import packets.DataType;
import packets.InboundPackets;
import packets.OutboundPackets;
import packets.PacketException;
import security.Login;
import util.Doublet;
import util.RunnableWithDatabaseException;
import util.Tripplet;
import arena.Arena;
import database.Database;
import database.DatabaseException;
import database.HistoryEvent;
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

	public Socket TCPSocket;
	public int UDPPort;
	
	public String username;

	public Arena arena;
	
	InputStream in;
	OutputStream out;
	
	Set<RunnableWithDatabaseException> closeList = new HashSet<>();
	
	Connection(Socket socket) {
		this.TCPSocket = socket;

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
					} catch (PacketException pde) {
						System.out.println(pde.getMessage());
					} catch (DatabaseException de) {
						OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
					} catch (Exception e) {
						e.printStackTrace();
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

	public synchronized void addCloseHook(RunnableWithDatabaseException r) {
		closeList.add(r);
	}

	public synchronized void removeCloseHook(RunnableWithDatabaseException r) {
		closeList.remove(r);
	}
	
	synchronized void closeConnection() {
		try {
			in.close();
			TCPSocket.close();
			out.close();
		} catch (IOException e) {}

		if(username != null)
			CONNECTIONS.remove(username);

		for(RunnableWithDatabaseException r : closeList) {
			try {
				r.run();
			} catch(DatabaseException de) {
				OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
			}
		}
	}

	void decodePacket(byte[] byteArray) throws PacketException, DatabaseException {
		ByteArrayInputStream in = new ByteArrayInputStream(FRAMER.decode(byteArray));

		int id = in.read();
		if(id >= InboundPackets.values().length || id < 0) {
			OutboundPackets.SERVER_ERROR.send(this, 2, "Invalid packet id: " + id);
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

	void onLoginSuccess() throws DatabaseException {
		CONNECTIONS.put(username, this);
		Database.IMPL.userLogin(username);
		
		//friends
		for(String name : Database.IMPL.friends(username)) {
			OutboundPackets.FRIEND_SEND.send(this, name);
			
			OutboundPackets.STATUS_UPDATE.send(this, name);
			OutboundPackets.STATUS_UPDATE.send(this, name);
		}
		
		//preferences
		sendPreferences();
		OutboundPackets.AVATAR_SEND.send(this, username, Database.IMPL.getAvatarData(username));
		
		//friendRequests
		List<Doublet<String, String>> requests = Database.IMPL.incommingFriendRequests(username);
		for(Doublet<String, String> request : requests)
			OutboundPackets.FRIEND_REQUEST.send(this, request.a, request.b);
		
		OutboundPackets.STATUS_UPDATE.send(this, username, Database.IMPL.getStatus(username));
	}
	
	public void login(String username, String password) throws DatabaseException {
		if (Login.passwordCorrect(password, Login.getPassword(username))) {
			this.username = username;
			onLoginSuccess();
			OutboundPackets.LOGIN_OK.send(this);
		} else {
			OutboundPackets.SERVER_ERROR.send(this, 5, "Invalid username or password");
		}
	}

	public void register(String username, String password) throws DatabaseException {
		Login.registerUser(username, password);
	}

	public boolean privilageCheck() {
		if(username != null)
			return true;

		OutboundPackets.SERVER_ERROR.send(this, 4, "You must be logged in to do this.");
		return false;
	}

	public void friendRequest(String username, String msg) throws DatabaseException {
		Connection other = CONNECTIONS.get(username);

		if(!privilageCheck())
			return;

		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "You cannot send a friend request to yourself, weirdo.");
			return;
		}

		Database.IMPL.addFriendRequest(this.username, username, msg);
		
		if(other != null)
			OutboundPackets.FRIEND_REQUEST.send(other, this.username, msg);
	}

	public void friendAccept(String username) throws DatabaseException {
		Connection other = CONNECTIONS.get(username);

		if(!privilageCheck())
			return;

		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "There is no friend request from you to yourself, you wish.");
			return;
		}

		Database.IMPL.acceptFriendRequest(this.username, username);
		
		if(other != null)
			OutboundPackets.FRIEND_SEND.send(other, this.username);
		
		OutboundPackets.FRIEND_SEND.send(this, username);
		
		OutboundPackets.STATUS_UPDATE.send(this, username, Database.IMPL.getStatus(username));
		OutboundPackets.STATUS_UPDATE.send(this, this.username, Database.IMPL.getStatus(this.username));
	}

	public void removeFriend(String otherGuy) throws DatabaseException {
		if(!privilageCheck())
			return;
		
		Database.IMPL.removeFriend(username, otherGuy);
		
		Connection c = CONNECTIONS.get(otherGuy);
		if(c != null)
			OutboundPackets.FRIEND_REMOVE.send(c, username);
	}
	
	public void friendReject(String name) throws DatabaseException {
		Connection other = CONNECTIONS.get(name);

		if(!privilageCheck())
			return;

		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "Why would you want to reject a friend request from yourself.");
			return;
		}

		Database.IMPL.removeFriendRequest(name, username);
	}

	public void inviteToArena(String other, String message) throws DatabaseException {
		if(!privilageCheck())
			return;
		
		if(!Arena.ARENAS.containsKey(username) && arena == null) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "You are do not own an arena or are part of one.");
			return;
		}
		
		Arena a = Arena.ARENAS.get(username);
		if(a == null)
			a = arena;
		
		if(!a.owner.equals(username) && 
		!Database.IMPL.allowNonFriendsToInvite(username) && 
		!(Database.IMPL.allowFriendsToInvite(username) && Database.IMPL.isFriend(a.owner, username))) {
			OutboundPackets.SERVER_ERROR.send(this, 4, "You are not allowed to invite people to this arena.");
			return;
		}
		
		a.makeInvite(other, message);
		
		Connection connection = CONNECTIONS.get(other);
		
		if(connection != null)
			OutboundPackets.ARENA_INVITE.send(connection, a.owner, message);
	}

	public void setPreferences(int preferences) throws DatabaseException {
		if(!privilageCheck())
			return;
		
		Database.IMPL.setPreferences(username, preferences);
	}
	
	public void sendPreferences() throws DatabaseException {
		if(!privilageCheck())
			return;
		
		OutboundPackets.PREFERENCES_SEND.send(this, Database.IMPL.getPreferences(username));
	}

	public void annotateText(float x, float y, float z, String string) {
		if(arena == null) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "You are not a member of an arena.");
			return;
		}
		
		for(Connection c : arena.members.keySet())
			OutboundPackets.ANNOTATE_TEXT.send(c, username, x, y, z, string);
	}

	public void sendText(byte type, String text, String to) throws DatabaseException {
		if(!privilageCheck())
			return;
		
		Database.IMPL.addChatMessage(username, to, text);
		
		Connection c = CONNECTIONS.get(to);
		
		if(c != null)
			OutboundPackets.TEXT_SEND.send(c, type, text, username);
	}

	public void setAvatar(byte[] data) throws DatabaseException {
		if(!privilageCheck())
			return;
		
		Database.IMPL.setAvatarData(username, data);
	}

	public void sendHistory(int from, int to) throws DatabaseException {
		if(!privilageCheck())
			return;

		for(Tripplet<HistoryEvent, Date, String> t : Database.IMPL.getHistory(username, from, to)) {
			long time = t.b.getTime();
			int high = (int) (time >> 32);
			int low = (int) time;
			OutboundPackets.HISTORY_SEND.send(this, (byte) t.a.ordinal(), high, low, t.c);
		}
	}

	public void updateStatus(String status) throws DatabaseException {
		if(!privilageCheck())
			return;
		
		Database.IMPL.setStatus(username, status);
		
		for(String friend : Database.IMPL.friends(username)) {
			Connection c = CONNECTIONS.get(friend);
			if(c != null)
				OutboundPackets.STATUS_UPDATE.send(c, username, status);
		}
	}

	public static void passDatagram(DatagramPacket packet, DatagramSocket UDPSocket) {
		ByteArrayInputStream data = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
		
		String username = null;
		try {
			username = (String) DataType.STRING.read(data); //TODO lower data use, options are using IP or string hash
		} catch(RuntimeException re) {
			System.out.println("Malformed string in UDP packet.");
			return;
		}
		
		Connection connection = CONNECTIONS.get(username);
		if(connection  == null) {
			System.out.println("The username " + username + " sent a UDP packet and is not currently connected.");
			return;
		}

		if(!connection.TCPSocket.getInetAddress().equals(packet.getAddress())) {
			System.out.println("Mismatching IP addresses " + connection.TCPSocket.getInetAddress() + " vs " + packet.getAddress());
		}
		
		connection.UDPPort = packet.getPort();
		System.out.println("UDP received from " + username);
		
		if(connection.arena == null)  {
			OutboundPackets.SERVER_ERROR.send(connection, 3, "You are not part of an arena.");
			return;
		}

		for(Connection c : connection.arena.members.keySet()) {
			if(c == connection || c.UDPPort == -1)
				continue;

			InetAddress IP = c.TCPSocket.getInetAddress();

			try {
				UDPSocket.send(new DatagramPacket(packet.getData(), packet.getOffset(), packet.getLength(), IP, c.UDPPort));
			} catch (IOException e) {
				System.out.println("Failed to send packet.");
			}
		}
	}
}

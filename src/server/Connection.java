package server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import packets.InboundPackets;
import packets.OutboundPackets;
import packets.PacketException;
import arena.Arena;
import database.Database;
import database.DatabaseException;
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

	public Arena arena;
	
	InputStream in;
	OutputStream out;
	
	Set<Runnable> closeList = new HashSet<>();
	
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
					} catch (PacketException pde) {
						System.out.println(pde.getMessage());
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

	public synchronized void addCloseHook(Runnable r) {
		closeList.add(r);
	}

	public synchronized void removeCloseHook(Runnable r) {
		closeList.remove(r);
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

	public void login(String username, String password) {
		// TODO Process login information
		this.username = username;
		CONNECTIONS.put(username, this);
		
		try {
		Database.IMPL.createUserIfAbsent(username);
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
		}
		
		OutboundPackets.OK.send(this);
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

		try {
			Database.IMPL.addFriendRequest(this.username, username, msg);
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
			return;
		}
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
			OutboundPackets.FRIEND_SEND.send(other, this.username);
			OutboundPackets.FRIEND_SEND.send(this, username);
		}

		try {
			Database.IMPL.acceptFriendRequest(this.username, username);
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
			return;
		}
	}

	public void friendReject(String name) {
		Connection other = CONNECTIONS.get(name);

		if(!privilageCheck())
			return;

		if(other == this) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "Why would you want to reject a friend request from yourself.");
			return;
		}

		try {
			Database.IMPL.removeFriendRequest(name, username);
		} catch (DatabaseException e) {
			OutboundPackets.SERVER_ERROR.send(this, 5, e.getMessage());
			return;
		}
	}

	public void handleFileUpload(byte fileType, byte connectionType, int size, String name, String URL) {
		switch(connectionType) {
			case 0: //stream from client
				inConnectionTransfer(fileType, size, name, URL);
				break;
			case 1: //stream from client in sepperate connection
				
				break;
			case 2: //URL stream
				break;
		}
	}

	public void inConnectionTransfer(byte fileType, int size, String name, String URL) {
		
	}

	public void handleFileDataPacket(String string, byte[] bs) {
		
	}

	public void inviteToArena(String other, String message) {
		if(!privilageCheck())
			return;
		
		if(!Arena.ARENAS.containsKey(username) && arena == null) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "You are do not own an arena or are part of one.");
			return;
		}
		
		Arena a = Arena.ARENAS.get(username);
		if(a == null)
			a = arena;
		
		try {
			if(!a.owner.equals(username) && 
			!Database.IMPL.allowNonFriendsToInvite(username) && 
			!(Database.IMPL.allowFriendsToInvite(username) && Database.IMPL.isFriend(a.owner, username))) {
				OutboundPackets.SERVER_ERROR.send(this, 4, "You are not allowed to invite people to this arena.");
				return;
			}
		
			Database.IMPL.addArenaInvite(a.owner, other, message);
		
			Connection connection = CONNECTIONS.get(other);
		
			if(connection != null) {
				OutboundPackets.ARENA_INVITE.send(connection, a.owner, message);
			}
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
			return;
		}
	}

	public void setPreferences(int preferences) {
		if(!privilageCheck())
			return;
		
		try {
			Database.IMPL.setPreferences(username, preferences);
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
			return;
		}
	}
	
	public void sendPreferences() {
		if(!privilageCheck())
			return;
		
		try {
			OutboundPackets.PREFERENCES_SEND.send(this, Database.IMPL.getPreferences(username));
		} catch(DatabaseException de) {
			OutboundPackets.SERVER_ERROR.send(this, 5, de.getMessage());
			return;
		}
	}

	public void annotateText(float x, float y, float z, String string) {
		if(!privilageCheck())
			return;
		
		if(arena == null) {
			OutboundPackets.SERVER_ERROR.send(this, 3, "You are not a member of an arena.");
			return;
		}
		
		for(Connection c : arena.members.keySet())
			OutboundPackets.ANNOTATE_TEXT.send(c, username, x, y, z, string);
	}
}

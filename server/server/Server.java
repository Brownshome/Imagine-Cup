package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Server {
	public static final int LISTEN_PORT = 50567;
	public static final int MAX_PACKET_SIZE = 131072;
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	
	public static void main(String[] args) {
		try (ServerSocket listenSocket = new ServerSocket(LISTEN_PORT)) {
			while(true) {
				Connection.createNewConnection(listenSocket.accept());
			}
		} catch (IOException e) {
			System.out.println("Could not create listen socket at port " + LISTEN_PORT + " : " + e.getMessage());
		}
	}
}

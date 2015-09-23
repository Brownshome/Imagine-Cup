package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import framing.COBS;

public class Server {
	public static final int LISTEN_PORT = 50567;
	public static final int MAX_PACKET_SIZE = 131072;
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	
	public static void main(String[] args) {
		COBS alg = new COBS();
		
		alg.decode(alg.encode(new byte[] {0x0A, (byte) 0xFF, 0x14, 0x00, 0x45, 0x00, 0x00, 0x2A}));
		
		try (ServerSocket listenSocket = new ServerSocket(LISTEN_PORT)) {
			while(true) {
				try (Socket socket = listenSocket.accept()) {
					Connection.createNewConnection(socket);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not create listen socket at port" + LISTEN_PORT, e);
		}
	}

	public static void error(String string, IOException e) {
		System.out.println("Error: " + string + "\n");
		e.printStackTrace();
	}
}

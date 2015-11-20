package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Server {
	public static final int LISTEN_PORT_TCP = 50567;
	public static final int LISTEN_PORT_UDP = 50568;
	
	public static final int MAX_PACKET_SIZE = 131072;
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	
	public static void main(String[] args) {
		byte[] buffer = new byte[MAX_PACKET_SIZE];
		
		Thread UDPListener = new Thread(() -> {
			try (DatagramSocket UDPSocket = new DatagramSocket(LISTEN_PORT_UDP)) {
				while(true) {
					DatagramPacket incomming = new DatagramPacket(buffer, MAX_PACKET_SIZE);
					UDPSocket.receive(incomming);
					Connection.passDatagram(incomming, UDPSocket);
				}
			} catch (IOException e) {
				System.out.println("Could not create listen socket at port " + LISTEN_PORT_UDP + " : " + e.getMessage());
			}
		}, "UDPListener");
		
		UDPListener.setDaemon(true);
		UDPListener.start();
		
		try (ServerSocket listenSocket = new ServerSocket(LISTEN_PORT_TCP)) {
			while(true) {
				Connection.createNewConnection(listenSocket.accept());
			}
		} catch (IOException e) {
			System.out.println("Could not create listen socket at port " + LISTEN_PORT_TCP + " : " + e.getMessage());
		}
	}
}

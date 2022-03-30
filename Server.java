/**
 Group 8
 // Yang Xu 500890631
 // Ruoling Yu 500976267
 // Xinyu Ma 500943173
 // Vince De Vera 500550779
 // Raynor Elgie 500964140
 **/

import java.io.*;
import java.net.*;
import java.util.*;


public class Server
{
    private DatagramSocket socket;
    private List<String> listQuotes = new ArrayList<String>();
    private Random random;
 
    public Server(int port) throws SocketException
	{
        socket = new DatagramSocket(port);
        random = new Random();
    }
 
    public static void main(String[] args)
	{
		int port;

		if (args.length < 1)
		{
            System.out.println("Usage: <port>\nInput: ");
			Scanner scanner = new Scanner(System.in);
			port = Integer.parseInt(scanner.next());
        }
		else { port = Integer.parseInt(args[0]);}

		try
		{
            Server server = new Server(port);
			String[] detail = server.waitForSyn().replace("/","").split(":");
            InetAddress client_ip = InetAddress.getByName(detail[0]);
			int client_port = Integer.parseInt(detail[1]);
			server.startConn(client_ip, client_port);
        }
		catch(IOException ex)	{ System.out.println("I/O error: " + ex.getMessage()); }
		catch (InterruptedException ex) { ex.printStackTrace(); }
    }
	
	private String waitForSyn() throws IOException
	{
		DatagramSocket socket = this.socket;
		byte[] buffer = new byte[16];
		while(true)
		{
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.receive(response);
			String res = new String(buffer, "IBM01140");
			System.out.println("Recieved: " + res);
			if(res.charAt(0) == 'S')
			{
				System.out.println("(SYN MSG)");
				return response.getAddress().toString()+":"+response.getPort();
			}
		}
	}
	
	private void startConn(InetAddress ip, int port) throws IOException, InterruptedException
	{
		DatagramSocket socket = this.socket;
		System.out.println(ip);
		System.out.println(port);
		socket.connect(ip, port);
		String packetString = "Z000000000000000"; // Z for synack
		byte[] buffer = new byte[16];
		buffer = packetString.getBytes("IBM01140");
		DatagramPacket request = new DatagramPacket(buffer, buffer.length);
		System.out.println("Sending: " + new String(buffer, "IBM01140"));
		Thread.sleep(1000);
        socket.send(request);
		System.out.println("Sent SYNACK");
		while(true)
		{
			socket.connect(ip, port);
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.receive(response);
			String res = new String(buffer, "IBM01140");
			System.out.println("Recieved packet: " + res);
			if(res.charAt(0) == 'R')	{ return ; }	// Request recieved
		}
	}
	
	private void sendFIN(InetAddress ip) throws IOException	//CHECK IF WE SHOULD ADD SEQ #
	{
		DatagramSocket socket = new DatagramSocket(); //Will probably need to set port somewhere here
		String packetString = "F"; //F for FIN
		byte[] buffer = new byte[16];
		System.arraycopy(packetString.getBytes(), 0, buffer, 16 - packetString.length(), packetString.length());
		DatagramPacket request = new DatagramPacket(buffer, 1, ip, 100);
        socket.send(request);
		while(true)
		{
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.receive(response);
			String res = new String(buffer, 0, response.getLength());
			if(res.charAt(0) == 'A')	{ return; }	//Ack recieved
		}
	}
}
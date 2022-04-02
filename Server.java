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
			server.sendData(client_ip, client_port, "This is the data. Here is a second sentence that we'll send. Here is a third...");
			server.sendFIN(client_ip, client_port);
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
			try{
				socket.receive(response);
				
				String res = new String(buffer, "IBM01140");
				System.out.println("Recieved: " + res);
				if(res.charAt(0) == 'S'){
					System.out.println("(SYN MSG)");
					return response.getAddress().toString()+":"+response.getPort();
				}
			}catch(SocketException e){
				System.out.println("Timeout occured...");
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
			try{
				socket.receive(response);
				String res = new String(buffer, "IBM01140");
				System.out.println("Recieved packet: " + res);
				if(res.charAt(0) == 'R')	{ return ; }	// Request recieved
			}catch(SocketTimeoutException e){
				System.out.println("Timeout occured...");
				continue;
			}
		}
	}
	
	private void sendData(InetAddress ip, int port, String data) throws IOException{
		DatagramSocket socket = this.socket;
		socket.connect(ip, port);
		
		int ceil = ((int) Math.ceil((double)data.length() / 14.0)) * 14;
		data = String.format("%-" + ceil + "s", data);
		
		byte[] allBytes = data.getBytes("IBM01140");
		ArrayList<byte[]> chunks = new ArrayList<byte[]>();
		
		int i = 0;
		while((i+1) * 14 <= allBytes.length){
			System.out.println((i * 14) + " to " + ((i + 1) * 14));
			byte[] chunk = Arrays.copyOfRange(allBytes, (i * 14), ((i + 1) * 14));
			chunks.add(chunk);
			//System.out.println(new String(chunk, "IBM01140"));
			i += 1;
		}
		
		byte[] buffer = new byte[16];
		byte[] recvBuffer = new byte[16];
		for(i = 0; i < chunks.size(); i++){
			while(true){ //TODO: HANDLE TIMEOUT
				buffer = ("D" + (i % 2) + "00000000000000").getBytes("IBM01140");
				System.arraycopy(chunks.get(i), 0, buffer, 2, 14);

				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				System.out.println("Sending: " + new String(buffer, "IBM01140"));
				socket.send(request);
				
				DatagramPacket response = new DatagramPacket(recvBuffer, recvBuffer.length);
				socket.setSoTimeout(400);
				try{
					socket.receive(response);
					String res = new String(recvBuffer, "IBM01140");
					System.out.println("Recieved packet: " + res);
					if(res.charAt(0) == 'A' && Character.getNumericValue(res.charAt(1)) == (i % 2))	{ break; }	// Correct ack
				}catch(SocketTimeoutException e){
					System.out.println("Timeout occured...");
					continue;
				}
				
			}
			
		}
		
		return;
	}
	
	private void sendFIN(InetAddress ip, int port) throws IOException	//CHECK IF WE SHOULD ADD SEQ # TODO: HANDLE TIMEOUT
	{
		DatagramSocket socket = this.socket;
		socket.connect(ip, port);
		String packetString = "F000000000000000"; //F for FIN
		byte[] buffer = new byte[16];
		buffer = packetString.getBytes("IBM01140");
		DatagramPacket request = new DatagramPacket(buffer, 2);
		socket.send(request);
		
		while(true)
		{
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.setSoTimeout(400);
			try{
				socket.receive(response);
				String res = new String(buffer, "IBM01140");
				if(res.charAt(0) == 'A')	{ return; }	//Ack recieved
			}catch(SocketTimeoutException e){
				System.out.println("Timeout occured...");
				continue;
			}
		}
	}
}
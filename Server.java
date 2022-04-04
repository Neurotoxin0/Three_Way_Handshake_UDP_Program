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
import static java.lang.System.exit;

public class Server
{
    private final DatagramSocket socket;
	private final Random random;

	private DatagramPacket msg_in;
	private DatagramPacket msg_out;

	private byte[] buffer;
	private List<String> listQuotes = new ArrayList<String>();

    public Server(int port) throws SocketException
	{
        socket = new DatagramSocket(port);
        random = new Random();
    }
 
    public static void main(String[] args)
	{
		try
		{
			int port;

			if (args.length != 1)
			{
				System.out.println("--------------------------------------------------");
				System.out.println("< Invalid Number of Argument, Reading From User >");
				System.out.println("--------------------------------------------------");
				System.out.print("Usage:\t<port>\n> ");
				Scanner scanner = new Scanner(System.in);

				port = Integer.parseInt(scanner.next());
			}
			else { port = Integer.parseInt(args[0]);}

            Server server = new Server(port);
			System.out.println("\nServing On Port: " + port);
			System.out.println("--------------------------------------------------");

			String[] client_detail = server.waitForSyn().replace("/","").split(":");
            InetAddress client_ip = InetAddress.getByName(client_detail[0]);
			int client_port = Integer.parseInt(client_detail[1]);

			server.HandShakes(client_ip, client_port);
			server.SendingMessages(client_ip, client_port, "This is the data. Here is a second sentence that we'll send. Here is a third...", true);
			exit(0);
        }
		catch(NumberFormatException e)	{ System.out.println("NumberFormatException: Invalid Port #"); }
		catch(IllegalArgumentException e)	{ System.out.println("IllegalArgumentException: Invalid Port Range"); }
		catch(IOException e)	{ System.out.println("IOException: " + e.getMessage()); }
		catch (InterruptedException e) { System.out.println("InterruptedException: " + e.getMessage()); }
    }
	
	private String waitForSyn() throws IOException
	{
		DatagramSocket socket = this.socket;
		buffer = new byte[16];

		while(true)
		{
			msg_in = new DatagramPacket(buffer, buffer.length);

			try
			{
				socket.receive(msg_in);
				String client_ip = String.valueOf(msg_in.getAddress());
				int client_port = msg_in.getPort();
				System.out.println("Incoming Client: " + client_ip + ":" + client_port + "\n");

				String res = new String(buffer, "IBM01140");
				System.out.println("Received: " + res);

				if(res.charAt(0) == 'S')
				{
					System.out.println("< SYN Received >");
					return client_ip + ":" + client_port;
				}
				else { System.out.println("< Invalid SYN Received! > - " + res); }
			}
			catch(SocketException e)	{ System.out.println("SocketException: Timeout Occurred"); }
		}
	}
	
	private void HandShakes(InetAddress ip, int port) throws IOException, InterruptedException
	{
		DatagramSocket socket = this.socket;
		socket.connect(ip, port);

		System.out.println("...\nPreparing SYN ACK\n...");
		String packetString = "Z000000000000000"; // Z for SYN ACK
		buffer = packetString.getBytes("IBM01140");
		msg_out = new DatagramPacket(buffer, buffer.length);
		Thread.sleep(1000);
        socket.send(msg_out);
		System.out.println("< SYN ACK Sent > - " + packetString);
		System.out.println("...\nAwait REQ\n...");

		while(true)
		{
			msg_in = new DatagramPacket(buffer, buffer.length);

			try
			{
				socket.receive(msg_in);
				String Expected_client = "" + ip + port;
				String Current_client = "" + msg_in.getAddress() + msg_in.getPort();

				if (Expected_client.equals(Current_client))	// verify if receiving from specific client
				{
					String res = new String(buffer, "IBM01140");
					System.out.println("Received packet: " + res);

					if(res.charAt(0) == 'R')	// Request received
					{
						System.out.println("< REQ Received >");
						System.out.println("--------------------------------------------------");
						return ;
					}
					else { System.out.println("< Invalid REQ Received! > - " + res); }
				}
				else { System.out.println("Expecting Client: " + Expected_client + "\nIncoming Client: " + Current_client); }
			}
			catch(SocketTimeoutException e) { System.out.println("SocketTimeoutException: Timeout Occurred"); }
		}
	}
	
	private void SendingMessages(InetAddress ip, int port, String data, boolean loss) throws IOException
	{
		DatagramSocket socket = this.socket;
		socket.connect(ip, port);
		Random random =this.random;

		System.out.println("Splitting Messages into Chunks:\n");

		int ceil = ((int) Math.ceil((double)data.length() / 14.0)) * 14;
		data = String.format("%-" + ceil + "s", data);
		
		byte[] allBytes = data.getBytes("IBM01140");
		ArrayList<byte[]> chunks = new ArrayList<byte[]>();
		
		int i = 0;

		while((i+1) * 14 <= allBytes.length)
		{
			System.out.println("Chunk " + i + ": " + (i * 14) + " to " + ((i + 1) * 14));
			byte[] chunk = Arrays.copyOfRange(allBytes, (i * 14), ((i + 1) * 14));
			chunks.add(chunk);
			//System.out.println(new String(chunk, "IBM01140"));
			i += 1;
		}
		
		buffer = new byte[16];
		byte[] recvBuffer = new byte[16];
		System.out.println("\nSending Messages:");

		for(i = 0; i < chunks.size(); i++)
		{
			while(true)	// TODO: HANDLE TIMEOUT
			{
				buffer = ("D" + (i % 2) + "00000000000000").getBytes("IBM01140");
				System.arraycopy(chunks.get(i), 0, buffer, 2, 14);

				msg_out = new DatagramPacket(buffer, buffer.length);
				System.out.println("\nSending: " + new String(buffer, "IBM01140"));

				if(!loss || random.nextInt(5) < 4) { socket.send(msg_out); }
				else { System.out.println("< Simulating Loss Event >"); }

				msg_in = new DatagramPacket(recvBuffer, recvBuffer.length);
				socket.setSoTimeout(400);

				try
				{
					socket.receive(msg_in);
					String Expected_client = "" + ip + port;
					String Current_client = "" + msg_in.getAddress() + msg_in.getPort();

					if (Expected_client.equals(Current_client))	// verify if receiving from specific client
					{
						String res = new String(recvBuffer, "IBM01140");

						if (res.charAt(0) == 'A' && Character.getNumericValue(res.charAt(1)) == (i % 2))    // Correct ack
						{
							System.out.println("< ACK Received > - " + res);
							break;
						}
						else { System.out.println("< Invalid ACK Received! > - " + res); }
					}
					else { System.out.println("Expecting Client: " + Expected_client + "\nIncoming Client: " + Current_client); }
				}
				catch(SocketTimeoutException e) { System.out.println("< ACK Loss > - Re-transmitting Current Chunk"); }
			}
		}
		SendFIN(ip, port);
	}
	
	private void SendFIN(InetAddress ip, int port) throws IOException	//CHECK IF WE SHOULD ADD SEQ # TODO: HANDLE TIMEOUT
	{
		DatagramSocket socket = this.socket;
		socket.connect(ip, port);
		String packetString = "F000000000000000"; //F for FIN
		buffer = packetString.getBytes("IBM01140");
		msg_out = new DatagramPacket(buffer, 2);
		socket.send(msg_out);
		System.out.println("\n< FIN Sent > - " + packetString);

		while(true)
		{
			DatagramPacket msg_in = new DatagramPacket(buffer, buffer.length);
			socket.setSoTimeout(400);
			try
			{
				socket.receive(msg_in);
				String Expected_client = "" + ip + port;
				String Current_client = "" + msg_in.getAddress() + msg_in.getPort();

				if (Expected_client.equals(Current_client))	// verify if receiving from specific client
				{
					String res = new String(buffer, "IBM01140");
					if (res.charAt(0) == 'A')    //Ack received
					{
						System.out.println("< FIN ACK Received > - " + res);
						System.out.println("--------------------------------------------------");
						socket.close();
						return;
					}
					else { System.out.println("< Invalid FIN ACK Received! > - " + res); }
				}
				else { System.out.println("Expecting Client: " + Expected_client + "\nIncoming Client: " + Current_client); }
			}
			catch(SocketTimeoutException e) { System.out.println("< FIN ACK Loss > - Re-transmitting FIN"); }
		}
	}
}
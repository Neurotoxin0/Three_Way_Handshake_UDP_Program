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
import java.util.Scanner;
import java.util.Random;

import static java.lang.System.exit;

public class Client
{
	public static void main(String[] args)
    {
        String hostname;
        int port;
		byte[] buffer;
		DatagramPacket msg_out;
		DatagramPacket msg_in;

		// Get address details from Command Line Args or system.in
        if (args.length != 2)
        {
			System.out.println("--------------------------------------------------");
			System.out.println("< Invalid Number of Argument, Reading From User >");
			System.out.println("--------------------------------------------------");
			System.out.print("Usage:\t<hostname> <port>\tOR\t<hostname:port>\n> ");
            Scanner scanner = new Scanner(System.in);

			String [] raw = scanner.next().split(":");
			if (raw.length == 2)
			{
				hostname = raw[0];
				port = Integer.parseInt(raw[1]);
			}
			else
			{
				hostname = raw[0];;
				port = Integer.parseInt(scanner.next());
			}
        }
        else
        {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        }

		// Connect Part
        try
        {
            InetAddress address = InetAddress.getByName(hostname);

			System.out.println("\n--------------------------------------------------");
			System.out.println("Connecting Server: " + address + ":" + port + "\n...");
            DatagramSocket socket = new DatagramSocket();
			socket.connect(address, port);
			System.out.println("Connected");
			System.out.println("--------------------------------------------------");


            while (true)
            {
				// Send SYN
				String packetString = "S000000000000000"; //S for SYN
				buffer = packetString.getBytes("IBM01140");
				msg_out = new DatagramPacket(buffer, 1);
				socket.send(msg_out);
				System.out.println("SYN Sent");

				// Await Response
				System.out.println("...\nAwait Response\n...");
				msg_in = new DatagramPacket(buffer, buffer.length, address, port);
                socket.receive(msg_in);
				System.out.print("Packet Received: ");
                String res = new String(buffer, "IBM01140");

				// if SYN-ACK
				if (res.charAt(0)=='Z')
				{
					System.out.println("SYN-ACK Received");
					Thread.sleep(10);

					// Send REQ
					System.out.println("...\nPreparing Request\n...");
					packetString = "R000000000000000"; // R for REQ
					buffer = packetString.getBytes("IBM01140");
					msg_out = new DatagramPacket(buffer, 1);
					socket.send(msg_out);
					System.out.println("REQ sent");
					System.out.println("--------------------------------------------------");
					break;
				}
				else	// packet received != SYN-ACK --> Exiting
				{
					System.out.println("Unknown Packet Received!\nExiting...");
					exit(1);
				}
            }

			// Receiving Messages
			System.out.println("Receiving Messages:\n");
			String msg = "";
			int i = 0;
			Random rand = new Random();
			boolean loss = true;

			while (true)
			{
				String packetString;
				buffer = new byte[16];	// empty buffer

				// Receive msg

				msg_in = new DatagramPacket(buffer, buffer.length, address, port);
				socket.receive(msg_in);
                String res = new String(buffer, "IBM01140");
				System.out.println("Received: " + res);
				
				if (res.charAt(0) == 'D')
				{
					if (Character.getNumericValue(res.charAt(1)) == i)
					{
						packetString = "A" + i + "00000000000000"; // ACK
						buffer = packetString.getBytes("IBM01140");
						msg_out = new DatagramPacket(buffer, 2);
						
						if (!loss || rand.nextInt(5) < 4)
						{
							socket.send(msg_out);
							System.out.println("< ACK Sent >\n");
						}
						else { System.out.println("< Simulating ACK Loss Event >\n"); }
						
						i = (i + 1) % 2;
						msg = msg + res.substring(2);
					}
					else
					{
						System.out.println("\nUnexpected SEQ # Received, With the Message: " + res);
						System.out.print("Re-ACKing: ");

						packetString = "A" + ((i + 1) % 2) + "00000000000000"; // ACK
						buffer = packetString.getBytes("IBM01140");
						msg_out = new DatagramPacket(buffer, 2);
						
						if (!loss || rand.nextInt(5) < 4)
						{
							socket.send(msg_out);
							System.out.println("< ACK Sent >\n");
						}
						else { System.out.println("< Simulating ACK Loss Event >\n"); }
					}
				}
				else if (res.charAt(0) == 'F')	// FIN Received
				{
					packetString = "A" + i + "00000000000000"; //ACK
					buffer = packetString.getBytes("IBM01140");
					msg_out = new DatagramPacket(buffer, 2);
					socket.send(msg_out);
					System.out.println("< FIN-ACK Sent >\n");
					System.out.println("--------------------------------------------------");
					break;
				}
			}
			System.out.println("Received message: " + msg);
			System.out.println("--------------------------------------------------");
 			exit(0);
        }
		catch (UnknownHostException e) { System.out.println("\nUnknownHostException: Unknown Hostname"); }
		catch (SocketException e) { System.out.println("\nSocketException: Network is unreachable"); }
		catch (UncheckedIOException e) { System.out.println("\nUncheckedIOException: Network is unreachable"); }
        catch (SocketTimeoutException ex)
        {
            System.out.println("Timeout error: " + ex.getMessage());
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            System.out.println("Client error: " + ex.getMessage());
            ex.printStackTrace();
        }
        catch (InterruptedException ex) { ex.printStackTrace(); }
    }
}
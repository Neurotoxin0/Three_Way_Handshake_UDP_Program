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


public class Client
{
	public static void main(String[] args)
    {
        String hostname;
        int port;

        if (args.length < 2)
        {
            System.out.println("Usage: <hostname> <port>\nInput: ");
            Scanner scanner = new Scanner(System.in);
            hostname = scanner.next();
            port = Integer.parseInt(scanner.next());
        }
        else
        {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        }

        try
        {
            InetAddress address = InetAddress.getByName(hostname);
            DatagramSocket socket = new DatagramSocket();
			socket.connect(address, port);
			
            while (true) //Send SYN, REQ
            {
				String packetString = "S000000000000000"; //S for SYN

				byte[] buffer = new byte[16];
				buffer = packetString.getBytes("IBM01140");
				DatagramPacket request = new DatagramPacket(buffer, 1);
				socket.send(request);
				System.out.println("Sent SYN");
                DatagramPacket response = new DatagramPacket(buffer, buffer.length, address, port);
                socket.receive(response);
				System.out.println("Recieved packet");
                String res = new String(buffer, "IBM01140");
 
				if(res.charAt(0)=='Z'){	//If that packet is a synack
					System.out.println("SYNACK recieved");
					Thread.sleep(10);
					packetString = "R000000000000000"; //R for REQ
					buffer = packetString.getBytes("IBM01140");
					request = new DatagramPacket(buffer, 1);
					socket.send(request);
					System.out.println("REQ sent");
					
					break; //Next loop
				}
            }
			String msg = "";
			int i = 0;
			while (true){
				String packetString;
				DatagramPacket request;
				byte[] buffer = new byte[16];
				DatagramPacket response = new DatagramPacket(buffer, buffer.length, address, port);
				socket.receive(response);
                String res = new String(buffer, "IBM01140");
				System.out.println(res);
				if(res.charAt(0) == 'D'){
					if(Character.getNumericValue(res.charAt(1)) == i){
						
						packetString = "A" + i + "00000000000000"; //ACK
						buffer = packetString.getBytes("IBM01140");
						request = new DatagramPacket(buffer, 2);
						socket.send(request);
						System.out.println("ACK sent");
						i = (i + 1) % 2;
						msg = msg + res.substring(2);
					}else{
						System.out.println("Unexpected SEQ # recieved: " + res);
					}
					
				}else if(res.charAt(0) == 'F'){
					//FIN RECIEVED
					packetString = "A" + i + "00000000000000"; //ACK
					buffer = packetString.getBytes("IBM01140");
					request = new DatagramPacket(buffer, 2);
					socket.send(request);
					System.out.println("FINACK sent");
					break;
				}
			}
			System.out.println("Recieved message:");
			System.out.println(msg);
 
        }
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
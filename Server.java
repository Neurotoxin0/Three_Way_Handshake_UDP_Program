import java.io.*;
import java.net.*;
import java.util.*;

public class Server{
    private DatagramSocket socket;
    private List<String> listQuotes = new ArrayList<String>();
    private Random random;
 
    public Server(int port) throws SocketException {
        socket = new DatagramSocket(port);
        random = new Random();
    }
 
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: Server port");
            return;
        }
 
		int port = Integer.parseInt(args[1]);
		
		try {
            Server server = new Server(port);
			
            InetAddress ip = server.waitForSyn();
			server.startConn(ip);
        }catch(IOException ex){
			System.out.println("I/O error: " + ex.getMessage());
		}
		
    }
	
	private InetAddress waitForSyn() throws IOException{
		DatagramSocket socket = new DatagramSocket(); //Will probably need to set port somewhere here
		byte[] buffer = new byte[16];
		while(true){
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.receive(response);
			String res = new String(buffer, 0, response.getLength());
			if(res.charAt(0) == 'S'){
				return response.getAddress();
			}
		}
	}
	
	private void startConn(InetAddress ip) throws IOException{
		DatagramSocket socket = new DatagramSocket(); //Will probably need to set port somewhere here
		String packetString = "Z"; //Z for synack
		byte[] buffer = new byte[16];
		System.arraycopy(packetString.getBytes(), 0, buffer, 16 - packetString.length(), packetString.length());
		DatagramPacket request = new DatagramPacket(buffer, 1, ip, 100);
        socket.send(request);
		while(true){
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.receive(response);
			String res = new String(buffer, 0, response.getLength());
			if(res.charAt(0) == 'R'){ //Request recieved
				return;
			}
		}
	}
	
	private void sendFIN(InetAddress ip) throws IOException{ //CHECK IF WE SHOULD ADD SEQ #
		DatagramSocket socket = new DatagramSocket(); //Will probably need to set port somewhere here
		String packetString = "F"; //F for FIN
		byte[] buffer = new byte[16];
		System.arraycopy(packetString.getBytes(), 0, buffer, 16 - packetString.length(), packetString.length());
		DatagramPacket request = new DatagramPacket(buffer, 1, ip, 100);
        socket.send(request);
		while(true){
			DatagramPacket response = new DatagramPacket(buffer, buffer.length);
			socket.receive(response);
			String res = new String(buffer, 0, response.getLength());
			if(res.charAt(0) == 'A'){ //Ack recieved
				return;
			}
		}
	}
 
}
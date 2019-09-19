import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class serveur {

	private static ServerSocket listener;
	
	public static void main(String[] args) throws Exception {
		// compteur pour les clients
		int clientNumber = 0;
		
		// Adresses et port du serveur 
		String serverAddress = "127.0.0.1"; // aussi localhost
		int serverPort = demandePort();
		
		// Cr�ation du ServerSocket pour trouver les clients
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		InetAddress serverIP = InetAddress.getByName(serverAddress);
		
		// Association de l'Adresse et du port � la connection
		listener.bind(new InetSocketAddress(serverIP, serverPort));
		
		System.out.format("The server is running on %s:%d%n", serverAddress, serverPort);
		
		try {
			while (true) {
				// la fonction accept() est bloquante: on attend un client
				new ClientHandler(listener.accept(), clientNumber++).start();
			}
		} finally {
			// fermer la connection
			listener.close();
		}
	}
	
	public static int demandePort() {
		Scanner input = new Scanner(System.in);
		Boolean mauvaisPort = true;
		int port = 0;
		
		while(mauvaisPort) {
			System.out.println("Entrez un port d'�coute [5000-5050] : ");
			if (input.hasNextInt()) {
				port = input.nextInt();
				if (port < 5000 || port > 5050) {
					System.out.println("Port invalide!");
				} else {
					mauvaisPort = false;
				}
				
			}
			
		}
		input.close();
		return port;
	} 
	
	private static class ClientHandler extends Thread {
		private Socket socket;
		private int clientNumber;
		
		public ClientHandler(Socket socket, int clientNumber) {
			this.socket = socket;
			this.clientNumber = clientNumber;
			System.out.println("New connection with client #" 
								+ this.clientNumber + " at " 
								+ socket);
		}
		
		// Thread qui se charge d'envoyer au client un message de bienvenue
		public void run() {
			try {
				// Cr�ation d'un canal sortant pour envoyer des messages au client
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				
				// Envoie d'un message au client
				out.writeUTF("Hello from server - you are client #" + this.clientNumber);
				String command = "";
				while(command != "exit") {
					DataInputStream clientIn = new DataInputStream(socket.getInputStream());
					command = clientIn.readUTF();
					switch(command) {
						default: break;
						case "cd": 
							out.writeUTF("server fait cd");
							break;
						case "ls":
							out.writeUTF("server fait ls");
							break;
						case "mkdir":
							out.writeUTF("server fait mkdir");
							break;
						case "upload":
							out.writeUTF("server fait upload");
							break;
						case "download":
							out.writeUTF("server fait download");
							break;
					}
					out.writeUTF("exit");
				}
			} catch (IOException e) {
				System.out.println("Error handling client #" + this.clientNumber
									+ " : " + e);
			} finally {
				try {
					// Fermeture de la connection avec le client
					socket.close();
				} catch (IOException e) {
					System.out.println("Couldn't close a socket : " + e);
				}
				System.out.println("Connection with client #" + this.clientNumber + " closed");
			}
		}
	}
}

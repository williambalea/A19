import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Vector;

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
			System.out.println("Entrez un port d'écoute [5000-5050] : ");
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

		String baseDir;
		Vector<String> dirs = new Vector<String>();
		
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
				DataInputStream in = new DataInputStream(socket.getInputStream());
				String command = "";
				
				// get current directory
				baseDir = new java.io.File(".").getCanonicalPath();	
				
				while(!command.equals("exit")) {
					command = in.readUTF();
					
					if (command.length() > 2 && command.substring(0, 3).equals("cd ")) {
						System.out.println("executing command : " + command);
						String newDir = command.substring(3, command.length());
						if(newDir.equals("..")) {
							try {
								int lastElement = dirs.size() - 1;
								dirs.remove(lastElement);
							} catch(Exception e) {
								// do not pop dirs
							}
							out.writeUTF(getDir());
						} else {
							Path path = Paths.get(getDir() + '/' + newDir);
							if(!Files.exists(path)) {
								out.writeUTF("repertoire " + newDir + " introuvable!" + '\n' + getDir());
							} else {
								dirs.add(newDir);
								out.writeUTF(getDir());
							}
						}

					} else if (command.equals("ls")) {
						System.out.println("executing command : " + command);
						File dir = new File("." + getDirsList());
						File[] liste = dir.listFiles();
						String affichage = getDir() + '\n';
						for (File fichier : liste) {
							if(fichier.isFile() || fichier.isDirectory()) {
								affichage += fichier.getName() + '\t';
							}
						}
						out.writeUTF(affichage);
	
					} else if (command.length() > 5 && command.substring(0, 6).equals("mkdir ")) {
						System.out.println("executing command : " + command);
					 	Path path = Paths.get(getDir() + '/' + command.substring(6, command.length()));
					 	if(!Files.exists(path)) {
					 		Files.createDirectory(path);
					 		out.writeUTF("Nouveau dossier " + command.substring(6, command.length()) + " cr�e!");
					 	} else {
					 		out.writeUTF(command.substring(6, command.length()) + " existe deja!");
					 	}

					} else if (command.equals("upload")) {
						out.writeUTF("server fait upload");
						System.out.println("executing command : " + command);
						
					} else if (command.equals("download")) {
						out.writeUTF("server fait download");
						System.out.println("executing command : " + command);
						
					} else if (command.equals("exit")) {
						// do nothing
					} else {
						out.writeUTF("commande invalide!");
						
					}
					
				}
				out.writeUTF("fermeture de la connection avec le serveur");
				out.flush();
				
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
		
		public String getDirsList() {
			String buffer = "";
			for(String dir : dirs) {
				buffer += "\\" + dir;
			}
			return buffer;
		}

		public String getDir() {
			return baseDir + getDirsList();
		}
	}
}

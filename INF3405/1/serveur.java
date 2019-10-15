import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;

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
				// creation canal pour envoyer fichier
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				// OutputStream fileOut = socket.getOutputStream();
				
				// Envoie d'un message au client
				// envoie d'un fichier au client
				out.writeUTF("Hello from server - you are client #" + this.clientNumber);
				DataInputStream in = new DataInputStream(socket.getInputStream());
				String command = "";
				
				// get current directory
				baseDir = new java.io.File(".").getCanonicalPath();

				// setting the root of server
				Path serverRootPath = Paths.get(getDir() + "/serverRoot");
				if(!Files.exists(serverRootPath)) {
					Files.createDirectory(serverRootPath);
				}
				baseDir = new java.io.File("./serverRoot").getCanonicalPath();

				// gerer envoi/reception des fichiers
				OutputStream fileOut;
				
				while(!command.equals("exit")) {
					command = in.readUTF();
					
					// cd 
					if (command.length() > 2 && command.substring(0, 3).equals("cd ")) {
						System.out.println("executing command : " + command);
						String newDir = command.substring(3, command.length());
						if(newDir.equals("..")) {
							try {
								int lastElement = dirs.size() - 1;
								dirs.remove(lastElement);
								out.writeUTF(getDir() + "\nVous etes dans le dossier " + getDirName());
							} catch(Exception e) {
								out.writeUTF("Impossible de sortir du repertoire serverRoot : acces refuse");
							}
						} else {
							Path path = Paths.get(getDir() + '/' + newDir);
							if(!Files.exists(path)) {
								out.writeUTF("repertoire " + newDir + " introuvable!" + '\n' + getDir());
							} else {
								dirs.add(newDir);
								out.writeUTF(getDir() + "\nVous etes dans le dossier " + getDirName());
							}
						}
					
					// ls
					} else if (command.equals("ls")) {
						System.out.println("executing command : " + command);
						File dir = new File(".\\serverRoot" + getDirsList());
						File[] liste = dir.listFiles();
						String affichage = getDir();
						for (File fichier : liste) {
							if(fichier.isFile()) {
								affichage += '\n' + "[File] " + fichier.getName();
							}
							
							if (fichier.isDirectory()) {
								affichage += '\n' + "[Folder] " + fichier.getName();
							}
						}
						out.writeUTF(affichage);
	
					// mkdir
					} else if (command.length() > 5 && command.substring(0, 6).equals("mkdir ")) {
						System.out.println("executing command : " + command);
						String newDir = command.substring(6, command.length());
					 	Path path = Paths.get(getDir() + '/' + newDir);
					 	if(!Files.exists(path)) {
					 		Files.createDirectory(path);
					 		out.writeUTF("Le dossier " + newDir + " a ete cree!");
					 	} else {
					 		out.writeUTF(newDir + " existe deja!");
					 	}

					// upload
					} else if (command.length() > 6 && command.substring(0, 7).equals("upload ")) {
						String fileName = command.substring(7, command.length());
						out.writeUTF("Le fichier " + fileName + " a bien ete televerse");
						System.out.println("executing command : " + command);
						fileOut = new FileOutputStream(getDir() + '/' + fileName);
						envoyerFichier(in, fileOut);
					
					//download
					} else if (command.length() > 8 && command.substring(0, 9).equals("download ")) {
						String fileName = command.substring(9, command.length());
						Path path = Paths.get(getDir() + '/' + fileName);
						if(!Files.exists(path)) {
							out.writeUTF("fichier " + fileName + " introuvable!" + '\n' + getDir());
						} else {
							out.writeUTF("Le fichier " + fileName + " a bien ete telecharge");
							System.out.println("executing command : " + command);
							InputStream fileIn = new FileInputStream(getDir() + '/' + fileName);
							envoyerFichier(fileIn, out);
						}
						
					// exit
					} else if (command.equals("exit")) {
						// do nothing

					// other commands
					} else {
						out.writeUTF("commande invalide!");
						
					}
					
				}
				out.writeUTF("Vous avez ete deconnecte avec succes.");
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

		public String getDirName() {
			String[] allDirs = getDir().split(Pattern.quote(File.separator));
			int lastElement = allDirs.length - 1;
			return allDirs[lastElement];
		}

		public void envoyerFichier(InputStream fileIn, OutputStream fileOut) throws IOException {
			int bufferSize = 8192;
			byte[] buffer = new byte[bufferSize];
			int compteur = 0;
			while ((compteur = fileIn.read(buffer)) > 0) {
				fileOut.write(buffer, 0, compteur);
				if(compteur != bufferSize)
					break;
			}
		}
	}
}

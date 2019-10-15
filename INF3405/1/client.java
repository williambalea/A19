import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class client {
	private static Socket socket;
	
	public static void main(String[] args) throws Exception {
		
		// Adresse et port du serveur
		Scanner s = new Scanner(System.in);
		String serverAddress = demandeAdresse(s);
		int port = demandePort(s);
		
		// Creation d'une nouvelle connection ave le serveur
		socket = new Socket(serverAddress, port);
		System.out.format("The server is running on %s:%d%n", serverAddress, port);
		
		// Creation d'un canal entrant pour recevoir les messages envoyés par le server
		DataInputStream in = new DataInputStream(socket.getInputStream());
		
		// Attente de la reception d'un message envoye par le server
		String message = in.readUTF();
		System.out.println(message);
		
		// gerer la communication avec le serveur
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		String command = "";
		
		// gerer l'envoi/la reception des fichiers
		OutputStream fileOut;
		String baseDir = new java.io.File(".").getCanonicalPath();
		
		
		while(!command.equals("exit")) {
						
			System.out.println(">>> ");
			command = input.readLine();

			if(command.length() > 6 && command.substring(0, 7).equals("upload ")) {
				String fileName = command.substring(7, command.length());
				Path path = Paths.get(baseDir + '/' + fileName);
				if(!Files.exists(path)) {
					command = "";
					out.writeUTF(command);
					out.flush();
					message = in.readUTF();
					System.out.println(message);
				} else {
					out.writeUTF(command); // hey server je veux faire upload car.jpg
					message = in.readUTF(); // hey client j'execute la commande upload
					InputStream fileIn = new FileInputStream(baseDir + '/' + fileName);
					envoyerFichier(fileIn, out);
					System.out.println(message);
				}
			} else {
				out.writeUTF(command);
				out.flush();
				message = in.readUTF();
				System.out.println(message);
				
				if(message.substring(0, 2).equals("Le") && command.length() > 8 && command.substring(0, 9).equals("download ")) {
					String fileName = command.substring(9, command.length());
					fileOut = new FileOutputStream(fileName);
					envoyerFichier(in, fileOut);
				}
			}
		}
		System.out.println("Client : Vous avez sorti de la boucle while.");
		
		// Fermeture de la connection avec le server
		socket.close();
	}
	
	public static boolean isNumeric(String s) {
        if (s == null || s.length() == 0) {
            return false;
        }

        return s.chars().allMatch(Character::isDigit);
    }
	
	private static int demandePort(Scanner s) {
		Boolean mauvaisPort = true;
		int port = 0;
		while (mauvaisPort) {
			System.out.println("Entrez un port d'écoute [5000-5050] : ");
			String input = s.next();
			if(isNumeric(input)) {
				port = Integer.parseInt(input);
				if (port >= 5000 && port <= 5050) {
					mauvaisPort = false;
				} else {
					System.out.println("Port invalide!");
				}
			} else {
				System.out.println("Port invalide!");
			}
		}
		return port;
	}
	
	public static String demandeAdresse(Scanner s) {
		Boolean mauvaiseAdresse = true;
		String adresse = "";
		
		while(mauvaiseAdresse) {
			System.out.println("Entrez l'adresse du serveur : ");
			adresse = s.next();
			if(adresse.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
				mauvaiseAdresse = false;
			} else {
				System.out.println("Adresse invalide!");
			}
			
		}
		return adresse;
	}
	
	public static void envoyerFichier(InputStream fileIn, OutputStream fileOut) throws IOException {
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
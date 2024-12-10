import java.io.*;
import java.net.*;
import java.util.*;
/**
 * This is my client class which is what players connect to the server with
 */
public class Client {

	private Scanner sc;//used to scan input
	private PrintWriter out;//used to output data
	private Socket socket;//used to connect to the handler
	private BufferedReader in;//used to take in data from the player
	/**
	 * My main constructor for a client
	 */
	public Client() {
		sc = new Scanner(System.in);
	}//end constructor
	
	/**
	 * My start method allows for players to input data to continues the game or exit
	 */

	public void start() {
		try {

			socket = new Socket("localhost", 12345);
			out = new PrintWriter(socket.getOutputStream(), true);

			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			new Thread(this::listenForServerUpdates).start();
			handlePlayerInput();

			String line = null;

			while (!"exit".equalsIgnoreCase(line)) {

				line = sc.nextLine();

				out.println(line);
				out.flush();

				System.out.println("Server replied " + in.readLine());
			}

			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeResources();
		}
	}//end start
	/**
	 * This method is used to out messages from the server
	 */
	private void listenForServerUpdates() {
		try {
			String serverMessage;
			while ((serverMessage = in.readLine()) != null) {
				System.out.println("Server: " + serverMessage);

				if (serverMessage.startsWith("UPDATE_BOARD")) {
					renderGameBoard(serverMessage);
				} else if (serverMessage.startsWith("YOUR_TURN")) {
					System.out.println("It's your turn! Draw a card.");
				}
			}
		} catch (IOException e) {
			System.err.println("Connection to the sever lost.");
		}
	}//end listenForServerUpdates
	/**
	 * Handles data input by the player
	 */

	private void handlePlayerInput() {
		try {
			while (true) {
				String playerInput = sc.nextLine();

				out.println(playerInput);

				if ("exit".equalsIgnoreCase(playerInput.trim())) {
					System.out.println("Exiting the game...");
					
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Error handling input: " + e.getMessage());
		}
	}//end handlePlayerInput
	/**
	 * This method is not currently used but in the future i would like to add graphics
	 * @param boardData the board
	 */
	private void renderGameBoard(String boardData) {
		System.out.println("Rendering game board");
		System.out.println(boardData);
	}//end renderGameBoard
	/**
	 * this closes the resources used by the client if they leave
	 */
	private void closeResources() {
		try {
			if (socket != null) {
				socket.close();

			}
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (sc != null) {
				sc.close();
			}
		} catch (IOException e) {
			System.err.println("Error closing resources: " + e.getMessage());
		}
	}//end closeResources
	
	/**
	 * my main method to create a client
	 * @param args
	 */

	public static void main(String[] args) {
		Client client = new Client();
		client.start();

	}//end main
}//end class

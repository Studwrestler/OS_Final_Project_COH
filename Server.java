
/**
 * This is my server class it communicates data to the Clients throught the Handler
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
	private static final int port = 12345;// used as port for server
	private static final int Max_players = 4;// number of players needed

	private ServerSocket server;// Creation for server socket
	private final GameState gameState = new GameState();// Gamestate for sorry
	private final CountDownLatch latch = new CountDownLatch(Max_players);// use to wait till all players ready
	private final List<String> playerNames = new ArrayList<>();// Names of players
	private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();//list of clients

	/**
	 * This is my method for starting the program
	 */

	public void start() {
		try {
			server = new ServerSocket(port);
			System.out.println("Server started. Waiting for players to connect...");

			while (clients.size() < Max_players) {
				Socket clientSocket = server.accept();
				System.out.println("Player connected: " + clientSocket.getInetAddress().getHostAddress());

				ClientHandler clientHandler = new ClientHandler(clientSocket, this, latch);
				clients.add(clientHandler);
				Thread clientThread =new Thread(clientHandler);
				clientHandler.setThread(clientThread);
				clientThread.start();
			}
			try {
				latch.await();
				System.out.println("All players are ready. Starting the game...");
				broadcast("Game is starting! Please enter draw to see if its your turn.");
				gameState.setServer(this);
				startGame();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.err.println("Thread interrupted while waiting for players to be ready");
			}

		} catch (IOException e) {
			System.err.println("Server error: " + e.getMessage());
		} finally {
			closeServer();
		}
	}// end start

	/**
	 * This is my method for broadcasting info to each player
	 * 
	 * @param message the message being sent
	 */

	public synchronized void broadcast(String message) {
		for (ClientHandler client : clients) {
			client.sendMessage(message);
		}
	}// end broadcast

	/**
	 * Method to add a player
	 * 
	 * @param playerName name of player
	 */
	public synchronized void addPlayer(String playerName) {
		playerNames.add(playerName);
	}// end addPlayer

	/**
	 * Method for starting the game
	 */
	public void startGame() {
		gameState.startGame(playerNames);
	}// end startGame

	/**
	 * THis is my method for processing the move being made and letting players know
	 * 
	 * @param playerName name of the current player
	 * @param move       where the player moved
	 */

	public synchronized void processMove(String playerName, String move) {

		String response = gameState.processMove(playerName, move);
		broadcast(response);

		broadcast("Game State: " + gameState.getCurrentState());

		String nextPlayer = gameState.getCurrentPlayer();
		broadcast("It's " + nextPlayer + "'s turn.");
	}// end processMove

	/**
	 * My method for closing the Server
	 */

	public void closeServer() {

		try {
			if (server != null)
				server.close();

		} catch (IOException e) {
			System.err.println("Error closing server: " + e.getMessage());
		}
	}// end Server

	/**
	 * My main method for the server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		Server server = new Server();
		server.start();
		

	}// end main
	/**
	 * Method for closing all client connections and stopping their threads
	 */
	public synchronized void closeConnections() {
	    try {
	        // Create a list to keep track of threads that need to be joined
	        List<Thread> clientThreads = new ArrayList<>();

	        // Loop through each client and close their connection
	        for (ClientHandler clientHandler : clients) {
	            // Close the connection for each client
	            clientHandler.closeConnection();
	            // Add the client handler's thread to the list for later joining
	            clientThreads.add(clientHandler.thread);
	        }
	        clients.clear();

	        // Wait for each client thread to finish
	        for (Thread clientThread : clientThreads) {
	            if (clientThread != null && clientThread.isAlive()) {
	                clientThread.join();  // Wait for the client thread to complete
	            }
	        }

	        System.out.println("All client connections and threads have been closed.");

	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();  // Restore interrupted status
	        System.err.println("Error while waiting for client threads to finish: " + e.getMessage());
	    }
	}//end closeConnections


	/**
	 * My ClientHandler class that handles the each clients thread
	 */

	private static class ClientHandler implements Runnable {
		private final Socket socket;// used to connect to the server
		private final Server server;// used to connect to the server
		private final CountDownLatch latch;// used to wait for each player
		private PrintWriter out;// used to print info to player
		private BufferedReader in;// used to bring in info from the player
		private String playerName;// hold the players name
		private Thread thread;
		

		/**
		 * This is my constructor to use a Client Handler
		 * 
		 * @param socket which socket to connect to
		 * @param server which server to connect to
		 * @param latch  used to make sure each player is connected
		 */

		public ClientHandler(Socket socket, Server server, CountDownLatch latch) {
			this.socket = socket;
			this.server = server;
			this.latch = latch;
			try {
				this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				this.out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				System.err.println("Error initializing client handler: " + e.getMessage());
			}

		}// end constructor

		/**
		 * This is my run method used to get infor from the player
		 */
		public void run() {
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				out.println("Enter your name:");
				playerName = in.readLine();
				server.addPlayer(playerName);
				server.broadcast(playerName + " has joined the game!");

				latch.countDown();

				out.println("Waiting for game to start...");

				String message;
				while ((message = in.readLine()) != null) {
					System.out.println(playerName + ": " + message);
					server.processMove(playerName, message);

					if ("exit".equalsIgnoreCase(message)) {
						break;
					}
				}

			} catch (IOException e) {
				System.err.println("Connection error with player " + playerName);
			} finally {
				closeConnection();
			}
		}//end run
		/**
		 * My sendMessage method is used to send messages to the player
		 */

		public void sendMessage(String message) {
			out.println(message);
		}//end sendMessage
		
		/**
		 * my closeConnection method closes all info for the client
		 */

		private void closeConnection() {
			try {
				if (socket != null)
					socket.close();
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
				System.err.println("Error closing connection for player " + playerName);
			}
			server.clients.remove(this);
			server.broadcast(playerName + " has left the game.");
		}//end closeConnection
		/**
	     * This method sets the thread handling this client
	     * 
	     * @param thread the thread handling this client
	     */
	    public void setThread(Thread thread) {
	        this.thread = thread;
	    }

	    /**
	     * This method gets the thread handling this client
	     * 
	     * @return the thread handling this client
	     */
	}//end class
}//end class

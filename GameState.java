import java.util.*;

/**
 * This is where the game is created and played
 */
public class GameState {
	private final Map<String, Integer> playerPositions = new LinkedHashMap<>();// used to map player positions
	private final Queue<String> turnQueue = new LinkedList<>();// used to control turns
	private int deckIndex = 0;// used to set index of the deck
	private final List<String> deck = Arrays.asList("5", "7", "9", "4", "2", "Sorry!", "7", "8", "1", "9", "6", "5",
			"7", "9", "4", "2", "Sorry!", "7", "8", "1", "9", "6"); // Cards to choose from in the deck
	private final List<String> shuffledDeck = new ArrayList<>(deck);// way to shuffle deck
	private Server server;// used to link to server

	/**
	 * Method to start the game
	 * 
	 * @param playerNames adds the players to their positions
	 */

	public void startGame(List<String> playerNames) {
		for (String playerName : playerNames) {
			playerPositions.put(playerName, 0);
			turnQueue.add(playerName);
		}
		resetDeck();
	}// end startGame

	/**
	 * This is where most of the action occurs the player draws and moves within
	 * this class
	 * 
	 * @param playerName current players turn
	 * @param move       how end up moving
	 * @return how the moved
	 */

	public synchronized String processMove(String playerName, String move) {
		String response;
		if (!turnQueue.peek().equals(playerName)) {
			return "It's " + getCurrentPlayer() + " turn";
		}
		if ("draw".equalsIgnoreCase(move)) {
			if (deckIndex >= shuffledDeck.size()) {
				resetDeck();
				response = "Deck is empty. Shuffling cards.";
			} else {
				String card = shuffledDeck.get(deckIndex++);
				response = playerName + " drew a card: " + card;

				if ("Sorry!".equals(card)) {
					response += "\n" + processSorryCard(playerName);
				} else {
					try {
						int moveValue = Integer.parseInt(card);
						response += "\n" + applyMove(playerName, moveValue, server);
					} catch (NumberFormatException e) {
						response += "\nInvalid card value: " + card;
					}
				}
			}
		} else {
			try {
				int moveValue = Integer.parseInt(move);
				response = applyMove(playerName, moveValue, server);
			} catch (NumberFormatException e) {
				return "Invalid move. Please enter 'draw' to draw a card.";
			}
			turnQueue.add(turnQueue.poll());
			return response;
		}

		turnQueue.add(turnQueue.poll());
		return response;
	}// end processMove

	/**
	 * This is the main class where the person actually moves based off card drawn
	 * 
	 * @param playerName current player
	 * @param moveValue  amount moved
	 * @param server     server created for ending server
	 * @return used to return the updated position
	 */

	private String applyMove(String playerName, int moveValue, Server server) {
		int currentPosition = playerPositions.get(playerName);
		int newPosition = currentPosition + moveValue;

		if (moveValue > 0 && newPosition >= 10) {
			playerPositions.put(playerName, newPosition);
			if (server != null) {
				server.broadcast(playerName + " has won the game! The game will end shortly.");
				
				server.closeConnections();
				server.closeServer();

			}
			return "Server Closing";

		}

		playerPositions.put(playerName, newPosition);
		return playerName + " moved to position " + newPosition;
	}// end applyMove

	/**
	 * sets the server
	 */
	public void setServer(Server server) {
		this.server = server;
	}// end setServer

	/**
	 * gets the current state of the game
	 * 
	 * @return player positions and turn order
	 */

	public synchronized String getCurrentState() {
		return "Player Positions: " + playerPositions + " | Turn Queue: " + turnQueue;

	}// end getCurrentState

	/**
	 * shuffles and resets the deck
	 */

	private void resetDeck() {
		Collections.shuffle(shuffledDeck);
		deckIndex = 0;
		System.out.println("Deck has been reset and shuffled!");

	}// end resetDeck

	/**
	 * USed to process the special sorry card that resets the next player
	 * 
	 * @param playerName current player
	 * @return the player who sorryed the player
	 */

	private String processSorryCard(String playerName) {
		String victim = getNextPlayer();
		playerPositions.put(victim, 0);
		return playerName + " played a Sorry! and sent " + victim + " back to start!";
	}// end processSorryCard

	/**
	 * Used to get the next player
	 * 
	 * @return the next player
	 */

	public String getNextPlayer() {
		List<String> players = new ArrayList<>(turnQueue);
		if (players.size() > 1) {
			return players.get(1);
		}
		return null;
	}// end getNextPlayer

	/**
	 * get the current player
	 * 
	 * @return the current player
	 */

	public String getCurrentPlayer() {
		return turnQueue.peek();
	}// end getCurrentPlayer

}// end class

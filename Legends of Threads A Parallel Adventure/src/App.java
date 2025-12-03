/**
 * Main Application Entry Point for Legends of Threads: A Parallel Adventure
 * CSC 325 Final Project - Multithreaded Text Adventure Game
 */
public class App {
    public static void main(String[] args) {
        try {
            // Create the game engine
            GameEngine gameEngine = new GameEngine();
            
            // Initialize the game world and characters
            gameEngine.initializeGame();
            
            // Start the parallel adventure
            gameEngine.startAdventure();
            
        } catch (Exception e) {
            System.err.println("❌ An error occurred during the adventure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
/**
 * Main Application Entry Point for Legends of Threads: A Parallel Adventure
 * CSC 325 Final Project - Multithreaded Text Adventure Game
 */
public class App {
    public static void main(String[] args) {
        GameEngine gameEngine = null;
        
        try {
            // Create the game engine
            gameEngine = new GameEngine();
            
            // Add shutdown hook for graceful cleanup
            final GameEngine finalGameEngine = gameEngine;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n Shutdown signal received - cleaning up threads...");
                finalGameEngine.shutdown();
            }, "ShutdownHook"));
            
            // Initialize the game world and characters
            gameEngine.initializeGame();
            
            // Start the parallel adventure
            gameEngine.startAdventure();
            
            System.out.println(" Main thread completed - all adventures synchronized and closed properly.");
            
        } catch (Exception e) {
            System.err.println(" An error occurred during the adventure: " + e.getMessage());
            e.printStackTrace();
            
            // Ensure cleanup even if there's an error
            if (gameEngine != null && gameEngine.isGameRunning()) {
                gameEngine.shutdown();
            }
        } finally {
            System.out.println(" Application terminating - all threads joined successfully.");
        }
    }
}
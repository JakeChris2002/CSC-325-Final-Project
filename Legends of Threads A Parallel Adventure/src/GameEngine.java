import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GameEngine - Main controller that manages all character threads and game state
 * Coordinates the parallel adventure and handles user interaction
 */
public class GameEngine {
    private List<GameCharacter> characters;
    private List<Thread> characterThreads;
    private boolean gameRunning;
    private Scanner scanner;
    private ReentrantLock gameLock;
    private long gameStartTime;
    private int gameRounds;
    
    public GameEngine() {
        this.characters = new ArrayList<>();
        this.characterThreads = new ArrayList<>();
        this.gameRunning = false;
        this.scanner = new Scanner(System.in);
        this.gameLock = new ReentrantLock();
        this.gameRounds = 0;
    }
    
    /**
     * Initialize the game world and create characters
     */
    public void initializeGame() {
        System.out.println("===============================================");
        System.out.println("🏰 WELCOME TO LEGENDS OF THREADS 🏰");
        System.out.println("    A Parallel Adventure Awaits!");
        System.out.println("===============================================\n");
        
        // Create the three main characters
        Knight knight = new Knight("Sir Galahad", 0, 0);
        Thief thief = new Thief("Shadowstep", 5, 5);
        Wizard wizard = new Wizard("Arcanum", 10, 10);
        
        // Add characters to our management lists
        characters.add(knight);
        characters.add(thief);
        characters.add(wizard);
        
        System.out.println("📋 HEROES ASSEMBLED:");
        characters.forEach(character -> {
            System.out.println("   " + character.toString());
        });
        System.out.println();
    }
    
    /**
     * Start all character threads and begin the adventure
     */
    public void startAdventure() {
        gameRunning = true;
        gameStartTime = System.currentTimeMillis();
        
        System.out.println("🚀 ADVENTURE BEGINS! All heroes start their quests...\n");
        
        // Create and start threads for each character
        for (GameCharacter character : characters) {
            Thread characterThread = new Thread(character, character.getName() + "Thread");
            characterThreads.add(characterThread);
            characterThread.start();
            System.out.println("🧵 Started thread for " + character.getName() + " the " + character.getCharacterType());
        }
        
        System.out.println("\n✨ All threads are running in parallel! Watch the adventure unfold...\n");
        
        // Start the game monitoring thread
        Thread monitorThread = new Thread(this::monitorGame, "GameMonitorThread");
        monitorThread.start();
        
        // Start user interaction
        handleUserInteraction();
    }
    
    /**
     * Monitor the game state and provide periodic updates
     */
    private void monitorGame() {
        while (gameRunning) {
            try {
                Thread.sleep(10000); // Update every 10 seconds
                gameRounds++;
                displayGameStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Display current game status and character statistics
     */
    private void displayGameStatus() {
        gameLock.lock();
        try {
            long elapsedTime = (System.currentTimeMillis() - gameStartTime) / 1000;
            System.out.println("\n" + "=".repeat(50));
            System.out.println("📊 ADVENTURE STATUS - Round " + gameRounds + " (Time: " + elapsedTime + "s)");
            System.out.println("=".repeat(50));
            
            for (GameCharacter character : characters) {
                System.out.printf("%-15s | Health: %3d/%3d | Position: (%2d,%2d) | Items: %2d | Status: %s%n",
                    character.getCharacterType() + " " + character.getName(),
                    character.getHealth(),
                    character.getMaxHealth(),
                    character.getX(),
                    character.getY(),
                    character.getInventory().size(),
                    character.isAlive() ? (character.isActive() ? "Active" : "Inactive") : "Defeated"
                );
            }
            
            // Display character-specific stats
            displayCharacterStats();
            System.out.println("=".repeat(50) + "\n");
            
        } finally {
            gameLock.unlock();
        }
    }
    
    /**
     * Display character-specific statistics
     */
    private void displayCharacterStats() {
        for (GameCharacter character : characters) {
            if (character instanceof Knight knight) {
                System.out.println("   🛡️ " + knight.getName() + " - Armor: " + knight.getArmor() + 
                                 ", Quests: " + knight.getQuestsCompleted());
            } else if (character instanceof Thief thief) {
                System.out.println("   🗡️ " + thief.getName() + " - Stealth: " + thief.getStealth() + 
                                 ", Items Stolen: " + thief.getItemsStolen());
            } else if (character instanceof Wizard wizard) {
                System.out.println("   🔮 " + wizard.getName() + " - Mana: " + wizard.getMana() + "/" + 
                                 wizard.getMaxMana() + ", Spells Cast: " + wizard.getSpellsCast());
            }
        }
    }
    
    /**
     * Handle user interaction during the game
     */
    private void handleUserInteraction() {
        System.out.println("💬 GAME COMMANDS:");
        System.out.println("   'status' - Show current game status");
        System.out.println("   'interact' - Force character interactions");
        System.out.println("   'pause' - Pause all characters");
        System.out.println("   'resume' - Resume all characters");
        System.out.println("   'quit' - End the adventure");
        System.out.println("   Press Enter to continue watching...\n");
        
        while (gameRunning) {
            String input = scanner.nextLine().trim().toLowerCase();
            
            switch (input) {
                case "status":
                    displayGameStatus();
                    break;
                case "interact":
                    forceCharacterInteractions();
                    break;
                case "pause":
                    pauseAllCharacters();
                    break;
                case "resume":
                    resumeAllCharacters();
                    break;
                case "quit":
                    endAdventure();
                    return;
                case "":
                    // Just continue watching
                    break;
                default:
                    System.out.println("❓ Unknown command. Type 'quit' to end the adventure.");
            }
        }
    }
    
    /**
     * Force interactions between characters
     */
    private void forceCharacterInteractions() {
        System.out.println("\n🤝 FORCING CHARACTER INTERACTIONS...\n");
        
        for (int i = 0; i < characters.size(); i++) {
            for (int j = i + 1; j < characters.size(); j++) {
                GameCharacter char1 = characters.get(i);
                GameCharacter char2 = characters.get(j);
                
                if (char1.isAlive() && char2.isAlive()) {
                    char1.interact(char2);
                    char2.interact(char1);
                }
            }
        }
        System.out.println();
    }
    
    /**
     * Pause all character threads
     */
    private void pauseAllCharacters() {
        System.out.println("⏸️ Pausing all characters...\n");
        for (GameCharacter character : characters) {
            character.stop();
        }
    }
    
    /**
     * Resume all character threads (restart them)
     */
    private void resumeAllCharacters() {
        System.out.println("▶️ Resuming all characters...\n");
        
        // Stop existing threads
        characterThreads.clear();
        
        // Restart characters that are still alive
        for (GameCharacter character : characters) {
            if (character.isAlive()) {
                // Reset character state
                if (character instanceof Knight knight) {
                    Knight newKnight = new Knight(knight.getName(), knight.getX(), knight.getY());
                    int index = characters.indexOf(character);
                    characters.set(index, newKnight);
                    character = newKnight;
                } else if (character instanceof Thief thief) {
                    Thief newThief = new Thief(thief.getName(), thief.getX(), thief.getY());
                    int index = characters.indexOf(character);
                    characters.set(index, newThief);
                    character = newThief;
                } else if (character instanceof Wizard wizard) {
                    Wizard newWizard = new Wizard(wizard.getName(), wizard.getX(), wizard.getY());
                    int index = characters.indexOf(character);
                    characters.set(index, newWizard);
                    character = newWizard;
                }
                
                Thread newThread = new Thread(character, character.getName() + "Thread");
                characterThreads.add(newThread);
                newThread.start();
            }
        }
    }
    
    /**
     * End the adventure and clean up all threads
     */
    public void endAdventure() {
        System.out.println("\n🏁 ENDING THE ADVENTURE...");
        
        gameRunning = false;
        
        // Stop all characters
        for (GameCharacter character : characters) {
            character.stop();
        }
        
        // Wait for all threads to finish
        for (Thread thread : characterThreads) {
            try {
                thread.join(2000); // Wait up to 2 seconds for each thread
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Display final statistics
        displayFinalStats();
        
        System.out.println("👋 Thank you for playing Legends of Threads!");
        System.out.println("===============================================");
    }
    
    /**
     * Display final game statistics
     */
    private void displayFinalStats() {
        long totalTime = (System.currentTimeMillis() - gameStartTime) / 1000;
        
        System.out.println("\n📈 FINAL ADVENTURE STATISTICS:");
        System.out.println("   Total Adventure Time: " + totalTime + " seconds");
        System.out.println("   Total Rounds: " + gameRounds);
        System.out.println("\n🏆 HERO FINAL STATUS:");
        
        for (GameCharacter character : characters) {
            System.out.println("   " + character.toString());
            
            if (character instanceof Knight knight) {
                System.out.println("      - Quests Completed: " + knight.getQuestsCompleted());
            } else if (character instanceof Thief thief) {
                System.out.println("      - Items Stolen: " + thief.getItemsStolen());
            } else if (character instanceof Wizard wizard) {
                System.out.println("      - Spells Cast: " + wizard.getSpellsCast());
            }
        }
    }
    
    /**
     * Get all characters (for potential GameWorld integration)
     */
    public List<GameCharacter> getAllCharacters() {
        return new ArrayList<>(characters);
    }
    
    /**
     * Check if game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }
}
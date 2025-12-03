import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private SharedResources sharedResources;
    private GameAnalytics analytics;
    private long gameStartTime;
    private int gameRounds;
    private static final long ADVENTURE_DURATION = 60000; // 60 seconds
    private Thread monitorThread;
    
    public GameEngine() {
        this.characters = new ArrayList<>();
        this.characterThreads = new ArrayList<>();
        this.gameRunning = false;
        this.scanner = new Scanner(System.in);
        this.gameLock = new ReentrantLock();
        this.gameRounds = 0;
        this.sharedResources = new SharedResources();
        this.analytics = new GameAnalytics();
    }
    
    /**
     * Initialize the game world and create characters
     */
    public void initializeGame() {
        System.out.println("===============================================");
        System.out.println("🏰 WELCOME TO LEGENDS OF THREADS 🏰");
        System.out.println("    A Parallel Adventure Awaits!");
        System.out.println("===============================================\n");
        
        // Create the three main characters with shared resources and analytics
        Knight knight = new Knight("Sir Galahad", 0, 0, sharedResources, analytics);
        Thief thief = new Thief("Shadowstep", 5, 5, sharedResources, analytics);
        Wizard wizard = new Wizard("Arcanum", 10, 10, sharedResources, analytics);
        
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
        monitorThread = new Thread(this::monitorGame, "GameMonitorThread");
        monitorThread.start();
        
        // Start user interaction
        handleUserInteraction();
        
        // Wait for all threads to complete
        waitForAdventureCompletion();
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
                
                // Check if adventure should end naturally
                long elapsedTime = System.currentTimeMillis() - gameStartTime;
                if (elapsedTime >= ADVENTURE_DURATION) {
                    System.out.println("\n⏰ The adventure has reached its natural conclusion after " + 
                                     (elapsedTime / 1000) + " seconds!");
                    gameRunning = false;
                    break;
                }
                
                // Check if all characters are defeated
                boolean anyAlive = characters.stream().anyMatch(GameCharacter::isAlive);
                if (!anyAlive) {
                    System.out.println("\n💀 All heroes have fallen! The adventure ends in tragedy...");
                    gameRunning = false;
                    break;
                }
                
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
            
            // Display shared resource status
            System.out.println(sharedResources.getResourceStatus());
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
        System.out.println("   'resources' - Show detailed shared resource status");
        System.out.println("   'analytics' - Show comprehensive game analytics");
        System.out.println("   'stats [character]' - Show specific character statistics");
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
                case "resources":
                    displayDetailedResourceStatus();
                    break;
                case "analytics":
                    displayGameAnalytics();
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
                    System.out.println("🛑 User requested to end the adventure...");
                    gameRunning = false; // This will trigger cleanup in waitForAdventureCompletion
                    return;
                case "":
                    // Just continue watching
                    break;
                default:
                    // Handle stats command with lambda
                    if (input.startsWith("stats ")) {
                        String characterName = input.substring(6).trim();
                        displayCharacterAnalytics(characterName);
                    } else {
                        System.out.println("❓ Unknown command. Type 'quit' to end the adventure.");
                    }
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
                    Knight newKnight = new Knight(knight.getName(), knight.getX(), knight.getY(), sharedResources, analytics);
                    int index = characters.indexOf(character);
                    characters.set(index, newKnight);
                    character = newKnight;
                } else if (character instanceof Thief thief) {
                    Thief newThief = new Thief(thief.getName(), thief.getX(), thief.getY(), sharedResources, analytics);
                    int index = characters.indexOf(character);
                    characters.set(index, newThief);
                    character = newThief;
                } else if (character instanceof Wizard wizard) {
                    Wizard newWizard = new Wizard(wizard.getName(), wizard.getX(), wizard.getY(), sharedResources, analytics);
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
        
        // Wait for all character threads to finish using join()
        System.out.println("⏳ Waiting for all character threads to complete...");
        for (int i = 0; i < characterThreads.size(); i++) {
            Thread thread = characterThreads.get(i);
            try {
                System.out.println("   Waiting for " + thread.getName() + " to finish...");
                thread.join(3000); // Wait up to 3 seconds for each thread
                if (thread.isAlive()) {
                    System.out.println("   ⚠️ " + thread.getName() + " did not finish gracefully, interrupting...");
                    thread.interrupt();
                    thread.join(1000); // Give it 1 more second after interrupt
                } else {
                    System.out.println("   ✅ " + thread.getName() + " completed successfully.");
                }
            } catch (InterruptedException e) {
                System.out.println("   ❌ Interrupted while waiting for " + thread.getName());
                Thread.currentThread().interrupt();
            }
        }
        
        // Wait for monitor thread to finish
        if (monitorThread != null && monitorThread.isAlive()) {
            try {
                System.out.println("   Waiting for monitor thread to finish...");
                monitorThread.join(2000);
                if (monitorThread.isAlive()) {
                    monitorThread.interrupt();
                }
                System.out.println("   ✅ Monitor thread completed.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Stop shared resource generation
        sharedResources.stopResourceGeneration();
        
        // Display final statistics
        displayFinalStats();
        
        System.out.println("\n🧵 All threads have been properly synchronized and closed.");
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
     * Wait for the adventure to complete naturally or by user command
     * Ensures proper synchronization of all threads
     */
    private void waitForAdventureCompletion() {
        System.out.println("\n🕐 Adventure will run for " + (ADVENTURE_DURATION / 1000) + " seconds or until ended manually.");
        
        // Wait for either the game to end naturally or user to quit
        while (gameRunning) {
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // End the adventure and ensure all threads are properly joined
        if (gameRunning) {
            endAdventure();
        }
    }
    
    /**
     * Graceful shutdown method - can be called from shutdown hook
     */
    public void shutdown() {
        if (gameRunning) {
            System.out.println("\n🛑 Shutdown requested - ending adventure gracefully...");
            endAdventure();
        }
    }
    
    /**
     * Display detailed shared resource status
     */
    private void displayDetailedResourceStatus() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏛️ DETAILED SHARED RESOURCE STATUS");
        System.out.println("=".repeat(60));
        
        // Display treasure vault
        System.out.print(sharedResources.viewTreasureVault("SYSTEM"));
        
        // Display trading post
        System.out.print(sharedResources.viewTradingPost("SYSTEM"));
        
        // Display resource summary
        System.out.println(sharedResources.getResourceStatus());
        
        // Display shared inventory
        List<String> sharedInv = sharedResources.viewSharedInventory("SYSTEM");
        System.out.println("📦 Shared Inventory Items: " + sharedInv.size());
        if (!sharedInv.isEmpty()) {
            sharedInv.forEach(item -> System.out.println("   - " + item));
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
    
    /**
     * Display comprehensive game analytics using lambda expressions
     */
    private void displayGameAnalytics() {
        System.out.println(analytics.generateComprehensiveReport());
        
        // Demonstrate lambda expressions for character analysis
        System.out.println("🧑‍💻 CHARACTER PERFORMANCE ANALYSIS:");
        System.out.println("=".repeat(50));
        
        characters.stream()
            .filter(GameCharacter::isAlive)
            .sorted((a, b) -> Integer.compare(b.getInventory().size(), a.getInventory().size()))
            .forEach(character -> {
                GameAnalytics.CharacterStats stats = analytics.getCharacterStats(character.getName());
                System.out.printf("%-15s | Battles: %2d/%2d | Items: %2d | Spells: %2d | Win Rate: %5.1f%%\n",
                    character.getName(),
                    stats.battlesWon,
                    stats.battlesWon + stats.battlesLost,
                    stats.itemsCollected,
                    stats.spellsCast,
                    stats.winRate);
            });
        
        System.out.println();
        
        // Show recent events using streams
        List<String> recentEvents = analytics.getRecentEvents(5);
        if (!recentEvents.isEmpty()) {
            System.out.println("🕜 RECENT EVENTS:");
            recentEvents.forEach(event -> System.out.println("   " + event));
            System.out.println();
        }
    }
    
    /**
     * Display specific character analytics
     */
    private void displayCharacterAnalytics(String characterName) {
        // Use lambda to find character
        characters.stream()
            .filter(c -> c.getName().equalsIgnoreCase(characterName))
            .findFirst()
            .ifPresentOrElse(
                character -> {
                    GameAnalytics.CharacterStats stats = analytics.getCharacterStats(character.getName());
                    System.out.println("\n🏆 DETAILED STATS for " + character.getName().toUpperCase());
                    System.out.println("=".repeat(40));
                    System.out.println("Character Type: " + character.getCharacterType());
                    System.out.println("Health: " + character.getHealth() + "/" + character.getMaxHealth());
                    System.out.println("Position: (" + character.getX() + ", " + character.getY() + ")");
                    System.out.println("Status: " + (character.isAlive() ? "Alive" : "Defeated"));
                    System.out.println();
                    
                    System.out.println("🎯 COMBAT STATISTICS:");
                    System.out.println("   Battles Won: " + stats.battlesWon);
                    System.out.println("   Battles Lost: " + stats.battlesLost);
                    System.out.println("   Win Rate: " + String.format("%.1f%%", stats.winRate));
                    System.out.println("   Total Damage Dealt: " + stats.totalDamageDealt);
                    System.out.println("   Total Damage Received: " + stats.totalDamageReceived);
                    System.out.println();
                    
                    System.out.println("🎒 COLLECTION STATISTICS:");
                    System.out.println("   Items Collected: " + stats.itemsCollected);
                    System.out.println("   Current Inventory Size: " + character.getInventory().size());
                    if (!stats.commonItems.isEmpty()) {
                        System.out.println("   Most Common Items: " + String.join(", ", stats.commonItems));
                    }
                    System.out.println();
                    
                    if (stats.spellsCast > 0) {
                        System.out.println("✨ MAGIC STATISTICS:");
                        System.out.println("   Spells Cast: " + stats.spellsCast);
                        System.out.println();
                    }
                    
                    if (!stats.favoriteEnemies.isEmpty()) {
                        System.out.println("⚔️ FAVORITE OPPONENTS:");
                        stats.favoriteEnemies.forEach(enemy -> System.out.println("   - " + enemy));
                        System.out.println();
                    }
                },
                () -> System.out.println("❓ Character '" + characterName + "' not found. Available characters: " + 
                    characters.stream().map(GameCharacter::getName).collect(java.util.stream.Collectors.joining(", ")))
            );
    }
    
    /**
     * Demonstrate advanced lambda operations on character data
     */
    private void demonstrateAdvancedAnalytics() {
        System.out.println("🔬 ADVANCED ANALYTICS DEMONSTRATION:");
        System.out.println("=".repeat(50));
        
        // Find character with highest health percentage using streams
        characters.stream()
            .filter(GameCharacter::isAlive)
            .max(java.util.Comparator.comparingDouble(c -> (double) c.getHealth() / c.getMaxHealth()))
            .ifPresent(c -> System.out.println("Healthiest Character: " + c.getName() + 
                " (" + String.format("%.1f%%", (double) c.getHealth() / c.getMaxHealth() * 100) + ")"));
        
        // Calculate average inventory size using streams
        double avgInventorySize = characters.stream()
            .mapToInt(c -> c.getInventory().size())
            .average()
            .orElse(0.0);
        System.out.println("Average Inventory Size: " + String.format("%.1f", avgInventorySize));
        
        // Group characters by type and count using streams
        Map<String, Long> characterTypeCount = characters.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                GameCharacter::getCharacterType, 
                java.util.stream.Collectors.counting()
            ));
        
        System.out.println("Characters by Type:");
        characterTypeCount.forEach((type, count) -> 
            System.out.println("   " + type + ": " + count));
        
        System.out.println();
    }
    
    /**
     * Check if game is running
     */
    public boolean isGameRunning() {
        return gameRunning;
    }
}
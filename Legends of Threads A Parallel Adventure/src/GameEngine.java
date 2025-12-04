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
    private final GameWorld gameWorld;
    private long gameStartTime;
    private int gameRounds;
    // Removed time limit - players can explore indefinitely
    private Thread monitorThread;
    private GameCharacter playerCharacter;
    private volatile boolean waitingForPlayerInput;
    private volatile String playerAction;
    private volatile boolean playerTurn;
    private volatile boolean gameInProgress;
    private static final int TEXT_DELAY_MS = 800; // Delay between text lines for readability
    
    // Cave exploration system
    private CaveExplorer caveExplorer;
    private boolean caveMode = false;
    private boolean gameWon = false;
    private List<GameCharacter> aiCharacters;
    
    public GameEngine() {
        this.characters = new ArrayList<>();
        this.characterThreads = new ArrayList<>();
        this.gameRunning = false;
        this.scanner = new Scanner(System.in);
        this.gameLock = new ReentrantLock();
        this.gameRounds = 0;
        this.sharedResources = new SharedResources();
        this.analytics = new GameAnalytics();
        this.gameWorld = new GameWorld();
        this.playerTurn = false;
        this.gameInProgress = true;
    }
    
    /**
     * Add a delay to make text easier to read
     */
    private void textDelay() {
        try {
            Thread.sleep(TEXT_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Initialize the game world and create characters
     */
    public void initializeGame() {
        System.out.println("===============================================");
        textDelay();
        System.out.println("WELCOME TO LEGENDS OF THREADS");
        textDelay();
        System.out.println("    A Parallel Adventure Awaits!");
        textDelay();
        System.out.println("===============================================\n");
        
        // Create the three main characters with shared resources and analytics
        Knight knight = new Knight("Sir Galahad", 0, 0, sharedResources, analytics, gameWorld);
        Thief thief = new Thief("Shadowstep", 5, 5, sharedResources, analytics, gameWorld);
        Wizard wizard = new Wizard("Arcanum", 10, 10, sharedResources, analytics, gameWorld);
        
        // Add characters to our management lists
        characters.add(knight);
        characters.add(thief);
        characters.add(wizard);
        
        // Set GameEngine reference for turn management
        knight.setGameEngine(this);
        thief.setGameEngine(this);
        wizard.setGameEngine(this);
        
        System.out.println("HEROES ASSEMBLED:");
        characters.forEach(character -> {
            System.out.println("   " + character.toString());
        });
        System.out.println();
        
        // Let player choose which character to control
        selectPlayerCharacter();
    }
    
    /**
     * Let the player choose which character to control
     */
    private void selectPlayerCharacter() {
        System.out.println("\nCHOOSE YOUR HERO TO CONTROL:");
        textDelay();
        System.out.println("1. Sir Galahad (Knight) - Noble warrior with high defense and honor");
        textDelay();
        System.out.println("2. Shadowstep (Thief) - Stealthy rogue with agility and cunning");
        textDelay();
        System.out.println("3. Arcanum (Wizard) - Powerful mage with magic and wisdom");
        textDelay();
        System.out.print("\nEnter your choice (1-3): ");
        
        int choice = -1;
        while (choice < 1 || choice > 3) {
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice < 1 || choice > 3) {
                    System.out.print("Invalid choice. Please enter 1, 2, or 3: ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter 1, 2, or 3: ");
            }
        }
        
        playerCharacter = characters.get(choice - 1);
        playerCharacter.setPlayerControlled(true);
        
        System.out.println("\nYou have chosen to control " + playerCharacter.getName() + " the " + playerCharacter.getCharacterType() + "!");
        System.out.println("The other heroes will fight alongside you as AI party members.\n");
        
        // Prepare AI characters list (all except player character)
        aiCharacters = new ArrayList<>();
        for (GameCharacter character : characters) {
            if (character != playerCharacter) {
                aiCharacters.add(character);
            }
        }
        
        // Display player controls
        displayPlayerControls();
    }
    
    /**
     * Display the available player controls
     */
    private void displayPlayerControls() {
        System.out.println("PLAYER CONTROLS:");
        if (playerCharacter instanceof Knight) {
            System.out.println("  [A]ttack - Engage in combat");
            System.out.println("  [P]atrol - Move and guard area");
            System.out.println("  [Q]uest - Undertake a noble quest");
            System.out.println("  [D]efend - Protect party members");
        } else if (playerCharacter instanceof Thief) {
            System.out.println("  [S]teal - Attempt to steal treasure");
            System.out.println("  [H]ide - Enter stealth mode");
            System.out.println("  [Scout] - Gather information");
            System.out.println("  [E]scape - Evade from danger");
        } else if (playerCharacter instanceof Wizard) {
            System.out.println("  [C]ast - Cast a magical spell");
            System.out.println("  [M]editate - Restore mana");
            System.out.println("  [R]esearch - Study ancient knowledge");
            System.out.println("  [Explore] - Investigate magical phenomena");
        }
        System.out.println("  [Status] - View character and world status");
        System.out.println("  [Wait/Skip] - End your turn and let AI act");
        System.out.println("  [Help] - Show this help again");
        System.out.println("  [Quit] - End the adventure\n");
    }
    
    /**
     * Start all character threads and begin the adventure
     */
    public void startAdventure() {
        // Ask if player wants to explore the cave
        System.out.println("\n=== ADVENTURE CHOICE ===\n");
        System.out.println("You and your party stand before the entrance to the mysterious Crystal Caverns.");
        System.out.println("Legend speaks of ancient treasures and a powerful guardian within...");
        System.out.println();
        System.out.println("1. Enter the Crystal Caverns (Turn-based dungeon crawler)");
        System.out.println("2. Continue the open world adventure (Original gameplay)");
        System.out.print("\nChoose your path (1-2): ");
        
        int pathChoice = -1;
        while (pathChoice < 1 || pathChoice > 2) {
            try {
                pathChoice = Integer.parseInt(scanner.nextLine().trim());
                if (pathChoice < 1 || pathChoice > 2) {
                    System.out.print("Invalid choice. Please enter 1 or 2: ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter 1 or 2: ");
            }
        }
        
        if (pathChoice == 1) {
            // Cave exploration mode - disable background activities
            caveMode = true;
            gameRunning = false; // Stop background threads from printing
            System.out.println("\nEntering turn-based cave exploration mode!");
            System.out.println("Background activities paused for focused exploration.");
            
            // Disable character auto-actions and resource messages for cave mode
            for (GameCharacter character : characters) {
                character.setCaveMode(true);
            }
            sharedResources.setCaveMode(true);
            gameWorld.setCaveMode(true);
            
            caveExplorer = new CaveExplorer(playerCharacter, aiCharacters, scanner);
            gameWon = caveExplorer.exploreCave();
            
            // Re-enable background activities
            for (GameCharacter character : characters) {
                character.setCaveMode(false);
            }
            sharedResources.setCaveMode(false);
            gameWorld.setCaveMode(false);
            
            if (gameWon) {
                System.out.println("\n=== CONGRATULATIONS! YOU HAVE WON THE GAME! ===\n");
                System.out.println("You have conquered the Crystal Caverns and defeated the Ancient Guardian!");
                System.out.println("Your legend will be told for generations to come!");
                System.out.println("\nPress ESC or type 'exit' to end the game.");
                
                // Wait for exit input
                String exitInput;
                do {
                    System.out.print("Enter 'exit' to quit: ");
                    exitInput = scanner.nextLine().toLowerCase().trim();
                } while (!exitInput.equals("exit") && !exitInput.equals("esc"));
                
                System.out.println("\nThank you for playing Legends of Threads: Crystal Caverns Adventure!");
            } else {
                System.out.println("\nYour adventure ends here... Better luck next time!");
            }
        } else {
            // Original gameplay mode - enable background activities
            gameRunning = true;
            caveMode = false;
            gameStartTime = System.currentTimeMillis();
            
            // Set characters to turn-based mode (similar to cave mode for AI pausing)
            for (GameCharacter character : characters) {
                character.setCaveMode(true); // Use cave mode to enable turn-based behavior
            }
            sharedResources.setCaveMode(true);
            gameWorld.setCaveMode(true);
            
            System.out.println("ADVENTURE BEGINS! All heroes start their quests...\n");
            
            // Create and start threads for each character
            for (GameCharacter character : characters) {
                Thread characterThread = new Thread(character, character.getName() + "Thread");
                characterThreads.add(characterThread);
                characterThread.start();
                System.out.println("Started thread for " + character.getName() + " the " + character.getCharacterType());
            }
            
            System.out.println("\nAll threads are running! Turn-based adventure begins...\n");
            
            // Start the game monitoring thread
            monitorThread = new Thread(this::monitorGame, "GameMonitorThread");
            monitorThread.start();
            
            // Start player control thread
            startPlayerControlThread();
            
            // Begin with player turn
            startPlayerTurn();
            
            // Wait for all threads to complete
            waitForAdventureCompletion();
        }
    }
    
    /**
     * Monitor the game state and provide periodic updates
     */
    private void monitorGame() {
        while (gameRunning) {
            try {
                Thread.sleep(10000); // Update every 10 seconds
                gameRounds++;
                // Removed automatic status display - players can check status manually
                
                // Time limit removed - players can explore indefinitely
                
                // Check if all characters are defeated
                boolean anyAlive = characters.stream().anyMatch(GameCharacter::isAlive);
                if (!anyAlive) {
                    System.out.println("\nAll heroes have fallen! The adventure ends in tragedy...");
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
            System.out.println("ADVENTURE STATUS - Round " + gameRounds + " (Time: " + elapsedTime + "s)");
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
                System.out.println("   " + knight.getName() + " - Armor: " + knight.getArmor() + 
                                 ", Quests: " + knight.getQuestsCompleted());
            } else if (character instanceof Thief thief) {
                System.out.println("   " + thief.getName() + " - Stealth: " + thief.getStealth() + 
                                 ", Items Stolen: " + thief.getItemsStolen());
            } else if (character instanceof Wizard wizard) {
                System.out.println("   " + wizard.getName() + " - Mana: " + wizard.getMana() + "/" + 
                                 wizard.getMaxMana() + ", Spells Cast: " + wizard.getSpellsCast());
            }
        }
    }
    
    /**
     * Start player's turn
     */
    private void startPlayerTurn() {
        playerTurn = true;
        System.out.println("\n=== YOUR TURN ===");
        
        // Provide contextual description of current situation
        describeCurrentSituation();
        
        // Show numbered menu choices
        showPlayerChoiceMenu();
    }
    
    public boolean isPlayerTurn() {
        return playerTurn;
    }
    
    /**
     * End player's turn and start AI turns
     */
    public void endPlayerTurn() {
        playerTurn = false;
        System.out.println("\n=== AI TURN ===");
        System.out.println("AI characters are now acting...\n");
        
        // Temporarily resume AI activity for their turns
        for (GameCharacter character : characters) {
            if (character != playerCharacter && character.isAlive()) {
                // Allow each AI character to take one action
                character.resumeForOneTurn();
            }
        }
        
        // Let AI characters complete their actions
        try {
            Thread.sleep(2000); // 2 seconds for AI actions
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Pause AI characters again
        for (GameCharacter character : characters) {
            if (character != playerCharacter) {
                character.pauseForPlayerTurn();
            }
        }
        
        if (gameRunning) {
            startPlayerTurn();
        }
    }
    
    /**
     * Start the player control thread
     */
    private void startPlayerControlThread() {
        Thread playerThread = new Thread(() -> {
            System.out.println("\nYou now control " + playerCharacter.getName() + "! This is a turn-based adventure.");
            System.out.println("AI characters will pause during your turn, then take their turns.\n");
            
            while (gameRunning) {
                try {
                    // Only accept input during player turn
                    if (playerTurn) {
                        System.out.print("\nChoose your action (1-10): ");
                        String input = scanner.nextLine().trim();
                        
                        if (input.isEmpty()) {
                            System.out.println(playerCharacter.getName() + " waits for your decision...");
                            continue;
                        }
                        
                        handlePlayerChoice(input);
                    } else {
                        // During AI turn, just wait
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    if (gameRunning) {
                        System.out.println("Error reading input: " + e.getMessage());
                    }
                }
            }
        }, "PlayerControlThread");
        
        playerThread.start();
    }
    
    /**
     * Handle user input during the game (legacy method - now replaced by player control)
     */
    private void handleUserInteraction() {
        System.out.println("GAME COMMANDS:");
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
                    System.out.println("User requested to end the adventure...");
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
                        System.out.println("Unknown command. Type 'quit' to end the adventure.");
                    }
            }
        }
    }
    
    /**
     * Handle world interaction for player character
     */
    private void handleWorldInteraction() {
        System.out.println("\n" + playerCharacter.getName() + " looks for something to interact with...");
        
        // Check for nearby characters
        boolean foundInteraction = false;
        for (GameCharacter other : characters) {
            if (other != playerCharacter && other.isAlive() && playerCharacter.distanceTo(other) < 3.0) {
                System.out.println(playerCharacter.getName() + " interacts with " + other.getName() + "!");
                playerCharacter.interact(other);
                other.interact(playerCharacter);
                foundInteraction = true;
                break;
            }
        }
        
        if (!foundInteraction) {
            // Interact with the world environment
            gameWorld.handleCharacterAction(playerCharacter.getName(), "explore", "searches the surrounding area");
        }
    }
    
    /**
     * Handle exploration with rich, immersive descriptions
     */
    private void handleExploration() {
        System.out.println("\n🔍 " + playerCharacter.getName() + " begins a thorough exploration of the area...");
        textDelay();
        
        // Describe the exploration process based on character type
        if (playerCharacter instanceof Knight) {
            System.out.println("   With noble purpose, you methodically search for signs of danger or injustice.");
            textDelay();
            System.out.println("   Your trained eye scans for anything that might require a knight's attention.");
        } else if (playerCharacter instanceof Thief) {
            System.out.println("   Moving with silent grace, you investigate every shadow and hidden corner.");
            textDelay();
            System.out.println("   Your keen senses alert you to the smallest details others might miss.");
        } else if (playerCharacter instanceof Wizard) {
            System.out.println("   You extend your magical awareness, feeling for arcane energies and mysteries.");
            textDelay();
            System.out.println("   Ancient knowledge guides your search through this mystical realm.");
        }
        textDelay();
        
        // Move to a new random location during exploration
        int oldX = playerCharacter.getX();
        int oldY = playerCharacter.getY();
        playerCharacter.moveRandomly();
        
        System.out.println("   Your exploration leads you from (" + oldX + ", " + oldY + ") to (" + 
                          playerCharacter.getX() + ", " + playerCharacter.getY() + ").");
        textDelay();
        
        // Check for discoveries with detailed descriptions
        double discoveryChance = 0.4; // Base 40% chance
        
        // Character-specific bonuses
        if (playerCharacter instanceof Thief) {
            discoveryChance += 0.2; // Thieves are better at finding things
        } else if (playerCharacter instanceof Wizard) {
            discoveryChance += 0.1; // Wizards can sense magical items
        }
        
        if (Math.random() < discoveryChance) {
            System.out.println("\n✨ Your careful search pays off!");
            textDelay();
            
            String[] discoveryTypes = {"Ancient Ruins", "Hidden Treasure", "Mysterious Portal", 
                                     "Sacred Grove", "Abandoned Camp", "Crystal Formation", 
                                     "Forgotten Shrine", "Secret Cache", "Magical Spring"};
            String discovery = discoveryTypes[(int)(Math.random() * discoveryTypes.length)];
            
            // Rich descriptions for each discovery type
            describeDiscovery(discovery);
            
            // Add item to inventory based on discovery
            addDiscoveryReward(discovery);
            
            gameWorld.handleCharacterAction(playerCharacter.getName(), "discover", "found " + discovery);
        } else {
            System.out.println("\n🌫️ Despite your thorough search, this area reveals no immediate secrets.");
            textDelay();
            System.out.println("   Sometimes the journey itself is more valuable than the destination.");
            textDelay();
            System.out.println("   You gain experience and knowledge from the exploration nonetheless.");
        }
    }
    
    /**
     * Provide rich descriptions for different types of discoveries
     */
    private void describeDiscovery(String discovery) {
        switch (discovery) {
            case "Ancient Ruins" -> {
                System.out.println("🏛️  You stumble upon crumbling stone structures, overgrown with vines.");
                textDelay();
                System.out.println("   Weathered carvings tell stories of a civilization lost to time.");
                textDelay();
                System.out.println("   The air here feels heavy with history and forgotten memories.");
            }
            case "Hidden Treasure" -> {
                System.out.println("💰 Half-buried beneath fallen leaves, a small chest catches your eye!");
                textDelay();
                System.out.println("   The lock has long since rusted away, revealing glinting contents within.");
                textDelay();
                System.out.println("   Fortune favors the bold - and the observant!");
            }
            case "Mysterious Portal" -> {
                System.out.println("🌀 Reality seems to bend and shimmer in a perfect circle before you.");
                textDelay();
                System.out.println("   Strange energies ripple through the air, showing glimpses of other realms.");
                textDelay();
                System.out.println("   This gateway holds secrets beyond mortal understanding.");
            }
            case "Sacred Grove" -> {
                System.out.println("🌳 Ancient trees form a perfect circle, their branches intertwining overhead.");
                textDelay();
                System.out.println("   Soft light filters through leaves that seem to glow with inner radiance.");
                textDelay();
                System.out.println("   This place pulses with natural magic and peaceful energy.");
            }
            case "Magical Spring" -> {
                System.out.println("💧 Crystal-clear water bubbles up from an ornate stone fountain.");
                textDelay();
                System.out.println("   The water glows with a faint blue light and feels warm to the touch.");
                textDelay();
                System.out.println("   Legends speak of springs like this having miraculous healing properties.");
            }
            default -> {
                System.out.println("🔍 You discover something remarkable: " + discovery + "!");
                textDelay();
                System.out.println("   This find fills you with wonder and curiosity about its origins.");
                textDelay();
                System.out.println("   Such discoveries make every adventure worthwhile.");
            }
        }
    }
    
    /**
     * Add appropriate rewards based on the type of discovery
     */
    private void addDiscoveryReward(String discovery) {
        switch (discovery) {
            case "Hidden Treasure" -> {
                playerCharacter.addToInventory("Ancient Gold Coin");
                System.out.println("   └─ You acquire: Ancient Gold Coin");
            }
            case "Sacred Grove" -> {
                playerCharacter.addToInventory("Blessed Leaf");
                System.out.println("   └─ You acquire: Blessed Leaf");
            }
            case "Crystal Formation" -> {
                playerCharacter.addToInventory("Crystal Shard");
                System.out.println("   └─ You acquire: Crystal Shard");
            }
            case "Magical Spring" -> {
                // Heal the player
                int healAmount = 20;
                playerCharacter.heal(healAmount);
                System.out.println("   └─ The spring's waters restore " + healAmount + " health!");
            }
            case "Ancient Ruins" -> {
                playerCharacter.addToInventory("Ancient Rune Stone");
                System.out.println("   └─ You acquire: Ancient Rune Stone");
            }
            default -> {
                playerCharacter.addToInventory("Mysterious Artifact");
                System.out.println("   └─ You acquire: Mysterious Artifact");
            }
        }
        textDelay();
    }
    
    /**
     * Handle trading for player character
     */
    private void handleTrading() {
        System.out.println("\n" + playerCharacter.getName() + " seeks out trading opportunities...");
        textDelay();
        
        // Check trading post
        sharedResources.checkTradingPost(playerCharacter.getName());
        textDelay();
        
        // Attempt to trade if items are available
        if (Math.random() < 0.5) { // 50% chance of successful trade
            String[] items = {"Health Potion", "Magic Scroll", "Iron Sword", "Silver Coin", "Ancient Artifact"};
            String item = items[(int)(Math.random() * items.length)];
            
            if (sharedResources.tryTradeForItem(playerCharacter.getName(), item)) {
                playerCharacter.addToInventory(item);
                System.out.println(playerCharacter.getName() + " successfully traded for " + item + "!");
                textDelay();
            }
        } else {
            System.out.println("No suitable trades are available at this time.");
            textDelay();
        }
    }
    
    /**
     * Describe the current situation with rich detail and context
     */
    private void describeCurrentSituation() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    CURRENT SCENE");
        System.out.println("=".repeat(60));
        textDelay();
        
        // Get environment details
        String[] environments = {"ancient forest clearing", "misty mountain path", "abandoned village square", 
                               "crystal cave entrance", "mystical shrine", "crossroads junction", 
                               "ruined watchtower", "enchanted grove", "desert oasis"};
        String currentEnv = environments[Math.abs((playerCharacter.getX() + playerCharacter.getY()) % environments.length)];
        
        // Rich environmental description based on location
        describeEnvironmentInDetail(currentEnv);
        textDelay();
        
        // Character's current state
        System.out.println("\n🧙 Your Status:");
        textDelay();
        System.out.println("   " + playerCharacter.getName() + " the " + playerCharacter.getCharacterType() + " stands ready for action.");
        textDelay();
        System.out.println("   Health: " + playerCharacter.getHealth() + "/" + playerCharacter.getMaxHealth() + " | Items: " + playerCharacter.getInventory().size());
        textDelay();
        
        // Check for nearby characters with detailed descriptions
        boolean foundNearbyCharacter = false;
        for (GameCharacter other : characters) {
            if (other != playerCharacter && other.isAlive() && playerCharacter.distanceTo(other) < 5.0) {
                if (!foundNearbyCharacter) {
                    System.out.println("\n👥 Nearby Companions:");
                    textDelay();
                    foundNearbyCharacter = true;
                }
                describeNearbyCharacter(other);
                textDelay();
            }
        }
        
        if (!foundNearbyCharacter) {
            System.out.println("\n🌟 You stand alone in this mystical realm, the silence broken only by");
            textDelay();
            System.out.println("   the whisper of wind and distant echoes of adventure.");
            textDelay();
        }
        
        // Show available interactions and opportunities
        describeAvailableOpportunities();
        
        System.out.println("=".repeat(60));
    }
    
    /**
     * Describe the environment in rich, immersive detail
     */
    private void describeEnvironmentInDetail(String environment) {
        System.out.println("🌍 Location: " + environment.substring(0, 1).toUpperCase() + environment.substring(1));
        textDelay();
        
        // Detailed descriptions based on environment type
        switch (environment) {
            case "ancient forest clearing" -> {
                System.out.println("   Towering oak and pine trees form a natural cathedral around you, their");
                textDelay();
                System.out.println("   gnarled branches filtering golden sunlight into dancing patterns on the");
                textDelay();
                System.out.println("   moss-covered ground. Ancient stone circles hint at forgotten rituals.");
            }
            case "misty mountain path" -> {
                System.out.println("   A narrow trail winds along the mountainside, shrouded in ethereal mist.");
                textDelay();
                System.out.println("   Rocky outcrops jut from the fog, and you hear the distant cry of eagles");
                textDelay();
                System.out.println("   soaring through the clouds above. The air is thin and crisp.");
            }
            case "abandoned village square" -> {
                System.out.println("   Crumbling cobblestones stretch before a weathered fountain. Empty windows");
                textDelay();
                System.out.println("   of abandoned houses stare like hollow eyes, while ivy reclaims the walls.");
                textDelay();
                System.out.println("   A sense of lost stories and forgotten lives permeates the air.");
            }
            case "crystal cave entrance" -> {
                System.out.println("   Glittering formations of amethyst and quartz catch the light, casting");
                textDelay();
                System.out.println("   rainbow reflections on the cave walls. The entrance yawns darkly ahead,");
                textDelay();
                System.out.println("   promising both wonder and danger in its mysterious depths.");
            }
            case "mystical shrine" -> {
                System.out.println("   An ornate altar of white marble stands surrounded by floating runes that");
                textDelay();
                System.out.println("   pulse with soft blue light. The very air hums with magical energy, and");
                textDelay();
                System.out.println("   you feel the presence of ancient powers watching over this sacred place.");
            }
            default -> {
                System.out.println("   The landscape stretches before you, filled with mystery and possibility.");
                textDelay();
                System.out.println("   Every shadow could hide adventure, every sound might herald discovery.");
            }
        }
    }
    
    /**
     * Describe nearby characters with personality and current activity
     */
    private void describeNearbyCharacter(GameCharacter other) {
        double distance = playerCharacter.distanceTo(other);
        String proximity = distance < 2.0 ? "stands close by" : distance < 3.5 ? "is nearby" : "can be seen in the distance";
        
        System.out.println("   • " + other.getName() + " the " + other.getCharacterType() + " " + proximity + ".");
        textDelay();
        
        // Character-specific descriptions based on their type and current state
        if (other instanceof Knight) {
            System.out.println("     Their armor gleams in the light, sword at ready. They scan the horizon");
            textDelay();
            System.out.println("     with vigilant eyes, ever watchful for threats or quests to undertake.");
        } else if (other instanceof Thief) {
            System.out.println("     Moving with fluid grace, they seem to blend with the shadows. Their keen");
            textDelay();
            System.out.println("     eyes dart about, noting every detail and potential opportunity.");
        } else if (other instanceof Wizard) {
            System.out.println("     Arcane symbols shimmer faintly around them as they study the magical");
            textDelay();
            System.out.println("     currents flowing through this realm. Ancient wisdom gleams in their eyes.");
        }
    }
    
    /**
     * Describe available opportunities and what the player can actually do right now
     */
    private void describeAvailableOpportunities() {
        System.out.println("\n✨ Current Opportunities:");
        textDelay();
        
        // Check what's actually available to do
        boolean hasNearbyCharacters = characters.stream()
            .anyMatch(c -> c != playerCharacter && c.isAlive() && playerCharacter.distanceTo(c) < 5.0);
        
        boolean hasItems = !playerCharacter.getInventory().isEmpty();
        boolean lowHealth = playerCharacter.getHealth() < playerCharacter.getMaxHealth() * 0.7;
        
        // Contextual opportunities
        if (hasNearbyCharacters) {
            System.out.println("   🤝 You could approach your companions for conversation or coordination.");
            textDelay();
        }
        
        System.out.println("   🔍 The area beckons to be explored - secrets may await discovery.");
        textDelay();
        
        if (Math.random() < 0.3) {
            System.out.println("   💰 A trading post's banner flutters in the distance - commerce awaits.");
            textDelay();
        }
        
        if (hasItems) {
            System.out.println("   🎒 Your inventory contains items that might prove useful here.");
            textDelay();
        }
        
        if (lowHealth) {
            System.out.println("   💔 Your wounds need attention - seek healing or rest carefully.");
            textDelay();
        }
        
        // Character-specific opportunities
        if (playerCharacter instanceof Knight) {
            System.out.println("   ⚔️  Your noble heart senses quests and worthy causes in this realm.");
        } else if (playerCharacter instanceof Thief) {
            System.out.println("   🦝 Your keen senses detect hidden paths and valuable opportunities.");
        } else if (playerCharacter instanceof Wizard) {
            System.out.println("   🔮 Magical energies swirl around you, ready to be harnessed and studied.");
        }
        textDelay();
    }
    
    /**
     * Show contextual choice menu based on current situation
     */
    private void showPlayerChoiceMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("          WHAT WOULD YOU LIKE TO DO?");
        System.out.println("=".repeat(50));
        textDelay();
        
        // Check current situation for contextual options
        boolean hasNearbyCharacters = characters.stream()
            .anyMatch(c -> c != playerCharacter && c.isAlive() && playerCharacter.distanceTo(c) < 5.0);
        boolean hasItems = !playerCharacter.getInventory().isEmpty();
        boolean lowHealth = playerCharacter.getHealth() < playerCharacter.getMaxHealth() * 0.7;
        
        // Contextual exploration option
        System.out.println("1. 🔍 Search this area thoroughly for hidden secrets");
        textDelay();
        System.out.println("   └─ Investigate surroundings, seek treasures or clues");
        textDelay();
        
        // Movement with environmental context
        System.out.println("2. 🚶 Journey to a new location in the realm");
        textDelay();
        System.out.println("   └─ Leave this area and discover new landscapes");
        textDelay();
        
        // Contextual interaction
        if (hasNearbyCharacters) {
            System.out.println("3. 👋 Approach and converse with your companions");
            textDelay();
            System.out.println("   └─ Coordinate plans, share stories, or seek advice");
        } else {
            System.out.println("3. 🌀 Commune with the mystical energies here");
            textDelay();
            System.out.println("   └─ Meditate and attune yourself to this place");
        }
        textDelay();
        
        // Trading with context
        if (Math.random() < 0.4) {
            System.out.println("4. 🏪 Seek out merchants and trading opportunities");
            textDelay();
            System.out.println("   └─ A traveling trader's banner is visible nearby");
        } else {
            System.out.println("4. 🎒 Organize and manage your possessions");
            textDelay();
            System.out.println("   └─ Sort inventory and prepare equipment");
        }
        textDelay();
        
        // Character-specific contextual action (slot 5)
        if (playerCharacter instanceof Knight) {
            System.out.println("5. ⚔️  Seek righteous quests and noble challenges");
            textDelay();
            System.out.println("   └─ Your knightly honor calls for heroic deeds");
        } else if (playerCharacter instanceof Thief) {
            System.out.println("5. 🗡️  Employ stealth and cunning abilities");
            textDelay();
            System.out.println("   └─ Scout ahead, hide, or search for valuables");
        } else if (playerCharacter instanceof Wizard) {
            System.out.println("5. ✨ Channel arcane powers and mystical knowledge");
            textDelay();
            System.out.println("   └─ Cast spells, research magic, or divine wisdom");
        }
        textDelay();
        
        // Inventory with context
        if (hasItems) {
            System.out.println("6. 📦 Review your collected treasures and gear");
            textDelay();
            System.out.println("   └─ You carry " + playerCharacter.getInventory().size() + " item(s) of interest");
        } else {
            System.out.println("6. 📦 Check your empty pack and current condition");
            textDelay();
            System.out.println("   └─ Assess your readiness for adventure");
        }
        textDelay();
        
        // Status with health context
        if (lowHealth) {
            System.out.println("7. 🩹 Assess your wounds and current condition");
            textDelay();
            System.out.println("   └─ Your health needs attention (" + playerCharacter.getHealth() + "/" + playerCharacter.getMaxHealth() + ")");
        } else {
            System.out.println("7. 📊 Review your status and party information");
            textDelay();
            System.out.println("   └─ Check detailed statistics and party status");
        }
        textDelay();
        
        System.out.println("8. 👁️  Carefully observe your immediate surroundings");
        textDelay();
        System.out.println("   └─ Take a moment to notice all environmental details");
        textDelay();
        
        System.out.println("9. ⏸️  Wait and observe (let companions act first)");
        textDelay();
        System.out.println("   └─ Sometimes patience reveals new opportunities");
        textDelay();
        
        System.out.println("10. 🚪 End this adventure and return to reality");
        textDelay();
        System.out.println("    └─ Your legend will be remembered in the realm");
        textDelay();
        
        System.out.println("=".repeat(50));
    }
    
    /**
     * Show player inventory
     */
    private void showPlayerInventory() {
        System.out.println("\n=== " + playerCharacter.getName().toUpperCase() + "'S INVENTORY ===");
        List<String> inventory = playerCharacter.getInventory();
        if (inventory.isEmpty()) {
            System.out.println("Your inventory is empty.");
        } else {
            System.out.println("Items carried:");
            for (int i = 0; i < inventory.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + inventory.get(i));
            }
        }
        System.out.println("Health: " + playerCharacter.getHealth() + "/" + playerCharacter.getMaxHealth());
        System.out.println("===========================\n");
    }
    
    /**
     * Handle player movement
     */
    private void handleMovement() {
        System.out.println("\n" + playerCharacter.getName() + " decides to travel to a new location...");
        
        // Move the character
        playerCharacter.moveRandomly();
        
        // Describe the new location
        String[] locations = {"a hidden valley", "an ancient crossroads", "a mystical clearing", 
                            "a abandoned ruins", "a crystal formation", "a sacred grove", 
                            "a mountain overlook", "a forest glade", "a desert shrine"};
        String newLocation = locations[(int)(Math.random() * locations.length)];
        System.out.println(playerCharacter.getName() + " arrives at " + newLocation + ".");
        
        // Chance of random encounter
        if (Math.random() < 0.3) {
            String[] encounters = {"a group of traveling merchants", "signs of ancient magic", 
                                 "a mysterious artifact", "traces of other adventurers",
                                 "a hidden passage", "an old campsite"};
            String encounter = encounters[(int)(Math.random() * encounters.length)];
            System.out.println("During your travels, you encounter " + encounter + "!");
            gameWorld.handleCharacterAction(playerCharacter.getName(), "encounter", "encountered " + encounter + " while traveling");
        }
    }
    
    /**
     * Display detailed status for everything
     */
    private void displayDetailedStatus() {
        System.out.println("\n=== DETAILED STATUS ===");
        System.out.println("=== PARTY STATUS ===");
        for (GameCharacter character : characters) {
            System.out.println("   " + character.getName() + " - Health: " + character.getHealth() + 
                             "/" + character.getMaxHealth() + ", Position: (" + character.getX() + 
                             ", " + character.getY() + "), Status: " + 
                             (character.isAlive() ? "Alive" : "Defeated"));
        }
        System.out.println("======================\n");
    }

    /**
     * Handle player's numbered choice selection
     */
    private void handlePlayerChoice(String input) {
        try {
            int choice = Integer.parseInt(input);
            
            switch (choice) {
                case 1 -> {
                    System.out.println(playerCharacter.getName() + " explores the surrounding area...");
                    handleExploration();
                    endPlayerTurn();
                }
                case 2 -> {
                    System.out.println(playerCharacter.getName() + " decides to travel somewhere new...");
                    handleMovement();
                    endPlayerTurn();
                }
                case 3 -> {
                    System.out.println(playerCharacter.getName() + " looks for someone or something to interact with...");
                    handleWorldInteraction();
                    endPlayerTurn();
                }
                case 4 -> {
                    System.out.println(playerCharacter.getName() + " heads to the trading post...");
                    handleTrading();
                    endPlayerTurn();
                }
                case 5 -> {
                    // Character-specific action
                    handleCharacterSpecificAction();
                    endPlayerTurn();
                }
                case 6 -> {
                    showPlayerInventory();
                    // Don't end turn for info commands
                }
                case 7 -> {
                    displayDetailedStatus();
                    displayWorldStatus();
                    // Don't end turn for info commands
                }
                case 8 -> {
                    describeCurrentSituation();
                    showPlayerChoiceMenu();
                    // Don't end turn for look command
                }
                case 9 -> {
                    System.out.println(playerCharacter.getName() + " waits and observes the world...");
                    endPlayerTurn();
                }
                case 10 -> {
                    System.out.println("\nEnding adventure...");
                    gameRunning = false;
                }
                default -> {
                    System.out.println("Invalid choice. Please select a number from 1-10.");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a number from 1-10.");
        }
    }
    
    /**
     * Handle character-specific actions based on class
     */
    private void handleCharacterSpecificAction() {
        if (playerCharacter instanceof Knight) {
            System.out.println(playerCharacter.getName() + " seeks out challenges worthy of a noble knight...");
            // Randomly choose between combat, quest, or patrol
            String[] knightActions = {"combat", "quest", "patrol"};
            String action = knightActions[(int)(Math.random() * knightActions.length)];
            playerCharacter.executePlayerAction(action);
            System.out.println(playerCharacter.getName() + " engages in " + action + "!");
        } else if (playerCharacter instanceof Thief) {
            System.out.println(playerCharacter.getName() + " uses their stealthy skills...");
            String[] thiefActions = {"hide", "scout", "steal"};
            String action = thiefActions[(int)(Math.random() * thiefActions.length)];
            playerCharacter.executePlayerAction(action);
            System.out.println(playerCharacter.getName() + " attempts to " + action + "!");
        } else if (playerCharacter instanceof Wizard) {
            System.out.println(playerCharacter.getName() + " channels their magical abilities...");
            String[] wizardActions = {"cast", "meditate", "research"};
            String action = wizardActions[(int)(Math.random() * wizardActions.length)];
            playerCharacter.executePlayerAction(action);
            System.out.println(playerCharacter.getName() + " begins to " + action + "!");
        }
    }

    /**
     * Display current world status
     */
    private void displayWorldStatus() {
        System.out.println("\n=== WORLD STATUS ===");
        sharedResources.displayGlobalStatus();
        gameWorld.displayCurrentWorldState();
        System.out.println("=====================\n");
    }

    /**
     * Force interactions between characters
     */
    private void forceCharacterInteractions() {
        System.out.println("\nFORCING CHARACTER INTERACTIONS...\n");
        
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
                    Knight newKnight = new Knight(knight.getName(), knight.getX(), knight.getY(), sharedResources, analytics, gameWorld);
                    newKnight.setPlayerControlled(knight.isPlayerControlled());
                    if (knight.isPlayerControlled()) {
                        playerCharacter = newKnight;
                    }
                    int index = characters.indexOf(character);
                    characters.set(index, newKnight);
                    character = newKnight;
                } else if (character instanceof Thief thief) {
                    Thief newThief = new Thief(thief.getName(), thief.getX(), thief.getY(), sharedResources, analytics, gameWorld);
                    newThief.setPlayerControlled(thief.isPlayerControlled());
                    if (thief.isPlayerControlled()) {
                        playerCharacter = newThief;
                    }
                    int index = characters.indexOf(character);
                    characters.set(index, newThief);
                    character = newThief;
                } else if (character instanceof Wizard wizard) {
                    Wizard newWizard = new Wizard(wizard.getName(), wizard.getX(), wizard.getY(), sharedResources, analytics, gameWorld);
                    newWizard.setPlayerControlled(wizard.isPlayerControlled());
                    if (wizard.isPlayerControlled()) {
                        playerCharacter = newWizard;
                    }
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
        System.out.println("\nENDING THE ADVENTURE...");
        
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
                    System.out.println("   " + thread.getName() + " did not finish gracefully, interrupting...");
                    thread.interrupt();
                    thread.join(1000); // Give it 1 more second after interrupt
                } else {
                    System.out.println("   " + thread.getName() + " completed successfully.");
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
        System.out.println("\n🕐 Adventure will continue indefinitely - type 'quit' or 'exit' to end manually.");
        
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
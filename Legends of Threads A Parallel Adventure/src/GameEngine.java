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
    // Text now waits for player input instead of automatic delays
    private boolean skipTextPrompts = false;
    private boolean instructionsShown = false;
    private volatile boolean displayingText = false; // Prevents choice prompts during text display
    
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
     * Show text scrolling instructions only once at the beginning
     */
    private void showTextInstructions() {
        if (!instructionsShown) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("                    TEXT SCROLLING CONTROLS");
            System.out.println("=".repeat(60));
            System.out.println("  • Press ENTER after each text section to continue");
            System.out.println("  • Type 'SKIP' and press ENTER to enable automatic scrolling");
            System.out.println("  • Enjoy the rich story at your own pace!");
            System.out.println("=".repeat(60));
            System.out.print("\nPress Enter to begin your adventure...");
            
            try {
                String input = scanner.nextLine();
                if (input != null && input.toLowerCase().contains("skip")) {
                    skipTextPrompts = true;
                    System.out.println("\n✓ Auto-scroll enabled. Text will appear automatically.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                // Continue if input fails
            }
            
            instructionsShown = true;
            System.out.println(); // Extra line for spacing
        }
    }

    /**
     * Wait for player to press Enter before continuing (unless skip mode is enabled)
     */
    private void textDelay() {
        if (skipTextPrompts) {
            // Small delay so text doesn't appear instantly
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        
        // Simple, clean Enter prompt
        System.out.print("\n    [Press Enter to continue]");
        try {
            // Use scanner for consistency and to avoid conflicts
            scanner.nextLine();
            
            // Clear both the input line and prompt line
            System.out.print("\033[2A"); // Move up 2 lines
            System.out.print("\033[2K"); // Clear current line
            System.out.print("\033[1B"); // Move down 1 line
            System.out.print("\033[2K"); // Clear this line too
            System.out.print("\033[1A"); // Move back up
        } catch (Exception e) {
            // If input fails, just use a short delay and continue
            try {
                Thread.sleep(800);
                // Still try to clear the prompt
                System.out.print("\033[1A\033[2K\r");
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Initialize the game world and create characters
     */
    public void initializeGame() {
        // Show text scrolling instructions first
        showTextInstructions();
        
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
        displayingText = true; // Prevent choice prompts during text display
        
        System.out.println("\n=== YOUR TURN ===");
        
        // Provide contextual description of current situation
        describeCurrentSituation();
        
        // Show numbered menu choices
        showPlayerChoiceMenu();
        
        displayingText = false; // Allow choice prompts now
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
                    // Only accept input during player turn and not while displaying text
                    if (playerTurn && !displayingText) {
                        System.out.print("\nChoose your action (1-10): ");
                        
                        String input = null;
                        try {
                            input = scanner.nextLine();
                            if (input != null) {
                                input = input.trim();
                            }
                        } catch (Exception inputEx) {
                            // If scanner fails, create a new one
                            scanner = new Scanner(System.in);
                            System.out.println("\nInput refreshed. Please enter your choice: ");
                            try {
                                input = scanner.nextLine().trim();
                            } catch (Exception retryEx) {
                                System.out.println("\nInput unavailable. Ending game.");
                                gameRunning = false;
                                break;
                            }
                        }
                        
                        if (input == null || input.isEmpty()) {
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
                        System.out.println("\nUnexpected error. Ending game gracefully.");
                        gameRunning = false;
                        break;
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
        
        describeMovement(oldX, oldY, playerCharacter.getX(), playerCharacter.getY(), "exploration");
        textDelay();
        
        // Enhanced encounter system - multiple types of discoveries
        double baseChance = 0.7; // Higher chance for encounters
        
        // Character-specific bonuses
        if (playerCharacter instanceof Thief) {
            baseChance += 0.15; // Thieves find more
        } else if (playerCharacter instanceof Wizard) {
            baseChance += 0.1; // Wizards sense magic
        } else if (playerCharacter instanceof Knight) {
            baseChance += 0.05; // Knights attract quests
        }
        
        if (Math.random() < baseChance) {
            // Determine encounter type with balanced distribution
            double encounterRoll = Math.random();
            if (encounterRoll < 0.25) {
                handleCombatEncounter(); // 25% combat
            } else if (encounterRoll < 0.45) {
                handleDiscoveryEncounter(); // 20% treasure/discovery
            } else if (encounterRoll < 0.70) {
                handleNPCEncounter(); // 25% NPCs (includes gifts and quests)
            } else {
                handleQuestEncounter(); // 30% dedicated quest opportunities
            }
        } else {
            // Even when no major encounter, sometimes find small treasures
            if (Math.random() < 0.4) {
                System.out.println("\n🔍 Your thorough search pays off!");
                textDelay();
                String[] minorFinds = {"Old Coins", "Useful Herbs", "Shiny Pebble", "Worn Map Fragment", "Lucky Token"};
                String find = minorFinds[(int)(Math.random() * minorFinds.length)];
                addItemWithDescription(find);
                textDelay();
            } else {
                System.out.println("\n🌫️ The area seems quiet, but you sense adventure nearby.");
                textDelay();
                System.out.println("   Your exploration skills improve from the careful search.");
                textDelay();
            }
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
            case "Scorched Village" -> {
                System.out.println("🔥 Blackened foundations mark where a village once stood proudly.");
                textDelay();
                System.out.println("   Dark flames still flicker among the ruins - Malachar's signature magic.");
                textDelay();
                System.out.println("   A weathered sign reads: 'Millbrook - Population 347' - all gone now.");
            }
            case "Corrupted Temple" -> {
                System.out.println("⛪ A once-holy temple now bears dark symbols carved into its walls.");
                textDelay();
                System.out.println("   The altar is stained black, and shadow-wraiths drift through the nave.");
                textDelay();
                System.out.println("   This was where the high priests made their last stand against Malachar.");
            }
            case "Broken Royal Statue" -> {
                System.out.println("👑 A massive statue lies shattered - King Aldric III, the last true ruler.");
                textDelay();
                System.out.println("   Dark magic has twisted the bronze, and the crown lies cracked at your feet.");
                textDelay();
                System.out.println("   Fresh flowers lay at the base - someone still remembers the old kingdom.");
            }
            case "Shadow-touched Grove" -> {
                System.out.println("🌲 Trees twist unnaturally, their leaves black as midnight.");
                textDelay();
                System.out.println("   The ground is cold and lifeless, infected by Malachar's shadow magic.");
                textDelay();
                System.out.println("   You sense this was once a place of natural beauty, now forever changed.");
            }
            case "Malachar's Monument" -> {
                System.out.println("🗿 A towering obsidian spire pierces the sky, radiating malevolent energy.");
                textDelay();
                System.out.println("   Strange runes pulse with purple light, and the air itself feels heavy here.");
                textDelay();
                System.out.println("   This marks territory claimed by the Dark Wizard - a warning to all.");
            }
            case "Cursed Battlefield" -> {
                System.out.println("⚔️ Ancient weapons jut from cursed earth where heroes made their last stand.");
                textDelay();
                System.out.println("   Ghostly voices whisper warnings about the Dark Wizard's growing power.");
                textDelay();
                System.out.println("   The very ground remembers the day freedom died in this realm.");
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
     * Describe treasure discoveries with rich detail
     */
    private void describeTreasureDiscovery(String treasure) {
        switch (treasure) {
            case "Buried Treasure Chest" -> {
                System.out.println("💰 Your keen eyes spot disturbed earth beneath an old oak tree.");
                textDelay();
                System.out.println("   Digging carefully, you uncover a weathered chest filled with riches!");
            }
            case "Ancient Coin Hoard" -> {
                System.out.println("🪙 Scattered among the ruins, ancient coins glint in the sunlight.");
                textDelay();
                System.out.println("   These currency pieces from a lost empire are worth a fortune!");
            }
            case "Dragon's Scattered Hoard" -> {
                System.out.println("🐉 Precious items lie abandoned, possibly from a dragon's lair.");
                textDelay();
                System.out.println("   The beast may have fled, but its treasure remains for the taking!");
            }
            case "Wizard's Secret Collection" -> {
                System.out.println("✨ Hidden magical implements and enchanted items await discovery.");
                textDelay();
                System.out.println("   A wizard's private collection, abandoned but still valuable!");
            }
            default -> {
                System.out.println("🏆 You've discovered a valuable cache of treasures!");
                textDelay();
                System.out.println("   Fortune smiles upon the bold and the persistent!");
            }
        }
        textDelay();
    }
    
    /**
     * Add appropriate rewards based on the type of discovery
     */
    private void addDiscoveryReward(String discovery) {
        switch (discovery) {
            case "Hidden Treasure" -> {
                addItemWithDescription("Ancient Gold Coin");
            }
            case "Sacred Grove" -> {
                addItemWithDescription("Blessed Leaf");
            }
            case "Crystal Formation" -> {
                addItemWithDescription("Crystal Shard");
            }
            case "Magical Spring" -> {
                // Heal the player
                int healAmount = 20;
                playerCharacter.heal(healAmount);
                System.out.println("   └─ The spring's waters restore " + healAmount + " health!");
            }
            case "Ancient Ruins" -> {
                addItemWithDescription("Ancient Rune Stone");
            }
            default -> {
                addItemWithDescription("Mysterious Artifact");
            }
        }
        textDelay();
    }
    
    /**
     * Handle combat encounters with various enemies
     */
    private void handleCombatEncounter() {
        String[] enemyNames = {"Goblin Raider", "Shadow Wolf", "Rogue Bandit", "Wild Troll", 
                              "Dark Sprite", "Corrupted Bear", "Skeleton Warrior", "Ice Wraith"};
        String enemyName = enemyNames[(int)(Math.random() * enemyNames.length)];
        
        System.out.println("\n⚔️ COMBAT ENCOUNTER!");
        textDelay();
        
        describeCombatEnemy(enemyName);
        
        // Create enemy with stats based on type
        CombatEnemy enemy = createEnemy(enemyName);
        
        System.out.println("\n⚡ Turn-based battle begins!");
        textDelay();
        System.out.println("🎯 " + enemy.name + " - Health: " + enemy.health + "/" + enemy.maxHealth + ", Attack: " + enemy.attack);
        textDelay();
        
        // Turn-based combat loop
        boolean playerTurn = true;
        boolean combatActive = true;
        
        while (combatActive && playerCharacter.isAlive() && enemy.isAlive()) {
            if (playerTurn) {
                combatActive = handlePlayerCombatTurn(enemy);
            } else {
                handleEnemyCombatTurn(enemy);
            }
            
            // Check for combat end conditions
            if (!enemy.isAlive()) {
                handleCombatVictory(enemy);
                combatActive = false;
            } else if (!playerCharacter.isAlive()) {
                handleCombatDefeat(enemy);
                combatActive = false;
            }
            
            playerTurn = !playerTurn; // Switch turns
        }
        
        gameWorld.handleCharacterAction(playerCharacter.getName(), "combat", "battled " + enemyName);
    }
    
    /**
     * Handle NPC encounters with dialogue and interactions
     */
    private void handleNPCEncounter() {
        String[] npcs = {"Wandering Merchant", "Lost Traveler", "Wise Hermit", "Village Elder", 
                        "Mysterious Mage", "Injured Knight", "Forest Ranger", "Ancient Oracle",
                        "Refugee Farmer", "Exiled Noble", "Underground Rebel", "Former Royal Guard"};
        String npc = npcs[(int)(Math.random() * npcs.length)];
        
        System.out.println("\n👤 NPC ENCOUNTER!");
        textDelay();
        
        describeNPCEncounter(npc);
        
        // NPC interaction outcomes - balanced between gifts, quests, treasure hints, and lore
        double interactionRoll = Math.random();
        if (interactionRoll < 0.3) {
            // Helpful NPC - gives useful items
            String[] gifts = {"Healing Herb", "Ancient Map", "Lucky Charm", "Traveler's Ration", "Wisdom Scroll",
                             "Magic Potion", "Silver Coins", "Enchanted Trinket", "Rare Ingredient"};
            String gift = gifts[(int)(Math.random() * gifts.length)];
            System.out.println("\n🎁 The " + npc + " offers you a gift: " + gift);
            textDelay();
            System.out.println("   \"Take this, brave adventurer. May it serve you well!\"");
            textDelay();
            addItemWithDescription(gift);
        } else if (interactionRoll < 0.55) {
            // Quest giver
            System.out.println("\n📜 The " + npc + " has a task for you!");
            textDelay();
            offerQuestFromNPC(npc);
        } else if (interactionRoll < 0.75) {
            // Treasure hint/direction
            String[] treasureHints = {
                "\"I've heard rumors of a hidden treasure vault beneath the ancient ruins.\"",
                "\"The old watchtower holds secrets - check the foundations for hidden chambers.\"",
                "\"Beware the dragon's lair, but know that great treasures lie within.\"",
                "\"The crystal formations hide more than beauty - search carefully.\"",
                "\"In the haunted battlefield, fallen warriors left behind valuable gear.\""
            };
            String hint = treasureHints[(int)(Math.random() * treasureHints.length)];
            System.out.println("\n🗺️ The " + npc + " leans in with valuable information:");
            textDelay();
            System.out.println("   " + hint);
            textDelay();
            
            // Small chance for immediate treasure reward
            if (Math.random() < 0.3) {
                String[] immediateFinds = {"Old Map Fragment", "Treasure Hunter's Note", "Cryptic Riddle", "Ancient Key"};
                String find = immediateFinds[(int)(Math.random() * immediateFinds.length)];
                System.out.println("   \"And here, take this - it might help in your search:\"");
                textDelay();
                addItemWithDescription(find);
            }
        } else {
            // Information/lore
            System.out.println("\n💬 The " + npc + " shares valuable information:");
            textDelay();
            shareNPCLore();
        }
        
        gameWorld.handleCharacterAction(playerCharacter.getName(), "social", "met " + npc);
    }
    
    /**
     * Handle quest encounters - offer quests to the player
     */
    private void handleQuestEncounter() {
        // Create a random quest
        Quest availableQuest = generateRandomQuest();
        
        System.out.println("\n📋 QUEST OPPORTUNITY!");
        textDelay();
        System.out.println("🧙 A mysterious figure approaches with urgent news...");
        textDelay();
        
        // Present the quest
        System.out.println("\n✨ Quest Available: " + availableQuest.title);
        textDelay();
        System.out.println("📖 " + availableQuest.description);
        textDelay();
        System.out.println("🎯 Objective: " + availableQuest.objective);
        textDelay();
        System.out.println("🏆 Reward: " + availableQuest.reward);
        textDelay();
        
        // Ask if player wants to accept
        System.out.println("\nDo you accept this quest?");
        textDelay();
        System.out.println("1. ✅ Yes, I accept this quest!");
        textDelay();
        System.out.println("2. ❌ No, perhaps another time.");
        textDelay();
        
        displayingText = true;
        System.out.print("Your choice (1-2): ");
        
        try {
            String input = scanner.nextLine();
            displayingText = false;
            int choice = Integer.parseInt(input);
            
            if (choice == 1) {
                acceptQuest(availableQuest);
            } else {
                System.out.println("\n👋 The figure nods understandingly and disappears into the mist.");
                textDelay();
                System.out.println("   Perhaps this quest will find another hero...");
                textDelay();
            }
        } catch (NumberFormatException e) {
            displayingText = false;
            System.out.println("\n❓ Your hesitation is taken as a refusal.");
            textDelay();
        }
    }
    
    /**
     * Handle traditional discovery encounters
     */
    private void handleDiscoveryEncounter() {
        System.out.println("\n✨ DISCOVERY!");
        textDelay();
        
        String[] discoveryTypes = {"Ancient Ruins", "Hidden Treasure", "Mysterious Portal", 
                                 "Sacred Grove", "Abandoned Camp", "Crystal Formation", 
                                 "Forgotten Shrine", "Secret Cache", "Magical Spring", 
                                 "Underground Cavern", "Floating Island", "Time Rift",
                                 "Scorched Village", "Corrupted Temple", "Broken Royal Statue", 
                                 "Shadow-touched Grove", "Malachar's Monument", "Cursed Battlefield"};
        String discovery = discoveryTypes[(int)(Math.random() * discoveryTypes.length)];
        
        describeDiscovery(discovery);
        addDiscoveryReward(discovery);
        
        gameWorld.handleCharacterAction(playerCharacter.getName(), "discover", "found " + discovery);
        updateQuestProgress("discovery", "found " + discovery);
    }
    
    /**
     * Describe combat enemies with vivid detail
     */
    private void describeCombatEnemy(String enemy) {
        switch (enemy) {
            case "Goblin Raider" -> {
                System.out.println("🏹 A snarling goblin emerges from the shadows, wielding crude weapons!");
                textDelay();
                System.out.println("   Its yellow eyes gleam with malice as it circles you menacingly.");
            }
            case "Shadow Wolf" -> {
                System.out.println("🐺 A massive wolf materializes from the darkness, its fur crackling with dark energy!");
                textDelay();
                System.out.println("   Ethereal mist swirls around its form as it bares supernatural fangs.");
            }
            case "Rogue Bandit" -> {
                System.out.println("🗡️ A scarred bandit steps from behind a tree, daggers flashing in hand!");
                textDelay();
                System.out.println("   Their weathered face bears the cruel smile of someone who lives by violence.");
            }
            case "Wild Troll" -> {
                System.out.println("👹 An enormous troll crashes through the undergrowth, club in hand!");
                textDelay();
                System.out.println("   Moss and lichens cover its stone-like hide as it roars a challenge.");
            }
            case "Dark Sprite" -> {
                System.out.println("🧚 A malevolent fairy dances in the air, surrounded by crackling dark magic!");
                textDelay();
                System.out.println("   Its once-beautiful form is now twisted by shadow, eyes burning with spite.");
            }
            case "Corrupted Bear" -> {
                System.out.println("🐻 A massive bear rears up on its hind legs, foam dripping from its muzzle!");
                textDelay();
                System.out.println("   Dark veins pulse beneath its matted fur - this beast has been tainted by evil.");
            }
            case "Skeleton Warrior" -> {
                System.out.println("💀 Bones clatter as an undead warrior shambles forward, sword and shield ready!");
                textDelay();
                System.out.println("   Empty sockets burn with unholy fire as it raises its ancient weapons.");
            }
            case "Ice Wraith" -> {
                System.out.println("❄️ A ghostly figure of living frost glides toward you, chilling the air!");
                textDelay();
                System.out.println("   Ice crystals form in its wake, and your breath mists in sudden cold.");
            }
            default -> {
                System.out.println("⚔️ A dangerous " + enemy + " blocks your path!");
                textDelay();
                System.out.println("   You must defend yourself against this hostile creature.");
            }
        }
        textDelay();
    }
    
    /**
     * Describe NPC encounters with story-rich personalities
     */
    private void describeNPCEncounter(String npc) {
        switch (npc) {
            case "Wandering Merchant" -> {
                System.out.println("🎒 A nervous merchant approaches, constantly glancing over his shoulder.");
                textDelay();
                System.out.println("   \"Times are dangerous, friend... but I still have goods to trade.\"");
            }
            case "Wise Hermit" -> {
                System.out.println("🧙 An ancient hermit emerges from shadows, his eyes heavy with sorrow.");
                textDelay();
                System.out.println("   \"Few dare travel these cursed lands anymore...\"");
            }
            case "Lost Traveler" -> {
                System.out.println("😰 A frightened traveler stumbles toward you, clothes torn and dirty.");
                textDelay();
                System.out.println("   \"The roads aren't safe! I've been fleeing for days!\"");
            }
            case "Village Elder" -> {
                System.out.println("👴 An elderly figure approaches with a walking stick, face etched with worry.");
                textDelay();
                System.out.println("   \"Our village... it's not what it once was, stranger.\"");
            }
            case "Refugee Farmer" -> {
                System.out.println("👨‍🌾 A weathered farmer sits by the roadside with meager possessions.");
                textDelay();
                System.out.println("   \"Lost everything to his dark magic... nowhere left to go.\"");
            }
            case "Exiled Noble" -> {
                System.out.println("👑 A once-proud noble in tattered finery looks up with haunted eyes.");
                textDelay();
                System.out.println("   \"My family's lands... taken by that accursed wizard.\"");
            }
            case "Underground Rebel" -> {
                System.out.println("🥷 A hooded figure emerges from concealment, hand on weapon.");
                textDelay();
                System.out.println("   \"You're not one of his spies, are you? These lands need heroes.\"");
            }
            case "Former Royal Guard" -> {
                System.out.println("⚔️ A battle-scarred warrior in broken armor regards you with tired eyes.");
                textDelay();
                System.out.println("   \"I served the crown once... before the dark times began.\"");
            }
            default -> {
                System.out.println("👋 A weary " + npc + " approaches cautiously.");
                textDelay();
                System.out.println("   They seem to carry the weight of dark times upon their shoulders.");
            }
        }
        textDelay();
    }
    
    /**
     * Generate a random quest with specific objectives and rewards
     */
    private Quest generateRandomQuest() {
        String[] questTitles = {
            "The Lost Crown of Eldara", "Shadow Cult Investigation", "The Missing Caravan", 
            "Ancient Relic Recovery", "The Corrupted Grove", "Resistance Supply Run",
            "The Phantom Knight", "Magical Herb Gathering", "Malachar's Stolen Tome",
            "Village Rescue Mission", "The Crystal of Power", "Dark Tower Reconnaissance"
        };
        
        String[] descriptions = {
            "The royal crown was hidden when Malachar conquered the kingdom. Find it to inspire hope.",
            "Shadow cultists serving Malachar are gathering in the old ruins. Stop their ritual.",
            "A merchant caravan carrying refugees has vanished. Find survivors or their fate.",
            "An ancient artifact that could resist Malachar's power has been discovered.",
            "Malachar's dark magic is corrupting a sacred grove. Cleanse the taint from the land.",
            "The resistance needs supplies smuggled to the last free settlements.",
            "A knight's spirit, killed defending against Malachar, haunts the old battlefield.",
            "Gather rare herbs to cure those afflicted by Malachar's plague magic.",
            "Malachar's agents stole a powerful spellbook from the royal library. Retrieve it.",
            "Shadow creatures are attacking a village that refused to bow to Malachar.",
            "One of the Five Crystals of Power has been found. Secure it before Malachar does.",
            "Scout Malachar's Dark Tower for weaknesses, but don't get too close."
        };
        
        String[] objectives = {
            "Defeat 3 enemies in combat and find the Crown item",
            "Win 2 combat encounters against goblin-type enemies",
            "Explore 5 different locations and find Caravan Remains",
            "Discover the Ancient Relic through exploration",
            "Visit the enchanted grove location and cleanse the corruption",
            "Survive combat with a powerful enemy and claim Dragon's Treasure",
            "Win a combat encounter in a haunted battlefield location",
            "Collect 3 Healing Herb items through exploration",
            "Defeat bandits and recover the Stolen Spellbook",
            "Protect the village by winning 3 combat encounters",
            "Find and stabilize the Unstable Crystal",
            "Clear the bandit camp by winning 4 combat encounters"
        };
        
        String[] rewards = {
            "Royal Crown + 200 Gold + Max Health Increase",
            "Goblin Slayer Badge + Combat Bonus",
            "Merchant's Thanks + Rare Trading Goods",
            "Ancient Artifact + Magical Powers",
            "Nature's Blessing + Healing Bonus",
            "Dragon's Treasure + Legendary Items",
            "Knight's Honor + Undead Protection",
            "Village Gratitude + Healing Supplies",
            "Spellbook Knowledge + Magic Bonus",
            "Hero's Medal + Village Protection",
            "Crystal Shard + Reality Manipulation",
            "Bandit's Hoard + Outlaw's Respect"
        };
        
        int index = (int)(Math.random() * questTitles.length);
        return new Quest(questTitles[index], descriptions[index], objectives[index], rewards[index]);
    }
    
    /**
     * Describe quest encounters found in the world
     */
    private void describeQuestEncounter(String questType) {
        switch (questType) {
            case "Ancient Mystery" -> {
                System.out.println("🏛️ You discover ancient stone tablets covered in mysterious runes.");
                textDelay();
                System.out.println("   The symbols seem to pulse with inner light, hinting at forgotten secrets.");
            }
            case "Monster Hunt" -> {
                System.out.println("👹 Fresh tracks and claw marks suggest a dangerous beast lurks nearby.");
                textDelay();
                System.out.println("   Local villages would surely reward whoever eliminates this threat.");
            }
            case "Lost Artifact" -> {
                System.out.println("💎 You sense powerful magical energies emanating from somewhere close.");
                textDelay();
                System.out.println("   An ancient artifact of great value must be hidden in this area.");
            }
            default -> {
                System.out.println("📜 Signs point to an important " + questType + " waiting to be undertaken.");
                textDelay();
                System.out.println("   This could be the adventure you've been seeking.");
            }
        }
        textDelay();
    }
    
    /**
     * Offer a quest from an NPC
     */
    private void offerQuestFromNPC(String npc) {
        Quest npcQuest = generateRandomQuest();
        
        System.out.println("🧙 The " + npc + " speaks urgently:");
        textDelay();
        System.out.println("\"I have need of a brave adventurer like yourself!\"");
        textDelay();
        
        // Present the quest
        System.out.println("\n✨ Quest Offered: " + npcQuest.title);
        textDelay();
        System.out.println("📖 " + npcQuest.description);
        textDelay();
        System.out.println("🎯 Task: " + npcQuest.objective);
        textDelay();
        System.out.println("🏆 Reward: " + npcQuest.reward);
        textDelay();
        
        // Ask if player wants to accept
        System.out.println("\nWill you help?");
        textDelay();
        System.out.println("1. ✅ Yes, I'll take on this quest!");
        textDelay();
        System.out.println("2. ❌ No, I must decline.");
        textDelay();
        
        displayingText = true;
        System.out.print("Your response (1-2): ");
        
        try {
            String input = scanner.nextLine();
            displayingText = false;
            int choice = Integer.parseInt(input);
            
            if (choice == 1) {
                acceptQuest(npcQuest);
                System.out.println("\n😊 The " + npc + " smiles gratefully.");
                textDelay();
                System.out.println("   \"Thank you, brave soul. May fortune favor your quest!\"");
                textDelay();
            } else {
                System.out.println("\n😔 The " + npc + " looks disappointed but understanding.");
                textDelay();
                System.out.println("   \"I understand. Perhaps another time...\"");
                textDelay();
            }
        } catch (NumberFormatException e) {
            displayingText = false;
            System.out.println("\n❓ Your silence is taken as a polite refusal.");
            textDelay();
        }
    }
    
    /**
     * Share story fragments about the Dark Wizard Malachar through NPCs
     */
    private void shareNPCLore() {
        String[] loreEntries = {
            "\"Malachar the Dark wasn't always evil... they say he was once the kingdom's greatest protector.\"",
            "\"The Shadow Spire - his tower - appeared overnight, twisting reality around it.\"",
            "\"Our king and his army marched against the wizard... none returned.\"",
            "\"The old capital burns with eternal black flames. No one can enter anymore.\"",
            "\"He seeks the Five Crystals of Power... already has three, they say.\"",
            "\"My grandmother remembers when the skies were blue, not this sickly green.\"",
            "\"The wizard's servants prowl at night - creatures of shadow and bone.\"",
            "\"There's a prophecy... about a hero who will challenge Malachar's rule.\"",
            "\"The last free city is Havenhold, but even there, people whisper in fear.\"",
            "\"They say he was driven mad by forbidden knowledge from the Void Realm.\"",
            "\"His magic corrupts the very land - crops wither, animals flee.\"",
            "\"The old gods have abandoned us since Malachar broke the Sacred Seals.\""
        };
        
        String lore = loreEntries[(int)(Math.random() * loreEntries.length)];
        System.out.println("   " + lore);
        textDelay();
        System.out.println("   This knowledge might prove useful in your adventures.");
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
        
        // Get environment details - expanded for richer world
        String[] environments = {"ancient forest clearing", "misty mountain path", "abandoned village square", 
                               "crystal cave entrance", "mystical shrine", "crossroads junction", 
                               "ruined watchtower", "enchanted grove", "desert oasis", "floating sky island",
                               "underground cavern", "haunted battlefield", "wizard's tower ruins", "dragon's lair entrance",
                               "moonlit lake shore", "volcanic crater rim", "ice crystal palace", "shadow realm portal",
                               "golden wheat fields", "ancient library ruins", "merchant caravan camp", "bandit hideout",
                               "elven tree city", "dwarven mine entrance", "pegasus nesting grounds", "time rift anomaly"};
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
            case "crossroads junction" -> {
                System.out.println("   Four paths converge here, marked by an ancient stone waymarker covered");
                textDelay();
                System.out.println("   in weathered symbols. Merchants and travelers often pass through, leaving");
                textDelay();
                System.out.println("   traces of their journeys and whispered tales of distant lands.");
            }
            case "ruined watchtower" -> {
                System.out.println("   The skeletal remains of a once-proud tower rise from overgrown rubble.");
                textDelay();
                System.out.println("   Broken battlements and shattered windows speak of long-ago battles, while");
                textDelay();
                System.out.println("   hidden chambers below may still guard forgotten secrets.");
            }
            case "enchanted grove" -> {
                System.out.println("   Silvery trees with luminescent leaves create a sanctuary of natural magic.");
                textDelay();
                System.out.println("   Fairy lights dance between the branches, and the very air shimmers with");
                textDelay();
                System.out.println("   enchantment. This place feels alive with ancient woodland spirits.");
            }
            case "desert oasis" -> {
                System.out.println("   A crystal-clear spring bubbles up from the sandy ground, surrounded by");
                textDelay();
                System.out.println("   date palms and flowering desert blooms. The contrast of life against");
                textDelay();
                System.out.println("   the endless dunes creates a miraculous refuge of peace and sustenance.");
            }
            case "floating sky island" -> {
                System.out.println("   Impossible chunks of earth drift through the clouds around you, connected");
                textDelay();
                System.out.println("   by shimmering bridges of crystallized air. Wind spirits dance among the");
                textDelay();
                System.out.println("   floating gardens, and the view of the world below is breathtaking.");
            }
            case "underground cavern" -> {
                System.out.println("   Vast stone chambers stretch into darkness, carved by ancient waters.");
                textDelay();
                System.out.println("   Stalactites drip endlessly while phosphorescent fungi provide an eerie");
                textDelay();
                System.out.println("   blue glow. Strange echoes hint at deeper passages yet unexplored.");
            }
            case "haunted battlefield" -> {
                System.out.println("   Broken weapons and royal banners lie scattered where the last army made");
                textDelay();
                System.out.println("   their stand against Malachar. Ghostly mists swirl among the remnants,");
                textDelay();
                System.out.println("   and you sense the spirits of fallen heroes still fighting an endless war.");
            }
            case "wizard's tower ruins" -> {
                System.out.println("   The collapsed tower of Archmage Valdris, who once challenged Malachar.");
                textDelay();
                System.out.println("   Dark magic still crackles through the rubble, and corrupted tomes drift");
                textDelay();
                System.out.println("   aimlessly - a warning of what happens to those who oppose the Dark Wizard.");
            }
            case "dragon's lair entrance" -> {
                System.out.println("   A massive cave mouth yawns before you, lined with teeth-like stalactites.");
                textDelay();
                System.out.println("   The acrid smell of sulfur and old fire permeates the air, while treasure");
                textDelay();
                System.out.println("   glints tantalizingly in the depths. This is clearly a dragon's domain.");
            }
            case "moonlit lake shore" -> {
                System.out.println("   Silver moonbeams dance across the mirror-still water, creating paths of");
                textDelay();
                System.out.println("   light that seem almost solid enough to walk upon. Night-blooming flowers");
                textDelay();
                System.out.println("   release their perfume, and you hear the haunting song of water spirits.");
            }
            case "volcanic crater rim" -> {
                System.out.println("   You stand at the edge of a smoldering caldera, feeling the earth's raw");
                textDelay();
                System.out.println("   power beneath your feet. Lava bubbles far below while steam vents hiss");
                textDelay();
                System.out.println("   around you. Fire elementals may dance in these primordial depths.");
            }
            case "ice crystal palace" -> {
                System.out.println("   Towering spires of living ice stretch toward the aurora-painted sky.");
                textDelay();
                System.out.println("   Every surface sparkles with frost-patterns of impossible beauty, and the");
                textDelay();
                System.out.println("   halls echo with the ethereal music of wind through frozen corridors.");
            }
            case "shadow realm portal" -> {
                System.out.println("   A tear in reality itself hovers before you, its edges crackling with dark");
                textDelay();
                System.out.println("   energy. Through it, you glimpse a world of eternal twilight where shadow");
                textDelay();
                System.out.println("   creatures move like living nightmares. The portal pulses hypnotically.");
            }
            case "golden wheat fields" -> {
                System.out.println("   Endless waves of grain ripple in the warm breeze like a golden sea.");
                textDelay();
                System.out.println("   The air is sweet with the promise of harvest, and farmhouses dot the");
                textDelay();
                System.out.println("   horizon. This peaceful land speaks of simple joys and honest work.");
            }
            case "ancient library ruins" -> {
                System.out.println("   Collapsed shelves and scattered tomes create a maze of lost knowledge.");
                textDelay();
                System.out.println("   Some books still glow with preserved magic, their pages turning on their");
                textDelay();
                System.out.println("   own. Wisps of ancient scholars may still haunt these halls of learning.");
            }
            case "merchant caravan camp" -> {
                System.out.println("   Colorful tents and wagon circles create a temporary city of commerce.");
                textDelay();
                System.out.println("   The air fills with exotic spices, foreign languages, and the jingle of");
                textDelay();
                System.out.println("   coins changing hands. Tales from distant lands flow as freely as wine.");
            }
            case "bandit hideout" -> {
                System.out.println("   Hidden among jagged rocks and twisted trees, this secret refuge reeks");
                textDelay();
                System.out.println("   of danger and ill-gotten gains. Stolen goods lie scattered about, and");
                textDelay();
                System.out.println("   you sense watchful eyes tracking your every movement from the shadows.");
            }
            case "elven tree city" -> {
                System.out.println("   Graceful bridges and spiral staircases wind around massive living trees.");
                textDelay();
                System.out.println("   Elven architecture flows seamlessly with nature, creating halls of living");
                textDelay();
                System.out.println("   wood and galleries where art and nature become indistinguishable.");
            }
            case "dwarven mine entrance" -> {
                System.out.println("   Sturdy stone archways frame tunnels that delve deep into the mountain's");
                textDelay();
                System.out.println("   heart. The ring of hammers on anvils echoes from within, and cart tracks");
                textDelay();
                System.out.println("   lead into depths rich with precious metals and ancient dwarven craft.");
            }
            case "pegasus nesting grounds" -> {
                System.out.println("   High mountain meadows where winged horses make their home among the clouds.");
                textDelay();
                System.out.println("   Nests woven from storm-wind and starlight dot the cliff faces, while");
                textDelay();
                System.out.println("   magnificent pegasi soar overhead with rainbow manes streaming behind them.");
            }
            case "time rift anomaly" -> {
                System.out.println("   Reality warps and bends around a fracture in time itself. Past, present,");
                textDelay();
                System.out.println("   and future blend together in impossible ways - you see glimpses of what");
                textDelay();
                System.out.println("   was, is, and might be all occupying the same space simultaneously.");
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
        
        // Show magic meter for Wizards
        if (playerCharacter instanceof Wizard wizard) {
            System.out.println("Magic: " + wizard.getMana() + "/" + wizard.getMaxMana() + " ✨");
        }
        
        // Also show active quests
        System.out.println("\n📋 ACTIVE QUESTS:");
        if (activeQuests.isEmpty()) {
            System.out.println("   No active quests. Look for quest opportunities in the world!");
        } else {
            for (Quest quest : activeQuests) {
                String status = quest.isCompleted() ? "✅ COMPLETE" : "🔄 " + quest.getProgressText();
                System.out.println("   • " + quest.title + " - " + status);
            }
        }
        
        System.out.println("===========================\n");
    }
    
    /**
     * Handle player movement
     */
    private void handleMovement() {
        System.out.println("\n" + playerCharacter.getName() + " decides to travel to a new location...");
        
        // Move the character with enhanced description
        int oldX = playerCharacter.getX();
        int oldY = playerCharacter.getY();
        playerCharacter.moveRandomly();
        
        describeMovement(oldX, oldY, playerCharacter.getX(), playerCharacter.getY(), "travel");
        
        // Describe the new location
        String[] locations = {"a hidden valley", "an ancient crossroads", "a mystical clearing", 
                            "a abandoned ruins", "a crystal formation", "a sacred grove", 
                            "a mountain overlook", "a forest glade", "a desert shrine"};
        String newLocation = locations[(int)(Math.random() * locations.length)];
        System.out.println(playerCharacter.getName() + " arrives at " + newLocation + ".");
        
        // Enhanced travel encounters with balanced variety
        if (Math.random() < 0.45) { // Increased chance for travel encounters
            double travelRoll = Math.random();
            if (travelRoll < 0.35) {
                // Treasure discoveries during travel
                String[] treasures = {"a hidden cache of gold coins", "an abandoned merchant wagon with goods", 
                                    "a mysterious glowing crystal", "ancient artifacts in a ruined shrine",
                                    "a chest buried beneath an old tree", "valuable gems scattered on the ground"};
                String treasure = treasures[(int)(Math.random() * treasures.length)];
                System.out.println("\n💰 TRAVEL DISCOVERY!");
                textDelay();
                System.out.println("During your journey, you discover " + treasure + "!");
                textDelay();
                
                // Add treasure items
                String[] items = {"Gold Coins", "Precious Gems", "Ancient Artifact", "Mysterious Crystal", "Valuable Trinket"};
                String item = items[(int)(Math.random() * items.length)];
                addItemWithDescription(item);
                textDelay();
                updateQuestProgress("discovery", "found " + treasure);
                
            } else if (travelRoll < 0.65) {
                // Combat encounters during travel
                String[] roadThreats = {"highway bandits blocking the path", "a pack of wild wolves",
                                       "roving goblins looking for trouble", "a lone orc warrior",
                                       "corrupted creatures from the dark woods", "desperate thieves"};
                String threat = roadThreats[(int)(Math.random() * roadThreats.length)];
                System.out.println("\n⚔️ TRAVEL DANGER!");
                textDelay();
                System.out.println("Your path is blocked by " + threat + "!");
                textDelay();
                handleCombatEncounter();
                
            } else {
                // Social/quest encounters during travel
                String[] encounters = {"a group of traveling merchants", "a lost pilgrim seeking guidance", 
                                     "a mysterious hooded figure", "fellow adventurers sharing tales",
                                     "a village messenger with urgent news", "a wise hermit offering counsel"};
                String encounter = encounters[(int)(Math.random() * encounters.length)];
                System.out.println("\n👥 TRAVEL ENCOUNTER!");
                textDelay();
                System.out.println("During your travels, you meet " + encounter + "!");
                textDelay();
                
                // 50/50 chance for quest vs treasure/info
                if (Math.random() < 0.5) {
                    offerQuestFromNPC("Mysterious Traveler");
                } else {
                    String[] gifts = {"Travel Rations", "Road Map", "Healing Potion", "Lucky Token", "Traveler's Cloak"};
                    String gift = gifts[(int)(Math.random() * gifts.length)];
                    System.out.println("   └─ They generously share something with you:");
                    textDelay();
                    addItemWithDescription(gift);
                }
            }
            gameWorld.handleCharacterAction(playerCharacter.getName(), "travel_encounter", "encountered something while traveling");
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
                    updateQuestProgress("exploration", "explored area");
                    endPlayerTurn();
                }
                case 2 -> {
                    System.out.println(playerCharacter.getName() + " decides to travel somewhere new...");
                    handleMovement();
                    updateQuestProgress("exploration", "traveled to new location");
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
                    
                    // Special benefit for wizards - meditation restores mana
                    if (playerCharacter instanceof Wizard wizard) {
                        int manaRestore = 8 + (int)(Math.random() * 7); // 8-14 mana
                        wizard.restoreMana(manaRestore);
                        System.out.println("🧘 Your meditation and observation of magical energies restores your power.");
                        textDelay();
                    }
                    
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
    
    // ===== TURN-BASED COMBAT SYSTEM =====
    
    /**
     * Simple enemy class for turn-based combat
     */
    private static class CombatEnemy {
        String name;
        int health;
        int maxHealth;
        int attack;
        String[] abilities;
        
        CombatEnemy(String name, int health, int attack, String[] abilities) {
            this.name = name;
            this.health = health;
            this.maxHealth = health;
            this.attack = attack;
            this.abilities = abilities;
        }
        
        boolean isAlive() {
            return health > 0;
        }
        
        void takeDamage(int damage) {
            health = Math.max(0, health - damage);
        }
    }
    
    /**
     * Create enemy with stats based on enemy type
     */
    private CombatEnemy createEnemy(String enemyName) {
        return switch (enemyName) {
            case "Goblin Raider" -> new CombatEnemy("Goblin Raider", 45, 12, 
                new String[]{"Crude Slash", "Dirty Fighting", "Cowardly Retreat"});
            case "Shadow Wolf" -> new CombatEnemy("Shadow Wolf", 60, 15, 
                new String[]{"Shadow Bite", "Phase Strike", "Howl of Terror"});
            case "Rogue Bandit" -> new CombatEnemy("Rogue Bandit", 55, 13, 
                new String[]{"Quick Strike", "Thrown Dagger", "Dirty Tricks"});
            case "Wild Troll" -> new CombatEnemy("Wild Troll", 80, 18, 
                new String[]{"Club Smash", "Stone Throw", "Regenerate"});
            case "Dark Sprite" -> new CombatEnemy("Dark Sprite", 35, 10, 
                new String[]{"Dark Bolt", "Confusion", "Blink"});
            case "Corrupted Bear" -> new CombatEnemy("Corrupted Bear", 70, 16, 
                new String[]{"Claw Swipe", "Bear Hug", "Roar"});
            case "Skeleton Warrior" -> new CombatEnemy("Skeleton Warrior", 50, 14, 
                new String[]{"Bone Sword", "Shield Bash", "Undead Resilience"});
            case "Ice Wraith" -> new CombatEnemy("Ice Wraith", 40, 11, 
                new String[]{"Frost Touch", "Ice Shard", "Chilling Presence"});
            default -> new CombatEnemy(enemyName, 50, 12, 
                new String[]{"Attack", "Special Move", "Defend"});
        };
    }
    
    /**
     * Handle player's turn in combat
     */
    private boolean handlePlayerCombatTurn(CombatEnemy enemy) {
        System.out.println("\n🗡️ YOUR TURN");
        textDelay();
        System.out.println("💗 Your Health: " + playerCharacter.getHealth() + "/" + playerCharacter.getMaxHealth());
        System.out.println("👹 " + enemy.name + " Health: " + enemy.health + "/" + enemy.maxHealth);
        textDelay();
        
        System.out.println("\nChoose your combat action:");
        textDelay();
        System.out.println("1. ⚔️  Attack with your weapon");
        textDelay();
        System.out.println("2. 🛡️  Defend and reduce incoming damage");
        textDelay();
        
        // Character-specific combat abilities
        if (playerCharacter instanceof Knight) {
            System.out.println("3. 🏆 Noble Strike (Knight special attack)");
        } else if (playerCharacter instanceof Thief) {
            System.out.println("3. 🗡️  Sneak Attack (Thief special attack)");
        } else if (playerCharacter instanceof Wizard) {
            System.out.println("3. ✨ Cast Spell (Wizard magic attack)");
        }
        textDelay();
        System.out.println("4. 🏃 Attempt to flee from combat");
        textDelay();
        
        displayingText = true;
        System.out.print("Choose your action (1-4): ");
        
        try {
            String input = scanner.nextLine();
            displayingText = false;
            int choice = Integer.parseInt(input);
            
            switch (choice) {
                case 1 -> {
                    int damage = calculatePlayerAttackDamage();
                    System.out.println("\n⚔️ You attack the " + enemy.name + " for " + damage + " damage!");
                    enemy.takeDamage(damage);
                    return true;
                }
                case 2 -> {
                    System.out.println("\n🛡️ You raise your guard, ready to defend!");
                    playerDefending = true;
                    return true;
                }
                case 3 -> {
                    // Check if wizard has enough mana for special ability
                    if (playerCharacter instanceof Wizard wizard) {
                        int manaCost = 25;
                        if (!wizard.hasEnoughMana(manaCost)) {
                            System.out.println("\n⚡ You don't have enough magical energy! (Need " + manaCost + ", have " + wizard.getMana() + ")");
                            System.out.println("   Your magic meter needs to recharge before casting powerful spells.");
                            return true; // Turn used but no action taken
                        }
                        wizard.consumeMana(manaCost);
                        System.out.println("\n✨ You channel your magical energy into a devastating spell!");
                        textDelay();
                    }
                    
                    int damage = calculatePlayerSpecialAttackDamage();
                    String abilityName = getPlayerSpecialAbilityName();
                    System.out.println("✨ You unleash " + abilityName + " for " + damage + " damage!");
                    enemy.takeDamage(damage);
                    return true;
                }
                case 4 -> {
                    if (Math.random() < 0.6) {
                        System.out.println("\n🏃 You successfully escape from combat!");
                        return false; // End combat
                    } else {
                        System.out.println("\n❌ You failed to escape! The " + enemy.name + " blocks your path!");
                        return true;
                    }
                }
                default -> {
                    System.out.println("Invalid choice! You hesitate and lose your turn.");
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            displayingText = false;
            System.out.println("Invalid input! You hesitate and lose your turn.");
            return true;
        }
    }
    
    /**
     * Handle enemy's turn in combat
     */
    private void handleEnemyCombatTurn(CombatEnemy enemy) {
        System.out.println("\n👹 " + enemy.name.toUpperCase() + "'S TURN");
        textDelay();
        
        // Enemy chooses random ability
        String ability = enemy.abilities[(int)(Math.random() * enemy.abilities.length)];
        int damage = enemy.attack + (int)(Math.random() * 8) - 2; // Vary damage
        
        // Apply defending bonus if player is defending
        if (playerDefending) {
            damage = Math.max(1, damage / 2);
            playerDefending = false;
            System.out.println("🛡️ Your defense reduces the incoming damage!");
            textDelay();
        }
        
        System.out.println("💥 " + enemy.name + " uses " + ability + " for " + damage + " damage!");
        playerCharacter.takeDamage(damage);
        textDelay();
        System.out.println("💗 Your health: " + playerCharacter.getHealth() + "/" + playerCharacter.getMaxHealth());
        textDelay();
    }
    
    /**
     * Handle combat victory
     */
    private void handleCombatVictory(CombatEnemy enemy) {
        System.out.println("\n🏆 VICTORY!");
        textDelay();
        System.out.println("⚔️ You have defeated the " + enemy.name + "!");
        textDelay();
        
        // Victory rewards
        String[] rewards = {"Battle Trophy", "Enemy Weapon", "Gold Coins", "Rare Gem", "Magic Potion"};
        String reward = rewards[(int)(Math.random() * rewards.length)];
        System.out.println("🎁 Victory spoils await!");
        textDelay();
        addItemWithDescription(reward);
        
        // Small health restoration for victory
        int healing = 5 + (int)(Math.random() * 10);
        playerCharacter.heal(healing);
        System.out.println("💚 The thrill of victory restores your strength!");
        textDelay();
        
        // Magic restoration for wizards
        if (playerCharacter instanceof Wizard wizard) {
            int manaRestore = 10 + (int)(Math.random() * 15);
            wizard.restoreMana(manaRestore);
            System.out.println("✨ Victorious energy replenishes your magical reserves!");
            textDelay();
        }
        
        // Update quest progress for combat victories
        updateQuestProgress("combat", "defeated " + enemy.name);
    }
    
    /**
     * Handle combat defeat
     */
    private void handleCombatDefeat(CombatEnemy enemy) {
        System.out.println("\n💀 DEFEAT...");
        textDelay();
        System.out.println("⚰️ The " + enemy.name + " has overcome you!");
        textDelay();
        System.out.println("🌟 But your spirit remains strong - you'll return to fight another day!");
        textDelay();
        
        // Restore some health to continue adventure (heal to minimum safe level)
        int targetHealth = Math.max(15, playerCharacter.getMaxHealth() / 3);
        int currentHealth = playerCharacter.getHealth();
        if (currentHealth < targetHealth) {
            int healAmount = targetHealth - currentHealth;
            playerCharacter.heal(healAmount);
            System.out.println("💚 You recover enough strength to continue your journey!");
        }
        textDelay();
    }
    
    /**
     * Calculate player attack damage
     */
    private int calculatePlayerAttackDamage() {
        int baseDamage = 15 + (int)(Math.random() * 10);
        
        // Character-specific bonuses
        if (playerCharacter instanceof Knight) {
            baseDamage += 5; // Knights hit harder
        } else if (playerCharacter instanceof Thief) {
            baseDamage += 3; // Thieves are precise
        } else if (playerCharacter instanceof Wizard) {
            baseDamage += 4; // Wizards use magic-enhanced weapons
        }
        
        return baseDamage;
    }
    
    /**
     * Calculate player special attack damage
     */
    private int calculatePlayerSpecialAttackDamage() {
        int specialDamage = 20 + (int)(Math.random() * 12);
        
        // Character-specific special bonuses
        if (playerCharacter instanceof Knight) {
            specialDamage += 8; // Noble Strike is very powerful
        } else if (playerCharacter instanceof Thief) {
            specialDamage += 10; // Sneak attacks are devastating
        } else if (playerCharacter instanceof Wizard) {
            specialDamage += 6; // Spells are consistent but not overwhelming
        }
        
        return specialDamage;
    }
    
    /**
     * Get the name of player's special ability
     */
    private String getPlayerSpecialAbilityName() {
        if (playerCharacter instanceof Knight) {
            return "Noble Strike";
        } else if (playerCharacter instanceof Thief) {
            return "Sneak Attack";
        } else if (playerCharacter instanceof Wizard) {
            return "Magic Missile";
        }
        return "Special Attack";
    }
    
    // Combat state tracking
    private boolean playerDefending = false;
    
    // ===== ITEM AND MOVEMENT DESCRIPTION SYSTEM =====
    
    /**
     * Add item to inventory with rich description of what it does
     */
    private void addItemWithDescription(String item) {
        playerCharacter.addToInventory(item);
        System.out.println("   📦 You acquire: " + item);
        textDelay();
        
        // Provide description of what the item does
        String description = getItemDescription(item);
        if (!description.isEmpty()) {
            System.out.println("   ✨ " + description);
            textDelay();
        }
        
        // Special effects for wizards with magical items
        if (playerCharacter instanceof Wizard wizard) {
            int manaRestore = getMagicItemManaRestore(item);
            if (manaRestore > 0) {
                wizard.restoreMana(manaRestore);
                System.out.println("   ✨ The magical item resonates with your arcane knowledge!");
                textDelay();
            }
        }
    }
    
    /**
     * Get mana restoration amount for magical items (wizards only)
     */
    private int getMagicItemManaRestore(String item) {
        return switch (item.toLowerCase()) {
            case "magic potion" -> 15;
            case "crystal shard", "magic crystals" -> 8;
            case "ancient rune stone" -> 12;
            case "mysterious crystal" -> 10;
            case "enchanted trinket" -> 5;
            case "mysterious artifact", "ancient artifact" -> 7;
            case "wisdom scroll" -> 3;
            default -> 0;
        };
    }
    
    /**
     * Get detailed description of what an item does
     */
    private String getItemDescription(String item) {
        return switch (item.toLowerCase()) {
            // Healing and consumable items
            case "healing herb", "healing potion" -> "Restores health when used in dire situations.";
            case "magic potion" -> "Contains mysterious energies that restore magical power and enhance abilities.";
            case "travel rations", "traveler's ration" -> "Nutritious food that sustains you during long journeys.";
            
            // Valuable items and currency
            case "gold coins", "ancient gold coin", "silver coins", "silver ingots" -> "Valuable currency accepted by merchants throughout the realm.";
            case "precious gems", "rare gem" -> "Sparkling jewels worth a small fortune to collectors.";
            case "ancient jewelry" -> "Ornate accessories from a bygone era, highly prized by antiquarians.";
            
            // Magical items
            case "crystal shard", "magic crystals" -> "Pulsing with arcane energy, useful for magical rituals.";
            case "ancient rune stone" -> "Carved with powerful symbols that enhance magical abilities.";
            case "lucky charm", "lucky token" -> "Brings good fortune and improves your chances of success.";
            case "enchanted trinket" -> "A small magical item that provides subtle but useful benefits.";
            case "mysterious crystal" -> "Glows with inner light and seems to respond to your thoughts.";
            
            // Information and tools
            case "ancient map", "road map", "old map fragment" -> "Reveals hidden paths and secret locations in the world.";
            case "wisdom scroll" -> "Contains ancient knowledge that expands your understanding.";
            case "cryptic riddle" -> "A puzzle that may lead to greater treasures when solved.";
            case "ancient key" -> "Opens locked doors and chests throughout your adventures.";
            case "treasure hunter's note" -> "Contains clues about hidden caches and valuable finds.";
            
            // Combat and equipment
            case "enemy weapon", "ornate weapons" -> "Well-crafted arms that give you an edge in battle.";
            case "battle trophy" -> "A symbol of your prowess that intimidates enemies.";
            case "traveler's cloak" -> "Provides protection from the elements during your journeys.";
            
            // Special artifacts
            case "mysterious artifact", "rare artifacts", "ancient artifact" -> "An item of unknown power that scholars would pay handsomely to study.";
            case "blessed leaf" -> "Touched by natural magic, it brings peace and wards off dark forces.";
            case "valuable art" -> "Beautiful creations that demonstrate the skill of master artisans.";
            case "valuable trinket" -> "A small but precious item that catches the eye of discerning buyers.";
            
            // Quest and special items
            case "royal crown" -> "The crown of ancient kings, symbolizing rightful authority and power.";
            case "dragon's treasure" -> "Hoarded wealth from a dragon's lair, immensely valuable and magical.";
            case "stolen spellbook" -> "Contains powerful magical formulas coveted by wizards everywhere.";
            
            // Common finds
            case "old coins", "shiny pebble" -> "Small treasures that accumulate into something meaningful over time.";
            case "useful herbs" -> "Natural remedies that can be brewed into helpful concoctions.";
            case "worn map fragment" -> "A piece of a larger map that hints at greater discoveries.";
            
            default -> ""; // No description for unknown items
        };
    }
    
    /**
     * Provide rich descriptions of character movement
     */
    private void describeMovement(int oldX, int oldY, int newX, int newY, String context) {
        int deltaX = newX - oldX;
        int deltaY = newY - oldY;
        
        // Determine primary direction
        String direction;
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            direction = deltaX > 0 ? "eastward" : "westward";
        } else {
            direction = deltaY > 0 ? "northward" : "southward";
        }
        
        // Character-specific movement descriptions
        if (playerCharacter instanceof Knight) {
            describeKnightMovement(direction, context, oldX, oldY, newX, newY);
        } else if (playerCharacter instanceof Thief) {
            describeThiefMovement(direction, context, oldX, oldY, newX, newY);
        } else if (playerCharacter instanceof Wizard) {
            describeWizardMovement(direction, context, oldX, oldY, newX, newY);
        }
    }
    
    private void describeKnightMovement(String direction, String context, int oldX, int oldY, int newX, int newY) {
        switch (context) {
            case "exploration" -> {
                System.out.println("   With noble purpose, you stride " + direction + " from (" + oldX + ", " + oldY + ") to (" + newX + ", " + newY + ").");
                System.out.println("   Your armor gleams as you search for those in need of a knight's protection.");
            }
            case "travel" -> {
                System.out.println(playerCharacter.getName() + " marches " + direction + " with determined steps, from (" + oldX + ", " + oldY + ") to (" + newX + ", " + newY + ").");
                System.out.println("   The path ahead calls to your sense of duty and honor.");
            }
        }
    }
    
    private void describeThiefMovement(String direction, String context, int oldX, int oldY, int newX, int newY) {
        switch (context) {
            case "exploration" -> {
                System.out.println("   You slip silently " + direction + " from (" + oldX + ", " + oldY + ") to (" + newX + ", " + newY + ").");
                System.out.println("   Every shadow offers concealment as you scout for opportunities.");
            }
            case "travel" -> {
                System.out.println(playerCharacter.getName() + " moves " + direction + " like a ghost, from (" + oldX + ", " + oldY + ") to (" + newX + ", " + newY + ").");
                System.out.println("   Your keen eyes scan for both danger and hidden treasures.");
            }
        }
    }
    
    private void describeWizardMovement(String direction, String context, int oldX, int oldY, int newX, int newY) {
        switch (context) {
            case "exploration" -> {
                System.out.println("   Guided by arcane intuition, you drift " + direction + " from (" + oldX + ", " + oldY + ") to (" + newX + ", " + newY + ").");
                System.out.println("   Magical energies seem to flow around you, revealing hidden mysteries.");
            }
            case "travel" -> {
                System.out.println(playerCharacter.getName() + " glides " + direction + " with mystical grace, from (" + oldX + ", " + oldY + ") to (" + newX + ", " + newY + ").");
                System.out.println("   The ley lines of power guide your steps toward ancient knowledge.");
            }
        }
    }
    
    // ===== QUEST SYSTEM =====
    
    // Active quests tracking
    private List<Quest> activeQuests = new ArrayList<>();
    
    /**
     * Quest class to represent player quests with objectives and tracking
     */
    private static class Quest {
        String title;
        String description;
        String objective;
        String reward;
        boolean completed;
        int progress; // Tracks quest progress (combat wins, items found, etc.)
        int targetProgress; // How much progress needed to complete
        
        Quest(String title, String description, String objective, String reward) {
            this.title = title;
            this.description = description;
            this.objective = objective;
            this.reward = reward;
            this.completed = false;
            this.progress = 0;
            this.targetProgress = extractTargetFromObjective(objective);
        }
        
        // Extract target number from objective string (e.g., "Defeat 3 enemies" -> 3)
        private int extractTargetFromObjective(String obj) {
            String[] words = obj.split(" ");
            for (String word : words) {
                try {
                    int num = Integer.parseInt(word);
                    return num;
                } catch (NumberFormatException e) {
                    // Continue looking
                }
            }
            return 1; // Default target
        }
        
        void addProgress(int amount) {
            progress += amount;
            if (progress >= targetProgress) {
                completed = true;
            }
        }
        
        boolean isCompleted() {
            return completed;
        }
        
        String getProgressText() {
            return "(" + progress + "/" + targetProgress + ")";
        }
    }
    
    /**
     * Accept a quest and add it to active quests
     */
    private void acceptQuest(Quest quest) {
        activeQuests.add(quest);
        System.out.println("\n✅ Quest Accepted: " + quest.title);
        textDelay();
        System.out.println("📝 The quest has been added to your journal.");
        textDelay();
        System.out.println("🎯 Remember: " + quest.objective);
        textDelay();
        
        // Add quest marker to inventory
        playerCharacter.addToInventory("[QUEST] " + quest.title);
        
        gameWorld.handleCharacterAction(playerCharacter.getName(), "quest", "accepted " + quest.title);
    }
    
    /**
     * Update quest progress based on player actions
     */
    private void updateQuestProgress(String actionType, String details) {
        for (Quest quest : activeQuests) {
            if (quest.isCompleted()) continue;
            
            // Check if this action progresses any active quest
            if (actionType.equals("combat") && quest.objective.toLowerCase().contains("defeat")) {
                quest.addProgress(1);
                System.out.println("\n📋 Quest Progress: " + quest.title + " " + quest.getProgressText());
                textDelay();
                
                if (quest.isCompleted()) {
                    completeQuest(quest);
                }
            } else if (actionType.equals("exploration") && quest.objective.toLowerCase().contains("explore")) {
                quest.addProgress(1);
                System.out.println("\n📋 Quest Progress: " + quest.title + " " + quest.getProgressText());
                textDelay();
                
                if (quest.isCompleted()) {
                    completeQuest(quest);
                }
            } else if (actionType.equals("discovery") && quest.objective.toLowerCase().contains("find")) {
                quest.addProgress(1);
                System.out.println("\n📋 Quest Progress: " + quest.title + " " + quest.getProgressText());
                textDelay();
                
                if (quest.isCompleted()) {
                    completeQuest(quest);
                }
            }
        }
    }
    
    /**
     * Complete a quest and give rewards
     */
    private void completeQuest(Quest quest) {
        System.out.println("\n🏆 QUEST COMPLETED!");
        textDelay();
        System.out.println("✨ " + quest.title + " - FINISHED!");
        textDelay();
        System.out.println("🎁 Reward: " + quest.reward);
        textDelay();
        
        // Give quest rewards
        String[] rewardItems = quest.reward.split(" \\+ ");
        for (String reward : rewardItems) {
            if (!reward.toLowerCase().contains("gold") && !reward.toLowerCase().contains("bonus") && 
                !reward.toLowerCase().contains("increase") && !reward.toLowerCase().contains("protection")) {
                addItemWithDescription(reward.trim());
            }
        }
        
        // Heal player for quest completion
        playerCharacter.heal(15);
        System.out.println("💚 Your sense of accomplishment restores 15 health!");
        textDelay();
        
        // Remove quest marker from inventory
        playerCharacter.getInventory().removeIf(item -> item.equals("[QUEST] " + quest.title));
        
        gameWorld.handleCharacterAction(playerCharacter.getName(), "quest_complete", "completed " + quest.title);
    }
    
    /**
     * Show active quests to the player
     */
    private void showActiveQuests() {
        if (activeQuests.isEmpty()) {
            System.out.println("\n📋 Quest Journal: Empty");
            textDelay();
            System.out.println("   No active quests. Explore the world to find new adventures!");
            textDelay();
            return;
        }
        
        System.out.println("\n📋 ACTIVE QUESTS:");
        textDelay();
        
        for (Quest quest : activeQuests) {
            String status = quest.isCompleted() ? "✅ READY TO COMPLETE" : "🔄 IN PROGRESS";
            System.out.println("\n" + status + ": " + quest.title);
            textDelay();
            System.out.println("   🎯 " + quest.objective + " " + quest.getProgressText());
            textDelay();
            System.out.println("   🏆 Reward: " + quest.reward);
            textDelay();
        }
    }
}
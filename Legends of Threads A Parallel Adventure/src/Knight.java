import java.util.Random;

/**
 * Knight character - A noble warrior who protects others and seeks combat
 * Specializes in defense and direct combat
 */
public class Knight extends GameCharacter {
    private int armor;
    private int strength;
    private Random random;
    private int questsCompleted;
    private int honor; // Knight's honor level
    private String currentQuest;
    private boolean inCombat;
    private int villagersSaved;
    private final GameWorld gameWorld;
    
    public Knight(String name, int startX, int startY, SharedResources sharedResources, GameAnalytics analytics, GameWorld gameWorld) {
        super(name, 120, startX, startY, sharedResources, analytics); // High health
        this.gameWorld = gameWorld;
        this.armor = 15;
        this.strength = 20;
        this.random = new Random();
        this.questsCompleted = 0;
        this.honor = 50; // Starting honor
        this.currentQuest = "Seek the Ancient Artifact";
        this.inCombat = false;
        this.villagersSaved = 0;
        addToInventory("Iron Sword");
        addToInventory("Shield");
        System.out.println("⚔️ " + name + " the Knight swears an oath to protect the innocent!");
    }
    
    @Override
    public void act() {
        if (!isAlive || !isActive || caveMode) return;
        
        // Knight's behavior: patrol, seek enemies, protect others
        int action = random.nextInt(4);
        
        switch (action) {
            case 0:
                patrol();
                break;
            case 1:
                seekCombat();
                break;
            case 2:
                searchForQuests();
                break;
            case 3:
                rest();
                break;
        }
    }
    
    @Override
    public void run() {
        if (!caveMode) {
            System.out.println(name + " the Knight begins their noble quest!");
        }
        
        while (isActive && isAlive) {
            try {
                // Skip actions during cave mode to prevent background messages
                if (!caveMode) {
                    act();
                }
                Thread.sleep(2000); // Knight acts every 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!caveMode) {
            System.out.println(name + " the Knight has ended their watch.");
        }
    }
    
    private void patrol() {
        // Move in a pattern to guard the area
        int direction = random.nextInt(4);
        switch (direction) {
            case 0: move(0, 1); break;  // North
            case 1: move(1, 0); break;  // East
            case 2: move(0, -1); break; // South
            case 3: move(-1, 0); break; // West
        }
        System.out.println(name + " patrols the realm with vigilant eyes at (" + x + ", " + y + ")");
    }
    
    private void seekCombat() {
        System.out.println(name + " searches for worthy opponents to test their mettle.");
        if (random.nextInt(4) == 0) { // 25% chance to find combat
            System.out.println(name + " draws sword, ready for battle!");
            useSpecialAbility();
        }
    }
    
    private void searchForQuests() {
        int event = random.nextInt(10);
        
        if (event == 0) { // 10% chance - Major quest completion
            questsCompleted++;
            honor += 10;
            addToInventory("Legendary Artifact " + questsCompleted);
            System.out.println("🏆 " + name + " completes the legendary quest '" + currentQuest + "'! Honor increased!");
            
            // Deposit quest reward to treasure vault
            sharedResources.depositTreasure("Gold Coins", 100, name);
            sharedResources.addToSharedInventory("Quest Completion Certificate", name);
            
            generateNewQuest();
            
        } else if (event <= 2) { // 20% chance - Save villagers
            villagersSaved++;
            honor += 5;
            System.out.println("🛡️ " + name + " rescues villagers from danger! (" + villagersSaved + " saved)");
            
            // Try to grab some loot as reward
            String loot = sharedResources.tryTakeLoot(name);
            if (loot != null) {
                addToInventory(loot);
            }
            
        } else if (event <= 4) { // 20% chance - Random challenge
            handleRandomChallenge();
            
        } else {
            System.out.println("⚔️ " + name + " continues the quest: '" + currentQuest + "'");
        }
    }
    
    private void generateNewQuest() {
        String[] quests = {
            "Slay the Shadow Dragon",
            "Recover the Lost Crown",
            "Defend the Sacred Temple",
            "Unite the Warring Kingdoms",
            "Purify the Cursed Lands"
        };
        currentQuest = quests[random.nextInt(quests.length)];
        System.out.println("📜 " + name + " receives a new quest: '" + currentQuest + "'");
    }
    
    private void handleRandomChallenge() {
        String[] challenges = {
            "faces a pack of dire wolves",
            "encounters a mysterious hooded figure",
            "discovers a cursed weapon",
            "meets a fellow knight in distress",
            "finds a village under siege"
        };
        
        String challenge = challenges[random.nextInt(challenges.length)];
        String enemyType = analytics.getRandomEnemyType();
        System.out.println("⚡ " + name + " " + challenge + " featuring " + enemyType + "!");
        
        // Create a challenge resolution thread using lambda
        Thread challengeThread = new Thread(() -> {
            try {
                inCombat = true;
                System.out.println("⚔️ " + name + " prepares for battle against " + enemyType + "...");
                Thread.sleep(2000); // Battle duration
                
                int damageDealt = 15 + random.nextInt(20);
                int damageReceived = random.nextInt(20);
                boolean victory = random.nextBoolean();
                
                // Log battle using analytics
                analytics.logBattle(name, enemyType, victory, damageDealt, damageReceived);
                
                if (victory) {
                    System.out.println("✅ " + name + " emerges victorious against " + enemyType + "!");
                    honor += 3;
                    heal(10);
                    
                    // Log item found as battle reward
                    String loot = "Battle Trophy (" + enemyType + ")";
                    addToInventory(loot);
                    analytics.logItemCollection(name, loot, "Battle Victory");
                } else {
                    System.out.println("💥 " + name + " takes heavy damage but fights on!");
                    takeDamage(damageReceived);
                }
                inCombat = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        challengeThread.start();
    }
    
    private void rest() {
        heal(5);
        System.out.println(name + " takes a moment to rest and recover strength.");
        
        // Try to use mana for enhanced healing
        if (sharedResources.consumeMana(20, name)) {
            heal(10); // Extra healing with mana
            System.out.println("✨ " + name + " uses mana for enhanced healing!");
        }
        
        // Check treasure vault occasionally
        if (random.nextInt(4) == 0) {
            System.out.print(sharedResources.viewTreasureVault(name));
        }
    }
    
    @Override
    public void interact(GameCharacter other) {
        if (other instanceof Thief) {
            System.out.println(name + " eyes " + other.getName() + " suspiciously, hand on sword hilt.");
        } else if (other instanceof Wizard) {
            System.out.println(name + " respectfully nods to the wise " + other.getName() + ".");
        } else {
            System.out.println(name + " greets " + other.getName() + " with honor.");
        }
    }
    
    @Override
    public void useSpecialAbility() {
        System.out.println(name + " raises shield and enters defensive stance! Armor increased temporarily.");
        armor += 5;
        
        // Create a thread to reduce armor back after some time
        Thread armorBoostThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 seconds
                armor -= 5;
                System.out.println(name + "'s defensive stance ends.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        armorBoostThread.start();
    }
    
    @Override
    public String getCharacterType() {
        return "Knight";
    }
    
    protected void executePlayerAction(String action) {
        switch (action.toLowerCase()) {
            case "a", "attack" -> {
                System.out.println("⚔️ " + name + " (You) seeks combat!");
                seekCombat();
                System.out.println("✅ Action completed.");
            }
            case "p", "patrol" -> {
                System.out.println("🚪 " + name + " (You) begins patrolling!");
                patrol();
                System.out.println("✅ Patrol completed.");
            }
            case "q", "quest" -> {
                System.out.println("🏆 " + name + " (You) undertakes a quest!");
                searchForQuests();
                System.out.println("✅ Quest action completed.");
            }
            case "d", "defend" -> {
                System.out.println("🛡️ " + name + " (You) takes a defensive stance!");
                armor += 5;
                System.out.println(name + " raises shield! Armor temporarily increased to " + armor);
                System.out.println("✅ Defense stance ready.");
            }
            default -> System.out.println("⚠️ Unknown command: " + action + ". Type 'help' for available commands.");
        }
    }
    
    // Knight-specific methods
    public void challenge(GameCharacter opponent) {
        if (opponent.isAlive() && distanceTo(opponent) <= 1) {
            System.out.println(name + " challenges " + opponent.getName() + " to honorable combat!");
            opponent.takeDamage(strength);
        }
    }
    
    public int getArmor() { return armor; }
    public int getStrength() { return strength; }
    public int getQuestsCompleted() { return questsCompleted; }
}
import java.util.List;
import java.util.Random;

/**
 * Thief character - A stealthy character who steals, hides, and moves quickly
 * Specializes in stealth and acquiring items
 */
public class Thief extends GameCharacter {
    private int stealth;
    private int agility;
    private Random random;
    private boolean isHiding;
    private int itemsStolen;
    private int reputation; // Thief's underground reputation
    private String currentHeist;
    private boolean onTheRun;
    private int guardsEluded;
    private final GameWorld gameWorld;
    
    public Thief(String name, int startX, int startY, SharedResources sharedResources, GameAnalytics analytics, GameWorld gameWorld) {
        super(name, 80, startX, startY, sharedResources, analytics); // Medium health
        this.gameWorld = gameWorld;
        this.stealth = 25;
        this.agility = 30;
        this.random = new Random();
        this.isHiding = false;
        this.itemsStolen = 0;
        this.reputation = 20; // Starting reputation in thieves' guild
        this.currentHeist = "Infiltrate the Noble's Manor";
        this.onTheRun = false;
        this.guardsEluded = 0;
        addToInventory("Lockpicks");
        addToInventory("Throwing Dagger");
        System.out.println("🗡️ " + name + " the Thief emerges from the shadows with a new target in mind!");
    }
    
    @Override
    public void act() {
        if (!isAlive || !isActive) return;
        
        // Thief's behavior: sneak, steal, hide, scout
        int action = random.nextInt(4);
        
        switch (action) {
            case 0:
                sneak();
                break;
            case 1:
                attemptSteal();
                break;
            case 2:
                hide();
                break;
            case 3:
                scout();
                break;
        }
    }
    
    @Override
    public void run() {
        System.out.println(name + " the Thief slips into the shadows...");
        
        while (isActive && isAlive) {
            try {
                act();
                Thread.sleep(1500); // Thief acts every 1.5 seconds (faster than Knight)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(name + " the Thief vanishes into the night.");
    }
    
    private void sneak() {
        // Move quietly and quickly
        int direction = random.nextInt(4);
        switch (direction) {
            case 0: move(0, 1); break;  // North
            case 1: move(1, 0); break;  // East
            case 2: move(0, -1); break; // South
            case 3: move(-1, 0); break; // West
        }
        
        if (random.nextInt(3) == 0) { // 33% chance of extra movement
            direction = random.nextInt(4);
            switch (direction) {
                case 0: move(0, 1); break;
                case 1: move(1, 0); break;
                case 2: move(0, -1); break;
                case 3: move(-1, 0); break;
            }
            System.out.println(name + " moves swiftly and silently to (" + x + ", " + y + ")");
            
            // Chance to find loot while moving quickly
            String loot = sharedResources.tryTakeLoot(name);
            if (loot != null) {
                addToInventory(loot);
            }
        } else {
            System.out.println(name + " sneaks carefully to (" + x + ", " + y + ")");
        }
    }
    
    private void attemptSteal() {
        int event = random.nextInt(10);
        
        if (event == 0) { // 10% chance - Major heist success
            itemsStolen += 3;
            reputation += 15;
            addToInventory("Priceless Gem");
            System.out.println("💎 " + name + " pulls off the heist '" + currentHeist + "'! Reputation soars!");
            
            // Steal from treasure vault
            if (sharedResources.withdrawTreasure("Precious Gems", 5, name)) {
                // Successfully stolen gems
            }
            
            // Try to trade at trading post
            if (sharedResources.tradeForItem("Invisibility Cloak", name)) {
                addToInventory("Invisibility Cloak");
            }
            
            generateNewHeist();
            
        } else if (event <= 2) { // 20% chance - Regular theft
            itemsStolen++;
            String stolenItem = "Stolen Treasure " + itemsStolen;
            addToInventory(stolenItem);
            System.out.println("🏺 " + name + " successfully pilfers " + stolenItem + "!");
            reputation += 2;
            
        } else if (event <= 4) { // 20% chance - Close call with guards
            handleGuardEncounter();
            
        } else if (event == 5) { // 10% chance - Find secret passage
            System.out.println("🚪 " + name + " discovers a hidden passage! New opportunities await.");
            addToInventory("Secret Map");
            
        } else {
            System.out.println("👁️ " + name + " scouts for the perfect opportunity for '" + currentHeist + "'");
        }
    }
    
    private void generateNewHeist() {
        String[] heists = {
            "Rob the Royal Treasury",
            "Steal the Wizard's Spellbook",
            "Infiltrate the Merchant's Vault",
            "Acquire the Knight's Legendary Sword",
            "Pilfer the Dragon's Hoard"
        };
        currentHeist = heists[random.nextInt(heists.length)];
        System.out.println("📋 " + name + " plans the next heist: '" + currentHeist + "'");
    }
    
    private void handleGuardEncounter() {
        System.out.println("🚨 " + name + " is spotted by guards!");
        
        Thread chaseThread = new Thread(() -> {
            try {
                onTheRun = true;
                System.out.println("🏃 " + name + " flees through the winding alleys...");
                Thread.sleep(1500);
                
                if (random.nextInt(stealth) > 15) { // Stealth check
                    System.out.println("✅ " + name + " vanishes into the shadows! Guards lost.");
                    guardsEluded++;
                    reputation += 3;
                    useSpecialAbility(); // Auto-activate stealth
                } else {
                    System.out.println("💥 " + name + " takes a hit while escaping!");
                    takeDamage(10);
                }
                
                Thread.sleep(1000);
                onTheRun = false;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        chaseThread.start();
    }
    
    private void hide() {
        isHiding = !isHiding;
        if (isHiding) {
            System.out.println(name + " melts into the shadows, becoming nearly invisible.");
            useSpecialAbility(); // Activate stealth boost
        } else {
            System.out.println(name + " emerges from hiding, ready for action.");
        }
    }
    
    private void scout() {
        String location = analytics.getRandomWorldLocation();
        System.out.println("👁️ " + name + " scouts the area near " + location + ", gathering information.");
        
        // Use streams to check for nearby threats
        List<String> nearbyEnemies = analytics.getAllEnemyTypes().stream()
            .filter(enemy -> random.nextInt(10) < 3) // 30% chance each enemy type is nearby
            .limit(2) // Max 2 enemy types
            .collect(java.util.stream.Collectors.toList());
        
        if (!nearbyEnemies.isEmpty()) {
            String threats = String.join(" and ", nearbyEnemies);
            System.out.println("⚠️ " + name + " spots " + threats + " in the area!");
            analytics.logEvent(name, GameAnalytics.EventType.INTERACTION, 
                "Scouted threats: " + threats + " near " + location);
        }
        
        if (random.nextInt(4) == 0) { // 25% chance of finding useful info
            String intelType = analytics.getRandomTreasureCategory() + " Intelligence";
            addToInventory(intelType);
            analytics.logItemCollection(name, intelType, "Scouting " + location);
            System.out.println(name + " discovers valuable intelligence about " + intelType + "!");
        }
    }
    
    @Override
    public void interact(GameCharacter other) {
        if (other instanceof Knight) {
            System.out.println(name + " tries to avoid " + other.getName() + "'s watchful gaze.");
        } else if (other instanceof Wizard) {
            System.out.println(name + " shows interest in " + other.getName() + "'s magical trinkets.");
        } else {
            System.out.println(name + " nods silently at " + other.getName() + " from the shadows.");
        }
    }
    
    @Override
    public void useSpecialAbility() {
        System.out.println(name + " activates shadow cloak! Stealth greatly increased.");
        stealth += 10;
        
        // Create a thread to reduce stealth back after some time
        Thread stealthBoostThread = new Thread(() -> {
            try {
                Thread.sleep(4000); // 4 seconds
                stealth -= 10;
                System.out.println(name + "'s shadow cloak fades.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        stealthBoostThread.start();
    }
    
    @Override
    public String getCharacterType() {
        return "Thief";
    }
    
    protected void executePlayerAction(String action) {
        switch (action.toLowerCase()) {
            case "s", "steal" -> {
                System.out.println("🎯 " + name + " (You) attempts to steal!");
                attemptSteal();
            }
            case "h", "hide" -> {
                System.out.println("🕵️ " + name + " (You) hides in the shadows!");
                hide();
            }
            case "scout" -> {
                System.out.println("🔍 " + name + " (You) scouts the area!");
                scout();
            }
            case "e", "escape" -> {
                System.out.println("🏃 " + name + " (You) sneaks away!");
                sneak();
            }
            default -> System.out.println("⚠️ Unknown command: " + action + ". Type 'help' for available commands.");
        }
    }
    
    // Thief-specific methods
    public void pickpocket(GameCharacter target) {
        if (target.isAlive() && distanceTo(target) <= 1 && !target.getInventory().isEmpty()) {
            System.out.println(name + " attempts to pickpocket " + target.getName() + "!");
            if (random.nextInt(2) == 0) { // 50% success rate
                System.out.println("Success! " + name + " steals an item.");
                itemsStolen++;
            } else {
                System.out.println("Failed! " + target.getName() + " notices the attempt.");
            }
        }
    }
    
    public int getStealth() { return stealth; }
    public int getAgility() { return agility; }
    public boolean isHiding() { return isHiding; }
    public int getItemsStolen() { return itemsStolen; }
}
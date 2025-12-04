import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract base class for all game characters
 * Implements Runnable for multithreading support
 */
public abstract class GameCharacter implements Runnable {
    // Shared attributes
    protected String name;
    protected int health;
    protected int maxHealth;
    protected int x, y; // Position coordinates
    protected boolean isAlive;
    protected boolean isActive;
    protected List<String> inventory;
    protected SharedResources sharedResources; // Reference to shared resources
    protected GameAnalytics analytics; // Reference to game analytics
    protected ReentrantLock characterLock; // For thread safety
    protected boolean isPlayerControlled; // Whether this character is controlled by the player
    protected volatile String pendingPlayerAction; // Action waiting to be executed by player
    
    // Constructor
    public GameCharacter(String name, int health, int startX, int startY, SharedResources sharedResources, GameAnalytics analytics) {
        this.name = name;
        this.health = health;
        this.maxHealth = health;
        this.x = startX;
        this.y = startY;
        this.isAlive = true;
        this.isActive = true;
        this.inventory = new ArrayList<>();
        this.sharedResources = sharedResources;
        this.analytics = analytics;
        this.characterLock = new ReentrantLock();
        this.isPlayerControlled = false;
        this.pendingPlayerAction = null;
    }
    
    // Abstract methods that must be implemented by subclasses
    public abstract void act();
    public abstract void interact(GameCharacter other);
    public abstract void useSpecialAbility();
    public abstract String getCharacterType();
    
    // Player control methods
    public void setPlayerControlled(boolean playerControlled) {
        this.isPlayerControlled = playerControlled;
    }
    
    public boolean isPlayerControlled() {
        return isPlayerControlled;
    }
    
    public void setPlayerAction(String action) {
        this.pendingPlayerAction = action;
    }
    
    public String getPlayerAction() {
        String action = pendingPlayerAction;
        pendingPlayerAction = null;
        return action;
    }
    
    // Concrete methods shared by all characters
    public void move(int deltaX, int deltaY) {
        characterLock.lock();
        try {
            if (isAlive && isActive) {
                int newX = x + deltaX;
                int newY = y + deltaY;
                
                // Simple boundary check (no GameWorld needed)
                if (newX >= -10 && newX <= 10 && newY >= -10 && newY <= 10) {
                    x = newX;
                    y = newY;
                    
                    // Log movement with analytics
                    analytics.logEvent(name, GameAnalytics.EventType.MOVEMENT, 
                        "Moved to (" + x + ", " + y + ")");
                }
            }
        } finally {
            characterLock.unlock();
        }
    }
    
    public void takeDamage(int damage) {
        characterLock.lock();
        try {
            health -= damage;
            if (health <= 0) {
                health = 0;
                isAlive = false;
                System.out.println("💀 " + name + " has been defeated!");
            } else {
                System.out.println("🩸 " + name + " took " + damage + " damage. Health: " + health + "/" + maxHealth);
                
                // Log damage event
                analytics.logEvent(name, GameAnalytics.EventType.BATTLE_LOST, 
                    "Received " + damage + " damage");
            }
        } finally {
            characterLock.unlock();
        }
    }
    
    public void heal(int amount) {
        characterLock.lock();
        try {
            if (isAlive) {
                health = Math.min(health + amount, maxHealth);
                System.out.println("💚 " + name + " healed for " + amount + ". Health: " + health + "/" + maxHealth);
                
                // Log healing event
                analytics.logEvent(name, GameAnalytics.EventType.HEALING, 
                    "Healed for " + amount + " HP");
            }
        } finally {
            characterLock.unlock();
        }
    }
    
    public void addToInventory(String item) {
        characterLock.lock();
        try {
            inventory.add(item);
            System.out.println("🎒 " + name + " acquired: " + item);
        } finally {
            characterLock.unlock();
        }
    }
    
    public boolean hasItem(String item) {
        characterLock.lock();
        try {
            return inventory.contains(item);
        } finally {
            characterLock.unlock();
        }
    }
    
    public void stop() {
        characterLock.lock();
        try {
            isActive = false;
        } finally {
            characterLock.unlock();
        }
    }
    
    // Runnable implementation for threading
    @Override
    public void run() {
        System.out.println("🧵 " + name + " thread started!" + (isPlayerControlled ? " (Player Controlled)" : " (AI Party Member)"));
        
        if (isPlayerControlled) {
            System.out.println("⏳ " + name + " awaits your command...");
        }
        
        while (isAlive && isActive) {
            try {
                if (isPlayerControlled) {
                    // Player controlled character waits for player input
                    handlePlayerControl();
                    // After action, show waiting message again
                    if (isAlive && isActive) {
                        System.out.println("⏳ " + name + " awaits your next command...");
                    }
                } else {
                    // AI controlled character acts automatically
                    act();
                    Thread.sleep(1000); // Wait 1 second between actions
                }
            } catch (InterruptedException e) {
                System.out.println(name + " thread interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("💀 " + name + " has stopped acting.");
    }
    
    /**
     * Handle player control input
     */
    protected void handlePlayerControl() throws InterruptedException {
        // Wait until player provides an action
        String action = null;
        while (action == null && isAlive && isActive) {
            action = getPlayerAction();
            if (action == null) {
                Thread.sleep(200); // Wait for player input
            }
        }
        
        if (action != null && isAlive && isActive) {
            executePlayerAction(action);
        }
    }
    
    /**
     * Execute a player action - to be overridden by subclasses
     */
    protected void executePlayerAction(String action) {
        System.out.println(name + " doesn't understand the command: " + action);
    }
    
    // Getters
    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return isAlive; }
    public boolean isActive() { return isActive; }
    public List<String> getInventory() { return new ArrayList<>(inventory); }
    
    // Calculate distance to another character
    public double distanceTo(GameCharacter other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    @Override
    public String toString() {
        return getCharacterType() + " " + name + " [Health: " + health + "/" + maxHealth + 
               ", Position: (" + x + ", " + y + "), Alive: " + isAlive + "]";
    }
}
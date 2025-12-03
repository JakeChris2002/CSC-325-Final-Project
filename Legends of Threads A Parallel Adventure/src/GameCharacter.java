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
    protected ReentrantLock characterLock; // For thread safety
    
    // Constructor
    public GameCharacter(String name, int health, int startX, int startY, SharedResources sharedResources) {
        this.name = name;
        this.health = health;
        this.maxHealth = health;
        this.x = startX;
        this.y = startY;
        this.isAlive = true;
        this.isActive = true;
        this.inventory = new ArrayList<>();
        this.sharedResources = sharedResources;
        this.characterLock = new ReentrantLock();
    }
    
    // Abstract methods that must be implemented by subclasses
    public abstract void act();
    public abstract void interact(GameCharacter other);
    public abstract void useSpecialAbility();
    public abstract String getCharacterType();
    
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
        while (isActive && isAlive) {
            try {
                act(); // Call the abstract act method
                Thread.sleep(1000); // Wait 1 second between actions
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("🛑 " + name + " (" + getCharacterType() + ") has stopped acting.");
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
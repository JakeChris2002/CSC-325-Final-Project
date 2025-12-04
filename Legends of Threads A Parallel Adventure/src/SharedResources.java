import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SharedResources - Thread-safe shared resource management system
 * Demonstrates various concurrency mechanisms for preventing race conditions
 */
public class SharedResources {
    
    // === TREASURE VAULT (using ReentrantReadWriteLock) ===
    private final ReentrantReadWriteLock treasureLock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<String, Integer> treasureVault = new ConcurrentHashMap<>();
    
    // === GLOBAL MANA POOL (using AtomicInteger) ===
    private final AtomicInteger globalManaPool = new AtomicInteger(1000);
    private final AtomicInteger totalManaConsumed = new AtomicInteger(0);
    
    // === LOOT QUEUE (using BlockingQueue) ===
    private final BlockingQueue<String> lootQueue = new ArrayBlockingQueue<>(50);
    
    // === SHARED INVENTORY (using synchronized methods) ===
    private final List<String> sharedInventory = new ArrayList<>();
    private final Object inventoryLock = new Object();
    
    // === RESOURCE GENERATION (using volatile) ===
    private volatile boolean resourceGenerationActive = true;
    private final AtomicInteger resourcesGenerated = new AtomicInteger(0);
    private volatile boolean caveMode = true; // Start silent until explicitly allowed
    private Thread resourceThread;
    private Thread lootThread;
    
    // === TRADING POST (using ConcurrentHashMap) ===
    private final ConcurrentHashMap<String, String> tradingPost = new ConcurrentHashMap<>();
    
    private final Random random = new Random();
    
    public SharedResources() {
        initializeTreasures();
        initializeTradingPost();
        startResourceGeneration();
        startLootGeneration();
    }
    
    public void setCaveMode(boolean caveMode) {
        this.caveMode = caveMode;
        if (caveMode) {
            // Completely stop resource generation during cave mode
            resourceGenerationActive = false;
            
            // Force interrupt and stop existing threads
            if (resourceThread != null && resourceThread.isAlive()) {
                resourceThread.interrupt();
                try {
                    resourceThread.join(1000); // Wait up to 1 second for thread to stop
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (lootThread != null && lootThread.isAlive()) {
                lootThread.interrupt();
                try {
                    lootThread.join(1000); // Wait up to 1 second for thread to stop
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            // Restart resource generation after cave mode
            resourceGenerationActive = true;
            // Only restart if threads are not running
            if (resourceThread == null || !resourceThread.isAlive()) {
                startResourceGeneration();
            }
            if (lootThread == null || !lootThread.isAlive()) {
                startLootGeneration();
            }
        }
    }
    
    // ===============================================
    // TREASURE VAULT METHODS (ReentrantReadWriteLock)
    // ===============================================
    
    private void initializeTreasures() {
        treasureVault.put("Gold Coins", 500);
        treasureVault.put("Silver Coins", 1000);
        treasureVault.put("Precious Gems", 50);
        treasureVault.put("Magic Crystals", 25);
        treasureVault.put("Ancient Artifacts", 10);
    }
    
    /**
     * Safely withdraw treasure from the vault (write lock)
     */
    public boolean withdrawTreasure(String treasureType, int amount, String characterName) {
        treasureLock.writeLock().lock();
        try {
            Integer currentAmount = treasureVault.get(treasureType);
            if (currentAmount != null && currentAmount >= amount) {
                treasureVault.put(treasureType, currentAmount - amount);
                System.out.println("💰 " + characterName + " withdrew " + amount + " " + treasureType + 
                                 " from the vault. Remaining: " + treasureVault.get(treasureType));
                return true;
            } else {
                System.out.println("❌ " + characterName + " failed to withdraw " + amount + " " + treasureType + 
                                 " - insufficient funds!");
                return false;
            }
        } finally {
            treasureLock.writeLock().unlock();
        }
    }
    
    /**
     * Safely deposit treasure to the vault (write lock)
     */
    public void depositTreasure(String treasureType, int amount, String characterName) {
        treasureLock.writeLock().lock();
        try {
            treasureVault.merge(treasureType, amount, Integer::sum);
        if (!caveMode) {
            System.out.println("💰 " + characterName + " deposited " + amount + " " + treasureType + 
                             " to the vault. New total: " + treasureVault.get(treasureType));
        }
        } finally {
            treasureLock.writeLock().unlock();
        }
    }
    
    /**
     * View treasure amounts (read lock - multiple readers allowed)
     */
    public String viewTreasureVault(String characterName) {
        treasureLock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder("🏦 " + characterName + " checks the treasure vault:\n");
            treasureVault.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> sb.append("   ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
            return sb.toString();
        } finally {
            treasureLock.readLock().unlock();
        }
    }
    
    // ===============================================
    // GLOBAL MANA POOL METHODS (AtomicInteger)
    // ===============================================
    
    /**
     * Safely consume mana from the global pool
     */
    public boolean consumeMana(int amount, String characterName) {
        int currentMana = globalManaPool.get();
        
        // Atomic compare-and-swap operation
        while (currentMana >= amount) {
            if (globalManaPool.compareAndSet(currentMana, currentMana - amount)) {
                totalManaConsumed.addAndGet(amount);
                System.out.println("✨ " + characterName + " consumed " + amount + " mana. " +
                                 "Global pool: " + globalManaPool.get() + ", Total consumed: " + totalManaConsumed.get());
                return true;
            }
            currentMana = globalManaPool.get(); // Re-read for next attempt
        }
        
        if (!caveMode) {
            System.out.println("❌ " + characterName + " failed to consume " + amount + " mana - insufficient mana!");
        }
        return false;
    }
    
    /**
     * Safely restore mana to the global pool
     */
    public void restoreMana(int amount, String characterName) {
        int newTotal = globalManaPool.addAndGet(amount);
        if (!caveMode) {
            System.out.println("🌟 " + characterName + " restored " + amount + " mana to the global pool. New total: " + newTotal);
        }
    }
    
    public int getGlobalMana() {
        return globalManaPool.get();
    }
    
    public int getTotalManaConsumed() {
        return totalManaConsumed.get();
    }
    
    // ===============================================
    // LOOT QUEUE METHODS (BlockingQueue)
    // ===============================================
    
    /**
     * Take loot from the queue (blocks if empty)
     */
    public String takeLoot(String characterName) throws InterruptedException {
        String loot = lootQueue.take(); // Blocks until item available
        if (!caveMode) {
            if (!caveMode) {
                System.out.println("🎒 " + characterName + " claimed: " + loot + " (Queue size: " + lootQueue.size() + ")");
            }
        }
        return loot;
    }
    
    /**
     * Try to take loot without blocking
     */
    public String tryTakeLoot(String characterName) {
        String loot = lootQueue.poll(); // Non-blocking
        if (loot != null) {
            if (!caveMode) {
                if (!caveMode) {
                    System.out.println("🎒 " + characterName + " quickly grabbed: " + loot + " (Queue size: " + lootQueue.size() + ")");
                }
            }
        }
        return loot;
    }
    
    /**
     * Add loot to the queue
     */
    public boolean addLoot(String loot) {
        boolean added = lootQueue.offer(loot);
        if (added) {
            if (!caveMode) {
                System.out.println("📦 New loot appeared: " + loot + " (Queue size: " + lootQueue.size() + ")");
            }
        }
        return added;
    }
    
    public int getLootQueueSize() {
        return lootQueue.size();
    }
    
    // ===============================================
    // SHARED INVENTORY METHODS (synchronized)
    // ===============================================
    
    /**
     * Add item to shared inventory (synchronized)
     */
    public synchronized void addToSharedInventory(String item, String characterName) {
        synchronized (inventoryLock) {
            sharedInventory.add(item);
            if (!caveMode) {
                System.out.println("📋 " + characterName + " added '" + item + "' to shared inventory. " +
                                 "Total items: " + sharedInventory.size());
            }
        }
    }
    
    /**
     * Remove item from shared inventory (synchronized)
     */
    public synchronized boolean removeFromSharedInventory(String item, String characterName) {
        synchronized (inventoryLock) {
            boolean removed = sharedInventory.remove(item);
            if (removed) {
                if (!caveMode) {
                    System.out.println("📋 " + characterName + " took '" + item + "' from shared inventory. " +
                                     "Remaining items: " + sharedInventory.size());
                }
            } else {
                if (!caveMode) {
                    System.out.println("❌ " + characterName + " couldn't find '" + item + "' in shared inventory.");
                }
            }
            return removed;
        }
    }
    
    /**
     * View shared inventory (synchronized)
     */
    public synchronized List<String> viewSharedInventory(String characterName) {
        synchronized (inventoryLock) {
            List<String> copy = new ArrayList<>(sharedInventory);
            if (!caveMode) {
                System.out.println("👀 " + characterName + " views shared inventory: " + copy.size() + " items");
            }
            return copy;
        }
    }
    
    // ===============================================
    // TRADING POST METHODS (ConcurrentHashMap)
    // ===============================================
    
    private void initializeTradingPost() {
        tradingPost.put("Healing Potion", "Available");
        tradingPost.put("Strength Elixir", "Available");
        tradingPost.put("Magic Scroll", "Available");
        tradingPost.put("Invisibility Cloak", "Available");
    }
    
    /**
     * Trade for an item at the trading post
     */
    public boolean tradeForItem(String itemName, String characterName) {
        String oldStatus = tradingPost.replace(itemName, "Sold to " + characterName);
        if (oldStatus != null && oldStatus.equals("Available")) {
            if (!caveMode) {
                System.out.println("🏦 " + characterName + " successfully traded for " + itemName + "!");
            }
            
            // Restore the item after some time (simulate restocking)
            Thread restockThread = new Thread(() -> {
                try {
                    Thread.sleep(15000); // 15 seconds
                    tradingPost.put(itemName, "Available");
                    if (!caveMode) {
                        System.out.println("🏦 " + itemName + " has been restocked at the trading post!");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            restockThread.start();
            
            return true;
        } else {
            if (!caveMode) {
                System.out.println("❌ " + characterName + " failed to trade for " + itemName + " - not available!");
            }
            return false;
        }
    }
    
    /**
     * View trading post status
     */
    public String viewTradingPost(String characterName) {
        StringBuilder sb = new StringBuilder("🏪 " + characterName + " checks the trading post:\n");
        tradingPost.entrySet().forEach(entry -> 
            sb.append("   ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
        return sb.toString();
    }
    
    // ===============================================
    // RESOURCE GENERATION (Background Threads)
    // ===============================================
    
    private void startResourceGeneration() {
        this.resourceThread = new Thread(() -> {
            while (resourceGenerationActive && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(8000); // Generate resources every 8 seconds
                    
                    // Skip resource generation during cave mode or if interrupted
                    if (caveMode || Thread.currentThread().isInterrupted()) {
                        continue;
                    }
                    
                    // Generate random treasure
                    String[] treasureTypes = {"Gold Coins", "Silver Coins", "Precious Gems", "Magic Crystals"};
                    String treasureType = treasureTypes[random.nextInt(treasureTypes.length)];
                    int amount = random.nextInt(50) + 10;
                    
                    depositTreasure(treasureType, amount, "SYSTEM");
                    
                    // Restore some mana
                    int manaAmount = random.nextInt(100) + 50;
                    restoreMana(manaAmount, "SYSTEM");
                    
                    resourcesGenerated.incrementAndGet();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "ResourceGeneratorThread");
        this.resourceThread.setDaemon(true);
        this.resourceThread.start();
    }
    
    private void startLootGeneration() {
        this.lootThread = new Thread(() -> {
            String[] lootItems = {
                "Enchanted Sword", "Magic Ring", "Health Potion", "Mana Crystal", 
                "Ancient Tome", "Dragon Scale", "Phoenix Feather", "Mystic Gem"
            };
            
            while (resourceGenerationActive && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000); // Generate loot every 5 seconds
                    
                    // Skip loot generation during cave mode or if interrupted
                    if (caveMode || Thread.currentThread().isInterrupted()) {
                        continue;
                    }
                    
                    String loot = lootItems[random.nextInt(lootItems.length)] + " #" + (resourcesGenerated.get() + 1);
                    addLoot(loot);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "LootGeneratorThread");
        this.lootThread.setDaemon(true);
        this.lootThread.start();
    }
    
    /**
     * Stop resource generation
     */
    public void stopResourceGeneration() {
        resourceGenerationActive = false;
    }
    
    /**
     * Get comprehensive resource status
     */
    public String getResourceStatus() {
        StringBuilder sb = new StringBuilder("🌍 GLOBAL RESOURCE STATUS:\n");
        sb.append("   Global Mana Pool: ").append(globalManaPool.get()).append("\n");
        sb.append("   Total Mana Consumed: ").append(totalManaConsumed.get()).append("\n");
        sb.append("   Loot Queue Size: ").append(lootQueue.size()).append("\n");
        sb.append("   Shared Inventory Items: ").append(sharedInventory.size()).append("\n");
        sb.append("   Resources Generated: ").append(resourcesGenerated.get()).append("\n");
        return sb.toString();
    }
}
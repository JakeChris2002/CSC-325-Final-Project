import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

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
    
    // === TRADING POST (using ConcurrentHashMap) ===
    private final ConcurrentHashMap<String, String> tradingPost = new ConcurrentHashMap<>();
    
    private final Random random = new Random();
    
    public SharedResources() {
        initializeTreasures();
        initializeTradingPost();
        startResourceGeneration();
        startLootGeneration();
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
            System.out.println("💎 " + characterName + " deposited " + amount + " " + treasureType + 
                             " to the vault. New total: " + treasureVault.get(treasureType));
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
        
        System.out.println("❌ " + characterName + " failed to consume " + amount + " mana - insufficient mana!");
        return false;
    }
    
    /**
     * Safely restore mana to the global pool
     */
    public void restoreMana(int amount, String characterName) {
        int newTotal = globalManaPool.addAndGet(amount);
        System.out.println("🌟 " + characterName + " restored " + amount + " mana to the global pool. New total: " + newTotal);
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
        System.out.println("🎒 " + characterName + " claimed: " + loot + " (Queue size: " + lootQueue.size() + ")");
        return loot;
    }
    
    /**
     * Try to take loot without blocking
     */
    public String tryTakeLoot(String characterName) {
        String loot = lootQueue.poll(); // Non-blocking
        if (loot != null) {
            System.out.println("🎒 " + characterName + " quickly grabbed: " + loot + " (Queue size: " + lootQueue.size() + ")");
        }
        return loot;
    }
    
    /**
     * Add loot to the queue
     */
    public boolean addLoot(String loot) {
        boolean added = lootQueue.offer(loot);
        if (added) {
            System.out.println("📦 New loot appeared: " + loot + " (Queue size: " + lootQueue.size() + ")");
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
            System.out.println("📋 " + characterName + " added '" + item + "' to shared inventory. " +
                             "Total items: " + sharedInventory.size());
        }
    }
    
    /**
     * Remove item from shared inventory (synchronized)
     */
    public synchronized boolean removeFromSharedInventory(String item, String characterName) {
        synchronized (inventoryLock) {
            boolean removed = sharedInventory.remove(item);
            if (removed) {
                System.out.println("📋 " + characterName + " took '" + item + "' from shared inventory. " +
                                 "Remaining items: " + sharedInventory.size());
            } else {
                System.out.println("❌ " + characterName + " couldn't find '" + item + "' in shared inventory.");
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
            System.out.println("👀 " + characterName + " views shared inventory: " + copy.size() + " items");
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
        String status = tradingPost.replace(itemName, "Available", "Sold to " + characterName);
        if (status != null && status.equals("Available")) {
            System.out.println("🏪 " + characterName + " successfully traded for " + itemName + "!");
            
            // Restore the item after some time (simulate restocking)
            Thread restockThread = new Thread(() -> {
                try {
                    Thread.sleep(15000); // 15 seconds
                    tradingPost.put(itemName, "Available");
                    System.out.println("🏪 " + itemName + " has been restocked at the trading post!");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            restockThread.start();
            
            return true;
        } else {
            System.out.println("❌ " + characterName + " failed to trade for " + itemName + " - not available!");
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
        Thread resourceThread = new Thread(() -> {
            while (resourceGenerationActive) {
                try {
                    Thread.sleep(8000); // Generate resources every 8 seconds
                    
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
        resourceThread.setDaemon(true);
        resourceThread.start();
    }
    
    private void startLootGeneration() {
        Thread lootThread = new Thread(() -> {
            String[] lootItems = {
                "Enchanted Sword", "Magic Ring", "Health Potion", "Mana Crystal", 
                "Ancient Tome", "Dragon Scale", "Phoenix Feather", "Mystic Gem"
            };
            
            while (resourceGenerationActive) {
                try {
                    Thread.sleep(5000); // Generate loot every 5 seconds
                    
                    String loot = lootItems[random.nextInt(lootItems.length)] + " #" + (resourcesGenerated.get() + 1);
                    addLoot(loot);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "LootGeneratorThread");
        lootThread.setDaemon(true);
        lootThread.start();
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
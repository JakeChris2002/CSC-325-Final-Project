import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * GameAnalytics - Lambda expressions and stream operations for logging, filtering, and aggregating results
 * Demonstrates functional programming concepts with arrays and collections
 */
public class GameAnalytics {
    
    // === ARRAYS FOR GAME ELEMENTS ===
    private final String[] enemyTypes = {
        "Shadow Wolf", "Fire Drake", "Ice Troll", "Dark Goblin", "Stone Golem", 
        "Lightning Sprite", "Poison Spider", "Crystal Beast", "Wind Elemental", "Void Wraith"
    };
    
    private final String[] treasureCategories = {
        "Weapons", "Armor", "Potions", "Scrolls", "Gems", "Artifacts", "Tools", "Food"
    };
    
    private final String[] worldLocations = {
        "Enchanted Forest", "Crystal Caves", "Volcanic Peaks", "Frozen Tundra", "Desert Oasis",
        "Ancient Ruins", "Sky Temple", "Underground Caverns", "Mystic Swamp", "Royal Castle"
    };
    
    // === COLLECTIONS FOR DATA TRACKING ===
    private final ConcurrentLinkedQueue<GameEvent> eventLog = new ConcurrentLinkedQueue<>();
    private final List<BattleRecord> battleHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<String>> characterInventories = new HashMap<>();
    private final AtomicLong eventCounter = new AtomicLong(0);
    
    // === LAMBDA EXPRESSIONS FOR VARIOUS OPERATIONS ===
    private final Function<GameEvent, String> eventFormatter = event -> 
        String.format("[%s] %s: %s", 
            event.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            event.characterName, 
            event.description);
    
    private final Predicate<GameEvent> isBattleEvent = event -> 
        event.type == EventType.BATTLE_WON || event.type == EventType.BATTLE_LOST;
    
    private final Predicate<GameEvent> isItemEvent = event -> 
        event.type == EventType.ITEM_FOUND || event.type == EventType.ITEM_STOLEN || event.type == EventType.ITEM_TRADED;
    
    private final Predicate<GameEvent> isMagicEvent = event -> 
        event.type == EventType.SPELL_CAST || event.type == EventType.MANA_CONSUMED || event.type == EventType.ENCHANTMENT;
    
    private final Consumer<String> eventLogger = message -> 
        System.out.println("📊 [ANALYTICS] " + message);
    
    private final BinaryOperator<Integer> sumReducer = Integer::sum;
    
    // === ENUM FOR EVENT TYPES ===
    public enum EventType {
        BATTLE_WON, BATTLE_LOST, ITEM_FOUND, ITEM_STOLEN, ITEM_TRADED, 
        SPELL_CAST, MANA_CONSUMED, ENCHANTMENT, MOVEMENT, QUEST_COMPLETED,
        TREASURE_DEPOSITED, TREASURE_WITHDRAWN, HEALING, INTERACTION
    }
    
    // === DATA CLASSES ===
    public static class GameEvent {
        final String characterName;
        final EventType type;
        final String description;
        final LocalDateTime timestamp;
        final Map<String, Object> metadata;
        
        public GameEvent(String characterName, EventType type, String description) {
            this.characterName = characterName;
            this.type = type;
            this.description = description;
            this.timestamp = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }
        
        public GameEvent withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
    }
    
    public static class BattleRecord {
        final String characterName;
        final String enemyType;
        final boolean won;
        final int damageDealt;
        final int damageReceived;
        final LocalDateTime timestamp;
        
        public BattleRecord(String characterName, String enemyType, boolean won, int damageDealt, int damageReceived) {
            this.characterName = characterName;
            this.enemyType = enemyType;
            this.won = won;
            this.damageDealt = damageDealt;
            this.damageReceived = damageReceived;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    public static class CharacterStats {
        final String name;
        final long battlesWon;
        final long battlesLost;
        final long itemsCollected;
        final long spellsCast;
        final long totalDamageDealt;
        final long totalDamageReceived;
        final double winRate;
        final List<String> favoriteEnemies;
        final List<String> commonItems;
        
        public CharacterStats(String name, long battlesWon, long battlesLost, long itemsCollected, 
                            long spellsCast, long totalDamageDealt, long totalDamageReceived,
                            List<String> favoriteEnemies, List<String> commonItems) {
            this.name = name;
            this.battlesWon = battlesWon;
            this.battlesLost = battlesLost;
            this.itemsCollected = itemsCollected;
            this.spellsCast = spellsCast;
            this.totalDamageDealt = totalDamageDealt;
            this.totalDamageReceived = totalDamageReceived;
            this.winRate = (battlesWon + battlesLost > 0) ? (double) battlesWon / (battlesWon + battlesLost) * 100 : 0;
            this.favoriteEnemies = favoriteEnemies;
            this.commonItems = commonItems;
        }
    }
    
    // === PUBLIC METHODS FOR EVENT LOGGING ===
    
    /**
     * Log a game event using lambda expressions
     */
    public void logEvent(String characterName, EventType type, String description) {
        GameEvent event = new GameEvent(characterName, type, description);
        eventLog.offer(event);
        eventCounter.incrementAndGet();
        
        // Use lambda to format and log
        eventLogger.accept(eventFormatter.apply(event));
    }
    
    /**
     * Log a battle with detailed information
     */
    public void logBattle(String characterName, String enemyType, boolean won, int damageDealt, int damageReceived) {
        BattleRecord battle = new BattleRecord(characterName, enemyType, won, damageDealt, damageReceived);
        battleHistory.add(battle);
        
        EventType eventType = won ? EventType.BATTLE_WON : EventType.BATTLE_LOST;
        String description = String.format("%s vs %s - %s! (Dealt: %d, Received: %d)", 
            characterName, enemyType, won ? "Victory" : "Defeat", damageDealt, damageReceived);
        
        logEvent(characterName, eventType, description);
    }
    
    /**
     * Log item collection with metadata
     */
    public void logItemCollection(String characterName, String itemName, String source) {
        characterInventories.computeIfAbsent(characterName, k -> Collections.synchronizedList(new ArrayList<>()))
                           .add(itemName);
        
        GameEvent event = new GameEvent(characterName, EventType.ITEM_FOUND, 
                                      "Collected: " + itemName + " from " + source)
                                      .withMetadata("item", itemName)
                                      .withMetadata("source", source);
        
        eventLog.offer(event);
        eventLogger.accept(eventFormatter.apply(event));
    }
    
    // === LAMBDA-BASED ANALYTICS METHODS ===
    
    /**
     * Get character statistics using streams and lambda expressions
     */
    public CharacterStats getCharacterStats(String characterName) {
        // Filter events for this character
        List<GameEvent> characterEvents = eventLog.stream()
            .filter(event -> event.characterName.equals(characterName))
            .collect(Collectors.toList());
        
        // Count battles won/lost using streams
        long battlesWon = characterEvents.stream()
            .filter(event -> event.type == EventType.BATTLE_WON)
            .count();
        
        long battlesLost = characterEvents.stream()
            .filter(event -> event.type == EventType.BATTLE_LOST)
            .count();
        
        // Count items collected
        long itemsCollected = characterEvents.stream()
            .filter(isItemEvent)
            .count();
        
        // Count spells cast
        long spellsCast = characterEvents.stream()
            .filter(event -> event.type == EventType.SPELL_CAST)
            .count();
        
        // Calculate damage statistics from battles
        List<BattleRecord> characterBattles = battleHistory.stream()
            .filter(battle -> battle.characterName.equals(characterName))
            .collect(Collectors.toList());
        
        long totalDamageDealt = characterBattles.stream()
            .mapToInt(battle -> battle.damageDealt)
            .reduce(0, Integer::sum);
        
        long totalDamageReceived = characterBattles.stream()
            .mapToInt(battle -> battle.damageReceived)
            .reduce(0, Integer::sum);
        
        // Find favorite enemies (most fought)
        List<String> favoriteEnemies = characterBattles.stream()
            .collect(Collectors.groupingBy(battle -> battle.enemyType, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Find most common items
        List<String> commonItems = characterInventories.getOrDefault(characterName, Collections.emptyList())
            .stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        return new CharacterStats(characterName, battlesWon, battlesLost, itemsCollected, spellsCast,
                                totalDamageDealt, totalDamageReceived, favoriteEnemies, commonItems);
    }
    
    /**
     * Get top performers using stream operations
     */
    public Map<String, Object> getTopPerformers() {
        // Get all unique character names
        Set<String> characterNames = eventLog.stream()
            .map(event -> event.characterName)
            .filter(name -> !name.equals("SYSTEM"))
            .collect(Collectors.toSet());
        
        // Find character with most battles won
        Optional<String> battleChampion = characterNames.stream()
            .max(Comparator.comparingLong(name -> 
                eventLog.stream().filter(e -> e.characterName.equals(name) && e.type == EventType.BATTLE_WON).count()));
        
        // Find character with most items collected
        Optional<String> itemCollector = characterNames.stream()
            .max(Comparator.comparingInt(name -> 
                characterInventories.getOrDefault(name, Collections.emptyList()).size()));
        
        // Find character with most spells cast
        Optional<String> spellmaster = characterNames.stream()
            .max(Comparator.comparingLong(name -> 
                eventLog.stream().filter(e -> e.characterName.equals(name) && e.type == EventType.SPELL_CAST).count()));
        
        Map<String, Object> topPerformers = new HashMap<>();
        battleChampion.ifPresent(name -> topPerformers.put("Battle Champion", name));
        itemCollector.ifPresent(name -> topPerformers.put("Item Collector", name));
        spellmaster.ifPresent(name -> topPerformers.put("Spell Master", name));
        
        return topPerformers;
    }
    
    /**
     * Get filtered event summary using predicates and streams
     */
    public String getEventSummary(Predicate<GameEvent> filter, String category) {
        List<GameEvent> filteredEvents = eventLog.stream()
            .filter(filter)
            .collect(Collectors.toList());
        
        Map<String, Long> eventsByCharacter = filteredEvents.stream()
            .collect(Collectors.groupingBy(
                event -> event.characterName,
                Collectors.counting()
            ));
        
        StringBuilder summary = new StringBuilder();
        summary.append("📈 ").append(category).append(" SUMMARY:\n");
        
        eventsByCharacter.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> 
                summary.append("   ")
                       .append(entry.getKey())
                       .append(": ")
                       .append(entry.getValue())
                       .append(" events\n"));
        
        return summary.toString();
    }
    
    /**
     * Generate comprehensive analytics report using multiple lambda expressions
     */
    public String generateComprehensiveReport() {
        StringBuilder report = new StringBuilder();
        report.append("🔬 COMPREHENSIVE GAME ANALYTICS REPORT\n");
        report.append("=".repeat(50)).append("\n\n");
        
        // Total events
        report.append("📊 OVERALL STATISTICS:\n");
        report.append("   Total Events Logged: ").append(eventCounter.get()).append("\n");
        report.append("   Total Battles: ").append(battleHistory.size()).append("\n");
        
        // Battle statistics using streams
        OptionalDouble avgDamagePerBattle = battleHistory.stream()
            .mapToInt(battle -> battle.damageDealt)
            .average();
        
        avgDamagePerBattle.ifPresent(avg -> 
            report.append("   Average Damage Per Battle: ").append(String.format("%.1f", avg)).append("\n"));
        
        // Most common enemy using stream operations
        battleHistory.stream()
            .collect(Collectors.groupingBy(battle -> battle.enemyType, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .ifPresent(entry -> 
                report.append("   Most Fought Enemy: ").append(entry.getKey())
                      .append(" (").append(entry.getValue()).append(" battles)\n"));
        
        report.append("\n");
        
        // Event summaries using predefined predicates
        report.append(getEventSummary(isBattleEvent, "BATTLE EVENTS"));
        report.append("\n");
        report.append(getEventSummary(isItemEvent, "ITEM EVENTS"));
        report.append("\n");
        report.append(getEventSummary(isMagicEvent, "MAGIC EVENTS"));
        report.append("\n");
        
        // Top performers
        Map<String, Object> topPerformers = getTopPerformers();
        if (!topPerformers.isEmpty()) {
            report.append("🏆 TOP PERFORMERS:\n");
            topPerformers.forEach((category, performer) -> 
                report.append("   ").append(category).append(": ").append(performer).append("\n"));
        }
        
        return report.toString();
    }
    
    /**
     * Get recent events using stream with limit
     */
    public List<String> getRecentEvents(int limit) {
        return eventLog.stream()
            .sorted((a, b) -> b.timestamp.compareTo(a.timestamp)) // Most recent first
            .limit(limit)
            .map(eventFormatter)
            .collect(Collectors.toList());
    }
    
    /**
     * Get events by time range using stream filtering
     */
    public List<GameEvent> getEventsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return eventLog.stream()
            .filter(event -> event.timestamp.isAfter(start) && event.timestamp.isBefore(end))
            .sorted(Comparator.comparing(event -> event.timestamp))
            .collect(Collectors.toList());
    }
    
    // === ARRAY UTILITY METHODS ===
    
    /**
     * Get random enemy type from array
     */
    public String getRandomEnemyType() {
        return enemyTypes[new Random().nextInt(enemyTypes.length)];
    }
    
    /**
     * Get random treasure category from array
     */
    public String getRandomTreasureCategory() {
        return treasureCategories[new Random().nextInt(treasureCategories.length)];
    }
    
    /**
     * Get random world location from array
     */
    public String getRandomWorldLocation() {
        return worldLocations[new Random().nextInt(worldLocations.length)];
    }
    
    /**
     * Get all enemy types (demonstrates array to stream conversion)
     */
    public List<String> getAllEnemyTypes() {
        return Arrays.stream(enemyTypes)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Search enemies by partial name using streams
     */
    public List<String> searchEnemies(String partial) {
        return Arrays.stream(enemyTypes)
            .filter(enemy -> enemy.toLowerCase().contains(partial.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Clear all analytics data
     */
    public void clearAnalytics() {
        eventLog.clear();
        battleHistory.clear();
        characterInventories.clear();
        eventCounter.set(0);
        eventLogger.accept("Analytics data cleared");
    }
}
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * GameWorld - Enhanced narrative system with cohesive fantasy setting
 * "The Shattered Realm": A world where ancient magic has been broken, 
 * and three heroes must work together (or against each other) to restore balance
 */
public class GameWorld {
    
    // === WORLD STATE ===
    private final ReentrantReadWriteLock worldLock = new ReentrantReadWriteLock();
    private final AtomicInteger worldStability = new AtomicInteger(50); // 0-100 scale
    private final AtomicInteger ancientMagicFragments = new AtomicInteger(0);
    private final AtomicInteger corruptionLevel = new AtomicInteger(30);
    
    // === NARRATIVE ELEMENTS ===
    private String currentCrisis = "The Shadow Plague spreads across the eastern villages";
    private final List<String> worldEvents = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> regionStability = new HashMap<>();
    private final Random random = new Random();
    
    // === CAVE MODE SUPPORT ===
    private volatile boolean caveMode = true; // Start silent until explicitly allowed
    
    // === WORLD LOCATIONS ===
    private final String[] ancientSites = {
        "The Sundered Tower of Arcanum", "Whispering Woods Sanctum", "The Cursed Mines of Shadowdeep",
        "Ruins of the Crystal Cathedral", "The Floating Sanctum of Winds", "Dragonbone Catacombs",
        "The Shimmering Lake of Reflections", "Thornwall Monastery", "The Obsidian Spire"
    };
    
    private final String[] corrupted_regions = {
        "Blightmoor Swamps", "The Screaming Desert", "Voidscar Canyon", "Shadowthorn Forest",
        "The Weeping Hills", "Crimson Wasteland", "The Howling Peaks", "Desolation Valley"
    };
    
    private final String[] neutral_territories = {
        "Haven's Rest Village", "Silverbrook Township", "The Great Market of Goldenheart",
        "Moonlight Harbor", "Ironforge Settlement", "Rosewood Hamlet", "Starfall Crossroads"
    };
    
    // === MAGICAL ARTIFACTS (Win Conditions) ===
    private final Map<String, Boolean> artifacts = new HashMap<>();
    private final String[] artifactNames = {
        "Shard of Eternal Dawn", "Crown of the Void Walker", "Heart of the Ancient Dragon",
        "Codex of Forbidden Spells", "Blade of Realm-Cleaving", "Orb of Temporal Mastery"
    };
    
    // === GAME PROGRESSION TRACKING ===
    private int gamePhase = 1; // 1: Discovery, 2: Conflict, 3: Resolution
    private final Set<String> completedQuests = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Integer> characterContributions = new HashMap<>();
    
    public GameWorld() {
        initializeWorld();
        startWorldEvents();
    }
    
    private void initializeWorld() {
        // Initialize region stability
        Arrays.stream(neutral_territories).forEach(region -> regionStability.put(region, 75));
        Arrays.stream(corrupted_regions).forEach(region -> regionStability.put(region, 20));
        Arrays.stream(ancientSites).forEach(region -> regionStability.put(region, 40));
        
        // Initialize artifacts as unfound
        Arrays.stream(artifactNames).forEach(artifact -> artifacts.put(artifact, false));
        
        // Set initial character contributions
        characterContributions.put("Sir Galahad", 0);
        characterContributions.put("Shadowstep", 0);
        characterContributions.put("Arcanum", 0);
        
        logWorldEvent("🌍 THE SHATTERED REALM AWAKENS", 
            "Long ago, the Great Sundering tore apart the magical essence of our world. " +
            "Ancient artifacts lie scattered, corruption spreads, and only brave heroes can restore balance.");
        
        logWorldEvent("⚡ CURRENT CRISIS", currentCrisis);
        logWorldEvent("🎯 THE GREAT QUEST BEGINS", 
            "Three heroes emerge with different paths but a shared destiny. Will they unite or compete?");
    }
    
    // === NARRATIVE LOGGING SYSTEM ===
    
    /**
     * Set cave mode to suppress story messages during exploration
     */
    public void setCaveMode(boolean caveMode) {
        this.caveMode = caveMode;
    }
    
    /**
     * Enhanced narrative logging with rich storytelling
     */
    public void logWorldEvent(String title, String description) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        String formattedEvent = String.format("[%s] %s\n    %s\n", timestamp, title, description);
        
        worldEvents.add(formattedEvent);
        if (!caveMode) {
            System.out.println("🌟 " + formattedEvent);
        }
    }
    
    /**
     * Character-specific narrative logging
     */
    public void logCharacterAction(String characterName, String characterType, String action, String outcome) {
        String location = getCurrentLocation(characterName);
        String narrativeText = generateNarrativeText(characterName, characterType, action, location);
        
        logWorldEvent("📖 " + characterName + " - " + action.toUpperCase(), narrativeText + " " + outcome);
        
        // Update character contributions
        characterContributions.merge(characterName, 1, Integer::sum);
    }
    
    /**
     * Generate rich narrative text based on character actions
     */
    private String generateNarrativeText(String characterName, String characterType, String action, String location) {
        Map<String, List<String>> narrativeTemplates = new HashMap<>();
        
        // Knight narratives
        narrativeTemplates.put("Knight-combat", Arrays.asList(
            "With righteous fury, %s draws their blade in %s, standing against the encroaching darkness.",
            "The stalwart %s charges into battle at %s, their armor gleaming with divine light.",
            "Honor guides %s's hand as they face evil forces threatening %s."
        ));
        
        narrativeTemplates.put("Knight-quest", Arrays.asList(
            "Following ancient oaths, %s embarks on a sacred mission from %s.",
            "%s, champion of the light, seeks to fulfill a noble quest that began in %s.",
            "The noble %s heeds the call of duty, departing from %s on a righteous path."
        ));
        
        // Thief narratives
        narrativeTemplates.put("Thief-stealth", Arrays.asList(
            "Like a shadow dancing between moonbeams, %s moves unseen through %s.",
            "The infamous %s melts into the darkness of %s, leaving no trace of their passage.",
            "Silent as the grave, %s navigates the treacherous paths of %s."
        ));
        
        narrativeTemplates.put("Thief-heist", Arrays.asList(
            "With nimble fingers and sharper wit, %s orchestrates their grandest scheme in %s.",
            "%s, master of the shadows, executes a daring heist that will be whispered about in %s for generations.",
            "The legendary %s weaves through %s like smoke, pursuing treasures beyond imagination."
        ));
        
        // Wizard narratives
        narrativeTemplates.put("Wizard-magic", Arrays.asList(
            "Ancient words of power flow from %s's lips, causing the very air in %s to shimmer with magical energy.",
            "Mystic energies swirl around %s as they channel the raw forces of creation in %s.",
            "The arcane scholar %s weaves spells of incredible complexity, transforming the essence of %s itself."
        ));
        
        narrativeTemplates.put("Wizard-research", Arrays.asList(
            "Surrounded by ancient tomes and glowing crystals, %s delves deep into forbidden knowledge at %s.",
            "The wise %s uncovers secrets lost to time, their studies in %s revealing truths that shake the foundations of reality.",
            "%s's relentless pursuit of arcane wisdom leads to a breakthrough that echoes through the halls of %s."
        ));
        
        // Select appropriate template
        String key = characterType + "-" + categorizeAction(action);
        List<String> templates = narrativeTemplates.getOrDefault(key, Arrays.asList(
            "The %s takes decisive action in %s, their deeds adding to the growing legend.",
            "With determination burning bright, %s forges ahead in %s, shaping the destiny of the realm."
        ));
        
        String template = templates.get(random.nextInt(templates.size()));
        return String.format(template, characterName, location);
    }
    
    private String categorizeAction(String action) {
        if (action.toLowerCase().contains("battle") || action.toLowerCase().contains("combat") || 
            action.toLowerCase().contains("fight")) {
            return "combat";
        } else if (action.toLowerCase().contains("quest") || action.toLowerCase().contains("mission")) {
            return "quest";
        } else if (action.toLowerCase().contains("steal") || action.toLowerCase().contains("sneak") ||
                   action.toLowerCase().contains("hide")) {
            return "stealth";
        } else if (action.toLowerCase().contains("heist") || action.toLowerCase().contains("theft")) {
            return "heist";
        } else if (action.toLowerCase().contains("spell") || action.toLowerCase().contains("magic") ||
                   action.toLowerCase().contains("enchant")) {
            return "magic";
        } else if (action.toLowerCase().contains("research") || action.toLowerCase().contains("study")) {
            return "research";
        }
        return "general";
    }
    
    private String getCurrentLocation(String characterName) {
        // Simple location assignment based on character progression
        int contribution = characterContributions.getOrDefault(characterName, 0);
        
        if (contribution < 5) {
            return neutral_territories[contribution % neutral_territories.length];
        } else if (contribution < 15) {
            return ancientSites[contribution % ancientSites.length];
        } else {
            return corrupted_regions[contribution % corrupted_regions.length];
        }
    }
    
    // === GAME PROGRESSION SYSTEM ===
    
    /**
     * Check and update game progression based on character actions
     */
    public void updateGameProgression() {
        worldLock.writeLock().lock();
        try {
            int totalContributions = characterContributions.values().stream().mapToInt(Integer::intValue).sum();
            
            // Phase progression
            if (gamePhase == 1 && totalContributions >= 15) {
                advanceToPhase2();
            } else if (gamePhase == 2 && totalContributions >= 35) {
                advanceToPhase3();
            } else if (gamePhase == 3 && checkVictoryConditions()) {
                triggerGameEnding();
            }
            
            // World stability changes
            if (totalContributions % 10 == 0 && totalContributions > 0) {
                updateWorldStability();
            }
            
        } finally {
            worldLock.writeLock().unlock();
        }
    }
    
    private void advanceToPhase2() {
        gamePhase = 2;
        currentCrisis = "The Ancient Seals begin to crack, releasing primordial chaos";
        
        logWorldEvent("🔥 PHASE 2: THE AWAKENING", 
            "The heroes' actions have stirred ancient powers! " +
            "Magical artifacts begin to resonate, but darker forces take notice. " +
            "The true challenge begins now.");
        
        // Reveal some artifacts
        int artifactsRevealed = 0;
        for (String artifact : artifactNames) {
            if (artifactsRevealed < 3 && random.nextInt(2) == 0) {
                logWorldEvent("✨ ARTIFACT DISCOVERED", 
                    "The " + artifact + " has been sensed by mystical forces! " +
                    "Its location becomes known to those who seek it.");
                artifactsRevealed++;
            }
        }
    }
    
    private void advanceToPhase3() {
        gamePhase = 3;
        currentCrisis = "The Final Convergence approaches - reality itself hangs in the balance";
        
        logWorldEvent("⚔️ PHASE 3: THE CONVERGENCE", 
            "The Shattered Realm trembles as the final phase begins! " +
            "All artifacts are now active, and the heroes must make their ultimate choice: " +
            "Unite to save the world, or compete for ultimate power?");
        
        // Make all artifacts findable
        Arrays.stream(artifactNames).forEach(artifact -> 
            logWorldEvent("🌟 FINAL ARTIFACT REVEALED", 
                "The " + artifact + " blazes with power, calling to those brave enough to claim it!"));
    }
    
    private boolean checkVictoryConditions() {
        long foundArtifacts = artifacts.values().stream().filter(found -> found).count();
        return foundArtifacts >= 4 || worldStability.get() >= 90 || corruptionLevel.get() <= 5;
    }
    
    private void triggerGameEnding() {
        String winner = determineWinner();
        String endingNarrative = generateEndingNarrative(winner);
        
        logWorldEvent("🏁 THE LEGEND CONCLUDES", endingNarrative);
    }
    
    private String determineWinner() {
        return characterContributions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("The Realm itself");
    }
    
    private String generateEndingNarrative(String winner) {
        Map<String, String> endings = Map.of(
            "Sir Galahad", "Through unwavering honor and righteous deeds, Sir Galahad united the realm under the banner of justice. The Knight's legend echoes through eternity.",
            "Shadowstep", "From the shadows emerged a legend. Shadowstep's cunning and resourcefulness reshaped the world's power structure, bringing balance through unexpected means.",
            "Arcanum", "The wise Arcanum's mastery of ancient magic restored the Shattered Realm's harmony. Knowledge and wisdom triumphed over chaos and destruction."
        );
        
        return endings.getOrDefault(winner, "Through the combined efforts of all heroes, the Shattered Realm found peace. Their unity proved stronger than any individual ambition.");
    }
    
    private void updateWorldStability() {
        int change = random.nextInt(11) - 5; // -5 to +5
        int newStability = Math.max(0, Math.min(100, worldStability.get() + change));
        worldStability.set(newStability);
        
        String stabilityDesc = newStability > 70 ? "flourishing" : 
                              newStability > 40 ? "balanced" : 
                              newStability > 20 ? "unstable" : "chaotic";
        
        logWorldEvent("🌍 WORLD STATE UPDATE", 
            "The realm's stability shifts to " + newStability + "/100 (" + stabilityDesc + "). " +
            "The heroes' actions continue to reshape the world's destiny.");
    }
    
    // === WORLD EVENTS SYSTEM ===
    
    /**
     * Background world events that create dynamic storytelling
     */
    private void startWorldEvents() {
        Thread worldEventThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(20000); // World event every 20 seconds
                    generateRandomWorldEvent();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "WorldEventThread");
        worldEventThread.setDaemon(true);
        worldEventThread.start();
    }
    
    private void generateRandomWorldEvent() {
        String[] eventTypes = {
            "MYSTICAL_PHENOMENON", "POLITICAL_SHIFT", "NATURAL_DISASTER", 
            "ANCIENT_AWAKENING", "MERCHANT_NEWS", "PROPHECY_FULFILLMENT"
        };
        
        String eventType = eventTypes[random.nextInt(eventTypes.length)];
        generateSpecificWorldEvent(eventType);
        updateGameProgression();
    }
    
    private void generateSpecificWorldEvent(String eventType) {
        switch (eventType) {
            case "MYSTICAL_PHENOMENON":
                logWorldEvent("✨ MYSTICAL PHENOMENON", 
                    "Strange lights dance across the sky above " + getRandomLocation() + 
                    ". Scholars debate whether this portends great fortune or terrible doom.");
                break;
                
            case "POLITICAL_SHIFT":
                logWorldEvent("👑 REALM POLITICS", 
                    "Word spreads that the Council of " + getRandomLocation() + 
                    " has issued new decrees regarding magical artifacts. The political landscape shifts.");
                break;
                
            case "NATURAL_DISASTER":
                String location = getRandomLocation();
                logWorldEvent("🌪️ NATURAL UPHEAVAL", 
                    "A great storm ravages " + location + ", but survivors speak of strange magical energies " +
                    "that seemed to protect certain areas. The land itself responds to the heroes' deeds.");
                regionStability.put(location, Math.max(10, regionStability.getOrDefault(location, 50) - 15));
                break;
                
            case "ANCIENT_AWAKENING":
                logWorldEvent("🗿 ANCIENT AWAKENING", 
                    "Deep beneath " + getRandomAncientSite() + ", something stirs. " +
                    "The old magics recognize the changing times and respond to the heroes' presence.");
                break;
                
            case "MERCHANT_NEWS":
                logWorldEvent("💰 MERCHANT TALES", 
                    "Traveling merchants bring news from across the realm. They speak of heroes whose " +
                    "legends grow with each passing day, inspiring hope in these troubled times.");
                break;
                
            case "PROPHECY_FULFILLMENT":
                logWorldEvent("📜 PROPHECY UNFOLDS", 
                    "Ancient prophecies written in the stars begin to manifest. The Shattered Realm's " +
                    "destiny becomes clearer as the heroes' paths converge toward their ultimate fate.");
                break;
        }
    }
    
    private String getRandomLocation() {
        List<String> allLocations = new ArrayList<>();
        allLocations.addAll(Arrays.asList(ancientSites));
        allLocations.addAll(Arrays.asList(corrupted_regions));
        allLocations.addAll(Arrays.asList(neutral_territories));
        return allLocations.get(random.nextInt(allLocations.size()));
    }
    
    private String getRandomAncientSite() {
        return ancientSites[random.nextInt(ancientSites.length)];
    }
    
    // === PUBLIC INTERFACE METHODS ===
    
    /**
     * Report a major character achievement
     */
    public void reportAchievement(String characterName, String achievementType, String description) {
        characterContributions.merge(characterName, 5, Integer::sum); // Major achievements worth more
        
        logWorldEvent("🏆 LEGENDARY DEED", 
            characterName + " has achieved something remarkable! " + description + 
            " This deed will be remembered in the annals of history.");
        
        // Check if this triggers an artifact discovery
        if (random.nextInt(3) == 0) { // 33% chance
            String artifact = findRandomArtifact();
            if (artifact != null) {
                artifacts.put(artifact, true);
                logWorldEvent("🔮 ARTIFACT CLAIMED", 
                    "The " + artifact + " resonates with " + characterName + "'s heroic deed! " +
                    "Ancient power flows to those who prove themselves worthy.");
            }
        }
        
        updateGameProgression();
    }
    
    private String findRandomArtifact() {
        List<String> unclaimedArtifacts = artifacts.entrySet().stream()
            .filter(entry -> !entry.getValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        return unclaimedArtifacts.isEmpty() ? null : 
               unclaimedArtifacts.get(random.nextInt(unclaimedArtifacts.size()));
    }
    
    /**
     * Get current game status for display
     */
    public String getWorldStatus() {
        return String.format(
            "🌍 SHATTERED REALM STATUS:\n" +
            "   Phase: %d/3 (%s)\n" +
            "   World Stability: %d/100\n" +
            "   Corruption Level: %d/100\n" +
            "   Current Crisis: %s\n" +
            "   Artifacts Found: %d/%d\n" +
            "   Hero Contributions: %s",
            gamePhase, getPhaseName(),
            worldStability.get(),
            corruptionLevel.get(),
            currentCrisis,
            artifacts.values().stream().mapToInt(found -> found ? 1 : 0).sum(),
            artifactNames.length,
            characterContributions.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .collect(Collectors.joining(", "))
        );
    }
    
    private String getPhaseName() {
        return switch (gamePhase) {
            case 1 -> "Discovery";
            case 2 -> "Awakening";
            case 3 -> "Convergence";
            default -> "Unknown";
        };
    }
    
    /**
     * Check if the game has ended
     */
    public boolean isGameComplete() {
        return gamePhase > 3 || checkVictoryConditions();
    }
    
    /**
     * Get the list of recent world events for display
     */
    public List<String> getRecentWorldEvents(int count) {
        return worldEvents.stream()
            .skip(Math.max(0, worldEvents.size() - count))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all discovered artifacts
     */
    public Map<String, Boolean> getArtifactStatus() {
        return new HashMap<>(artifacts);
    }
    
    /**
     * Handle character actions in the world
     */
    public void handleCharacterAction(String characterName, String action, String description) {
        worldLock.writeLock().lock();
        try {
            String actionText = characterName + " " + description;
            logWorldEvent("CHARACTER ACTION", actionText);
            
            // Actions can affect world stability
            switch (action.toLowerCase()) {
                case "explore" -> {
                    worldStability.addAndGet(1); // Exploration slightly improves stability
                }
                case "discover" -> {
                    ancientMagicFragments.incrementAndGet(); // Discoveries help restore magic
                    worldStability.addAndGet(2);
                }
                case "combat" -> {
                    corruptionLevel.addAndGet(-1); // Combat reduces corruption
                }
            }
            
            // Ensure bounds
            worldStability.set(Math.max(0, Math.min(100, worldStability.get())));
            corruptionLevel.set(Math.max(0, Math.min(100, corruptionLevel.get())));
            
        } finally {
            worldLock.writeLock().unlock();
        }
    }
    
    /**
     * Display current world state for players
     */
    public void displayCurrentWorldState() {
        worldLock.readLock().lock();
        try {
            System.out.println("=== THE SHATTERED REALM ===");
            System.out.println("World Stability: " + worldStability.get() + "/100");
            System.out.println("Ancient Magic Fragments: " + ancientMagicFragments.get());
            System.out.println("Corruption Level: " + corruptionLevel.get() + "/100");
            System.out.println("Current Crisis: " + currentCrisis);
            System.out.println("Game Phase: " + getPhaseName() + " (" + gamePhase + "/3)");
            
            // Show recent events
            List<String> recentEvents = getRecentWorldEvents(3);
            if (!recentEvents.isEmpty()) {
                System.out.println("Recent Events:");
                recentEvents.forEach(event -> System.out.println("  - " + event));
            }
            System.out.println("===========================");
            
        } finally {
            worldLock.readLock().unlock();
        }
    }
}
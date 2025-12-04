import java.util.*;

public class CaveExplorer {
    private GameCharacter player;
    private List<GameCharacter> party;
    private Scanner scanner;
    private List<String> playerInventory;
    private static final int MAX_INVENTORY = 5;
    
    private int currentRoom = 1;
    private static final int BOSS_ROOM = 10;
    private Map<Integer, List<String>> roomTreasures = new HashMap<>();
    private Map<Integer, String> roomEnemies = new HashMap<>();
    private boolean bossDefeated = false;
    
    public CaveExplorer(GameCharacter player, List<GameCharacter> aiParty, Scanner scanner) {
        this.player = player;
        this.party = new ArrayList<>();
        this.party.add(player);
        this.party.addAll(aiParty);
        this.scanner = scanner;
        this.playerInventory = new ArrayList<>();
        initializeCave();
    }
    
    private void initializeCave() {
        roomTreasures.put(1, Arrays.asList("Health Potion", "Rusty Dagger"));
        roomTreasures.put(2, Arrays.asList("Magic Crystal", "Ancient Coin"));
        roomTreasures.put(3, Arrays.asList("Enchanted Ring", "Cave Map"));
        roomTreasures.put(4, Arrays.asList("Fire Scroll", "Iron Shield"));
        roomTreasures.put(5, Arrays.asList("Mana Potion", "Silver Key"));
        roomTreasures.put(6, Arrays.asList("Lightning Bolt Spell", "Treasure Chest"));
        roomTreasures.put(7, Arrays.asList("Dragon Scale", "Healing Herb"));
        roomTreasures.put(8, Arrays.asList("Elven Bow", "Gemstone"));
        roomTreasures.put(9, Arrays.asList("Master Key", "Power Crystal"));
        roomTreasures.put(10, Arrays.asList("Legendary Sword", "Crown of Victory"));
        
        roomEnemies.put(1, "Cave Rat");
        roomEnemies.put(2, "Goblin Scout");
        roomEnemies.put(3, "Stone Gargoyle");
        roomEnemies.put(4, "Fire Salamander");
        roomEnemies.put(5, "Shadow Wraith");
        roomEnemies.put(6, "Crystal Golem");
        roomEnemies.put(7, "Cave Troll");
        roomEnemies.put(8, "Dark Wizard");
        roomEnemies.put(9, "Dragon Whelp");
        roomEnemies.put(10, "Ancient Guardian (BOSS)");
    }
    
    public boolean exploreCave() {
        System.out.println("\\n🏔️ === ENTERING THE CRYSTAL CAVERNS ===\\n");
        System.out.println("You and your party step into the mysterious caverns.");
        System.out.println("The air is thick with ancient magic and hidden dangers...");
        System.out.println("\\n👥 Your Party:");
        for (GameCharacter character : party) {
            System.out.println("   " + getCharacterIcon(character) + " " + character.getName() + " the " + character.getCharacterType());
        }
        
        while (currentRoom <= BOSS_ROOM && !bossDefeated) {
            exploreCurrentRoom();
            if (bossDefeated) {
                return true;
            }
            if (currentRoom < BOSS_ROOM) {
                offerRoomChoices();
            }
        }
        
        return bossDefeated;
    }
    
    private void exploreCurrentRoom() {
        System.out.println("\\n" + "=".repeat(50));
        System.out.println("🏛️ ROOM " + currentRoom + (currentRoom == BOSS_ROOM ? " - BOSS CHAMBER" : ""));
        System.out.println("=".repeat(50));
        
        if (currentRoom == BOSS_ROOM) {
            exploreBossRoom();
        } else {
            exploreNormalRoom();
        }
    }
    
    private void exploreNormalRoom() {
        System.out.println("🔍 You enter a " + getRoomDescription(currentRoom));
        System.out.println();
        
        String enemy = roomEnemies.get(currentRoom);
        if (enemy != null) {
            System.out.println("⚠️ A " + enemy + " blocks your path!");
            boolean victory = handleCombat(enemy);
            if (!victory) {
                System.out.println("💀 Your party has been defeated! Game Over.");
                return;
            }
        }
        
        List<String> treasures = roomTreasures.get(currentRoom);
        if (treasures != null && !treasures.isEmpty()) {
            System.out.println("✨ You discovered treasures in this room:");
            for (int i = 0; i < treasures.size(); i++) {
                System.out.println("   " + (i + 1) + ". " + treasures.get(i));
            }
            handleTreasureCollection(treasures);
        }
        
        displayInventory();
    }
    
    private void exploreBossRoom() {
        System.out.println("🏛️ You enter the massive boss chamber...");
        System.out.println("Ancient pillars stretch toward a vaulted ceiling covered in glowing crystals.");
        System.out.println("At the center of the room stands the ANCIENT GUARDIAN - a massive stone titan!");
        System.out.println();
        System.out.println("💀 ANCIENT GUARDIAN awakens and prepares for battle!");
        System.out.println("This is the final test of your adventure!");
        
        boolean victory = handleBossFight();
        if (victory) {
            bossDefeated = true;
            System.out.println("\\n🎉 === VICTORY! ===\\n");
            System.out.println("The Ancient Guardian crumbles to dust, revealing the greatest treasures!");
            
            List<String> finalTreasures = roomTreasures.get(10);
            if (finalTreasures != null) {
                System.out.println("🏆 LEGENDARY REWARDS:");
                for (String treasure : finalTreasures) {
                    System.out.println("   ✨ " + treasure);
                    if (playerInventory.size() < MAX_INVENTORY) {
                        playerInventory.add(treasure);
                        System.out.println("     (Added to inventory)");
                    }
                }
            }
        }
    }
    
    private boolean handleCombat(String enemy) {
        System.out.println("\\n⚔️ === COMBAT BEGINS ===\\n");
        System.out.println("🎯 Facing: " + enemy);
        System.out.println("👥 Your party prepares for battle!");
        
        System.out.println("\\nChoose your combat strategy:");
        System.out.println("1. ⚔️ Aggressive Attack (High damage, risky)");
        System.out.println("2. 🛡️ Defensive Strategy (Safe, moderate damage)");
        System.out.println("3. 🔮 Magic Focus (Variable results)");
        System.out.print("\\nEnter your choice (1-3): ");
        
        int strategy = getPlayerChoice(1, 3);
        
        Random random = new Random();
        int playerRoll = random.nextInt(20) + 1;
        int enemyRoll = random.nextInt(20) + 1;
        
        switch (strategy) {
            case 1:
                playerRoll += 3;
                System.out.println("\\n⚔️ " + player.getName() + " leads an aggressive assault!");
                break;
            case 2:
                playerRoll += 1;
                System.out.println("\\n🛡️ Your party forms a defensive formation!");
                break;
            case 3:
                playerRoll += random.nextInt(6);
                System.out.println("\\n🔮 Magical energies surge through the party!");
                break;
        }
        
        for (GameCharacter character : party) {
            playerRoll += 1;
        }
        
        System.out.println("\\n🎲 Combat Resolution:");
        System.out.println("   Your Party Roll: " + playerRoll);
        System.out.println("   " + enemy + " Roll: " + enemyRoll);
        
        if (playerRoll >= enemyRoll) {
            System.out.println("\\n🏆 VICTORY! Your party defeats the " + enemy + "!");
            String reward = getRandomCombatReward();
            System.out.println("💎 You found: " + reward);
            if (playerInventory.size() < MAX_INVENTORY) {
                playerInventory.add(reward);
                System.out.println("   (Added to inventory)");
            } else {
                System.out.println("   (Inventory full - item left behind)");
            }
            return true;
        } else {
            System.out.println("\\n💔 Your party takes heavy damage but manages to retreat!");
            System.out.println("You can try a different approach or continue exploring.");
            return askRetryOrContinue();
        }
    }
    
    private boolean handleBossFight() {
        System.out.println("\\n⚔️ === FINAL BOSS BATTLE ===\\n");
        System.out.println("🏛️ The Ancient Guardian's eyes glow with ancient power!");
        System.out.println("This battle will determine the fate of your quest!");
        
        for (int round = 1; round <= 3; round++) {
            System.out.println("\\n--- ROUND " + round + " ---");
            
            System.out.println("\\nChoose your strategy for round " + round + ":");
            System.out.println("1. ⚔️ Direct Assault (High risk/reward)");
            System.out.println("2. 🛡️ Coordinated Defense (Steady progress)");
            System.out.println("3. 🔮 Ultimate Spell Combo (All or nothing)");
            System.out.println("4. 🏹 Ranged Attacks (Safe but slower)");
            System.out.print("\\nEnter your choice (1-4): ");
            
            int strategy = getPlayerChoice(1, 4);
            
            if (executeBossStrategy(strategy, round)) {
                System.out.println("\\n✨ BOSS DEFEATED! The Ancient Guardian falls!");
                return true;
            }
        }
        
        System.out.println("\\n💀 The Ancient Guardian proves too powerful! Your quest ends here...");
        return false;
    }
    
    private boolean executeBossStrategy(int strategy, int round) {
        Random random = new Random();
        int successThreshold = 15 - (round * 2);
        
        switch (strategy) {
            case 1:
                System.out.println("\\n⚔️ Your party charges directly at the Guardian!");
                int roll1 = random.nextInt(20) + 1 + 5;
                System.out.println("🎲 Attack Roll: " + roll1);
                if (roll1 >= successThreshold) {
                    System.out.println("💥 Critical hit! The Guardian staggers!");
                    return roll1 >= 18;
                }
                break;
            case 2:
                System.out.println("\\n🛡️ Your party works together in perfect harmony!");
                int roll2 = random.nextInt(20) + 1 + 3;
                System.out.println("🎲 Team Roll: " + roll2);
                if (roll2 >= successThreshold) {
                    System.out.println("🤝 Excellent teamwork! You find an opening!");
                    return roll2 >= 16;
                }
                break;
            case 3:
                System.out.println("\\n🔮 All party members channel their magical power!");
                int roll3 = random.nextInt(20) + 1 + random.nextInt(8);
                System.out.println("🎲 Magic Roll: " + roll3);
                if (roll3 >= successThreshold) {
                    System.out.println("✨ Magical energies overwhelm the Guardian!");
                    return roll3 >= 17;
                }
                break;
            case 4:
                System.out.println("\\n🏹 Your party attacks from safe distance!");
                int roll4 = random.nextInt(20) + 1 + 2;
                System.out.println("🎲 Ranged Roll: " + roll4);
                if (roll4 >= successThreshold) {
                    System.out.println("🎯 Precise strikes wear down the Guardian!");
                    return round == 3 && roll4 >= 14;
                }
                break;
        }
        
        System.out.println("💨 The Guardian resists your attack but shows signs of wear!");
        return false;
    }
    
    private void handleTreasureCollection(List<String> treasures) {
        if (treasures.isEmpty()) return;
        
        System.out.println("\\n💰 Would you like to collect treasures?");
        for (String treasure : treasures) {
            System.out.print("\\nTake " + treasure + "? (y/n): ");
            String response = scanner.nextLine().toLowerCase().trim();
            
            if (response.startsWith("y")) {
                if (playerInventory.size() < MAX_INVENTORY) {
                    playerInventory.add(treasure);
                    System.out.println("✅ Added " + treasure + " to inventory!");
                } else {
                    System.out.println("❌ Inventory full! Drop something first? (y/n): ");
                    String dropResponse = scanner.nextLine().toLowerCase().trim();
                    if (dropResponse.startsWith("y")) {
                        dropItemFromInventory();
                        if (playerInventory.size() < MAX_INVENTORY) {
                            playerInventory.add(treasure);
                            System.out.println("✅ Added " + treasure + " to inventory!");
                        }
                    } else {
                        System.out.println("⏭️ Left " + treasure + " behind.");
                    }
                }
            } else {
                System.out.println("⏭️ Left " + treasure + " behind.");
            }
        }
    }
    
    private void dropItemFromInventory() {
        if (playerInventory.isEmpty()) {
            System.out.println("Your inventory is empty!");
            return;
        }
        
        System.out.println("\\n🎒 Choose item to drop:");
        for (int i = 0; i < playerInventory.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + playerInventory.get(i));
        }
        System.out.print("\\nDrop item number (1-" + playerInventory.size() + "): ");
        
        int choice = getPlayerChoice(1, playerInventory.size());
        String droppedItem = playerInventory.remove(choice - 1);
        System.out.println("🗑️ Dropped " + droppedItem + " from inventory.");
    }
    
    private void offerRoomChoices() {
        System.out.println("\\n🚪 === CHOOSE YOUR PATH ===\\n");
        
        if (currentRoom == BOSS_ROOM - 1) {
            System.out.println("You stand before the final chamber. The air thrums with ancient power.");
            System.out.println("1. 🚪 Enter the Boss Chamber (Final Battle)");
            System.out.print("\\nAre you ready for the final battle? (y/n): ");
            
            String response = scanner.nextLine().toLowerCase().trim();
            if (response.startsWith("y")) {
                currentRoom = BOSS_ROOM;
            } else {
                System.out.println("You decide to prepare more before the final battle.");
                System.out.println("\\n📋 Current Status:");
                displayInventory();
                displayPartyStatus();
                System.out.println("\\nPress Enter when ready to continue...");
                scanner.nextLine();
                currentRoom = BOSS_ROOM;
            }
        } else {
            System.out.println("You see multiple paths ahead...");
            System.out.println("1. 🚪 Continue to Room " + (currentRoom + 1));
            if (currentRoom > 1) {
                System.out.println("2. ↩️ Return to Room " + (currentRoom - 1));
            }
            System.out.println("3. 🎒 Manage Inventory");
            System.out.println("4. 👥 Check Party Status");
            
            System.out.print("\\nEnter your choice: ");
            int choice = getPlayerChoice(1, 4);
            
            switch (choice) {
                case 1:
                    currentRoom++;
                    System.out.println("🚶 Moving forward to Room " + currentRoom + "...");
                    break;
                case 2:
                    if (currentRoom > 1) {
                        currentRoom--;
                        System.out.println("↩️ Returning to Room " + currentRoom + "...");
                    }
                    break;
                case 3:
                    manageInventory();
                    break;
                case 4:
                    displayPartyStatus();
                    System.out.println("\\nPress Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    private void manageInventory() {
        System.out.println("\\n🎒 === INVENTORY MANAGEMENT ===\\n");
        displayInventory();
        
        if (!playerInventory.isEmpty()) {
            System.out.println("\\n1. 🗑️ Drop an item");
            System.out.println("2. 📋 Just view inventory");
            System.out.print("\\nEnter choice (1-2): ");
            
            int choice = getPlayerChoice(1, 2);
            if (choice == 1) {
                dropItemFromInventory();
            }
        }
        
        System.out.println("\\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    private void displayInventory() {
        System.out.println("\\n🎒 INVENTORY (" + playerInventory.size() + "/" + MAX_INVENTORY + "):");
        if (playerInventory.isEmpty()) {
            System.out.println("   (Empty)");
        } else {
            for (int i = 0; i < playerInventory.size(); i++) {
                System.out.println("   " + (i + 1) + ". " + playerInventory.get(i));
            }
        }
    }
    
    private void displayPartyStatus() {
        System.out.println("\\n👥 === PARTY STATUS ===\\n");
        for (GameCharacter character : party) {
            System.out.println(getCharacterIcon(character) + " " + character.getName() + " the " + character.getCharacterType());
            System.out.println("   Health: " + character.getHealth() + "/" + character.getMaxHealth());
            if (character instanceof Wizard) {
                System.out.println("   Mana: 100/100");
            }
            System.out.println();
        }
    }
    
    private int getPlayerChoice(int min, int max) {
        int choice = -1;
        while (choice < min || choice > max) {
            try {
                String input = scanner.nextLine().trim();
                choice = Integer.parseInt(input);
                if (choice < min || choice > max) {
                    System.out.print("Invalid choice. Please enter " + min + "-" + max + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter " + min + "-" + max + ": ");
            }
        }
        return choice;
    }
    
    private boolean askRetryOrContinue() {
        System.out.println("\\n1. 🔄 Try fighting again");
        System.out.println("2. ➡️ Continue exploring (bypass enemy)");
        System.out.print("\\nEnter choice (1-2): ");
        
        int choice = getPlayerChoice(1, 2);
        return choice == 1 ? handleCombat(roomEnemies.get(currentRoom)) : true;
    }
    
    private String getRandomCombatReward() {
        String[] rewards = {"Health Potion", "Gold Coins", "Magic Gem", "Ancient Relic", "Enchanted Item", "Mysterious Key"};
        return rewards[new Random().nextInt(rewards.length)];
    }
    
    private String getRoomDescription(int room) {
        String[] descriptions = {
            "",
            "damp cave entrance with moss-covered walls and echoing drips",
            "wider cavern with glowing crystals embedded in the ceiling",
            "ancient chamber with mysterious carvings on the walls",
            "hot volcanic room with lava pools casting eerie red light",
            "misty chamber where shadows seem to move on their own",
            "crystal formation room where light refracts in rainbow patterns",
            "deep underground hall with massive stone pillars",
            "magical library cave filled with floating books and scrolls",
            "treasure vault guarded by ancient magical wards",
            "massive boss chamber with towering ceilings and ancient power"
        };
        return room < descriptions.length ? descriptions[room] : "mysterious chamber";
    }
    
    private String getCharacterIcon(GameCharacter character) {
        if (character instanceof Knight) return "⚔️";
        if (character instanceof Thief) return "🗡️";
        if (character instanceof Wizard) return "🔮";
        return "👤";
    }
}
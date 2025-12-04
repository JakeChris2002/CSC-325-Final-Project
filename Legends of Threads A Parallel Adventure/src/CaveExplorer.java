import java.util.*;

/**
 * CaveExplorer - Enhanced descriptive adventure system
 * Provides rich story descriptions and clear player choices
 */
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
        System.out.println("\n🏔️ === ENTERING THE CRYSTAL CAVERNS ===\n");
        System.out.println("As you step through the ancient stone archway, the warm sunlight behind you");
        System.out.println("fades into cool, mysterious shadows. The air carries whispers of forgotten");
        System.out.println("magic and the distant echo of dripping water. Crystalline formations on the");
        System.out.println("walls pulse with an otherworldly blue glow, providing just enough light to");
        System.out.println("see the path ahead.\n");
        
        System.out.println("👥 Your brave party consists of:");
        for (GameCharacter character : party) {
            System.out.println("   " + getCharacterIcon(character) + " " + character.getName() + 
                             " the " + character.getCharacterType() + " - " + getCharacterDescription(character));
        }
        System.out.println();
        
        while (currentRoom <= BOSS_ROOM && !bossDefeated) {
            exploreCurrentRoom();
            if (bossDefeated) {
                return true;
            }
            if (currentRoom < BOSS_ROOM) {
                offerNavigationChoices();
            }
        }
        
        return bossDefeated;
    }
    
    private void exploreCurrentRoom() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🏛️ " + getRoomTitle(currentRoom));
        System.out.println("=".repeat(60));
        
        if (currentRoom == BOSS_ROOM) {
            exploreBossRoom();
        } else {
            exploreNormalRoom();
        }
    }
    
    private void exploreNormalRoom() {
        System.out.println(getDetailedRoomDescription(currentRoom));
        System.out.println();
        
        // Present room exploration choices without revealing hidden elements
        presentRoomChoices();
    }
    
    private void presentRoomChoices() {
        boolean roomCompleted = false;
        String enemy = roomEnemies.get(currentRoom);
        List<String> treasures = roomTreasures.get(currentRoom);
        boolean enemyDefeated = false;
        boolean treasuresLooted = false;
        boolean enemyDiscovered = false;
        boolean treasuresDiscovered = false;
        
        while (!roomCompleted) {
            System.out.println("\n🤔 What would you like to do?");
            List<String> options = new ArrayList<>();
            
            // Always available options
            options.add("👀 Look around the room carefully");
            options.add("🔍 Search for hidden dangers");
            options.add("💎 Search for valuable items");
            options.add("📋 Check your inventory and party status");
            
            // Enemy-related options (only after discovery)
            if (enemy != null && !enemyDefeated && enemyDiscovered) {
                options.add("⚔️  Approach the " + enemy + " for combat");
                options.add("🤫 Try to sneak past the " + enemy + " quietly");
                options.add("🛡️  Prepare defenses before engaging");
            }
            
            // Treasure-related options (only after discovery)
            if (treasures != null && !treasures.isEmpty() && !treasuresLooted && treasuresDiscovered && (enemy == null || enemyDefeated)) {
                options.add("💰 Collect the discovered treasures");
                options.add("🎯 Choose specific treasures to take");
            }
            
            // Navigation options
            if ((enemy == null || enemyDefeated)) {
                if (currentRoom < BOSS_ROOM) {
                    options.add("➡️  Continue deeper into the caverns");
                } else {
                    options.add("🚪 Approach the final chamber");
                }
            }
            
            // Display options with clear numbering
            for (int i = 0; i < options.size(); i++) {
                System.out.println("   " + (i + 1) + ". " + options.get(i));
            }
            
            System.out.print("\n📝 Choose your action (1-" + options.size() + "): ");
            int choice = getPlayerChoice(1, options.size());
            String selectedAction = options.get(choice - 1);
            
            // Handle the chosen action with detailed descriptions
            if (selectedAction.contains("Look around")) {
                describeSurroundings();
            } else if (selectedAction.contains("Search for hidden dangers")) {
                if (enemy != null && !enemyDiscovered) {
                    System.out.println("\n⚠️ Alert! You discover a " + enemy + " lurking in the shadows!");
                    describeEnemy(enemy);
                    enemyDiscovered = true;
                } else if (enemy == null) {
                    System.out.println("\n✅ You search carefully but find no immediate threats in this chamber.");
                } else {
                    System.out.println("\n👁️ You've already spotted the " + enemy + " in this room.");
                }
            } else if (selectedAction.contains("Search for valuable items")) {
                if (treasures != null && !treasures.isEmpty() && !treasuresDiscovered) {
                    System.out.println("\n✨ Excellent! You discover hidden treasures:");
                    describeTreasureDiscovery(treasures);
                    treasuresDiscovered = true;
                } else if (treasures == null || treasures.isEmpty()) {
                    System.out.println("\n🔍 You search thoroughly but find no valuable items in this chamber.");
                } else {
                    System.out.println("\n💰 You've already found all the treasures in this room.");
                }
            } else if (selectedAction.contains("inventory")) {
                showPartyStatus();
            } else if (selectedAction.contains("Approach") && selectedAction.contains("combat")) {
                System.out.println("\n⚔️ You steel yourself for battle and approach the " + enemy + "...");
                System.out.println("The creature notices your approach and prepares to fight!");
                enemyDefeated = handleCombat(enemy);
                if (!enemyDefeated) {
                    System.out.println("💀 Your party has been defeated! The adventure ends here...");
                    return;
                }
            } else if (selectedAction.contains("sneak")) {
                System.out.println("\n🤫 You motion to your party to move quietly...");
                if (attemptStealth(enemy)) {
                    System.out.println("✅ Success! You manage to slip past the " + enemy + " undetected!");
                    enemyDefeated = true; // Bypassed, not defeated
                } else {
                    System.out.println("❌ The " + enemy + " spots you! Combat is unavoidable!");
                    enemyDefeated = handleCombat(enemy);
                    if (!enemyDefeated) {
                        System.out.println("💀 Your party has been defeated! The adventure ends here...");
                        return;
                    }
                }
            } else if (selectedAction.contains("defenses")) {
                System.out.println("\n🛡️ You take time to prepare your party's defenses and strategy...");
                System.out.println("Your tactical preparation will give you an advantage in the coming fight!");
                enemyDefeated = handleCombat(enemy, true); // Combat with advantage
                if (!enemyDefeated) {
                    System.out.println("💀 Despite your preparations, your party has been defeated!");
                    return;
                }
            } else if (selectedAction.contains("Collect the discovered")) {
                System.out.println("\n💰 You begin collecting the treasures you found...");
                handleTreasureCollection(treasures);
                treasuresLooted = true;
            } else if (selectedAction.contains("Choose specific treasures")) {
                System.out.println("\n🎯 You carefully consider which treasures to take...");
                handleSelectiveTreasureCollection(treasures);
                treasuresLooted = true;
            } else if (selectedAction.contains("Continue") || selectedAction.contains("Approach")) {
                System.out.println("\n🚶 You signal to your party that it's time to move forward...");
                roomCompleted = true;
            }
        }
    }
    
    private String getDetailedRoomDescription(int roomNum) {
        switch (roomNum) {
            case 1:
                return "🕳️ **The Entrance Chamber**\n" +
                       "You find yourself in a vast circular chamber carved from living rock. Stalactites\n" +
                       "hang like ancient teeth from the ceiling, and the floor is covered with smooth\n" +
                       "stones worn by countless years of water flow. To your left and right, narrow\n" +
                       "passages disappear into darkness. Straight ahead, a wider tunnel beckons with\n" +
                       "the promise of deeper mysteries. The air smells of damp earth and old magic.";
            case 2:
                return "✨ **The Crystal Gallery** \n" +
                       "The walls of this elongated chamber are embedded with thousands of tiny crystals\n" +
                       "that catch and reflect your light, creating a dazzling display of colors. The\n" +
                       "floor slopes gently downward, and you can see three distinct paths: one curves\n" +
                       "to the left around a massive crystal formation, another heads straight through\n" +
                       "a natural archway, and a third descends steeply to the right.";
            case 3:
                return "🗿 **The Hall of Statues**\n" +
                       "Ancient stone statues line both sides of this grand hallway. Each depicts a\n" +
                       "different warrior or mage from ages past, their faces weathered but still\n" +
                       "proud. Some hold weapons, others clutch spell components. At the far end,\n" +
                       "you can see the hall splits into two directions: left toward what sounds\n" +
                       "like running water, and right toward a chamber that glows with warm light.";
            case 4:
                return "🔥 **The Forge Chamber**\n" +
                       "Heat radiates from ancient forge fires that still burn with an otherworldly\n" +
                       "flame. Hammers and anvils sit ready for use, though dust shows they haven't\n" +
                       "been touched in decades. The air shimmers with heat, and you see paths\n" +
                       "leading in three directions: north through a steam-filled passage, east\n" +
                       "toward cooler air, and south back toward familiar territory.";
            case 5:
                return "🌫️ **The Mist-Shrouded Cavern**\n" +
                       "A thick, supernatural mist fills this irregular chamber, making it difficult\n" +
                       "to see more than a few feet in any direction. Strange whispers seem to echo\n" +
                       "from the fog, and occasional glimpses of movement suggest you're not alone.\n" +
                       "Through the mist, you can make out passages leading northeast toward clearer\n" +
                       "air, northwest toward a faint blue glow, and southwest toward the sound of\n" +
                       "dripping water.";
            case 6:
                return "💎 **The Gemstone Vault**\n" +
                       "The walls of this chamber are embedded with precious gems of every color -\n" +
                       "rubies, sapphires, emeralds, and diamonds that pulse with inner light.\n" +
                       "The wealth here is staggering, but something feels dangerous about disturbing\n" +
                       "it. You notice paths leading in four directions: north toward what sounds\n" +
                       "like a great waterfall, south toward familiar ground, east up a steep\n" +
                       "incline, and west through a narrow squeeze between gem-covered walls.";
            case 7:
                return "🦴 **The Bone Garden**\n" +
                       "This unsettling chamber is filled with the remains of ancient creatures -\n" +
                       "not just bones, but complete fossilized skeletons of beasts you can't\n" +
                       "identify. Some are enormous, others tiny, all arranged as if this were\n" +
                       "some macabre museum. The paths here lead north toward fresher air, east\n" +
                       "toward a chamber filled with strange music, and west toward what might\n" +
                       "be the sound of something very large breathing.";
            case 8:
                return "📚 **The Ancient Library**\n" +
                       "Towering bookshelves carved directly from the cave walls contain thousands\n" +
                       "of ancient tomes and scrolls. Some books glow softly, others seem to move\n" +
                       "slightly when you're not looking directly at them. A reading area with\n" +
                       "stone tables and chairs sits in the center. From here, passages lead\n" +
                       "north toward what feels like the heart of the mountain, south toward\n" +
                       "familiar territory, and up a spiral staircase carved into the east wall.";
            case 9:
                return "🌟 **The Celestial Observatory**\n" +
                       "The ceiling of this chamber has been carved away to reveal the night sky,\n" +
                       "though strange constellations shine overhead that you don't recognize.\n" +
                       "Ancient astronomical instruments made of brass and crystal fill the room.\n" +
                       "This feels like the antechamber to something greater - a single passage\n" +
                       "leads north toward a chamber that hums with power, while other exits\n" +
                       "lead back to safer, more familiar ground.";
            default:
                return "🏛️ You find yourself in an unremarkable stone chamber with passages\n" +
                       "leading in multiple directions.";
        }
    }
    
    private String getRoomTitle(int roomNum) {
        switch (roomNum) {
            case 1: return "THE ENTRANCE CHAMBER";
            case 2: return "THE CRYSTAL GALLERY";
            case 3: return "THE HALL OF STATUES";
            case 4: return "THE FORGE CHAMBER";
            case 5: return "THE MIST-SHROUDED CAVERN";
            case 6: return "THE GEMSTONE VAULT";
            case 7: return "THE BONE GARDEN";
            case 8: return "THE ANCIENT LIBRARY";
            case 9: return "THE CELESTIAL OBSERVATORY";
            case 10: return "THE GUARDIAN'S SANCTUM - FINAL CHAMBER";
            default: return "CHAMBER " + roomNum;
        }
    }
    
    private void describeSurroundings() {
        System.out.println("\n👁️ You take a moment to carefully observe your surroundings...\n");
        
        // Describe environmental details without revealing hidden elements
        switch (currentRoom) {
            case 1:
                System.out.println("🔍 The entrance chamber shows signs of recent passage - footprints in");
                System.out.println("   the dust suggest others have been here before you. The air currents");
                System.out.println("   indicate multiple passages ahead. Some shadows seem deeper than others.");
                break;
            case 2:
                System.out.println("🔍 The crystals seem to respond to your presence, glowing slightly");
                System.out.println("   brighter. You notice some crystals have been recently harvested.");
                System.out.println("   The play of light creates many hiding spots throughout the chamber.");
                break;
            case 3:
                System.out.println("🔍 The statues' eyes seem to follow your movement. Some bear inscriptions");
                System.out.println("   in ancient languages, possibly warnings or blessings.");
                System.out.println("   The spaces between statues are shrouded in mystery.");
                break;
            case 4:
                System.out.println("🔍 The forge fires cast dancing shadows on the walls. Ancient tools");
                System.out.println("   lie scattered about, and the heat distorts the air making it hard");
                System.out.println("   to see clearly into all corners of the chamber.");
                break;
            case 5:
                System.out.println("🔍 The supernatural mist swirls around you, limiting visibility.");
                System.out.println("   Whispers echo from unseen sources, and shapes move just beyond");
                System.out.println("   the edge of sight. Anything could be hiding in this fog.");
                break;
            default:
                System.out.println("🔍 You notice interesting details about the chamber's construction");
                System.out.println("   and signs of who or what might have passed through recently.");
                System.out.println("   Many areas remain unexplored and could hold secrets.");
        }
        
        System.out.println("\n💡 To discover what this chamber truly holds, you'll need to search more actively.");
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    private void describeEnemy(String enemy) {
        switch (enemy) {
            case "Cave Rat":
                System.out.println("   A large rat with glowing red eyes and unusually sharp teeth.");
                System.out.println("   It seems more aggressive than normal rats, possibly mutated by cave magic.");
                break;
            case "Goblin Scout":
                System.out.println("   A small but cunning goblin wearing crude leather armor.");
                System.out.println("   It carries a rusty dagger and watches you with intelligent malice.");
                break;
            case "Stone Gargoyle":
                System.out.println("   A creature of living stone that blends perfectly with the cave walls.");
                System.out.println("   Its wings are folded, but its claws look razor-sharp.");
                break;
            case "Fire Salamander":
                System.out.println("   A lizard-like creature wreathed in flames, leaving scorch marks on the stone.");
                System.out.println("   Its breath steams in the cool cave air, and embers fall from its scales.");
                break;
            default:
                System.out.println("   A dangerous creature that shouldn't be underestimated.");
        }
    }
    
    private void showPartyStatus() {
        System.out.println("\n👥 === PARTY STATUS ===\n");
        for (GameCharacter character : party) {
            String status = "Healthy";
            if (character.getHealth() < character.getMaxHealth() * 0.3) {
                status = "Badly Wounded";
            } else if (character.getHealth() < character.getMaxHealth() * 0.7) {
                status = "Injured";
            }
            
            System.out.println(getCharacterIcon(character) + " " + character.getName() + " the " + character.getCharacterType());
            System.out.println("   Status: " + status + " (" + character.getHealth() + "/" + character.getMaxHealth() + " HP)");
            System.out.println("   " + getCharacterCurrentState(character));
            System.out.println();
        }
        
        System.out.println("🎒 === YOUR INVENTORY ===");
        if (playerInventory.isEmpty()) {
            System.out.println("   (Empty - " + MAX_INVENTORY + " slots available)");
        } else {
            System.out.println("   (" + playerInventory.size() + "/" + MAX_INVENTORY + " slots used)");
            for (String item : playerInventory) {
                System.out.println("   • " + item);
            }
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    private boolean attemptStealth(String enemy) {
        Random random = new Random();
        int stealthRoll = random.nextInt(20) + 1;
        
        // Thief gets bonus to stealth
        if (player instanceof Thief) {
            stealthRoll += 5;
        }
        
        System.out.println("🎲 Stealth attempt: " + stealthRoll + "/20");
        
        return stealthRoll >= 12; // Base difficulty
    }
    
    private void describeTreasureDiscovery(List<String> treasures) {
        System.out.println("After searching carefully, you discover:");
        for (String treasure : treasures) {
            System.out.println("   ✨ " + treasure + " - " + getTreasureDescription(treasure));
        }
    }
    
    private void describeTreasureDetails(List<String> treasures) {
        System.out.println("You examine each treasure carefully:");
        for (String treasure : treasures) {
            System.out.println("\n📝 " + treasure + ":");
            System.out.println("   " + getTreasureDescription(treasure));
            System.out.println("   " + getTreasureUsefulness(treasure));
        }
    }
    
    private String getTreasureDescription(String treasure) {
        switch (treasure) {
            case "Health Potion": return "A red liquid in a crystal vial that glows with healing energy";
            case "Rusty Dagger": return "An old but still sharp blade with mysterious runes on the hilt";
            case "Magic Crystal": return "A blue crystal that pulses with arcane power";
            case "Ancient Coin": return "A gold coin with an unknown emperor's face, might be valuable";
            case "Enchanted Ring": return "A silver ring that feels warm to the touch";
            case "Cave Map": return "A detailed map showing secret passages through these caverns";
            case "Fire Scroll": return "A scroll containing a powerful fire spell";
            case "Iron Shield": return "A sturdy shield that could protect against enemy attacks";
            default: return "A mysterious item with unknown properties";
        }
    }
    
    private String getTreasureUsefulness(String treasure) {
        switch (treasure) {
            case "Health Potion": return "Could save your life in a tough battle";
            case "Magic Crystal": return "Useful for powering magical abilities";
            case "Cave Map": return "Might reveal shortcuts or hidden areas";
            case "Fire Scroll": return "Devastating against ice or plant enemies";
            case "Iron Shield": return "Provides excellent protection in combat";
            default: return "Might be useful later in your adventure";
        }
    }
    
    private void handleSelectiveTreasureCollection(List<String> treasures) {
        for (String treasure : treasures) {
            System.out.println("\n💰 Would you like to take the " + treasure + "?");
            System.out.println("   (You have " + (MAX_INVENTORY - playerInventory.size()) + " inventory slots remaining)");
            System.out.print("   Take it? (y/n): ");
            
            String response = scanner.nextLine().toLowerCase().trim();
            if (response.startsWith("y")) {
                if (playerInventory.size() < MAX_INVENTORY) {
                    playerInventory.add(treasure);
                    System.out.println("✅ " + treasure + " added to your inventory!");
                } else {
                    System.out.println("❌ Your inventory is full! Drop something first? (y/n): ");
                    String dropResponse = scanner.nextLine().toLowerCase().trim();
                    if (dropResponse.startsWith("y")) {
                        dropItemFromInventory();
                        if (playerInventory.size() < MAX_INVENTORY) {
                            playerInventory.add(treasure);
                            System.out.println("✅ " + treasure + " added to your inventory!");
                        }
                    } else {
                        System.out.println("⏭️ Left " + treasure + " behind.");
                    }
                }
            } else {
                System.out.println("⏭️ You decide to leave the " + treasure + " behind.");
            }
        }
    }
    
    private void offerNavigationChoices() {
        System.out.println("\n🗺️ === CHOOSE YOUR PATH ===\n");
        System.out.println("You've completed this chamber. Where would you like to go next?");
        System.out.println();
        System.out.println("1. ⬆️  Continue forward deeper into the caverns");
        System.out.println("2. ⬅️  Explore a side passage to the left");  
        System.out.println("3. ➡️  Investigate a passage to the right");
        System.out.println("4. 📋 Check party status before deciding");
        System.out.println("5. 💭 Think carefully about your options");
        
        System.out.print("\nWhat is your choice? (1-5): ");
        int choice = getPlayerChoice(1, 5);
        
        switch (choice) {
            case 1:
                currentRoom++;
                System.out.println("\n🚶 Your party moves deeper into the mysterious caverns...");
                break;
            case 2:
                System.out.println("\n🚶 You decide to explore the left passage...");
                // Could lead to the same next room or special areas
                currentRoom++;
                break;
            case 3:
                System.out.println("\n🚶 You choose the right-hand passage...");
                currentRoom++;
                break;
            case 4:
                showPartyStatus();
                offerNavigationChoices(); // Recursive call to choose again
                return;
            case 5:
                System.out.println("\n💭 You take a moment to consider your options carefully...");
                System.out.println("The deeper you go, the more dangerous it becomes, but also");
                System.out.println("the greater the potential rewards. Your party seems ready to continue.");
                offerNavigationChoices(); // Recursive call to choose again
                return;
        }
        
        System.out.println("The adventure continues...\n");
    }
    
    // Boss room and combat methods
    private void exploreBossRoom() {
        System.out.println("🏛️ **THE GUARDIAN'S SANCTUM**\n");
        System.out.println("You enter a massive circular chamber with a domed ceiling that disappears");
        System.out.println("into darkness above. Ancient pillars carved with mystical symbols support");
        System.out.println("the vast space. At the center, on a raised dais, sits an enormous creature");
        System.out.println("made of living crystal and shadow - the Ancient Guardian of the caverns.");
        System.out.println();
        System.out.println("The Guardian's eyes open as you approach, glowing with power accumulated");
        System.out.println("over millennia. This is the final test of your adventure!");
        System.out.println();
        
        bossDefeated = handleBossBattle();
    }
    
    private boolean handleBossBattle() {
        System.out.println("⚔️ === FINAL BATTLE: THE ANCIENT GUARDIAN ===\n");
        
        for (int round = 1; round <= 3; round++) {
            System.out.println("--- ROUND " + round + " ---");
            System.out.println("The Ancient Guardian " + getBossRoundDescription(round));
            System.out.println();
            
            System.out.println("Choose your strategy for round " + round + ":");
            System.out.println("1. ⚔️ Coordinated Attack - All party members strike together");
            System.out.println("2. 🛡️ Defensive Strategy - Protect while looking for weaknesses");
            System.out.println("3. 🔮 Magic Focus - Use all magical abilities and items");
            System.out.println("4. 🎯 Tactical Strike - Target specific weak points");
            System.out.print("\nEnter your choice (1-4): ");
            
            int strategy = getPlayerChoice(1, 4);
            
            if (executeBossStrategy(strategy, round)) {
                System.out.println("\n🎉 === VICTORY! ===");
                System.out.println("The Ancient Guardian's form begins to dissolve into sparkling light!");
                System.out.println("'You have proven yourselves worthy,' its voice echoes as it fades.");
                System.out.println("'Take these treasures as a reward for your courage and teamwork.'");
                return true;
            }
        }
        
        System.out.println("\n💀 The Ancient Guardian's power proves overwhelming...");
        System.out.println("Your party fought valiantly, but this challenge was too great.");
        return false;
    }
    
    private String getBossRoundDescription(int round) {
        switch (round) {
            case 1: return "rises from its throne, crystal formations crackling with energy.";
            case 2: return "unleashes waves of magical force, the chamber shaking with power.";
            case 3: return "glows with desperate fury, this is your final chance!";
            default: return "prepares for battle.";
        }
    }
    
    private boolean executeBossStrategy(int strategy, int round) {
        Random random = new Random();
        int baseRoll = random.nextInt(20) + 1;
        int totalRoll = baseRoll;
        
        switch (strategy) {
            case 1: // Coordinated Attack
                totalRoll += 3 + party.size(); // Bonus for teamwork
                System.out.println("🎲 Teamwork Roll: " + totalRoll + " (base: " + baseRoll + " + teamwork bonus)");
                break;
            case 2: // Defensive Strategy  
                totalRoll += 2;
                System.out.println("🎲 Defense Roll: " + totalRoll + " (base: " + baseRoll + " + defense bonus)");
                break;
            case 3: // Magic Focus
                totalRoll += 4; // High risk, high reward
                System.out.println("🎲 Magic Roll: " + totalRoll + " (base: " + baseRoll + " + magic bonus)");
                break;
            case 4: // Tactical Strike
                totalRoll += 1 + round; // Gets better each round
                System.out.println("🎲 Precision Roll: " + totalRoll + " (base: " + baseRoll + " + tactical bonus)");
                break;
        }
        
        int difficulty = 15 - round; // Gets easier each round
        System.out.println("🎯 Needed: " + difficulty + " or higher");
        
        if (totalRoll >= difficulty) {
            System.out.println("✅ Your strategy succeeds! The Guardian staggers!");
            return round == 3 || totalRoll >= 20; // Win on round 3 or critical success
        } else {
            System.out.println("❌ The Guardian resists, but shows signs of wear!");
            return false;
        }
    }
    
    // Combat system
    private boolean handleCombat(String enemy) {
        return handleCombat(enemy, false);
    }
    
    private boolean handleCombat(String enemy, boolean prepared) {
        System.out.println("\n⚔️ === COMBAT: " + enemy.toUpperCase() + " ===\n");
        
        Random random = new Random();
        int playerRoll = random.nextInt(20) + 1 + party.size();
        int enemyRoll = random.nextInt(20) + 1;
        
        if (prepared) {
            playerRoll += 3;
            System.out.println("🛡️ Your preparation gives you an advantage!");
        }
        
        System.out.println("🎲 Your party's attack: " + playerRoll);
        System.out.println("🎲 " + enemy + "'s defense: " + enemyRoll);
        
        if (playerRoll >= enemyRoll) {
            System.out.println("\n🎉 Victory! You defeat the " + enemy + "!");
            System.out.println(getVictoryDescription(enemy));
            return true;
        } else {
            System.out.println("\n💔 Defeat! The " + enemy + " proves too powerful...");
            return false;
        }
    }
    
    private String getVictoryDescription(String enemy) {
        switch (enemy) {
            case "Cave Rat": return "The oversized rat squeaks once and scurries away into the darkness.";
            case "Goblin Scout": return "The goblin drops its weapon and flees, muttering curses.";
            case "Stone Gargoyle": return "The gargoyle crumbles back into ordinary stone.";
            case "Fire Salamander": return "The salamander's flames dim and it retreats to the deeper caves.";
            default: return "Your enemy is defeated and the path ahead is clear.";
        }
    }
    
    // Utility methods
    private void handleTreasureCollection(List<String> treasures) {
        System.out.println("\n💰 Treasures found in this room:");
        for (String treasure : treasures) {
            System.out.println("   ✨ " + treasure);
        }
        
        System.out.println("\nWould you like to:");
        System.out.println("1. 📦 Take all treasures (if space allows)");
        System.out.println("2. 🎯 Choose specific treasures");
        System.out.println("3. ⏭️ Leave all treasures behind");
        
        System.out.print("Your choice (1-3): ");
        int choice = getPlayerChoice(1, 3);
        
        switch (choice) {
            case 1:
                for (String treasure : treasures) {
                    if (playerInventory.size() < MAX_INVENTORY) {
                        playerInventory.add(treasure);
                        System.out.println("✅ Took " + treasure);
                    } else {
                        System.out.println("❌ No space for " + treasure);
                    }
                }
                break;
            case 2:
                handleSelectiveTreasureCollection(treasures);
                break;
            case 3:
                System.out.println("⏭️ You leave all treasures behind.");
                break;
        }
    }
    
    private void dropItemFromInventory() {
        if (playerInventory.isEmpty()) return;
        
        System.out.println("\n🗑️ Which item would you like to drop?");
        for (int i = 0; i < playerInventory.size(); i++) {
            System.out.println((i + 1) + ". " + playerInventory.get(i));
        }
        
        System.out.print("Drop item (1-" + playerInventory.size() + "): ");
        int choice = getPlayerChoice(1, playerInventory.size());
        String dropped = playerInventory.remove(choice - 1);
        System.out.println("🗑️ Dropped " + dropped + " from inventory.");
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
                System.out.print("Invalid input. Please enter a number " + min + "-" + max + ": ");
            }
        }
        return choice;
    }
    
    private String getCharacterIcon(GameCharacter character) {
        if (character instanceof Knight) return "⚔️";
        if (character instanceof Thief) return "🗡️";  
        if (character instanceof Wizard) return "🔮";
        return "👤";
    }
    
    private String getCharacterDescription(GameCharacter character) {
        if (character instanceof Knight) return "Brave and honorable, skilled in combat";
        if (character instanceof Thief) return "Stealthy and cunning, master of shadows";
        if (character instanceof Wizard) return "Wise and powerful, wielder of ancient magic";
        return "A skilled adventurer";
    }
    
    private String getCharacterCurrentState(GameCharacter character) {
        if (character instanceof Knight) return "Ready to defend the party with sword and shield";
        if (character instanceof Thief) return "Alert and watching for traps or ambushes";
        if (character instanceof Wizard) return "Mentally preparing spells and analyzing magical auras";
        return "Prepared for whatever lies ahead";
    }
}
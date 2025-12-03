import java.util.Random;

/**
 * Wizard character - A wise spellcaster who studies magic and casts spells
 * Specializes in magic and knowledge gathering
 */
public class Wizard extends GameCharacter {
    private int mana;
    private int maxMana;
    private int intelligence;
    private Random random;
    private int spellsCast;
    private boolean isMeditating;
    private int wisdom; // Wizard's accumulated wisdom
    private String currentResearch;
    private boolean inMagicalStorm;
    private int artifactsDiscovered;
    private int apprenticesHelped;
    
    public Wizard(String name, int startX, int startY) {
        super(name, 70, startX, startY, null); // Lower health but has magic
        this.mana = 100;
        this.maxMana = 100;
        this.intelligence = 35;
        this.random = new Random();
        this.spellsCast = 0;
        this.isMeditating = false;
        this.wisdom = 25; // Starting wisdom
        this.currentResearch = "Unraveling the Mysteries of Time Magic";
        this.inMagicalStorm = false;
        this.artifactsDiscovered = 0;
        this.apprenticesHelped = 0;
        addToInventory("Spell Book");
        addToInventory("Magic Staff");
        addToInventory("Crystal Orb");
        System.out.println("🧙 " + name + " the Wizard begins their quest for ultimate magical knowledge!");
    }
    
    @Override
    public void act() {
        if (!isAlive || !isActive) return;
        
        // Wizard's behavior: study, cast spells, meditate, explore
        int action = random.nextInt(4);
        
        switch (action) {
            case 0:
                study();
                break;
            case 1:
                castSpell();
                break;
            case 2:
                meditate();
                break;
            case 3:
                explore();
                break;
        }
    }
    
    @Override
    public void run() {
        System.out.println(name + " the Wizard begins their mystical journey!");
        
        while (isActive && isAlive) {
            try {
                act();
                Thread.sleep(2500); // Wizard acts every 2.5 seconds (slowest, but powerful)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(name + " the Wizard concludes their magical studies.");
    }
    
    private void study() {
        int event = random.nextInt(10);
        
        if (event == 0) { // 10% chance - Major research breakthrough
            wisdom += 20;
            intelligence += 2;
            maxMana += 10;
            mana += 10;
            addToInventory("Forbidden Knowledge Scroll");
            System.out.println("📚 " + name + " achieves breakthrough in '" + currentResearch + "'! Wisdom greatly increased!");
            generateNewResearch();
            
        } else if (event <= 2) { // 20% chance - Discover magical artifact
            artifactsDiscovered++;
            String artifact = "Ancient Artifact #" + artifactsDiscovered;
            addToInventory(artifact);
            System.out.println("🔮 " + name + " uncovers " + artifact + " with mysterious properties!");
            wisdom += 5;
            
        } else if (event <= 4) { // 20% chance - Help an apprentice
            apprenticesHelped++;
            wisdom += 3;
            System.out.println("👨‍🎓 " + name + " mentors a young apprentice in the magical arts! (" + apprenticesHelped + " helped)");
            
        } else if (event == 5) { // 10% chance - Magical storm
            handleMagicalStorm();
            
        } else {
            System.out.println("📖 " + name + " delves deeper into '" + currentResearch + "'");
            wisdom += 1;
        }
    }
    
    private void generateNewResearch() {
        String[] research = {
            "Mastering Dimensional Portal Magic",
            "Deciphering Ancient Dragon Prophecies",
            "Perfecting Immortality Elixirs",
            "Understanding Cosmic Energy Flows",
            "Creating Sentient Magical Constructs"
        };
        currentResearch = research[random.nextInt(research.length)];
        System.out.println("🔬 " + name + " begins new research: '" + currentResearch + "'");
    }
    
    private void handleMagicalStorm() {
        System.out.println("⚡ " + name + " senses a powerful magical storm approaching!");
        
        Thread stormThread = new Thread(() -> {
            try {
                inMagicalStorm = true;
                System.out.println("🌪️ " + name + " is caught in a chaotic magical vortex!");
                Thread.sleep(2500);
                
                if (random.nextInt(wisdom) > 30) { // Wisdom check
                    System.out.println("✨ " + name + " harnesses the storm's power! Mana greatly increased!");
                    mana = maxMana;
                    addToInventory("Storm-Charged Crystal");
                    wisdom += 10;
                } else {
                    System.out.println("💫 " + name + " is overwhelmed by chaotic energies!");
                    mana = Math.max(mana - 30, 0);
                    takeDamage(8);
                }
                
                Thread.sleep(1500);
                System.out.println("🌅 The magical storm subsides...");
                inMagicalStorm = false;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        stormThread.start();
    }
    
    private void castSpell() {
        if (mana >= 20) {
            mana -= 20;
            spellsCast++;
            String spell = generateSpellName();
            System.out.println(name + " casts " + spell + "! Mana: " + mana + "/" + maxMana);
            
            // Different spell effects
            int effect = random.nextInt(3);
            switch (effect) {
                case 0:
                    heal(15);
                    System.out.println("The spell heals " + name + "!");
                    break;
                case 1:
                    System.out.println("The spell creates a protective barrier!");
                    useSpecialAbility();
                    break;
                case 2:
                    // Teleport to random location
                    int newX = random.nextInt(10) - 5 + x;
                    int newY = random.nextInt(10) - 5 + y;
                    move(newX - x, newY - y);
                    System.out.println("The spell teleports " + name + " to (" + x + ", " + y + ")!");
                    break;
            }
        } else {
            System.out.println(name + " is out of mana and cannot cast spells.");
            meditate(); // Auto-meditate if out of mana
        }
    }
    
    private void meditate() {
        if (!isMeditating) {
            isMeditating = true;
            System.out.println(name + " begins deep meditation to restore mana.");
            
            // Create a meditation thread
            Thread meditationThread = new Thread(() -> {
                try {
                    Thread.sleep(3000); // 3 seconds of meditation
                    mana = Math.min(mana + 40, maxMana);
                    isMeditating = false;
                    System.out.println(name + " completes meditation. Mana restored: " + mana + "/" + maxMana);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            meditationThread.start();
        } else {
            System.out.println(name + " continues their peaceful meditation...");
        }
    }
    
    private void explore() {
        // Move to explore magical phenomena
        int direction = random.nextInt(4);
        switch (direction) {
            case 0: move(0, 1); break;  // North
            case 1: move(1, 0); break;  // East
            case 2: move(0, -1); break; // South
            case 3: move(-1, 0); break; // West
        }
        System.out.println(name + " explores mystical energies at (" + x + ", " + y + ")");
        
        if (random.nextInt(5) == 0) { // 20% chance of finding magical item
            addToInventory("Magical Artifact " + (spellsCast + 1));
            System.out.println(name + " discovers a mysterious magical artifact!");
        }
    }
    
    private String generateSpellName() {
        String[] prefixes = {"Mystic", "Arcane", "Divine", "Shadow", "Fire", "Ice", "Lightning"};
        String[] suffixes = {"Bolt", "Shield", "Heal", "Ward", "Blast", "Whisper", "Storm"};
        
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        return prefix + " " + suffix;
    }
    
    @Override
    public void interact(GameCharacter other) {
        if (other instanceof Knight) {
            System.out.println(name + " offers magical assistance to the noble " + other.getName() + ".");
        } else if (other instanceof Thief) {
            System.out.println(name + " senses " + other.getName() + "'s presence despite their stealth.");
        } else {
            System.out.println(name + " shares ancient wisdom with " + other.getName() + ".");
        }
    }
    
    @Override
    public void useSpecialAbility() {
        System.out.println(name + " weaves a powerful magical barrier around themselves!");
        
        // Create a protective spell thread
        Thread protectionThread = new Thread(() -> {
            try {
                System.out.println(name + " is protected by magical energy!");
                Thread.sleep(5000); // 5 seconds of protection
                System.out.println(name + "'s magical barrier dissipates.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        protectionThread.start();
    }
    
    @Override
    public String getCharacterType() {
        return "Wizard";
    }
    
    // Wizard-specific methods
    public void enchant(GameCharacter target) {
        if (target.isAlive() && mana >= 30 && distanceTo(target) <= 2) {
            mana -= 30;
            target.heal(20);
            System.out.println(name + " enchants " + target.getName() + " with beneficial magic!");
        }
    }
    
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public int getIntelligence() { return intelligence; }
    public int getSpellsCast() { return spellsCast; }
    public boolean isMeditating() { return isMeditating; }
}
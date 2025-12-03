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
    
    public Wizard(String name, int startX, int startY) {
        super(name, 70, startX, startY, null); // Lower health but has magic
        this.mana = 100;
        this.maxMana = 100;
        this.intelligence = 35;
        this.random = new Random();
        this.spellsCast = 0;
        this.isMeditating = false;
        addToInventory("Spell Book");
        addToInventory("Magic Staff");
        addToInventory("Crystal Orb");
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
        System.out.println(name + " studies ancient tomes and magical theories.");
        if (random.nextInt(3) == 0) { // 33% chance of learning new spell
            addToInventory("New Spell: " + generateSpellName());
            System.out.println(name + " learns a new magical incantation!");
        }
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
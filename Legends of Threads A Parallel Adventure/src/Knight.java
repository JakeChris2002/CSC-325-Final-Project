import java.util.Random;

/**
 * Knight character - A noble warrior who protects others and seeks combat
 * Specializes in defense and direct combat
 */
public class Knight extends GameCharacter {
    private int armor;
    private int strength;
    private Random random;
    private int questsCompleted;
    
    public Knight(String name, int startX, int startY) {
        super(name, 120, startX, startY, null); // High health
        this.armor = 15;
        this.strength = 20;
        this.random = new Random();
        this.questsCompleted = 0;
        addToInventory("Iron Sword");
        addToInventory("Shield");
    }
    
    @Override
    public void act() {
        if (!isAlive || !isActive) return;
        
        // Knight's behavior: patrol, seek enemies, protect others
        int action = random.nextInt(4);
        
        switch (action) {
            case 0:
                patrol();
                break;
            case 1:
                seekCombat();
                break;
            case 2:
                searchForQuests();
                break;
            case 3:
                rest();
                break;
        }
    }
    
    @Override
    public void run() {
        System.out.println(name + " the Knight begins their noble quest!");
        
        while (isActive && isAlive) {
            try {
                act();
                Thread.sleep(2000); // Knight acts every 2 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(name + " the Knight has ended their watch.");
    }
    
    private void patrol() {
        // Move in a pattern to guard the area
        int direction = random.nextInt(4);
        switch (direction) {
            case 0: move(0, 1); break;  // North
            case 1: move(1, 0); break;  // East
            case 2: move(0, -1); break; // South
            case 3: move(-1, 0); break; // West
        }
        System.out.println(name + " patrols the realm with vigilant eyes at (" + x + ", " + y + ")");
    }
    
    private void seekCombat() {
        System.out.println(name + " searches for worthy opponents to test their mettle.");
        if (random.nextInt(4) == 0) { // 25% chance to find combat
            System.out.println(name + " draws sword, ready for battle!");
            useSpecialAbility();
        }
    }
    
    private void searchForQuests() {
        if (random.nextInt(5) == 0) { // 20% chance
            questsCompleted++;
            addToInventory("Quest Reward " + questsCompleted);
            System.out.println(name + " completes a noble quest and gains honor!");
        } else {
            System.out.println(name + " searches the land for those in need of aid.");
        }
    }
    
    private void rest() {
        heal(5);
        System.out.println(name + " takes a moment to rest and recover strength.");
    }
    
    @Override
    public void interact(GameCharacter other) {
        if (other instanceof Thief) {
            System.out.println(name + " eyes " + other.getName() + " suspiciously, hand on sword hilt.");
        } else if (other instanceof Wizard) {
            System.out.println(name + " respectfully nods to the wise " + other.getName() + ".");
        } else {
            System.out.println(name + " greets " + other.getName() + " with honor.");
        }
    }
    
    @Override
    public void useSpecialAbility() {
        System.out.println(name + " raises shield and enters defensive stance! Armor increased temporarily.");
        armor += 5;
        
        // Create a thread to reduce armor back after some time
        Thread armorBoostThread = new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 seconds
                armor -= 5;
                System.out.println(name + "'s defensive stance ends.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        armorBoostThread.start();
    }
    
    @Override
    public String getCharacterType() {
        return "Knight";
    }
    
    // Knight-specific methods
    public void challenge(GameCharacter opponent) {
        if (opponent.isAlive() && distanceTo(opponent) <= 1) {
            System.out.println(name + " challenges " + opponent.getName() + " to honorable combat!");
            opponent.takeDamage(strength);
        }
    }
    
    public int getArmor() { return armor; }
    public int getStrength() { return strength; }
    public int getQuestsCompleted() { return questsCompleted; }
}
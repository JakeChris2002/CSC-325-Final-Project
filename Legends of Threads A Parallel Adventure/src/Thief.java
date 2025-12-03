import java.util.Random;

/**
 * Thief character - A stealthy character who steals, hides, and moves quickly
 * Specializes in stealth and acquiring items
 */
public class Thief extends GameCharacter {
    private int stealth;
    private int agility;
    private Random random;
    private boolean isHiding;
    private int itemsStolen;
    
    public Thief(String name, int startX, int startY) {
        super(name, 80, startX, startY, null); // Medium health
        this.stealth = 25;
        this.agility = 30;
        this.random = new Random();
        this.isHiding = false;
        this.itemsStolen = 0;
        addToInventory("Lockpicks");
        addToInventory("Throwing Dagger");
    }
    
    @Override
    public void act() {
        if (!isAlive || !isActive) return;
        
        // Thief's behavior: sneak, steal, hide, scout
        int action = random.nextInt(4);
        
        switch (action) {
            case 0:
                sneak();
                break;
            case 1:
                attemptSteal();
                break;
            case 2:
                hide();
                break;
            case 3:
                scout();
                break;
        }
    }
    
    @Override
    public void run() {
        System.out.println(name + " the Thief slips into the shadows...");
        
        while (isActive && isAlive) {
            try {
                act();
                Thread.sleep(1500); // Thief acts every 1.5 seconds (faster than Knight)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(name + " the Thief vanishes into the night.");
    }
    
    private void sneak() {
        // Move quietly and quickly
        int direction = random.nextInt(4);
        switch (direction) {
            case 0: move(0, 1); break;  // North
            case 1: move(1, 0); break;  // East
            case 2: move(0, -1); break; // South
            case 3: move(-1, 0); break; // West
        }
        
        if (random.nextInt(3) == 0) { // 33% chance of extra movement
            direction = random.nextInt(4);
            switch (direction) {
                case 0: move(0, 1); break;
                case 1: move(1, 0); break;
                case 2: move(0, -1); break;
                case 3: move(-1, 0); break;
            }
            System.out.println(name + " moves swiftly and silently to (" + x + ", " + y + ")");
        } else {
            System.out.println(name + " sneaks carefully to (" + x + ", " + y + ")");
        }
    }
    
    private void attemptSteal() {
        if (random.nextInt(3) == 0) { // 33% chance of finding something to steal
            itemsStolen++;
            String stolenItem = "Stolen Treasure " + itemsStolen;
            addToInventory(stolenItem);
            System.out.println(name + " successfully pilfers " + stolenItem + "!");
        } else {
            System.out.println(name + " searches for opportunities but finds nothing of value.");
        }
    }
    
    private void hide() {
        isHiding = !isHiding;
        if (isHiding) {
            System.out.println(name + " melts into the shadows, becoming nearly invisible.");
            useSpecialAbility(); // Activate stealth boost
        } else {
            System.out.println(name + " emerges from hiding, ready for action.");
        }
    }
    
    private void scout() {
        System.out.println(name + " scouts the area, gathering information about surroundings.");
        if (random.nextInt(4) == 0) { // 25% chance of finding useful info
            addToInventory("Secret Information");
            System.out.println(name + " discovers valuable intelligence!");
        }
    }
    
    @Override
    public void interact(GameCharacter other) {
        if (other instanceof Knight) {
            System.out.println(name + " tries to avoid " + other.getName() + "'s watchful gaze.");
        } else if (other instanceof Wizard) {
            System.out.println(name + " shows interest in " + other.getName() + "'s magical trinkets.");
        } else {
            System.out.println(name + " nods silently at " + other.getName() + " from the shadows.");
        }
    }
    
    @Override
    public void useSpecialAbility() {
        System.out.println(name + " activates shadow cloak! Stealth greatly increased.");
        stealth += 10;
        
        // Create a thread to reduce stealth back after some time
        Thread stealthBoostThread = new Thread(() -> {
            try {
                Thread.sleep(4000); // 4 seconds
                stealth -= 10;
                System.out.println(name + "'s shadow cloak fades.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        stealthBoostThread.start();
    }
    
    @Override
    public String getCharacterType() {
        return "Thief";
    }
    
    // Thief-specific methods
    public void pickpocket(GameCharacter target) {
        if (target.isAlive() && distanceTo(target) <= 1 && !target.getInventory().isEmpty()) {
            System.out.println(name + " attempts to pickpocket " + target.getName() + "!");
            if (random.nextInt(2) == 0) { // 50% success rate
                System.out.println("Success! " + name + " steals an item.");
                itemsStolen++;
            } else {
                System.out.println("Failed! " + target.getName() + " notices the attempt.");
            }
        }
    }
    
    public int getStealth() { return stealth; }
    public int getAgility() { return agility; }
    public boolean isHiding() { return isHiding; }
    public int getItemsStolen() { return itemsStolen; }
}
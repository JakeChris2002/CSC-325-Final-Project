# UML Class Diagram - Legends of Threads: A Parallel Adventure

## PlantUML Code

```plantuml
@startuml LegendsOfThreads

' Interfaces
interface Runnable {
    +run(): void
}

' Abstract Classes
abstract class GameCharacter implements Runnable {
    #String name
    #int health
    #int maxHealth
    #int x, y
    #boolean isAlive
    #boolean isActive
    #List<String> inventory
    #SharedResources sharedResources
    #GameAnalytics analytics
    #ReentrantLock characterLock
    #boolean isPlayerControlled
    #volatile String pendingPlayerAction
    #boolean caveMode
    #GameEngine gameEngine
    
    +GameCharacter(String, int, int, int, SharedResources, GameAnalytics)
    {abstract} +act(): void
    {abstract} +interact(GameCharacter): void
    {abstract} +useSpecialAbility(): void
    {abstract} +getCharacterType(): String
    +run(): void
    +move(int, int): void
    +takeDamage(int): void
    +heal(int): void
    +setPlayerControlled(boolean): void
    
    +Character(name: String, health: int, attack: int, defense: int)
    +attack(target: Enemy): void
    +takeDamage(damage: int): void
    +move(location: Location): void
    +useAbility(): void
    {abstract} +specialAbility(): void
    +rest(): void
    +levelUp(): void
    +run(): void
    +isAlive(): boolean
}

abstract class GameObject implements GameEntity {
    #String id
    #String name
    #String description
    
    +GameObject(name: String, description: String)
    +getId(): String
    +getName(): String
    +getDescription(): String
}

abstract class Item extends GameObject {
    #int value
    #ItemType type
    
    +Item(name: String, description: String, value: int)
    {abstract} +use(character: Character): void
}

' Concrete Character Classes
class Knight extends Character {
    -int shieldStrength
    -boolean defendingStance
    
    +Knight(name: String)
    +specialAbility(): void
    +defend(): void
    +shieldBash(target: Enemy): void
}

class Rogue extends Character {
    -int stealthLevel
    -double criticalChance
    
    +Rogue(name: String)
    +specialAbility(): void
    +stealth(): void
    +backstab(target: Enemy): void
    +pickLock(chest: TreasureChest): boolean
}

' Core Engine Classes
class GameEngine {
    -List<GameCharacter> characters
    -List<Thread> characterThreads
    -SharedResources sharedResources
    -GameAnalytics analytics
    -GameWorld gameWorld
    -Scanner scanner
    -boolean gameRunning
    -boolean playerTurn
    -GameCharacter playerCharacter
    -Thread monitorThread
    -List<Quest> activeQuests
    
    +GameEngine()
    +startAdventure(): void
    +createCharacter(int, String): GameCharacter
    +handlePlayerTurn(): void
    +endPlayerTurn(): void
    +handleWorldInteraction(): void
    +generateRandomEncounter(): void
    +shutdownGame(): void
}

class SharedResources {
    -ReentrantReadWriteLock treasureLock
    -ConcurrentHashMap<String, Integer> treasureVault
    -AtomicInteger globalManaPool
    -BlockingQueue<String> lootQueue
    -List<String> sharedInventory
    -Object inventoryLock
    -volatile boolean resourceGenerationActive
    -ConcurrentHashMap<String, String> tradingPost
    
    +SharedResources()
    +withdrawTreasure(String, int, String): boolean
    +depositTreasure(String, int, String): void
    +consumeGlobalMana(int, String): boolean
    +restoreGlobalMana(int, String): void
    +tryTakeLoot(String): String
    +addToSharedInventory(String, String): void
    +setCaveMode(boolean): void
}

class GameAnalytics {
    -ConcurrentLinkedQueue<GameEvent> eventLog
    -List<BattleRecord> battleHistory
    -Map<String, List<String>> characterInventories
    -Function<GameEvent, String> eventFormatter
    -Predicate<GameEvent> isBattleEvent
    -Consumer<String> eventLogger
    
    +GameAnalytics()
    +logEvent(String, EventType, String): void
    +logBattle(String, String, boolean, int, int): void
    +logItemCollection(String, String, String): void
    +getCharacterStats(String): CharacterStats
    +generateDetailedReport(): void
}

class GameWorld {
    -Map<String, Location> locations
    -List<GameCharacter> characters
    -Random random
    
    +GameWorld()
    +addCharacter(GameCharacter): void
    +handleCharacterAction(String, String, String): void
    +getLocationDescription(int, int): String
    +isLocationSafe(int, int): boolean
}

class CaveExplorer {
    -GameCharacter character
    -SharedResources sharedResources
    -Scanner scanner
    -Random random
    -boolean explorationActive
    
    +CaveExplorer(GameCharacter, SharedResources)
    +startExploration(): void
    +exploreRoom(): void
    +handleCombat(): void
    +collectCrystal(): void
}

' Utility Classes
class Quest {
    -String title
    -String description
    -boolean completed
    -boolean accepted
    -int goldReward
    -List<String> itemRewards
    
    +Quest(String, String, int, List<String>)
    +accept(): void
    +complete(): void
    +isAccepted(): boolean
    +isCompleted(): boolean
}

class GameEvent {
    -String characterName
    -EventType type
    -String description
    -LocalDateTime timestamp
    -Map<String, Object> metadata
    
    +GameEvent(String, EventType, String)
    +withMetadata(String, Object): GameEvent
}

class BattleRecord {
    -String characterName
    -String enemyType
    -boolean victory
    -int damageDealt
    -int damageReceived
    -LocalDateTime battleTime
    
    +BattleRecord(String, String, boolean, int, int)
}

' Relationships
GameEngine --> GameCharacter : manages
GameEngine --> SharedResources : coordinates
GameEngine --> GameAnalytics : logs to
GameEngine --> GameWorld : controls
GameCharacter --> SharedResources : accesses
GameCharacter --> GameAnalytics : reports to
Knight --> GameCharacter : extends
Thief --> GameCharacter : extends
Wizard --> GameCharacter : extends
CaveExplorer --> GameCharacter : uses
GameAnalytics --> GameEvent : creates
GameAnalytics --> BattleRecord : stores
    +castSpell(spell: Spell, target: Enemy): void
    +restoreMana(): void
    +areaOfEffect(targets: List<Enemy>): void
}

' World Classes
class GameWorld {
    -static GameWorld instance
    -List<Location> locations
    -List<Character> characters
    -List<Enemy> enemies
    -ExecutorService threadPool
    -ReentrantLock worldLock
    -boolean gameRunning
    
    -GameWorld()
    +static getInstance(): GameWorld
    +initializeWorld(): void
    +addCharacter(character: Character): void
    +startGame(): void
    +stopGame(): void
    +getLocationById(id: String): Location
    +getAllCharacters(): List<Character>
}

class Location extends GameObject {
    -List<Enemy> enemies
    -List<TreasureChest> treasures
    -List<Location> connectedLocations
    -ReentrantLock locationLock
    -int dangerLevel
    
    +Location(name: String, description: String)
    +addEnemy(enemy: Enemy): void
    +removeEnemy(enemy: Enemy): void
    +addTreasure(chest: TreasureChest): void
    +getTreasure(): TreasureChest
    +getRandomEnemy(): Enemy
    +connectTo(location: Location): void
    +getConnectedLocations(): List<Location>
}

' Combat System
class CombatSystem {
    -ReentrantLock combatLock
    
    +CombatSystem()
    +initiateCombat(character: Character, enemy: Enemy): void
    +calculateDamage(attacker: Character, defender: Enemy): int
    +resolveTurn(character: Character, enemy: Enemy): boolean
    +applyCritical(damage: int, chance: double): int
}

class Enemy extends GameObject {
    -int health
    -int maxHealth
    -int attackPower
    -int defense
    -int experienceReward
    -boolean isAlive
    -EnemyType type
    
    +Enemy(name: String, health: int, attack: int)
    +attack(target: Character): void
    +takeDamage(damage: int): void
    +isAlive(): boolean
    +getExperienceReward(): int
}

enum EnemyType {
    GOBLIN
    ORC
    DRAGON
    SKELETON
    SPIDER
}

' Item System
class Inventory {
    -List<Item> items
    -int maxCapacity
    -ReentrantLock inventoryLock
    
    +Inventory(capacity: int)
    +addItem(item: Item): boolean
    +removeItem(item: Item): void
    +hasItem(itemName: String): boolean
    +getItems(): List<Item>
    +isFull(): boolean
}

class TreasureChest extends GameObject {
    -List<Item> contents
    -boolean isLocked
    -boolean isOpened
    -ReentrantLock chestLock
    
    +TreasureChest(name: String)
    +open(character: Character): List<Item>
    +isLocked(): boolean
    +unlock(): void
}

class Weapon extends Item {
    -int damage
    -WeaponType weaponType
    
    +Weapon(name: String, damage: int, type: WeaponType)
    +use(character: Character): void
    +getDamage(): int
}

class Potion extends Item {
    -int healingAmount
    
    +Potion(name: String, healingAmount: int)
    +use(character: Character): void
}

enum ItemType {
    WEAPON
    POTION
    TREASURE
    KEY
}

enum WeaponType {
    SWORD
    DAGGER
    STAFF
}

' Utility Classes
class GameLogger {
    -static GameLogger instance
    -ReentrantLock logLock
    -List<String> eventLog
    
    -GameLogger()
    +static getInstance(): GameLogger
    +logEvent(message: String): void
    +logCombat(attacker: String, defender: String, damage: int): void
    +logMovement(character: String, location: String): void
    +displayLogs(): void
}

class EventHandler {
    -List<Consumer<GameEvent>> listeners
    
    +EventHandler()
    +addEventListener(listener: Consumer<GameEvent>): void
    +triggerEvent(event: GameEvent): void
    +processEvents(): void
}

class GameEvent {
    -String eventType
    -String description
    -long timestamp
    -Character source
    
    +GameEvent(type: String, description: String, source: Character)
    +getEventType(): String
    +getDescription(): String
    +getTimestamp(): long
}

class Spell {
    -String name
    -int manaCost
    -int damage
    -boolean isAreaEffect
    
    +Spell(name: String, manaCost: int, damage: int)
    +cast(caster: Wizard, target: Enemy): void
    +isAreaEffect(): boolean
}

' Main Entry Point
class Main {
    +static main(args: String[]): void
    -static initializeCharacters(): List<Character>
    -static setupWorld(): void
}

' Relationships
GameWorld "1" *-- "many" Location : contains
GameWorld "1" o-- "many" Character : manages
GameWorld "1" o-- "many" Enemy : spawns
Location "1" *-- "many" Enemy : hosts
Location "1" *-- "many" TreasureChest : contains
Character "1" *-- "1" Inventory : has
Inventory "1" o-- "many" Item : stores
TreasureChest "1" o-- "many" Item : contains
Character -- CombatSystem : uses
CombatSystem -- Enemy : targets
Character -- Location : explores
GameLogger -- Character : logs actions
EventHandler -- GameEvent : processes
Wizard "1" o-- "many" Spell : knows
Main -- GameWorld : initializes
Main -- Character : creates

note right of GameWorld
  Singleton pattern
  Manages all game state
  Coordinates threads
end note

note right of Character
  Implements Runnable
  Each instance runs in
  separate thread
end note

note right of CombatSystem
  Uses ReentrantLock
  for thread-safe combat
end note

note right of Inventory
  Thread-safe collection
  using locks
end note

@enduml
```

## Text-Based Class Diagram Representation

### Core Architecture

```
┌─────────────────────────────────────────────────────┐
│                    <<interface>>                     │
│                      Runnable                        │
└─────────────────────────────────────────────────────┘
                          △
                          │
┌─────────────────────────────────────────────────────┐
│                  <<abstract>>                        │
│                   Character                          │
├─────────────────────────────────────────────────────┤
│ # name: String                                       │
│ # health: int                                        │
│ # maxHealth: int                                     │
│ # attackPower: int                                   │
│ # defense: int                                       │
│ # level: int                                         │
│ # currentLocation: Location                          │
│ # inventory: Inventory                               │
│ # isAlive: boolean                                   │
├─────────────────────────────────────────────────────┤
│ + attack(target: Enemy): void                        │
│ + takeDamage(damage: int): void                      │
│ + move(location: Location): void                     │
│ + specialAbility(): void {abstract}                  │
│ + run(): void                                        │
└─────────────────────────────────────────────────────┘
          △              △              △
          │              │              │
    ┌─────┴─────┐   ┌────┴────┐   ┌────┴────┐
    │  Knight   │   │  Rogue  │   │ Wizard  │
    ├───────────┤   ├─────────┤   ├─────────┤
    │-shieldStr │   │-stealth │   │-mana:int│
    │-defending │   │-critChan│   │-spells  │
    ├───────────┤   ├─────────┤   ├─────────┤
    │+defend()  │   │+stealth()│   │+castSp()│
    │+shieldBash│   │+backstab│   │+areaOfE │
    └───────────┘   └─────────┘   └─────────┘
```

### Key Relationships

1. **Inheritance Hierarchy**
   - `Character` (abstract) ← `Knight`, `Rogue`, `Wizard`
   - `GameObject` (abstract) ← `Location`, `Enemy`, `Item`
   - `Item` (abstract) ← `Weapon`, `Potion`

2. **Composition (Strong)**
   - `GameWorld` ◆→ `Location[]`
   - `Character` ◆→ `Inventory`
   - `Location` ◆→ `Enemy[]`, `TreasureChest[]`

3. **Aggregation (Weak)**
   - `GameWorld` ◇→ `Character[]`
   - `Inventory` ◇→ `Item[]`
   - `Wizard` ◇→ `Spell[]`

4. **Usage Dependencies**
   - `Character` → `CombatSystem`
   - `CombatSystem` → `Enemy`
   - `Character` → `Location`
   - `GameLogger` → `Character` events

5. **Interfaces Implemented**
   - `Character` implements `Runnable` (for threading)
   - `Character`, `GameObject` implement `GameEntity`

### Thread Safety Mechanisms

- **ReentrantLock** used in:
  - `GameWorld` (worldLock)
  - `Location` (locationLock)
  - `Inventory` (inventoryLock)
  - `TreasureChest` (chestLock)
  - `CombatSystem` (combatLock)
  - `GameLogger` (logLock)

### Design Patterns

1. **Singleton**: `GameWorld`, `GameLogger`
2. **Observer**: `EventHandler` + `GameEvent`
3. **Strategy**: Combat behaviors in `CombatSystem`
4. **Factory**: Character creation in `Main`
5. **Template Method**: `Character.run()` with abstract `specialAbility()`

## Instructions to Generate Visual Diagram

### Option 1: Online PlantUML Editor
1. Go to http://www.plantuml.com/plantuml/uml/
2. Copy the PlantUML code above
3. Paste it into the editor
4. Download as PNG or SVG

### Option 2: VS Code with PlantUML Extension
1. Install "PlantUML" extension in VS Code
2. Create a file named `UML_Diagram.puml`
3. Paste the PlantUML code
4. Press Alt+D to preview
5. Right-click and export as PNG

### Option 3: Command Line (requires Java and Graphviz)
```bash
# Install PlantUML jar
# Then run:
java -jar plantuml.jar UML_Diagram.puml
```

## Class Responsibilities Summary

| Class | Primary Responsibility | Thread Safety |
|-------|----------------------|---------------|
| `Character` | Abstract base for all playable characters | Runs in own thread |
| `Knight` | Tank character with defense abilities | Thread-safe operations |
| `Rogue` | Fast character with stealth and critical hits | Thread-safe operations |
| `Wizard` | Ranged magic user with mana system | Thread-safe operations |
| `GameWorld` | Singleton managing entire game state | ReentrantLock |
| `Location` | Represents dungeon rooms with enemies/treasures | ReentrantLock |
| `CombatSystem` | Handles all combat calculations | ReentrantLock |
| `Inventory` | Thread-safe item storage | ReentrantLock |
| `Enemy` | AI-controlled monsters | Thread-safe state |
| `GameLogger` | Centralized logging system | ReentrantLock |
| `EventHandler` | Event-driven architecture with lambdas | Consumer<GameEvent> |
| `WeatherAPIService` | Fetches real-time weather data from API | Thread-safe HTTP calls |
| `DynamicEventGenerator` | Generates events based on API data | Async processing |

---

## Method-Level Documentation

### Character (Abstract Class)

#### Constructor
```java
public Character(String name, int health, int attack, int defense)
```
**Purpose**: Initializes a new character with base stats  
**Parameters**:
- `name` - Character's display name
- `health` - Starting and maximum health points
- `attack` - Base attack power for damage calculations
- `defense` - Damage reduction value  
**Thread Safety**: Constructor is not thread-safe; initialize before threading  
**Usage**: Called by subclass constructors (Knight, Rogue, Wizard)

#### run()
```java
public void run()
```
**Purpose**: Main game loop executed when character thread starts  
**Behavior**:
1. Continuously moves through locations while alive
2. Encounters and fights enemies
3. Collects treasures
4. Logs all actions to GameLogger
5. Sleeps between actions to simulate real-time  
**Thread Safety**: Entire method executes in character's dedicated thread  
**Termination**: Exits when `isAlive` becomes false  
**Override**: Implements `Runnable.run()`

#### attack()
```java
public void attack(Enemy target)
```
**Purpose**: Initiates an attack against an enemy  
**Parameters**: `target` - Enemy to attack  
**Returns**: void  
**Side Effects**:
- Calculates damage using CombatSystem
- Reduces target's health
- Logs combat event
- May trigger character death if retaliation kills character  
**Thread Safety**: Uses CombatSystem's ReentrantLock  
**Related**: `CombatSystem.initiateCombat()`

#### takeDamage()
```java
public void takeDamage(int damage)
```
**Purpose**: Applies damage to character after defense calculation  
**Parameters**: `damage` - Raw damage before defense mitigation  
**Algorithm**:
```
actualDamage = max(0, damage - defense)
health = max(0, health - actualDamage)
if (health == 0) isAlive = false
```
**Thread Safety**: Synchronized to prevent concurrent damage application  
**Logging**: Logs damage taken and death events

#### move()
```java
public void move(Location newLocation)
```
**Purpose**: Moves character to a new dungeon location  
**Parameters**: `newLocation` - Destination location  
**Preconditions**: Location must be connected to current location  
**Thread Safety**: Acquires location locks in consistent order to prevent deadlock  
**Side Effects**:
- Updates `currentLocation` field
- Logs movement event
- May trigger location-specific events

#### specialAbility()
```java
public abstract void specialAbility()
```
**Purpose**: Executes character-specific special ability  
**Implementation**: Must be overridden by each character subclass  
**Examples**:
- Knight: Activates defensive stance
- Rogue: Enters stealth mode
- Wizard: Casts area-effect spell  
**Thread Safety**: Implementation-dependent

#### levelUp()
```java
public void levelUp()
```
**Purpose**: Increases character level and improves stats  
**Behavior**:
```
level++
maxHealth += 10
health = maxHealth
attackPower += 5
defense += 2
```
**Trigger**: Called when experience points reach threshold  
**Thread Safety**: Synchronized method  
**Logging**: Logs level-up event with new stats

---

### Knight (Concrete Class)

#### Constructor
```java
public Knight(String name)
```
**Purpose**: Creates a knight with preset tank-focused stats  
**Default Stats**:
- Health: 150
- Attack: 30
- Defense: 25
- Shield Strength: 50  
**Inheritance**: Calls `super(name, 150, 30, 25)`

#### specialAbility()
```java
@Override
public void specialAbility()
```
**Purpose**: Activates defensive stance, doubling defense temporarily  
**Duration**: 3 turns  
**Effect**: `defense *= 2` while active  
**Cooldown**: 10 seconds  
**Thread Safety**: Synchronized on character instance

#### defend()
```java
public void defend()
```
**Purpose**: Blocks incoming attack and reflects damage  
**Behavior**:
- Sets `defendingStance = true`
- Next attack received deals 50% damage
- Reflects 25% damage back to attacker  
**Thread Safety**: Must be called before enemy's attack

#### shieldBash()
```java
public void shieldBash(Enemy target)
```
**Purpose**: Special attack that stuns enemy for one turn  
**Parameters**: `target` - Enemy to bash  
**Damage**: `attackPower + shieldStrength`  
**Effect**: Target cannot attack next turn  
**Mana Cost**: None (cooldown-based)

---

### Rogue (Concrete Class)

#### Constructor
```java
public Rogue(String name)
```
**Purpose**: Creates a rogue with speed and critical-hit focused stats  
**Default Stats**:
- Health: 100
- Attack: 40
- Defense: 10
- Stealth Level: 5
- Critical Chance: 0.30 (30%)

#### specialAbility()
```java
@Override
public void specialAbility()
```
**Purpose**: Enters stealth mode, avoiding next enemy encounter  
**Duration**: Until next combat  
**Effect**: `stealthLevel` increases, bypass enemy detection  
**Cooldown**: 15 seconds

#### stealth()
```java
public void stealth()
```
**Purpose**: Increases stealth level for treasure-finding bonus  
**Effect**: 
- 50% chance to avoid combat
- +20% treasure finding rate  
**Duration**: 30 seconds

#### backstab()
```java
public void backstab(Enemy target)
```
**Purpose**: Critical strike with guaranteed critical hit  
**Parameters**: `target` - Enemy to backstab  
**Damage**: `attackPower * 3.0`  
**Condition**: Only works if in stealth mode  
**Side Effect**: Exits stealth after use

#### pickLock()
```java
public boolean pickLock(TreasureChest chest)
```
**Purpose**: Attempts to unlock a locked treasure chest  
**Parameters**: `chest` - Chest to unlock  
**Returns**: `true` if successful, `false` if failed  
**Success Rate**: `baseChance + (stealthLevel * 0.1)`  
**Thread Safety**: Acquires chest's ReentrantLock

---

### Wizard (Concrete Class)

#### Constructor
```java
public Wizard(String name)
```
**Purpose**: Creates a wizard with mana and spell-casting abilities  
**Default Stats**:
- Health: 80
- Attack: 50
- Defense: 5
- Mana: 100
- Max Mana: 100

#### specialAbility()
```java
@Override
public void specialAbility()
```
**Purpose**: Casts most powerful spell in spell list  
**Behavior**: Selects highest-damage spell with sufficient mana  
**Target**: Nearest enemy  
**Fallback**: Basic attack if insufficient mana

#### castSpell()
```java
public void castSpell(Spell spell, Enemy target)
```
**Purpose**: Casts a specific spell at target enemy  
**Parameters**:
- `spell` - Spell to cast
- `target` - Enemy target  
**Preconditions**: `mana >= spell.getManaCost()`  
**Effects**:
- Reduces mana by spell cost
- Deals spell damage to target
- Logs spell cast event  
**Thread Safety**: Synchronized on wizard instance

#### restoreMana()
```java
public void restoreMana()
```
**Purpose**: Regenerates mana over time  
**Effect**: `mana = min(maxMana, mana + 10)`  
**Called**: During `rest()` method or periodically in game loop  
**Thread Safety**: Synchronized

#### areaOfEffect()
```java
public void areaOfEffect(List<Enemy> targets)
```
**Purpose**: Casts spell affecting multiple enemies  
**Parameters**: `targets` - List of enemies in range  
**Damage**: `attackPower * 0.7` per target  
**Mana Cost**: `30 + (5 * targets.size())`  
**Max Targets**: 5  
**Thread Safety**: Uses lambda to apply damage: `targets.forEach(e -> e.takeDamage(...))`

---

### GameWorld (Singleton)

#### getInstance()
```java
public static synchronized GameWorld getInstance()
```
**Purpose**: Returns singleton instance of game world  
**Pattern**: Thread-safe lazy initialization  
**Returns**: The single GameWorld instance  
**Thread Safety**: Synchronized method ensures one instance

#### initializeWorld()
```java
public void initializeWorld()
```
**Purpose**: Sets up initial game state  
**Behavior**:
1. Creates all locations and connects them
2. Spawns initial enemies in locations
3. Places treasure chests
4. Initializes weather service
5. Sets `gameRunning = true`  
**Called**: Once at game start from `Main.main()`  
**Thread Safety**: Acquires `worldLock`

#### addCharacter()
```java
public void addCharacter(Character character)
```
**Purpose**: Registers a character to the game world  
**Parameters**: `character` - Character to add  
**Side Effects**:
- Adds to `characters` list
- Assigns starting location
- Prepares character thread for execution  
**Thread Safety**: Synchronized on world lock

#### startGame()
```java
public void startGame()
```
**Purpose**: Begins game execution by starting all character threads  
**Behavior**:
```java
characters.forEach(c -> c.getThread().start())
weatherService.start()
```
**Thread Safety**: Uses ExecutorService for thread pool management  
**Blocking**: Returns immediately; game runs in background threads

#### stopGame()
```java
public void stopGame()
```
**Purpose**: Gracefully shuts down all game threads  
**Behavior**:
1. Sets `gameRunning = false`
2. Interrupts all character threads
3. Waits for threads to terminate
4. Shuts down ExecutorService
5. Logs final game statistics  
**Thread Safety**: Acquires world lock, then safely terminates threads

---

### Location

#### addEnemy()
```java
public synchronized void addEnemy(Enemy enemy)
```
**Purpose**: Spawns an enemy in this location  
**Parameters**: `enemy` - Enemy to add  
**Thread Safety**: Synchronized method  
**Side Effects**: Logs enemy spawn event

#### getTreasure()
```java
public synchronized TreasureChest getTreasure()
```
**Purpose**: Retrieves and removes one treasure chest from location  
**Returns**: TreasureChest if available, null otherwise  
**Thread Safety**: Synchronized ensures only one character gets each chest  
**Pattern**: First-come, first-served access

#### getRandomEnemy()
```java
public Enemy getRandomEnemy()
```
**Purpose**: Selects a random living enemy for combat encounter  
**Returns**: Random Enemy from enemies list, or null if empty  
**Algorithm**: Uses `Random.nextInt(enemies.size())`  
**Thread Safety**: Uses parallel stream with filter: `enemies.stream().filter(Enemy::isAlive)`

---

### CombatSystem

#### initiateCombat()
```java
public void initiateCombat(Character character, Enemy enemy)
```
**Purpose**: Orchestrates turn-based combat between character and enemy  
**Parameters**:
- `character` - Player character
- `enemy` - Enemy combatant  
**Algorithm**:
```
while (character.isAlive() && enemy.isAlive()) {
    characterTurn()
    if (enemy.isAlive()) enemyTurn()
    sleep(1000) // 1 second per turn
}
```
**Thread Safety**: Acquires `combatLock` for entire combat duration  
**Returns**: void (updates character/enemy state)

#### calculateDamage()
```java
public int calculateDamage(Character attacker, Enemy defender)
```
**Purpose**: Calculates final damage after defense and critical hits  
**Parameters**:
- `attacker` - Character dealing damage
- `defender` - Enemy receiving damage  
**Formula**:
```
baseDamage = attacker.getAttackPower()
if (isCritical()) baseDamage *= 2
finalDamage = max(1, baseDamage - defender.getDefense())
```
**Returns**: Final damage integer  
**Thread Safety**: Pure function, no shared state

#### applyCritical()
```java
public int applyCritical(int damage, double chance)
```
**Purpose**: Applies critical hit multiplier based on chance  
**Parameters**:
- `damage` - Base damage
- `chance` - Probability of critical (0.0 to 1.0)  
**Returns**: Damage * 2 if critical, otherwise damage unchanged  
**Used By**: Rogue's backstab, random combat events

---

### Inventory

#### addItem()
```java
public boolean addItem(Item item)
```
**Purpose**: Adds item to inventory if space available  
**Parameters**: `item` - Item to add  
**Returns**: `true` if added, `false` if inventory full  
**Thread Safety**: Acquires `inventoryLock`  
**Capacity Check**: `items.size() < maxCapacity`

#### hasItem()
```java
public boolean hasItem(String itemName)
```
**Purpose**: Checks if inventory contains item by name  
**Parameters**: `itemName` - Name to search for  
**Returns**: `true` if found  
**Implementation**: Uses stream with lambda:
```java
items.stream().anyMatch(item -> item.getName().equals(itemName))
```

---

### WeatherAPIService (API Integration)

#### fetchWeatherAsync()
```java
public CompletableFuture<WeatherData> fetchWeatherAsync()
```
**Purpose**: Asynchronously fetches weather data from Open-Meteo API  
**Returns**: CompletableFuture containing WeatherData  
**API Endpoint**: `https://api.open-meteo.com/v1/forecast?latitude=40.7128&longitude=-74.0060&current_weather=true`  
**Thread Safety**: Non-blocking async operation  
**Error Handling**: Returns default weather data if API call fails  
**Implementation**:
```java
HttpClient client = HttpClient.newHttpClient()
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(API_URL))
    .GET()
    .build()
return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenApply(this::parseWeatherResponse)
```

#### parseWeatherResponse()
```java
private WeatherData parseWeatherResponse(HttpResponse<String> response)
```
**Purpose**: Parses JSON response into WeatherData object  
**Parameters**: `response` - HTTP response with JSON body  
**Returns**: WeatherData object  
**JSON Parsing**:
```java
JSONObject json = new JSONObject(response.body())
JSONObject current = json.getJSONObject("current_weather")
double temp = current.getDouble("temperature")
int weatherCode = current.getInt("weathercode")
```
**Library**: Uses org.json library

#### run()
```java
@Override
public void run()
```
**Purpose**: Continuously fetches weather data in background thread  
**Frequency**: Every 60 seconds  
**Behavior**:
```
while (gameRunning) {
    fetchWeatherAsync().thenAccept(this::applyWeatherEffects)
    Thread.sleep(60000)
}
```
**Thread**: Implements Runnable, runs in separate thread

#### applyWeatherEffects()
```java
private void applyWeatherEffects(WeatherData weather)
```
**Purpose**: Applies weather-based modifiers to game world  
**Parameters**: `weather` - Current weather data  
**Effects**:
- Cold weather: Reduce character movement speed
- Rain: Increase enemy spawn rate
- Storm: Spawn special boss enemy
- Clear: Increase treasure spawn rate  
**Thread Safety**: Acquires GameWorld lock before modifications

---

### DynamicEventGenerator

#### generateEventFromWeather()
```java
public GameEvent generateEventFromWeather(WeatherData weather)
```
**Purpose**: Creates game events based on weather conditions  
**Parameters**: `weather` - Current weather data from API  
**Returns**: GameEvent object  
**Logic**:
```java
if (weather.isStormy()) {
    return new GameEvent("STORM", "A powerful storm summons ancient enemies!")
} else if (weather.isCold()) {
    return new GameEvent("FROST", "Freezing temperatures slow movement")
}
```
**Uses**: Lambda expressions and Stream API for event processing

---

### GameLogger (Singleton)

#### logEvent()
```java
public synchronized void logEvent(String message)
```
**Purpose**: Thread-safe logging of game events  
**Parameters**: `message` - Event description  
**Format**: `[HH:mm:ss] message`  
**Thread Safety**: Synchronized method with ReentrantLock  
**Storage**: Adds to concurrent `eventLog` list

#### logCombat()
```java
public void logCombat(String attacker, String defender, int damage)
```
**Purpose**: Specialized logging for combat events  
**Parameters**:
- `attacker` - Name of attacker
- `defender` - Name of defender
- `damage` - Damage dealt  
**Output**: `"[12:34:56] Knight attacks Goblin for 25 damage"`

#### displayLogs()
```java
public void displayLogs()
```
**Purpose**: Prints all logged events to console  
**Implementation**: Uses stream and lambda:
```java
eventLog.stream()
    .forEach(System.out::println)
```

---

### Main

#### main()
```java
public static void main(String[] args)
```
**Purpose**: Entry point of application  
**Execution Flow**:
1. Initialize GameWorld singleton
2. Create three characters (Knight, Rogue, Wizard)
3. Set up locations and connections
4. Start weather service thread
5. Start character threads
6. Wait for game completion
7. Display final statistics  
**Thread Coordination**: Uses `Thread.join()` to wait for all characters

#### initializeCharacters()
```java
private static List<Character> initializeCharacters()
```
**Purpose**: Factory method to create playable characters  
**Returns**: List of Character objects  
**Implementation**:
```java
List<Character> chars = new ArrayList<>();
chars.add(new Knight("Sir Galahad"));
chars.add(new Rogue("Shadow"));
chars.add(new Wizard("Merlin"));
return chars;
```
**Pattern**: Factory pattern with lambda for custom initialization

---

## Lambda Expression Usage Examples

### Event Handling
```java
eventHandler.addEventListener(event -> {
    if (event.getType().equals("TREASURE_FOUND")) {
        gameLogger.logEvent(event.getDescription());
    }
});
```

### Parallel Stream Processing
```java
// Find all living enemies across all locations
List<Enemy> allEnemies = locations.parallelStream()
    .flatMap(loc -> loc.getEnemies().stream())
    .filter(Enemy::isAlive)
    .collect(Collectors.toList());
```

### Combat Damage Calculation
```java
// Apply damage to multiple targets with lambda
targets.forEach(enemy -> {
    int damage = calculateDamage(character, enemy);
    enemy.takeDamage(damage);
    gameLogger.logCombat(character.getName(), enemy.getName(), damage);
});
```

### Asynchronous API Processing
```java
weatherService.fetchWeatherAsync()
    .thenApply(weather -> processWeather(weather))
    .thenAccept(effects -> applyToGameWorld(effects))
    .exceptionally(ex -> {
        gameLogger.logEvent("Weather API failed: " + ex.getMessage());
        return null;
    });
```

---

## Thread Safety Patterns

### ReentrantLock Usage
```java
// In Location class
public TreasureChest getTreasure() {
    locationLock.lock();
    try {
        if (treasures.isEmpty()) return null;
        return treasures.remove(0);
    } finally {
        locationLock.unlock(); // Always unlock in finally
    }
}
```

### Synchronized Method
```java
// In Inventory class
public synchronized boolean addItem(Item item) {
    if (isFull()) return false;
    items.add(item);
    return true;
}
```

### Concurrent Collection
```java
// In GameWorld class
private ConcurrentHashMap<String, Location> locations = new ConcurrentHashMap<>();

// Thread-safe access without explicit locking
Location loc = locations.get(locationId);
```
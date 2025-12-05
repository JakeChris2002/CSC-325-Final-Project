# Legends of Threads: A Parallel Adventure

Jake Blankenship  
**Course:** CSC325-FA25

## Game Title and Story Summary

Legends of Threads: A Parallel Adventure is a multithreaded medieval fantasy game where players control one of three unique character classes in a world dominated by the dark wizard Malachar. The game features parallel execution of multiple characters, each running in their own thread while sharing resources and interacting with a dynamic world.

### The Story
The realm was once peaceful under King Aldric III until Malachar the Dark, formerly the kingdom's greatest protector, fell to corruption after discovering forbidden knowledge from the Void Realm. Now his shadow magic spreads across the land, corrupting villages, temples, and natural groves. Players can choose to be:

- Knight: A noble warrior seeking to protect the innocent and restore honor to the realm
- Thief: A stealthy character navigating the underground resistance against Malachar's tyranny  
- Wizard: A scholar of magic working to understand and counter Malachar's dark powers

The game combines turn-based strategy with real-time AI party member actions, quest systems, environmental storytelling, and rich character development as players work toward liberating the realm from Malachar's dark reign.

## Instructions for Running the Program

### Prerequisites
- Java SE Development Kit (JDK) 11 or higher
- Command line terminal (PowerShell on Windows, Terminal on macOS/Linux)

### Compilation
1. Navigate to the project directory:
   ```bash
   cd "Legends of Threads A Parallel Adventure"
   ```

2. Compile the Java source files:
   ```bash
   javac -d bin -cp lib/* src/*.java
   ```

### Running the Game
1. Run the main application:
   ```bash
   java -cp bin App
   ```

2. Follow the on-screen prompts to:
   - Choose your character class (Knight, Thief, or Wizard)
   - Enter your character name
   - Begin your adventure!

### Game Controls
- **Movement**: Use directional commands (north, south, east, west) or (n, s, e, w)
- **Actions**: Character-specific actions like `attack`, `cast` (Wizard), `steal` (Thief), `patrol` (Knight)
- **Exploration**: `explore` to discover new locations and items
- **Interaction**: `interact` to talk with NPCs and learn the story
- **Inventory**: `inventory` to view collected items
- **Cave Adventure**: `cave` to enter the crystal cavern exploration mode
- **Help**: `help` for available commands
- **Quit**: `quit` to end the game

## Brief Explanation of Thread Synchronization Approach

### Thread Architecture
The game employs a sophisticated multithreading architecture with several levels of synchronization:

### 1. **Character Threads**
- Each game character (Knight, Thief, Wizard) extends the `GameCharacter` abstract class and implements `Runnable`
- Characters execute in separate threads managed by the `GameEngine`
- **Player-controlled character**: Waits for input during their turn, pauses AI execution
- **AI party members**: Continue autonomous actions when not in player's turn

### 2. **Shared Resource Management**
The `SharedResources` class demonstrates multiple concurrency mechanisms:
- **ReentrantReadWriteLock**: For treasure vault operations (multiple readers, exclusive writers)
- **AtomicInteger**: For global mana pool and statistics (lock-free atomic operations)
- **BlockingQueue**: For loot distribution using producer-consumer pattern
- **ConcurrentHashMap**: For thread-safe trading post operations
- **Synchronized methods**: For shared inventory access with intrinsic locks

### 3. **Thread Coordination**
- **GameEngine**: Central coordinator that starts all character threads and manages game state
- **Monitor Thread**: Background health monitoring of the game system
- **Turn-based Control**: Uses `join()` with timeouts to ensure proper thread synchronization
- **Graceful Shutdown**: Implements proper thread cleanup with interrupt handling

### 4. **Race Condition Prevention**
- **Individual locks**: Each character has a `ReentrantLock` for thread-safe state modifications
- **Atomic operations**: Statistics tracking uses atomic integers to prevent race conditions
- **Synchronized collections**: Thread-safe data structures throughout the application
- **Volatile flags**: For safe inter-thread communication of state changes

### 5. **Thread Safety Patterns**
- **Producer-Consumer**: Background threads generate resources, character threads consume them
- **Reader-Writer**: Multiple characters can read treasure vault, but only one can modify it
- **Lock-free algorithms**: Using `AtomicInteger` for high-frequency counter operations
- **Defensive copying**: Returning copies of shared collections to prevent external modification

This architecture ensures thread safety while maintaining responsive gameplay and preventing deadlocks or race conditions in the complex multi-character environment.

## Key Features

### Core Gameplay
- **Concurrent Character System**: Three hero characters (Knight, Rogue, Wizard) operate simultaneously in parallel threads, each with unique abilities and goals
- **Dynamic World Interaction**: Characters autonomously explore rooms, encounter monsters, collect treasures, and interact with the environment
- **Real-time Combat System**: Thread-safe battle mechanics where multiple characters can engage enemies concurrently
- **Shared Resource Management**: Synchronized access to dungeon rooms, treasure chests, and monster encounters

### Technical Features
- **Multithreading Architecture**: Each character runs on a separate thread with coordinated access to shared game world
- **Thread Synchronization**: Safe handling of concurrent operations using ReentrantLock and synchronized blocks
- **Polymorphic Design**: Abstract character classes with specialized implementations for each hero type
- **Lambda Expressions**: Event handling, combat resolution, and logging implemented with functional programming
- **Parallel Streams**: Efficient processing of game world data and character actions
- **Observer Pattern**: Real-time event logging and state monitoring across all threads
- **External API Integration**: Real-time weather and random event data fetched from public APIs to dynamically influence gameplay

### Character Abilities
- **Knight**: High defense, powerful melee attacks, can protect other characters
- **Rogue**: High speed, stealth abilities, treasure-finding expertise
- **Wizard**: Ranged magic attacks, area-of-effect spells, mana management

## Intended Use Cases

1. **Educational Demonstration**: Showcase advanced Java OOP concepts and concurrent programming techniques
2. **Game Development Learning**: Illustrate game loop design, state management, and event-driven architecture
3. **Multithreading Practice**: Demonstrate proper thread coordination, synchronization, and race condition prevention
4. **Interactive Entertainment**: Provide an engaging text-based adventure experience with autonomous AI characters

## Technologies Used

### Languages
- **Java 17+**: Core programming language utilizing modern Java features

### Key Java Libraries & Frameworks
- `java.util.concurrent`: ReentrantLock, ConcurrentHashMap, ExecutorService for thread management
- `java.util.stream`: Stream API for functional data processing
- `java.util.*`: Collections framework (ArrayList, HashMap, etc.)
- `java.lang.Thread`: Base threading functionality
- `java.net.http`: HttpClient for REST API calls (Java 11+)
- `org.json`: JSON parsing library for API response handling

### Development Tools
- **JDK 17 or higher**: Required for compilation and execution
- **Maven/Gradle** (optional): Build automation and dependency management
- **Git**: Version control

### Design Patterns Implemented
- Abstract Factory Pattern (character creation)
- Observer Pattern (event system)
- Strategy Pattern (combat behaviors)
- Singleton Pattern (game world manager)

## Setup Instructions

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Command line terminal or IDE (IntelliJ IDEA, Eclipse, VS Code)

### Compilation

#### Using Command Line
```bash
# Navigate to the project directory
cd CSC-325-Final-Project

# Compile all Java files
javac -d bin src/**/*.java

# Or compile with source and target version specified
javac -source 17 -target 17 -d bin src/**/*.java
```

#### Using an IDE
1. Open the project folder in your IDE
2. Ensure JDK 17+ is configured as the project SDK
3. Build the project using the IDE's build function

### Running the Game

#### From Command Line
```bash
# Run from the bin directory
java -cp bin Main

# Or if running from project root with compiled classes in bin
java -cp bin com.realmofstadows.Main
```

#### From IDE
1. Locate the `Main.java` file (entry point)
2. Right-click and select "Run" or use the run configuration
3. The game will start in the console/terminal window

### Expected Output
The game will display real-time logs of all three characters:
- Exploring different rooms
- Encountering and battling monsters
- Finding treasures
- Leveling up and gaining abilities
- Interacting with shared world resources

The game continues until all characters complete their objectives or are defeated.

## Project Structure

```
CSC-325-Final-Project/
├── src/
│   ├── characters/        # Character classes (Knight, Rogue, Wizard)
│   ├── world/            # Game world, rooms, locations
│   ├── combat/           # Combat system and mechanics
│   ├── items/            # Inventory, weapons, treasures
│   ├── api/              # API integration classes
│   │   ├── WeatherAPIService.java
│   │   ├── DynamicEventGenerator.java
│   │   └── models/       # API response models
│   ├── utils/            # Helper classes, logging
│   └── Main.java         # Entry point
├── docs/
│   └── UML_Diagram.png   # Class structure diagram
├── lib/
│   └── json-20231013.jar # JSON parsing library
├── README.md             # This file
└── .gitignore
```

## Game Rules & Mechanics

1. Each character starts in a different location in the dungeon
2. Characters move through rooms autonomously based on their AI
3. Combat is automatically resolved when characters encounter monsters
4. Treasure can only be collected by one character (thread-safe access)
5. The game ends when all characters complete their quests or perish
6. **Dynamic World Events**: The game fetches real-world data from external APIs to create random events that affect gameplay

## API Integration

The game integrates with external REST APIs to create dynamic, real-world influenced gameplay experiences. This demonstrates practical API consumption, JSON parsing, and asynchronous data fetching in a multithreaded environment.

### APIs Used

#### 1. Open-Meteo Weather API
- **URL**: `https://api.open-meteo.com/v1/forecast`
- **Purpose**: Fetches current weather conditions to influence dungeon environment
- **Data Retrieved**: Temperature, weather code, precipitation
- **Game Impact**: 
  - Rain increases enemy spawn rates
  - Cold weather reduces character movement speed
  - Storms trigger special enemy encounters
  - Clear weather improves treasure finding chances

#### 2. Random User API (Optional Alternative)
- **URL**: `https://randomuser.me/api/`
- **Purpose**: Generates random NPC merchants or quest givers
- **Data Retrieved**: Names, descriptions for dynamic NPCs
- **Game Impact**: Creates unique merchant encounters with randomized inventory

### API Implementation Details

**Class**: `WeatherAPIService`
- Runs in a separate thread to periodically fetch weather data
- Updates game world conditions every 60 seconds
- Parses JSON responses using `org.json` library
- Implements retry logic with exponential backoff
- Thread-safe updates to game world state

**Class**: `DynamicEventGenerator`
- Consumes weather data to generate in-game events
- Uses lambda expressions to map API data to game effects
- Applies weather modifiers to character stats and enemy behavior

### Example API Flow

```java
// Simplified example of API integration
WeatherAPIService weatherService = new WeatherAPIService();
weatherService.start(); // Runs in background thread

// Weather data influences game state
CompletableFuture<WeatherData> future = weatherService.fetchWeatherAsync();
future.thenApply(weather -> {
    if (weather.isRaining()) {
        gameWorld.increaseEnemySpawnRate(1.5);
        gameLogger.logEvent("Heavy rain increases danger!");
    }
    return weather;
});
```

### API Data Usage

| API Data | Game Effect | Implementation |
|----------|-------------|----------------|
| Temperature < 50°F | Characters lose 1 HP per turn | `Character.applyWeatherEffect()` |
| Weather Code 95+ (Storm) | Spawn legendary enemy | `GameWorld.spawnStormBoss()` |
| Precipitation > 50% | 2x treasure spawn rate | `Location.generateTreasure()` |
| Clear weather (Code 0-1) | +10% XP gain | `Character.gainExperience()` |

### Benefits of API Integration

1. **Real-world Connection**: Game world reacts to actual weather conditions
2. **Dynamic Gameplay**: No two game sessions are exactly alike
3. **Asynchronous Processing**: API calls don't block main game threads
4. **Error Handling**: Graceful fallbacks if API is unavailable
5. **Thread Safety**: Demonstrates concurrent API consumption with synchronized game state updates

### Setup Requirements for API

- **Internet Connection**: Required for API calls
- **No API Key Needed**: Open-Meteo is free and doesn't require authentication
- **Fallback Mode**: Game functions with default values if API is unreachable

## Learning Outcomes Demonstrated

- ✅ Abstraction through abstract Character and GameObject classes
- ✅ Inheritance with specialized character types
- ✅ Polymorphism in combat and ability systems
- ✅ Multithreading with concurrent character execution
- ✅ Thread synchronization for shared resource access
- ✅ Lambda expressions for event handling
- ✅ Stream API for data processing
- ✅ Arrays and collections for game state management
- ✅ REST API integration with external data sources
- ✅ Asynchronous programming with CompletableFuture
- ✅ JSON parsing and data transformation

## Future Enhancements

- Player input to control one character while others operate autonomously
- Save/load game state functionality
- Configuration file for game parameters
- GUI interface using JavaFX
- Network multiplayer support

## Author

Created as a capstone project for CSC-325: Advanced Object-Oriented Programming

## License

This project is created for educational purposes.

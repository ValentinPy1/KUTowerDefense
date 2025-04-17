package towerdefense.model;

/**
 * GameModel class represents the main game state and logic.
 */
public class GameModel {
    // Add game state variables and methods here
    private int lives;
    private int gold;
    private int currentWave;
    private GameMap currentMap;

    public GameModel() {
        // Initialize default game state
        this.lives = 20; // Example default
        this.gold = 100; // Example default
        this.currentWave = 0;
        this.currentMap = null;
        System.out.println("GameModel initialized.");
    }

    /**
     * Checks if the game over condition is met (e.g., lives <= 0).
     * 
     * @return true if the game is over, false otherwise.
     */
    public boolean isGameOver() {
        // TODO: Implement actual game over logic
        // return this.lives <= 0;
        return false; // Placeholder
    }

    /**
     * Checks if the game won condition is met (e.g., all waves defeated).
     * 
     * @return true if the game is won, false otherwise.
     */
    public boolean isGameWon() {
        // TODO: Implement actual game won logic
        // return currentWave > totalWaves && allEnemiesDefeated();
        return false; // Placeholder
    }

    // --- Map Handling --- //

    /**
     * Sets the map currently being played.
     * Called by GameController after loading a map.
     * 
     * @param map The GameMap object.
     */
    public void setCurrentMap(GameMap map) {
        this.currentMap = map;
        // TODO: Reset any map-dependent state if necessary
        System.out.println("GameModel: Map set.");
    }

    /**
     * Gets the currently loaded map.
     * 
     * @return The current GameMap, or null if none loaded.
     */
    public GameMap getCurrentMap() {
        return currentMap;
    }

    // --- Placeholder Getters for UI Update Example --- //
    public int getLives() {
        return lives;
    }

    public int getGold() {
        return gold;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return 10;
    } // Example placeholder

    /**
     * Initializes the game state for the start of a new game with a specific map.
     */
    public void initializeGame(GameMap map) {
        this.currentMap = map; // Ensure map is set
        this.lives = 20; // Reset to defaults
        this.gold = 100;
        this.currentWave = 0;
        // TODO: Reset enemy lists, tower lists etc.
        System.out.println("GameModel: Game initialized for new game.");
    }
}
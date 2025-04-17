package towerdefense.controller;

import javafx.animation.AnimationTimer; // For game loop
import towerdefense.model.GameMap;
import towerdefense.model.GameModel;
import towerdefense.view.screens.GameScreen; // Import the view
// Import specific model elements if needed (Enemy, Tower, Projectile)

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Controller for the main game logic.
 * Handles game state updates, user input during gameplay, and game loop.
 */
public class GameController {

    private GameModel model;
    private String mapPath;
    private GameLoopTimer gameLoop;
    private boolean isPaused = false;
    private double gameSpeed = 1.0; // Multiplier for game time
    private GameScreen view; // Add reference to the view

    // Maybe hold reference to view to call update methods?
    // private towerdefense.view.screens.GameScreen gameView;

    public GameController(GameModel model, String mapPath) {
        this.model = model;
        this.mapPath = mapPath;
        GameMap loadedMap = loadMapFromFile(mapPath); // Load the map

        if (loadedMap == null) {
            System.err.println("ERROR: Failed to load map from: " + mapPath + ". Game cannot proceed.");
            // Prevent game loop creation if map loading fails
            this.gameLoop = null;
            // Optionally set a flag or state indicating loading failure
        } else {
            System.out.println("GameController initialized with map: " + mapPath);
            // Pass loaded map data to the GameModel
            model.setCurrentMap(loadedMap);
            this.gameLoop = new GameLoopTimer();
        }
    }

    /** Sets the view associated with this controller */
    public void setView(GameScreen view) {
        this.view = view;
        // Initial update after view is set (if map loaded)
        if (model.getCurrentMap() != null && view != null) {
            view.drawMap(model.getCurrentMap()); // Draw initial map
            view.updateInfoLabels(model.getCurrentWave(), model.getTotalWaves(), model.getGold(), model.getLives());
        }
    }

    /**
     * Attempts to load a GameMap object from a file.
     * 
     * @param path Path to the .tdmap file.
     * @return The loaded GameMap, or null if loading fails.
     */
    private GameMap loadMapFromFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("Map file not found: " + path);
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof GameMap) {
                System.out.println("Map loaded successfully from: " + file.getAbsolutePath());
                return (GameMap) obj;
            } else {
                System.err.println("Invalid file content, not a GameMap: " + path);
                return null;
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("Error loading map from file " + path + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Starts the game loop if the map was loaded successfully.
     */
    public void startGame() {
        GameMap currentMap = model.getCurrentMap(); // Get map from model
        if (gameLoop == null || currentMap == null) {
            System.err.println("Cannot start game, map not loaded properly.");
            // TODO: Navigate back or show error message?
            return;
        }
        System.out.println("GameController: Starting game with map " + mapPath + "...");
        isPaused = false;
        gameSpeed = 1.0;
        // Initialize game state IN THE MODEL using the loaded gameMap
        model.initializeGame(currentMap);
        // Update view labels with initial game state
        if (view != null) {
            view.updateInfoLabels(model.getCurrentWave(), model.getTotalWaves(), model.getGold(), model.getLives());
        }
        gameLoop.start();
    }

    /**
     * Stops the game loop.
     */
    public void stopGame() {
        if (gameLoop != null) {
            System.out.println("GameController: Stopping game...");
            gameLoop.stop();
        }
    }

    /**
     * Handles pausing/resuming the game.
     */
    public void handlePauseToggle() {
        isPaused = !isPaused;
        System.out.println("GameController: Game " + (isPaused ? "paused" : "resumed"));
        if (isPaused) {
            // gameLoop.stop(); // Or just skip update logic inside loop
        } else {
            // gameLoop.start(); // If stopped
        }
    }

    /**
     * Handles changing the game speed.
     */
    public void handleSpeedToggle() {
        if (gameSpeed == 1.0) {
            gameSpeed = 2.0;
        } else {
            gameSpeed = 1.0;
        }
        System.out.println("GameController: Game speed set to x" + gameSpeed);
        // The game loop will use this speed multiplier
    }

    /**
     * Handles selecting a tower type to build.
     * 
     * @param towerType The identifier of the tower type.
     */
    public void handleTowerSelection(int towerType) {
        System.out.println("Controller: Selected Tower Type for placement: " + towerType);
        // TODO: Store the selected tower type, possibly change cursor, wait for click
        // on empty lot
        // model.setSelectedTowerToBuild(towerType);
    }

    /**
     * Handles clicking on an empty tower lot on the map.
     * 
     * @param lotId Identifier for the clicked empty lot.
     */
    public void handlePlaceTower(Object lotId) {
        System.out.println("Controller: Attempting to place tower at lot: " + lotId);
        // TODO: Check if a tower type is selected and player has enough gold
        // int selectedType = model.getSelectedTowerToBuild();
        // if (selectedType != null && model.canAffordTower(selectedType)) {
        // boolean success = model.buildTower(lotId, selectedType);
        // if (success) { // Update view }
        // }
    }

    // --- Game Loop --- //
    private class GameLoopTimer extends AnimationTimer {
        private long lastUpdateNanos = 0;

        @Override
        public void handle(long nowNanos) {
            if (lastUpdateNanos == 0) {
                lastUpdateNanos = nowNanos;
                return;
            }

            if (isPaused) {
                lastUpdateNanos = nowNanos; // Prevent large delta after unpausing
                return;
            }

            double elapsedSeconds = (nowNanos - lastUpdateNanos) / 1_000_000_000.0;
            lastUpdateNanos = nowNanos;

            double gameDeltaTime = elapsedSeconds * gameSpeed;

            // --- Update Game State --- //
            updateGameModel(gameDeltaTime);

            // --- Update UI --- //
            if (view != null) {
                // Update labels (can be optimized to update only when changed)
                view.updateInfoLabels(model.getCurrentWave(), model.getTotalWaves(), model.getGold(), model.getLives());
                // Trigger redraw of dynamic elements (enemies, projectiles)
                view.redrawGameBoard();
            }

            // --- Check Game Over/Win Conditions --- //
            if (model.isGameOver()) {
                System.out.println("Controller: Game Over detected!");
                stopGame();
                // TODO: Show Game Over message/screen
                // Main.loadGameOverScreen(); ??
            } else if (model.isGameWon()) {
                System.out.println("Controller: Game Won detected!");
                stopGame();
                // TODO: Show Victory message/screen
                // Main.loadVictoryScreen(); ??
            }
        }
    }

    /**
     * Updates the game model state based on elapsed time.
     * 
     * @param deltaTime Time elapsed since last update (adjusted for game speed).
     */
    private void updateGameModel(double deltaTime) {
        if (model.getCurrentMap() == null)
            return; // Don't update if no map
        // TODO: Implement game logic using model.getCurrentMap()
        // model.spawnEnemies(deltaTime, model.getCurrentMap());
        // model.moveEnemies(deltaTime, model.getCurrentMap());
        // ... etc ...
    }
}
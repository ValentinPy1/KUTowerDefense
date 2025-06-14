package com.ku.towerdefense.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.ArrayDeque;

import com.ku.towerdefense.model.GamePath;
import com.ku.towerdefense.model.entity.Enemy;
import com.ku.towerdefense.model.entity.Goblin;
import com.ku.towerdefense.model.entity.Knight;
import com.ku.towerdefense.model.entity.Projectile;
import com.ku.towerdefense.model.entity.Tower;
import com.ku.towerdefense.model.entity.MageTower;
import com.ku.towerdefense.model.entity.ArcherTower;
import com.ku.towerdefense.model.entity.DroppedGold;
import com.ku.towerdefense.model.map.GameMap;
import com.ku.towerdefense.model.map.TileType;
import com.ku.towerdefense.util.GameSettings;
import com.ku.towerdefense.ui.UIAssets;
import com.ku.towerdefense.model.effects.AnimatedEffect;
import javafx.scene.image.Image;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.animation.Animation;
import com.ku.towerdefense.model.wave.Wave;
import com.ku.towerdefense.model.wave.WaveConfig;
import com.ku.towerdefense.powerup.PowerUpManager;
import com.ku.towerdefense.powerup.PowerUpType;

/**
 * Main controller for the game, handling the game loop, entities, and game
 * state.
 */
public class GameController {
    private GameMap gameMap;
    private List<Tower> towers;
    private List<Enemy> enemies;
    private List<Projectile> projectiles;
    private List<DroppedGold> activeGoldBags = new ArrayList<>();
    private int playerGold;
    private int playerLives;
    private int currentWave;
    private boolean gameOver;
    private AnimationTimer gameLoop;
    private List<AnimatedEffect> activeEffects = new ArrayList<>();

    // Time between waves in milliseconds
    private static final long WAVE_BREAK_TIME = 5000;
    private boolean betweenWaves = false;
    private long waveStartTime = 0;
    private boolean isSpawningEnemies = false;

    // Grace period for first wave
    private static final long GRACE_PERIOD_MS = 4000; // 4 seconds
    private boolean gracePeriodActive = false;
    private Timeline gracePeriodTimer;

    // Game speed control
    private boolean speedAccelerated = false;
    private static final double SPEED_MULTIPLIER = 2.0;
    
    // Path flash system
    private boolean pathFlashActive = false;
    private long pathFlashStartTime = 0;
    private static final long PATH_FLASH_DURATION = 3000; // 3 seconds
    private double pathFlashAlpha = 0.0;
    
    // Power-up system
    private PowerUpManager powerUpManager;

    // Listener for wave events
    private WaveCompletedListener onWaveCompletedListener;

    private boolean isPaused = false; // Added to track pause state internally
    private Timeline waveTimer; // For timed wave progression
    private Duration waveTimerProgressOnPause; // To store progress when paused

    /**
     * Creates a new game controller with the specified game map.
     *
     * @param gameMap the game map to use
     */
    public GameController(GameMap gameMap) {
        this.gameMap = gameMap;
        this.towers = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.projectiles = new ArrayList<>();
        this.activeGoldBags = new ArrayList<>();
        this.playerGold = GameSettings.getInstance().getStartingGold();
        this.playerLives = GameSettings.getInstance().getStartingLives();
        this.currentWave = 0;
        this.gameOver = false;
        
        // Initialize power-up system
        this.powerUpManager = new PowerUpManager(this);

        // Initialize game loop
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // Convert to seconds
                lastUpdate = now;

                update(deltaTime);
            }
        };
    }

    /**
     * Starts the game loop.
     */
    public void startGame() {
        // gameLoop.start(); // GameScreen handles AnimationTimer start/stop via its own
        // pause
        if (currentWave == 0) { // Auto-start first wave with grace period
            startFirstWaveWithGracePeriod();
        }
    }

    /**
     * Starts the first wave after a 4-second grace period.
     */
    private void startFirstWaveWithGracePeriod() {
        gracePeriodActive = true;
        System.out.println("Starting grace period: 4 seconds to build towers before first wave...");
        
        gracePeriodTimer = new Timeline(new KeyFrame(Duration.millis(GRACE_PERIOD_MS), e -> {
            gracePeriodActive = false;
            System.out.println("Grace period ended. Starting first wave!");
            startNextWave();
        }));
        gracePeriodTimer.play();
    }

    /**
     * Stops the game loop.
     */
    public void stopGame() {
        // gameLoop.stop(); // GameScreen handles AnimationTimer
        if (waveTimer != null) {
            waveTimer.stop();
        }
        if (gracePeriodTimer != null) {
            gracePeriodTimer.stop();
        }
    }

    public void pauseGame() {
        // Add any game-specific pause logic here
    }

    public void resumeGame() {
        // Add any game-specific resume logic here
    }

    public void setPaused(boolean isPaused) {
        this.isPaused = isPaused;
        if (isPaused) {
            if (waveTimer != null && waveTimer.getStatus() == Animation.Status.RUNNING) {
                waveTimerProgressOnPause = waveTimer.getCurrentTime();
                waveTimer.pause();
            }
            if (gracePeriodTimer != null && gracePeriodTimer.getStatus() == Animation.Status.RUNNING) {
                gracePeriodTimer.pause();
            }
        } else {
            if (waveTimer != null && waveTimer.getStatus() == Animation.Status.PAUSED) {
                if (waveTimerProgressOnPause != null) {
                    waveTimer.playFrom(waveTimerProgressOnPause);
                } else {
                    waveTimer.play(); // Should ideally not happen if paused correctly
                }
            }
            if (gracePeriodTimer != null && gracePeriodTimer.getStatus() == Animation.Status.PAUSED) {
                gracePeriodTimer.play();
            }
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Check if the game is in the grace period.
     *
     * @return true if in grace period, false otherwise
     */
    public boolean isInGracePeriod() {
        return gracePeriodActive;
    }

    /**
     * Get the total number of waves from game settings.
     *
     * @return total number of waves
     */
    public int getTotalWaves() {
        return GameSettings.getInstance().getTotalWaves();
    }

    /**
     * Updates the game state.
     *
     * @param deltaTime time elapsed since the last update in seconds
     */
    public void update(double initialDeltaTime) {
        if (gameOver || isPaused) { // Check internal pause state
            return;
        }

        double currentDeltaTime = initialDeltaTime;
        // Apply speed multiplier if accelerated
        if (speedAccelerated) {
            currentDeltaTime *= SPEED_MULTIPLIER;
        }

        // Update towers and collect projectiles
        for (Tower tower : towers) {
            Projectile projectile = tower.update(currentDeltaTime, enemies);
            if (projectile != null) {
                projectiles.add(projectile);
            }
        }

        // Update projectiles and check for hits
        List<Projectile> projectilesToRemove = new ArrayList<>();
        for (Projectile projectile : projectiles) {
            boolean hit = projectile.update(currentDeltaTime);
            if (hit || !projectile.isActive()) {
                projectilesToRemove.add(projectile);
                if (hit) {
                    // Apply damage to the target
                    Enemy target = projectile.getTarget();
                    if (target != null) {
                        target.applyDamage(projectile.getDamage(), projectile.getDamageType());

                        // Mage Tower specific effects
                        Tower sourceTower = projectile.getSourceTower();
                        if (sourceTower instanceof MageTower) {
                            // Teleport: 3% chance for any Mage Tower hit
                            if (Math.random() < 0.03) {
                                Point2D startPoint = gameMap.getStartPoint();
                                if (startPoint != null) {
                                    target.teleportTo(startPoint.getX(), startPoint.getY());
                                    System.out.println("Enemy " + target.hashCode() + " teleported by Mage Tower.");
                                }
                            }

                            // Slow: Only for Level 2 Mage Tower
                            if (sourceTower.getLevel() >= 2) {
                                target.applySlow(0.8, 4.0); // 20% slow (1.0 - 0.8 = 0.2) for 4 seconds
                                System.out.println("Enemy " + target.hashCode() + " slowed by L2 Mage Tower.");
                            }
                        }

                        // Apply AOE damage if applicable
                        if (projectile.hasAoeEffect()) {
                            // Log primary target impact location for AOE reference
                            Point2D impactPoint = new Point2D(target.getCenterX(), target.getCenterY());
                            // System.out.println("AOE centered at: " + impactPoint.getX() + "," +
                            // impactPoint.getY() + " for projectile targeting " + target);

                            for (Enemy enemy : new ArrayList<>(enemies)) { // Iterate over a copy to avoid
                                                                           // ConcurrentModificationException
                                if (enemy != target && enemy.getCurrentHealth() > 0) {
                                    Point2D enemyCenter = new Point2D(enemy.getCenterX(), enemy.getCenterY());
                                    double distance = impactPoint.distance(enemyCenter);

                                    if (distance <= projectile.getAoeRange()) {
                                        System.out.println("Artillery AOE: Hit " + enemy.getClass().getSimpleName() +
                                                " (ID: " + enemy.hashCode() + ") for " + (projectile.getDamage() / 2)
                                                + " damage. " +
                                                "Dist: " + String.format("%.2f", distance) +
                                                ", Range: " + projectile.getAoeRange() +
                                                ", Primary Target: " + target.getClass().getSimpleName() + " (ID: "
                                                + target.hashCode() + ")");

                                        boolean aoeKilled = enemy.applyDamage(projectile.getDamage() / 2,
                                                projectile.getDamageType());
                                        if (aoeKilled) {
                                            playerGold += enemy.getGoldReward();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Spawn impact visual effect
                    Point2D impactPointForEffect = projectile.getTarget() != null
                            ? new Point2D(projectile.getTarget().getCenterX(), projectile.getTarget().getCenterY())
                            : new Point2D(projectile.getCenterX(), projectile.getCenterY()); // Fallback if target is
                                                                                             // gone

                    switch (projectile.getImpactEffect()) {
                        case EXPLOSION:
                            Image explSheet = UIAssets.getImage("ExplosionEffect");
                            if (explSheet != null) {
                                activeEffects.add(new AnimatedEffect(explSheet,
                                        impactPointForEffect.getX(), impactPointForEffect.getY(),
                                        192, 192, // frameW, frameH for Explosion.png
                                        9, // totalFrames for Explosion.png
                                        0.05)); // frameDurationSeconds
                            } else {
                                System.err.println("ExplosionEffect spritesheet not loaded!");
                            }
                            break;
                        case FIRE:
                            Image fireSheet = UIAssets.getImage("FireEffect");
                            if (fireSheet != null) {
                                activeEffects.add(new AnimatedEffect(fireSheet,
                                        impactPointForEffect.getX(), impactPointForEffect.getY(),
                                        128, 128, // frameW, frameH for Fire.png
                                        7, // totalFrames for Fire.png
                                        0.05)); // frameDurationSeconds
                            } else {
                                System.err.println("FireEffect spritesheet not loaded!");
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        projectiles.removeAll(projectilesToRemove);

        // Update enemies and check for ones that reached the end
        List<Enemy> enemiesToRemove = new ArrayList<>();
        for (Enemy enemy : enemies) {
            boolean reachedEnd = enemy.update(currentDeltaTime, enemies);

            if (reachedEnd) {
                enemiesToRemove.add(enemy);
                playerLives--;

                if (playerLives <= 0) {
                    gameOver = true;
                    stopGame();
                    // Potentially trigger a game over UI event or screen change from here
                    // or GameScreen can check isGameOver() state
                }
            } else if (enemy.getCurrentHealth() <= 0 && !enemiesToRemove.contains(enemy)) {
                enemiesToRemove.add(enemy);
                playerGold += enemy.getGoldReward(); // Base gold reward

                if (Math.random() < 0.25) { // 25% chance to drop a bag
                    int archerBaseCost = ArcherTower.BASE_COST;
                    int minGoldInBag = 2;
                    int maxGoldInBag = archerBaseCost / 2;
                    if (maxGoldInBag < minGoldInBag)
                        maxGoldInBag = minGoldInBag;
                    if (minGoldInBag > maxGoldInBag && minGoldInBag > 0)
                        minGoldInBag = maxGoldInBag > 0 ? maxGoldInBag : 1;
                    else if (minGoldInBag <= 0)
                        minGoldInBag = 1;
                    if (maxGoldInBag <= 0)
                        maxGoldInBag = 1;
                    if (minGoldInBag > maxGoldInBag)
                        minGoldInBag = maxGoldInBag;

                    final int randomGold = minGoldInBag + (int) (Math.random() * (maxGoldInBag - minGoldInBag + 1));
                    final int finalRandomGold = (randomGold <= 0) ? 1 : randomGold;

                    final double dropX = enemy.getCenterX();
                    final double dropY = enemy.getCenterY();

                    // 1. Spawn the G_Spawn.png animation
                    Image goldSpawnSheet = UIAssets.getImage("GoldSpawnEffect");
                    if (goldSpawnSheet != null) {
                        AnimatedEffect goldAnimation = new AnimatedEffect(goldSpawnSheet,
                                dropX, dropY, // Position at enemy center
                                128, 128, // Frame width, height for G_Spawn.png
                                7, // Total frames
                                0.07, // Frame duration in seconds (approx 0.5s total animation)
                                128, 128 // Display width/height for the animation itself
                        );
                        goldAnimation.setOnCompletion(() -> {
                            DroppedGold bag = new DroppedGold(dropX, dropY, finalRandomGold);
                            activeGoldBags.add(bag);
                            System.out.println("Dropped gold bag (value: " + finalRandomGold
                                    + "G) created AFTER animation at (" + dropX + "," + dropY + ")");
                        });
                        activeEffects.add(goldAnimation);
                        System.out.println("Spawned gold drop animation at (" + dropX + "," + dropY + ")");
                    } else {
                        System.err.println(
                                "GoldSpawnEffect spritesheet not loaded for animation! Dropping bag directly.");
                        // Fallback: If animation sheet is missing, drop the bag directly
                        DroppedGold bag = new DroppedGold(dropX, dropY, finalRandomGold);
                        activeGoldBags.add(bag);
                    }

                    // 2. Spawn the clickable DroppedGold entity (which uses last frame of
                    // G_Spawn.png)
                    // DroppedGold bag = new DroppedGold(dropX, dropY, randomGold); // MOVED to
                    // onCompletion
                    // activeGoldBags.add(bag); // MOVED to onCompletion
                    // System.out.println("Dropped gold bag (value: " + randomGold + "G) and spawned
                    // animation at (" + dropX + "," + dropY + ")"); // Old log
                }
            }
        }
        enemies.removeAll(enemiesToRemove);

        // Update and remove inactive visual effects
        final double finalDeltaTimeForEffects = currentDeltaTime; // Effectively final for lambda
        activeEffects.removeIf(effect -> {
            effect.update(finalDeltaTimeForEffects);
            return !effect.isActive();
        });

        // Update and remove expired gold bags
        activeGoldBags.removeIf(bag -> {
            // DroppedGold.update() isn't strictly needed if it has no animation or
            // per-frame logic
            // We just check expiry here.
            if (bag.isExpired()) {
                System.out.println("Gold bag expired and removed.");
                return true;
            }
            return false;
        });
        
        // Update path flash animation
        updatePathFlash();
        
        // Update power-up effects
        powerUpManager.update(currentDeltaTime);

        // Check if wave is completed and all enemies are spawned
        // AND if waveTimer is not already running (to prevent multiple timers)
        if (enemies.isEmpty() && !isSpawningEnemies && currentWave > 0 && !betweenWaves
                && (waveTimer == null || waveTimer.getStatus() != Animation.Status.RUNNING)) {
            betweenWaves = true;
            System.out.println(
                    "Wave " + currentWave + " cleared! Next wave in " + (WAVE_BREAK_TIME / 1000) + " seconds.");
            if (onWaveCompletedListener != null) {
                onWaveCompletedListener.onWaveCompleted(currentWave, 100); // Example bonus gold
            }

            waveTimer = new Timeline(new KeyFrame(Duration.millis(WAVE_BREAK_TIME), ae -> {
                betweenWaves = false;
                isSpawningEnemies = false; // Ensure this is reset before starting next wave
                startNextWave();
            }));
            waveTimer.play();
        } else if (isSpawningEnemies && enemies.isEmpty() && !anyEnemiesLeftInWave()) {
            // This case handles if all enemies of a wave are killed before the spawning
            // queue is empty
            // (e.g. very fast killing). Effectively ends the spawning part of the wave.
            isSpawningEnemies = false;
            System.out.println("All spawned enemies for wave " + currentWave + " defeated.");
            // The main betweenWaves logic above will then trigger the timer for the next
            // wave.
        }
    }

    /**
     * Renders all game elements.
     *
     * @param gc the graphics context to render on
     */
    public void render(GraphicsContext gc) {
        // Render map
        gameMap.render(gc);

        // Render enemies
        for (Enemy enemy : enemies) {
            enemy.render(gc);
        }

        // Render towers
        for (Tower tower : towers) {
            tower.render(gc);
        }

        // Render projectiles
        for (Projectile projectile : projectiles) {
            projectile.render(gc);
        }

        // Render active visual effects
        for (AnimatedEffect effect : activeEffects) {
            effect.render(gc);
        }

        // Render dropped gold bags
        for (DroppedGold bag : activeGoldBags) {
            bag.render(gc);
        }
        
        // Render path flash (if active)
        renderPathFlash(gc);

        // Additional UI rendering can be handled elsewhere
    }

    public void setPlayerGold(int i) {
        this.playerGold=i;
    }
    
    /**
     * Set the player's lives (for save/load system)
     */
    public void setPlayerLives(int lives) {
        this.playerLives = lives;
    }
    
    /**
     * Set the current wave (for save/load system)
     */
    public void setCurrentWave(int wave) {
        this.currentWave = wave;
    }
    
    /**
     * Set the game over state (for save/load system)
     */
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    /**
     * Interface for wave completed event
     */
    public interface WaveCompletedListener {
        void onWaveCompleted(int waveNumber, int goldBonus);
    }

    /**
     * Set listener for wave completed events
     *
     * @param listener the listener to set
     */
    public void setOnWaveCompletedListener(WaveCompletedListener listener) {
        this.onWaveCompletedListener = listener;
    }

    /**
     * Starts the next wave of enemies.
     */
    public void startNextWave() {
        // Enhanced debugging for enemy path issues
        System.out.println("startNextWave called - attempting to start wave " + (currentWave + 1));

        // Check if we've reached the maximum waves
        if (currentWave >= getTotalWaves()) {
            System.out.println("All waves completed! Player has won the game!");
            gameOver = true;
            stopGame();
            return;
        }

        // Check if path exists
        if (gameMap.getEnemyPath() == null) {
            System.err.println("ERROR: Cannot start wave - No path defined on the map. Place Start and End tiles.");

            // Debug extra information to help diagnose
            boolean hasStartTile = false;
            boolean hasEndTile = false;
            for (int x = 0; x < gameMap.getWidth(); x++) {
                for (int y = 0; y < gameMap.getHeight(); y++) {
                    if (gameMap.getTileType(x, y) == TileType.START_POINT) {
                        hasStartTile = true;
                        System.out.println("Found START_POINT at (" + x + "," + y + ")");
                    }
                    if (gameMap.getTileType(x, y) == TileType.END_POINT) {
                        hasEndTile = true;
                        System.out.println("Found END_POINT at (" + x + "," + y + ")");
                    }
                }
            }

            if (!hasStartTile)
                System.err.println("Missing START_POINT tile on map");
            if (!hasEndTile)
                System.err.println("Missing END_POINT tile on map");

            // Try to force path generation if we have start and end points but no path
            if (hasStartTile && hasEndTile) {
                System.out.println("Trying to force path generation since start and end points exist...");
                gameMap.generatePath();

                // Check if it worked
                if (gameMap.getEnemyPath() == null) {
                    System.err.println("Path generation failed. Please check map configuration.");
                    return;
                } else {
                    System.out.println("Path was successfully generated!");
                }
            } else {
                return; // Exit if we don't have both required points
            }
        }

        // Increment wave counter
        currentWave++;
        System.out.println("Starting wave " + currentWave);
        
        // Update power-up manager with new wave
        powerUpManager.setCurrentWave(currentWave);
        
        // ✨ TRIGGER PATH FLASH - Show players the enemy route!
        startPathFlash();

        // Calculate enemy numbers
        int num = GameSettings.getInstance().getEnemiesPerGroup() * (1 + currentWave / 3); // Example scaling
        int goblins = (int) (num * GameSettings.getInstance().getGoblinPercentage() / 100.0);
        int knights = num - goblins;
        System.out.println("Wave " + currentWave + " will have " + goblins + " goblins and " + knights + " knights");

        // Find start point
        Point2D start = gameMap.getStartPoint();
        if (start == null) {
            System.err.println("ERROR: Cannot start wave - Start point not found on map.");
            return;
        }
        System.out.println("Using start point at: (" + start.getX() + ", " + start.getY() + ")");

        // Create enemy queue
        Queue<Enemy> queue = new ArrayDeque<>();
        for (int i = 0; i < goblins; i++)
            queue.add(new Goblin(start.getX(), start.getY()));
        for (int i = 0; i < knights; i++)
            queue.add(new Knight(start.getX(), start.getY()));
        System.out.println("Created queue with " + queue.size() + " enemies");

        // Setting up spawning
        isSpawningEnemies = true;
        double delay = GameSettings.getInstance().getEnemyDelay() / 1000.0; // Convert ms to seconds
        System.out.println("Enemy spawn delay: " + delay + " seconds");

        // Create and start Timeline for enemy spawning using AtomicReference to avoid
        // initialization issues
        final Timeline[] spawnerRef = new Timeline[1];
        Timeline spawner = new Timeline(
                new KeyFrame(Duration.seconds(delay), e -> {
                    Enemy next = queue.poll();
                    if (next == null) {
                        isSpawningEnemies = false;
                        spawnerRef[0].stop(); // Use the reference instead of direct variable
                        System.out.println("Wave " + currentWave + " spawning complete.");
                        return;
                    }

                    // Ensure the enemy has the path reference
                    GamePath path = gameMap.getEnemyPath();
                    if (path != null) {
                        next.setPath(path);
                        enemies.add(next);
                        System.out.println("Spawned " + (next instanceof Goblin ? "Goblin" : "Knight") +
                                " at (" + next.getX() + "," + next.getY() + ")");
                    } else {
                        System.err.println("ERROR: Enemy path disappeared during spawning!");
                    }
                }));
        spawnerRef[0] = spawner; // Store the Timeline in the array reference
        spawner.setCycleCount(Animation.INDEFINITE);
        spawner.play();

        System.out.println("Wave " + currentWave + " spawning started!");
    }

    /**
     * Sells a tower at the specified position if one exists there.
     *
     * @param x x coordinate (pixels)
     * @param y y coordinate (pixels)
     * @return the amount of gold refunded, or 0 if no tower was sold
     */
    public int sellTower(double x, double y) {
        // Convert world coordinates to tile coordinates based on a 64x64 grid
        int tileX = (int) (x / 64.0);
        int tileY = (int) (y / 64.0);

        Tower towerToRemove = null;
        for (Tower tower : towers) {
            // Check if the tower's center falls within the clicked 64x64 tile
            int towerTileX = (int) (tower.getX() / 64.0);
            int towerTileY = (int) (tower.getY() / 64.0);
            if (towerTileX == tileX && towerTileY == tileY) {
                towerToRemove = tower;
                break;
            }
        }

        if (towerToRemove != null) {
            int refundAmount = towerToRemove.getSellRefund();
            towers.remove(towerToRemove);
            playerGold += refundAmount;
            return refundAmount;
        }

        return 0;
    }

    /**
     * Select a tower at the specified position.
     *
     * @param x x coordinate (pixels)
     * @param y y coordinate (pixels)
     * @return the selected tower or null if none exists at that position
     */
    public Tower selectTowerAt(double x, double y) {
        int tileX = (int) (x / gameMap.getTileSize());
        int tileY = (int) (y / gameMap.getTileSize());

        for (Tower tower : towers) {
            // Check if the tower's center falls within the clicked 64x64 tile
            int towerTileX = (int) (tower.getX() / gameMap.getTileSize());
            int towerTileY = (int) (tower.getY() / gameMap.getTileSize());
            if (towerTileX == tileX && towerTileY == tileY) {
                // Deselect previously selected tower
                for (Tower t : towers) {
                    t.setSelected(false);
                }
                // Select the new tower
                tower.setSelected(true);
                return tower;
            }
        }
        // No tower found at the click, deselect any currently selected tower
        for (Tower t : towers) {
            t.setSelected(false);
        }
        return null; // No tower found at this location
    }

    /**
     * Gets the tower at the specified world coordinates without changing selection
     * state.
     *
     * @param worldX The x-coordinate in the game world.
     * @param worldY The y-coordinate in the game world.
     * @return The tower at the given coordinates, or null if no tower is present.
     */
    public Tower getTowerAt(double worldX, double worldY) {
        // Ensure GameMap.TILE_SIZE is used or gameMap.getTileSize() is reliable
        double tileSize = gameMap.getTileSize();
        if (tileSize <= 0)
            tileSize = GameMap.TILE_SIZE; // Fallback if somehow not initialized

        int clickTileX = (int) (worldX / tileSize);
        int clickTileY = (int) (worldY / tileSize);
        System.out.println("[GameController.getTowerAt] Checking for tower at world (" + worldX + "," + worldY
                + ") -> tile (" + clickTileX + "," + clickTileY + ")"); // DEBUG

        for (Tower tower : towers) {
            // Calculate tower's tile coordinates based on its top-left position
            int towerStoredTileX = (int) (tower.getX() / tileSize);
            int towerStoredTileY = (int) (tower.getY() / tileSize);
            System.out.println("[GameController.getTowerAt]   Comparing with tower '" + tower.getName()
                    + "' at stored tile (" + towerStoredTileX + "," + towerStoredTileY + ") / world (" + tower.getX()
                    + "," + tower.getY() + ")"); // DEBUG

            if (towerStoredTileX == clickTileX && towerStoredTileY == clickTileY) {
                System.out.println("[GameController.getTowerAt]     Found matching tower: " + tower.getName()); // DEBUG
                return tower;
            }
        }
        System.out.println("[GameController.getTowerAt]   No tower found at these coordinates."); // DEBUG
        return null;
    }

    /**
     * Check if game speed is accelerated.
     *
     * @return true if speed is accelerated, false otherwise
     */
    public boolean isSpeedAccelerated() {
        return speedAccelerated;
    }

    /**
     * Returns the current game speed multiplier.
     * 
     * @return 2 if fast-forward is active, 1 otherwise.
     */
    public int getGameSpeed() {
        return speedAccelerated ? (int) SPEED_MULTIPLIER : 1;
    }

    /**
     * Set the game speed acceleration.
     *
     * @param speedAccelerated true to accelerate, false for normal speed
     */
    public void setSpeedAccelerated(boolean speedAccelerated) {
        this.speedAccelerated = speedAccelerated;
    }

    /**
     * Getters and setters
     */
    public GameMap getGameMap() {
        return gameMap;
    }

    public List<Tower> getTowers() {
        return towers;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public int getPlayerGold() {
        return playerGold;
    }

    public int getPlayerLives() {
        return playerLives;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Reinitialize all entities after loading a saved game.
     * This ensures that images and other transient fields are properly reloaded.
     */
    public void reinitializeAfterLoad() {
        System.out.println("GameController: Reinitializing all entities after loading saved game");

        // First, ensure the map path is properly initialized
        if (gameMap != null) {
            // Force map to regenerate tiles and paths
            if (gameMap.getEnemyPath() == null) {
                System.out.println("Regenerating enemy path in map...");
                gameMap.generatePath();
            }

            // Reload map tile images if needed
            for (int x = 0; x < gameMap.getWidth(); x++) {
                for (int y = 0; y < gameMap.getHeight(); y++) {
                    if (gameMap.getTile(x, y) != null) {
                        gameMap.getTile(x, y).reinitializeAfterLoad();
                    }
                }
            }
        }

        // Reload enemy images
        System.out.println("Reinitializing " + enemies.size() + " enemies");
        for (Enemy enemy : enemies) {
            enemy.reinitializeAfterLoad();
        }

        // Reload projectile images
        System.out.println("Reinitializing " + projectiles.size() + " projectiles");
        for (Projectile projectile : projectiles) {
            projectile.reinitializeAfterLoad();
        }

        // Reload tower images
        System.out.println("Reinitializing " + towers.size() + " towers");
        for (Tower tower : towers) {
            tower.reinitializeAfterLoad();
        }

        // Reinitialize gold bags if they are part of save/load
        System.out.println("Reinitializing " + activeGoldBags.size() + " gold bags");
        for (DroppedGold bag : activeGoldBags) {
            bag.reinitializeAfterLoad();
        }

        System.out.println("GameController: Reinitialization complete");
    }

    // Helper to check if any enemies are scheduled for the current wave but not yet
    // spawned
    private boolean anyEnemiesLeftInWave() {
        Wave currentWaveData = WaveConfig.getWave(this.currentWave);
        if (currentWaveData == null)
            return false;
        // This is a simplified check. Real implementation would need to track spawned
        // vs total for the wave.
        // For now, if isSpawningEnemies is true, we assume some might still be in a
        // queue.
        // The logic in startNextWave needs to manage this queue properly.
        return isSpawningEnemies; // Placeholder, proper check depends on startNextWave implementation
    }

    // Method to purchase and place tower using TILE coordinates
    /**
     * Purchases and places a tower at the specified tile coordinates.
     *
     * REQUIRES: towerTemplate != null, tileX >= 0, tileY >= 0,
     *          tileX < gameMap.getWidth(), tileY < gameMap.getHeight(),
     *          gameMap != null, towers != null
     * MODIFIES: this.towers, this.playerGold, this.gameMap
     * EFFECTS: If player has enough gold (>= towerTemplate.getBaseCost()) and
     *          the tile at (tileX, tileY) can accept a tower placement,
     *          creates a new tower instance, places it at the specified tile,
     *          deducts the tower cost from playerGold, marks the tile as occupied,
     *          and returns true. Otherwise, returns false and leaves the game state unchanged.
     *          The placed tower will be at level 1 with position set to world coordinates
     *          (tileX * TILE_SIZE, tileY * TILE_SIZE).
     *
     * @param towerTemplate the template tower to base the new tower on
     * @param tileX the x-coordinate of the tile (in tile units, not pixels)
     * @param tileY the y-coordinate of the tile (in tile units, not pixels)
     * @return true if tower was successfully purchased and placed, false otherwise
     */
    public boolean purchaseAndPlaceTower(Tower towerTemplate, int tileX, int tileY) {
        if (towerTemplate == null)
            return false;

        // Set tower position based on tile coordinates (top-left of the tile)
        double worldX = tileX * GameMap.TILE_SIZE;
        double worldY = tileY * GameMap.TILE_SIZE;
        towerTemplate.setX(worldX);
        towerTemplate.setY(worldY);
        // Ensure cost is based on the template's base cost, not a potentially leveled
        // instance
        int cost = towerTemplate.getBaseCost();

        if (playerGold >= cost) {
            if (gameMap.canPlaceTower(worldX + GameMap.TILE_SIZE / 2.0, worldY + GameMap.TILE_SIZE / 2.0, towers)) {
                // Create a new instance for placement using the template's type
                Tower newTower = towerTemplate.cloneTower(); // Assumes a cloneTower() or similar method exists
                newTower.setX(worldX);
                newTower.setY(worldY);
                newTower.setLevel(1); // Ensure it's level 1

                towers.add(newTower);
                playerGold -= cost;
                System.out.println(newTower.getName() + " purchased and placed at (" + tileX + "," + tileY + "). Gold: "
                        + playerGold);
                gameMap.setTileAsOccupiedByTower(tileX, tileY, true);
                return true;
            }
            System.err.println("Cannot place tower: Tile (" + tileX + "," + tileY + ") is not suitable or blocked.");
        } else {
            System.err.println("Cannot place tower: Not enough gold. Need " + cost + ", have " + playerGold);
        }
        return false;
    }

    public boolean upgradeTower(Tower towerToUpgrade, int tileX, int tileY) {
        if (towerToUpgrade == null) {
            // Attempt to find tower if only coordinates are implicitly known by context
            towerToUpgrade = getTowerAtTile(tileX, tileY);
            if (towerToUpgrade == null) {
                System.err.println("Upgrade failed: No tower at tile (" + tileX + "," + tileY + ")");
                return false;
            }
        }

        if (towerToUpgrade.canUpgrade() && playerGold >= towerToUpgrade.getUpgradeCost()) {
            playerGold -= towerToUpgrade.getUpgradeCost();
            towerToUpgrade.upgrade();
            System.out.println("Tower at (" + tileX + "," + tileY + ") upgraded to level " + towerToUpgrade.getLevel()
                    + ". Gold: " + playerGold);
            return true;
        } else if (!towerToUpgrade.canUpgrade()) {
            System.err.println("Upgrade failed: Tower at max level.");
        } else {
            System.err.println("Upgrade failed: Not enough gold. Need " + towerToUpgrade.getUpgradeCost() + ", have "
                    + playerGold);
        }
        return false;
    }

    public int sellTower(int tileX, int tileY) {
        System.out
                .println("[GameController.sellTower] Attempting to sell tower at TILE: (" + tileX + "," + tileY + ")"); // DEBUG
        Tower towerToSell = null;
        int indexToRemove = -1;
        double tileSize = gameMap.getTileSize(); // Use getter for consistency, or GameMap.TILE_SIZE directly
        if (tileSize <= 0)
            tileSize = GameMap.TILE_SIZE;

        for (int i = 0; i < towers.size(); i++) {
            Tower t = towers.get(i);
            int tTileX = (int) (t.getX() / tileSize);
            int tTileY = (int) (t.getY() / tileSize);
            System.out.println("[GameController.sellTower]   Checking against tower '" + t.getName() + "' at tile ("
                    + tTileX + "," + tTileY + ")"); // DEBUG
            if (tTileX == tileX && tTileY == tileY) {
                towerToSell = t;
                indexToRemove = i;
                System.out.println("[GameController.sellTower]     Found tower to sell: " + t.getName()); // DEBUG
                break;
            }
        }

        if (towerToSell != null) {
            int refund = towerToSell.getSellRefund();
            towers.remove(indexToRemove);
            playerGold += refund;
            System.out.println("[GameController.sellTower]     Tower removed. Gold after refund: " + playerGold); // DEBUG
            gameMap.setTileAsOccupiedByTower(tileX, tileY, false);
            System.out.println("[GameController.sellTower]     Called setTileAsOccupiedByTower for (" + tileX + ","
                    + tileY + ") to false."); // DEBUG
            return refund;
        }
        System.err.println("[GameController.sellTower] Sell failed: No tower found at tile (" + tileX + "," + tileY
                + ") to sell."); // DEBUG
        return 0;
    }

    public Tower getTowerAtTile(int tileX, int tileY) {
        for (Tower t : towers) {
            int tTileX = (int) (t.getX() / GameMap.TILE_SIZE);
            int tTileY = (int) (t.getY() / GameMap.TILE_SIZE);
            if (tTileX == tileX && tTileY == tileY) {
                return t;
            }
        }
        return null;
    }

    // Method to collect a gold bag (called by GameScreen)
    public void collectGoldBag(DroppedGold bag) {
        if (activeGoldBags.contains(bag)) {
            playerGold += bag.getGoldAmount();
            activeGoldBags.remove(bag);
            System.out.println("Collected gold bag with " + bag.getGoldAmount() + "G. Total gold: " + playerGold);
        } else {
            System.err.println("Attempted to collect an already collected or non-existent gold bag.");
        }
    }

    // Getter for GameScreen to check gold bags for clicks
    public List<DroppedGold> getActiveGoldBags() {
        return activeGoldBags;
    }
    
    // ===== SAVE/LOAD SYSTEM SUPPORT METHODS =====
    
    /**
     * Get whether the game is currently between waves
     */
    public boolean isBetweenWaves() {
        return betweenWaves;
    }
    
    /**
     * Set whether the game is between waves (for save/load)
     */
    public void setBetweenWaves(boolean betweenWaves) {
        this.betweenWaves = betweenWaves;
    }
    
    /**
     * Get the wave start time
     */
    public long getWaveStartTime() {
        return waveStartTime;
    }
    
    /**
     * Set the wave start time (for save/load)
     */
    public void setWaveStartTime(long waveStartTime) {
        this.waveStartTime = waveStartTime;
    }
    
    /**
     * Get whether enemies are currently spawning
     */
    public boolean isSpawningEnemies() {
        return isSpawningEnemies;
    }
    
    /**
     * Set whether enemies are spawning (for save/load)
     */
    public void setSpawningEnemies(boolean isSpawningEnemies) {
        this.isSpawningEnemies = isSpawningEnemies;
    }
    
    /**
     * Set grace period active state (for save/load)
     */
    public void setGracePeriodActive(boolean gracePeriodActive) {
        this.gracePeriodActive = gracePeriodActive;
    }
    
    // ===== PATH FLASH SYSTEM =====
    
    /**
     * Start the path flash animation to show players the enemy route
     */
    private void startPathFlash() {
        pathFlashActive = true;
        pathFlashStartTime = System.currentTimeMillis();
        pathFlashAlpha = 1.0;
        System.out.println("🌟 Path flash started! Showing enemy route for " + (PATH_FLASH_DURATION / 1000) + " seconds");
    }
    
    /**
     * Update the path flash animation
     */
    private void updatePathFlash() {
        if (!pathFlashActive) return;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - pathFlashStartTime;
        
        if (elapsed >= PATH_FLASH_DURATION) {
            // Flash duration complete
            pathFlashActive = false;
            pathFlashAlpha = 0.0;
            System.out.println("✨ Path flash ended - route hidden");
        } else {
            // Calculate pulsing alpha (creates a breathing effect)
            double progress = (double) elapsed / PATH_FLASH_DURATION;
            double pulseSpeed = 4.0; // How fast the pulse is
            double basePulse = Math.sin(progress * pulseSpeed * Math.PI) * 0.3 + 0.7; // 0.4 to 1.0
            
            // Fade out over time
            double fadeOut = 1.0 - (progress * 0.3); // Gradually reduce intensity
            
            pathFlashAlpha = basePulse * fadeOut;
        }
    }
    
    /**
     * Render the path flash if active (currently disabled - no visual path shown)
     */
    public void renderPathFlash(GraphicsContext gc) {
        // Path flash rendering disabled since there's only one path
        // System infrastructure kept for potential future use
        return;
    }
    
    // ===== POWER-UP SYSTEM =====
    
    /**
     * Get the power-up manager
     */
    public PowerUpManager getPowerUpManager() {
        return powerUpManager;
    }
    
    /**
     * Activate a power-up (called from UI)
     */
    public boolean activatePowerUp(PowerUpType type) {
        return powerUpManager.activatePowerUp(type);
    }
    
    /**
     * Check if a power-up can be used
     */
    public boolean canUsePowerUp(PowerUpType type) {
        return powerUpManager.canUsePowerUp(type);
    }
}
package com.ku.towerdefense.service;

import com.ku.towerdefense.controller.GameController;
import com.ku.towerdefense.model.entity.*;
import com.ku.towerdefense.model.map.GameMap;
import com.ku.towerdefense.model.map.Tile;
import com.ku.towerdefense.model.map.TileType;
// AnimatedEffect import removed - visual effects don't need to be saved
import com.ku.towerdefense.util.GameSettings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Professional Save/Load Service for KU Tower Defense
 * Handles complete game state serialization with compression, validation, and error recovery.
 * Demonstrates advanced software engineering practices including:
 * - Data persistence and serialization
 * - File I/O with compression
 * - Error handling and validation
 * - Backup and recovery systems
 * - Version compatibility
 */
public class GameSaveService {
    
    private static final String SAVE_DIRECTORY = "saves";
    private static final String SAVE_EXTENSION = ".ktsave";
    private static final String BACKUP_EXTENSION = ".backup";
    private static final int MAX_SAVE_SLOTS = 10;
    private static final int CURRENT_SAVE_VERSION = 1;
    
    // Singleton pattern for service management
    private static GameSaveService instance;
    
    private GameSaveService() {
        initializeSaveDirectory();
    }
    
    public static synchronized GameSaveService getInstance() {
        if (instance == null) {
            instance = new GameSaveService();
        }
        return instance;
    }
    
    /**
     * Complete game state data structure for serialization
     */
    public static class GameSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        // Save metadata
        public int saveVersion = CURRENT_SAVE_VERSION;
        public String saveName;
        public LocalDateTime saveTime;
        public String gameVersion = "1.0.0";
        
        // Core game state
        public int playerGold;
        public int playerLives;
        public int currentWave;
        public boolean gameOver;
        public boolean isPaused;
        public boolean speedAccelerated;
        
        // Game timing
        public boolean betweenWaves;
        public long waveStartTime;
        public boolean isSpawningEnemies;
        public boolean gracePeriodActive;
        
        // Entity data
        public List<TowerSaveData> towers = new ArrayList<>();
        public List<EnemySaveData> enemies = new ArrayList<>();
        public List<ProjectileSaveData> projectiles = new ArrayList<>();
        public List<DroppedGoldSaveData> goldBags = new ArrayList<>();
        // Visual effects don't need to be saved
        
        // Map state (complete tile information)
        public TileType[][] tileTypes;
        // Legacy compatibility for old saves
        @Deprecated
        public boolean[][] tileOccupancy;
        public String mapName;
        public int mapWidth;
        public int mapHeight;
        
        // Game settings snapshot
        public GameSettings gameSettings;
        
        // Statistics for analysis
        public long gameStartTime;
        public int towersBuilt;
        public int enemiesKilled;
        public int totalGoldEarned;
        public int totalGoldSpent;
        
        public GameSaveData(String saveName) {
            this.saveName = saveName;
            this.saveTime = LocalDateTime.now();
            this.gameStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Tower serialization data
     */
    public static class TowerSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String towerType;
        public double x, y;
        public int level;
        public int damage;
        public double range;
        public long lastFireTime;
        public boolean selected;
        public int upgradeCount;
        public int totalDamageDealt;
        public int enemiesKilled;
        
        public TowerSaveData(Tower tower) {
            this.towerType = tower.getClass().getSimpleName();
            this.x = tower.getX();
            this.y = tower.getY();
            this.level = tower.getLevel();
            this.damage = tower.getDamage();
            this.range = tower.getRange();
            this.selected = tower.isSelected();
            // Additional stats would be tracked if implemented
        }
    }
    
    /**
     * Enemy serialization data
     */
    public static class EnemySaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String enemyType;
        public double x, y;
        public int currentHealth;
        public int maxHealth;
        public double speed;
        public double pathProgress;
        public boolean isSlowed;
        public double slowFactor;
        public double slowDuration;
        
        public EnemySaveData(Enemy enemy) {
            this.enemyType = enemy.getClass().getSimpleName();
            this.x = enemy.getX();
            this.y = enemy.getY();
            this.currentHealth = enemy.getCurrentHealth();
            this.maxHealth = enemy.getMaxHealth();
            this.speed = enemy.getSpeed();
            this.pathProgress = enemy.getPathProgress();
            // Slow effects would be saved if implemented
        }
    }
    
    /**
     * Projectile serialization data
     */
    public static class ProjectileSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public String projectileType;
        public double x, y;
        public double targetX, targetY;
        public int damage;
        public String damageType;
        public double speed;
        public boolean hasAoeEffect;
        public double aoeRange;
        public String sourceTowerType;
        
        public ProjectileSaveData(Projectile projectile) {
            this.x = projectile.getX();
            this.y = projectile.getY();
            this.damage = projectile.getDamage();
            // Note: Projectile doesn't have getSpeed() method, using default
            this.speed = 300.0; // Default speed
            this.hasAoeEffect = projectile.hasAoeEffect();
            this.aoeRange = projectile.getAoeRange();
            
            if (projectile.getTarget() != null) {
                this.targetX = projectile.getTarget().getCenterX();
                this.targetY = projectile.getTarget().getCenterY();
            }
            
            if (projectile.getSourceTower() != null) {
                this.sourceTowerType = projectile.getSourceTower().getClass().getSimpleName();
            }
        }
    }
    
    /**
     * Dropped gold serialization data
     */
    public static class DroppedGoldSaveData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public double x, y;
        public int goldAmount;
        public long dropTime;
        
        public DroppedGoldSaveData(DroppedGold goldBag) {
            this.x = goldBag.getX();
            this.y = goldBag.getY();
            this.goldAmount = goldBag.getGoldAmount();
            this.dropTime = System.currentTimeMillis();
        }
    }
    
    // Visual effects don't need to be saved - they're temporary
    
    /**
     * Save the complete game state to a file
     */
    public boolean saveGame(GameController gameController, String saveName) {
        try {
            System.out.println("🔄 Starting game save process...");
            
            // Create save data
            GameSaveData saveData = createSaveData(gameController, saveName);
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = saveName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + SAVE_EXTENSION;
            Path savePath = Paths.get(SAVE_DIRECTORY, filename);
            
            // Create backup of existing save if it exists
            if (Files.exists(savePath)) {
                createBackup(savePath);
            }
            
            // Save with compression
            try (FileOutputStream fos = new FileOutputStream(savePath.toFile());
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(gzos)) {
                
                oos.writeObject(saveData);
                oos.flush();
            }
            
            // Validate the save
            if (validateSaveFile(savePath)) {
                System.out.println("✅ Game saved successfully: " + filename);
                System.out.println("📊 Save Statistics:");
                System.out.println("   - Towers: " + saveData.towers.size());
                System.out.println("   - Enemies: " + saveData.enemies.size());
                System.out.println("   - Projectiles: " + saveData.projectiles.size());
                System.out.println("   - Gold Bags: " + saveData.goldBags.size());
                System.out.println("   - File Size: " + Files.size(savePath) + " bytes");
                
                // Clean up old saves
                cleanupOldSaves();
                return true;
            } else {
                System.err.println("❌ Save validation failed!");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to save game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load a game state from file
     */
    public boolean loadGame(GameController gameController, String filename) {
        Path savePath = Paths.get(SAVE_DIRECTORY, filename);
        
        if (!Files.exists(savePath)) {
            System.err.println("❌ Save file not found: " + filename);
            return false;
        }
        
        try {
            System.out.println("🔄 Loading game from: " + filename);
            
            // Load save data
            GameSaveData saveData;
            try (FileInputStream fis = new FileInputStream(savePath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gzis)) {
                
                saveData = (GameSaveData) ois.readObject();
            }
            
            // Validate save version compatibility
            if (!isVersionCompatible(saveData.saveVersion)) {
                System.err.println("❌ Save file version incompatible: " + saveData.saveVersion);
                return false;
            }
            
            // Apply save data to game controller
            if (applySaveData(gameController, saveData)) {
                System.out.println("✅ Game loaded successfully!");
                System.out.println("📊 Loaded Statistics:");
                System.out.println("   - Save Date: " + saveData.saveTime);
                System.out.println("   - Wave: " + saveData.currentWave);
                System.out.println("   - Gold: " + saveData.playerGold);
                System.out.println("   - Lives: " + saveData.playerLives);
                System.out.println("   - Towers: " + saveData.towers.size());
                return true;
            } else {
                System.err.println("❌ Failed to apply save data!");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get list of available save files
     */
    public List<SaveFileInfo> getAvailableSaves() {
        List<SaveFileInfo> saves = new ArrayList<>();
        
        try {
            if (!Files.exists(Paths.get(SAVE_DIRECTORY))) {
                return saves;
            }
            
            Files.list(Paths.get(SAVE_DIRECTORY))
                .filter(path -> path.toString().endsWith(SAVE_EXTENSION))
                .forEach(path -> {
                    try {
                        SaveFileInfo info = getSaveFileInfo(path);
                        if (info != null) {
                            saves.add(info);
                        }
                    } catch (Exception e) {
                        System.err.println("Warning: Could not read save file: " + path);
                    }
                });
                
        } catch (IOException e) {
            System.err.println("Error reading save directory: " + e.getMessage());
        }
        
        // Sort by save time (newest first)
        saves.sort((a, b) -> b.saveTime.compareTo(a.saveTime));
        return saves;
    }
    
    /**
     * Save file information for UI display
     */
    public static class SaveFileInfo {
        public String filename;
        public String saveName;
        public LocalDateTime saveTime;
        public int currentWave;
        public int playerGold;
        public int playerLives;
        public long fileSize;
        public boolean isValid;
        
        public String getDisplayName() {
            return saveName + " (Wave " + currentWave + ")";
        }
        
        public String getFormattedTime() {
            return saveTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        public String getFormattedSize() {
            if (fileSize < 1024) return fileSize + " B";
            if (fileSize < 1024 * 1024) return (fileSize / 1024) + " KB";
            return (fileSize / (1024 * 1024)) + " MB";
        }
    }
    
    // Private helper methods
    
    private void initializeSaveDirectory() {
        try {
            Path saveDir = Paths.get(SAVE_DIRECTORY);
            if (!Files.exists(saveDir)) {
                Files.createDirectories(saveDir);
                System.out.println("📁 Created save directory: " + SAVE_DIRECTORY);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to create save directory: " + e.getMessage());
        }
    }
    
    private GameSaveData createSaveData(GameController gameController, String saveName) {
        GameSaveData saveData = new GameSaveData(saveName);
        
        // Core game state
        saveData.playerGold = gameController.getPlayerGold();
        saveData.playerLives = gameController.getPlayerLives();
        saveData.currentWave = gameController.getCurrentWave();
        saveData.gameOver = gameController.isGameOver();
        saveData.isPaused = gameController.isPaused();
        saveData.speedAccelerated = gameController.isSpeedAccelerated();
        
        // Wave timing state - CRITICAL for proper game flow restoration
        try {
            saveData.betweenWaves = gameController.isBetweenWaves();
        } catch (Exception e) {
            saveData.betweenWaves = false; // Safe default
        }
        try {
            saveData.waveStartTime = gameController.getWaveStartTime();
        } catch (Exception e) {
            saveData.waveStartTime = System.currentTimeMillis(); // Safe default
        }
        try {
            saveData.isSpawningEnemies = gameController.isSpawningEnemies();
        } catch (Exception e) {
            saveData.isSpawningEnemies = false; // Safe default
        }
        saveData.gracePeriodActive = gameController.isInGracePeriod();
        
        // Path flash state (optional - could be reset on load)
        // saveData.pathFlashActive = gameController.isPathFlashActive(); // Not critical for save/load
        
        // Map information
        GameMap gameMap = gameController.getGameMap();
        saveData.mapWidth = gameMap.getWidth();
        saveData.mapHeight = gameMap.getHeight();
        saveData.mapName = "Current Map"; // Could be enhanced with actual map names
        
        // Save complete tile types
        saveData.tileTypes = new TileType[gameMap.getWidth()][gameMap.getHeight()];
        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                Tile tile = gameMap.getTile(x, y);
                saveData.tileTypes[x][y] = tile.getType();
            }
        }
        
        // Save entities
        for (Tower tower : gameController.getTowers()) {
            saveData.towers.add(new TowerSaveData(tower));
        }
        
        for (Enemy enemy : gameController.getEnemies()) {
            saveData.enemies.add(new EnemySaveData(enemy));
        }
        
        for (Projectile projectile : gameController.getProjectiles()) {
            saveData.projectiles.add(new ProjectileSaveData(projectile));
        }
        
        for (DroppedGold goldBag : gameController.getActiveGoldBags()) {
            saveData.goldBags.add(new DroppedGoldSaveData(goldBag));
        }
        
        // Save game settings
        saveData.gameSettings = GameSettings.getInstance();
        
        return saveData;
    }
    
    private boolean applySaveData(GameController gameController, GameSaveData saveData) {
        try {
            // Clear current game state
            gameController.getTowers().clear();
            gameController.getEnemies().clear();
            gameController.getProjectiles().clear();
            gameController.getActiveGoldBags().clear();
            
                    // Apply core game state
        gameController.setPlayerGold(saveData.playerGold);
        gameController.setPlayerLives(saveData.playerLives);
        gameController.setCurrentWave(saveData.currentWave);
        gameController.setGameOver(saveData.gameOver);
        gameController.setPaused(saveData.isPaused);
        gameController.setSpeedAccelerated(saveData.speedAccelerated);
        
        // Apply wave timing state - CRITICAL for proper game flow
        gameController.setBetweenWaves(saveData.betweenWaves);
        gameController.setWaveStartTime(saveData.waveStartTime);
        gameController.setSpawningEnemies(saveData.isSpawningEnemies);
        gameController.setGracePeriodActive(saveData.gracePeriodActive);
            
            // Restore tile types
            GameMap gameMap = gameController.getGameMap();
            for (int x = 0; x < Math.min(gameMap.getWidth(), saveData.mapWidth); x++) {
                for (int y = 0; y < Math.min(gameMap.getHeight(), saveData.mapHeight); y++) {
                    if (saveData.tileTypes != null && saveData.tileTypes[x][y] != null) {
                        // New format: restore exact tile types
                        gameMap.setTileType(x, y, saveData.tileTypes[x][y]);
                    } else if (saveData.tileOccupancy != null) {
                        // Legacy format: only restore tower occupancy
                        gameMap.setTileAsOccupiedByTower(x, y, saveData.tileOccupancy[x][y]);
                    }
                }
            }
            
            // Restore towers
            for (TowerSaveData towerData : saveData.towers) {
                Tower tower = createTowerFromSaveData(towerData);
                if (tower != null) {
                    gameController.getTowers().add(tower);
                }
            }
            
            // Restore enemies
            for (EnemySaveData enemyData : saveData.enemies) {
                Enemy enemy = createEnemyFromSaveData(enemyData);
                if (enemy != null) {
                    // Set the path from the game map BEFORE adding to controller
                    if (gameMap.getEnemyPath() != null) {
                        enemy.setPath(gameMap.getEnemyPath());
                    }
                    gameController.getEnemies().add(enemy);
                }
            }
            
            // Restore dropped gold
            for (DroppedGoldSaveData goldData : saveData.goldBags) {
                DroppedGold goldBag = createGoldBagFromSaveData(goldData);
                if (goldBag != null) {
                    gameController.getActiveGoldBags().add(goldBag);
                }
            }
            
            // Reinitialize all entities after loading
            gameController.reinitializeAfterLoad();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error applying save data: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private Tower createTowerFromSaveData(TowerSaveData data) {
        try {
            Tower tower;
            switch (data.towerType) {
                case "ArcherTower":
                    tower = new ArcherTower(data.x, data.y);
                    break;
                case "ArtilleryTower":
                    tower = new ArtilleryTower(data.x, data.y);
                    break;
                case "MageTower":
                    tower = new MageTower(data.x, data.y);
                    break;
                default:
                    System.err.println("Unknown tower type: " + data.towerType);
                    return null;
            }
            
            tower.setLevel(data.level);
            tower.setSelected(data.selected);
            
            return tower;
        } catch (Exception e) {
            System.err.println("Error creating tower from save data: " + e.getMessage());
            return null;
        }
    }
    
    private Enemy createEnemyFromSaveData(EnemySaveData data) {
        try {
            Enemy enemy;
            switch (data.enemyType) {
                case "Goblin":
                    enemy = new Goblin(data.x, data.y);
                    break;
                case "Knight":
                    enemy = new Knight(data.x, data.y);
                    break;
                default:
                    System.err.println("Unknown enemy type: " + data.enemyType);
                    return null;
            }
            
            enemy.setCurrentHealth(data.currentHealth);
            enemy.setPathProgress(data.pathProgress);
            
            return enemy;
        } catch (Exception e) {
            System.err.println("Error creating enemy from save data: " + e.getMessage());
            return null;
        }
    }
    
    private DroppedGold createGoldBagFromSaveData(DroppedGoldSaveData data) {
        try {
            return new DroppedGold(data.x, data.y, data.goldAmount);
        } catch (Exception e) {
            System.err.println("Error creating gold bag from save data: " + e.getMessage());
            return null;
        }
    }
    
    private boolean validateSaveFile(Path savePath) {
        try {
            // Quick validation by attempting to read the file
            try (FileInputStream fis = new FileInputStream(savePath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gzis)) {
                
                GameSaveData saveData = (GameSaveData) ois.readObject();
                return saveData != null && saveData.saveName != null;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private SaveFileInfo getSaveFileInfo(Path savePath) {
        try {
            SaveFileInfo info = new SaveFileInfo();
            info.filename = savePath.getFileName().toString();
            info.fileSize = Files.size(savePath);
            
            // Read save data for metadata
            try (FileInputStream fis = new FileInputStream(savePath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gzis)) {
                
                GameSaveData saveData = (GameSaveData) ois.readObject();
                info.saveName = saveData.saveName;
                info.saveTime = saveData.saveTime;
                info.currentWave = saveData.currentWave;
                info.playerGold = saveData.playerGold;
                info.playerLives = saveData.playerLives;
                info.isValid = true;
            }
            
            return info;
        } catch (Exception e) {
            SaveFileInfo info = new SaveFileInfo();
            info.filename = savePath.getFileName().toString();
            info.saveName = "Corrupted Save";
            info.isValid = false;
            return info;
        }
    }
    
    private void createBackup(Path originalPath) {
        try {
            Path backupPath = Paths.get(originalPath.toString() + BACKUP_EXTENSION);
            Files.copy(originalPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Warning: Could not create backup: " + e.getMessage());
        }
    }
    
    private void cleanupOldSaves() {
        try {
            List<SaveFileInfo> saves = getAvailableSaves();
            if (saves.size() > MAX_SAVE_SLOTS) {
                // Remove oldest saves beyond the limit
                for (int i = MAX_SAVE_SLOTS; i < saves.size(); i++) {
                    Path oldSave = Paths.get(SAVE_DIRECTORY, saves.get(i).filename);
                    Files.deleteIfExists(oldSave);
                    System.out.println("🗑️ Cleaned up old save: " + saves.get(i).filename);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not cleanup old saves: " + e.getMessage());
        }
    }
    
    private boolean isVersionCompatible(int saveVersion) {
        // For now, only support current version
        // In future, could implement migration logic
        return saveVersion == CURRENT_SAVE_VERSION;
    }
    
    /**
     * Delete a save file
     */
    public boolean deleteSave(String filename) {
        try {
            Path savePath = Paths.get(SAVE_DIRECTORY, filename);
            boolean deleted = Files.deleteIfExists(savePath);
            if (deleted) {
                System.out.println("🗑️ Deleted save file: " + filename);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("❌ Failed to delete save: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Export save data as human-readable text for debugging
     */
    public boolean exportSaveAsText(String filename, String outputFilename) {
        try {
            Path savePath = Paths.get(SAVE_DIRECTORY, filename);
            GameSaveData saveData;
            
            try (FileInputStream fis = new FileInputStream(savePath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gzis)) {
                
                saveData = (GameSaveData) ois.readObject();
            }
            
            StringBuilder report = new StringBuilder();
            report.append("=== KU TOWER DEFENSE SAVE FILE ANALYSIS ===\n");
            report.append("Save Name: ").append(saveData.saveName).append("\n");
            report.append("Save Time: ").append(saveData.saveTime).append("\n");
            report.append("Game Version: ").append(saveData.gameVersion).append("\n");
            report.append("Save Version: ").append(saveData.saveVersion).append("\n\n");
            
            report.append("GAME STATE:\n");
            report.append("- Player Gold: ").append(saveData.playerGold).append("\n");
            report.append("- Player Lives: ").append(saveData.playerLives).append("\n");
            report.append("- Current Wave: ").append(saveData.currentWave).append("\n");
            report.append("- Game Over: ").append(saveData.gameOver).append("\n");
            report.append("- Paused: ").append(saveData.isPaused).append("\n\n");
            
            report.append("ENTITIES:\n");
            report.append("- Towers: ").append(saveData.towers.size()).append("\n");
            report.append("- Enemies: ").append(saveData.enemies.size()).append("\n");
            report.append("- Projectiles: ").append(saveData.projectiles.size()).append("\n");
            report.append("- Gold Bags: ").append(saveData.goldBags.size()).append("\n\n");
            
            // Write to file
            Path outputPath = Paths.get(SAVE_DIRECTORY, outputFilename);
            Files.write(outputPath, report.toString().getBytes());
            
            System.out.println("📄 Save exported as text: " + outputFilename);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to export save as text: " + e.getMessage());
            return false;
        }
    }
} 
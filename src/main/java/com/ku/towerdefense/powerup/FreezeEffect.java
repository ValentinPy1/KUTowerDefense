package com.ku.towerdefense.powerup;

import com.ku.towerdefense.controller.GameController;
import com.ku.towerdefense.model.entity.Enemy;
import java.util.List;
import java.util.ArrayList;

/**
 * Power-up effect that freezes all enemies for a specified duration
 */
public class FreezeEffect implements PowerUpEffect {
    
    private static final double FREEZE_DURATION = 5.0; // 5 seconds
    
    private boolean active = false;
    private double remainingDuration = 0.0;
    private List<Enemy> frozenEnemies = new ArrayList<>();
    private List<Double> originalSpeeds = new ArrayList<>();
    
    @Override
    public boolean activate(GameController gameController) {
        if (active) {
            return false; // Already active
        }
        
        // Get all current enemies
        List<Enemy> enemies = gameController.getEnemies();
        if (enemies.isEmpty()) {
            return false; // No enemies to freeze
        }
        
        // Freeze all enemies
        frozenEnemies.clear();
        originalSpeeds.clear();
        
        for (Enemy enemy : enemies) {
            frozenEnemies.add(enemy);
            originalSpeeds.add(enemy.getSpeed());
            enemy.setSpeed(0.0); // Freeze by setting speed to 0
        }
        
        active = true;
        remainingDuration = FREEZE_DURATION;
        
        System.out.println("🧊 FREEZE ACTIVATED! " + frozenEnemies.size() + " enemies frozen for " + FREEZE_DURATION + " seconds!");
        
        return true;
    }
    
    @Override
    public boolean update(double deltaTime) {
        if (!active) {
            return false;
        }
        
        remainingDuration -= deltaTime;
        
        if (remainingDuration <= 0) {
            // Unfreeze all enemies
            unfreezeEnemies();
            active = false;
            System.out.println("❄️ Freeze effect ended - enemies unfrozen!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Unfreeze all frozen enemies by restoring their original speeds
     */
    private void unfreezeEnemies() {
        for (int i = 0; i < frozenEnemies.size() && i < originalSpeeds.size(); i++) {
            Enemy enemy = frozenEnemies.get(i);
            double originalSpeed = originalSpeeds.get(i);
            
            // Only unfreeze if the enemy still exists (wasn't killed while frozen)
            if (enemy != null) {
                enemy.setSpeed(originalSpeed);
            }
        }
        
        frozenEnemies.clear();
        originalSpeeds.clear();
    }
    
    @Override
    public double getRemainingDuration() {
        return remainingDuration;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public PowerUpType getType() {
        return PowerUpType.FREEZE_ENEMIES;
    }
    
    /**
     * Get the list of currently frozen enemies (for visual effects)
     * @return list of frozen enemies
     */
    public List<Enemy> getFrozenEnemies() {
        return new ArrayList<>(frozenEnemies);
    }
} 
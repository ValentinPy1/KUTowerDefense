package com.ku.towerdefense.powerup;

import com.ku.towerdefense.controller.GameController;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all power-ups in the game including cooldowns, costs, and activation
 */
public class PowerUpManager {
    
    private GameController gameController;
    private Map<PowerUpType, Long> cooldowns = new HashMap<>();
    private List<PowerUpEffect> activeEffects = new ArrayList<>();
    
    // Cooldown durations in waves
    private static final Map<PowerUpType, Integer> COOLDOWN_WAVES = new HashMap<>();
    static {
        COOLDOWN_WAVES.put(PowerUpType.FREEZE_ENEMIES, 2); // 2 waves cooldown
    }
    
    private int currentWave = 0;
    
    public PowerUpManager(GameController gameController) {
        this.gameController = gameController;
    }
    
    /**
     * Update the current wave number (called when a new wave starts)
     */
    public void setCurrentWave(int wave) {
        this.currentWave = wave;
    }
    
    /**
     * Check if a power-up can be used (not on cooldown and player has enough gold)
     */
    public boolean canUsePowerUp(PowerUpType type) {
        // Check cooldown
        if (isOnCooldown(type)) {
            return false;
        }
        
        // Check if player has enough gold
        int cost = type.getCost();
        if (gameController.getPlayerGold() < cost) {
            return false;
        }
        
        // Check if there are enemies to affect (for freeze)
        if (type == PowerUpType.FREEZE_ENEMIES && gameController.getEnemies().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Attempt to activate a power-up
     */
    public boolean activatePowerUp(PowerUpType type) {
        if (!canUsePowerUp(type)) {
            return false;
        }
        
        // Create the appropriate effect
        PowerUpEffect effect = createEffect(type);
        if (effect == null) {
            return false;
        }
        
        // Try to activate the effect
        if (effect.activate(gameController)) {
            // Deduct gold cost
            int cost = type.getCost();
            gameController.setPlayerGold(gameController.getPlayerGold() - cost);
            
            // Set cooldown
            setCooldown(type);
            
            // Add to active effects
            activeEffects.add(effect);
            
            System.out.println("💫 Power-up activated: " + type.getDisplayName() + " (Cost: " + cost + "G)");
            return true;
        }
        
        return false;
    }
    
    /**
     * Update all active power-up effects
     */
    public void update(double deltaTime) {
        // Update all active effects and remove finished ones
        activeEffects.removeIf(effect -> !effect.update(deltaTime));
    }
    
    /**
     * Check if a power-up type is currently on cooldown
     */
    public boolean isOnCooldown(PowerUpType type) {
        Long cooldownWave = cooldowns.get(type);
        if (cooldownWave == null) {
            return false;
        }
        
        return currentWave < cooldownWave;
    }
    
    /**
     * Get the number of waves remaining for cooldown
     */
    public int getCooldownWavesRemaining(PowerUpType type) {
        Long cooldownWave = cooldowns.get(type);
        if (cooldownWave == null) {
            return 0;
        }
        
        int remaining = (int)(cooldownWave - currentWave);
        return Math.max(0, remaining);
    }
    
    /**
     * Set cooldown for a power-up type
     */
    private void setCooldown(PowerUpType type) {
        Integer cooldownDuration = COOLDOWN_WAVES.get(type);
        if (cooldownDuration != null) {
            cooldowns.put(type, (long)(currentWave + cooldownDuration));
        }
    }
    
    /**
     * Create a power-up effect instance
     */
    private PowerUpEffect createEffect(PowerUpType type) {
        switch (type) {
            case FREEZE_ENEMIES:
                return new FreezeEffect();
            default:
                return null;
        }
    }
    
    /**
     * Get all currently active effects
     */
    public List<PowerUpEffect> getActiveEffects() {
        return new ArrayList<>(activeEffects);
    }
    
    /**
     * Get the active freeze effect if any
     */
    public FreezeEffect getActiveFreezeEffect() {
        for (PowerUpEffect effect : activeEffects) {
            if (effect instanceof FreezeEffect) {
                return (FreezeEffect) effect;
            }
        }
        return null;
    }
    
    /**
     * Check if any freeze effect is currently active
     */
    public boolean isFreezeActive() {
        return getActiveFreezeEffect() != null;
    }
} 
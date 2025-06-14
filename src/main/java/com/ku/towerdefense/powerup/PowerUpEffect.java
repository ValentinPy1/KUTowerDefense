package com.ku.towerdefense.powerup;

import com.ku.towerdefense.controller.GameController;

/**
 * Interface for all power-up effects in the game
 */
public interface PowerUpEffect {
    
    /**
     * Activate the power-up effect
     * @param gameController the game controller to apply effects to
     * @return true if the effect was successfully activated
     */
    boolean activate(GameController gameController);
    
    /**
     * Update the power-up effect (called each frame while active)
     * @param deltaTime time elapsed since last update
     * @return true if the effect is still active, false if it should be removed
     */
    boolean update(double deltaTime);
    
    /**
     * Get the remaining duration of the effect in seconds
     * @return remaining duration, or 0 if not active
     */
    double getRemainingDuration();
    
    /**
     * Check if this effect is currently active
     * @return true if active
     */
    boolean isActive();
    
    /**
     * Get the type of this power-up
     * @return the PowerUpType
     */
    PowerUpType getType();
} 
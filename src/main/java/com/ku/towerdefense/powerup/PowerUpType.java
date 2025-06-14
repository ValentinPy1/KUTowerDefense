package com.ku.towerdefense.powerup;

/**
 * Enum defining different types of power-ups available in the game
 */
public enum PowerUpType {
    FREEZE_ENEMIES("Freeze All Enemies", 500, "Freezes all enemies for 5 seconds", "❄️"),
    // Future power-ups can be added here
    // INSTANT_KILL("Lightning Strike", 800, "Instantly kills all enemies", "⚡"),
    // GOLD_RAIN("Gold Rain", 300, "Spawns extra gold", "💰")
    ;
    
    private final String displayName;
    private final int cost;
    private final String description;
    private final String icon;
    
    PowerUpType(String displayName, int cost, String description, String icon) {
        this.displayName = displayName;
        this.cost = cost;
        this.description = description;
        this.icon = icon;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getCost() {
        return cost;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getIcon() {
        return icon;
    }
} 
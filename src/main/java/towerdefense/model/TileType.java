package towerdefense.model;

/**
 * Enum representing the different types of tiles on the map.
 */
public enum TileType {
    GRASS, // Empty background
    PATH, // Path for enemies
    TOWER_SLOT, // Place where a tower can be built
    START, // Enemy spawn point (must be on a PATH tile)
    END, // Enemy exit point (must be on a PATH tile)
    DECOR_TREE, // Decorative tree
    DECOR_ROCK // Decorative rock
    // Add other types as needed (e.g., specific path curves, other decorations)
}
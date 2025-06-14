package com.ku.towerdefense.model.entity;

import com.ku.towerdefense.util.GameSettings;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OVERVIEW:
 *   Tower is a mutable ADT representing a defensive unit.
 *
 * ABSTRACT FUNCTION:
 *   AF(c) = a tower t where
 *     t.position = (c.x,c.y),
 *     t.stats = (c.damage,c.range,c.fireRate,c.damageType),
 *     t.level  = c.level,
 *     t.cost   = c.baseCost + upgrades (represented by getCost() method)
 *
 * REPRESENTATION INVARIANT:
 *   damage>0, range>0, fireRate>0,
 *   1<=level<=MAX_TOWER_LEVEL,
 *   baseDamage>0, baseRange>0, baseFireRate>0,
 *   width>0, height>0, x>=0, y>=0,
 *   damageType!=null, lastFireTime>=0
 */
public abstract class Tower extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected int damage;
    protected int range;
    protected long lastFireTime;
    protected long fireRate; // milliseconds between shots
    protected boolean selected;
    protected transient Image image; // Made transient, will be reloaded
    protected String imageFile; // Stores the *current* image file name
    protected DamageType damageType;

    protected int level;
    protected static final int MAX_TOWER_LEVEL = 2; // User requirement
    protected int baseDamage; // Store base damage for scaling
    protected int baseRange;  // Store base range for scaling
    protected long baseFireRate; // Store base fire rate for scaling

    protected static final double UPGRADE_COST_MULTIPLIER = 0.75; // How much base cost to add per level
    // protected static final double UPGRADE_STAT_MULTIPLIER = 0.25; // No longer used here, handled by subclasses

    /**
     * Constructor for towers with specified properties.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param width width of the tower
     * @param height height of the tower
     * @param damage damage dealt by each shot
     * @param range range in pixels
     * @param fireRate rate of fire in milliseconds
     * @param cost gold cost to build
     * @param damageType the type of damage dealt
     */
    public Tower(double x, double y, double width, double height, int damage, int range,
                 long fireRate, int cost, DamageType damageType) {
        super(x, y, width, height);
        this.baseDamage = damage;
        this.baseRange = range;
        this.baseFireRate = fireRate;
        this.damage = damage;
        this.range = range;
        this.fireRate = fireRate;
        this.lastFireTime = 0;
        this.selected = false;
        this.damageType = damageType;
        this.level = 1;
        this.imageFile = getBaseImageName(); // Set initial image
        loadImage(); // Load initial image
    }

    public boolean repOk() {
        if (!(damage>0 && range>0 && fireRate>0)) return false;
        if (level<1 || level>MAX_TOWER_LEVEL) return false;
        if (!(baseDamage>0 && baseRange>0 && baseFireRate>0)) return false;
        // Entity class (superclass of Tower) should ideally enforce width > 0, height > 0, x >= 0, y >= 0
        // So, we rely on Entity's constructor or setters to maintain these.
        // If Entity does not guarantee this, checks should be added here or in Entity.repOk()
        if (!(width>0 && height>0 && x>=0 && y>=0)) return false; // Added as per explicit requirement
        if (damageType==null || lastFireTime<0) return false;
        return true;
    }

    /**
     * Update the tower's state and generate projectiles if needed.
     *
     * @param deltaTime time elapsed since the last update (in seconds)
     * @param enemies list of enemies in the game
     * @return a projectile if the tower fires, or null if not
     */
    public Projectile update(double deltaTime, List<Enemy> enemies) {
        if (enemies.isEmpty()) {
            return null;
        }

        // Check if enough time has passed since the last shot
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFireTime < fireRate) {
            return null;
        }

        // Find the enemy that has progressed furthest along the path within range
        Enemy target = findBestTarget(enemies);
        if (target == null) {
            return null;
        }

        // Fire at the target
        lastFireTime = currentTime;
        return createProjectile(target);
    }

    /**
     * Find the best target based on path progression.
     * The best target is the enemy that has progressed furthest along the path
     * and is within the tower's range.
     *
     * @param enemies list of all enemies
     * @return the best target enemy, or null if no enemies are in range
     */
    protected Enemy findBestTarget(List<Enemy> enemies) {
        // Get center coordinates for range calculation
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        // Filter enemies that are in range
        List<Enemy> enemiesInRange = enemies.stream()
                .filter(enemy -> isInRange(enemy, centerX, centerY))
                .collect(Collectors.toList());

        if (enemiesInRange.isEmpty()) {
            return null;
        }

        // Find the enemy with the highest path progress
        return enemiesInRange.stream()
                .max((e1, e2) -> Double.compare(e1.getPathProgress(), e2.getPathProgress()))
                .orElse(null);
    }

    /**
     * Check if an enemy is in range of this tower.
     *
     * @param enemy the enemy to check
     * @param centerX the x coordinate of the tower's center
     * @param centerY the y coordinate of the tower's center
     * @return true if the enemy is in range, false otherwise
     */
    protected boolean isInRange(Enemy enemy, double centerX, double centerY) {
        double enemyCenterX = enemy.getX() + enemy.getWidth() / 2;
        double enemyCenterY = enemy.getY() + enemy.getHeight() / 2;

        double distance = Math.sqrt(
                Math.pow(centerX - enemyCenterX, 2) +
                        Math.pow(centerY - enemyCenterY, 2)
        );

        return distance <= range;
    }

    /**
     * Create a projectile targeting the specified enemy.
     *
     * @param target the target enemy
     * @return a new projectile
     */
    protected abstract Projectile createProjectile(Enemy target);

    /**
     * Render the tower.
     *
     * @param gc the graphics context to render on
     */
    @Override
    public void render(GraphicsContext gc) {
        // Load image if not already loaded (e.g., after deserialization or if it failed first time)
        if (image == null && imageFile != null && !imageFile.isEmpty()) {
            loadImage();
        }

        // Use consistent standard size for all towers regardless of image dimensions
        double drawX = x;
        double drawY = y;
        double standardWidth = 64.0;  // Force consistent size
        double standardHeight = 64.0; // Force consistent size

        // Draw the tower image if available, always at the standard size
        if (image != null) {
            // Always render at the standard size, regardless of image or Entity dimensions
            gc.drawImage(image, drawX, drawY, standardWidth, standardHeight);
        } else {
            // Fallback to a simple shape if no image, using standard size
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(drawX, drawY, standardWidth, standardHeight);
        }

        // Level is shown by different images, no text indicator needed

        // Draw range circle if selected
        if (selected) {
            renderRangeCircle(gc);
        }
    }

    /**
     * Reinitialize after deserialization to reload images
     */
    public void reinitializeAfterLoad() {
        // CRITICAL: Update imageFile to match the tower's current level
        // This ensures level 2 towers show the correct upgraded texture
        if (level == MAX_TOWER_LEVEL) {
            this.imageFile = getUpgradedImageName();
        } else {
            this.imageFile = getBaseImageName();
        }
        
        // Now load the correct image for the tower's level
        if (this.imageFile != null && !this.imageFile.isEmpty()) {
            loadImage(); // This will load the correct image for the current level
        }
    }

    /**
     * Loads the tower image from the specified file path (relative to resources).
     * Expects imageFile to be like "Asset_pack/Towers/tower_name.png"
     */
    protected void loadImage() {
        if (imageFile == null || imageFile.isEmpty()) {
            System.err.println("Cannot load image: imageFile is null or empty for " + getName());
            return;
        }
        try {
            String resourcePath = "/" + imageFile; // Assuming imageFile is relative to resources root e.g. "Asset_pack/Towers/archer.png"
            Image newImage = new Image(getClass().getResourceAsStream(resourcePath));
            if (newImage != null && !newImage.isError()) {
                this.image = newImage;
                // System.out.println("Successfully loaded tower image: " + resourcePath + " for " + getName());
            } else {
                if (newImage != null && newImage.isError()) {
                    System.err.println("Error loading tower image (isError=true): " + resourcePath + " for " + getName() + ". Error: " + newImage.getException());
                } else {
                    System.err.println("Failed to load tower image (null): " + resourcePath + " for " + getName());
                }
                // Fallback if needed - though ideally all images should load
                // this.image = null; // or a default placeholder
            }
        } catch (Exception e) {
            System.err.println("Exception loading tower image " + imageFile + " for " + getName() + ": " + e.getMessage());
            e.printStackTrace(); // More detailed error
            // this.image = null;
        }
    }

    /**
     * Render the range circle around the tower.
     *
     * @param gc the graphics context to render on
     */
    protected void renderRangeCircle(GraphicsContext gc) {
        double centerX = x + width / 2;
        double centerY = y + height / 2;

        // Draw a semi-transparent circle showing the tower's range
        gc.setGlobalAlpha(0.3);
        gc.setFill(Color.WHITE);
        gc.fillOval(centerX - range, centerY - range, range * 2, range * 2);

        // Draw a border for the range circle
        gc.setGlobalAlpha(0.7);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeOval(centerX - range, centerY - range, range * 2, range * 2);

        // Reset alpha
        gc.setGlobalAlpha(1.0);
    }


    /**
     * Calculate the refund amount when selling this tower.
     *
     * @return gold amount refunded
     */
    public int getSellRefund() {
        // Refund 75% of the cost
        return (int)(getBaseCost() * 0.75);
    }

    // Getters and setters

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getCost() {
        // Current cost might be same as base cost, or could include level factor if towers are bought pre-levelled
        // For now, cost is just the base cost for initial purchase
        return getBaseCost();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public long getFireRate() {
        return fireRate;
    }

    public void setFireRate(long fireRate) {
        this.fireRate = fireRate;
    }

    public double getCenterX() {
        return x + width / 2;
    }

    public double getCenterY() {
        return y + height / 2;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
        this.image = null; // Force reload if path changes
    }

    public Image getImage() {
        return image;
    }

    public abstract String getName(); // e.g., "Archer Tower"
    public abstract int getBaseCost(); // The initial cost of the tower at level 1
    protected abstract String getBaseImageName(); // e.g., "Asset_pack/Towers/archer.png"
    protected abstract String getUpgradedImageName(); // e.g., "Asset_pack/Towers/archer_up.png"

    public int getLevel() {
        return level;
    }

    public int getMaxLevel() {
        return MAX_TOWER_LEVEL;
    }

    public boolean canUpgrade() {
        return level < MAX_TOWER_LEVEL;
    }

    /**
     * Gets the cost to upgrade to the next level.
     * For simplicity, let's say upgrading from L1 to L2 costs UPGRADE_COST_MULTIPLIER * baseCost.
     * If already max level, cost is effectively infinite (or not applicable).
     * @return The cost to upgrade, or -1 (or Integer.MAX_VALUE) if not upgradeable.
     */
    public int getUpgradeCost() {
        if (canUpgrade()) {
            // Cost to upgrade from level 1 to level 2
            if (level == 1) {
                return (int) (getBaseCost() * UPGRADE_COST_MULTIPLIER);
            }
            // Add more tiers if MAX_TOWER_LEVEL > 2
            // else if (level == 2) { ... } 
        }
        return Integer.MAX_VALUE; // Cannot be upgraded or no defined cost for this level
    }

    /**
     * Upgrades the tower to the next level if possible.
     * Increases stats and changes image.
     * @return true if upgrade was successful, false otherwise.
     */
    public boolean upgrade() {
        if (!canUpgrade()) {
            return false;
        }
        level++;

        // Subclasses will apply specific L2 stat changes after calling super.upgrade()

        // Update image to L2 image
        if (level == MAX_TOWER_LEVEL) { // Check against MAX_TOWER_LEVEL for clarity
            this.imageFile = getUpgradedImageName();
        }
        // else if (level == 1) { // If downgrading was possible, reset to base image
        //     this.imageFile = getBaseImageName();
        // }

        loadImage(); // Reload the image for the new level

        System.out.println(getName() + " upgraded to level " + level + ". Image will be updated.");
        return true;
    }

    public void setLevel(int level) {
        // Allow setting any level for repOk testing purposes.
        // Business logic for valid upgrades is in the upgrade() method and constructor.
        // repOk() is responsible for validating the current state.
        this.level = level;
    }

    public abstract Tower cloneTower();
} 
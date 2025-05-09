package com.ku.towerdefense.model.entity;

import com.ku.towerdefense.model.GamePath;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all enemy types in the game.
 */
public abstract class Enemy extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    // Static image cache for enemy types
    private static final Map<EnemyType, Image> ENEMY_IMAGES = new HashMap<>();

    // Static initializer
    static {
        loadEnemyImages();
    }

    protected int maxHealth;
    protected int currentHealth;
    protected double speed; // pixels per second
    protected int goldReward;
    protected EnemyType type;

    protected GamePath path;
    protected double pathProgress; // 0.0 to 1.0
    protected double distanceTraveled;
    protected double totalPathDistance;

    protected Image image;
    protected String imageFile;

    /**
     * Constructor for the Enemy class.
     *
     * @param x initial x position
     * @param y initial y position
     * @param width width of the enemy
     * @param height height of the enemy
     * @param health the enemy's health
     * @param speed the movement speed
     * @param goldReward the gold rewarded when defeated
     */
    public Enemy(double x, double y, double width, double height, int health, double speed, int goldReward) {
        super(x, y, width, height);
        this.maxHealth = health;
        this.currentHealth = health;
        this.speed = speed;
        this.pathProgress = 0.0;
        this.goldReward = goldReward;
        this.distanceTraveled = 0;
        this.totalPathDistance = 0;
    }

    /**
     * Constructor for the Enemy class with size 64x64.
     *
     * @param x initial x position
     * @param y initial y position
     * @param health the enemy's health
     * @param speed the movement speed
     * @param goldReward the gold rewarded when defeated
     */
    public Enemy(double x, double y, int health, double speed, int goldReward) {
        this(x, y, 32, 32, health, speed, goldReward);
    }

    /**
     * Constructor for an enemy.
     *
     * @param x initial x position
     * @param y initial y position
     * @param width width
     * @param height height
     * @param health health points
     * @param speed movement speed in pixels per second
     * @param goldReward gold reward when defeated
     * @param type the type of enemy
     */
    public Enemy(double x, double y, double width, double height,
                 int health, double speed, int goldReward, EnemyType type) {
        super(x, y, width, height);
        this.maxHealth = health;
        this.currentHealth = health;
        this.speed = speed;
        this.goldReward = goldReward;
        this.type = type;
        this.distanceTraveled = 0;
        this.totalPathDistance = 0;
        this.pathProgress = 0.0;

        // Set image from cache
        this.image = ENEMY_IMAGES.get(type);
    }

    /**
     * Load all enemy images into the static cache
     */
    private static void loadEnemyImages() {
        try {
            // Use classpath resources instead of absolute file paths
            // Load enemy images from the classpath resources
            String goblinImagePath = "/Asset_pack/Enemies/Goblin_Red.png";
            Image goblinImage = new Image(Enemy.class.getResourceAsStream(goblinImagePath));
            if (goblinImage != null && !goblinImage.isError()) {
                ENEMY_IMAGES.put(EnemyType.GOBLIN, goblinImage);
                System.out.println("Loaded Goblin image from classpath: " + goblinImagePath);
            } else {
                System.err.println("Error loading Goblin image from classpath: " + goblinImagePath);
            }

            // Knight
            String knightImagePath = "/Asset_pack/Enemies/Warrior_Blue.png";
            Image knightImage = new Image(Enemy.class.getResourceAsStream(knightImagePath));
            if (knightImage != null && !knightImage.isError()) {
                ENEMY_IMAGES.put(EnemyType.KNIGHT, knightImage);
                System.out.println("Loaded Knight image from classpath: " + knightImagePath);
            } else {
                System.err.println("Error loading Knight image from classpath: " + knightImagePath);
            }
        } catch (Exception e) {
            System.err.println("Error loading enemy images: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the enemy state.
     *
     * @param deltaTime time elapsed since last update in seconds
     * @return true if the enemy reached the end of the path
     */
    public boolean update(double deltaTime) {
        if (path == null) {
            System.err.println("Enemy has no path to follow!");
            return false;
        }

        // Calculate the distance to move based on speed and time
        double distanceToMove = speed * deltaTime;

        // Convert to path progress (0.0 to 1.0)
        double progressIncrement = distanceToMove / totalPathDistance;
        pathProgress += progressIncrement;

        // Cap progress at 1.0 (end of path)
        if (pathProgress > 1.0) {
            pathProgress = 1.0;
            return true; // Reached the end
        }

        // Calculate new position based on path progress
        double[] newPosition = path.getPositionAt(pathProgress);
        if (newPosition == null) {
            System.err.println("Failed to get position at progress: " + pathProgress);
            return false;
        }

        // Update position (centered on the path)
        this.x = newPosition[0] - width / 2;
        this.y = newPosition[1] - height / 2;

        // Update distance traveled
        distanceTraveled += distanceToMove;

        return false;
    }

    /**
     * Set the path for this enemy to follow.
     *
     * @param path the path to follow
     */
    public void setPath(GamePath path) {
        this.path = path;
        this.totalPathDistance = path.calculateTotalLength();
        this.pathProgress = 0.0;
        this.distanceTraveled = 0;

        // Set the initial position to the start of the path
        double[] startPos = path.getPositionAt(0);
        if (startPos != null) {
            this.x = startPos[0] - width / 2;
            this.y = startPos[1] - height / 2;
            System.out.println("Enemy path set, starting at (" + x + "," + y + ")");
        } else {
            System.err.println("Failed to get start position from path!");
        }
    }

    /**
     * Render the enemy and its health bar.
     *
     * @param gc the graphics context to render on
     */
    @Override
    public void render(GraphicsContext gc) {
        // Load image if not already loaded
        if (image == null && imageFile != null) {
            loadImage();
        }

        // Draw the enemy image if available
        if (image != null) {
            gc.drawImage(image, x, y, width, height);
        } else {
            // Fallback to a simple shape if no image
            gc.setFill(Color.RED);
            gc.fillOval(x, y, width, height);
        }

        // Draw health bar
        renderHealthBar(gc);
    }

    /**
     * Load the enemy image from file.
     */
    protected void loadImage() {
        try {
            // First check if we can get the image from the static cache based on type
            if (this.type != null) {
                Image cachedImage = ENEMY_IMAGES.get(this.type);
                if (cachedImage != null) {
                    this.image = cachedImage;
                    return;
                }
            }
            
            // Then try to parse the imageFile to see if it's an absolute path or a resource path
            if (imageFile != null && !imageFile.isEmpty()) {
                // If it's an absolute path, try to extract just the filename
                String resourcePath;
                if (imageFile.contains("Asset_pack") || imageFile.contains("assets")) {
                    // Extract just the file name from the path
                    int lastSlash = Math.max(imageFile.lastIndexOf('\\'), imageFile.lastIndexOf('/'));
                    if (lastSlash >= 0 && lastSlash < imageFile.length() - 1) {
                        String fileName = imageFile.substring(lastSlash + 1);
                        // Try to load using the extracted file name in our standard path
                        resourcePath = "/Asset_pack/Enemies/" + fileName;
                    } else {
                        resourcePath = "/Asset_pack/Enemies/" + imageFile;
                    }
                } else {
                    resourcePath = "/Asset_pack/Enemies/" + imageFile;
                }
                
                // Try to load the resource
                try {
                    image = new Image(getClass().getResourceAsStream(resourcePath));
                    if (image != null && !image.isError()) {
                        System.out.println("Loaded image for " + getClass().getSimpleName() + " from classpath: " + resourcePath);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Could not load image from classpath: " + resourcePath + " - " + e.getMessage());
                }
                
                // Fallback to file system only if absolutely necessary
                try {
                    File file = new File(imageFile);
                    if (file.exists()) {
                        image = new Image(file.toURI().toString());
                        System.out.println("Loaded image for " + getClass().getSimpleName() + " from file: " + imageFile);
                    } else {
                        System.err.println("Image file not found: " + imageFile);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading from file system: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading image " + imageFile + ": " + e.getMessage());
        }
    }

    /**
     * Render the health bar above the enemy.
     *
     * @param gc the graphics context to render on
     */
    private void renderHealthBar(GraphicsContext gc) {
        // Calculate health bar dimensions
        double barWidth = width * 0.8;
        double barHeight = 8; // Increased height for better visibility
        double barY = y - 15; // Position higher above the enemy
        double barX = x + (width - barWidth) / 2;

        // Draw background (full health bar)
        gc.setFill(Color.rgb(50, 0, 0)); // Darker red background
        gc.fillRect(barX, barY, barWidth, barHeight);

        // Draw current health with color gradient based on health percentage
        double healthPercentage = (double) currentHealth / maxHealth;
        double healthWidth = barWidth * healthPercentage;

        // Color gradient from red to green based on health percentage
        Color healthColor;
        if (healthPercentage > 0.6) {
            healthColor = Color.rgb(0, 200, 0); // Brighter green
        } else if (healthPercentage > 0.3) {
            healthColor = Color.rgb(255, 165, 0); // Brighter orange
        } else {
            healthColor = Color.rgb(255, 0, 0); // Brighter red
        }

        // Draw health bar with a slight glow effect
        gc.setFill(healthColor);
        gc.fillRect(barX, barY, healthWidth, barHeight);

        // Draw border with a thicker line
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(barX, barY, barWidth, barHeight);

        // Draw health text with better positioning and visibility
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        String healthText = currentHealth + "/" + maxHealth;

        // Center the text above the health bar
        double textX = barX + (barWidth - gc.getFont().getSize() * healthText.length() / 2) / 2;
        double textY = barY - 5;

        // Draw text with outline for better visibility
        gc.strokeText(healthText, textX, textY);
        gc.fillText(healthText, textX, textY);
    }

    /**
     * Apply damage to the enemy.
     *
     * @param amount amount of damage to apply
     * @return true if the enemy was defeated
     */
    public boolean applyDamage(int amount) {
        currentHealth -= amount;
        return currentHealth <= 0;
    }

    /**
     * Apply damage to the enemy with a specific damage type.
     *
     * @param amount amount of damage to apply
     * @param damageType type of damage
     * @return true if the enemy was defeated
     */
    public abstract boolean applyDamage(int amount, DamageType damageType);

    /**
     * Calculate the distance traveled along the path.
     *
     * @return distance in pixels
     */
    public double getDistanceTraveled() {
        if (path == null) {
            return 0.0;
        }
        return path.calculateTotalLength() * pathProgress;
    }

    /**
     * Calculate the distance to another entity.
     *
     * @param other the other entity
     * @return the distance in pixels
     */
    public double distanceTo(Entity other) {
        double centerX = this.x + this.width / 2;
        double centerY = this.y + this.height / 2;
        double otherCenterX = other.getX() + other.getWidth() / 2;
        double otherCenterY = other.getY() + other.getHeight() / 2;

        double deltaX = centerX - otherCenterX;
        double deltaY = centerY - otherCenterY;

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    /**
     * Get the enemy's path progress as a percentage (0.0 to 1.0).
     *
     * @return path progress percentage
     */
    public double getPathProgressPercentage() {
        if (totalPathDistance <= 0) {
            return 0;
        }
        return Math.min(pathProgress, 1.0);
    }

    // Getters and setters
    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public void setGoldReward(int goldReward) {
        this.goldReward = goldReward;
    }

    public EnemyType getType() {
        return type;
    }

    public double getPathProgress() {
        return pathProgress;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    /**
     * Reinitialize after deserialization - use this to reload images
     */
    public void reinitializeAfterLoad() {
        // Reload enemy image from the cache based on type
        if (this.type != null) {
            this.image = ENEMY_IMAGES.get(this.type);
            
            // If cache didn't have the image, try to reload from file
            if (this.image == null && this.imageFile != null) {
                loadImage();
            }
        }
    }

    /**
     * Enum of enemy types.
     */
    public enum EnemyType {
        GOBLIN,
        KNIGHT
    }
} 
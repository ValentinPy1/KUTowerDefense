package com.ku.towerdefense.model.entity;

import com.ku.towerdefense.model.GamePath;
import com.ku.towerdefense.ui.UIAssets;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all enemy types in the game.
 */
public abstract class Enemy extends Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- Sprite Sheet Loading ---
    private static class SpriteSheetInfo {
        final Image spriteSheet;
        final int frameCount;
        final double frameWidth;
        final double frameHeight;

        SpriteSheetInfo(Image sheet, int count) {
            this.spriteSheet = sheet;
            this.frameCount = count;
            if (sheet != null && count > 0) {
                this.frameWidth = sheet.getWidth() / count;
                this.frameHeight = sheet.getHeight();
            } else {
                this.frameWidth = 0;
                this.frameHeight = 0;
                System.err.println("Error creating SpriteSheetInfo: Invalid image or frame count.");
            }
        }
    }

    private static final Map<EnemyType, SpriteSheetInfo> ENEMY_SPRITE_INFO = new HashMap<>();
    private static transient Image snowflakeIcon; // For slow effect
    private static transient Image thunderIcon; // For Knight synergy (corrected name)

    // Static initializer
    static {
        loadEnemySpriteSheets(); // Renamed method
        loadEffectIcons();
    }
    // --- End Sprite Sheet Loading ---

    protected int maxHealth;
    protected int currentHealth;
    protected double speed; // pixels per second
    protected int goldReward;
    protected EnemyType type;

    protected GamePath path;
    protected double pathProgress; // 0.0 to 1.0
    protected double distanceTraveled;
    protected double totalPathDistance;

    // --- Animation Fields ---
    protected transient SpriteSheetInfo spriteInfo; // Transient: will be re-initialized after load
    protected int currentFrameIndex = 0;
    protected double frameDuration = 0.1; // seconds per frame (e.g., 10 FPS)
    protected double animationTimer = 0;
    // --- End Animation Fields ---

    // --- Status Effect Fields ---
    protected boolean isSlowed = false;
    protected double slowTimer = 0; // seconds
    protected double slowFactor = 1.0; // e.g., 0.8 for 20% slow (speed * factor)
    protected boolean isKnightSpeedBoosted = false; // For combat synergy thunder icon
    // --- End Status Effect Fields ---

    // protected Image image; // Replaced by spriteInfo
    protected String imageFile; // Keep for potential future use or different loading mechanisms if needed

    /**
     * Constructor for the Enemy class.
     *
     * @param x          initial x position
     * @param y          initial y position
     * @param width      width of the enemy
     * @param height     height of the enemy
     * @param health     the enemy's health
     * @param speed      the movement speed
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
        // Note: Type is not set here, so spriteInfo won't be loaded initially.
        // This constructor might need adjustment depending on how enemies are created
        // without type.
    }

    /**
     * Constructor for the Enemy class with size 64x64.
     *
     * @param x          initial x position
     * @param y          initial y position
     * @param health     the enemy's health
     * @param speed      the movement speed
     * @param goldReward the gold rewarded when defeated
     */
    public Enemy(double x, double y, int health, double speed, int goldReward) {
        this(x, y, 48, 48, health, speed, goldReward);
    }

    /**
     * Constructor for an enemy.
     *
     * @param x          initial x position
     * @param y          initial y position
     * @param width      width
     * @param height     height
     * @param health     health points
     * @param speed      movement speed in pixels per second
     * @param goldReward gold reward when defeated
     * @param type       the type of enemy
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

        // Set sprite info from cache
        this.spriteInfo = ENEMY_SPRITE_INFO.get(type);
        if (this.spriteInfo == null) {
            System.err.println("SpriteSheetInfo not found for type: " + type);
            // Consider setting a default/fallback if needed
        }

        // Reset animation state
        this.currentFrameIndex = 0;
        this.animationTimer = 0;
    }

    /**
     * Load all enemy sprite sheets into the static cache
     */
    private static void loadEnemySpriteSheets() {
        // --- Load Goblin ---
        String goblinSheetPath = "/Asset_pack/Enemies/Goblin_Red.png"; // Corrected filename
        try {
            Image goblinSheet = new Image(Enemy.class.getResourceAsStream(goblinSheetPath));
            if (goblinSheet != null && !goblinSheet.isError()) {
                int frameCount = 6; // Assuming 6 frames
                ENEMY_SPRITE_INFO.put(EnemyType.GOBLIN, new SpriteSheetInfo(goblinSheet, frameCount));
                System.out.println(
                        "Loaded Goblin spritesheet (" + frameCount + " frames) from classpath: " + goblinSheetPath);
            } else {
                System.err.println("Error loading Goblin spritesheet from classpath: " + goblinSheetPath
                        + (goblinSheet == null ? " - Stream is null" : " - Image has error"));
            }
        } catch (Exception e) {
            System.err.println("Exception loading Goblin spritesheet: " + e.getMessage());
            e.printStackTrace();
        }

        // --- Load Knight ---
        String knightSheetPath = "/Asset_pack/Enemies/Warrior_Blue.png"; // Corrected filename
        try {
            Image knightSheet = new Image(Enemy.class.getResourceAsStream(knightSheetPath));
            if (knightSheet != null && !knightSheet.isError()) {
                int frameCount = 6; // Assuming 6 frames
                ENEMY_SPRITE_INFO.put(EnemyType.KNIGHT, new SpriteSheetInfo(knightSheet, frameCount));
                System.out.println(
                        "Loaded Knight spritesheet (" + frameCount + " frames) from classpath: " + knightSheetPath);
            } else {
                System.err.println("Error loading Knight spritesheet from classpath: " + knightSheetPath
                        + (knightSheet == null ? " - Stream is null" : " - Image has error"));
            }
        } catch (Exception e) {
            System.err.println("Exception loading Knight spritesheet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadEffectIcons() {
        try {
            // Load snowflake icon and remove white background
            String snowflakePath = "/Asset_pack/Effects/snow_flake_icon.png";
            try {
                Image originalSnow = new Image(Enemy.class.getResourceAsStream(snowflakePath));
                if (originalSnow != null && !originalSnow.isError()) {
                    snowflakeIcon = removeWhiteBackground(originalSnow);
                    System.out.println("Loaded and processed snowflake icon from: " + snowflakePath);
            } else {
                    throw new Exception("Failed to load original image");
                }
            } catch (Exception e) {
                System.out.println("Creating fallback snowflake icon due to: " + e.getMessage());
                snowflakeIcon = createFallbackIcon(javafx.scene.paint.Color.LIGHTBLUE, 16);
            }

            // Load thunder icon and remove white background
            String thunderPath = "/Asset_pack/Effects/thunder_icon.png";
            try {
                Image originalThunder = new Image(Enemy.class.getResourceAsStream(thunderPath));
                if (originalThunder != null && !originalThunder.isError()) {
                    thunderIcon = removeWhiteBackground(originalThunder);
                    System.out.println("Loaded and processed thunder icon from: " + thunderPath);
            } else {
                    throw new Exception("Failed to load original image");
                }
            } catch (Exception e) {
                System.out.println("Creating fallback thunder icon due to: " + e.getMessage());
                thunderIcon = createFallbackIcon(javafx.scene.paint.Color.YELLOW, 16);
            }
            
        } catch (Exception e) {
            System.err.println("Exception loading effect icons: " + e.getMessage());
            e.printStackTrace();
            // Create fallback icons
            snowflakeIcon = createFallbackIcon(javafx.scene.paint.Color.LIGHTBLUE, 16);
            thunderIcon = createFallbackIcon(javafx.scene.paint.Color.YELLOW, 16);
        }
    }
    
    /**
     * Remove white background from an image by making white pixels transparent
     */
    private static Image removeWhiteBackground(Image originalImage) {
        try {
            int width = (int) originalImage.getWidth();
            int height = (int) originalImage.getHeight();
            
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(width, height);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Clear to transparent
            gc.clearRect(0, 0, width, height);
            
            // Get pixel reader from original image
            javafx.scene.image.PixelReader pixelReader = originalImage.getPixelReader();
            if (pixelReader == null) {
                return originalImage; // Return original if can't read pixels
            }
            
            // Create writable image
            javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter pixelWriter = writableImage.getPixelWriter();
            
            // Process each pixel
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    javafx.scene.paint.Color color = pixelReader.getColor(x, y);
                    
                    // If pixel is white or very light, make it transparent
                    if (color.getRed() > 0.9 && color.getGreen() > 0.9 && color.getBlue() > 0.9) {
                        pixelWriter.setColor(x, y, javafx.scene.paint.Color.TRANSPARENT);
                    } else {
                        pixelWriter.setColor(x, y, color);
                    }
                }
            }
            
            return writableImage;
        } catch (Exception e) {
            System.err.println("Failed to remove white background: " + e.getMessage());
            return originalImage; // Return original on error
        }
    }
    
    /**
     * Create a simple colored circle as a fallback icon with transparent background
     */
    private static Image createFallbackIcon(javafx.scene.paint.Color color, int size) {
        try {
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(size, size);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Clear to ensure transparent background
            gc.clearRect(0, 0, size, size);
            
            // Fill with solid color circle
            gc.setFill(color);
            gc.fillOval(2, 2, size - 4, size - 4);
            
            // Add a dark border for visibility
            gc.setStroke(javafx.scene.paint.Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeOval(2, 2, size - 4, size - 4);
            
            // Create with transparent background
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.TRANSPARENT);
            return canvas.snapshot(params, null);
        } catch (Exception e) {
            System.err.println("Failed to create fallback icon: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates the enemy state, including movement and animation.
     *
     * @param deltaTime  time elapsed since last update in seconds
     * @param allEnemies list of all active enemies for synergy checks
     * @return true if the enemy reached the end of the path
     */
    public boolean update(double deltaTime, List<Enemy> allEnemies) {
        // System.out.println("[Enemy " + this.hashCode() + " type "+ this.type + "]
        // update called. Received deltaTime: " + deltaTime + ". Current speed: " +
        // this.speed * (isSlowed ? slowFactor : 1.0) ); // DEBUG LINE REMOVED

        // --- Status Effects Update ---
        if (isSlowed) {
            slowTimer -= deltaTime;
            if (slowTimer <= 0) {
                isSlowed = false;
                slowFactor = 1.0;
                slowTimer = 0;
            }
        }
        // --- End Status Effects Update ---

        // --- Animation Update ---
        if (spriteInfo != null && spriteInfo.frameCount > 1) { // Only animate if there are multiple frames
            animationTimer += deltaTime;
            if (animationTimer >= frameDuration) {
                animationTimer -= frameDuration;
                currentFrameIndex = (currentFrameIndex + 1) % spriteInfo.frameCount;
            }
        }
        // --- End Animation Update ---

        // --- Movement Update ---
        if (path == null) {
            // System.err.println("Enemy has no path to follow!"); // Reduced verbosity
            return false;
        }
        if (totalPathDistance <= 0) { // Avoid division by zero if path length is 0
            return false;
        }

        // Calculate the distance to move based on speed and time
        double currentSpeed = this.speed;
        if (isSlowed) {
            currentSpeed *= slowFactor;
        }
        // Knight combat synergy speed adjustment will be handled in Knight.update()
        // before this
        // or passed into this update method if Enemy class needs to be aware of the
        // final speed source.
        // For now, assume Knight.update() adjusts this.speed directly if boosted.

        double distanceToMove = currentSpeed * deltaTime;

        // Convert to path progress (0.0 to 1.0)
        double progressIncrement = distanceToMove / totalPathDistance;
        pathProgress += progressIncrement;

        // Cap progress at 1.0 (end of path)
        if (pathProgress >= 1.0) {
            pathProgress = 1.0;
            // Set position to the exact end point before returning true
            double[] endPos = path.getPositionAt(1.0);
            if (endPos != null) {
                this.x = endPos[0] - width / 2;
                this.y = endPos[1] - height / 2;
            }
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
        // --- End Movement Update ---

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
     * Render the enemy (current animation frame) and its health bar.
     *
     * @param gc the graphics context to render on
     */
    @Override
    public void render(GraphicsContext gc) {
        // Re-check spriteInfo in case it was loaded late or after deserialization
        if (spriteInfo == null && this.type != null) {
            this.spriteInfo = ENEMY_SPRITE_INFO.get(this.type);
            // If still null after check, log error once?
            if (this.spriteInfo == null) {
                System.err.println("Missing SpriteSheetInfo for rendering type: " + type);
            }
        }

        // Draw the current frame of the enemy sprite sheet if available
        if (spriteInfo != null && spriteInfo.spriteSheet != null && spriteInfo.frameCount > 0) {
            // Source rectangle (sx, sy, sw, sh) within the sprite sheet
            double sx = spriteInfo.frameWidth * currentFrameIndex;
            double sy = 0; // Assuming frames are only horizontal
            double sw = spriteInfo.frameWidth;
            double sh = spriteInfo.frameHeight;

            // Destination rectangle (dx, dy, dw, dh) on the canvas
            double dx = this.x;
            double dy = this.y;
            double dw = this.width; // Use the enemy's defined width/height for drawing
            double dh = this.height;

            gc.drawImage(spriteInfo.spriteSheet, sx, sy, sw, sh, dx, dy, dw, dh);
        } else {
            // Fallback to a simple shape if no image/sprite info
            gc.setFill(Color.RED);
            gc.fillOval(x, y, width, height);
            // Draw frame index for debugging if needed
            // gc.setFill(Color.WHITE);
            // gc.fillText(String.valueOf(currentFrameIndex), x + width / 2, y + height /
            // 2);
        }

        // Draw health bar
        renderHealthBar(gc);

        // Render status icons very close to the enemy
        double iconX = this.x + this.width - 48; // Position much closer, more overlap
        double iconY = this.y - 2; // Position almost touching enemy
        double iconSize = 16; // Back to original size
        int iconOffset = 0;

        if (isSlowed && snowflakeIcon != null) {
            gc.drawImage(snowflakeIcon, iconX + iconOffset, iconY, iconSize, iconSize);
            iconOffset += iconSize + 2; // Add padding for next icon
        }

        if (isKnightSpeedBoosted && thunderIcon != null) { // Use corrected thunderIcon
            gc.drawImage(thunderIcon, iconX + iconOffset, iconY, iconSize, iconSize);
            // iconOffset += iconSize + 2; // If more icons could follow
        }
    }

    /**
     * Load the enemy image from file.
     */
    protected void loadImage() {
        try {
            // First check if we can get the image from the static cache based on type
            if (this.type != null) {
                SpriteSheetInfo cachedInfo = ENEMY_SPRITE_INFO.get(this.type);
                if (cachedInfo != null) {
                    this.spriteInfo = cachedInfo;
                    return;
                }
            }

            // Then try to parse the imageFile to see if it's an absolute path or a resource
            // path
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
                    Image image = new Image(getClass().getResourceAsStream(resourcePath));
                    if (image != null && !image.isError()) {
                        System.out.println(
                                "Loaded image for " + getClass().getSimpleName() + " from classpath: " + resourcePath);
                        this.spriteInfo = new SpriteSheetInfo(image, 1); // Assuming single frame
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Could not load image from classpath: " + resourcePath + " - " + e.getMessage());
                }

                // Fallback to file system only if absolutely necessary
                try {
                    File file = new File(imageFile);
                    if (file.exists()) {
                        Image image = new Image(file.toURI().toString());
                        System.out
                                .println("Loaded image for " + getClass().getSimpleName() + " from file: " + imageFile);
                        this.spriteInfo = new SpriteSheetInfo(image, 1); // Assuming single frame
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
        double barWidth = width * 0.2;
        double barHeight = 6;
        double barY = y + 12; // Was y - 12, moving it 4 pixels closer
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

        // Remove border
        // gc.setStroke(Color.BLACK);
        // gc.setLineWidth(2);
        // gc.strokeRect(barX, barY, barWidth, barHeight);
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
     * @param amount     amount of damage to apply
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
    
    /**
     * Set the path progress (for save/load system)
     */
    public void setPathProgress(double pathProgress) {
        this.pathProgress = Math.max(0.0, Math.min(1.0, pathProgress));
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    /**
     * Reinitialize after deserialization - use this to reload sprite info
     */
    public void reinitializeAfterLoad() {
        if (this.type != null) {
            this.spriteInfo = ENEMY_SPRITE_INFO.get(this.type);
            if (this.spriteInfo == null) {
                System.err.println("Failed to reinitialize SpriteSheetInfo for type: " + this.type);
            }
        }
        // Reset animation state
        this.currentFrameIndex = 0;
        this.animationTimer = 0;
    }

    public void applySlow(double factor, double duration) {
        this.isSlowed = true;
        this.slowFactor = factor;
        this.slowTimer = duration;
        System.out.println(this.getType() + " slowed by " + ((1 - factor) * 100) + "% for " + duration + "s");
    }

    public boolean isSlowed() {
        return isSlowed;
    }

    // For Knight synergy visual
    public void setKnightSpeedBoosted(boolean boosted) {
        this.isKnightSpeedBoosted = boosted;
    }

    public void teleportTo(double newX, double newY) {
        // newX and newY are the center of the start tile/point.
        // Enemy's x, y are top-left. Adjust accordingly.
        this.x = newX - this.width / 2.0;
        this.y = newY - this.height / 2.0;
        this.pathProgress = 0.0; // Reset path progress
        this.distanceTraveled = 0.0; // Reset distance traveled
        // Current health and status effects (like slow) are maintained as per
        // requirement.
        System.out.println(this.getType() + " teleported to (" + this.x + "," + this.y + "). Path progress reset.");
    }

    /**
     * Enum of enemy types.
     */
    public enum EnemyType {
        GOBLIN,
        KNIGHT
    }
}
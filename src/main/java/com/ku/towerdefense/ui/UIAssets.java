package com.ku.towerdefense.ui;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.ImageCursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Utility class for loading and managing UI assets.
 */
public class UIAssets {
    private static final Map<String, Image> imageCache = new HashMap<>();
    private static ImageCursor customCursor;

    /**
     * Initialize all UI assets.
     */
    public static void initialize() {
        loadImages();
        createCustomCursor();
    }

    /**
     * Load images from the Asset_pack/UI folder.
     */
    private static void loadImages() {
        // Get the base path from the class loader
        String basePath = "/Asset_pack/UI/";

        try {
            // Load all button images
            String[] buttonFiles = {
                    "Button_Blue.png",
                    "Button_Blue_Pressed.png",
                    "Button_Red.png",
                    "Button_Red_Pressed.png",
                    "Button_Hover.png",
                    "Button_Disable.png",
                    "Button_Blue_3Slides.png",
                    "Button_Red_3Slides.png",
                    "Button_Hover_3Slides.png",
                    "Button_Disable_3Slides.png",
            };

            for (String file : buttonFiles) {
                String key = file.replace(".png", "");
                loadImage(key, basePath + file);
            }

            // Load other UI images
            loadImage("GameUI", basePath + "Coin_Health_Wave.png");
            loadImage("Ribbon_Blue", basePath + "Ribbon_Blue_3Slides.png");
            loadImage("Ribbon_Red", basePath + "Ribbon_Red_3Slides.png");
            loadImage("Ribbon_Yellow", basePath + "Ribbon_Yellow_3Slides.png");
            loadImage("KUTowerButtons", basePath + "kutowerbuttons4.png");
            loadImage("WizardButton", basePath + "wiz.png");
            loadImage("01", basePath + "01.png");

            // Effect sprite sheets
            loadImage("ExplosionEffect", "/Asset_pack/Effects/Explosions.png");
            loadImage("FireEffect", "/Asset_pack/Effects/Fire.png");
            loadImage("GoldSpawnEffect", "/Asset_pack/Effects/G_Spawn.png");

            // Item Images
            loadImage("GoldBag", "/Asset_pack/Items/gold_bag.png");

            // Background Images
            loadImage("WoodBackground", "/Asset_pack/Background/wood.jpg");

            // Tower specific effects/icons
            // loadImage("ThunderEffect", "/Asset_pack/Towers/thunder_icon.png"); // Removed

            System.out.println("UI assets loaded successfully - " + imageCache.size() + " images");
        } catch (Exception e) {
            System.err.println("Error loading UI assets: " + e.getMessage());
        }
    }

    /**
     * Load a single image into the cache.
     * 
     * @param name image name/key
     * @param path file path
     */
    private static void loadImage(String name, String path) {
        try {
            Image image = new Image(UIAssets.class.getResourceAsStream(path));
            if (image != null) {
                imageCache.put(name, image);
            } else {
                System.err.println("Failed to load image: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error loading image " + path + ": " + e.getMessage());
        }
    }

    /**
     * Create a custom cursor.
     */
    private static void createCustomCursor() {
        try {
            Image cursorImage = imageCache.get("01");
            if (cursorImage != null) {
                // Hotspot at center of the image
                customCursor = new ImageCursor(cursorImage, cursorImage.getWidth() / 2, cursorImage.getHeight() / 2);
                System.out.println("Created custom cursor");
            } else {
                System.err.println("Cursor image not found");
            }
        } catch (Exception e) {
            System.err.println("Failed to create custom cursor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the custom cursor.
     * 
     * @return the custom cursor, or null if not available
     */
    public static ImageCursor getCustomCursor() {
        return customCursor;
    }

    /**
     * Get an image from the cache.
     * 
     * @param name image name/key
     * @return the image, or null if not found
     */
    public static Image getImage(String name) {
        return imageCache.get(name);
    }

    /**
     * Extracts a specific frame from a cached sprite sheet.
     *
     * @param sheetName   The key of the loaded sprite sheet in the cache.
     * @param frameIndex  The 0-based index of the frame to extract.
     * @param frameWidth  The width of a single frame in the sprite sheet.
     * @param frameHeight The height of a single frame in the sprite sheet.
     * @return An Image object of the specified frame, or null if an error occurs.
     */
    public static Image getSpriteFrame(String sheetName, int frameIndex, int frameWidth, int frameHeight) {
        Image spriteSheet = imageCache.get(sheetName);
        if (spriteSheet == null) {
            System.err.println("Sprite sheet not found in cache: " + sheetName);
            return null;
        }

        int sheetWidth = (int) spriteSheet.getWidth();
        // int sheetHeight = (int) spriteSheet.getHeight(); // Assuming all frames are
        // in one row for now

        int framesPerRow = sheetWidth / frameWidth;
        if (frameIndex < 0 || frameIndex >= framesPerRow) { // Basic check, assumes single row of frames
            System.err.println("Frame index " + frameIndex + " is out of bounds for sheet " + sheetName + " with "
                    + framesPerRow + " frames.");
            return null;
        }

        try {
            javafx.scene.image.PixelReader reader = spriteSheet.getPixelReader();
            if (reader == null) {
                System.err.println("PixelReader not available for sprite sheet: " + sheetName);
                return null;
            }
            // Corrected: x coordinate of the frame
            int x = frameIndex * frameWidth;
            int y = 0; // Assuming frames are in a single horizontal row

            javafx.scene.image.WritableImage frameImage = new javafx.scene.image.WritableImage(reader, x, y, frameWidth,
                    frameHeight);
            return frameImage;
        } catch (Exception e) {
            System.err.println(
                    "Error extracting frame " + frameIndex + " from sheet " + sheetName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Apply a styled button appearance.
     * 
     * @param button the button to style
     * @param type   "blue" or "red"
     */
    public static void styleButton(Button button, String type) {
        styleButton(button, type, false); // Calls the new method with isThreeSlides = false
    }

    /**
     * Apply a styled button appearance, supporting standard and 3-slides versions.
     * 
     * @param button        the button to style
     * @param type          "blue" or "red"
     * @param isThreeSlides true to use 3-slides assets, false for standard assets
     */
    public static void styleButton(Button button, String type, boolean isThreeSlides) {
        try {
            String capitalizedType = type.substring(0, 1).toUpperCase() + type.substring(1);
            final Image normalImageFinal;
            Image pressedImageInitial = null;
            final Image hoverImageFinal;
            String baseKey = "Button_" + capitalizedType;

            if (isThreeSlides) {
                normalImageFinal = imageCache.get(baseKey + "_3Slides");
                hoverImageFinal = imageCache.get("Button_Hover_3Slides");
                pressedImageInitial = imageCache.get(baseKey + "_Pressed_3Slides");
                if (pressedImageInitial == null) {
                    pressedImageInitial = hoverImageFinal;
                }
                if (pressedImageInitial == null) {
                    pressedImageInitial = normalImageFinal;
                }
            } else {
                normalImageFinal = imageCache.get(baseKey);
                pressedImageInitial = imageCache.get(baseKey + "_Pressed");
                hoverImageFinal = imageCache.get("Button_Hover");
            }

            final Image finalNormalImage = normalImageFinal;
            final Image finalHoverImage = hoverImageFinal;
            final Image finalPressedImage = (pressedImageInitial != null) ? pressedImageInitial : finalNormalImage;

            if (finalNormalImage != null && finalHoverImage != null) {
                ImageView iv = new ImageView(finalNormalImage);
                iv.setFitWidth(finalNormalImage.getWidth());
                iv.setFitHeight(finalNormalImage.getHeight());
                button.setGraphic(iv);
                button.setContentDisplay(ContentDisplay.CENTER);
                button.setStyle("-fx-background-color: transparent; -fx-background-image: none; -fx-padding: 0;");

                button.setOnMousePressed(e -> ((ImageView) button.getGraphic()).setImage(finalPressedImage));
                button.setOnMouseReleased(e -> ((ImageView) button.getGraphic()).setImage(finalNormalImage));
                button.setOnMouseEntered(e -> {
                    button.setStyle(
                            "-fx-cursor: hand; -fx-background-color: transparent; -fx-background-image: none; -fx-padding: 0;");
                    if (!button.isPressed()) {
                        ((ImageView) button.getGraphic()).setImage(finalHoverImage);
                    }
                });
                button.setOnMouseExited(e -> {
                    button.setStyle("-fx-background-color: transparent; -fx-background-image: none; -fx-padding: 0;");
                    if (!button.isPressed()) {
                        ((ImageView) button.getGraphic()).setImage(finalNormalImage);
                    }
                });
            } else {
                System.err.println("Missing button images for style: " + type + (isThreeSlides ? " (3Slides)" : ""));
                button.setStyle("-fx-base: " + (type.equals("blue") ? "#3c7fb1" : "#d14836") + ";");
            }
        } catch (Exception e) {
            System.err.println("Failed to style button: " + e.getMessage());
            e.printStackTrace();
            button.setStyle("-fx-base: " + (type.equals("blue") ? "#3c7fb1" : "#d14836") + ";");
        }
    }

    /**
     * Constants for KUTowerButtons sprite sheet (kutowerbuttons4.png)
     */
    public static final double KUTOWERBUTTONS_ICON_WIDTH = 64.0;
    public static final double KUTOWERBUTTONS_ICON_HEIGHT = 64.0;

    // Icon Definitions for KUTowerButtons sprite sheet (kutowerbuttons4.png)
    // Row 0
    public static final String LABEL_EDIT = "Edit";
    public static final int ICON_EDIT_COL = 0;
    public static final int ICON_EDIT_ROW = 0;

    public static final String LABEL_DELETE = "Delete";
    public static final int ICON_DELETE_COL = 1;
    public static final int ICON_DELETE_ROW = 0;

    public static final String LABEL_SAVE = "Save";
    public static final int ICON_SAVE_COL = 2;
    public static final int ICON_SAVE_ROW = 0;

    public static final String LABEL_CLOSE = "Close";
    public static final int ICON_CLOSE_COL = 3;
    public static final int ICON_CLOSE_ROW = 0;

    // Row 1
    public static final String LABEL_PLAY = "Play";
    public static final int ICON_PLAY_COL = 0;
    public static final int ICON_PLAY_ROW = 1;

    public static final String LABEL_FAST_FORWARD = "Fast Forward";
    public static final int ICON_FAST_FORWARD_COL = 1;
    public static final int ICON_FAST_FORWARD_ROW = 1;

    public static final String LABEL_PAUSE = "Pause";
    public static final int ICON_PAUSE_COL = 2;
    public static final int ICON_PAUSE_ROW = 1;

    public static final String LABEL_SETTINGS = "Settings";
    public static final int ICON_SETTINGS_COL = 3;
    public static final int ICON_SETTINGS_ROW = 1;

    // Row 2
    public static final String LABEL_DESELECT = "Deselect"; // Target with an X
    public static final int ICON_DESELECT_COL = 0;
    public static final int ICON_DESELECT_ROW = 2;

    public static final String LABEL_UPGRADE = "Upgrade"; // Star icon
    public static final int ICON_UPGRADE_COL = 1;
    public static final int ICON_UPGRADE_ROW = 2;

    public static final String LABEL_FIRE = "Fire"; // Fire icon
    public static final int ICON_FIRE_COL = 2;
    public static final int ICON_FIRE_ROW = 2;

    public static final String LABEL_BOMB = "Bomb"; // Bomb icon
    public static final int ICON_BOMB_COL = 3;
    public static final int ICON_BOMB_ROW = 2;

    // Row 3
    public static final String LABEL_FILL = "Fill"; // Paint bucket icon
    public static final int ICON_FILL_COL = 0;
    public static final int ICON_FILL_ROW = 3;

    public static final String LABEL_ERASE = "Erase"; // Eraser icon
    public static final int ICON_ERASER_COL = 1;
    public static final int ICON_ERASER_ROW = 3;

    public static final String LABEL_BUILD = "Build"; // Square block icon
    public static final int ICON_BUILD_COL = 2;
    public static final int ICON_BUILD_ROW = 3;

    /**
     * Creates a button with an icon from the "KUTowerButtons" sprite sheet.
     *
     * @param tooltipText     Text for the button's tooltip.
     * @param iconCol         Column of the icon in the sprite sheet (0-indexed).
     * @param iconRow         Row of the icon in the sprite sheet (0-indexed).
     * @param iconDisplaySize The desired display size (width and height) for the
     *                        icon on the button.
     * @return A new Button configured with the specified icon and tooltip.
     */
    public static Button createIconButton(String tooltipText, int iconCol, int iconRow, double iconDisplaySize) {
        Button button = new Button();
        Image spriteSheet = getImage("KUTowerButtons");

        if (spriteSheet != null) {
            ImageView iconView = new ImageView(spriteSheet);
            iconView.setViewport(new javafx.geometry.Rectangle2D(
                    iconCol * KUTOWERBUTTONS_ICON_WIDTH,
                    iconRow * KUTOWERBUTTONS_ICON_HEIGHT,
                    KUTOWERBUTTONS_ICON_WIDTH,
                    KUTOWERBUTTONS_ICON_HEIGHT));
            iconView.setFitWidth(iconDisplaySize);
            iconView.setFitHeight(iconDisplaySize);
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true); // Or false if pixel art style is preferred

            button.setGraphic(iconView);
            button.getStyleClass().add("icon-button"); // For CSS styling
            // Basic styling for icon buttons (can be overridden/enhanced in CSS)
            // button.setStyle("-fx-background-color: transparent; -fx-padding: 3px;"); //
            // REMOVED
            // button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0;
            // -fx-padding: 3px; -fx-cursor: hand;")); // REMOVED
            // button.setOnMouseExited(e -> button.setStyle("-fx-background-color:
            // transparent; -fx-padding: 3px;")); // REMOVED

            // CSS (.icon-button) should handle -fx-cursor: hand;
            // Revert to custom cursor on exit if it was changed by something else (though
            // less likely now)
            button.setOnMouseExited(e -> {
                if (button.getScene() != null && button.getScene().getCursor() != UIAssets.getCustomCursor()) {
                    button.getScene().setCursor(UIAssets.getCustomCursor());
                }
            });

        } else {
            // Fallback if the sprite sheet isn't loaded
            button.setText("?"); // Placeholder for missing icon
            System.err.println("KUTowerButtons spritesheet not found for icon button.");
        }

        if (tooltipText != null && !tooltipText.isEmpty()) {
            button.setTooltip(new javafx.scene.control.Tooltip(tooltipText));
        }

        return button;
    }
    
    /**
     * Creates a button with a standalone image (not from sprite sheet).
     *
     * @param tooltipText     Text for the button's tooltip.
     * @param imageName       Name of the image in the cache (e.g., "WizardButton").
     * @param iconDisplaySize The desired display size (width and height) for the icon on the button.
     * @return A new Button configured with the specified image and tooltip.
     */
    public static Button createStandaloneIconButton(String tooltipText, String imageName, double iconDisplaySize) {
        Button button = new Button();
        Image buttonImage = getImage(imageName);

        if (buttonImage != null) {
            ImageView iconView = new ImageView(buttonImage);
            iconView.setFitWidth(iconDisplaySize);
            iconView.setFitHeight(iconDisplaySize);
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true);

            button.setGraphic(iconView);
            button.getStyleClass().add("icon-button"); // For CSS styling - this handles transparent background
            
            // Remove the inline styling - let CSS handle it
            // The .icon-button CSS class already sets:
            // -fx-background-color: transparent;
            // -fx-padding: 0px;
            // -fx-border-color: transparent;
            // -fx-border-width: 0;
            // -fx-background-insets: 0;
            // -fx-background-radius: 0;
            
            // Revert to custom cursor on exit if it was changed by something else
            button.setOnMouseExited(e -> {
                if (button.getScene() != null && button.getScene().getCursor() != UIAssets.getCustomCursor()) {
                    button.getScene().setCursor(UIAssets.getCustomCursor());
                }
            });

        } else {
            // Fallback if the image isn't loaded
            button.setText("?"); // Placeholder for missing icon
            System.err.println("Standalone image '" + imageName + "' not found for icon button.");
        }

        if (tooltipText != null && !tooltipText.isEmpty()) {
            button.setTooltip(new javafx.scene.control.Tooltip(tooltipText));
        }

        return button;
    }

    /**
     * Enforces the custom cursor on a scene and all its children.
     * This prevents UI elements from overriding the custom cursor.
     * 
     * @param scene the scene to enforce cursor on
     */
    public static void enforceCustomCursor(javafx.scene.Scene scene) {
        ImageCursor customCursor = getCustomCursor();
        if (customCursor != null && scene != null) {
            // Set cursor on scene
            scene.setCursor(customCursor);
            
            // Force cursor on root and all children
            if (scene.getRoot() != null) {
                enforceCustomCursorOnNode(scene.getRoot());
            }
            
            // Add listeners to maintain cursor when scene focus changes
            scene.focusOwnerProperty().addListener((obs, oldNode, newNode) -> {
                javafx.application.Platform.runLater(() -> {
                    if (scene.getCursor() != customCursor) {
                        scene.setCursor(customCursor);
                    }
                });
            });
        }
    }

    /**
     * Recursively enforces custom cursor on a node and all its children.
     */
    private static void enforceCustomCursorOnNode(javafx.scene.Node node) {
        ImageCursor customCursor = getCustomCursor();
        if (customCursor == null) return;
        
        // Set cursor on this node
        node.setCursor(customCursor);
        
        // Special handling for different node types
        if (node instanceof javafx.scene.control.Button) {
            Button button = (Button) node;
            
            // Override button hover behavior to maintain custom cursor
            button.setOnMouseEntered(e -> {
                button.setCursor(customCursor);
                e.consume();
            });
            
            button.setOnMouseExited(e -> {
                button.setCursor(customCursor);
                e.consume();
            });
            
            // Maintain cursor during press
            button.setOnMousePressed(e -> {
                button.setCursor(customCursor);
            });
            
            button.setOnMouseReleased(e -> {
                button.setCursor(customCursor);
            });
        }
        
        // Handle Canvas separately to maintain custom cursor during interactions
        if (node instanceof javafx.scene.canvas.Canvas) {
            javafx.scene.canvas.Canvas canvas = (javafx.scene.canvas.Canvas) node;
            canvas.setCursor(customCursor);
            
            // Ensure cursor stays custom during all mouse events
            canvas.setOnMouseEntered(e -> canvas.setCursor(customCursor));
            canvas.setOnMouseMoved(e -> {
                if (canvas.getCursor() != customCursor) {
                    canvas.setCursor(customCursor);
                }
            });
        }
        
        // Recursively apply to children if it's a parent node
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                enforceCustomCursorOnNode(child);
            }
        }
    }

    /**
     * Sets up periodic cursor enforcement to catch any cursor overrides.
     * Call this once per scene to maintain custom cursor.
     */
    public static void startCursorEnforcement(javafx.scene.Scene scene) {
        ImageCursor customCursor = getCustomCursor();
        if (customCursor == null || scene == null) return;
        
        // Create a timeline that periodically checks and restores the custom cursor
        javafx.animation.Timeline cursorEnforcer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(100), e -> {
                if (scene.getCursor() != customCursor) {
                    scene.setCursor(customCursor);
                }
                
                // Also check focused node
                if (scene.getFocusOwner() != null && scene.getFocusOwner().getCursor() != customCursor) {
                    scene.getFocusOwner().setCursor(customCursor);
                }
            })
        );
        cursorEnforcer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        cursorEnforcer.play();
    }
}
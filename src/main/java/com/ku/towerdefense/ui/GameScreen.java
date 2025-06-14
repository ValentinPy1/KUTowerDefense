package com.ku.towerdefense.ui;

import com.ku.towerdefense.controller.GameController;
import com.ku.towerdefense.model.entity.ArcherTower;
import com.ku.towerdefense.model.entity.ArtilleryTower;
import com.ku.towerdefense.model.entity.MageTower;
import com.ku.towerdefense.model.entity.Tower;
import com.ku.towerdefense.model.entity.DroppedGold;
import com.ku.towerdefense.powerup.PowerUpType;
import com.ku.towerdefense.model.map.Tile;
import com.ku.towerdefense.model.map.TileType;
import com.ku.towerdefense.ui.MainMenuScreen;
import com.ku.towerdefense.service.GameSaveService;
import com.ku.towerdefense.Main;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Affine;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.Image;
import javafx.scene.ImageCursor;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.paint.ImagePattern;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.geometry.Point2D;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.NumberBinding;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main game screen where the tower defense gameplay takes place.
 */

public class GameScreen extends BorderPane {
    private final Stage primaryStage;
    private final GameController gameController;
    private Canvas gameCanvas;
    private GameRenderTimer renderTimer;
    private AnimationTimer topBarUpdateTimer;
    private Tower selectedTower;
    private boolean isPaused = false;
    private final StackPane canvasRootPane = new StackPane();
    private final Pane uiOverlayPane = new Pane();
    private final Affine worldTransform = new Affine();
    private Node activePopup = null;
    private static final double POPUP_ICON_SIZE = 64.0;
    // private static final double POPUP_SPACING = 5.0; // Not currently used, can
    // be removed or kept for future

    // Game world and UI constants
    private static final double TILE_SIZE = 64.0;
    private static final double HALF_TILE_SIZE = TILE_SIZE / 2.0;
    private static final double BUILD_POPUP_RADIUS = 60.0;
    private static final double UPGRADE_SELL_POPUP_RADIUS = 60.0;

    // Zoom and Pan state
    private double currentZoomLevel = 1.0;
    private double minZoom = 0.2; // Adjusted for potentially large maps, was 0.25
    private double maxZoom = 4.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private boolean isPanning = false;
    private double lastMouseXForPan;
    private double lastMouseYForPan;
    private boolean dragOccurred = false; // New flag
    private double currentEffectiveScale = 1.0; // Added for drag handler to use renderer's scale

    // Tower hover tracking
    private Tower hoveredTower = null;

    // Memory tracker
    private MemoryTracker memoryTracker;
    private boolean memoryTrackerVisible = false;

    private Label goldLabel;
    private Label livesLabel;
    private Label waveLabel;
    private ImageView goldIcon;
    private ImageView livesIcon;
    private ImageView waveIcon;

    // private Button pauseResumeButton; // REMOVED
    // private Button gameSpeedButton; // REMOVED
    private Button pauseButton;
    private Button playButton;
    private Button fastForwardButton;
    private Button menuButton;
    private Button freezeButton;
    private static final String TIME_CONTROL_SELECTED_STYLE_CLASS = "time-control-selected";

    // Property to track the visual width of the map on screen
    private ReadOnlyDoubleWrapper visualMapWidthProperty = new ReadOnlyDoubleWrapper();

    // Define TowerBuildOption as a private static nested class
    private static class TowerBuildOption {
        String name;
        int cost;
        int iconCol, iconRow;
        java.util.function.Supplier<Tower> constructor;

        TowerBuildOption(String name, int cost, int iconCol, int iconRow,
                java.util.function.Supplier<Tower> constructor) {
            this.name = name;
            this.cost = cost;
            this.iconCol = iconCol;
            this.iconRow = iconRow;
            this.constructor = constructor;
        }
    }

    // Custom AnimationTimer class with additional methods
    private class GameRenderTimer extends AnimationTimer {
        private long lastTime = -1;
        private String statusMessage = "Ready to play!";
        private long statusTimestamp = 0;
        private double mouseX = 0;
        private double mouseY = 0;
        private boolean mouseInCanvas = false;

        @Override
        public void handle(long now) {
            GraphicsContext gc = gameCanvas.getGraphicsContext2D();
            double canvasWidth = gameCanvas.getWidth();
            double canvasHeight = gameCanvas.getHeight();

            if (canvasWidth <= 0 || canvasHeight <= 0) {
                return; // Canvas not ready yet
            }

            gc.clearRect(0, 0, canvasWidth, canvasHeight);

            // --- Calculate world dimensions ---
            double worldWidth = gameController.getGameMap().getWidth() * TILE_SIZE;
            double worldHeight = gameController.getGameMap().getHeight() * TILE_SIZE;

            // --- Calculate zoom and pan ---
            double containerAspectRatio = canvasWidth / canvasHeight;
            double worldAspectRatio = worldWidth / worldHeight;

            double baseScale;
            if (containerAspectRatio > worldAspectRatio) {
                // Canvas is wider than world aspect ratio -> fit to height
                baseScale = canvasHeight / worldHeight;
            } else {
                // Canvas is taller than world aspect ratio -> fit to width
                baseScale = canvasWidth / worldWidth;
            }

            // Calculate the current effective scale
            currentEffectiveScale = baseScale * currentZoomLevel;

            // Calculate the center of the canvas
            double centerX = canvasWidth / 2.0;
            double centerY = canvasHeight / 2.0;

            // Calculate the offset to center the world view
            double offsetX = centerX - panX * currentEffectiveScale;
            double offsetY = centerY - panY * currentEffectiveScale;

            // Update the world transform
            worldTransform.setToIdentity();
            worldTransform.appendTranslation(offsetX, offsetY);
            worldTransform.appendScale(currentEffectiveScale, currentEffectiveScale);
            
            // CRITICAL: Update visual map width for UI positioning
            double visualMapWidth = worldWidth * currentEffectiveScale;
            visualMapWidthProperty.set(visualMapWidth);

            // Save the current transform
            gc.save();

            // Fill background
            if (UIAssets.getImage("WoodBackground") != null) {
                ImagePattern pattern = new ImagePattern(UIAssets.getImage("WoodBackground"));
                gc.setFill(pattern);
            } else {
                gc.setFill(javafx.scene.paint.Color.DARKGREEN);
            }
            gc.fillRect(0, 0, canvasWidth, canvasHeight);

            // Apply the world transformation (scale and center)
            gc.setTransform(worldTransform);

            // ---- Draw border around the map ---- START
            gc.setStroke(javafx.scene.paint.Color.web("#3B270E")); // Dark brown border
            gc.setLineWidth(12.0); // Border thickness in world units (will scale with zoom)
            gc.strokeRect(0, 0, worldWidth, worldHeight);
            // ---- Draw border around the map ---- END

            // Render game elements using original world coordinates
            // The transform handles scaling them correctly onto the canvas
            gameController.render(gc);

            // Render tower preview (using transformed mouse coords - see setOnMouseClicked)
            if (selectedTower != null && mouseInCanvas) {
                // Transform mouse coordinates from canvas space to world space
                javafx.geometry.Point2D worldMouse = transformMouseCoords(mouseX, mouseY);

                if (worldMouse != null) {
                    // Convert world coordinates to grid coordinates
                    int tileX = (int) (worldMouse.getX() / TILE_SIZE);
                    int tileY = (int) (worldMouse.getY() / TILE_SIZE);

                    // Get center of the tile in world coordinates
                    double previewCenterX = tileX * TILE_SIZE + HALF_TILE_SIZE;
                    double previewCenterY = tileY * TILE_SIZE + HALF_TILE_SIZE;

                    // Check if we can place here (uses world coordinates)
                    boolean canPlace = gameController.getGameMap().canPlaceTower(previewCenterX, previewCenterY,
                            gameController.getTowers());

                    // Draw preview circle in world coordinates
                    gc.setGlobalAlpha(0.5);
                    gc.setFill(canPlace ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.RED);
                    // Use world coordinates for drawing - transform handles canvas placement
                    gc.fillOval(previewCenterX - HALF_TILE_SIZE, previewCenterY - HALF_TILE_SIZE, TILE_SIZE, TILE_SIZE);

                    // Draw range preview in world coordinates
                    gc.setStroke(javafx.scene.paint.Color.WHITE);
                    gc.setGlobalAlpha(0.2);
                    double range = 0;
                    if (selectedTower instanceof ArcherTower)
                        range = ((ArcherTower) selectedTower).getRange();
                    else if (selectedTower instanceof ArtilleryTower)
                        range = ((ArtilleryTower) selectedTower).getRange();
                    else if (selectedTower instanceof MageTower)
                        range = ((MageTower) selectedTower).getRange();

                    if (range > 0) {
                        gc.strokeOval(previewCenterX - range, previewCenterY - range, range * 2, range * 2);
                    }
                    gc.setGlobalAlpha(1.0);
                }
            }

            // Render hovered tower range
            if (hoveredTower != null && mouseInCanvas) {
                gc.setGlobalAlpha(0.3);
                gc.setStroke(javafx.scene.paint.Color.CYAN);
                gc.setLineWidth(2.0);
                
                double hoverCenterX = hoveredTower.getX() + hoveredTower.getWidth() / 2;
                double hoverCenterY = hoveredTower.getY() + hoveredTower.getHeight() / 2;
                double range = hoveredTower.getRange();
                
                gc.strokeOval(hoverCenterX - range, hoverCenterY - range, range * 2, range * 2);
                gc.setGlobalAlpha(1.0);
            }

            gc.restore(); // Restore default transform for drawing UI overlays

            // --- Update game logic ---
            if (!isPaused) {
                if (lastTime < 0) {
                    lastTime = now;
                }
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                gameController.update(deltaTime);
            } else {
                lastTime = -1; // Reset delta time calculation when paused
            }

            // --- UI Overlays (drawn directly on canvas, not scaled) ---
            // Status message (bottom-left)
            long currentTime = System.currentTimeMillis();
            if (currentTime - statusTimestamp < 3000) {
                double alpha = 1.0 - (currentTime - statusTimestamp) / 3000.0;
                gc.setGlobalAlpha(alpha);
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.setStroke(javafx.scene.paint.Color.BLACK);
                gc.setLineWidth(1); // Reduced thickness slightly
                gc.strokeText(statusMessage, 10, canvasHeight - 10); // Anchor bottom-left
                gc.fillText(statusMessage, 10, canvasHeight - 10);
                gc.setGlobalAlpha(1.0);
            }

            // Grace period message (center of screen) - Enhanced styling
            if (gameController.isInGracePeriod()) {
                // Create a stylish background box
                double boxWidth = 600;
                double boxHeight = 150;
                double boxX = (canvasWidth - boxWidth) / 2;
                double boxY = (canvasHeight - boxHeight) / 2 - 100; // Slightly above center
                
                // Semi-transparent dark background with border
                gc.setGlobalAlpha(0.85);
                gc.setFill(javafx.scene.paint.Color.web("#1a1a2e"));
                gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
                
                // Border with gradient effect
                gc.setStroke(javafx.scene.paint.Color.web("#16537e"));
                gc.setLineWidth(4);
                gc.strokeRoundRect(boxX, boxY, boxWidth, boxHeight, 20, 20);
                
                // Inner border for glow effect
                gc.setStroke(javafx.scene.paint.Color.web("#4ca3dd"));
                gc.setLineWidth(2);
                gc.strokeRoundRect(boxX + 2, boxY + 2, boxWidth - 4, boxHeight - 4, 18, 18);
                
                gc.setGlobalAlpha(1.0);
                
                // Main title
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 36));
                gc.setFill(javafx.scene.paint.Color.web("#ffd700"));
                gc.setStroke(javafx.scene.paint.Color.BLACK);
                gc.setLineWidth(2);
                String mainMessage = "⚡ GRACE PERIOD ⚡";
                double mainTextWidth = 350; // Approximate
                double mainTextX = (canvasWidth - mainTextWidth) / 2;
                double mainTextY = boxY + 50;
                gc.strokeText(mainMessage, mainTextX, mainTextY);
                gc.fillText(mainMessage, mainTextX, mainTextY);
                
                // Subtitle
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 24));
                gc.setFill(javafx.scene.paint.Color.web("#87ceeb"));
                gc.setStroke(javafx.scene.paint.Color.BLACK);
                gc.setLineWidth(1);
                String subMessage = "Build Your Towers Now!";
                double subTextWidth = 280; // Approximate
                double subTextX = (canvasWidth - subTextWidth) / 2;
                double subTextY = boxY + 90;
                gc.strokeText(subMessage, subTextX, subTextY);
                gc.fillText(subMessage, subTextX, subTextY);
                
                // Timer countdown (optional enhancement)
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 18));
                gc.setFill(javafx.scene.paint.Color.web("#ff6b6b"));
                String timerMessage = "4 seconds to prepare...";
                double timerTextWidth = 200; // Approximate
                double timerTextX = (canvasWidth - timerTextWidth) / 2;
                double timerTextY = boxY + 120;
                gc.fillText(timerMessage, timerTextX, timerTextY);
            }

            // Asset loading issue message
            if (!gameController.getTowers().isEmpty() && gameController.getTowers().get(0).getImage() == null) {
                gc.setFill(javafx.scene.paint.Color.RED);
                gc.fillText("Asset loading issue detected!", 10, 80);
                gc.fillText("Using fallback rendering instead", 10, 100);
            }
        }

        // Method to set mouse position
        public void setMousePosition(double x, double y, boolean inCanvas) {
            this.mouseX = x;
            this.mouseY = y;
            this.mouseInCanvas = inCanvas;
        }

        // Method to set status message
        public void setStatusMessage(String message) {
            this.statusMessage = message;
            this.statusTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * Constructor for the game screen.
     *
     * @param primaryStage   the primary stage of the application
     * @param gameController the game controller
     */
    public GameScreen(Stage primaryStage, GameController gameController) {
        this.primaryStage = primaryStage;
        this.gameController = gameController;

        // Initialize panX and panY to the center of the map for initial full view
        if (gameController != null && gameController.getGameMap() != null) {
            double worldWidth = gameController.getGameMap().getWidth() * TILE_SIZE;
            double worldHeight = gameController.getGameMap().getHeight() * TILE_SIZE;
            this.panX = worldWidth / 2.0;
            this.panY = worldHeight / 2.0;
        } else {
            // Fallback if map is not ready, though it should be
            this.panX = 0;
            this.panY = 0;
        }

        initializeUI();
        startRenderLoop();
    }

    /**
     * Initialize the user interface components.
     */
    private void initializeUI() {
        getStyleClass().add("game-screen");

        gameCanvas = new Canvas(); // Canvas takes available space

        uiOverlayPane.setPickOnBounds(false);
        uiOverlayPane.prefWidthProperty().bind(gameCanvas.widthProperty());
        uiOverlayPane.prefHeightProperty().bind(gameCanvas.heightProperty());

        uiOverlayPane.setOnMouseClicked(event -> {
            if (activePopup != null && !event.isConsumed()) {
                boolean clickOutsidePopup = true;
                if (activePopup.getBoundsInParent().contains(event.getX(), event.getY())) {
                    clickOutsidePopup = false;
                }
                if (event.getTarget() == uiOverlayPane && clickOutsidePopup) {
                    clearActivePopup();
                }
            }
        });

        canvasRootPane.getChildren().addAll(gameCanvas, uiOverlayPane);

        setCenter(canvasRootPane);

        gameCanvas.widthProperty().bind(canvasRootPane.widthProperty());
        gameCanvas.heightProperty().bind(canvasRootPane.heightProperty());

        // ---- Create Game Info Display (Top-Left) ----
        VBox gameInfoPane = new VBox(12); // Spacing between elements, was 8
        gameInfoPane.setPadding(new Insets(22)); // Padding around the pane, was 15
        gameInfoPane.setAlignment(Pos.TOP_LEFT);
        gameInfoPane.setPickOnBounds(false);

        Image hudIconsSheet = UIAssets.getImage("GameUI");
        double iconSheetEntryWidth = 79;
        double iconSheetEntryHeight = 218.0 / 3.0;
        double displayIconSize = 54.0; // Was 36.0, now 1.5x bigger

        // Gold Display
        goldIcon = new ImageView(hudIconsSheet);
        goldIcon.setViewport(new javafx.geometry.Rectangle2D(0, 0, iconSheetEntryWidth, iconSheetEntryHeight));
        goldIcon.setFitWidth(displayIconSize);
        goldIcon.setFitHeight(displayIconSize);
        goldIcon.setPreserveRatio(true);
        goldIcon.setSmooth(true);
        goldLabel = new Label();
        goldLabel.getStyleClass().add("game-info-text");
        goldLabel.setStyle("-fx-font-size: 20px;"); // Increased font size
        HBox goldDisplay = new HBox(12, goldIcon, goldLabel); // Adjusted spacing, was 8
        goldDisplay.setAlignment(Pos.CENTER_LEFT);

        // Lives Display
        livesIcon = new ImageView(hudIconsSheet);
        livesIcon.setViewport(
                new javafx.geometry.Rectangle2D(0, iconSheetEntryHeight, iconSheetEntryWidth, iconSheetEntryHeight));
        livesIcon.setFitWidth(displayIconSize);
        livesIcon.setFitHeight(displayIconSize);
        livesIcon.setPreserveRatio(true);
        livesIcon.setSmooth(true);
        livesLabel = new Label();
        livesLabel.getStyleClass().add("game-info-text");
        livesLabel.setStyle("-fx-font-size: 20px;"); // Increased font size
        HBox livesDisplay = new HBox(12, livesIcon, livesLabel); // Adjusted spacing, was 8
        livesDisplay.setAlignment(Pos.CENTER_LEFT);

        // Wave Display
        waveIcon = new ImageView(hudIconsSheet);
        waveIcon.setViewport(new javafx.geometry.Rectangle2D(0, iconSheetEntryHeight * 2, iconSheetEntryWidth,
                iconSheetEntryHeight));
        waveIcon.setFitWidth(displayIconSize);
        waveIcon.setFitHeight(displayIconSize);
        waveIcon.setPreserveRatio(true);
        waveIcon.setSmooth(true);
        waveLabel = new Label();
        waveLabel.getStyleClass().add("game-info-text");
        waveLabel.setStyle("-fx-font-size: 20px;"); // Increased font size
        HBox waveDisplay = new HBox(12, waveIcon, waveLabel); // Adjusted spacing, was 8
        waveDisplay.setAlignment(Pos.CENTER_LEFT);

        gameInfoPane.getChildren().addAll(goldDisplay, livesDisplay, waveDisplay);
        uiOverlayPane.getChildren().add(gameInfoPane);

        // Position gameInfoPane conditionally
        NumberBinding leftBandWidth = Bindings.when(visualMapWidthProperty().lessThan(uiOverlayPane.widthProperty()))
                .then((uiOverlayPane.widthProperty().subtract(visualMapWidthProperty())).divide(2))
                .otherwise(0.0);

        NumberBinding layoutXWhenLeftBandExists = leftBandWidth.divide(2)
                .subtract(gameInfoPane.widthProperty().divide(2));
        // More direct: ( ( (T-M)/2 ) - I_w) / 2 if T-M > 0, else 15
        // (
        // (uiOverlayPane.widthProperty().subtract(visualMapWidthProperty())).divide(2)
        // .subtract(gameInfoPane.widthProperty()) ).divide(2)
        // Let's try centering the middle of the pane in the middle of the left band.
        // Center of left band: ( (T-M)/2 ) / 2 = (T-M)/4
        // Left edge of pane = Center of left band - PaneWidth/2
        NumberBinding newLayoutXWhenLeftBandExists = (uiOverlayPane.widthProperty().subtract(visualMapWidthProperty()))
                .divide(4)
                .subtract(gameInfoPane.widthProperty().divide(2));

        // FIXED: Position HUD in left decorative area - simple and reliable approach
        NumberBinding leftDecorationWidth = uiOverlayPane.widthProperty()
                .subtract(visualMapWidthProperty())
                .divide(2); // Width of left decorative band
        
        NumberBinding hudPosition = Bindings.max(
            leftDecorationWidth.divide(2).subtract(gameInfoPane.widthProperty().divide(2)), // Center in left band
            15.0 // Minimum padding from edge
        );
        
        gameInfoPane.layoutXProperty().bind(hudPosition);
        gameInfoPane.setLayoutY(15.0); // Fixed top padding

        // ---- Create Control Buttons (Top-Right) ----
        VBox controlButtonsPane = new VBox(10); // Spacing between buttons
        controlButtonsPane.setPadding(new Insets(15));
        controlButtonsPane.setAlignment(Pos.TOP_CENTER); // Changed from TOP_RIGHT to TOP_CENTER
        controlButtonsPane.setPickOnBounds(false); // Allow clicks to pass through empty areas

        final double controlButtonIconSize = 108.0;

        pauseButton = UIAssets.createIconButton(UIAssets.LABEL_PAUSE, UIAssets.ICON_PAUSE_COL, UIAssets.ICON_PAUSE_ROW,
                controlButtonIconSize);
        pauseButton.setOnAction(e -> {
            isPaused = true;
            gameController.setPaused(true);
            updateTimeControlStates();
            e.consume();
        });

        playButton = UIAssets.createIconButton(UIAssets.LABEL_PLAY, UIAssets.ICON_PLAY_COL, UIAssets.ICON_PLAY_ROW,
                controlButtonIconSize);
        playButton.setOnAction(e -> {
            isPaused = false;
            gameController.setPaused(false);
            gameController.setSpeedAccelerated(false);
            updateTimeControlStates();
            e.consume();
        });

        fastForwardButton = UIAssets.createIconButton(UIAssets.LABEL_FAST_FORWARD, UIAssets.ICON_FAST_FORWARD_COL,
                UIAssets.ICON_FAST_FORWARD_ROW, controlButtonIconSize);
        fastForwardButton.setOnAction(e -> {
            isPaused = false;
            gameController.setPaused(false);
            gameController.setSpeedAccelerated(true);
            updateTimeControlStates();
            e.consume();
        });

        menuButton = UIAssets.createIconButton(UIAssets.LABEL_SETTINGS, UIAssets.ICON_SETTINGS_COL,
                UIAssets.ICON_SETTINGS_ROW, controlButtonIconSize);
        menuButton.setOnAction(e -> {
            showGameSettingsPopup();
            e.consume();
        });
        
        // Create freeze power-up button using wizard image with magical effects
        freezeButton = UIAssets.createStandaloneIconButton("Allmighty Wizard Hakan Hoca - Freeze All Enemies", "WizardButton", controlButtonIconSize);
        
        // Remove the icon-button class and add a unique wizard-button class
        freezeButton.getStyleClass().remove("icon-button");
        freezeButton.getStyleClass().add("wizard-button");
        
        // Ensure button size matches other control buttons exactly
        freezeButton.setPrefSize(controlButtonIconSize, controlButtonIconSize);
        freezeButton.setMinSize(controlButtonIconSize, controlButtonIconSize);
        freezeButton.setMaxSize(controlButtonIconSize, controlButtonIconSize);
        
        // Style to blend with woody background texture - create hybrid effect
        freezeButton.setStyle(
            "-fx-background-color: #8B4513;" + // Saddle brown to match wood texture
            "-fx-background-image: url('/Asset_pack/Background/wood.jpg');" +
            "-fx-background-repeat: repeat;" +
            "-fx-background-position: center;" +
            "-fx-background-size: cover;" +
            "-fx-border-color: #654321;" + // Darker brown border
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 2px;" + // Small padding to create wood frame effect
            "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.3), 3, 0, 1, 1);" // Subtle depth
        );
        
        // Add magical hover and click effects
        addMagicalEffectsToWizardButton(freezeButton);
        
        freezeButton.setOnAction(e -> {
            activateFreezeEffect();
            e.consume();
        });

        // Memory tracker toggle button
        Button memoryTrackerButton = UIAssets.createIconButton("Memory Tracker", UIAssets.ICON_BUILD_COL,
                UIAssets.ICON_BUILD_ROW, controlButtonIconSize);
        memoryTrackerButton.setOnAction(e -> {
            toggleMemoryTracker();
            e.consume();
        });

        // Initialize memory tracker
        memoryTracker = new MemoryTracker(gameController);
        memoryTracker.setVisible(false);

        // Remove HBox for timeControls, add buttons directly to VBox for vertical
        // layout
        controlButtonsPane.getChildren().addAll(pauseButton, playButton, fastForwardButton, menuButton, freezeButton, memoryTrackerButton);
        uiOverlayPane.getChildren().addAll(controlButtonsPane, memoryTracker);

        // Position controlButtonsPane at top-right, conditionally centered in right
        // band
        BooleanBinding mapIsNarrowerThanScreen = visualMapWidthProperty().lessThan(uiOverlayPane.widthProperty());

        NumberBinding layoutXWhenBandExists = uiOverlayPane.widthProperty().multiply(3)
                .add(visualMapWidthProperty())
                .divide(4)
                .subtract(controlButtonsPane.widthProperty().divide(2));

        NumberBinding layoutXWhenNoBand = uiOverlayPane.widthProperty()
                .subtract(controlButtonsPane.widthProperty())
                .subtract(15); // 15px padding from far right

        // FIXED: Position control buttons in right decorative area - simple and reliable approach
        NumberBinding rightDecorationWidth = uiOverlayPane.widthProperty()
                .subtract(visualMapWidthProperty())
                .divide(2); // Width of right decorative band
        
        NumberBinding mapRightEdge = uiOverlayPane.widthProperty()
                .add(visualMapWidthProperty())
                .divide(2); // Right edge of the map
        
        NumberBinding buttonPosition = Bindings.max(
            mapRightEdge.add(rightDecorationWidth.divide(2)).subtract(controlButtonsPane.widthProperty().divide(2)), // Center in right band
            mapRightEdge.add(20) // Minimum 20px from map edge
        );
        
        controlButtonsPane.layoutXProperty().bind(buttonPosition);
        controlButtonsPane.setLayoutY(15.0); // Set to fixed top padding

        // Position memory tracker on the left side
        memoryTracker.setLayoutX(15.0);
        memoryTracker.setLayoutY(200.0); // Below the game info panel

        // Initial state for time controls
        isPaused = false;
        gameController.setPaused(false);
        gameController.setSpeedAccelerated(false);
        updateTimeControlStates();

        // Mouse event handling on gameCanvas (remains the same)
        gameCanvas.setOnMouseMoved(e -> {
            renderTimer.setMousePosition(e.getX(), e.getY(), true);
            
            // Check for tower hover
            javafx.geometry.Point2D worldCoord = transformMouseCoords(e.getX(), e.getY());
            if (worldCoord != null) {
                Tower towerAtMouse = gameController.getTowerAt(worldCoord.getX(), worldCoord.getY());
                hoveredTower = towerAtMouse;
            } else {
                hoveredTower = null;
            }
        });
        gameCanvas.setOnMouseExited(e -> {
            renderTimer.setMousePosition(0, 0, false);
            hoveredTower = null; // Clear hover when mouse leaves canvas
        });
        
        // Zoom functionality - DISABLED during gameplay to prevent UI misalignment
        gameCanvas.setOnScroll(event -> {
            // Zoom is disabled during gameplay to prevent UI element misalignment
            // If you want to enable zoom, consider implementing UI-independent positioning
            event.consume(); // Consume the event to prevent any default behavior
        });
        
        // Pan functionality
        gameCanvas.setOnMousePressed(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                isPanning = true;
                lastMouseXForPan = event.getX();
                lastMouseYForPan = event.getY();
                dragOccurred = false;
                event.consume();
            }
        });
        
        gameCanvas.setOnMouseDragged(event -> {
            if (isPanning && event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                double deltaX = event.getX() - lastMouseXForPan;
                double deltaY = event.getY() - lastMouseYForPan;

                // Update pan position
                panX -= deltaX / currentEffectiveScale;
                panY -= deltaY / currentEffectiveScale;

                lastMouseXForPan = event.getX();
                lastMouseYForPan = event.getY();
                dragOccurred = true;
                event.consume();
            }
        });
        
        gameCanvas.setOnMouseReleased(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                isPanning = false;
                event.consume();
            }
        });

        gameCanvas.setOnMouseClicked(e -> {
            if (dragOccurred) {
                dragOccurred = false;
                e.consume();
                System.out.println("[GameScreen] Click ignored due to drag.");
                return;
            }
            System.out.println("[GameScreen] Canvas clicked. Screen Coords: (" + e.getX() + "," + e.getY()
                    + ") Button: " + e.getButton());

            javafx.geometry.Point2D worldCoord = transformMouseCoords(e.getX(), e.getY());
            if (worldCoord == null) {
                System.out.println("[GameScreen] World coordinate transformation failed.");
                return;
            }

            double worldX = worldCoord.getX();
            double worldY = worldCoord.getY();
            int tileX = (int) (worldX / TILE_SIZE);
            int tileY = (int) (worldY / TILE_SIZE);
            System.out.println("[GameScreen] World Coords: (" + worldX + "," + worldY + ") -> Tile: (" + tileX + ","
                    + tileY + ")");

            Point2D tileCenterWorld = new Point2D(tileX * TILE_SIZE + HALF_TILE_SIZE,
                    tileY * TILE_SIZE + HALF_TILE_SIZE);
            Point2D tileCenterScreen = worldTransform.transform(tileCenterWorld);

            boolean actionTaken = false;

            // --- Check for Gold Bag Click FIRST ---
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                List<DroppedGold> bags = gameController.getActiveGoldBags();
                // Iterate in reverse to allow safe removal if multiple bags overlap
                for (int i = bags.size() - 1; i >= 0; i--) {
                    DroppedGold bag = bags.get(i);
                    // Check if click (worldX, worldY) is within bag's bounds
                    if (worldX >= bag.getX() && worldX <= (bag.getX() + bag.getWidth()) &&
                            worldY >= bag.getY() && worldY <= (bag.getY() + bag.getHeight())) {

                        gameController.collectGoldBag(bag); // Controller handles adding gold and removing bag
                        renderTimer.setStatusMessage("Collected " + bag.getGoldAmount() + " Gold!");
                        actionTaken = true;
                        break; // Stop checking other bags if one is clicked
                    }
                }
            }
            // --- End Gold Bag Click Check ---

            if (actionTaken) {
                System.out.println("[GameScreen] Consuming click event because gold bag was collected.");
                e.consume();
                return; // Do not process tower/tile clicks if a bag was collected
            }

            // --- Tower/Tile Click Logic (existing) ---
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                System.out.println("[GameScreen] Primary click. Attempting to find tower at world: (" + worldX + ","
                        + worldY + ")");
                Tower existingTower = gameController.getTowerAt(worldX, worldY);
                System.out.println("[GameScreen] gameController.getTowerAt returned: "
                        + (existingTower != null ? existingTower.getName() : "null"));

                if (existingTower != null) {
                    System.out.println("[GameScreen] Existing tower found: " + existingTower.getName()
                            + ". Showing upgrade/sell popup.");
                    clearActivePopup();
                    createUpgradeSellPopup(tileCenterScreen.getX(), tileCenterScreen.getY(), existingTower, tileX,
                            tileY);
                    actionTaken = true;
                } else {
                    System.out.println("[GameScreen] No existing tower found by getTowerAt. Checking tile type.");
                    Tile clickedTile = gameController.getGameMap().getTile(tileX, tileY);
                    if (clickedTile != null) {
                        System.out.println("[GameScreen] Clicked tile type: " + clickedTile.getType());
                        if (clickedTile.getType() == TileType.TOWER_SLOT) {
                            System.out.println("[GameScreen] Tile is TOWER_SLOT. Showing build popup.");
                            clearActivePopup();
                            createBuildTowerPopup(tileCenterScreen.getX(), tileCenterScreen.getY(), tileX, tileY);
                            actionTaken = true;
                        } else {
                            System.out.println("[GameScreen] Tile is not TOWER_SLOT. Clearing active popup.");
                            clearActivePopup(); // Clears if clicked on non-actionable tile like PATH or GRASS
                        }
                    } else {
                        System.out.println("[GameScreen] Clicked tile is null. Clearing active popup.");
                        clearActivePopup();
                    }
                }
            } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                System.out.println("[GameScreen] Secondary click. Clearing active popup.");
                clearActivePopup();
            }

            // No explicit consume here, let existing logic decide or consume at the end if
            // needed
            // if (actionTakenOnTowerOrTile) { e.consume(); }
        });

        // Initialize and start the top bar update timer with game over checking
        topBarUpdateTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateGameInfoLabels();
                
                // Check for game over
                if (gameController.isGameOver()) {
                    stop();
                    renderTimer.stop();
                    showGameOverScreen();
                }
            }
        };
        topBarUpdateTimer.start();
    }

    private void updateTimeControlStates() {
        boolean isCurrentlyPaused = isPaused; // Cache isPaused state
        int currentSpeed = gameController.getGameSpeed();

        // Update Pause Button
        if (isCurrentlyPaused) {
            pauseButton.getStyleClass().add(TIME_CONTROL_SELECTED_STYLE_CLASS);
            pauseButton.setOpacity(1.0); // Selected
        } else {
            pauseButton.getStyleClass().removeAll(TIME_CONTROL_SELECTED_STYLE_CLASS);
            pauseButton.setOpacity(0.6); // Not selected
        }

        // Update Play Button
        if (!isCurrentlyPaused && currentSpeed == 1) {
            playButton.getStyleClass().add(TIME_CONTROL_SELECTED_STYLE_CLASS);
            playButton.setOpacity(1.0); // Selected
        } else {
            playButton.getStyleClass().removeAll(TIME_CONTROL_SELECTED_STYLE_CLASS);
            playButton.setOpacity(0.6); // Not selected
        }

        // Update Fast Forward Button
        if (!isCurrentlyPaused && currentSpeed == 2) {
            fastForwardButton.getStyleClass().add(TIME_CONTROL_SELECTED_STYLE_CLASS);
            fastForwardButton.setOpacity(1.0); // Selected
        } else {
            fastForwardButton.getStyleClass().removeAll(TIME_CONTROL_SELECTED_STYLE_CLASS);
            fastForwardButton.setOpacity(0.6); // Not selected
        }

        // Ensure lastTime in renderTimer is reset if we are unpausing
        if (renderTimer != null) {
            if (!isCurrentlyPaused) {
                renderTimer.start();
            } else {
                renderTimer.stop();
                renderTimer.lastTime = -1; // Explicitly reset lastTime here
            }
        }
    }

    private void updateGameInfoLabels() {
        if (goldLabel != null) {
            goldLabel.setText("" + gameController.getPlayerGold());
        }
        if (livesLabel != null) {
            livesLabel.setText("" + gameController.getPlayerLives());
        }
        if (waveLabel != null) {
            // Enhanced wave display showing current/total
            int currentWave = gameController.getCurrentWave();
            int totalWaves = gameController.getTotalWaves();
            
            if (gameController.isInGracePeriod()) {
                waveLabel.setText("Grace Period");
            } else if (currentWave == 0) {
                waveLabel.setText("Starting...");
            } else {
                waveLabel.setText("Wave " + currentWave + "/" + totalWaves);
            }
        }
        // Pause/Speed button text/icon updates are now handled by
        // updateTimeControlStates()
        
        // Update freeze button availability
        updateFreezeButtonStyle();
    }

    /**
     * Start the render loop for the game canvas.
     */
    private void startRenderLoop() {
        renderTimer = new GameRenderTimer();
        renderTimer.start();

        // Add mouse moved listener to track position
        // gameCanvas.setOnMouseMoved(e -> { // REMOVED - Already set in initializeUI
        // renderTimer.setMousePosition(e.getX(), e.getY(), true);
        // });

        // Track when mouse exits canvas
        // gameCanvas.setOnMouseExited(e -> { // REMOVED - Already set in initializeUI
        // renderTimer.setMousePosition(0, 0, false);
        // });
    }

    /**
     * Transforms mouse coordinates from Canvas space to World space.
     * 
     * @param canvasX Mouse X relative to canvas.
     * @param canvasY Mouse Y relative to canvas.
     * @return Point2D in world coordinates, or null if transform is invalid.
     */
    private javafx.geometry.Point2D transformMouseCoords(double canvasX, double canvasY) {
        try {
            // Use the inverse of the world transform to go from canvas -> world
            return worldTransform.inverseTransform(canvasX, canvasY);
        } catch (javafx.scene.transform.NonInvertibleTransformException e) {
            System.err.println("Warning: Could not invert world transform for mouse input.");
            return null; // Return null if transform is broken
        }
    }

    private void clearActivePopup() {
        if (activePopup != null) {
            final Node popupNodeBeingCleared = activePopup; // Final for use in lambda
            activePopup = null; // Crucial: mark no popup as active *now*

            // Make the outgoing popup non-interactive immediately
            popupNodeBeingCleared.setMouseTransparent(true);

            FadeTransition ft = new FadeTransition(Duration.millis(150), popupNodeBeingCleared);
            // ft.setFromValue(1.0); // Assuming opacity is 1.0 when clearing
            ft.setFromValue(popupNodeBeingCleared.getOpacity()); // Fade from current opacity, good if it could be
                                                                 // non-1.0
            ft.setToValue(0.0);
            ft.setOnFinished(event -> {
                uiOverlayPane.getChildren().remove(popupNodeBeingCleared);
                // Optional: Reset mouseTransparent if the node were to be reused,
                // but it's being removed from the scene graph, so not strictly necessary.
                // popupNodeBeingCleared.setMouseTransparent(false);
            });
            ft.play();
        }
    }

    private void createBuildTowerPopup(double centerXScreen, double centerYScreen, int tileX, int tileY) {
        clearActivePopup();
        Pane popupPane = new Pane();
        popupPane.setPickOnBounds(false);

        List<TowerBuildOption> options = new ArrayList<>();
        // Ensure ArcherTower, MageTower, ArtilleryTower have public static int
        // BASE_COST;
        options.add(new TowerBuildOption("Archer Tower", ArcherTower.BASE_COST, 0, 2, () -> new ArcherTower(0, 0)));
        options.add(new TowerBuildOption("Mage Tower", MageTower.BASE_COST, 2, 2, () -> new MageTower(0, 0)));
        options.add(new TowerBuildOption("Close", 0, 3, 0, null));
        options.add(new TowerBuildOption("Artillery Tower", ArtilleryTower.BASE_COST, 3, 2,
                () -> new ArtilleryTower(0, 0)));

        int numOptions = options.size();
        double angleStep = 360.0 / numOptions;

        for (int i = 0; i < numOptions; i++) {
            TowerBuildOption opt = options.get(i);
            double angle = (i * angleStep) - 90; // Start at top (-90 degrees)
            double buttonX = centerXScreen + BUILD_POPUP_RADIUS * Math.cos(Math.toRadians(angle))
                    - POPUP_ICON_SIZE / 2.0;
            double buttonY = centerYScreen + BUILD_POPUP_RADIUS * Math.sin(Math.toRadians(angle))
                    - POPUP_ICON_SIZE / 2.0;

            Button button = UIAssets.createIconButton(opt.name + (opt.cost > 0 ? " (Cost: " + opt.cost + ")" : ""),
                    opt.iconCol, opt.iconRow, POPUP_ICON_SIZE);
            button.setLayoutX(buttonX);
            button.setLayoutY(buttonY);

            if (opt.constructor != null) {
                button.setOnAction(e -> {
                    Tower towerToBuild = opt.constructor.get(); // Creates a template tower
                    gameController.purchaseAndPlaceTower(towerToBuild, tileX, tileY); // Pass tile coords
                    clearActivePopup();
                    e.consume();
                });
            } else { // Close button
                button.setOnAction(e -> {
                    clearActivePopup();
                    e.consume();
                });
            }
            popupPane.getChildren().add(button);
        }

        activePopup = popupPane;
        uiOverlayPane.getChildren().add(activePopup);
        // Apply animation
        FadeTransition ft = new FadeTransition(Duration.millis(200), activePopup);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(200), activePopup);
        st.setFromX(0.7);
        st.setFromY(0.7);
        st.setToX(1.0);
        st.setToY(1.0);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
    }

    private void createUpgradeSellPopup(double centerXScreen, double centerYScreen, Tower existingTower, int tileX,
            int tileY) {
        clearActivePopup();
        Pane popupPane = new Pane();
        popupPane.setPickOnBounds(false);

        List<Button> buttons = new ArrayList<>();
        double angleStart = -90; // Top
        double radius = UPGRADE_SELL_POPUP_RADIUS;

        // Upgrade Button
        if (existingTower.canUpgrade()) {
            int upgradeCost = existingTower.getUpgradeCost();
            boolean canAfford = gameController.getPlayerGold() >= upgradeCost;
            String upgradeText = "Upgrade (";
            if (upgradeCost == Integer.MAX_VALUE) { // Should not happen if canUpgrade is true, but good check
                upgradeText += "N/A)";
            } else {
                upgradeText += upgradeCost + "G)";
            }

            Button upgradeButton = UIAssets.createIconButton(upgradeText, 1, 2, POPUP_ICON_SIZE); // Upgrade icon (1,2)

            if (canAfford) {
                upgradeButton.setOnAction(e -> {
                    boolean upgraded = gameController.upgradeTower(existingTower, tileX, tileY);
                    // if (upgraded) { // Effect removed
                    // }
                    clearActivePopup();
                    e.consume();
                });
            } else {
                upgradeButton.setDisable(true);
                // Optionally add a specific style class for better visual indication
                // e.g., upgradeButton.getStyleClass().add("disabled-upgrade-button");
                // Tooltip could also indicate why it's disabled
                upgradeButton
                        .setTooltip(new javafx.scene.control.Tooltip("Not enough gold! Needs: " + upgradeCost + "G"));
            }
            buttons.add(upgradeButton);
        }
        // Sell Button
        Button sellButton = UIAssets.createIconButton("Sell (+" + existingTower.getSellRefund() + "G)", 1, 0,
                POPUP_ICON_SIZE); // Sell icon (1,0)
        sellButton.setOnAction(e -> {
            gameController.sellTower(tileX, tileY);
            clearActivePopup();
            e.consume();
        });
        buttons.add(sellButton);

        // Close button
        Button closeButton = UIAssets.createIconButton("Close", 3, 0, POPUP_ICON_SIZE); // Close icon (3,0)
        closeButton.setOnAction(e -> {
            clearActivePopup();
            e.consume();
        });
        buttons.add(closeButton);

        int numButtons = buttons.size();
        double angleStep = numButtons > 1 ? ((numButtons == 2) ? 60 : 360.0 / numButtons) : 0; // Adjust for few buttons
        if (numButtons == 2)
            angleStart = -120; // Adjust start angle for 2 buttons to be bottom-ish
        if (numButtons == 3 && buttons.get(0).getTooltip().getText().startsWith("Upgrade"))
            angleStart = -90; // Standard for 3
        else if (numButtons == 2 && buttons.get(0).getTooltip().getText().startsWith("Sell"))
            angleStart = -60; // Only sell and close, put them side by side nicely

        for (int i = 0; i < numButtons; i++) {
            Button button = buttons.get(i);
            double angle = angleStart + (i * angleStep);
            double buttonX = centerXScreen + radius * Math.cos(Math.toRadians(angle)) - POPUP_ICON_SIZE / 2.0;
            double buttonY = centerYScreen + radius * Math.sin(Math.toRadians(angle)) - POPUP_ICON_SIZE / 2.0;
            button.setLayoutX(buttonX);
            button.setLayoutY(buttonY);
            popupPane.getChildren().add(button);
        }

        activePopup = popupPane;
        uiOverlayPane.getChildren().add(activePopup);
        // Apply animation
        FadeTransition ft = new FadeTransition(Duration.millis(150), activePopup);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(Duration.millis(150), activePopup);
        st.setFromX(0.7);
        st.setFromY(0.7);
        st.setToX(1.0);
        st.setToY(1.0);
                    st.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();
    }

    public void stop() { // Assuming this method exists or should be added for cleanup
        if (renderTimer != null) {
            renderTimer.stop();
        }
        if (topBarUpdateTimer != null) {
            topBarUpdateTimer.stop();
        }
        if (memoryTracker != null) {
            memoryTracker.stop();
        }
        gameController.stopGame(); // Ensure controller's game loop is also stopped
    }

    /**
     * Show the enhanced game over screen with victory/defeat status.
     */
    private void showGameOverScreen() {
        Platform.runLater(() -> {
            clearActivePopup();
            
            // Determine if player won or lost
            boolean playerWon = gameController.getCurrentWave() >= gameController.getTotalWaves();
            int finalWave = gameController.getCurrentWave();
            int totalWaves = gameController.getTotalWaves();
            int finalGold = gameController.getPlayerGold();
            int finalLives = gameController.getPlayerLives();
            
            // Create game over overlay with animated background
            StackPane gameOverPane = new StackPane();
            gameOverPane.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 80%, " +
                                 "rgba(0, 0, 0, 0.95), rgba(20, 20, 40, 0.98));");
            gameOverPane.prefWidthProperty().bind(gameCanvas.widthProperty());
            gameOverPane.prefHeightProperty().bind(gameCanvas.heightProperty());
            
            VBox contentBox = new VBox(25);
            contentBox.setAlignment(Pos.CENTER);
            contentBox.setMaxWidth(500);
            contentBox.setPadding(new Insets(50));
            
            // Enhanced gradient background based on win/loss
            String primaryColor = playerWon ? "#2e7d32" : "#c62828";
            String secondaryColor = playerWon ? "#4caf50" : "#f44336";
            String accentColor = playerWon ? "#81c784" : "#ef5350";
            
            contentBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(30, 30, 30, 0.98), rgba(10, 10, 10, 0.99));" +
                "-fx-background-radius: 20px; " +
                "-fx-border-color: linear-gradient(to bottom, " + primaryColor + ", " + secondaryColor + "); " +
                "-fx-border-width: 4px; " +
                "-fx-border-radius: 20px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 25, 0, 0, 8);"
            );
            
            // Animated title with larger, more dramatic text
            Label titleLabel = new Label(playerWon ? "🏆 VICTORY ACHIEVED! 🏆" : "⚔️ DEFEAT ⚔️");
            titleLabel.setStyle(
                "-fx-font-size: 42px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: linear-gradient(to right, " + secondaryColor + ", " + accentColor + ", " + secondaryColor + "); " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 5, 0, 2, 2);"
            );
            titleLabel.setWrapText(true);
            titleLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            // Enhanced status message with better typography
            Label statusLabel = new Label(playerWon ? 
                "🎊 Outstanding! You have successfully defended the kingdom against all enemies! 🎊" :
                "💔 The kingdom has fallen to the enemy forces. Rally your defenses and try again! 💔");
            statusLabel.setStyle(
                "-fx-font-size: 18px; " +
                "-fx-text-fill: #e0e0e0; " +
                "-fx-text-alignment: center; " +
                "-fx-line-spacing: 2px;"
            );
            statusLabel.setWrapText(true);
            statusLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            statusLabel.setMaxWidth(450);
            
            // Enhanced statistics box with better visual hierarchy
            VBox statsBox = new VBox(15);
            statsBox.setAlignment(Pos.CENTER);
            statsBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " +
                "rgba(40, 40, 60, 0.8), rgba(20, 20, 40, 0.9));" +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: rgba(100, 150, 200, 0.5); " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-padding: 25px; " +
                "-fx-effect: innershadow(gaussian, rgba(0, 0, 0, 0.5), 5, 0, 0, 2);"
            );
            
            // Statistics title
            Label statsTitle = new Label("📊 FINAL STATISTICS 📊");
            statsTitle.setStyle(
                "-fx-font-size: 20px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #ffd700; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 2, 0, 1, 1);"
            );
            
            // Individual stat items with icons and better formatting
            HBox waveStatsBox = createStatItem("🌊", "Waves Completed", finalWave + "/" + totalWaves, 
                                               playerWon ? "#4caf50" : "#ff9800");
            HBox goldStatsBox = createStatItem("💰", "Final Gold", String.valueOf(finalGold), "#ffd700");
            HBox livesStatsBox = createStatItem("❤️", "Lives Remaining", String.valueOf(finalLives), 
                                                finalLives > 0 ? "#4caf50" : "#f44336");
            
            statsBox.getChildren().addAll(statsTitle, waveStatsBox, goldStatsBox, livesStatsBox);
            
            // Enhanced buttons with better styling and spacing
            HBox buttonBox = new HBox(20);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));
            
            Button playAgainButton = createEnhancedGameOverButton("🔄 Play Again", "#1976d2", "#2196f3");
            playAgainButton.setOnAction(e -> returnToMainMenu());
            
            Button mainMenuButton = createEnhancedGameOverButton("🏠 Main Menu", "#f57c00", "#ff9800");
            mainMenuButton.setOnAction(e -> returnToMainMenu());
            
            buttonBox.getChildren().addAll(playAgainButton, mainMenuButton);
            
            contentBox.getChildren().addAll(titleLabel, statusLabel, statsBox, buttonBox);
            gameOverPane.getChildren().add(contentBox);
            
            // Add overlay to the UI
            uiOverlayPane.getChildren().add(gameOverPane);
            
            // Enhanced animation sequence
            gameOverPane.setOpacity(0.0);
            contentBox.setScaleX(0.7);
            contentBox.setScaleY(0.7);
            
            // Fade in background
            FadeTransition backgroundFade = new FadeTransition(Duration.millis(800), gameOverPane);
            backgroundFade.setFromValue(0.0);
            backgroundFade.setToValue(1.0);
            
            // Scale and fade content
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(600), contentBox);
            scaleUp.setFromX(0.7);
            scaleUp.setFromY(0.7);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);
            scaleUp.setInterpolator(Interpolator.EASE_OUT);
            
            // Sequence the animations
            ParallelTransition showAnimation = new ParallelTransition(backgroundFade, scaleUp);
            showAnimation.setDelay(Duration.millis(100));
            showAnimation.play();
        });
    }
    
    /**
     * Create a styled stat item for the game over screen.
     */
    private HBox createStatItem(String icon, String label, String value, String valueColor) {
        HBox statBox = new HBox(10);
        statBox.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");
        
        Label labelText = new Label(label + ":");
        labelText.setStyle("-fx-font-size: 16px; -fx-text-fill: #cccccc; -fx-font-weight: normal;");
        
        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 16px; -fx-text-fill: " + valueColor + "; -fx-font-weight: bold;");
        
        statBox.getChildren().addAll(iconLabel, labelText, valueText);
        return statBox;
    }
    
    /**
     * Create enhanced buttons for the game over screen.
     */
    private Button createEnhancedGameOverButton(String text, String baseColor, String hoverColor) {
        Button button = new Button(text);
        button.setPrefWidth(180);
        button.setPrefHeight(50);
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + baseColor + ", derive(" + baseColor + ", -15%));" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-border-color: derive(" + baseColor + ", 20%);" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 8, 0, 0, 4);"
        );
        
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + hoverColor + ", derive(" + hoverColor + ", -15%));" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-border-color: derive(" + hoverColor + ", 20%);" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 12, 0, 0, 6);" +
            "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
        ));
        
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + baseColor + ", derive(" + baseColor + ", -15%));" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-radius: 12px;" +
            "-fx-border-color: derive(" + baseColor + ", 20%);" +
            "-fx-border-width: 2px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 8, 0, 0, 4);" +
            "-fx-scale-x: 1.0; -fx-scale-y: 1.0;"
        ));
        
        return button;
    }

    /**
     * Enhanced return to main menu with proper cleanup and transition.
     */
    private void returnToMainMenu() {
        stop(); // Stop all timers and game
        
        // Enhanced transition to main menu
        MainMenuScreen mainMenu = new MainMenuScreen(primaryStage);
        double targetWidth = primaryStage.getScene() != null ? primaryStage.getScene().getWidth() : primaryStage.getWidth();
        double targetHeight = primaryStage.getScene() != null ? primaryStage.getScene().getHeight() : primaryStage.getHeight();
        Scene scene = new Scene(mainMenu, targetWidth, targetHeight);
        
        // IMPORTANT: Add the CSS stylesheet to prevent white screen
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        // Apply custom cursor
        ImageCursor customCursor = UIAssets.getCustomCursor();
        if (customCursor != null) {
            scene.setCursor(customCursor);
        }
        
        // Enhanced transition
        primaryStage.setFullScreen(false);
        primaryStage.setScene(scene);
        
        Platform.runLater(() -> {
            primaryStage.setFullScreen(true);
        });
    }

    private void showGameSettingsPopup() {
        clearActivePopup(); // Clear any existing popups like tower build/upgrade
        
        // Auto-pause the game when menu opens
        isPaused = true;
        gameController.setPaused(true);
        updateTimeControlStates();

        // Create medieval-themed side panel with wooden background
        VBox sidePanel = new VBox(20);
        sidePanel.setPadding(new Insets(30, 25, 30, 25));
        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPrefWidth(320);
        sidePanel.setMaxWidth(320);
        sidePanel.setPrefHeight(uiOverlayPane.getHeight());
        
        // Medieval wooden panel styling with authentic textures
        try {
            URL woodUrl = getClass().getResource("/Asset_pack/Background/wood.jpg");
            if (woodUrl != null) {
        sidePanel.setStyle(
                    "-fx-background-image: url('" + woodUrl.toExternalForm() + "');" +
                    "-fx-background-repeat: repeat;" +
                    "-fx-background-size: 200px 200px;" +
                    "-fx-border-color: linear-gradient(to bottom, #8B4513, #654321, #3E2723);" +
                    "-fx-border-width: 0 0 0 8px;" +
                    "-fx-border-style: solid;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 20, 0, -8, 0), " +
                    "innershadow(gaussian, rgba(139, 69, 19, 0.3), 10, 0, 2, 2);"
                );
            } else {
                // Fallback wooden styling
                sidePanel.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #8B4513, #654321, #3E2723);" +
                    "-fx-border-color: linear-gradient(to bottom, #D2691E, #8B4513, #654321);" +
                    "-fx-border-width: 0 0 0 8px;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 20, 0, -8, 0);"
                );
            }
        } catch (Exception e) {
            System.err.println("Error loading wood background: " + e.getMessage());
            // Fallback wooden styling
            sidePanel.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #8B4513, #654321, #3E2723);" +
                "-fx-border-color: linear-gradient(to bottom, #D2691E, #8B4513, #654321);" +
                "-fx-border-width: 0 0 0 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.9), 20, 0, -8, 0);"
            );
        }

        // Medieval banner title using ribbon assets
        StackPane titleBanner = new StackPane();
        titleBanner.setPrefWidth(280);
        titleBanner.setPrefHeight(60);
        
        // Use the blue ribbon as background for title
        try {
            Image blueRibbon = UIAssets.getImage("Ribbon_Blue_3Slides");
            if (blueRibbon != null) {
                ImageView ribbonBg = new ImageView(blueRibbon);
                ribbonBg.setFitWidth(280);
                ribbonBg.setFitHeight(60);
                ribbonBg.setPreserveRatio(false);
                titleBanner.getChildren().add(ribbonBg);
            }
        } catch (Exception e) {
            System.err.println("Error loading blue ribbon: " + e.getMessage());
        }
        
        Label title = new Label("⚔ GAME MENU ⚔");
        title.setStyle(
            "-fx-font-family: 'Serif';" +
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #F5DEB3;" + // Wheat color for medieval parchment look
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 3, 0, 1, 1);"
        );
        titleBanner.getChildren().add(title);
        


        // Medieval action buttons using the game's button assets
        Button resumeButton = createMedievalButton("▶ Resume Battle", "blue");
        resumeButton.setOnAction(e -> {
            // Unpause the game when resuming
            isPaused = false;
            gameController.setPaused(false);
            gameController.setSpeedAccelerated(false); // Reset to normal speed
            updateTimeControlStates();
            clearActivePopup();
            e.consume();
        });

        // Save/Load section
        VBox saveLoadSection = createSaveLoadSection();
        
        // Medieval music selection scroll
        VBox musicSection = createMedievalMusicSection();

        Button mainMenuButton = createMedievalButton("🏠 Return to Castle", "red");
        mainMenuButton.setOnAction(e -> {
            stop();
            MainMenuScreen mainMenu = new MainMenuScreen(primaryStage);
            double targetWidth = primaryStage.getScene() != null ? primaryStage.getScene().getWidth() : primaryStage.getWidth();
            double targetHeight = primaryStage.getScene() != null ? primaryStage.getScene().getHeight() : primaryStage.getHeight();
            Scene scene = new Scene(mainMenu, targetWidth, targetHeight);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            ImageCursor customCursor = UIAssets.getCustomCursor();
            if (customCursor != null) scene.setCursor(customCursor);
            primaryStage.setScene(scene);
            primaryStage.setFullScreen(true);
            e.consume();
        });

        // Add decorative spacing with medieval elements
        javafx.scene.layout.Region spacer1 = new javafx.scene.layout.Region();
        spacer1.setPrefHeight(25);
        javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region();
        spacer2.setPrefHeight(25);
        javafx.scene.layout.Region spacer2b = new javafx.scene.layout.Region();
        spacer2b.setPrefHeight(25);
        javafx.scene.layout.Region spacer3 = new javafx.scene.layout.Region();
        spacer3.setPrefHeight(25);

        sidePanel.getChildren().addAll(titleBanner, spacer1, resumeButton, spacer2, saveLoadSection, spacer2b, musicSection, spacer3, mainMenuButton);

        // Position panel off-screen initially (slide from right)
        sidePanel.setLayoutX(uiOverlayPane.getWidth());
        sidePanel.setLayoutY(0);

        activePopup = sidePanel;
        uiOverlayPane.getChildren().add(activePopup);

        // Medieval-style slide-in animation (slower, more dramatic)
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), sidePanel);
        slideIn.setFromX(0);
        slideIn.setToX(-320); // Slide in by panel width
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), sidePanel);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        ParallelTransition showPanel = new ParallelTransition(slideIn, fadeIn);
        showPanel.play();
    }
    
    /**
     * Create a medieval-themed button using the game's UI assets
     */
    private Button createMedievalButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefWidth(260);
        button.setPrefHeight(55);
        
        // Create beautiful medieval button styling similar to Bard's Melodies
        String baseColor, hoverColor;
        switch (color) {
            case "blue":
                baseColor = "#4682B4"; // Steel blue
                hoverColor = "#5A9BD4";
                break;
            case "green":
                baseColor = "#228B22"; // Forest green
                hoverColor = "#32CD32";
                break;
            case "purple":
                baseColor = "#8B008B"; // Dark magenta
                hoverColor = "#BA55D3";
                break;
            case "red":
                baseColor = "#DC143C"; // Crimson
                hoverColor = "#FF6347";
                break;
            default:
                baseColor = "#8B4513"; // Saddle brown
                hoverColor = "#A0522D";
                break;
        }
        
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + baseColor + ", derive(" + baseColor + ", -20%));" +
            "-fx-text-fill: #F5DEB3;" + // Wheat color like Bard's Melodies
            "-fx-font-family: 'Serif';" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: derive(" + baseColor + ", -30%);" +
            "-fx-border-width: 3px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 4, 0, 1, 2);"
        );
        
        // Add hover effects similar to music buttons
        button.setOnMouseEntered(e -> {
            button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + hoverColor + ", derive(" + hoverColor + ", -20%));" +
                "-fx-text-fill: #FFFACD;" + // Light goldenrod like music buttons
                "-fx-font-family: 'Serif';" +
                "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8px;" +
                "-fx-border-color: derive(" + hoverColor + ", -30%);" +
                "-fx-border-width: 3px;" +
            "-fx-border-radius: 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 6, 0, 1, 3);" +
                "-fx-scale-x: 1.03; -fx-scale-y: 1.03;"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + baseColor + ", derive(" + baseColor + ", -20%));" +
                "-fx-text-fill: #F5DEB3;" +
                "-fx-font-family: 'Serif';" +
                "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 8px;" +
                "-fx-border-color: derive(" + baseColor + ", -30%);" +
                "-fx-border-width: 3px;" +
            "-fx-border-radius: 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 4, 0, 1, 2);" +
            "-fx-scale-x: 1.0; -fx-scale-y: 1.0;"
            );
        });
        
        return button;
    }
    
    /**
     * Create the save/load section with medieval styling
     */
    private VBox createSaveLoadSection() {
        VBox saveLoadSection = new VBox(12);
        saveLoadSection.setAlignment(Pos.CENTER);
        saveLoadSection.setPrefWidth(280);
        
        // Save/Load banner with green ribbon
        StackPane saveLoadBanner = new StackPane();
        saveLoadBanner.setPrefWidth(260);
        saveLoadBanner.setPrefHeight(50);
        
        try {
            Image blueRibbon = UIAssets.getImage("Ribbon_Blue_3Slides");
            if (blueRibbon != null) {
                ImageView saveLoadRibbonBg = new ImageView(blueRibbon);
                saveLoadRibbonBg.setFitWidth(260);
                saveLoadRibbonBg.setFitHeight(50);
                saveLoadRibbonBg.setPreserveRatio(false);
                saveLoadBanner.getChildren().add(saveLoadRibbonBg);
            }
        } catch (Exception e) {
            System.err.println("Error loading blue ribbon: " + e.getMessage());
        }
        
        Label saveLoadTitle = new Label("💾 ROYAL ARCHIVES 💾");
        saveLoadTitle.setStyle(
            "-fx-font-family: 'Serif';" +
            "-fx-font-size: 18px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #F5DEB3;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 2, 0, 1, 1);"
        );
        saveLoadBanner.getChildren().add(saveLoadTitle);
        
        // Save/Load buttons container
        VBox buttonContainer = new VBox(8);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(15));
        buttonContainer.setStyle(
            "-fx-background-color: rgba(139, 69, 19, 0.3);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: #8B4513;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: innershadow(gaussian, rgba(0, 0, 0, 0.5), 5, 0, 0, 2);"
        );
        
        // Save Game button
        Button saveButton = createMedievalButton("💾 Save Kingdom", "green");
        saveButton.setOnAction(e -> {
            showSaveGameDialog();
            e.consume();
        });
        
        // Load Game button
        Button loadButton = createMedievalButton("📜 Load Kingdom", "purple");
        loadButton.setOnAction(e -> {
            showLoadGameDialog();
            e.consume();
        });
        
        buttonContainer.getChildren().addAll(saveButton, loadButton);
        saveLoadSection.getChildren().addAll(saveLoadBanner, buttonContainer);
        
        return saveLoadSection;
    }
    
    /**
     * Create the medieval-themed music selection section.
     */
    private VBox createMedievalMusicSection() {
        VBox musicSection = new VBox(12);
        musicSection.setAlignment(Pos.CENTER);
        musicSection.setPrefWidth(280);
        
        // Medieval music scroll banner
        StackPane musicBanner = new StackPane();
        musicBanner.setPrefWidth(260);
        musicBanner.setPrefHeight(50);
        
        try {
            Image redRibbon = UIAssets.getImage("Ribbon_Red_3Slides");
            if (redRibbon != null) {
                ImageView musicRibbonBg = new ImageView(redRibbon);
                musicRibbonBg.setFitWidth(260);
                musicRibbonBg.setFitHeight(50);
                musicRibbonBg.setPreserveRatio(false);
                musicBanner.getChildren().add(musicRibbonBg);
            }
        } catch (Exception e) {
            System.err.println("Error loading red ribbon: " + e.getMessage());
        }
        
        Label musicTitle = new Label("🎵 BARD'S MELODIES 🎵");
        musicTitle.setStyle(
            "-fx-font-family: 'Serif';" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #F5DEB3;" + // Wheat color for parchment look
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 2, 0, 1, 1);"
        );
        musicBanner.getChildren().add(musicTitle);
        
        // Medieval music tracks with thematic names
        List<MusicTrack> musicTracks = Arrays.asList(
            new MusicTrack("Yeah", "Yeah.mp3", "⚔️ Battle Anthem"),
            new MusicTrack("Club", "Club.mp3", "🏰 Castle Feast"),
            new MusicTrack("Hips", "Hips.mp3", "🎭 Royal Dance"),
            new MusicTrack("Candy", "Candy.mp3", "🌸 Spring Festival"),
            new MusicTrack("Toxic", "Toxic.mp3", "⚡ Thunder Storm")
        );
        
        // Create medieval-styled music list with wooden background
        VBox musicList = new VBox(8);
        musicList.setAlignment(Pos.CENTER);
        musicList.setPadding(new Insets(15));
        musicList.setStyle(
            "-fx-background-color: rgba(139, 69, 19, 0.3);" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: #8B4513;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: innershadow(gaussian, rgba(0, 0, 0, 0.5), 5, 0, 0, 2);"
        );
        
        for (MusicTrack track : musicTracks) {
            Button trackButton = createMedievalMusicButton(track);
            musicList.getChildren().add(trackButton);
        }
        
        // Make music list scrollable if needed
        ScrollPane scrollPane = new ScrollPane(musicList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(150);
        scrollPane.setMaxHeight(150);
        scrollPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background: transparent;" +
            "-fx-border-color: transparent;"
        );
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Wood-colored scrollbar styling will be added via CSS later
        
        musicSection.getChildren().addAll(musicBanner, scrollPane);
        return musicSection;
    }
    
    /**
     * Create a medieval-themed music track button
     */
    private Button createMedievalMusicButton(MusicTrack track) {
        Button button = new Button(track.displayName);
        button.setPrefWidth(220);
        button.setPrefHeight(35);
        
        // Medieval wooden button styling
        button.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #D2691E, #8B4513);" +
            "-fx-text-fill: #F5DEB3;" + // Wheat color for readability
            "-fx-font-family: 'Serif';" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 6px;" +
            "-fx-border-color: #654321;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 6px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 3, 0, 1, 1);"
        );
        
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #CD853F, #A0522D);" +
                "-fx-text-fill: #FFFACD;" + // Light goldenrod yellow for hover
                "-fx-font-family: 'Serif';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: #8B4513;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 6px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 4, 0, 1, 2);" +
                "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #D2691E, #8B4513);" +
                "-fx-text-fill: #F5DEB3;" + // Back to wheat color
                "-fx-font-family: 'Serif';" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 6px;" +
                "-fx-border-color: #654321;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 6px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.7), 3, 0, 1, 1);" +
                "-fx-scale-x: 1.0; -fx-scale-y: 1.0;"
            );
        });
        
        button.setOnAction(e -> {
            switchMusic(track.fileName);
            // Show medieval-style feedback message
            if (renderTimer != null) {
                renderTimer.setStatusMessage("🎵 Now playing: " + track.displayName + " 🎵");
            }
        });
        
        return button;
    }
    
    /**
     * Switch to a different music track.
     */
    private void switchMusic(String fileName) {
        try {
            Main.switchBackgroundMusic(fileName);
        } catch (Exception e) {
            System.err.println("Error switching music to " + fileName + ": " + e.getMessage());
            renderTimer.setStatusMessage("❌ Failed to switch music");
        }
    }
    
    /**
     * Helper class to represent a music track.
     */
    private static class MusicTrack {
        final String name;
        final String fileName;
        final String displayName;
        
        MusicTrack(String name, String fileName, String displayName) {
            this.name = name;
            this.fileName = fileName;
            this.displayName = displayName;
        }
    }

    // Getter for visualMapWidth (optional, but good practice)
    public double getVisualMapWidth() {
        return visualMapWidthProperty.get();
    }

    // Property getter for visualMapWidth (needed for bindings)
    public ReadOnlyDoubleProperty visualMapWidthProperty() {
        return visualMapWidthProperty.getReadOnlyProperty();
    }
    
    /**
     * Toggle the memory tracker visibility
     */
    private void toggleMemoryTracker() {
        memoryTrackerVisible = !memoryTrackerVisible;
        memoryTracker.setVisible(memoryTrackerVisible);
        
        if (memoryTrackerVisible) {
            memoryTracker.start();
            if (renderTimer != null) {
                renderTimer.setStatusMessage("🔧 Memory Tracker Enabled");
            }
        } else {
            memoryTracker.stop();
            if (renderTimer != null) {
                renderTimer.setStatusMessage("🔧 Memory Tracker Disabled");
            }
        }
    }
    
    /**
     * Show save game dialog with medieval styling
     */
    private void showSaveGameDialog() {
        try {
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("My Kingdom");
            dialog.setTitle("💾 Save Your Kingdom");
            dialog.setHeaderText("📜 Chronicle Your Victory");
            dialog.setContentText("Enter a name for your save:");
            
            // Apply medieval styling to dialog
            dialog.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #F5DEB3, #DEB887);" +
                "-fx-border-color: #8B4513; -fx-border-width: 3px;"
            );
            
            java.util.Optional<String> result = dialog.showAndWait();
            result.ifPresent(saveName -> {
                if (saveName != null && !saveName.trim().isEmpty()) {
                    try {
                        GameSaveService saveService = GameSaveService.getInstance();
                        boolean success = saveService.saveGame(gameController, saveName.trim());
                        
                        if (success) {
                            if (renderTimer != null) {
                                renderTimer.setStatusMessage("💾 Kingdom saved successfully!");
                            }
                        } else {
                            if (renderTimer != null) {
                                renderTimer.setStatusMessage("❌ Failed to save kingdom!");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error during save: " + e.getMessage());
                        if (renderTimer != null) {
                            renderTimer.setStatusMessage("❌ Save error: " + e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error showing save dialog: " + e.getMessage());
            if (renderTimer != null) {
                renderTimer.setStatusMessage("❌ Could not open save dialog!");
            }
        }
    }
    
    /**
     * Show load game dialog with available saves
     */
    private void showLoadGameDialog() {
        try {
            GameSaveService saveService = GameSaveService.getInstance();
            List<GameSaveService.SaveFileInfo> saves = saveService.getAvailableSaves();
        
        if (saves.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("📜 Royal Archives");
            alert.setHeaderText("No Saved Kingdoms Found");
            alert.setContentText("The royal archives are empty. Save your current kingdom first!");
            
            // Apply medieval styling
            alert.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #F5DEB3, #DEB887);" +
                "-fx-border-color: #8B4513; -fx-border-width: 3px;"
            );
            
            alert.showAndWait();
            return;
        }
        
        // Create choice dialog with save files
        javafx.scene.control.ChoiceDialog<GameSaveService.SaveFileInfo> dialog = 
            new javafx.scene.control.ChoiceDialog<>(saves.get(0), saves);
        
        dialog.setTitle("📜 Load Your Kingdom");
        dialog.setHeaderText("🏰 Choose a Saved Kingdom");
        dialog.setContentText("Select a save file:");
        
        // Apply medieval styling
        dialog.getDialogPane().setStyle(
            "-fx-background-color: linear-gradient(to bottom, #F5DEB3, #DEB887);" +
            "-fx-border-color: #8B4513; -fx-border-width: 3px;"
        );
        
        // Custom converter to show save info nicely
        javafx.scene.control.ComboBox<GameSaveService.SaveFileInfo> comboBox = 
            (javafx.scene.control.ComboBox<GameSaveService.SaveFileInfo>) dialog.getDialogPane().lookup(".combo-box");
        if (comboBox != null) {
            comboBox.setConverter(new javafx.util.StringConverter<GameSaveService.SaveFileInfo>() {
                @Override
                public String toString(GameSaveService.SaveFileInfo saveInfo) {
                    if (saveInfo == null) return "";
                    return String.format("%s - Wave %d (%s)", 
                        saveInfo.saveName, saveInfo.currentWave, saveInfo.getFormattedTime());
                }
                
                @Override
                public GameSaveService.SaveFileInfo fromString(String string) {
                    return null; // Not needed for this use case
                }
            });
        }
        
        java.util.Optional<GameSaveService.SaveFileInfo> result = dialog.showAndWait();
        result.ifPresent(saveInfo -> {
            if (saveInfo != null && saveInfo.isValid) {
                boolean success = saveService.loadGame(gameController, saveInfo.filename);
                
                if (success) {
                    if (renderTimer != null) {
                        renderTimer.setStatusMessage("📜 Kingdom loaded successfully!");
                    }
                    // Close the menu after successful load
                    clearActivePopup();
                } else {
                    if (renderTimer != null) {
                        renderTimer.setStatusMessage("❌ Failed to load kingdom!");
                    }
                }
            }
        });
        } catch (Exception e) {
            System.err.println("Error showing load dialog: " + e.getMessage());
            if (renderTimer != null) {
                renderTimer.setStatusMessage("❌ Could not open load dialog!");
            }
        }
    }
    
    // ===== POWER-UP SYSTEM UI =====
    
    /**
     * Activate the freeze effect power-up
     */
    private void activateFreezeEffect() {
        PowerUpType freezeType = PowerUpType.FREEZE_ENEMIES;
        
        if (!gameController.canUsePowerUp(freezeType)) {
            // Show why it can't be used
            String reason = "";
            if (gameController.getPowerUpManager().isOnCooldown(freezeType)) {
                int wavesRemaining = gameController.getPowerUpManager().getCooldownWavesRemaining(freezeType);
                reason = "Cooldown: " + wavesRemaining + " waves remaining";
            } else if (gameController.getPlayerGold() < freezeType.getCost()) {
                reason = "Need " + freezeType.getCost() + " gold (have " + gameController.getPlayerGold() + ")";
            } else if (gameController.getEnemies().isEmpty()) {
                reason = "No enemies to freeze";
            }
            
            renderTimer.setStatusMessage("❄️ Cannot freeze: " + reason);
            return;
        }
        
        // Activate the power-up
        boolean success = gameController.activatePowerUp(freezeType);
        if (success) {
            // EPIC ACTIVATION ANIMATION!
            createMagicalActivationEffect();
            renderTimer.setStatusMessage("🧙‍♂️✨ ALLMIGHTY WIZARD HAKAN HOCA ACTIVATED! ❄️🧊 All enemies frozen for 5 seconds! 🧊❄️");
        } else {
            renderTimer.setStatusMessage("❄️ Freeze failed to activate");
        }
        
        // Update button appearance
        updateFreezeButtonStyle();
    }
    
    /**
     * Create spectacular magical activation effect
     */
    private void createMagicalActivationEffect() {
        if (freezeButton == null) return;
        
        // Create multiple layered effects for epic activation
        
        // 1. Intense white flash
        javafx.scene.effect.DropShadow flashEffect = new javafx.scene.effect.DropShadow();
        flashEffect.setColor(javafx.scene.paint.Color.WHITE);
        flashEffect.setRadius(50);
        flashEffect.setSpread(1.0);
        
        // 2. Blue magical explosion
        javafx.scene.effect.DropShadow magicalExplosion = new javafx.scene.effect.DropShadow();
        magicalExplosion.setColor(javafx.scene.paint.Color.LIGHTBLUE);
        magicalExplosion.setRadius(40);
        magicalExplosion.setSpread(0.8);
        
        // 3. Cyan afterglow
        javafx.scene.effect.DropShadow afterglow = new javafx.scene.effect.DropShadow();
        afterglow.setColor(javafx.scene.paint.Color.CYAN);
        afterglow.setRadius(20);
        afterglow.setSpread(0.5);
        
        // Animation sequence
        javafx.animation.Timeline activationSequence = new javafx.animation.Timeline();
        
        // Phase 1: Intense flash (0-200ms)
        activationSequence.getKeyFrames().addAll(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, e -> {
                freezeButton.setEffect(flashEffect);
                freezeButton.setScaleX(1.3);
                freezeButton.setScaleY(1.3);
            }),
            
            // Phase 2: Magical explosion (200-600ms)
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> {
                freezeButton.setEffect(magicalExplosion);
                freezeButton.setScaleX(1.2);
                freezeButton.setScaleY(1.2);
            }),
            
            // Phase 3: Afterglow (600-1000ms)
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(600), e -> {
                freezeButton.setEffect(afterglow);
                freezeButton.setScaleX(1.1);
                freezeButton.setScaleY(1.1);
            }),
            
            // Phase 4: Return to normal (1000ms)
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(1000), e -> {
                // Restore original magical glow
                javafx.scene.effect.DropShadow normalGlow = new javafx.scene.effect.DropShadow();
                normalGlow.setColor(javafx.scene.paint.Color.CYAN);
                normalGlow.setRadius(15);
                normalGlow.setSpread(0.3);
                freezeButton.setEffect(normalGlow);
                freezeButton.setScaleX(1.0);
                freezeButton.setScaleY(1.0);
            })
        );
        
        activationSequence.play();
        
        // Add screen-wide magical effect (optional - creates a brief blue tint)
        if (uiOverlayPane != null) {
            javafx.scene.shape.Rectangle screenFlash = new javafx.scene.shape.Rectangle();
            screenFlash.setWidth(uiOverlayPane.getWidth());
            screenFlash.setHeight(uiOverlayPane.getHeight());
            screenFlash.setFill(javafx.scene.paint.Color.LIGHTBLUE);
            screenFlash.setOpacity(0.0);
            screenFlash.setMouseTransparent(true);
            
            uiOverlayPane.getChildren().add(screenFlash);
            
            // Flash animation
            javafx.animation.Timeline screenFlashAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, 
                    new javafx.animation.KeyValue(screenFlash.opacityProperty(), 0.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(100), 
                    new javafx.animation.KeyValue(screenFlash.opacityProperty(), 0.3)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), 
                    new javafx.animation.KeyValue(screenFlash.opacityProperty(), 0.0))
            );
            
            screenFlashAnimation.setOnFinished(e -> uiOverlayPane.getChildren().remove(screenFlash));
            screenFlashAnimation.play();
        }
    }
    
    /**
     * Add magical visual effects to the wizard button
     */
    private void addMagicalEffectsToWizardButton(Button wizardButton) {
        // Store the original tooltip to preserve it
        javafx.scene.control.Tooltip originalTooltip = wizardButton.getTooltip();
        
        // Create magical glow effect
        javafx.scene.effect.DropShadow magicalGlow = new javafx.scene.effect.DropShadow();
        magicalGlow.setColor(javafx.scene.paint.Color.CYAN);
        magicalGlow.setRadius(15);
        magicalGlow.setSpread(0.3);
        
        // Create pulsing animation for the glow
        javafx.animation.Timeline pulseAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, 
                new javafx.animation.KeyValue(magicalGlow.radiusProperty(), 10)),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), 
                new javafx.animation.KeyValue(magicalGlow.radiusProperty(), 20)),
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), 
                new javafx.animation.KeyValue(magicalGlow.radiusProperty(), 10))
        );
        pulseAnimation.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        
        // Store original event handlers to avoid conflicts
        javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> originalEntered = wizardButton.getOnMouseEntered();
        javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> originalExited = wizardButton.getOnMouseExited();
        javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> originalPressed = wizardButton.getOnMousePressed();
        javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> originalReleased = wizardButton.getOnMouseReleased();
        
        // Hover effects - enhance existing behavior
        wizardButton.setOnMouseEntered(e -> {
            // Call original handler first if it exists
            if (originalEntered != null) {
                originalEntered.handle(e);
            }
            
            if (!wizardButton.isDisabled()) {
                // Bright magical glow on hover
                magicalGlow.setColor(javafx.scene.paint.Color.LIGHTBLUE);
                magicalGlow.setRadius(25);
                wizardButton.setEffect(magicalGlow);
                pulseAnimation.play();
                
                // Scale up slightly
                javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(
                    javafx.util.Duration.millis(200), wizardButton);
                scaleUp.setToX(1.1);
                scaleUp.setToY(1.1);
                scaleUp.play();
            }
            
            // Ensure tooltip is preserved
            if (originalTooltip != null && wizardButton.getTooltip() == null) {
                wizardButton.setTooltip(originalTooltip);
            }
        });
        
        wizardButton.setOnMouseExited(e -> {
            // Call original handler first if it exists
            if (originalExited != null) {
                originalExited.handle(e);
            }
            
            // Return to normal
            pulseAnimation.stop();
            magicalGlow.setColor(javafx.scene.paint.Color.CYAN);
            magicalGlow.setRadius(15);
            
            javafx.animation.ScaleTransition scaleDown = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), wizardButton);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.play();
        });
        
        // Click effect
        wizardButton.setOnMousePressed(e -> {
            // Call original handler first if it exists
            if (originalPressed != null) {
                originalPressed.handle(e);
            }
            
            if (!wizardButton.isDisabled()) {
                // Intense flash effect
                javafx.scene.effect.DropShadow flashEffect = new javafx.scene.effect.DropShadow();
                flashEffect.setColor(javafx.scene.paint.Color.WHITE);
                flashEffect.setRadius(30);
                flashEffect.setSpread(0.8);
                wizardButton.setEffect(flashEffect);
                
                // Quick scale down
                javafx.animation.ScaleTransition clickScale = new javafx.animation.ScaleTransition(
                    javafx.util.Duration.millis(100), wizardButton);
                clickScale.setToX(0.95);
                clickScale.setToY(0.95);
                clickScale.play();
            }
        });
        
        wizardButton.setOnMouseReleased(e -> {
            // Call original handler first if it exists
            if (originalReleased != null) {
                originalReleased.handle(e);
            }
            
            // Return to hover state
            if (wizardButton.isHover() && !wizardButton.isDisabled()) {
                magicalGlow.setColor(javafx.scene.paint.Color.LIGHTBLUE);
                magicalGlow.setRadius(25);
                wizardButton.setEffect(magicalGlow);
            } else {
                wizardButton.setEffect(magicalGlow);
            }
            
            javafx.animation.ScaleTransition releaseScale = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(100), wizardButton);
            releaseScale.setToX(wizardButton.isHover() ? 1.1 : 1.0);
            releaseScale.setToY(wizardButton.isHover() ? 1.1 : 1.0);
            releaseScale.play();
        });
        
        // Set initial subtle glow
        wizardButton.setEffect(magicalGlow);
        
        // Ensure the original tooltip is preserved
        if (originalTooltip != null) {
            wizardButton.setTooltip(originalTooltip);
        }
    }
    
    /**
     * Update the freeze button's appearance based on availability
     */
    private void updateFreezeButtonStyle() {
        if (freezeButton == null) return;
        
        PowerUpType freezeType = PowerUpType.FREEZE_ENEMIES;
        boolean canUse = gameController.canUsePowerUp(freezeType);
        
        // Get the current magical glow effect
        javafx.scene.effect.DropShadow currentEffect = null;
        if (freezeButton.getEffect() instanceof javafx.scene.effect.DropShadow) {
            currentEffect = (javafx.scene.effect.DropShadow) freezeButton.getEffect();
        }
        
        if (canUse) {
            // Available - enhanced magical appearance with woody background
            freezeButton.setOpacity(1.0);
            freezeButton.setDisable(false);
            
            // Enhance the magical glow for available state - brighter to show through wood
            if (currentEffect != null) {
                currentEffect.setColor(javafx.scene.paint.Color.LIGHTBLUE);
                currentEffect.setRadius(20);
                currentEffect.setSpread(0.6);
            }
            
            // Update tooltip
            String tooltip = "🧙‍♂️ Allmighty Wizard Hakan Hoca\n" +
                           "✨ " + freezeType.getDisplayName() + "\n" +
                           "💰 Cost: " + freezeType.getCost() + " gold\n" +
                           "❄️ " + freezeType.getDescription();
            freezeButton.setTooltip(new javafx.scene.control.Tooltip(tooltip));
            
        } else {
            // Not available - dimmed appearance with darker wood
            freezeButton.setOpacity(0.6);
            freezeButton.setDisable(true);
            
            // Dim the magical glow and make wood darker
            if (currentEffect != null) {
                currentEffect.setColor(javafx.scene.paint.Color.DARKGRAY);
                currentEffect.setRadius(10);
                currentEffect.setSpread(0.2);
            }
            
            // Update tooltip with reason why it's not available
            String reason = "";
            if (gameController.getPowerUpManager().isOnCooldown(freezeType)) {
                int wavesRemaining = gameController.getPowerUpManager().getCooldownWavesRemaining(freezeType);
                reason = "⏳ Cooldown: " + wavesRemaining + " waves remaining";
            } else if (gameController.getPlayerGold() < freezeType.getCost()) {
                reason = "💰 Need " + freezeType.getCost() + " gold (have " + gameController.getPlayerGold() + ")";
            } else if (gameController.getEnemies().isEmpty()) {
                reason = "👻 No enemies to freeze";
            }
            
            String tooltip = "🧙‍♂️ Allmighty Wizard Hakan Hoca (UNAVAILABLE)\n" +
                           "✨ " + freezeType.getDisplayName() + "\n" +
                           "💰 Cost: " + freezeType.getCost() + " gold\n" +
                           "❌ " + reason;
            freezeButton.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        }
    }
}

package towerdefense.view.screens;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import towerdefense.Main; // For navigation
import towerdefense.controller.GameController; // Import controller
import towerdefense.model.GameModel; // If game state is needed
import towerdefense.model.GameMap; // Import GameMap
import towerdefense.model.TileType; // Import TileType

/**
 * Provides the JavaFX UI components for the Game screen.
 */
public class GameScreen {

    private BorderPane view;
    private Pane gameBoardPane; // Placeholder for game rendering area
    private Label waveLabel;
    private Label goldLabel;
    private Label livesLabel;
    // private GameModel model; // Controller likely manages model interaction
    private GameController controller; // Store the controller
    private final String buttonStyle = "-fx-background-color: #6f4f2f; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-size: 11px; -fx-background-radius: 3; -fx-border-color: #4a3b2a; -fx-border-width: 1; -fx-border-radius: 3;";
    private final String buttonHoverStyle = "-fx-background-color: #8a6e4b; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-size: 11px; -fx-background-radius: 3; -fx-border-color: #4a3b2a; -fx-border-width: 1; -fx-border-radius: 3;";
    private final String labelStyle = "-fx-font-family: 'Arial Black'; -fx-font-size: 13px; -fx-text-fill: #e4d8c4;"; // Light
                                                                                                                      // text
                                                                                                                      // for
                                                                                                                      // dark
                                                                                                                      // background
    private final double TILE_SIZE = 32.0; // Define a fixed tile size

    public GameScreen(GameController controller) {
        this.controller = controller;
        initializeUI();
        // Controller should be started externally (e.g., by Main after screen load)
    }

    private void initializeUI() {
        view = new BorderPane();
        view.setPadding(new Insets(10));
        // Darker background for the game screen
        view.setStyle("-fx-background-color: #5a4a3a;");

        // --- Game Board Area (Center) ---
        gameBoardPane = new Pane();
        gameBoardPane.setStyle("-fx-background-color: #5a4a3a;"); // Set background for area around map
        // Size will be determined by the map loaded

        view.setCenter(gameBoardPane);
        BorderPane.setMargin(gameBoardPane, new Insets(0, 10, 10, 0)); // Margin adjusted

        // --- Control Panel (Right) ---
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setAlignment(Pos.TOP_CENTER);
        // Dark wood panel style
        controlPanel.setStyle(
                "-fx-background-color: #6f4f2f; -fx-border-color: #4a3b2a; -fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5;");
        controlPanel.setPrefWidth(160); // Slightly narrower

        Label controlsTitle = new Label("Game Info");
        controlsTitle.setFont(Font.font("Arial Black", FontWeight.BOLD, 16));
        controlsTitle.setStyle("-fx-text-fill: #e4d8c4;");
        controlsTitle.setUnderline(true);

        // Apply label style
        waveLabel = new Label("Wave: 0/0");
        waveLabel.setStyle(labelStyle);
        goldLabel = new Label("Gold: 0");
        goldLabel.setStyle(labelStyle);
        livesLabel = new Label("Lives: 0");
        livesLabel.setStyle(labelStyle);

        // Style buttons
        Button pauseButton = createStyledButton("Pause");
        Button speedButton = createStyledButton("Speed x1");
        Button optionsButton = createStyledButton("Options");
        Button quitButton = createStyledButton("Quit");

        // Wire actions to controller
        pauseButton.setOnAction(e -> controller.handlePauseToggle());
        speedButton.setOnAction(e -> {
            controller.handleSpeedToggle();
            // TODO: Update speedButton text based on controller state
        });
        optionsButton.setOnAction(e -> Main.loadOptionsScreen()); // Navigate to options
        quitButton.setOnAction(e -> {
            controller.stopGame(); // Stop game loop before leaving
            Main.loadMainMenuScreen();
        });

        controlPanel.getChildren().addAll(
                controlsTitle, new Separator(), waveLabel, goldLabel, livesLabel,
                new Separator(),
                pauseButton, speedButton, optionsButton,
                new Separator(),
                quitButton);
        view.setRight(controlPanel);
        BorderPane.setMargin(controlPanel, new Insets(0, 0, 10, 0)); // Margin adjusted

        // --- Tower Selection Panel (Bottom) ---
        HBox towerPanel = new HBox(15);
        towerPanel.setPadding(new Insets(10));
        towerPanel.setAlignment(Pos.CENTER_LEFT);
        // Dark wood panel style
        towerPanel.setStyle(
                "-fx-background-color: #6f4f2f; -fx-border-color: #4a3b2a; -fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5;");

        Label towersLabel = new Label("Build Tower:");
        towersLabel.setStyle(labelStyle);
        towerPanel.getChildren().add(towersLabel);
        for (int i = 1; i <= 3; i++) {
            Button towerButton = createStyledButton("Tower " + i);
            towerButton.setPrefSize(90, 55); // Adjusted size
            final int towerType = i;
            towerButton.setOnAction(e -> controller.handleTowerSelection(towerType));
            towerPanel.getChildren().add(towerButton);
        }

        view.setBottom(towerPanel);

        // Initial UI update is now handled by controller/game loop
        // updateUI(); // Remove initial call from here
    }

    // Helper for styled buttons
    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle(buttonStyle);
        button.setOnMouseEntered(e -> button.setStyle(buttonHoverStyle));
        button.setOnMouseExited(e -> button.setStyle(buttonStyle));
        button.setPrefHeight(30);
        return button;
    }

    /**
     * Updates UI elements like labels (call this from controller or game loop).
     * Ensures update happens on JavaFX Application Thread.
     */
    public void updateInfoLabels(int currentWave, int totalWaves, int gold, int lives) {
        Platform.runLater(() -> {
            waveLabel.setText(String.format("Wave: %d/%d", currentWave, totalWaves));
            goldLabel.setText(String.format("Gold: %d", gold));
            livesLabel.setText(String.format("Lives: %d", lives));
        });
    }

    /**
     * Draws the static background map tiles based on the GameMap data.
     * Called by the controller when the view is set or the map changes.
     */
    public void drawMap(GameMap map) {
        if (map == null)
            return;
        Platform.runLater(() -> {
            gameBoardPane.getChildren().clear(); // Clear previous drawings
            int numRows = map.getHeight();
            int numCols = map.getWidth();

            // Adjust pane size based on map dimensions
            gameBoardPane.setPrefSize(numCols * TILE_SIZE, numRows * TILE_SIZE);
            gameBoardPane.setMaxSize(numCols * TILE_SIZE, numRows * TILE_SIZE);

            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    TileType type = map.getTile(r, c);
                    Node tileNode = createTileNode(type, r, c); // Create node for the tile
                    tileNode.setLayoutX(c * TILE_SIZE);
                    tileNode.setLayoutY(r * TILE_SIZE);
                    gameBoardPane.getChildren().add(tileNode);
                }
            }
            // TODO: Draw grid lines if needed for debugging
        });
    }

    /**
     * Creates a visual Node (e.g., colored Rectangle or ImageView) for a given tile
     * type.
     */
    private Node createTileNode(TileType type, int row, int col) {
        // For now, use colored rectangles. Later, replace with ImageViews.
        Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
        Text marker = null;
        Color color = Color.PINK; // Default/Error color

        switch (type) {
            case GRASS:
                color = Color.web("#90ee90");
                break; // Light Green
            case PATH:
                color = Color.web("#f5deb3");
                break; // Wheat
            case TOWER_SLOT:
                color = Color.web("#a9a9a9");
                break; // Dark Gray
            case START:
                color = Color.web("#f5deb3");
                marker = new Text("S");
                break;
            case END:
                color = Color.web("#f5deb3");
                marker = new Text("E");
                break;
            case DECOR_TREE:
                color = Color.DARKGREEN;
                break;
            case DECOR_ROCK:
                color = Color.GRAY;
                break;
        }
        rect.setFill(color);
        rect.setStroke(Color.web("#5a6870")); // Light border
        rect.setStrokeWidth(0.5);

        // If using markers (like S/E), stack them on the rectangle
        if (marker != null) {
            marker.setStyle("-fx-font-weight: bold;");
            // Center the text within the tile
            StackPane stack = new StackPane(rect, marker);
            return stack;
        } else {
            return rect;
        }
        // TODO: Add click listener here if needed for interaction (e.g., build tower)
    }

    /**
     * Placeholder method for redrawing dynamic elements (enemies, towers,
     * projectiles).
     * This should be called by the controller/game loop.
     */
    public void redrawGameBoard(/* Potentially pass dynamic game state */) {
        Platform.runLater(() -> {
            // TODO: Clear only dynamic elements (or redraw everything)
            // TODO: Draw enemies based on their current positions from model
            // TODO: Draw towers (potentially with animations/state)
            // TODO: Draw projectiles
            // System.out.println("View: Redrawing dynamic elements (placeholder)");
        });
    }

    /**
     * Returns the root node of the game screen UI.
     */
    public Parent getView() {
        if (view == null) {
            initializeUI();
        }
        return view;
    }
}
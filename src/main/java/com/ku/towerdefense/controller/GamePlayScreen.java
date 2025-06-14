package com.ku.towerdefense.controller;

import com.ku.towerdefense.model.map.GameMap;
import com.ku.towerdefense.ui.MainMenuScreen;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Screen for playing the tower defense game.
 */
public class GamePlayScreen extends BorderPane {

    private final Stage primaryStage;
    private GameController gameController;
    private Canvas gameCanvas;
    private AnimationTimer gameLoop;
    private Label goldLabel;
    private Label livesLabel;
    private Label waveLabel;
    private long lastUpdateTime;

    /**
     * Creates a new game play screen.
     *
     * @param primaryStage the primary stage
     * @param gameMap the game map to use
     */
    public GamePlayScreen(Stage primaryStage, GameMap gameMap) {
        this.primaryStage = primaryStage;
        this.gameController = new GameController(gameMap);
        initializeUI();
        setupGameController();
    }

    /**
     * Initialize the UI components.
     */
    private void initializeUI() {
        setStyle("-fx-background-color: #222222;");
        setPadding(new Insets(10));

        // Create the top panel with game info
        HBox topPanel = createTopPanel();
        setTop(topPanel);

        // Create the game canvas
        gameCanvas = new Canvas(800, 600);
        setCenter(gameCanvas);

        // Create the bottom panel with controls
        HBox bottomPanel = createBottomPanel();
        setBottom(bottomPanel);
    }

    /**
     * Create the top panel with game information.
     *
     * @return the top panel
     */
    private HBox createTopPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: #333333; -fx-border-color: #444444; -fx-border-width: 0 0 1 0;");

        goldLabel = new Label("Gold: 0");
        goldLabel.setTextFill(Color.GOLD);
        
        livesLabel = new Label("Lives: 0");
        livesLabel.setTextFill(Color.RED);
        
        waveLabel = new Label("Wave: 0");
        waveLabel.setTextFill(Color.WHITE);
        
        panel.getChildren().addAll(goldLabel, livesLabel, waveLabel);
        
        return panel;
    }

    /**
     * Create the bottom panel with game controls.
     *
     * @return the bottom panel
     */
    private HBox createBottomPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(10));
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: #333333; -fx-border-color: #444444; -fx-border-width: 1 0 0 0;");

        Button startWaveButton = new Button("Start Wave");
        startWaveButton.setOnAction(e -> gameController.startNextWave());
        
        Button saveButton = new Button("Save Game");
        saveButton.setOnAction(e -> saveGame());
        
        Button loadButton = new Button("Load Game");
        loadButton.setOnAction(e -> loadGame());
        
        Button mainMenuButton = new Button("Main Menu");
        mainMenuButton.setOnAction(e -> returnToMainMenu());
        
        panel.getChildren().addAll(startWaveButton, saveButton, loadButton, mainMenuButton);
        
        return panel;
    }

    /**
     * Set up the game controller and start the game loop.
     */
    private void setupGameController() {
        // Update the UI to reflect the controller's state
        updateUI();
        
        // Set up wave completion listener
        gameController.setOnWaveCompletedListener((waveNumber, goldBonus) -> 
            Platform.runLater(() -> {
                showAlert("Wave Completed", "Wave " + waveNumber + " completed!\nGold bonus: " + goldBonus);
                updateUI();
            })
        );
        
        // Start the game loop
        startGameLoop();
    }

    /**
     * Start the game animation loop.
     */
    private void startGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        lastUpdateTime = System.nanoTime();
        
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Update game
                // Note: game controller already handles its own update in its game loop
                
                // Render game
                GraphicsContext gc = gameCanvas.getGraphicsContext2D();
                gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
                gameController.render(gc);
                
                // Update UI
                if (now - lastUpdateTime > 1_000_000_000) { // Update UI once per second
                    lastUpdateTime = now;
                    updateUI();
                }
                
                // Check for game over
                if (gameController.isGameOver()) {
                    stop();
                    showGameOverScreen();
                }
            }
        };
        
        gameLoop.start();
        gameController.startGame();
    }
    
    /**
     * Update the UI to reflect the current game state.
     */
    private void updateUI() {
        Platform.runLater(() -> {
            goldLabel.setText("Gold: " + gameController.getPlayerGold());
            livesLabel.setText("Lives: " + gameController.getPlayerLives());
            waveLabel.setText("Wave: " + gameController.getCurrentWave());
        });
    }
    
    /**
     * Show the game over screen.
     */
    private void showGameOverScreen() {
        Platform.runLater(() -> {
            showAlert("Game Over", "Game over! You survived " + gameController.getCurrentWave() + " waves.");
            returnToMainMenu();
        });
    }

    /**
     * Return to the main menu.
     */
    private void returnToMainMenu() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        gameController.stopGame();
        
        MainMenuScreen mainMenu = new MainMenuScreen(primaryStage);
        Scene scene = new Scene(mainMenu, 800, 600);
        primaryStage.setScene(scene);
    }
    
    /**
     * Save the current game state to a file.
     */
    private void saveGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Game");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Save Files", "*.save"));
        
        // Pause the game while saving
        boolean wasRunning = false;
        if (gameLoop != null) {
            gameLoop.stop();
            wasRunning = true;
        }
        
        File selectedFile = fileChooser.showSaveDialog(getScene().getWindow());
        if (selectedFile != null) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(selectedFile))) {
                oos.writeObject(gameController);
                showAlert("Game Saved", "Game saved successfully to " + selectedFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to save game: " + e.getMessage());
            }
        }
        
        // Resume the game if it was running before
        if (wasRunning) {
            gameLoop.start();
        }
    }

    /**
     * Load a game from a file.
     */
    private void loadGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Game");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Save Files", "*.save"));
        
        File selectedFile = fileChooser.showOpenDialog(getScene().getWindow());
        if (selectedFile != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(selectedFile))) {
                GameController loadedController = (GameController) ois.readObject();
                
                // Reinitialize the controller to fix graphical issues with entities
                loadedController.reinitializeAfterLoad();
                
                gameController = loadedController;
                
                // Update UI references to the loaded controller
                setupGameController();
                
                // Start the game animation
                startGameLoop();
                
                showAlert("Game Loaded", "Game loaded successfully from " + selectedFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error", "Failed to load game: " + e.getMessage());
            }
        }
    }
    
    /**
     * Show an alert dialog with enhanced styling and proper fullscreen handling.
     * 
     * @param title the title of the alert
     * @param message the message to display
     */
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            try {
                Alert.AlertType type = Alert.AlertType.INFORMATION;
                if (title.toLowerCase().contains("error") || title.toLowerCase().contains("failed")) {
                    type = Alert.AlertType.ERROR;
                } else if (title.toLowerCase().contains("success") || title.toLowerCase().contains("saved")
                        || title.toLowerCase().contains("loaded")) {
                    type = Alert.AlertType.INFORMATION;
                } else if (title.toLowerCase().contains("game over")) {
                    type = Alert.AlertType.WARNING;
                }

                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);

                // Apply styling to match editor style
                try {
                    String cssPath = getClass().getResource("/css/style.css").toExternalForm();
                    alert.getDialogPane().getStylesheets().add(cssPath);
                    alert.getDialogPane().getStyleClass().add("dialog-pane");
                } catch (Exception e) {
                    System.err.println("Could not load CSS for gameplay dialog: " + e.getMessage());
                }

                // Add type-specific styling
                if (alert.getAlertType() == Alert.AlertType.ERROR) {
                    alert.getDialogPane().getStyleClass().add("error-dialog");
                } else if (alert.getAlertType() == Alert.AlertType.WARNING) {
                    alert.getDialogPane().getStyleClass().add("warning-dialog");
                } else if (alert.getAlertType() == Alert.AlertType.INFORMATION) {
                    alert.getDialogPane().getStyleClass().add("info-dialog");
                }

                // CRITICAL: Ensure dialog shows on top WITHOUT exiting fullscreen during gameplay
                alert.initOwner(primaryStage);
                alert.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                
                // Prevent fullscreen exit by keeping dialog centered and properly sized
                alert.setResizable(false);
                alert.getDialogPane().setPrefWidth(400);
                alert.getDialogPane().setMinWidth(400);
                alert.getDialogPane().setPrefHeight(200);
                
                // Show in center of parent window, not system desktop
                alert.showAndWait();
                
            } catch (Exception e) {
                // Fallback if alert fails - log to console instead of crashing
                System.err.println("Gameplay Alert Error - " + title + ": " + message);
                System.err.println("Exception showing gameplay alert: " + e.getMessage());
                e.printStackTrace();
                
                // Try a minimal system dialog as last resort
                try {
                    Alert fallbackAlert = new Alert(Alert.AlertType.INFORMATION);
                    fallbackAlert.setTitle("Game Alert");
                    fallbackAlert.setContentText(title + ": " + message);
                    fallbackAlert.initOwner(primaryStage);
                    fallbackAlert.setResizable(false);
                    fallbackAlert.showAndWait();
                } catch (Exception ex) {
                    // If even that fails, just log it
                    System.err.println("Complete gameplay dialog failure: " + ex.getMessage());
                }
            }
        });
    }
} 
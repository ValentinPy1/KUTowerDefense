package com.ku.towerdefense.ui;

import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

// Additional imports for load game functionality
import com.ku.towerdefense.service.GameSaveService;
import com.ku.towerdefense.controller.GameController;
import com.ku.towerdefense.model.map.GameMap;
import java.util.List;
import java.util.Optional;

/**
 * The main menu screen for the KU Tower Defense game.
 */
public class MainMenuScreen extends VBox {
    private final Stage primaryStage;

    /**
     * Constructor for the main menu.
     *
     * @param primaryStage the primary stage
     */
    public MainMenuScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Make the window properly resizable with minimum dimensions
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        initializeUI();

        // Bind the size of this VBox (MainMenuScreen) to the size of the Scene it's
        // placed in.
        // This ensures the VBox fills the scene, allowing its background to cover the
        // whole area.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                prefWidthProperty().bind(newScene.widthProperty());
                prefHeightProperty().bind(newScene.heightProperty());
            }
        });
    }

    /**
     * Initialize the user interface components for the main menu.
     */
    private void initializeUI() {
        // Set spacing and alignment
        setSpacing(15);
        setAlignment(Pos.CENTER);
        // setStyle("-fx-background-color: transparent;"); // Let CSS handle the
        // background entirely
        getStyleClass().add("main-menu-layout"); // Add style class for CSS targeting

        // Attempt to prevent dragging the window by its content
        this.setOnMousePressed(event -> {
            if (event.getTarget() == this) {
                // System.out.println("MainMenuScreen VBox pressed, consuming event to prevent
                // potential drag.");
                event.consume();
            }
        });

        // Game title
        Text gameTitle = new Text("KU Tower Defense");
        gameTitle.getStyleClass().add("menu-title");
        getChildren().add(gameTitle); // Add the title directly

        // Create menu buttons
        Button newGameButton = createMenuButton("New Game", this::startNewGame);
        Button loadGameButton = createMenuButton("Load Game", this::loadGame);
        Button mapEditorButton = createMenuButton("Map Editor", this::openMapEditor);
        Button optionsButton = createMenuButton("Options", this::openOptions);
        Button quitButton = createMenuButton("Quit", this::quitGame);

        // Add buttons to layout
        getChildren().addAll(newGameButton, loadGameButton, mapEditorButton, optionsButton, quitButton);
    }

    /**
     * Helper method to create consistently styled menu buttons.
     * 
     * @param text   button text
     * @param action action to perform when clicked
     * @return styled button
     */
    private Button createMenuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setOnAction(e -> action.run());
        return button;
    }

    /**
     * Helper method to transition to a new scene with a fade effect while maintaining fullscreen.
     * 
     * @param newScene The scene to transition to.
     */
    private void transitionToScene(Scene newScene) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            try {
                String css = getClass().getResource("/css/style.css").toExternalForm();
                newScene.getStylesheets().add(css);
            } catch (NullPointerException | IllegalArgumentException e) {
                System.err.println("Could not load stylesheet /css/style.css for the new scene: " + e.getMessage());
            }

            ImageCursor customCursor = UIAssets.getCustomCursor();
            if (customCursor != null) {
                newScene.setCursor(customCursor);
            }

            // IMPROVED FIX: Use Platform.runLater to ensure smooth fullscreen transition
            javafx.application.Platform.runLater(() -> {
                // Temporarily disable fullscreen exit hint to prevent flashing
                String originalHint = primaryStage.getFullScreenExitHint();
                primaryStage.setFullScreenExitHint("");
                
                // Set scene without fullscreen first (this prevents the flash)
                primaryStage.setFullScreen(false);
                primaryStage.setScene(newScene);
                
                // Enforce custom cursor on the new scene and start periodic enforcement
                UIAssets.enforceCustomCursor(newScene);
                UIAssets.startCursorEnforcement(newScene);
                
                // Immediately re-enable fullscreen in the next frame
                javafx.application.Platform.runLater(() -> {
                    primaryStage.setFullScreen(true);
                    primaryStage.setFullScreenExitHint(originalHint);
                });
            });

            if (newScene.getRoot() != null) {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newScene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            }
        });
        fadeOut.play();
    }

    /**
     * Action to start a new game.
     */
    private void startNewGame() {
        MapSelectionScreen mapSelection = new MapSelectionScreen(primaryStage);
        // Use screen dimensions to match fullscreen size
        double w = javafx.stage.Screen.getPrimary().getBounds().getWidth();
        double h = javafx.stage.Screen.getPrimary().getBounds().getHeight();
        Scene mapSelectionScene = new Scene(mapSelection, w, h);
        transitionToScene(mapSelectionScene);
    }

    /**
     * Action to open the map editor.
     */
    private void openMapEditor() {
        MapEditorScreen mapEditor = new MapEditorScreen(primaryStage);
        // Use screen dimensions to match fullscreen size
        double w = javafx.stage.Screen.getPrimary().getBounds().getWidth();
        double h = javafx.stage.Screen.getPrimary().getBounds().getHeight();
        Scene mapEditorScene = new Scene(mapEditor, w, h);
        transitionToScene(mapEditorScene);
    }

    /**
     * Action to open the options screen.
     */
    private void openOptions() {
        OptionsScreen options = new OptionsScreen(primaryStage);
        // Use screen dimensions to match fullscreen size
        double w = javafx.stage.Screen.getPrimary().getBounds().getWidth();
        double h = javafx.stage.Screen.getPrimary().getBounds().getHeight();
        Scene optionsScene = new Scene(options, w, h);
        transitionToScene(optionsScene);
    }

    /**
     * Action to load a saved game.
     */
    private void loadGame() {
        try {
            GameSaveService saveService = GameSaveService.getInstance();
            List<GameSaveService.SaveFileInfo> saves = saveService.getAvailableSaves();
        
            if (saves.isEmpty()) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("📜 Royal Archives");
                alert.setHeaderText("No Saved Kingdoms Found");
                alert.setContentText("The royal archives are empty. Start a new game and save your progress first!");
                
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
            
            Optional<GameSaveService.SaveFileInfo> result = dialog.showAndWait();
            result.ifPresent(saveInfo -> {
                if (saveInfo != null && saveInfo.isValid) {
                    try {
                        // Create a temporary map with default dimensions (will be overridden by save data)
                        GameMap tempMap = new GameMap("LoadedMap", 20, 15);
                        
                        // Create a new game controller with the temporary map
                        GameController gameController = new GameController(tempMap);
                        boolean success = saveService.loadGame(gameController, saveInfo.filename);
                        
                        if (success) {
                            // Transition to the game screen with the loaded game
                            GameScreen gameScreen = new GameScreen(primaryStage, gameController);
                            double w = javafx.stage.Screen.getPrimary().getBounds().getWidth();
                            double h = javafx.stage.Screen.getPrimary().getBounds().getHeight();
                            Scene gameScene = new Scene(gameScreen, w, h);
                            transitionToScene(gameScene);
                        } else {
                            // Show error dialog
                            javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            errorAlert.setTitle("❌ Load Failed");
                            errorAlert.setHeaderText("Failed to Load Kingdom");
                            errorAlert.setContentText("The selected save file could not be loaded. It may be corrupted or incompatible.");
                            
                            // Apply medieval styling
                            errorAlert.getDialogPane().setStyle(
                                "-fx-background-color: linear-gradient(to bottom, #F5DEB3, #DEB887);" +
                                "-fx-border-color: #8B4513; -fx-border-width: 3px;"
                            );
                            
                            errorAlert.showAndWait();
                        }
                    } catch (Exception e) {
                        System.err.println("Error creating game controller for load: " + e.getMessage());
                        
                        // Show error dialog
                        javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        errorAlert.setTitle("❌ Load Error");
                        errorAlert.setHeaderText("Could Not Load Kingdom");
                        errorAlert.setContentText("An error occurred while loading the game: " + e.getMessage());
                        
                        // Apply medieval styling
                        errorAlert.getDialogPane().setStyle(
                            "-fx-background-color: linear-gradient(to bottom, #F5DEB3, #DEB887);" +
                            "-fx-border-color: #8B4513; -fx-border-width: 3px;"
                        );
                        
                        errorAlert.showAndWait();
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error showing load dialog: " + e.getMessage());
            
            // Show error dialog
            javafx.scene.control.Alert errorAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            errorAlert.setTitle("❌ Load Error");
            errorAlert.setHeaderText("Could Not Open Load Dialog");
            errorAlert.setContentText("An error occurred while trying to load saved games: " + e.getMessage());
            
            // Apply medieval styling
            errorAlert.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #F5DEB3, #DEB887);" +
                "-fx-border-color: #8B4513; -fx-border-width: 3px;"
            );
            
            errorAlert.showAndWait();
        }
    }

    /**
     * Action to quit the game.
     */
    private void quitGame() {
        // Optional: Add a fade-out before closing
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> primaryStage.close());
        fadeOut.play();
        // primaryStage.close(); // Original direct close
    }
}
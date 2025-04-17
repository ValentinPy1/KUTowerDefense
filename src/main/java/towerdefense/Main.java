package towerdefense;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import towerdefense.controller.MapSelectionController;
import towerdefense.model.GameModel;
import towerdefense.view.screens.MainMenuScreen;
import towerdefense.view.screens.OptionsScreen;
import towerdefense.view.screens.MapEditorScreen;
import towerdefense.view.screens.GameScreen;
// Import necessary classes
import java.io.IOException;
import java.net.URL;
import towerdefense.controller.GameController;
import towerdefense.controller.MapEditorController;
import towerdefense.controller.OptionsController;

/**
 * Main entry point for the Tower Defense game.
 * Manages the primary stage and scene transitions.
 */
public class Main extends Application {

    private static Stage primaryStage;
    private static GameModel gameModel;
    private static Scene primaryScene; // Keep a reference to the scene
    private static GameController currentGameController; // Keep track of active game controller

    @Override
    public void start(Stage primaryStage) {
        Main.primaryStage = primaryStage;
        Main.gameModel = new GameModel(); // Initialize the model once
        loadMainMenuScreen(); // Load initial screen
        primaryStage.setTitle("KU Tower Defense");
        primaryStage.setOnCloseRequest(e -> {
            if (currentGameController != null) {
                currentGameController.stopGame(); // Ensure game loop stops on close
            }
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void loadMainMenuScreen() {
        if (currentGameController != null) {
            currentGameController.stopGame(); // Stop game if returning to menu
            currentGameController = null;
        }
        try {
            MainMenuScreen mainMenuProvider = new MainMenuScreen(gameModel);
            Parent mainMenuRoot = mainMenuProvider.getView();
            setSceneRoot(mainMenuRoot, "Tower Defense - Main Menu");
        } catch (Exception e) {
            System.err.println("Failed to load Main Menu screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadMapSelectionScreen() {
        try {
            URL fxmlLocation = Main.class.getResource("/fxml/MapSelectionScreen.fxml");
            if (fxmlLocation == null) {
                System.err.println("Cannot find FXML file: /fxml/MapSelectionScreen.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent mapSelectionRoot = loader.load();
            MapSelectionController controller = loader.getController();
            controller.initialize(gameModel, null); // Pass model
            setSceneRoot(mapSelectionRoot, "Tower Defense - Select Map");
        } catch (IOException e) {
            System.err.println("Failed to load MapSelectionScreen FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadGameScreen(String selectedMap) {
        try {
            System.out.println("Loading JavaFX GameScreen with map: " + selectedMap);
            // Create Controller first
            currentGameController = new GameController(gameModel, selectedMap);
            // Create Screen, passing the controller
            GameScreen gameProvider = new GameScreen(currentGameController);
            // Give controller reference to the view
            currentGameController.setView(gameProvider);
            // Get the root node AFTER view might have been updated by controller.setView()
            Parent gameRoot = gameProvider.getView();
            setSceneRoot(gameRoot, "Tower Defense - Game");
            // Start the game loop AFTER the scene is set and shown
            currentGameController.startGame();
        } catch (Exception e) {
            System.err.println("Failed to load Game screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadOptionsScreen() {
        try {
            // Create Controller
            OptionsController optionsController = new OptionsController(gameModel);
            // Create Screen, passing controller
            OptionsScreen optionsProvider = new OptionsScreen(optionsController);
            Parent optionsRoot = optionsProvider.getView();
            setSceneRoot(optionsRoot, "Tower Defense - Options");
        } catch (Exception e) {
            System.err.println("Failed to load Options screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadMapEditorScreen() {
        try {
            // Create Controller
            MapEditorController editorController = new MapEditorController(gameModel);
            // Create Screen, passing controller
            MapEditorScreen editorProvider = new MapEditorScreen(editorController);
            Parent editorRoot = editorProvider.getView();
            setSceneRoot(editorRoot, "Tower Defense - Map Editor");
        } catch (Exception e) {
            System.err.println("Failed to load Map Editor screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Helper to set Scene Root --- //
    private static void setSceneRoot(Parent rootNode, String title) {
        if (primaryScene == null) {
            // Determine initial size based on the first screen loaded, or use defaults
            double width = (rootNode.getBoundsInLocal().getWidth() > 0) ? rootNode.getBoundsInLocal().getWidth() : 800;
            double height = (rootNode.getBoundsInLocal().getHeight() > 0) ? rootNode.getBoundsInLocal().getHeight()
                    : 600;
            primaryScene = new Scene(rootNode, width, height);
            primaryStage.setScene(primaryScene);
        } else {
            primaryScene.setRoot(rootNode);
        }
        primaryStage.setTitle(title);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
        // Consider requesting focus on the root node
        rootNode.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
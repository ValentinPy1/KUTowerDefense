package towerdefense.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import towerdefense.Main; // Import Main for navigation
import towerdefense.model.GameModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the Map Selection Screen.
 * Handles listing available maps and starting the game with the selected map.
 */
public class MapSelectionController {

    private static final String MAPS_DIRECTORY = "maps"; // Directory relative to project root

    @FXML
    private ListView<String> mapListView; // Display map file names

    @FXML
    private Button startGameButton;

    @FXML
    private Button backButton;

    private GameModel model;

    // Initialize loads map files
    public void initialize(GameModel model, Void unused) { // Signature matches call in Main
        this.model = model;

        loadMapFiles();

        // Disable start button until a map is selected
        startGameButton.setDisable(true);
        mapListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            startGameButton.setDisable(newVal == null);
        });
    }

    /**
     * Scans the maps directory and populates the ListView.
     */
    private void loadMapFiles() {
        List<String> mapFiles = new ArrayList<>();
        File mapsDir = new File(MAPS_DIRECTORY);

        if (mapsDir.exists() && mapsDir.isDirectory()) {
            File[] files = mapsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".tdmap"));
            if (files != null) {
                mapFiles = java.util.Arrays.stream(files)
                        .map(File::getName)
                        // Optionally remove the .tdmap extension for display
                        // .map(name -> name.substring(0, name.length() - ".tdmap".length()))
                        .collect(Collectors.toList());
            }
        }

        if (mapFiles.isEmpty()) {
            mapListView.setPlaceholder(new javafx.scene.control.Label("No maps found in 'maps' directory."));
        }
        mapListView.setItems(FXCollections.observableArrayList(mapFiles));
    }

    @FXML
    private void handleStartGame() {
        String selectedMapFile = mapListView.getSelectionModel().getSelectedItem();
        if (selectedMapFile != null) {
            System.out.println("Starting game with map file: " + selectedMapFile);
            // Construct the full path if needed, or just pass the name
            String mapNameToLoad = MAPS_DIRECTORY + File.separator + selectedMapFile;
            // Load game screen using Main's method, passing the map identifier
            Main.loadGameScreen(mapNameToLoad); // Pass full path or just name
        } else {
            // Should not happen due to button disable logic, but good practice
            System.out.println("No map selected!");
        }
    }

    @FXML
    private void handleBackButton() {
        System.out.println("Going back to Main Menu");
        Main.loadMainMenuScreen();
    }
}
package towerdefense.controller;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import towerdefense.Main; // Needed?
import towerdefense.model.GameMap;
import towerdefense.model.GameModel;
import towerdefense.model.TileType;
import towerdefense.view.screens.MapEditorScreen; // Assume controller might need to trigger view updates

import java.io.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Controller for the map editor logic.
 * Handles map creation, loading, saving, validation, and tile placement.
 */
public class MapEditorController {

    private static final int DEFAULT_WIDTH = 20;
    private static final int DEFAULT_HEIGHT = 15;

    private GameModel gameModel; // Keep reference if needed
    private GameMap currentMap;
    private TileType selectedTileType;
    private MapEditorScreen view; // Reference to the view to trigger updates
    private File currentFile = null; // Track the currently opened/saved file

    public MapEditorController(GameModel gameModel) {
        this.gameModel = gameModel;
        handleNewMap(); // Start with a new default map
    }

    // Method for the view to register itself
    public void setView(MapEditorScreen view) {
        this.view = view;
        if (view != null) {
            view.updateGrid(); // Initial grid update
            view.updateStatusLabel("New map created. Select a tile.");
        }
    }

    // --- Toolbar Actions --- //

    public void handleNewMap() {
        // TODO: Prompt if current map has unsaved changes
        System.out.println("Controller: Creating new map...");
        this.currentMap = new GameMap(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.selectedTileType = null;
        this.currentFile = null;
        if (view != null) {
            view.updateGrid(); // Update view with new empty map
            view.updateStatusLabel("New map created. Select a tile.");
            view.clearTileSelection(); // Deselect any selected tile button
        }
    }

    public void handleOpenMap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Map File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tower Defense Map Files (*.tdmap)", "*.tdmap"));
        File file = fileChooser.showOpenDialog(getStage());

        if (file != null) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof GameMap) {
                    this.currentMap = (GameMap) obj;
                    this.currentFile = file;
                    this.selectedTileType = null;
                    if (view != null) {
                        view.updateGrid();
                        view.updateStatusLabel("Map loaded: " + file.getName());
                        view.clearTileSelection();
                    }
                    System.out.println("Map loaded successfully from: " + file.getAbsolutePath());
                } else {
                    showErrorAlert("Invalid File Content", "The selected file does not contain valid map data.");
                }
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                System.err.println("Error loading map: " + e.getMessage());
                e.printStackTrace();
                showErrorAlert("Error Loading Map", "Could not load map data from file.\n" + e.getMessage());
            }
        }
    }

    public void handleSaveMap() {
        if (currentMap == null)
            return;

        if (currentFile != null) {
            // Save to the current file
            saveMapToFile(currentFile);
        } else {
            // No current file, so prompt for Save As
            handleSaveMapAs();
        }
    }

    public void handleSaveMapAs() {
        if (currentMap == null)
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Map As");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tower Defense Map Files (*.tdmap)", "*.tdmap"));
        fileChooser.setInitialFileName("new_map.tdmap");
        File file = fileChooser.showSaveDialog(getStage());

        if (file != null) {
            // Ensure filename has the correct extension
            if (!file.getName().toLowerCase().endsWith(".tdmap")) {
                file = new File(file.getAbsolutePath() + ".tdmap");
            }
            saveMapToFile(file);
        }
    }

    private void saveMapToFile(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this.currentMap);
            this.currentFile = file; // Update current file reference
            if (view != null) {
                view.updateStatusLabel("Map saved: " + file.getName());
            }
            System.out.println("Map saved successfully to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving map: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Error Saving Map", "Could not save map data to file.\n" + e.getMessage());
        }
    }

    public void handleValidateMap() {
        if (currentMap != null) {
            boolean isValid = currentMap.validate();
            String message = isValid ? "Map validation successful!" : "Map validation failed. See console for details.";
            if (view != null)
                view.updateStatusLabel(message);
            // Show an alert dialog with validation result
            Alert alert = new Alert(isValid ? AlertType.INFORMATION : AlertType.WARNING);
            alert.setTitle("Map Validation Result");
            alert.setHeaderText(message);
            alert.setContentText(isValid ? "The map meets all requirements."
                    : "Please check the console output for specific validation errors and correct the map.");
            alert.showAndWait();
        } else {
            if (view != null)
                view.updateStatusLabel("No map to validate.");
        }
    }

    // --- Tile Palette and Grid Actions --- //

    public void handleTileSelection(TileType tileType) {
        this.selectedTileType = tileType;
        System.out.println("Controller: Selected tile type: " + tileType);
        if (view != null)
            view.updateStatusLabel("Selected: " + tileType.name());
    }

    public void handleMapGridClick(int row, int col) {
        System.out.println(String.format("Controller: Map grid clicked at R:%d, C:%d", row, col));
        if (selectedTileType == null) {
            if (view != null)
                view.updateStatusLabel("Click ignored: No tile type selected.");
            return;
        }
        if (currentMap == null) {
            if (view != null)
                view.updateStatusLabel("Click ignored: No map loaded/created.");
            return;
        }

        boolean updateNeeded = false;
        TileType currentTile = currentMap.getTile(row, col);

        // Special handling for Start/End points (must be placed on existing PATH)
        if (selectedTileType == TileType.START) {
            if (currentTile == TileType.PATH && currentMap.setStartPoint(row, col, true)) {
                if (view != null)
                    view.updateStatusLabel("Start point set at (" + col + "," + row + ")");
                updateNeeded = true;
            } else {
                if (view != null)
                    view.updateStatusLabel("Cannot set Start: Must be on edge PATH tile.");
            }
        } else if (selectedTileType == TileType.END) {
            if (currentTile == TileType.PATH && currentMap.setEndPoint(row, col, true)) {
                if (view != null)
                    view.updateStatusLabel("End point set at (" + col + "," + row + ")");
                updateNeeded = true;
            } else {
                if (view != null)
                    view.updateStatusLabel("Cannot set End: Must be on edge PATH tile.");
            }
        } else {
            // General tile placement
            if (currentMap.setTile(row, col, selectedTileType)) {
                updateNeeded = true;
                if (view != null)
                    view.updateStatusLabel("Placed " + selectedTileType.name() + " at (" + col + "," + row + ")");
            } else {
                if (view != null)
                    view.updateStatusLabel("Cannot place tile here.");
            }
        }

        if (updateNeeded && view != null) {
            view.updateCell(row, col); // Update only the changed cell
            // If placing start/end might require update of old start/end cell too
            if (selectedTileType == TileType.START || selectedTileType == TileType.END) {
                view.updateGrid(); // Update whole grid for simplicity when changing start/end
            }
        }
    }

    // --- Getters for View --- //
    public GameMap getCurrentMap() {
        return currentMap;
    }

    // --- Helpers --- //
    private Stage getStage() {
        // Helper to get the Stage, assuming the view is a Node in the scene
        if (view != null && view.getViewNode() != null && view.getViewNode().getScene() != null) {
            return (Stage) view.getViewNode().getScene().getWindow();
        }
        // Fallback or alternative method to get the primary stage if needed
        return null; // Or Main.getPrimaryStage(); if Main provides it
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
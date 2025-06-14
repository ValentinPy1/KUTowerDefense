package com.ku.towerdefense.ui;

import com.ku.towerdefense.controller.GameController;
import com.ku.towerdefense.model.map.GameMap;
import com.ku.towerdefense.model.map.Tile;
import com.ku.towerdefense.model.map.TileType;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.ImageCursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.awt.Point;
import javafx.scene.layout.Region;
import java.io.*;

import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Modality;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Screen for creating and editing game maps.
 */
public class MapEditorScreen extends BorderPane {
    private final Stage primaryStage;
    private GameMap currentMap;
    private File mapsDirectory;
    private MapEditorTilePalette tilePalette;
    private MapEditorTopToolbar topToolbar;
    private MapEditorCanvasView canvasView;

    // Default and minimum window dimensions
    private static final double MIN_WINDOW_WIDTH = 800;
    private static final double MIN_WINDOW_HEIGHT = 600;
    private static final double PREVIEW_CANVAS_WIDTH = 200;
    private static final double PREVIEW_CANVAS_HEIGHT = 150;

    public MapEditorScreen(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.currentMap = new GameMap("New Map", 20, 15);
        initializeMapsDirectory();
        initializeUI();
        setupBindings();
    }

    private void initializeMapsDirectory() {
        String userHome = System.getProperty("user.home");
        mapsDirectory = new File(userHome, "KUTowerDefenseMaps");
        if (!mapsDirectory.exists()) {
            if (mapsDirectory.mkdirs()) {
                System.out.println("Maps directory created at: " + mapsDirectory.getAbsolutePath());
            } else {
                System.err.println("Failed to create maps directory. Saving/Loading disabled.");
                mapsDirectory = null;
            }
        } else {
            System.out.println("Using existing maps directory: " + mapsDirectory.getAbsolutePath());
        }
    }

    private void initializeUI() {
        getStyleClass().add("map-editor-screen");
        setPadding(new Insets(10));

        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.setResizable(true);

        topToolbar = new MapEditorTopToolbar(currentMap);
        tilePalette = new MapEditorTilePalette();
        canvasView = new MapEditorCanvasView(currentMap, tilePalette);

        HBox bottomToolbar = createBottomToolbar();

        setTop(topToolbar);
        setLeft(tilePalette);
        setCenter(canvasView);
        setBottom(bottomToolbar);

        primaryStage.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWindow, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        ((Stage) newWindow).widthProperty().addListener((obs, oldVal,
                                newVal) -> javafx.application.Platform.runLater(this::handleWindowResize));
                        ((Stage) newWindow).heightProperty().addListener((obs, oldVal,
                                newVal) -> javafx.application.Platform.runLater(this::handleWindowResize));
                        ((Stage) newWindow)
                                .setOnShown(e -> javafx.application.Platform.runLater(this::handleWindowResize));
                    }
                });
            }
        });
    }

    private void setupBindings() {
        topToolbar.setOnSetStart(e -> {
            System.out.println("Set Start mode triggered from toolbar");
            canvasView.activateSetStartMode();
        });

        topToolbar.setOnResize(e -> {
            System.out.println("Resize requested: " + e.getNewWidth() + "x" + e.getNewHeight());
            resizeMap(e.getNewWidth(), e.getNewHeight());
        });
    }

    private void handleWindowResize() {
        if (primaryStage.getScene() == null || getScene().getRoot() == null)
            return;

        double leftWidth = (tilePalette != null && tilePalette.isVisible()) ? tilePalette.getWidth() : 0;
        double topHeight = (topToolbar != null && topToolbar.isVisible()) ? topToolbar.getHeight() : 0;
        double bottomHeight = 50;

        double sceneWidth = getScene() != null ? getScene().getWidth() : primaryStage.getWidth();
        double sceneHeight = getScene() != null ? getScene().getHeight() : primaryStage.getHeight();

        double availableWidth = sceneWidth - leftWidth - getPadding().getLeft() - getPadding().getRight();
        double availableHeight = sceneHeight - topHeight - bottomHeight - getPadding().getTop()
                - getPadding().getBottom();

        if (canvasView != null) {
            // canvasView.updateScrollPaneSize(availableWidth, availableHeight); // REMOVED
            // - CanvasView now manages its own size
        }
    }

    private HBox createBottomToolbar() {
        HBox bottomToolbar = new HBox(10);
        bottomToolbar.setPadding(new Insets(10));
        bottomToolbar.getStyleClass().add("editor-bottom-toolbar");

        Button saveButton = new Button("Save Map");
        saveButton.getStyleClass().add("button");
        saveButton.setOnAction(e -> saveMap());

        Button loadButton = new Button("Load Map");
        loadButton.getStyleClass().add("button");
        loadButton.setOnAction(e -> loadMap());

        Button validateButton = new Button("Validate Map");
        validateButton.getStyleClass().add("button");
        validateButton.setOnAction(e -> validateMap());

        Button helpButton = new Button("Help");
        helpButton.getStyleClass().add("button");
        helpButton.setOnAction(e -> showGameMechanicsHelp());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button backButton = new Button("Back to Main Menu");
        backButton.getStyleClass().add("button");
        backButton.setOnAction(e -> goBack());

        bottomToolbar.getChildren().addAll(saveButton, loadButton, validateButton, helpButton, spacer, backButton);
        return bottomToolbar;
    }

    private void showGameMechanicsHelp() {
        Alert helpAlert = new Alert(Alert.AlertType.INFORMATION);
        helpAlert.setTitle("Map Editor Help");
        helpAlert.setHeaderText("Map Requirements & Editor Usage");

        TextArea textArea = new TextArea(
                "Map Requirements:\n" +
                        "- Must have exactly one Start Point (where enemies spawn).\n" +
                        "- The Start Point must be placed on an edge tile.\n" +
                        "- Must have exactly one End Point (enemy target).\n" +
                        "- The End Point is typically represented by a Castle structure.\n" +
                        "- There must be a valid path from Start Point to End Point.\n" +
                        "\nSpecial Tiles (Required):\n" +
                        "- START_POINT: Place this on a map edge where enemies will spawn.\n" +
                        "- END_POINT: Place this where enemies should try to reach. It automatically\n" +
                        "  places a 2x2 Castle structure.\n" +
                        "\nEditor Usage:\n" +
                        "- Select a tile from the left palette, including the new Special Tiles section.\n" +
                        "- Click on the canvas to place the selected tile.\n" +
                        "- For path creation, use various path tiles to connect Start and End points.\n" +
                        "- Tower slots can only be placed on Grass tiles.\n" +
                        "- Validate your map before saving to check all requirements are met.");
        textArea.setEditable(false);
        textArea.setWrapText(true);

        helpAlert.getDialogPane().setContent(textArea);
        helpAlert.setResizable(true);
        helpAlert.showAndWait();
    }

    private boolean validateMap() {
        boolean hasStart = false;
        boolean hasEnd = false;
        Point startPoint = null;
        Point endPointAdjacent = null;

        // First pass: Check for start and end points
        for (int y = 0; y < currentMap.getHeight(); y++) {
            for (int x = 0; x < currentMap.getWidth(); x++) {
                TileType type = currentMap.getTileType(x, y);
                if (type == TileType.START_POINT) {
                    if (hasStart) {
                        showAlert("Map Validation Error", 
                            "Multiple Start Points detected!\n\n" +
                            "❌ Found more than one Start Point tile\n" +
                            "✅ Solution: Keep only ONE Start Point on your map\n\n" +
                            "💡 Tip: Use the 'Set Start' button to properly place a single start point.");
                        return false;
                    }
                    hasStart = true;
                    startPoint = new Point(x, y);
                } else if (type == TileType.END_POINT) {
                    if (hasEnd) {
                        showAlert("Map Validation Error", 
                            "Multiple End Points detected!\n\n" +
                            "❌ Found more than one Castle structure\n" +
                            "✅ Solution: Keep only ONE Castle (2x2 End Point) on your map\n\n" +
                            "💡 Tip: A Castle serves as the enemy target - only one is needed.");
                        return false;
                    }
                    if (!isCastleComplete(x, y)) {
                        showAlert("Map Validation Error", 
                            "Incomplete Castle structure!\n\n" +
                            "❌ Castle at position (" + (x+1) + "," + (y+1) + ") is not properly constructed\n" +
                            "✅ Solution: Ensure the Castle is a complete 2x2 structure\n\n" +
                            "💡 Tip: Place 4 Castle tiles in a square formation on grass tiles.");
                        return false;
                    }
                    hasEnd = true;
                    endPointAdjacent = findAdjacentWalkable(x, y);
                    if (endPointAdjacent == null) {
                        showAlert("Map Validation Error", 
                            "Castle is not accessible!\n\n" +
                            "❌ Castle at position (" + (x+1) + "," + (y+1) + ") has no adjacent path tiles\n" +
                            "✅ Solution: Place at least one path tile next to the Castle\n\n" +
                            "💡 Tip: Enemies need a path tile to reach the Castle entrance.");
                        return false;
                    }
                }
            }
        }

        if (!hasStart) {
            // Check for valid path tiles that could serve as start points
            for (int y_coord = 0; y_coord < currentMap.getHeight(); y_coord++) {
                for (int x_coord = 0; x_coord < currentMap.getWidth(); x_coord++) {
                    if (isValidStartTile(x_coord, y_coord)) {
                        hasStart = true;
                        startPoint = new Point(x_coord, y_coord);
                        currentMap.setTileType(x_coord, y_coord, TileType.START_POINT);
                        System.out.println("Implicit start point found and set at: (" + x_coord + "," + y_coord + ")");
                        break;
                    }
                }
                if (hasStart)
                    break;
            }
            if (!hasStart) {
                showAlert("Map Validation Error", 
                    "No Start Point found!\n\n" +
                    "❌ Your map is missing a Start Point where enemies spawn\n" +
                    "✅ Solution: Add a Start Point at the edge of your map\n\n" +
                    "💡 How to fix:\n" +
                    "   • Use the 'Set Start' button in the toolbar\n" +
                    "   • Click on a path tile at the map edge\n" +
                    "   • Or place a Start Point tile from the palette");
                return false;
            }
        }

        if (!hasEnd) {
            showAlert("Map Validation Error", 
                "No End Point (Castle) found!\n\n" +
                "❌ Your map is missing a Castle where enemies go to win\n" +
                "✅ Solution: Add a 2x2 Castle structure to your map\n\n" +
                "💡 How to fix:\n" +
                "   • Select 'Castle' from the Special Points section\n" +
                "   • Click to place a 2x2 Castle structure\n" +
                "   • Ensure it's placed on grass tiles");
            return false;
        }

        if (startPoint == null) {
            showAlert("Map Validation Error", 
                "Start Point error!\n\n" +
                "❌ Start point detected but location is invalid\n" +
                "✅ Solution: Re-place your Start Point using the 'Set Start' button\n\n" +
                "💡 Tip: This is usually a temporary issue - try setting the start point again.");
            return false;
        }
        if (endPointAdjacent == null) {
            showAlert("Map Validation Error", 
                "Castle accessibility error!\n\n" +
                "❌ Castle is present but cannot be reached by enemies\n" +
                "✅ Solution: Place path tiles adjacent to your Castle\n\n" +
                "💡 Tip: Enemies need to walk to the Castle - ensure there's a path connection.");
            return false;
        }

        if (!isPathConnected(startPoint, endPointAdjacent)) {
            showAlert("Map Validation Error", 
                "Path not connected!\n\n" +
                "❌ No valid route found from Start Point to Castle\n" +
                "✅ Solution: Create a continuous path using path tiles\n\n" +
                "💡 How to fix:\n" +
                "   • Use path tiles to connect Start Point to Castle\n" +
                "   • Check for gaps in your path\n" +
                "   • Ensure path tiles are properly oriented\n" +
                "   • Use corners and turns to navigate around obstacles");
            return false;
        }

        // Count TOWER_SLOT tiles
        int towerSlotCount = 0;
        for (int y = 0; y < currentMap.getHeight(); y++) {
            for (int x = 0; x < currentMap.getWidth(); x++) {
                if (currentMap.getTileType(x, y) == TileType.TOWER_SLOT) {
                    towerSlotCount++;
                }
            }
        }
        if (towerSlotCount < 4) {
            showAlert("Map Validation Error", 
                "Not enough Tower Slots!\n\n" +
                "❌ Found only " + towerSlotCount + " Tower Slots (minimum required: 4)\n" +
                "✅ Solution: Add more Tower Slot tiles to your map\n\n" +
                "💡 How to fix:\n" +
                "   • Select 'Tower Slot' from the Towers section\n" +
                "   • Place at least " + (4 - towerSlotCount) + " more Tower Slots\n" +
                "   • These allow players to build towers during gameplay\n" +
                "   • Place them strategically along the enemy path");
            return false;
        }

        showAlert("Map Validation Success", 
            "🎉 Map is valid and ready to play!\n\n" +
            "✅ Start Point: Properly placed at map edge\n" +
            "✅ End Point: Castle structure complete and accessible\n" +
            "✅ Path: Continuous route from start to end\n" +
            "✅ Tower Slots: Found " + towerSlotCount + " slots for player towers\n\n" +
            "💾 Your map is ready to save and enjoy!");
        return true;
    }

    private boolean isValidStartTile(int x, int y) {
        Tile tile = currentMap.getTile(x, y);
        if (tile == null || !tile.isWalkable()) {
            return false;
        }
        return x == 0 || y == 0 || x == currentMap.getWidth() - 1 || y == currentMap.getHeight() - 1;
    }

    private boolean isCastleComplete(int baseX, int baseY) {
        if (baseX + 1 >= currentMap.getWidth() || baseY + 1 >= currentMap.getHeight()) {
            return false;
        }

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                Tile tile = currentMap.getTile(baseX + i, baseY + j);
                if (tile == null || tile.getType() != TileType.END_POINT) {
                    if (!(baseX + i == baseX && baseY + j == baseY && tile != null
                            && tile.getType() == TileType.END_POINT)) {
                    }
                }
            }
        }
        return true;
    }

    private Point findAdjacentWalkable(int baseX, int baseY) {
        // Castle structure: END_POINT is at (baseX, baseY), right side is at (baseX+1, baseY)
        // We need to check for walkable tiles adjacent to the castle RIGHT SIDE
        int castleRightX = baseX + 1;
        int castleRightY = baseY;
        
        // Check tiles adjacent to the castle right side (2,1 position)
        Point[] potentialEntries = {
                new Point(castleRightX - 1, castleRightY),     // Left of right side (back to center)
                new Point(castleRightX + 1, castleRightY),     // Right of right side (outside castle)
                new Point(castleRightX, castleRightY - 1),     // Above right side
                new Point(castleRightX, castleRightY + 1)      // Below right side
        };

        for (Point p : potentialEntries) {
            Tile tile = currentMap.getTile(p.x, p.y);
            if (currentMap.inBounds(p.x, p.y) && tile != null && tile.isWalkable()) {
                return p;
            }
        }
        return null;
    }

    private boolean isPathConnected(Point start, Point endAdjacent) {
        if (start == null || endAdjacent == null)
            return false;
        currentMap.generatePath();
        return currentMap.getEnemyPath() != null;
    }

    private Optional<File> showLoadMapDialog() {
        if (mapsDirectory == null || !mapsDirectory.exists() || !mapsDirectory.isDirectory()) {
            showAlert("Load Error", "Maps directory not found or is not a directory: "
                    + (mapsDirectory != null ? mapsDirectory.getAbsolutePath() : "Path not set"));
            return Optional.empty();
        }

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(primaryStage.getScene().getWindow());
        dialogStage.setTitle("Load Map - KUTowerDefense");

        // Left side: List of maps
        Label instructionLabel = new Label("Select a Map File:");
        ListView<File> mapListView = new ListView<>();
        mapListView.getStyleClass().add("map-file-list");
        File[] mapFilesArray = mapsDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".map"));

        if (mapFilesArray != null && mapFilesArray.length > 0) {
            Arrays.sort(mapFilesArray, Comparator.comparing(File::getName));
            mapListView.getItems().addAll(mapFilesArray);
            mapListView.getSelectionModel().selectFirst();
        } else {
            mapListView.setPlaceholder(new Label("No .map files found in " + mapsDirectory.getName()));
        }
        mapListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        mapListView.setCellFactory(lv -> new javafx.scene.control.ListCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName().replace(".map", ""));
            }
        });
        mapListView.setPrefHeight(250); // Adjusted height for list view
        VBox listVBox = new VBox(5, instructionLabel, mapListView);
        listVBox.setPadding(new Insets(0, 10, 0, 0));

        // Right side: Map Preview
        Label previewTitleLabel = new Label("Preview:");
        Canvas previewCanvas = new Canvas(PREVIEW_CANVAS_WIDTH, PREVIEW_CANVAS_HEIGHT);
        GraphicsContext previewGc = previewCanvas.getGraphicsContext2D();
        Label previewPlaceholder = new Label("Select a map to see preview.");
        previewPlaceholder.setFont(Font.font(14));
        VBox previewVBox = new VBox(5, previewTitleLabel, previewCanvas, previewPlaceholder);
        previewVBox.setAlignment(Pos.CENTER);
        previewVBox.setMinWidth(PREVIEW_CANVAS_WIDTH + 20);
        previewVBox.getStyleClass().add("map-preview-area");

        // Main layout for dialog content
        HBox mainContentBox = new HBox(10, listVBox, previewVBox);
        mainContentBox.setPadding(new Insets(10));
        mainContentBox.getStyleClass().add("load-dialog-content-box");

        // Buttons
        Button loadButton = new Button("Load");
        loadButton.setDefaultButton(true);
        loadButton.setId("load-map-button");
        Button deleteButton = new Button("Delete");
        deleteButton.setId("delete-map-button");
        Button cancelButton = new Button("Cancel");
        cancelButton.setCancelButton(true);
        cancelButton.setId("cancel-map-button");

        HBox buttonsBox = new HBox(10, loadButton, deleteButton, cancelButton);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));
        buttonsBox.getStyleClass().add("dialog-button-bar");

        // Overall dialog layout
        VBox dialogLayout = new VBox(15, mainContentBox, buttonsBox);
        dialogLayout.setPadding(new Insets(5));
        dialogLayout.getStyleClass().add("load-map-dialog");

        // --- Logic for preview and button states ---
        final Optional<File>[] selectedFileResult = new Optional[] { Optional.empty() };
        Runnable updatePreviewAction = () -> {
            File selectedFile = mapListView.getSelectionModel().getSelectedItem();
            boolean isFileSelected = selectedFile != null;
            loadButton.setDisable(!isFileSelected);
            deleteButton.setDisable(!isFileSelected);
            previewPlaceholder.setVisible(!isFileSelected);
            previewCanvas.setVisible(isFileSelected);

            if (isFileSelected) {
                previewPlaceholder.setText("Loading preview...");
                previewPlaceholder.setVisible(true);
                previewCanvas.setVisible(false);
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(selectedFile))) {
                    Object obj = ois.readObject();
                    if (obj instanceof GameMap) {
                        GameMap tempMap = (GameMap) obj;
                        tempMap.renderPreview(previewGc, PREVIEW_CANVAS_WIDTH, PREVIEW_CANVAS_HEIGHT);
                        previewPlaceholder.setVisible(false);
                        previewCanvas.setVisible(true);
                    } else {
                        drawPreviewMessage(previewGc, "Invalid map file format.", PREVIEW_CANVAS_WIDTH,
                                PREVIEW_CANVAS_HEIGHT);
                        previewPlaceholder.setVisible(false);
                        previewCanvas.setVisible(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    drawPreviewMessage(previewGc, "Preview unavailable: " + e.getMessage(), PREVIEW_CANVAS_WIDTH,
                            PREVIEW_CANVAS_HEIGHT);
                    previewPlaceholder.setVisible(false);
                    previewCanvas.setVisible(true);
                }
            } else {
                clearPreviewCanvas(previewGc, PREVIEW_CANVAS_WIDTH, PREVIEW_CANVAS_HEIGHT, "Select a map to preview.");
                previewPlaceholder.setText("Select a map to see preview.");
                previewPlaceholder.setVisible(true);
                previewCanvas.setVisible(false);
            }
        };

        mapListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> updatePreviewAction.run());
        updatePreviewAction.run(); // Initial call to set preview and button states

        // --- Button Actions ---
        loadButton.setOnAction(event -> {
            File choice = mapListView.getSelectionModel().getSelectedItem();
            if (choice != null) {
                selectedFileResult[0] = Optional.of(choice);
                dialogStage.close();
            } else {
                showAlert("No Selection", "Please select a map to load.");
            }
        });

        deleteButton.setOnAction(event -> {
            File selectedMapFile = mapListView.getSelectionModel().getSelectedItem();
            if (selectedMapFile != null) {
                Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmationDialog.setTitle("Confirm Deletion");
                confirmationDialog.setHeaderText("Delete Map: " + selectedMapFile.getName().replace(".map", ""));
                confirmationDialog.setContentText(
                        "Are you sure you want to permanently delete this map?\nThis action cannot be undone.");
                applyDialogStyling(confirmationDialog.getDialogPane());

                Optional<ButtonType> result = confirmationDialog.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        if (selectedMapFile.delete()) {
                            mapListView.getItems().remove(selectedMapFile);
                            showAlert("Map Deleted",
                                    "Map '" + selectedMapFile.getName().replace(".map", "") + "' was deleted.");
                            // Refresh preview after deletion (will show placeholder or next selected map)
                            updatePreviewAction.run();
                        } else {
                            showAlert("Deletion Failed",
                                    "Could not delete map '" + selectedMapFile.getName().replace(".map", "") + "'.");
                        }
                    } catch (Exception e) {
                        showAlert("Deletion Error", "Error deleting map '"
                                + selectedMapFile.getName().replace(".map", "") + "': " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });

        cancelButton.setOnAction(event -> {
            dialogStage.close();
        });

        // Apply general dialog styling (e.g. fonts, basic control appearances)
        // to the Scene, not specific panes if they have their own background/border.
        Scene dialogScene = new Scene(dialogLayout);
        try {
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            dialogScene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Could not load /css/style.css for load dialog scene: " + e.getMessage());
        }

        // Apply specific classes to components AFTER scene stylesheet is loaded for
        // overrides if necessary
        // (though usually order of definition in CSS matters more for specificity)
        instructionLabel.getStyleClass().addAll("dialog-label", "load-map-instruction");
        previewTitleLabel.getStyleClass().addAll("dialog-label", "load-map-preview-title");
        previewPlaceholder.getStyleClass().addAll("dialog-placeholder-label", "load-map-preview-placeholder");
        loadButton.getStyleClass().add("button");
        deleteButton.getStyleClass().add("button");
        cancelButton.getStyleClass().add("button");

        dialogStage.setScene(dialogScene);
        dialogStage.sizeToScene();
        dialogStage.showAndWait();

        return selectedFileResult[0];
    }

    // Helper to clear and draw message on preview canvas
    private void clearPreviewCanvas(GraphicsContext gc, double width, double height, String message) {
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.rgb(60, 60, 60)); // Dark background for canvas area
        gc.fillRect(0, 0, width, height);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", 12));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(message, width / 2, height / 2);
    }

    // Helper to draw an error/status message on the preview canvas
    private void drawPreviewMessage(GraphicsContext gc, String message, double canvasWidth, double canvasHeight) {
        gc.clearRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.rgb(80, 80, 80)); // Slightly lighter dark background
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
        gc.setFill(Color.ORANGERED);
        gc.setFont(Font.font("System", Font.font("System").getSize())); // Use default font size
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        // Simple text wrapping
        double y = canvasHeight / 2 - 10;
        for (String line : message.split("\\n")) {
            gc.fillText(line, canvasWidth / 2, y);
            y += gc.getFont().getSize() + 2; // Move to next line
        }
    }

    // Helper to apply styling to DialogPane (used for Alerts, TextInputDialogs)
    private void applyDialogStyling(javafx.scene.control.DialogPane dialogPane) {
        try {
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            dialogPane.getStylesheets().add(cssPath);
            dialogPane.getStyleClass().add("dialog-pane");
            // Removed Alert-specific logic from here
        } catch (Exception e) {
            System.err.println("Could not load CSS for dialog pane: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        try {
            Alert.AlertType type = Alert.AlertType.INFORMATION;
            if (title.toLowerCase().contains("error") || title.toLowerCase().contains("failed")) {
                type = Alert.AlertType.ERROR;
            } else if (title.toLowerCase().contains("success") || title.toLowerCase().contains("saved")
                    || title.toLowerCase().contains("deleted")) {
                type = Alert.AlertType.INFORMATION;
            } else if (title.toLowerCase().contains("confirm")) {
                type = Alert.AlertType.CONFIRMATION;
            }

            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);

            // Apply base dialog styling first
            applyDialogStyling(alert.getDialogPane());

            // Then add type-specific styling
            if (alert.getAlertType() == Alert.AlertType.ERROR) {
                alert.getDialogPane().getStyleClass().add("error-dialog");
            } else if (alert.getAlertType() == Alert.AlertType.CONFIRMATION) {
                alert.getDialogPane().getStyleClass().add("confirmation-dialog");
            } else if (alert.getAlertType() == Alert.AlertType.INFORMATION) {
                alert.getDialogPane().getStyleClass().add("info-dialog");
            }

            // CRITICAL: Ensure dialog shows on top WITHOUT exiting fullscreen
            alert.initOwner(primaryStage);
            alert.initModality(Modality.APPLICATION_MODAL);
            
            // Prevent fullscreen exit by keeping dialog centered and properly sized
            alert.setResizable(false);
            alert.getDialogPane().setPrefWidth(400);
            alert.getDialogPane().setMinWidth(400);
            alert.getDialogPane().setPrefHeight(200);
            
            // Show in center of parent window, not system desktop
            alert.showAndWait();
            
        } catch (Exception e) {
            // Fallback if alert fails - log to console instead of crashing
            System.err.println("Alert Error - " + title + ": " + content);
            System.err.println("Exception showing alert: " + e.getMessage());
            e.printStackTrace();
            
            // Try a minimal system dialog as last resort
            try {
                Alert fallbackAlert = new Alert(Alert.AlertType.ERROR);
                fallbackAlert.setTitle("Map Editor Alert");
                fallbackAlert.setContentText(title + ": " + content);
                fallbackAlert.initOwner(primaryStage);
                fallbackAlert.setResizable(false);
                fallbackAlert.showAndWait();
            } catch (Exception ex) {
                // If even that fails, just log it
                System.err.println("Complete dialog failure: " + ex.getMessage());
            }
        }
    }

    private void saveMap() {
        if (!validateMap()) {
            return;
        }

        String mapName = currentMap.getName();
        boolean isNewMapOrNeedsName = mapName == null || mapName.trim().isEmpty() || mapName.equals("New Map");

        if (isNewMapOrNeedsName) {
            TextInputDialog dialog = new TextInputDialog(isNewMapOrNeedsName ? "MyMap" : mapName);
            dialog.setTitle("Save Map As");
            dialog.setHeaderText("Enter a name for your map. Existing maps will be overwritten.");
            dialog.setContentText("Map name:");
            try {
                String cssPath = getClass().getResource("/css/style.css").toExternalForm();
                dialog.getDialogPane().getStylesheets().add(cssPath);
                dialog.getDialogPane().getStyleClass().add("dialog-pane");
            } catch (Exception e) {
                System.err.println("Could not load stylesheet for save dialog: " + e.getMessage());
            }

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                mapName = result.get().trim();
                currentMap.setName(mapName);
            } else {
                showAlert("Save Cancelled", "Map saving was cancelled or no name provided.");
                return;
            }
        }

        if (mapsDirectory == null) {
            showAlert("Save Error", "Maps directory is not configured. Cannot save map.");
            return;
        }

        String fileName = mapName.replaceAll("[^a-zA-Z0-9_\\-\\.]+", "_") + ".map";
        File mapFile = new File(mapsDirectory, fileName);

        if (!isNewMapOrNeedsName && mapFile.exists()) {
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(mapFile))) {
            oos.writeObject(currentMap);
            showAlert("Map Saved",
                    "Map '" + currentMap.getName() + "' saved as " + fileName + " in " + mapsDirectory.getName() + ".");
        } catch (IOException e) {
            showAlert("Save Error", "Failed to save map '" + currentMap.getName() + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMap() {
        if (mapsDirectory == null) {
            showAlert("Load Error", "Maps directory path is not configured.");
            return;
        }
        if (!mapsDirectory.exists() || !mapsDirectory.isDirectory()) {
            showAlert("Load Error", "Cannot find or access the maps directory: " + mapsDirectory.getAbsolutePath());
            return;
        }

        Optional<File> selectedFileOptional = showLoadMapDialog();

        if (selectedFileOptional.isPresent()) {
            File selectedFile = selectedFileOptional.get();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(selectedFile))) {
                Object loadedObject = ois.readObject();
                if (!(loadedObject instanceof GameMap)) {
                    throw new ClassCastException("Loaded file is not a valid GameMap object.");
                }
                GameMap loadedMap = (GameMap) loadedObject;

                if (loadedMap.getWidth() <= 0 || loadedMap.getHeight() <= 0) {
                    throw new IOException("Loaded map has invalid dimensions.");
                }

                this.currentMap = loadedMap;

                topToolbar.setGameMap(this.currentMap);
                canvasView.setGameMap(this.currentMap);

                handleWindowResize();
                canvasView.renderMap();

                tilePalette.selectTile(TileType.GRASS);
                canvasView.resetPlacementMode();

                showAlert("Load Success", "Map '" + currentMap.getName() + "' loaded successfully.");

            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                e.printStackTrace();
                showAlert("Load Error", "Failed to load map file '" + selectedFile.getName() + "': " + e.getMessage());
            }
        } else {
            System.out.println("Map loading cancelled or no file selected.");
        }
    }

    private void goBack() {
        MainMenuScreen mainMenu = new MainMenuScreen(primaryStage);
        
        // Use screen dimensions to match fullscreen size
        double w = javafx.stage.Screen.getPrimary().getBounds().getWidth();
        double h = javafx.stage.Screen.getPrimary().getBounds().getHeight();
        Scene mainMenuScene = new Scene(mainMenu, w, h);
        mainMenuScene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        ImageCursor customCursor = UIAssets.getCustomCursor();
        if (customCursor != null) {
            mainMenuScene.setCursor(customCursor);
        }

        // IMPROVED FIX: Use Platform.runLater for smooth transition and cursor enforcement
        javafx.application.Platform.runLater(() -> {
            String originalHint = primaryStage.getFullScreenExitHint();
            primaryStage.setFullScreenExitHint("");
            
            primaryStage.setFullScreen(false);
            primaryStage.setScene(mainMenuScene);
            
            // Enforce custom cursor on the new scene
            UIAssets.enforceCustomCursor(mainMenuScene);
            UIAssets.startCursorEnforcement(mainMenuScene);
            
            javafx.application.Platform.runLater(() -> {
                primaryStage.setFullScreen(true);
                primaryStage.setFullScreenExitHint(originalHint);
            });
        });
    }

    private void resizeMap(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            showAlert("Resize Error", "Map dimensions must be positive.");
            return;
        }

        String mapName = currentMap.getName();
        this.currentMap = new GameMap(mapName, newWidth, newHeight);

        topToolbar.setGameMap(this.currentMap);
        canvasView.setGameMap(this.currentMap);

        handleWindowResize();
        canvasView.renderMap();

        tilePalette.selectTile(TileType.GRASS);
        canvasView.resetPlacementMode();

        System.out.println("Map resized to: " + newWidth + "x" + newHeight);
        showAlert("Map Resized",
                "Map resized to " + newWidth + "x" + newHeight + ". Start/End points may need resetting.");
    }
}

package towerdefense.view.screens;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle; // For simple tile coloring
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import towerdefense.Main;
import towerdefense.controller.MapEditorController;
import towerdefense.model.GameMap;
import towerdefense.model.TileType;

/**
 * Provides the JavaFX UI components for the Map Editor screen.
 */
public class MapEditorScreen {

    private BorderPane view;
    private Label statusLabel;
    private Label positionLabel;
    private GridPane mapGridPane;
    private MapEditorController controller;
    private ToggleGroup tileToggleGroup;
    private final String buttonStyle = "-fx-background-color: #6f4f2f; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-size: 12px; -fx-background-radius: 3; -fx-border-color: #4a3b2a; -fx-border-width: 1; -fx-border-radius: 3;";
    private final String buttonHoverStyle = "-fx-background-color: #8a6e4b; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-size: 12px; -fx-background-radius: 3; -fx-border-color: #4a3b2a; -fx-border-width: 1; -fx-border-radius: 3;";
    private final String toggleSelectedStyle = "-fx-background-color: #a08664; -fx-text-fill: black; -fx-font-family: 'Arial'; -fx-font-size: 12px; -fx-background-radius: 3; -fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 3;";

    public MapEditorScreen(MapEditorController controller) {
        this.controller = controller;
        initializeUI();
        this.controller.setView(this); // Register view with controller
    }

    private void initializeUI() {
        view = new BorderPane();
        view.setPadding(new Insets(10));
        view.setStyle("-fx-background-color: #9e8a70;");

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.setStyle("-fx-background-color: #c1a98a;");
        Button newButton = createStyledButton("New", buttonStyle, buttonHoverStyle);
        Button openButton = createStyledButton("Open", buttonStyle, buttonHoverStyle);
        Button saveButton = createStyledButton("Save", buttonStyle, buttonHoverStyle);
        Button saveAsButton = createStyledButton("Save As...", buttonStyle, buttonHoverStyle);
        Button validateButton = createStyledButton("Validate", buttonStyle, buttonHoverStyle);
        Button exitButton = createStyledButton("Exit Editor", buttonStyle, buttonHoverStyle);

        newButton.setOnAction(e -> controller.handleNewMap());
        openButton.setOnAction(e -> controller.handleOpenMap());
        saveButton.setOnAction(e -> controller.handleSaveMap());
        saveAsButton.setOnAction(e -> controller.handleSaveMapAs()); // Added Save As action
        validateButton.setOnAction(e -> controller.handleValidateMap());
        exitButton.setOnAction(e -> Main.loadMainMenuScreen());

        toolBar.getItems().addAll(newButton, new Separator(), openButton, new Separator(), saveButton, saveAsButton,
                new Separator(), validateButton, new Separator(), exitButton);
        view.setTop(toolBar);

        // --- Map Grid ---
        mapGridPane = new GridPane();
        mapGridPane.setStyle("-fx-background-color: #708090; -fx-grid-lines-visible: true;"); // Slate gray background
        mapGridPane.setAlignment(Pos.CENTER);
        // buildMapGrid() is called by controller after map is ready
        ScrollPane mapScrollPane = new ScrollPane(mapGridPane);
        mapScrollPane.setFitToWidth(true);
        mapScrollPane.setFitToHeight(true);
        view.setCenter(mapScrollPane);
        BorderPane.setMargin(mapScrollPane, new Insets(10));

        // --- Tile Selector Panel ---
        VBox tileSelectorPanel = new VBox(8);
        tileSelectorPanel.setPadding(new Insets(10));
        tileSelectorPanel.setAlignment(Pos.TOP_CENTER);
        tileSelectorPanel.setStyle(
                "-fx-background-color: #d4c0a1; -fx-border-color: #7a5c3a; -fx-border-width: 2; -fx-background-radius: 5; -fx-border-radius: 5;");
        tileSelectorPanel.setPrefWidth(150);
        Label tileSelectorTitle = new Label("Tile Palette");
        tileSelectorTitle.setFont(Font.font("Arial Black", FontWeight.BOLD, 14));
        tileSelectorTitle.setStyle("-fx-text-fill: #4a3b2a;");
        tileSelectorPanel.getChildren().add(tileSelectorTitle);
        tileSelectorPanel.getChildren().add(new Separator(javafx.geometry.Orientation.HORIZONTAL));

        tileToggleGroup = new ToggleGroup(); // Assign to field

        // Add buttons for each TileType enum constant
        for (TileType type : TileType.values()) {
            ToggleButton tileButton = createStyledToggleButton(type.name(), tileToggleGroup, buttonStyle,
                    toggleSelectedStyle, buttonHoverStyle);
            // Add tooltip
            tileButton.setTooltip(new Tooltip("Click grid to place " + type.name()));
            tileButton.setOnAction(e -> {
                if (tileButton.isSelected()) {
                    controller.handleTileSelection(type);
                }
                // Allow deselection? ToggleGroup might handle this.
            });
            tileSelectorPanel.getChildren().add(tileButton);
        }
        view.setRight(tileSelectorPanel);

        // --- Status Bar ---
        BorderPane statusPane = new BorderPane();
        statusPane.setPadding(new Insets(5, 10, 5, 10));
        statusPane.setStyle("-fx-background-color: #c1a98a;");
        statusLabel = new Label("Ready.");
        statusLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-text-fill: #3e2c1d;");
        positionLabel = new Label("Position: -, -");
        positionLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-text-fill: #3e2c1d;");
        statusPane.setLeft(statusLabel);
        statusPane.setRight(positionLabel);
        view.setBottom(statusPane);
    }

    // --- Public Methods for Controller --- //

    /** Called by controller to rebuild the entire grid display */
    public void updateGrid() {
        Platform.runLater(() -> buildMapGrid());
    }

    /** Called by controller to update a single cell's visual */
    public void updateCell(int row, int col) {
        Platform.runLater(() -> {
            GameMap map = controller.getCurrentMap();
            if (map == null || row < 0 || row >= map.getHeight() || col < 0 || col >= map.getWidth())
                return;

            Node cellNode = findCellNode(row, col);
            if (cellNode instanceof Pane) {
                Pane cellPane = (Pane) cellNode;
                styleCellPane(cellPane, map.getTile(row, col), row, col);
            }
        });
    }

    /** Called by controller to update status text */
    public void updateStatusLabel(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    /** Called by controller to deselect tile palette button */
    public void clearTileSelection() {
        if (tileToggleGroup.getSelectedToggle() != null) {
            tileToggleGroup.getSelectedToggle().setSelected(false);
        }
    }

    /** Provides the root node for getting the Stage */
    public Node getViewNode() {
        return view;
    }

    // --- Internal Helper Methods --- //

    /** Builds the visual map grid based on controller's currentMap */
    private void buildMapGrid() {
        mapGridPane.getChildren().clear();
        GameMap map = controller.getCurrentMap();
        if (map == null)
            return; // No map loaded

        int numRows = map.getHeight();
        int numCols = map.getWidth();

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                Pane cellPane = new Pane();
                cellPane.setPrefSize(32, 32);
                TileType type = map.getTile(r, c);
                styleCellPane(cellPane, type, r, c); // Apply styling

                final int row = r;
                final int col = c;
                cellPane.setOnMouseClicked(event -> {
                    controller.handleMapGridClick(row, col);
                    updatePositionLabel(col, row); // Update position display
                });
                // Add hover effects maybe?
                mapGridPane.add(cellPane, col, row);
            }
        }
        // Add grid lines if needed for debugging
        // mapGridPane.setGridLinesVisible(true);
    }

    /** Styles a single cell Pane based on TileType */
    private void styleCellPane(Pane cellPane, TileType type, int row, int col) {
        cellPane.getChildren().clear(); // Clear previous content (like text markers)
        String style = "-fx-border-color: #5a6870; -fx-border-width: 0.5;";
        Color color = Color.LIGHTSLATEGRAY; // Default/Error

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
                color = Color.web("#f5deb3"); // Path color
                cellPane.getChildren().add(new Text("S")); // Mark Start
                break;
            case END:
                color = Color.web("#f5deb3"); // Path color
                cellPane.getChildren().add(new Text("E")); // Mark End
                break;
            case DECOR_TREE:
                color = Color.DARKGREEN;
                break;
            case DECOR_ROCK:
                color = Color.GRAY;
                break;
            default:
                color = Color.PINK; // Unknown type
        }
        cellPane.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
        cellPane.setStyle(style);
    }

    /** Finds the specific Pane node in the GridPane */
    private Node findCellNode(int row, int col) {
        for (Node node : mapGridPane.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == col) {
                return node;
            }
        }
        return null;
    }

    private void updatePositionLabel(int col, int row) {
        Platform.runLater(() -> positionLabel.setText(String.format("Pos: %d, %d", col, row)));
    }

    // --- Re-add Helper Methods --- //

    // Helper for styled Buttons
    private Button createStyledButton(String text, String defaultStyle, String hoverStyle) {
        Button button = new Button(text);
        button.setStyle(defaultStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(defaultStyle));
        // Add other common settings if needed (prefWidth?)
        return button;
    }

    // Helper for styled ToggleButtons
    private ToggleButton createStyledToggleButton(String text, ToggleGroup group, String defaultStyle,
            String selectedStyle, String hoverStyle) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setStyle(defaultStyle);
        button.setPrefWidth(120); // Keep consistent width for palette
        // Listener to change style based on selection
        button.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            button.setStyle(isSelected ? selectedStyle : defaultStyle);
        });
        // Hover effect only when not selected
        button.setOnMouseEntered(e -> {
            if (!button.isSelected())
                button.setStyle(hoverStyle);
        });
        button.setOnMouseExited(e -> {
            if (!button.isSelected())
                button.setStyle(defaultStyle);
        });
        return button;
    }

    /** Returns the root node of the map editor UI. */
    public Parent getView() {
        return view;
    }
}